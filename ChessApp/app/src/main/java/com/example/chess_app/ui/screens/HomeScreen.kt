package com.example.chess_app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chess_app.R
import com.example.chess_app.ui.theme.*
import com.example.chess_app.viewmodel.HomeNavEvent
import com.example.chess_app.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    onGoToWaiting: (roomId: String, color: String) -> Unit,
    onGoToGame: (color: String) -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val state by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.navEvent.collect { event ->
            when (event) {
                is HomeNavEvent.GoToWaiting -> onGoToWaiting(event.roomId, event.color)
                is HomeNavEvent.GoToGame    -> onGoToGame(event.color)
            }
        }
    }

    var joinRoomId by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
//            .windowInsetsPadding(WindowInsets.safeDrawing)
            .background(
                Brush.verticalGradient(listOf(BackgroundDark, SurfaceVariant))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.app_logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(24.dp))
            )

            Text(
                text = "Chess",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AccentGold
            )
            Text(
                text = "Multiplayer",
                fontSize = 18.sp,
                color = OnSurfaceVariant,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.createRoom() },
                enabled = !state.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color(0xFF1A1A1A)),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFF1A1A1A),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text("Create Room", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = OnSurfaceVariant)
                Text("  OR  ", color = OnSurfaceVariant, fontSize = 13.sp)
                HorizontalDivider(modifier = Modifier.weight(1f), color = OnSurfaceVariant)
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SurfaceVariant, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Join Room", fontWeight = FontWeight.SemiBold, color = OnSurface, fontSize = 16.sp)
                    OutlinedTextField(
                        value = joinRoomId,
                        onValueChange = { joinRoomId = it.uppercase() },
                        placeholder = { Text("Enter Room ID", color = OnSurfaceVariant) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = OnSurfaceVariant,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { vm.joinRoom(joinRoomId) },
                        enabled = !state.isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant, contentColor = AccentGoldLight),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Join", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            AnimatedVisibility(visible = state.error != null) {
                state.error?.let { error ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(error, color = ErrorRed, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.clearError() }) {
                                Text("OK", color = ErrorRed)
                            }
                        }
                    }
                }
            }
        }
    }
}
