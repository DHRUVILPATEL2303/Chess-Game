package main

import (
	"log"
	"net/http"
	"sync"

	"github.com/gorilla/websocket"
)

type clientMessage struct {
	client *Client
	msg    InboundMessage
}

type Hub struct {
	clients    map[*Client]bool
	rooms      map[string]*Room
	incoming   chan clientMessage
	unregister chan *Client
	mu         sync.RWMutex
}

func newHub() *Hub {
	return &Hub{
		clients:    make(map[*Client]bool),
		rooms:      make(map[string]*Room),
		incoming:   make(chan clientMessage, 256),
		unregister: make(chan *Client, 64),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case cm := <-h.incoming:
			h.handleMessage(cm.client, cm.msg)
		case client := <-h.unregister:
			h.handleDisconnect(client)
		}
	}
}

func (h *Hub) handleMessage(c *Client, msg InboundMessage) {
	switch msg.Type {

	case MsgCreateRoom:
		room := newRoom()
		room.addPlayer(c)
		h.mu.Lock()
		h.rooms[room.ID] = room
		h.mu.Unlock()

		c.SendMsg(OutboundMessage{
			Type: MsgRoomCreated,
			Payload: RoomCreatedPayload{
				RoomID: room.ID,
				Color:  "white",
			},
		})
		log.Printf("[room %s] created by white", room.ID)

	case MsgJoinRoom:
		h.mu.RLock()
		room, exists := h.rooms[msg.RoomID]
		h.mu.RUnlock()

		if !exists {
			c.SendMsg(OutboundMessage{
				Type:    MsgError,
				Payload: ErrorPayload{Message: "room not found"},
			})
			return
		}
		if room.isFull() {
			c.SendMsg(OutboundMessage{
				Type:    MsgError,
				Payload: ErrorPayload{Message: "room is full"},
			})
			return
		}
		if room.Done {
			c.SendMsg(OutboundMessage{
				Type:    MsgError,
				Payload: ErrorPayload{Message: "game already finished"},
			})
			return
		}

		room.addPlayer(c)

		c.SendMsg(OutboundMessage{
			Type:    MsgRoomJoined,
			Payload: RoomJoinedPayload{Color: "black"},
		})

		startPayload := GameStartedPayload{
			Board: room.Board.Squares,
			Turn:  room.Board.Turn,
			White: "Player 1",
			Black: "Player 2",
		}
		room.broadcast(OutboundMessage{
			Type:    MsgGameStarted,
			Payload: startPayload,
		})
		log.Printf("[room %s] game started", room.ID)

	case MsgMakeMove:
		room := c.room
		if room == nil {
			c.SendMsg(OutboundMessage{
				Type:    MsgError,
				Payload: ErrorPayload{Message: "not in a room"},
			})
			return
		}
		if room.Done {
			return
		}

		room.mu.Lock()
		result := room.Board.ApplyMove(msg.From, msg.To, msg.Promo)
		if !result.Valid {
			room.mu.Unlock()
			c.SendMsg(OutboundMessage{
				Type:    MsgInvalidMove,
				Payload: InvalidMovePayload{Reason: result.Reason},
			})
			return
		}
		room.Board = result.Board
		statePayload := GameStatePayload{
			Board:         room.Board.Squares,
			Turn:          room.Board.Turn,
			LastMove:      &LastMove{From: msg.From, To: msg.To},
			IsCheck:       result.IsCheck,
			CapturedWhite: room.Board.CapturedByWhite,
			CapturedBlack: room.Board.CapturedByBlack,
		}
		room.mu.Unlock()

		room.broadcast(OutboundMessage{
			Type:    MsgGameState,
			Payload: statePayload,
		})

		if result.IsCheckmate {
			room.Done = true
			room.broadcast(OutboundMessage{
				Type: MsgGameOver,
				Payload: GameOverPayload{
					Result: "checkmate",
					Winner: c.color,
				},
			})
			log.Printf("[room %s] checkmate – %s wins", room.ID, c.color)
		} else if result.IsStalemate {
			room.Done = true
			room.broadcast(OutboundMessage{
				Type: MsgGameOver,
				Payload: GameOverPayload{
					Result: "stalemate",
					Winner: "",
				},
			})
			log.Printf("[room %s] stalemate – draw", room.ID)
		}

	case MsgResign:
		room := c.room
		if room == nil || room.Done {
			return
		}
		room.Done = true
		winner := "black"
		if c.color == "black" {
			winner = "white"
		}
		room.broadcast(OutboundMessage{
			Type: MsgGameOver,
			Payload: GameOverPayload{
				Result: "resignation",
				Winner: winner,
			},
		})
		log.Printf("[room %s] %s resigned", room.ID, c.color)

	default:
		log.Printf("unknown message type: %s", msg.Type)
	}
}

func (h *Hub) handleDisconnect(c *Client) {
	h.mu.Lock()
	delete(h.clients, c)
	h.mu.Unlock()
	close(c.send)

	if c.room != nil && !c.room.Done {
		c.room.removePlayer(c)
		go func() {
			h.mu.Lock()
			delete(h.rooms, c.room.ID)
			h.mu.Unlock()
		}()
	}
	log.Printf("client disconnected (%s)", c.color)
}

var upgrader = websocket.Upgrader{
	ReadBufferSize:  1024,
	WriteBufferSize: 1024,
	CheckOrigin:     func(r *http.Request) bool { return true },
}

func (h *Hub) ServeWS(w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("upgrade error: %v", err)
		return
	}
	client := newClient(conn, h)

	h.mu.Lock()
	h.clients[client] = true
	h.mu.Unlock()

	log.Printf("new client connected")

	go client.WritePump()
	go client.ReadPump()
}
