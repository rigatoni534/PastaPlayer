package rigatoni534.apps.PastaPlayer.data.discord

import android.util.Log
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * Discord user-gateway connection used to publish Rich Presence.
 *
 * Ported from Kizzy/Rigakizzy's `DiscordWebSocketImpl`, but built on OkHttp (already a
 * PastaPlayer dependency) instead of Ktor: identify → heartbeat → presence updates, with
 * resume after transport errors.
 */
@Singleton
class DiscordGatewayClient @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client = OkHttpClient.Builder()
        .pingInterval(30, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var sequence = 0
    private var sessionId: String? = null
    private var resumeGatewayUrl: String? = null
    private var token: String? = null
    private var pendingPresence: DiscordPresence? = null
    private var lastPresenceSentAt = 0L
    private var pendingFlush: Job? = null
    private val connectedToAccount = AtomicBoolean(false)
    private val running = AtomicBoolean(false)
    private val _connected = kotlinx.coroutines.flow.MutableStateFlow(false)

    /** Emits true once the gateway has sent READY/RESUMED and presence can be published. */
    val connected: kotlinx.coroutines.flow.StateFlow<Boolean> = _connected.asStateFlow()

    val isConnected: Boolean get() = connectedToAccount.get()

    fun connect(token: String, presence: DiscordPresence?) {
        if (token.isBlank()) return
        val alreadyRunning = running.get() && this.token == token
        this.token = token
        pendingPresence = presence
        if (alreadyRunning) {
            // Already connecting/connected on this token: just refresh the activity.
            if (connectedToAccount.get()) presence?.let { updatePresence(it) }
            return
        }
        running.set(true)
        scope.launch { openSocket(forceNew = sessionId == null) }
    }

    fun updatePresence(presence: DiscordPresence) {
        pendingPresence = presence
        if (!connectedToAccount.get()) return
        // Discord rate limits presence updates, so coalesce bursts from pausing/resuming.
        val elapsed = System.currentTimeMillis() - lastPresenceSentAt
        if (elapsed >= MIN_PRESENCE_INTERVAL_MS) {
            sendPresenceNow(presence)
        } else {
            pendingFlush?.cancel()
            pendingFlush = scope.launch {
                delay(MIN_PRESENCE_INTERVAL_MS - elapsed)
                pendingPresence?.let { sendPresenceNow(it) }
            }
        }
    }

    private fun sendPresenceNow(presence: DiscordPresence) {
        if (!connectedToAccount.get()) return
        lastPresenceSentAt = System.currentTimeMillis()
        webSocket?.send(presenceFrame(presence))
    }

    fun disconnect() {
        running.set(false)
        heartbeatJob?.cancel()
        heartbeatJob = null
        pendingFlush?.cancel()
        pendingFlush = null
        lastPresenceSentAt = 0L
        connectedToAccount.set(false)
        _connected.value = false
        sequence = 0
        sessionId = null
        resumeGatewayUrl = null
        pendingPresence = null
        runCatching { webSocket?.close(1000, "Rigakizzy stopped") }
        webSocket = null
    }

    private fun openSocket(forceNew: Boolean) {
        if (!running.get()) return
        val url = if (!forceNew && !resumeGatewayUrl.isNullOrBlank()) {
            resumeGatewayUrl!!
        } else {
            DEFAULT_GATEWAY_URL
        }
        val request = Request.Builder().url(url).build()
        runCatching { webSocket?.cancel() }
        connectedToAccount.set(false)
        webSocket = client.newWebSocket(request, listener)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "Gateway socket opened")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            handleMessage(text)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.w(TAG, "Gateway closed code=$code reason=$reason")
            connectedToAccount.set(false)
            _connected.value = false
            heartbeatJob?.cancel()
            if (code in FATAL_CLOSE_CODES) {
                // Invalid/expired token or a banned session: stop instead of retrying forever.
                running.set(false)
                Log.w(TAG, "Gateway closed with non-recoverable code=$code; presence stopped")
                return
            }
            if (running.get() && code != 1000) {
                scope.launch {
                    delay(RECONNECT_DELAY_MS)
                    openSocket(forceNew = code != 4000)
                }
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.w(TAG, "Gateway failure: ${t.message}")
            connectedToAccount.set(false)
            _connected.value = false
            heartbeatJob?.cancel()
            if (response?.code == 401) {
                running.set(false)
                Log.w(TAG, "Gateway rejected the token; presence stopped")
                return
            }
            if (running.get()) {
                scope.launch {
                    delay(RECONNECT_DELAY_MS)
                    openSocket(forceNew = sessionId == null)
                }
            }
        }
    }

    private fun handleMessage(text: String) {
        val frame = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
        val op = frame["op"]?.jsonPrimitive?.intOrNull ?: return
        frame["s"]?.jsonPrimitive?.intOrNull?.let { sequence = it }

        when (op) {
            OP_DISPATCH -> handleDispatch(frame)
            OP_HEARTBEAT -> sendHeartbeat()
            OP_RECONNECT -> {
                connectedToAccount.set(false)
                runCatching { webSocket?.close(4000, "Reconnect requested") }
            }
            OP_INVALID_SESSION -> {
                connectedToAccount.set(false)
                sessionId = null
                resumeGatewayUrl = null
                sequence = 0
                scope.launch {
                    delay(INVALID_SESSION_DELAY_MS)
                    webSocket?.send(identifyFrame(token.orEmpty()))
                }
            }
            OP_HELLO -> {
                establishSession()
                                val interval = runCatching {
                    frame["d"]?.jsonObject
                        ?.get("heartbeat_interval")
                        ?.jsonPrimitive
                        ?.content
                        ?.toLongOrNull()
                }.getOrNull() ?: DEFAULT_HEARTBEAT_MS
                startHeartbeat(interval)
            }
        }
    }

    private fun handleDispatch(frame: kotlinx.serialization.json.JsonObject) {
        val event = frame["t"]?.jsonPrimitive?.content ?: return
        when (event) {
            "READY" -> {
                connectedToAccount.set(true)
                _connected.value = true
                val data = frame["d"]?.jsonObject
                sessionId = data?.get("session_id")?.jsonPrimitive?.content
                val resumeUrl = data?.get("resume_gateway_url")?.jsonPrimitive?.content
                if (!resumeUrl.isNullOrBlank()) {
                    resumeGatewayUrl = "$resumeUrl/?v=10&encoding=json"
                }
                pendingPresence?.let { sendPresenceNow(it) }
            }
            "RESUMED" -> {
                connectedToAccount.set(true)
                _connected.value = true
                pendingPresence?.let { sendPresenceNow(it) }
            }
        }
    }

    private fun establishSession() {
        val currentToken = token.orEmpty()
        val currentSession = sessionId
        if (!currentSession.isNullOrBlank() && sequence > 0) {
            webSocket?.send(resumeFrame(currentToken, currentSession, sequence))
        } else {
            webSocket?.send(identifyFrame(currentToken))
        }
    }

    private fun sendHeartbeat() {
        webSocket?.send(heartbeatFrame(sequence))
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (running.get()) {
                sendHeartbeat()
                delay(intervalMs.coerceAtLeast(1_000L))
            }
        }
    }

    companion object {
        private const val TAG = "RigakizzyGateway"
        private const val DEFAULT_GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
        private const val DEFAULT_HEARTBEAT_MS = 41_250L
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val INVALID_SESSION_DELAY_MS = 1_500L
        private const val MIN_PRESENCE_INTERVAL_MS = 1_500L
        private val FATAL_CLOSE_CODES = setOf(4004, 4010, 4011, 4012, 4013, 4014)
        private const val OP_DISPATCH = 0
        private const val OP_HEARTBEAT = 1
        private const val OP_RECONNECT = 7
        private const val OP_INVALID_SESSION = 9
        private const val OP_HELLO = 10
    }
}
