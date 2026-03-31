# DPKV Chess - Real-Time Multiplayer Chess Game ♟️

A modern, real-time multiplayer chess platform that brings players together. The project features a beautifully designed **Android frontend** built with Jetpack Compose and a high-performance **Go-based WebSocket backend** designed for minimal latency. 

---

## 📸 Overview

The DPKV Chess experience allows users to create custom game rooms and invite friends by sharing a unique `Room ID`. The game is fully real-time and updates instantly on both devices using an efficient WebSocket connection.

### ✨ Key Features

- **Real-Time Multiplayer**: Instantly syncs moves across devices with virtually zero latency using WebSockets.
- **Room-Based Matchmaking**: Easily create a room and share the 6-character room code, or join an existing room seamlessly.
- **Modern Android UI**: Built entirely with Jetpack Compose, featuring smooth animations, a custom dark theme, and visually rich elements.
- **High-Performance Backend**: The Go backend efficiently handles concurrent connections and room state management. 
- **Lightweight & Fast**: The game logic is optimized for fast load times and quick response rates.

---

## 🛠 Tech Stack

### Frontend (Android)
- **Framework**: Android SDK, Kotlin
- **UI Toolkit**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking/WebSockets**: OkHttp3, Coroutines
- **JSON Parsing**: Gson

### Backend (Go)
- **Language**: Go (Golang)
- **Architecture**: Custom WebSocket Hub-and-Spoke pattern
- **Deployment**: Configured for deployment on [Fly.io](https://fly.io/) (via `fly.toml` & Dockerfile)

---

## 🚀 Getting Started

Follow these instructions to run both the backend server and the Android client locally.

### 1. Running the Backend (Go)

Ensure you have [Go](https://go.dev/doc/install) installed on your machine.

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```
2. Download the required modules:
   ```bash
   go mod download
   ```
3. Start the WebSocket server:
   ```bash
   go run main.go
   ```
   > The server will start on `ws://localhost:8080/ws` by default.

### 2. Running the Android App

Ensure you have [Android Studio](https://developer.android.com/studio) installed.

1. Open Android Studio and select **Open an existing project**.
2. Navigate to the `ChessApp` folder and open it.
3. Once the Gradle sync is complete, make sure the backend server URL in your `WebSocketClient` or `ViewModel` is pointed to your local IP address (e.g., `ws://192.168.x.x:8080/ws`) rather than the production server if you are testing locally.
4. Build and run the project on an Android emulator or a physical device.

---

## 📂 Project Structure

```
Chess-Game/
├── ChessApp/               # Android Application Source Code
│   ├── app/src/main//...
│   │   ├── viewmodel/      # GameViewModel, HomeViewModel
│   │   ├── ui/screens/     # HomeScreen, GameScreen, WaitingScreen 
│   │   └── ui/theme/       # Material3 Custom Theming 
│   └── build.gradle.kts
│
└── backend/                # Go WebSocket Server
    ├── main.go             # Server Entry Point
    ├── hub.go              # WebSocket Connection Hub
    ├── room.go             # Room & Match Lifecycle
    ├── chess.go            # Game Logic integration
    └── fly.toml            # Deployment Configuration for Fly.io
```

---

## 💡 Future Enhancements

- **Move Validation**: Implement full server-side validation of chess piece moves.
- **Player Accounts & ELO Rating**: Firebase Authentication integration with global leaderboards.
- **Spectator Mode**: Allow friends to watch live matches using the room code.
- **In-Game Chat**: A simple chat system directly integrated into the `GameScreen`.

---

*Designed and Developed for seamless Mobile Chess gaming.*
