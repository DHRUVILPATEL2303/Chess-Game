package com.example.chess_app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess_app.engine.ChessEngine
import com.example.chess_app.model.Square
import com.example.chess_app.ui.theme.*

@Composable
fun ChessBoard(
    board: Array<Array<String>>,
    myColor: String,
    selectedSquare: Square?,
    legalMoves: List<Square>,
    lastMoveFrom: Square?,
    lastMoveTo: Square?,
    checkSquare: Square?,
    onSquareTap: (Square) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = if (myColor == "black") (0..7).toList() else (7 downTo 0).toList()
    val cols = if (myColor == "black") (7 downTo 0).toList() else (0..7).toList()

    BoxWithConstraints(modifier = modifier.aspectRatio(1f)) {
        val squareSize: Dp = maxWidth / 8

        Column(modifier = Modifier.fillMaxSize()) {
            rows.forEach { row ->
                Row(modifier = Modifier.height(squareSize)) {
                    cols.forEach { col ->
                        val sq = Square(row, col)
                        val piece = board[row][col]
                        val isLight = (row + col) % 2 == 0

                        val bg = when {
                            selectedSquare == sq         -> BoardSelected
                            lastMoveFrom == sq || lastMoveTo == sq -> BoardLastMove
                            checkSquare == sq            -> BoardCheck
                            isLight                      -> BoardLight
                            else                         -> BoardDark
                        }
                        val isLegal = legalMoves.contains(sq)

                        Box(
                            modifier = Modifier
                                .size(squareSize)
                                .background(bg)
                                .clickable { onSquareTap(sq) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLegal) {
                                if (piece != ".") {
                                    Box(
                                        modifier = Modifier
                                            .size(squareSize)
                                            .border(3.dp, BoardHighlight)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(squareSize * 0.32f)
                                            .clip(CircleShape)
                                            .background(BoardHighlight)
                                    )
                                }
                            }

                            if (piece != ".") {
                                val isWhitePiece = ChessEngine.isWhite(piece)
                                Text(
                                    text = ChessEngine.pieceUnicode(piece),
                                    fontSize = (squareSize.value * 0.72f).sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isWhitePiece) WhitePiece else BlackPiece,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxSize()
                                        .wrapContentSize(Alignment.Center)
                                )
                            }

                            if (col == cols.first()) {
                                Text(
                                    text = "${row + 1}",
                                    fontSize = 8.sp,
                                    color = if (isLight) BoardDark else BoardLight,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 1.dp, top = 1.dp)
                                )
                            }
                            if (row == rows.last()) {
                                Text(
                                    text = "${'a' + col}",
                                    fontSize = 8.sp,
                                    color = if (isLight) BoardDark else BoardLight,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 1.dp, bottom = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
