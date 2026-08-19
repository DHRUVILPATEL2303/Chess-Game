package main

import (
	"fmt"
	"log"
	"net/http"
	"os"
)

func main() {
	hub := newHub()
	go hub.Run()

	http.HandleFunc("/ws", hub.ServeWS)
	http.HandleFunc("/health", func(w http.ResponseWriter, r *http.Request) {
		fmt.Fprintln(w, "Chess server is running")
	})

	port := os.Getenv("PORT")
	if port == "" {
		port = "8080"
	}

	log.Printf("Chess WebSocket server webhook listening on :%s", port)
	log.Printf("WebSocket endpoint: ws://0.0.0.0:%s/ws", port)
	if err := http.ListenAndServe(":"+port, nil); err != nil {
		log.Fatalf("server error: %v", err)
	}
}
