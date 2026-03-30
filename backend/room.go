package main

import (
	"math/rand"
	"sync"
	"time"
)

func init() {
	rand.Seed(time.Now().UnixNano())
}

const roomIDChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

func generateRoomID() string {
	b := make([]byte, 6)
	for i := range b {
		b[i] = roomIDChars[rand.Intn(len(roomIDChars))]
	}
	return string(b)
}

type Room struct {
	ID      string
	Players [2]*Client
	Board   Board
	mu      sync.Mutex
	Done    bool
}

func newRoom() *Room {
	return &Room{
		ID:    generateRoomID(),
		Board: NewBoard(),
	}
}

func (r *Room) addPlayer(c *Client) bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.Players[0] == nil {
		r.Players[0] = c
		c.color = "white"
		c.room = r
		return true
	}
	if r.Players[1] == nil {
		r.Players[1] = c
		c.color = "black"
		c.room = r
		return true
	}
	return false
}

func (r *Room) isFull() bool {
	r.mu.Lock()
	defer r.mu.Unlock()
	return r.Players[0] != nil && r.Players[1] != nil
}

func (r *Room) broadcast(msg OutboundMessage) {
	r.mu.Lock()
	defer r.mu.Unlock()
	for _, p := range r.Players {
		if p != nil {
			p.SendMsg(msg)
		}
	}
}

func (r *Room) opponent(c *Client) *Client {
	r.mu.Lock()
	defer r.mu.Unlock()
	if r.Players[0] == c {
		return r.Players[1]
	}
	return r.Players[0]
}

func (r *Room) removePlayer(c *Client) {
	r.mu.Lock()
	defer r.mu.Unlock()
	r.Done = true
	for i, p := range r.Players {
		if p == c {
			r.Players[i] = nil
		}
	}
	for _, p := range r.Players {
		if p != nil {
			p.SendMsg(OutboundMessage{
				Type:    MsgOpponentDisconnected,
				Payload: nil,
			})
		}
	}
}

func (r *Room) statePayload() GameStatePayload {
	return GameStatePayload{
		Board:         r.Board.Squares,
		Turn:          r.Board.Turn,
		IsCheck:       r.Board.isInCheck(r.Board.Turn),
		CapturedWhite: r.Board.CapturedByWhite,
		CapturedBlack: r.Board.CapturedByBlack,
	}
}
