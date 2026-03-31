package com.example.chess_app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess_app.engine.ChessEngine
import com.example.chess_app.model.Square
import com.example.chess_app.ui.components.ChessBoard
import com.example.chess_app.ui.theme.*
import com.example.chess_app.viewmodel.GameViewModel

@Composable
fun GameScreen(
    myColor: String,
    onNavigateHome: () -> Unit,
    vm: GameViewModel = viewModel()
) {
    LaunchedEffect(myColor) {
        vm.initColor(myColor)
    }

    val state by vm.uiState.collectAsStateWithLifecycle()

    val checkSquare: Square? = remember(state.board, state.isCheck, state.turn) {
        if (!state.isCheck) null
        else {
            val kingPiece = if (state.turn == "white") "K" else "k"
            var result: Square? = null
            outer@ for (r in 0..7) for (c in 0..7) {
                if (state.board[r][c] == kingPiece) { result = Square(r, c); break@outer }
            }
            result
        }
    }

    var showResignDialog by remember { mutableStateOf(false) }

    if (state.promotionPending != null) {
        PromotionDialog(myColor = myColor) { piece -> vm.onPromotionChosen(piece) }
    }

    if (state.isGameOver) {
        GameOverDialog(
            result = state.gameOverResult,
            winner = state.gameOverWinner,
            myColor = myColor,
            onHome = onNavigateHome
        )
    }

    if (state.opponentDisconnected) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Opponent Left", color = OnSurface) },
            text = { Text("Your opponent disconnected.", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = onNavigateHome) { Text("Go Home", color = AccentGold) }
            },
            containerColor = SurfaceDark
        )
    }

    if (showResignDialog) {
        AlertDialog(
            onDismissRequest = { showResignDialog = false },
            title = { Text("Resign?", color = OnSurface) },
            text = { Text("Are you sure you want to resign?", color = OnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = { vm.resign(); showResignDialog = false }) {
                    Text("Resign", color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResignDialog = false }) { Text("Cancel", color = AccentGold) }
            },
            containerColor = SurfaceDark
        )
    }

    Scaffold(
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
//            .windowInsetsPadding(WindowInsets.safeDrawing)
                .background(BackgroundDark)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                PlayerBar(
                    label = "Opponent",
                    color = if (myColor == "white") "black" else "white",
                    capturedPieces = state.capturedByOpponent,
                    isActive = state.turn != myColor && !state.isWaiting,
                    modifier = Modifier.fillMaxWidth()
                )

                AnimatedStatusBanner(message = state.statusMessage, isCheck = state.isCheck)

                ChessBoard(
                    board = state.board,
                    myColor = myColor,
                    selectedSquare = state.selectedSquare,
                    legalMoves = state.legalMoves,
                    lastMoveFrom = state.lastMoveFrom,
                    lastMoveTo = state.lastMoveTo,
                    checkSquare = checkSquare,
                    onSquareTap = { sq -> vm.onSquareTapped(sq) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                )

                PlayerBar(
                    label = "You (${myColor.replaceFirstChar { it.uppercase() }})",
                    color = myColor,
                    capturedPieces = state.capturedByMe,
                    isActive = state.turn == myColor && !state.isWaiting,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.weight(1f))

                if (!state.isGameOver && !state.isWaiting) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showResignDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = BorderStroke(1.dp, ErrorRed),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.SportsEsports, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Resign")
                        }
                    }
                }
            }
        }
    }


}

@Composable
private fun PlayerBar(
    label: String,
    color: String,
    capturedPieces: List<String>,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(if (isActive) SurfaceVariant else SurfaceDark)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    if (color == "white") Color(0xFFF5F5F5) else Color(0xFF333333),
                    RoundedCornerShape(50)
                )
                .border(1.dp, AccentGold, RoundedCornerShape(50))
        )
        Text(label, color = OnSurface, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

        if (isActive) {
            Text("●", color = SuccessGreen, fontSize = 10.sp)
        }

        Spacer(Modifier.weight(1f))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy((-4).dp),
            reverseLayout = true
        ) {
            items(capturedPieces) { piece ->
                Text(
                    ChessEngine.pieceUnicode(piece),
                    fontSize = 14.sp,
                    color = OnSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnimatedStatusBanner(message: String, isCheck: Boolean) {
    val bg = if (isCheck) ErrorRed.copy(alpha = 0.85f) else SurfaceDark
    val textColor = if (isCheck) Color.White else OnSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(message, color = textColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PromotionDialog(myColor: String, onChosen: (String) -> Unit) {
    val pieces = listOf("q" to "♛", "r" to "♜", "b" to "♝", "n" to "♞")
    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.border(2.dp, AccentGold, RoundedCornerShape(16.dp))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Promote Pawn", fontWeight = FontWeight.Bold, color = AccentGold, fontSize = 18.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pieces.forEach { (code, unicode) ->
                        val displayUnicode = if (myColor == "white") {
                            unicode.replace("♛","♕").replace("♜","♖").replace("♝","♗").replace("♞","♘")
                        } else unicode
                        OutlinedButton(
                            onClick = { onChosen(code) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGold),
                            border = BorderStroke(1.dp, AccentGold),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.size(56.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(displayUnicode, fontSize = 28.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameOverDialog(
    result: String,
    winner: String?,
    myColor: String,
    onHome: () -> Unit
) {
    val iWon = winner == myColor
    val isDraw = winner == null || winner.isEmpty()

    val emoji = when {
        isDraw -> "🤝"
        iWon   -> "🏆"
        else   -> "😔"
    }
    val headline = when {
        isDraw -> "It's a Draw!"
        iWon   -> "You Win!"
        else   -> "You Lose"
    }
    val subtitle = when (result) {
        "checkmate"   -> "by Checkmate"
        "stalemate"   -> "Stalemate"
        "resignation" -> if (iWon) "Opponent Resigned" else "You Resigned"
        "draw"        -> "Draw by Agreement"
        else -> result
    }

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.border(2.dp, if (iWon) AccentGold else ErrorRed.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(emoji, fontSize = 56.sp)
                Text(headline, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                    color = if (isDraw) AccentGold else if (iWon) AccentGold else ErrorRed)
                Text(subtitle, fontSize = 15.sp, color = OnSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onHome,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF1A1A1A)),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}
