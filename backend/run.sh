#!/bin/bash
# Chess Game Backend - Setup & Run Script
# Run this script to download dependencies and start the server

set -e

echo "======================================"
echo "  Chess Multiplayer Server Setup"
echo "======================================"

# Check Go is installed
if ! command -v go &> /dev/null; then
    echo "Error: Go is not installed."
    echo "Install from: https://go.dev/dl/"
    exit 1
fi

echo "Go version: $(go version)"
echo ""

# Download dependencies
echo "Downloading dependencies..."
go mod download
go mod tidy

echo ""
echo "Starting Chess WebSocket server on :8080 ..."
echo "WebSocket endpoint: ws://localhost:8080/ws"
echo ""
echo "For Android Emulator: ws://10.0.2.2:8080/ws"
echo "For Real Device: ws://<YOUR_LAN_IP>:8080/ws"
echo ""
echo "Press Ctrl+C to stop"
echo "--------------------------------------"

go run .
