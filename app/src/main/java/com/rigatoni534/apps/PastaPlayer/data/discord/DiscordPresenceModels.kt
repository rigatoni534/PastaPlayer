package rigatoni534.apps.PastaPlayer.data.discord

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Minimal Discord gateway / Rich Presence payloads ported from Kizzy (Rigakizzy). */
@Serializable
data class DiscordActivity(
    @SerialName("name") val name: String,
    @SerialName("type") val type: Int = 2,
    @SerialName("details") val details: String? = null,
    @SerialName("state") val state: String? = null,
    @SerialName("application_id") val applicationId: String? = null,
    @SerialName("timestamps") val timestamps: DiscordTimestamps? = null,
    @SerialName("assets") val assets: DiscordAssets? = null,
    @SerialName("platform") val platform: String? = null,
)

@Serializable
data class DiscordTimestamps(
    @SerialName("start") val start: Long? = null,
    @SerialName("end") val end: Long? = null,
)

@Serializable
data class DiscordAssets(
    @SerialName("large_image") val largeImage: String? = null,
    @SerialName("large_text") val largeText: String? = null,
    @SerialName("small_image") val smallImage: String? = null,
    @SerialName("small_text") val smallText: String? = null,
)

@Serializable
data class DiscordPresence(
    @SerialName("activities") val activities: List<DiscordActivity>,
    @SerialName("afk") val afk: Boolean = false,
    @SerialName("since") val since: Long? = null,
    @SerialName("status") val status: String = "online",
)

private val discordJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** `{"op": N, "d": ...}` gateway envelope. */
fun buildGatewayFrame(op: Int, data: JsonObject? = null): String =
    buildJsonObject {
        put("op", op)
        if (data != null) put("d", data)
    }.toString()

fun identifyFrame(token: String): String = buildGatewayFrame(
    op = 2,
    data = buildJsonObject {
        put("token", token)
        put("capabilities", 65)
        put("compress", false)
        put("largeThreshold", 100)
        put(
            "properties",
            buildJsonObject {
                put("os", "Android")
                put("browser", "PastaPlayer")
                put("device", "PastaPlayer")
            }
        )
    }
)

fun presenceFrame(presence: DiscordPresence): String {
    val data: JsonObject =
        discordJson.encodeToJsonElement(DiscordPresence.serializer(), presence).jsonObject
    return buildGatewayFrame(op = 3, data = data)
}

fun heartbeatFrame(sequence: Int): String =
    if (sequence <= 0) """{"op":1,"d":null}""" else """{"op":1,"d":$sequence}"""

fun resumeFrame(token: String, sessionId: String, sequence: Int): String = buildGatewayFrame(
    op = 6,
    data = buildJsonObject {
        put("token", token)
        put("session_id", sessionId)
        put("seq", sequence)
    }
)
