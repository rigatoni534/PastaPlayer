package rigatoni534.apps.PastaPlayer.data.discord

import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Validates a Discord user token (extracted by the login WebView) before it is stored. */
@Singleton
class DiscordAccountApi @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Returns the account username when the token works, or null when Discord rejects it. */
    suspend fun fetchUsername(token: String): String? {
        if (token.isBlank()) return null
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://discord.com/api/v9/users/@me")
                .header("Authorization", token)
                .header("User-Agent", "PastaPlayer (Rigakizzy)")
                .get()
                .build()
            runCatching {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    Regex("\"username\"\\s*:\\s*\"([^\"]+)\"")
                        .find(body)
                        ?.groupValues
                        ?.getOrNull(1)
                }
            }.getOrNull()
        }
    }
}
