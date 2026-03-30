package com.example.chess_app.model

const val EMPTY_SQUARE = "."

data class Square(val row: Int, val col: Int) {
    val algebraic: String get() = "${'a' + col}${'1' + row}"

    companion object {
        fun fromAlgebraic(alg: String): Square? {
            if (alg.length != 2) return null
            val col = alg[0] - 'a'
            val row = alg[1] - '1'
            if (col !in 0..7 || row !in 0..7) return null
            return Square(row, col)
        }
    }
}

data class LastMove(val from: String, val to: String)

data class GameState(
    val board: Array<Array<String>>,
    val turn: String,
    val lastMove: LastMove? = null,
    val isCheck: Boolean = false,
    val capturedWhite: List<String> = emptyList(),
    val capturedBlack: List<String> = emptyList()
) {
    fun pieceAt(row: Int, col: Int): String = board[row][col]
    fun pieceAt(sq: Square): String = board[sq.row][sq.col]
    fun isEmpty(sq: Square) = pieceAt(sq) == EMPTY_SQUARE
}

data class ServerMessage(
    val type: String,
    val payload: Any? = null
)

data class RoomCreatedPayload(val room_id: String, val color: String)
data class RoomJoinedPayload(val color: String)
data class GameStartedPayload(
    val board: Array<Array<String>>,
    val turn: String,
    val white: String,
    val black: String
)
data class GameStatePayload(
    val board: Array<Array<String>>,
    val turn: String,
    val last_move: LastMoveJson? = null,
    val is_check: Boolean = false,
    val captured_white: List<String> = emptyList(),
    val captured_black: List<String> = emptyList()
)
data class LastMoveJson(val from: String, val to: String)
data class GameOverPayload(val result: String, val winner: String? = null)
data class InvalidMovePayload(val reason: String)
data class ErrorPayload(val message: String)

object ServerMsgType {
    const val ROOM_CREATED           = "room_created"
    const val ROOM_JOINED            = "room_joined"
    const val GAME_STARTED           = "game_started"
    const val GAME_STATE             = "game_state"
    const val INVALID_MOVE           = "invalid_move"
    const val GAME_OVER              = "game_over"
    const val OPPONENT_DISCONNECTED  = "opponent_disconnected"
    const val ERROR                  = "error"
}

object ClientMsgType {
    const val CREATE_ROOM = "create_room"
    const val JOIN_ROOM   = "join_room"
    const val MAKE_MOVE   = "make_move"
    const val RESIGN      = "resign"
}
