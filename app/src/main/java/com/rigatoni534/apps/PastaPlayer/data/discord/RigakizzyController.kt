package rigatoni534.apps.PastaPlayer.data.discord

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import rigatoni534.apps.PastaPlayer.data.database.MusicDao
import rigatoni534.apps.PastaPlayer.data.model.Song
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzyPreferencesRepository
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzyPreset
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzySettings

enum class RigakizzyStatus {
    /** User turned Rigakizzy off. */
    DISABLED,

    /** No Discord token stored yet. */
    NOT_LOGGED_IN,

    /** "Stop for session" is active. */
    STOPPED,

    /** Waiting for something to play. */
    IDLE,

    CONNECTING,
    CONNECTED,
}

/**
 * Bridges PastaPlayer playback to Discord Rich Presence (the Rigakizzy feature).
 *
 * Presence is published as soon as music starts, updated when the song changes, switched to
 * the pause state while paused, and cleared when playback stops or the user stops Rigakizzy.
 */
@Singleton
class RigakizzyController @Inject constructor(
    private val preferences: RigakizzyPreferencesRepository,
    private val gateway: DiscordGatewayClient,
    private val artworkResolver: DiscordArtworkResolver,
    private val musicDao: MusicDao,
) {

    data class PlaybackSnapshot(
        val songId: Long,
        val title: String,
        val artist: String,
        val album: String,
        val artworkUri: String?,
        val durationMs: Long,
        val positionMs: Long,
        val isPlaying: Boolean,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _status = MutableStateFlow(RigakizzyStatus.DISABLED)
    val status: StateFlow<RigakizzyStatus> = _status.asStateFlow()

    private var snapshot: PlaybackSnapshot? = null
    private var refreshJob: Job? = null
    private var lastPresenceSignature: String? = null

    init {
        scope.launch {
            gateway.connected.collect { connected ->
                if (connected) {
                    _status.value = RigakizzyStatus.CONNECTED
                } else if (_status.value == RigakizzyStatus.CONNECTED) {
                    _status.value = RigakizzyStatus.CONNECTING
                }
            }
        }
        scope.launch {
            preferences.settingsFlow.collect { refresh() }
        }
    }

    /** Called by the player whenever the current song / playback state changes. */
    fun updateSong(song: Song?, isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        snapshot = song?.let {
            PlaybackSnapshot(
                songId = it.id.toLongOrNull() ?: -1L,
                title = it.title,
                artist = it.displayArtist.ifBlank { it.artist },
                album = it.album,
                artworkUri = it.albumArtUriString,
                durationMs = durationMs.takeIf { value -> value > 0 } ?: it.duration,
                positionMs = positionMs.coerceAtLeast(0L),
                isPlaying = isPlaying,
            )
        }
        refresh(force = true)
    }

    fun clearPlayback() {
        snapshot = null
        refresh(force = true)
    }

    /** Settings → Rigakizzy → "Stop for session". */
    fun stopForSession() {
        scope.launch { preferences.setStoppedForSession(true) }
    }

    fun resumeAfterStop() {
        scope.launch { preferences.setStoppedForSession(false) }
    }

    private fun refresh(force: Boolean = false) {
        refreshJob?.cancel()
        refreshJob = scope.launch { publish(force) }
    }

    private suspend fun publish(force: Boolean) {
        val settings = preferences.settingsOnce()
        if (!settings.canPublish) {
            lastPresenceSignature = null
            gateway.disconnect()
            _status.value = when {
                !settings.isLoggedIn -> RigakizzyStatus.NOT_LOGGED_IN
                settings.stoppedForSession -> RigakizzyStatus.STOPPED
                else -> RigakizzyStatus.DISABLED
            }
            return
        }

        val state = snapshot
        if (state == null) {
            lastPresenceSignature = null
            gateway.disconnect()
            _status.value = RigakizzyStatus.IDLE
            return
        }

        val presence = buildPresence(settings, state)
        // Ignore the timestamp portion so identical activities are not re-sent.
        val signature = presence.activities.firstOrNull()?.let { activity ->
            listOf(
                activity.name,
                activity.details,
                activity.state,
                activity.assets?.largeImage,
                activity.assets?.smallImage,
                activity.timestamps != null,
            ).joinToString("|")
        }
        if (!force && signature != null && signature == lastPresenceSignature) return
        lastPresenceSignature = signature

        if (gateway.isConnected) {
            gateway.updatePresence(presence)
            _status.value = RigakizzyStatus.CONNECTED
        } else {
            _status.value = RigakizzyStatus.CONNECTING
            gateway.connect(settings.token, presence)
        }
    }

    private suspend fun buildPresence(
        settings: RigakizzySettings,
        state: PlaybackSnapshot,
    ): DiscordPresence {
        val lines = resolveLines(settings, state)
        val artworkUrl = resolveArtworkUrl(state)
        val largeImage = artworkUrl?.let { url ->
            artworkResolver.resolve(settings.applicationId, settings.token, url)
        }
        val smallImage = if (state.isPlaying) {
            settings.playingAssetKey
        } else {
            settings.pausedAssetKey
        }
        val now = System.currentTimeMillis()
        val timestamps = if (state.isPlaying && state.durationMs > 0L) {
            val start = now - state.positionMs.coerceIn(0L, state.durationMs)
            DiscordTimestamps(start = start, end = start + state.durationMs)
        } else {
            null
        }

        Log.d(
            TAG,
            "Publishing presence title=${lines.name} details=${lines.details} state=${lines.state} " +
                "artwork=${largeImage != null} playing=${state.isPlaying}"
        )

        return DiscordPresence(
            activities = listOf(
                DiscordActivity(
                    name = lines.name,
                    type = ACTIVITY_TYPE_LISTENING,
                    details = lines.details,
                    state = lines.state,
                    applicationId = settings.applicationId,
                    timestamps = timestamps,
                    assets = DiscordAssets(
                        largeImage = largeImage,
                        largeText = state.album.ifBlank { null },
                        smallImage = smallImage.ifBlank { null },
                        smallText = if (state.isPlaying) "Playing" else "Paused",
                    ),
                    platform = "android",
                )
            ),
            since = now,
        )
    }

    private data class PresenceLines(val name: String, val details: String, val state: String)

    private fun resolveLines(
        settings: RigakizzySettings,
        state: PlaybackSnapshot,
    ): PresenceLines {
        val artist = state.artist.ifBlank { "Unknown artist" }
        return when (settings.preset) {
            RigakizzyPreset.PLAYER_TITLE -> PresenceLines(
                name = PLAYER_NAME,
                details = state.title,
                state = artist,
            )
            RigakizzyPreset.ARTIST_TITLE -> PresenceLines(
                name = artist,
                details = state.title,
                state = PLAYER_NAME,
            )
            RigakizzyPreset.SONG_TITLE -> PresenceLines(
                name = state.title,
                details = artist,
                state = PLAYER_NAME,
            )
            RigakizzyPreset.CUSTOM -> PresenceLines(
                name = expand(settings.customName, state).ifBlank { PLAYER_NAME },
                details = expand(settings.customDetails, state),
                state = expand(settings.customState, state),
            )
        }.sanitized()
    }

    private fun PresenceLines.sanitized() = PresenceLines(
        name = name.take(MAX_FIELD_LENGTH).ifBlank { PLAYER_NAME },
        details = details.take(MAX_FIELD_LENGTH),
        state = state.take(MAX_FIELD_LENGTH),
    )

    private fun expand(template: String, state: PlaybackSnapshot): String = template
        .replace("{song}", state.title)
        .replace("{artist}", state.artist)
        .replace("{album}", state.album)
        .replace("{player}", PLAYER_NAME)
        .take(MAX_FIELD_LENGTH)

    /** Song/album artwork first, then the artist image, else null (Discord shows the app icon). */
    private suspend fun resolveArtworkUrl(state: PlaybackSnapshot): String? {
        state.artworkUri
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?.let { return it }

        if (state.songId <= 0L) return null
        return runCatching {
            musicDao.getArtistsForSongList(state.songId)
                .asSequence()
                .mapNotNull { artist -> artist.customImageUri ?: artist.imageUrl }
                .firstOrNull { it.startsWith("http://") || it.startsWith("https://") }
        }.getOrNull()
    }

    companion object {
        private const val TAG = "Rigakizzy"
        private const val PLAYER_NAME = "PastaPlayer"
        private const val ACTIVITY_TYPE_LISTENING = 2
        private const val MAX_FIELD_LENGTH = 128
    }
}
