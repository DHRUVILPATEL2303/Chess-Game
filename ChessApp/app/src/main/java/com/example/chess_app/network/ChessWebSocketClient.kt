package com.example.chess_app.network

import com.example.chess_app.model.*
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import java.util.concurrent.TimeUnit

sealed class WsEvent {
    data class RoomCreated(val roomId: String, val color: String) : WsEvent()
    data class RoomJoined(val color: String) : WsEvent()
    data class GameStarted(val board: Array<Array<String>>, val turn: String) : WsEvent()
    data class GameStateUpdated(val payload: GameStatePayload) : WsEvent()
    data class InvalidMove(val reason: String) : WsEvent()
    data class GameOver(val result: String, val winner: String?) : WsEvent()
    object OpponentDisconnected : WsEvent()
    data class ServerError(val message: String) : WsEvent()
    data class ConnectionError(val message: String) : WsEvent()
    object Connected : WsEvent()
    object Disconnected : WsEvent()
}

object ChessWebSocketClient {

    private val gson = Gson()
    private var ws: WebSocket? = null

    private val _events = MutableSharedFlow<WsEvent>(replay = 1, extraBufferCapacity = 64)
    val events: SharedFlow<WsEvent> = _events

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    fun connect(serverUrl: String) {
        disconnect()
        val request = Request.Builder().url(serverUrl).build()
        ws = client.newWebSocket(request, listener)
    }

    fun disconnect() {
        ws?.close(1000, "closing")
        ws = null
    }

    fun sendCreateRoom() = send(mapOf("type" to ClientMsgType.CREATE_ROOM))

    fun sendCreateBotRoom() = send(mapOf("type" to ClientMsgType.CREATE_BOT_ROOM))

    fun sendJoinRoom(roomId: String) = send(
        mapOf("type" to ClientMsgType.JOIN_ROOM, "room_id" to roomId)
    )

    fun sendMove(from: String, to: String, promo: String = "q") = send(
        mapOf("type" to ClientMsgType.MAKE_MOVE, "from" to from, "to" to to, "promo" to promo)
    )

    fun sendResign() = send(mapOf("type" to ClientMsgType.RESIGN))

    private fun send(data: Map<String, String>) {
        val json = gson.toJson(data)
        ws?.send(json) ?: _events.tryEmit(WsEvent.ConnectionError("Not connected"))
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            _events.tryEmit(WsEvent.Connected)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val root = gson.fromJson(text, JsonObject::class.java)
                val type = root.get("type")?.asString ?: return
                val payload = root.get("payload")

                when (type) {
                    ServerMsgType.ROOM_CREATED -> {
                        val p = gson.fromJson(payload, RoomCreatedPayload::class.java)
                        _events.tryEmit(WsEvent.RoomCreated(p.room_id, p.color))
                    }
                    ServerMsgType.ROOM_JOINED -> {
                        val p = gson.fromJson(payload, RoomJoinedPayload::class.java)
                        _events.tryEmit(WsEvent.RoomJoined(p.color))
                    }
                    ServerMsgType.GAME_STARTED -> {
                        val p = gson.fromJson(payload, GameStartedPayload::class.java)
                        _events.tryEmit(WsEvent.GameStarted(p.board, p.turn))
                    }
                    ServerMsgType.GAME_STATE -> {
                        val p = gson.fromJson(payload, GameStatePayload::class.java)
                        _events.tryEmit(WsEvent.GameStateUpdated(p))
                    }
                    ServerMsgType.INVALID_MOVE -> {
                        val p = gson.fromJson(payload, InvalidMovePayload::class.java)
                        _events.tryEmit(WsEvent.InvalidMove(p.reason))
                    }
                    ServerMsgType.GAME_OVER -> {
                        val p = gson.fromJson(payload, GameOverPayload::class.java)
                        _events.tryEmit(WsEvent.GameOver(p.result, p.winner))
                    }
                    ServerMsgType.OPPONENT_DISCONNECTED -> {
                        _events.tryEmit(WsEvent.OpponentDisconnected)
                    }
                    ServerMsgType.ERROR -> {
                        val p = gson.fromJson(payload, ErrorPayload::class.java)
                        _events.tryEmit(WsEvent.ServerError(p.message))
                    }
                }
            } catch (e: Exception) {
                _events.tryEmit(WsEvent.ServerError("Parse error: ${e.message}"))
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            _events.tryEmit(WsEvent.ConnectionError(t.message ?: "Unknown error"))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            _events.tryEmit(WsEvent.Disconnected)
        }
    }
}
