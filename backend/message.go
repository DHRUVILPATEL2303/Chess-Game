package main

const (
	MsgCreateRoom = "create_room"
	MsgJoinRoom   = "join_room"
	MsgMakeMove   = "make_move"
	MsgResign     = "resign"
	MsgOfferDraw  = "offer_draw"
	MsgAcceptDraw = "accept_draw"
)

const (
	MsgRoomCreated          = "room_created"
	MsgRoomJoined           = "room_joined"
	MsgGameStarted          = "game_started"
	MsgGameState            = "game_state"
	MsgInvalidMove          = "invalid_move"
	MsgGameOver             = "game_over"
	MsgOpponentDisconnected = "opponent_disconnected"
	MsgError                = "error"
	MsgDrawOffered          = "draw_offered"
	MsgDrawDeclined         = "draw_declined"
)

type InboundMessage struct {
	Type   string `json:"type"`
	RoomID string `json:"room_id,omitempty"`
	From   string `json:"from,omitempty"`
	To     string `json:"to,omitempty"`
	Promo  string `json:"promo,omitempty"`
}

type OutboundMessage struct {
	Type    string      `json:"type"`
	Payload interface{} `json:"payload,omitempty"`
}

type RoomCreatedPayload struct {
	RoomID string `json:"room_id"`
	Color  string `json:"color"`
}

type RoomJoinedPayload struct {
	Color string `json:"color"`
}

type GameStartedPayload struct {
	Board [8][8]string `json:"board"`
	Turn  string       `json:"turn"`
	White string       `json:"white"`
	Black string       `json:"black"`
}

type LastMove struct {
	From string `json:"from"`
	To   string `json:"to"`
}

type GameStatePayload struct {
	Board         [8][8]string `json:"board"`
	Turn          string       `json:"turn"`
	LastMove      *LastMove    `json:"last_move,omitempty"`
	IsCheck       bool         `json:"is_check"`
	CapturedWhite []string     `json:"captured_white"`
	CapturedBlack []string     `json:"captured_black"`
}

type InvalidMovePayload struct {
	Reason string `json:"reason"`
}

type GameOverPayload struct {
	Result string `json:"result"`
	Winner string `json:"winner,omitempty"`
}

type ErrorPayload struct {
	Message string `json:"message"`
}
