package com.example.chess_app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chess_app.network.ChessWebSocketClient
import com.example.chess_app.network.WsEvent
import com.example.chess_app.util.Constants
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class HomeNavEvent {
    data class GoToWaiting(val roomId: String, val color: String) : HomeNavEvent()
    data class GoToGame(val color: String) : HomeNavEvent()
}

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navEvent = MutableSharedFlow<HomeNavEvent>()
    val navEvent: SharedFlow<HomeNavEvent> = _navEvent.asSharedFlow()

    init {
        collectEvents()
    }

    private fun collectEvents() {
        viewModelScope.launch {
            ChessWebSocketClient.events.collect { event ->
                when (event) {
                    is WsEvent.RoomCreated -> {
                        _uiState.value = HomeUiState(isLoading = false)
                        _navEvent.emit(HomeNavEvent.GoToWaiting(event.roomId, event.color))
                    }
                    is WsEvent.RoomJoined -> {
                        _uiState.value = HomeUiState(isLoading = false)
                        _navEvent.emit(HomeNavEvent.GoToGame(event.color))
                    }
                    is WsEvent.ServerError -> {
                        _uiState.value = HomeUiState(isLoading = false, error = event.message)
                    }
                    is WsEvent.ConnectionError -> {
                        _uiState.value = HomeUiState(isLoading = false, error = "Connection failed: ${event.message}")
                    }
                    else -> {}
                }
            }
        }
    }

    fun createRoom() {
        _uiState.value = HomeUiState(isLoading = true)
        ensureConnected()
        ChessWebSocketClient.sendCreateRoom()
    }

    fun joinRoom(roomId: String) {
        if (roomId.isBlank()) {
            _uiState.value = HomeUiState(error = "Please enter a room ID")
            return
        }
        _uiState.value = HomeUiState(isLoading = true)
        ensureConnected()
        ChessWebSocketClient.sendJoinRoom(roomId.trim().uppercase())
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun ensureConnected() {
        ChessWebSocketClient.connect(Constants.SERVER_URL)
    }
}
