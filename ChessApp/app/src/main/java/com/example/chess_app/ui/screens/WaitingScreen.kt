package com.example.chess_app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess_app.network.ChessWebSocketClient
import com.example.chess_app.network.WsEvent
import com.example.chess_app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun WaitingScreen(
    roomId: String,
    myColor: String,
    onGameStarted: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ChessWebSocketClient.events.collect { event ->
            if (event is WsEvent.GameStarted) onGameStarted()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BackgroundDark, SurfaceVariant))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (myColor == "white") "♔" else "♚",
                fontSize = 72.sp,
                modifier = Modifier.scale(scale).alpha(alpha),
                color = if (myColor == "white") WhitePiece else AccentGold
            )

            Text(
                "Room Created!",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )

            Text(
                "Share this code\nwith your friend",
                fontSize = 15.sp,
                color = OnSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, AccentGold, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("ROOM ID", fontSize = 12.sp, color = OnSurfaceVariant, letterSpacing = 3.sp)
                    Text(
                        roomId,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AccentGold,
                        letterSpacing = 6.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("room_id", roomId))
                                copied = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGoldLight),
                            border = BorderStroke(1.dp, AccentGoldLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (copied) "Copied!" else "Copy")
                        }
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Join my Chess game! Room ID: $roomId\nDownload the app and enter this code.")
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Room ID"))
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGoldLight),
                            border = BorderStroke(1.dp, AccentGoldLight),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share")
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = AccentGold,
                    strokeWidth = 2.dp
                )
                Text("Waiting for opponent…", color = OnSurfaceVariant, fontSize = 14.sp)
            }

            Text(
                "Playing as ${myColor.replaceFirstChar { it.uppercase() }}",
                fontSize = 13.sp,
                color = if (myColor == "white") WhitePiece else AccentGold
            )
        }
    }
}
