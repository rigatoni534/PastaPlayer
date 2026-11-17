package rigatoni534.apps.PastaPlayer.data.discord

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Turns a public image URL into a Discord Rich Presence asset (`mp:external/...`).
 *
 * Discord only accepts uploaded application assets or registered external assets, so this
 * calls `POST /applications/{app}/external-assets`. That endpoint usually needs the
 * application owner's credentials; when it refuses the request the presence simply falls
 * back to Discord's default application icon, which is the same fallback Kizzy uses.
 */
@Singleton
class DiscordArtworkResolver @Inject constructor() {

    @Serializable
    private data class ExternalAsset(
        @SerialName("external_asset_path") val externalAssetPath: String? = null,
        @SerialName("url") val url: String? = null,
    )

    private val tag = "RigakizzyArtwork"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /** url → presence asset (null when the URL could not be registered). */
    private val cache = ConcurrentHashMap<String, String?>()

    suspend fun resolve(applicationId: String, token: String, imageUrl: String): String? {
        if (imageUrl.isBlank() || token.isBlank() || applicationId.isBlank()) return null
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) return null
        val cacheKey = "$applicationId|$imageUrl"
        if (cache.containsKey(cacheKey)) return cache[cacheKey]

        val asset = withContext(Dispatchers.IO) { register(applicationId, token, imageUrl) }
        cache[cacheKey] = asset
        return asset
    }

    private fun register(applicationId: String, token: String, imageUrl: String): String? {
        val body = """{"urls":["${imageUrl.replace("\\", "\\\\").replace("\"", "\\\"")}"]}"""
            .toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("https://discord.com/api/v9/applications/$applicationId/external-assets")
            .header("Authorization", token)
            .header("Content-Type", "application/json")
            .post(body)
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.d(tag, "external-assets ${response.code}; falling back to app icon")
                    return@use null
                }
                val raw = response.body?.string() ?: return@use null
                val assets = json.decodeFromString<List<ExternalAsset>>(raw)
                assets.firstOrNull()?.externalAssetPath?.takeIf { it.isNotBlank() }?.let { "mp:$it" }
            }
        }.getOrNull()
    }
}
