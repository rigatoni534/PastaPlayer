package rigatoni534.apps.PastaPlayer.data.vault

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rigatoni534.apps.PastaPlayer.data.database.MusicDao
import rigatoni534.apps.PastaPlayer.data.database.VaultedSongRow
import rigatoni534.apps.PastaPlayer.data.model.Song
import rigatoni534.apps.PastaPlayer.data.preferences.UserPreferencesRepository

/**
 * Song Vault: moves audio files into the app's private storage
 * (`filesDir/song_vault`) so no other app (or the system media scanner) can see them.
 *
 * The songs table keeps the same song ids, so playlists, favorites, lyrics, playback
 * stats and engagement rows keep pointing at the right song — only the file location
 * (`content_uri_string`, `file_path`, `parent_directory_path`) is rewritten to the
 * private copy and the original location is remembered for restoring.
 */
@Singleton
class SongVaultManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicDao: MusicDao,
    private val userPreferencesRepository: UserPreferencesRepository,
) {

    data class VaultSummary(
        val songCount: Int,
        val totalBytes: Long,
    )

    data class VaultedSong(
        val id: String,
        val title: String,
        val artist: String,
        val bytes: Long,
    )

    enum class Failure {
        UNSUPPORTED_SONG,
        NOT_VAULTED,
        SOURCE_UNREADABLE,
        COPY_FAILED,
        EXPORT_FAILED,
    }

    sealed interface VaultResult {
        /**
         * [pendingDeleteUri] is set when the original copy could not be removed
         * automatically and the caller should ask the system for permission
         * (`MediaStore.createDeleteRequest`) to delete it.
         */
        data class Vaulted(
            val songId: String,
            val pendingDeleteUri: String?,
            val originalRemoved: Boolean,
        ) : VaultResult

        data class Restored(
            val songId: String,
            val contentUri: String,
        ) : VaultResult

        data class Failed(
            val songId: String,
            val failure: Failure,
        ) : VaultResult
    }

    private val vaultDirectory: File
        get() = File(context.filesDir, VAULT_DIR_NAME).also { dir ->
            if (!dir.exists()) dir.mkdirs()
        }

    suspend fun isVaulted(songId: String): Boolean = withContext(Dispatchers.IO) {
        val numericId = songId.toLongOrNull() ?: return@withContext false
        musicDao.countSongVaulted(numericId) > 0
    }

    suspend fun getSummary(): VaultSummary = withContext(Dispatchers.IO) {
        val files = vaultDirectory.listFiles().orEmpty().filter { it.isFile }
        VaultSummary(
            songCount = musicDao.getVaultedSongIds().size,
            totalBytes = files.sumOf { it.length() },
        )
    }

    suspend fun getVaultedSongs(): List<VaultedSong> = withContext(Dispatchers.IO) {
        musicDao.getVaultedSongEntities().map { entity ->
            val file = File(entity.filePath)
            VaultedSong(
                id = entity.id.toString(),
                title = entity.title,
                artist = entity.artistName,
                bytes = if (file.exists()) file.length() else 0L,
            )
        }
    }

    /**
     * Copies [song]'s audio into private storage, rewrites the song row so every other
     * part of the app (playlists, favorites, queue, widgets) uses the private copy, and
     * tries to delete the original public file.
     */
    suspend fun vaultSong(song: Song): VaultResult = withContext(Dispatchers.IO) {
        val numericId = song.id.toLongOrNull()
            ?: return@withContext VaultResult.Failed(song.id, Failure.UNSUPPORTED_SONG)

        if (musicDao.countSongVaulted(numericId) > 0) {
            return@withContext VaultResult.Vaulted(
                songId = song.id,
                pendingDeleteUri = null,
                originalRemoved = false,
            )
        }

        val sourceUriString = song.contentUriString.ifBlank { song.path }
        if (sourceUriString.isBlank()) {
            return@withContext VaultResult.Failed(song.id, Failure.SOURCE_UNREADABLE)
        }
        val sourceUri = runCatching { sourceUriString.toUri() }.getOrNull()
            ?: return@withContext VaultResult.Failed(song.id, Failure.SOURCE_UNREADABLE)

        val directory = vaultDirectory
        val target = File(directory, "$numericId.${resolveExtension(song)}")
        val copied = runCatching { copyToFile(sourceUri, target) }.getOrDefault(false)
        if (!copied || !target.exists() || target.length() <= 0L) {
            runCatching { target.delete() }
            return@withContext VaultResult.Failed(song.id, Failure.COPY_FAILED)
        }

        val parentDirectory = if (song.path.contains('/')) {
            song.path.substringBeforeLast('/')
        } else {
            directory.absolutePath
        }

        musicDao.markSongVaulted(
            songId = numericId,
            contentUri = Uri.fromFile(target).toString(),
            filePath = target.absolutePath,
            parentDirectoryPath = parentDirectory,
            originalUri = sourceUriString,
            originalPath = song.path.takeIf { it.isNotBlank() },
        )

        rewritePlaybackSnapshot(song.id, Uri.fromFile(target).toString())

        val removal = removePublicCopy(sourceUriString)
        VaultResult.Vaulted(
            songId = song.id,
            pendingDeleteUri = removal.pendingUri,
            originalRemoved = removal.removed,
        )
    }

    /**
     * Moves a song out of the vault. If the original file still exists it is reused,
     * otherwise the private copy is exported back to `Music/PastaPlayer` through MediaStore.
     */
    suspend fun restoreSong(songId: String): VaultResult = withContext(Dispatchers.IO) {
        val numericId = songId.toLongOrNull()
            ?: return@withContext VaultResult.Failed(songId, Failure.UNSUPPORTED_SONG)
        val row = musicDao.getVaultedSongRow(numericId)
            ?: return@withContext VaultResult.Failed(songId, Failure.NOT_VAULTED)
        val vaultFile = File(row.filePath)
        if (!vaultFile.exists()) {
            return@withContext VaultResult.Failed(songId, Failure.SOURCE_UNREADABLE)
        }

        val originalPath = row.vaultOriginalPath
        val originalUri = row.vaultOriginalUri
        if (!originalPath.isNullOrBlank() && File(originalPath).exists()) {
            musicDao.clearSongVaultState(
                songId = numericId,
                contentUri = originalUri ?: originalPath,
                filePath = originalPath,
                parentDirectoryPath = originalPath.substringBeforeLast('/', ""),
            )
            runCatching { vaultFile.delete() }
            rewritePlaybackSnapshot(songId, originalUri ?: originalPath)
            return@withContext VaultResult.Restored(songId, originalUri ?: originalPath)
        }

        val export = exportToSharedMusic(row, vaultFile)
            ?: return@withContext VaultResult.Failed(songId, Failure.EXPORT_FAILED)

        musicDao.clearSongVaultState(
            songId = numericId,
            contentUri = export.first,
            filePath = export.second,
            parentDirectoryPath = export.second.substringBeforeLast('/', ""),
        )
        runCatching { vaultFile.delete() }
        rewritePlaybackSnapshot(songId, export.first)
        VaultResult.Restored(songId, export.first)
    }

    /**
     * The persisted playback queue stores content URIs per item, so it has to follow the
     * song when the file moves in or out of the vault.
     */
    private suspend fun rewritePlaybackSnapshot(songId: String, newUri: String) {
        val snapshot = runCatching { userPreferencesRepository.getPlaybackQueueSnapshotOnce() }
            .getOrNull() ?: return
        var changed = false
        val items = snapshot.items.map { item ->
            if (item.mediaId == songId && item.uri != newUri) {
                changed = true
                item.copy(uri = newUri)
            } else {
                item
            }
        }
        if (changed) {
            runCatching {
                userPreferencesRepository.setPlaybackQueueSnapshot(snapshot.copy(items = items))
            }
        }
    }

    private fun copyToFile(sourceUri: Uri, target: File): Boolean {
        if (sourceUri.scheme == "file") {
            val source = File(sourceUri.path ?: return false)
            if (!source.exists()) return false
            source.inputStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            return true
        }
        val input = context.contentResolver.openInputStream(sourceUri) ?: return false
        input.use { stream ->
            target.outputStream().use { output -> stream.copyTo(output) }
        }
        return true
    }

    private data class RemovalResult(
        val removed: Boolean,
        val pendingUri: String?,
    )

    private fun removePublicCopy(uriString: String): RemovalResult {
        val uri = runCatching { uriString.toUri() }.getOrNull()
            ?: return RemovalResult(removed = false, pendingUri = null)
        return when (uri.scheme) {
            "content" -> {
                val deleted = runCatching { context.contentResolver.delete(uri, null, null) }
                    .getOrDefault(0)
                if (deleted > 0) {
                    RemovalResult(removed = true, pendingUri = null)
                } else {
                    // Shared media needs per-item consent; the UI asks for it.
                    RemovalResult(removed = false, pendingUri = uriString)
                }
            }
            "file" -> {
                val path = uri.path
                val deleted = path != null && runCatching { File(path).delete() }.getOrDefault(false)
                RemovalResult(removed = deleted, pendingUri = null)
            }
            else -> RemovalResult(removed = false, pendingUri = null)
        }
    }

    private fun exportToSharedMusic(row: VaultedSongRow, vaultFile: File): Pair<String, String>? {
        val resolver = context.contentResolver
        val displayName = buildExportName(row, vaultFile)
        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Audio.Media.MIME_TYPE, mimeTypeFor(vaultFile.extension))
            put(MediaStore.Audio.Media.RELATIVE_PATH, "$EXPORT_FOLDER")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = runCatching { resolver.insert(collection, values) }.getOrNull() ?: return null

        val output = runCatching { resolver.openOutputStream(uri) }.getOrNull()
        if (output == null) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }
        val written = runCatching {
            output.use { stream -> vaultFile.inputStream().use { input -> input.copyTo(stream) } }
        }
        if (written.isFailure) {
            runCatching { resolver.delete(uri, null, null) }
            return null
        }

        val published = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
        runCatching { resolver.update(uri, published, null, null) }

        val absolutePath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            "$EXPORT_FOLDER/$displayName",
        ).absolutePath
        return uri.toString() to absolutePath
    }

    private fun buildExportName(row: VaultedSongRow, vaultFile: File): String {
        val safeTitle = row.title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .trim()
            .take(80)
            .ifBlank { "Track ${row.id}" }
        val extension = vaultFile.extension.ifBlank { "mp3" }
        return "$safeTitle.${row.id}.$extension"
    }

    private fun resolveExtension(song: Song): String {
        val fromPath = song.path.substringAfterLast('.', "").lowercase()
        if (fromPath.length in 1..5 && fromPath.all { it.isLetterOrDigit() }) {
            return fromPath
        }
        return when (song.mimeType?.lowercase()) {
            "audio/mpeg", "audio/mp3" -> "mp3"
            "audio/flac", "audio/x-flac" -> "flac"
            "audio/mp4", "audio/m4a", "audio/x-m4a" -> "m4a"
            "audio/ogg" -> "ogg"
            "audio/opus" -> "opus"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/aac" -> "aac"
            else -> "mp3"
        }
    }

    private fun mimeTypeFor(extension: String): String = when (extension.lowercase()) {
        "mp3" -> "audio/mpeg"
        "flac" -> "audio/flac"
        "m4a", "mp4" -> "audio/mp4"
        "ogg" -> "audio/ogg"
        "opus" -> "audio/opus"
        "wav" -> "audio/x-wav"
        "aac" -> "audio/aac"
        else -> "audio/mpeg"
    }

    companion object {
        const val VAULT_DIR_NAME = "song_vault"
        private const val EXPORT_FOLDER = "Music/PastaPlayer"
    }
}
