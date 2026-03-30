package com.example.chess_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess_app.engine.ChessEngine
import com.example.chess_app.model.GameStatePayload
import com.example.chess_app.model.Square
import com.example.chess_app.network.ChessWebSocketClient
import com.example.chess_app.network.WsEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class GameUiState(
    val board: Array<Array<String>> = Array(8) { Array(8) { "." } },
    val myColor: String = "white",
    val turn: String = "white",
    val selectedSquare: Square? = null,
    val legalMoves: List<Square> = emptyList(),
    val lastMoveFrom: Square? = null,
    val lastMoveTo: Square? = null,
    val isCheck: Boolean = false,
    val capturedByMe: List<String> = emptyList(),
    val capturedByOpponent: List<String> = emptyList(),
    val isGameOver: Boolean = false,
    val gameOverResult: String = "",
    val gameOverWinner: String? = null,
    val statusMessage: String = "Waiting for players…",
    val isWaiting: Boolean = true,
    val opponentDisconnected: Boolean = false,
    val promotionPending: Pair<Square, Square>? = null,
)

class GameViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var initialized = false

    fun initColor(color: String) {
        if (initialized) return
        initialized = true
        _uiState.value = _uiState.value.copy(myColor = color)
        collectEvents()
    }

    private fun collectEvents() {
        viewModelScope.launch {
            ChessWebSocketClient.events.collect { event ->
                when (event) {
                    is WsEvent.GameStarted -> {
                        val s = _uiState.value
                        _uiState.value = s.copy(
                            board = event.board,
                            turn = event.turn,
                            isWaiting = false,
                            statusMessage = turnMessage(event.turn, s.myColor)
                        )
                    }
                    is WsEvent.GameStateUpdated -> applyState(event.payload)
                    is WsEvent.InvalidMove -> {
                        _uiState.value = _uiState.value.copy(
                            selectedSquare = null,
                            legalMoves = emptyList(),
                            statusMessage = "Invalid move – try again"
                        )
                    }
                    is WsEvent.GameOver -> {
                        _uiState.value = _uiState.value.copy(
                            isGameOver = true,
                            gameOverResult = event.result,
                            gameOverWinner = event.winner,
                            selectedSquare = null,
                            legalMoves = emptyList()
                        )
                    }
                    is WsEvent.OpponentDisconnected -> {
                        _uiState.value = _uiState.value.copy(opponentDisconnected = true)
                    }
                    else -> {}
                }
            }
        }
    }

    private fun applyState(p: GameStatePayload) {
        val s = _uiState.value
        val lastFrom = p.last_move?.from?.let { Square.fromAlgebraic(it) }
        val lastTo   = p.last_move?.to?.let { Square.fromAlgebraic(it) }

        val capturedByMe = if (s.myColor == "white") p.captured_white else p.captured_black
        val capturedByOp = if (s.myColor == "white") p.captured_black else p.captured_white

        _uiState.value = s.copy(
            board = p.board,
            turn = p.turn,
            isCheck = p.is_check,
            lastMoveFrom = lastFrom,
            lastMoveTo = lastTo,
            selectedSquare = null,
            legalMoves = emptyList(),
            capturedByMe = capturedByMe,
            capturedByOpponent = capturedByOp,
            statusMessage = turnMessage(p.turn, s.myColor) +
                    if (p.is_check) " — CHECK!" else ""
        )
    }

    fun onSquareTapped(sq: Square) {
        val s = _uiState.value
        if (s.isGameOver || s.isWaiting) return
        if (s.turn != s.myColor) return

        val selected = s.selectedSquare

        if (selected != null && s.legalMoves.contains(sq)) {
            val piece = s.board[selected.row][selected.col]
            val isPromo = piece.uppercase() == "P" &&
                    ((s.myColor == "white" && sq.row == 7) ||
                            (s.myColor == "black" && sq.row == 0))
            if (isPromo) {
                _uiState.value = s.copy(promotionPending = selected to sq, selectedSquare = null, legalMoves = emptyList())
                return
            }
            sendMove(selected, sq, "q")
            return
        }

        val piece = s.board[sq.row][sq.col]
        if (piece == "." || ChessEngine.colorOf(piece) != s.myColor) {
            _uiState.value = s.copy(selectedSquare = null, legalMoves = emptyList())
            return
        }
        val moves = ChessEngine.legalMovesFor(s.board, sq, s.myColor)
        _uiState.value = s.copy(selectedSquare = sq, legalMoves = moves)
    }

    fun onPromotionChosen(piece: String) {
        val s = _uiState.value
        val (from, to) = s.promotionPending ?: return
        _uiState.value = s.copy(promotionPending = null)
        sendMove(from, to, piece)
    }

    fun resign() {
        ChessWebSocketClient.sendResign()
    }

    private fun sendMove(from: Square, to: Square, promo: String) {
        _uiState.value = _uiState.value.copy(selectedSquare = null, legalMoves = emptyList())
        ChessWebSocketClient.sendMove(from.algebraic, to.algebraic, promo)
    }

    private fun turnMessage(turn: String, myColor: String) =
        if (turn == myColor) "Your turn" else "Opponent's turn"
}
