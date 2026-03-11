package com.ultimate.engine.network

import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class NetEntityState(val id: Int, val x: Float, val y: Float, val z: Float, val timeMs: Long)

class MultiplayerClient {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val client = OkHttpClient()
    private var socket: WebSocket? = null
    private val _updates = MutableStateFlow<NetEntityState?>(null)
    val updates: StateFlow<NetEntityState?> = _updates

    fun connect(url: String, onConnected: () -> Unit = {}, onRawMessage: (String) -> Unit = {}) {
        socket = client.newWebSocket(Request.Builder().url(url).build(), object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) = onConnected()
            override fun onMessage(webSocket: WebSocket, text: String) {
                onRawMessage(text)
                val parts = text.split(",")
                if (parts.size >= 5 && parts[0] == "STATE") {
                    _updates.value = NetEntityState(parts[1].toInt(), parts[2].toFloat(), parts[3].toFloat(), parts[4].toFloat(), System.currentTimeMillis())
                }
            }
        })
    }

    fun sendState(state: NetEntityState) {
        socket?.send("STATE,${state.id},${state.x},${state.y},${state.z},${state.timeMs}")
    }

    fun join(roomId: String, playerId: String) {
        socket?.send("JOIN,$roomId,$playerId")
    }

    fun close() {
        socket?.close(1000, "bye")
        scope.cancel()
    }
}

class MatchmakingServer(private val port: Int = 8080) {
    private val rooms = ConcurrentHashMap<String, MutableSet<String>>()
    private var job: Job? = null

    fun start() {
        job = CoroutineScope(Dispatchers.IO).launch {
            embeddedServer(CIO, port = port) {
                install(WebSockets)
                routing {
                    webSocket("/multiplayer") {
                        val sessionId = UUID.randomUUID().toString()
                        incoming.consumeEach { frame ->
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                val parts = text.split(",")
                                when (parts.firstOrNull()) {
                                    "JOIN" -> {
                                        if (parts.size >= 3) {
                                            rooms.computeIfAbsent(parts[1]) { mutableSetOf() }.add(parts[2])
                                            outgoing.send(Frame.Text("ROOM,${parts[1]},${rooms[parts[1]]?.size ?: 0}"))
                                        }
                                    }
                                    "STATE" -> outgoing.send(Frame.Text(text))
                                }
                            }
                        }
                        rooms.values.forEach { it.remove(sessionId) }
                    }
                }
            }.start(wait = true)
        }
    }

    fun stop() {
        job?.cancel()
    }
}

class LatencyInterpolator(private val alpha: Float = 0.18f) {
    fun interpolate(current: NetEntityState, target: NetEntityState): NetEntityState =
        current.copy(
            x = current.x + (target.x - current.x) * alpha,
            y = current.y + (target.y - current.y) * alpha,
            z = current.z + (target.z - current.z) * alpha,
            timeMs = target.timeMs
        )
}
