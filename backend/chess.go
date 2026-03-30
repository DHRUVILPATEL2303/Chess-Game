package main

import "strings"

const Empty = "."

func isWhite(p string) bool {
	if p == Empty {
		return false
	}
	return p == strings.ToUpper(p)
}

func isBlack(p string) bool {
	if p == Empty {
		return false
	}
	return p == strings.ToLower(p)
}

func isPiece(p string) bool { return p != Empty }

func colorOf(p string) string {
	if isWhite(p) {
		return "white"
	}
	return "black"
}

func enemy(color string) string {
	if color == "white" {
		return "black"
	}
	return "white"
}

type Sq struct{ Row, Col int }

func sqFromAlg(alg string) (Sq, bool) {
	if len(alg) != 2 {
		return Sq{}, false
	}
	col := int(alg[0] - 'a')
	row := int(alg[1] - '1')
	if col < 0 || col > 7 || row < 0 || row > 7 {
		return Sq{}, false
	}
	return Sq{row, col}, true
}

func sqToAlg(s Sq) string {
	return string(rune('a'+s.Col)) + string(rune('1'+s.Row))
}

func inBounds(s Sq) bool {
	return s.Row >= 0 && s.Row <= 7 && s.Col >= 0 && s.Col <= 7
}

type Board struct {
	Squares      [8][8]string
	Turn         string
	EnPassant    *Sq
	CastleRights [4]bool
	HalfMove     int
	FullMove     int

	CapturedByWhite []string
	CapturedByBlack []string
}

func NewBoard() Board {
	b := Board{Turn: "white", FullMove: 1,
		CastleRights: [4]bool{true, true, true, true}}
	b.Squares[0] = [8]string{"R", "N", "B", "Q", "K", "B", "N", "R"}
	for c := 0; c < 8; c++ {
		b.Squares[1][c] = "P"
	}
	for r := 2; r <= 5; r++ {
		for c := 0; c < 8; c++ {
			b.Squares[r][c] = Empty
		}
	}
	for c := 0; c < 8; c++ {
		b.Squares[6][c] = "p"
	}
	b.Squares[7] = [8]string{"r", "n", "b", "q", "k", "b", "n", "r"}
	return b
}

func (b *Board) get(s Sq) string    { return b.Squares[s.Row][s.Col] }
func (b *Board) set(s Sq, p string) { b.Squares[s.Row][s.Col] = p }

func (b *Board) pseudoMoves(from Sq) []Sq {
	piece := b.get(from)
	if piece == Empty {
		return nil
	}
	color := colorOf(piece)
	pt := strings.ToUpper(piece)
	var targets []Sq

	slide := func(drs, dcs []int) {
		for i := range drs {
			cur := Sq{from.Row + drs[i], from.Col + dcs[i]}
			for inBounds(cur) {
				t := b.get(cur)
				if t == Empty {
					targets = append(targets, cur)
				} else {
					if colorOf(t) != color {
						targets = append(targets, cur)
					}
					break
				}
				cur.Row += drs[i]
				cur.Col += dcs[i]
			}
		}
	}
	step := func(drs, dcs []int) {
		for i := range drs {
			cur := Sq{from.Row + drs[i], from.Col + dcs[i]}
			if inBounds(cur) {
				t := b.get(cur)
				if t == Empty || colorOf(t) != color {
					targets = append(targets, cur)
				}
			}
		}
	}

	switch pt {
	case "P":
		dir := 1
		startRow := 1
		if color == "black" {
			dir = -1
			startRow = 6
		}
		fwd := Sq{from.Row + dir, from.Col}
		if inBounds(fwd) && b.get(fwd) == Empty {
			targets = append(targets, fwd)
			fwd2 := Sq{from.Row + 2*dir, from.Col}
			if from.Row == startRow && b.get(fwd2) == Empty {
				targets = append(targets, fwd2)
			}
		}
		for _, dc := range []int{-1, 1} {
			cap := Sq{from.Row + dir, from.Col + dc}
			if inBounds(cap) {
				t := b.get(cap)
				if isPiece(t) && colorOf(t) != color {
					targets = append(targets, cap)
				}
				if b.EnPassant != nil && *b.EnPassant == cap {
					targets = append(targets, cap)
				}
			}
		}
	case "N":
		step([]int{-2, -2, -1, -1, 1, 1, 2, 2},
			[]int{-1, 1, -2, 2, -2, 2, -1, 1})
	case "B":
		slide([]int{1, 1, -1, -1}, []int{1, -1, 1, -1})
	case "R":
		slide([]int{1, -1, 0, 0}, []int{0, 0, 1, -1})
	case "Q":
		slide([]int{1, 1, -1, -1, 1, -1, 0, 0},
			[]int{1, -1, 1, -1, 0, 0, 1, -1})
	case "K":
		step([]int{1, 1, 1, 0, 0, -1, -1, -1},
			[]int{1, 0, -1, 1, -1, 1, 0, -1})
		if color == "white" {
			if b.CastleRights[0] &&
				b.get(Sq{0, 5}) == Empty && b.get(Sq{0, 6}) == Empty &&
				!b.isAttacked(Sq{0, 4}, "black") &&
				!b.isAttacked(Sq{0, 5}, "black") &&
				!b.isAttacked(Sq{0, 6}, "black") {
				targets = append(targets, Sq{0, 6})
			}
			if b.CastleRights[1] &&
				b.get(Sq{0, 3}) == Empty && b.get(Sq{0, 2}) == Empty && b.get(Sq{0, 1}) == Empty &&
				!b.isAttacked(Sq{0, 4}, "black") &&
				!b.isAttacked(Sq{0, 3}, "black") &&
				!b.isAttacked(Sq{0, 2}, "black") {
				targets = append(targets, Sq{0, 2})
			}
		} else {
			if b.CastleRights[2] &&
				b.get(Sq{7, 5}) == Empty && b.get(Sq{7, 6}) == Empty &&
				!b.isAttacked(Sq{7, 4}, "white") &&
				!b.isAttacked(Sq{7, 5}, "white") &&
				!b.isAttacked(Sq{7, 6}, "white") {
				targets = append(targets, Sq{7, 6})
			}
			if b.CastleRights[3] &&
				b.get(Sq{7, 3}) == Empty && b.get(Sq{7, 2}) == Empty && b.get(Sq{7, 1}) == Empty &&
				!b.isAttacked(Sq{7, 4}, "white") &&
				!b.isAttacked(Sq{7, 3}, "white") &&
				!b.isAttacked(Sq{7, 2}, "white") {
				targets = append(targets, Sq{7, 2})
			}
		}
	}
	return targets
}

func (b *Board) isAttacked(s Sq, byColor string) bool {
	for r := 0; r < 8; r++ {
		for c := 0; c < 8; c++ {
			from := Sq{r, c}
			piece := b.get(from)
			if piece == Empty || colorOf(piece) != byColor {
				continue
			}
			if b.attacks(from, s) {
				return true
			}
		}
	}
	return false
}

func (b *Board) attacks(from, to Sq) bool {
	piece := b.get(from)
	if piece == Empty {
		return false
	}
	color := colorOf(piece)
	pt := strings.ToUpper(piece)
	dr := to.Row - from.Row
	dc := to.Col - from.Col

	switch pt {
	case "P":
		dir := 1
		if color == "black" {
			dir = -1
		}
		return dr == dir && (dc == 1 || dc == -1)
	case "N":
		return (abs(dr) == 2 && abs(dc) == 1) || (abs(dr) == 1 && abs(dc) == 2)
	case "B":
		if abs(dr) != abs(dc) || dr == 0 {
			return false
		}
		return b.clearDiag(from, to)
	case "R":
		if dr != 0 && dc != 0 {
			return false
		}
		return b.clearLine(from, to)
	case "Q":
		if abs(dr) == abs(dc) && dr != 0 {
			return b.clearDiag(from, to)
		}
		if (dr == 0 || dc == 0) && (dr != 0 || dc != 0) {
			return b.clearLine(from, to)
		}
		return false
	case "K":
		return abs(dr) <= 1 && abs(dc) <= 1 && (dr != 0 || dc != 0)
	}
	return false
}

func (b *Board) clearLine(from, to Sq) bool {
	dr, dc := sign(to.Row-from.Row), sign(to.Col-from.Col)
	cur := Sq{from.Row + dr, from.Col + dc}
	for cur != to {
		if b.get(cur) != Empty {
			return false
		}
		cur.Row += dr
		cur.Col += dc
	}
	return true
}

func (b *Board) clearDiag(from, to Sq) bool { return b.clearLine(from, to) }

func (b *Board) legalMoves(from Sq) []Sq {
	piece := b.get(from)
	if piece == Empty {
		return nil
	}
	color := colorOf(piece)
	if color != b.Turn {
		return nil
	}
	var legal []Sq
	for _, to := range b.pseudoMoves(from) {
		nb := b.applyUnchecked(from, to, "q")
		if !nb.isInCheck(color) {
			legal = append(legal, to)
		}
	}
	return legal
}

func (b *Board) hasAnyLegalMove() bool {
	for r := 0; r < 8; r++ {
		for c := 0; c < 8; c++ {
			from := Sq{r, c}
			p := b.get(from)
			if p != Empty && colorOf(p) == b.Turn {
				if len(b.legalMoves(from)) > 0 {
					return true
				}
			}
		}
	}
	return false
}

func (b *Board) isInCheck(color string) bool {
	kingPiece := "K"
	if color == "black" {
		kingPiece = "k"
	}
	for r := 0; r < 8; r++ {
		for c := 0; c < 8; c++ {
			if b.Squares[r][c] == kingPiece {
				return b.isAttacked(Sq{r, c}, enemy(color))
			}
		}
	}
	return false
}

type MoveResult struct {
	Board       Board
	Valid       bool
	Reason      string
	IsCheck     bool
	IsCheckmate bool
	IsStalemate bool
}

func (b *Board) ApplyMove(fromAlg, toAlg, promoStr string) MoveResult {
	from, ok1 := sqFromAlg(fromAlg)
	to, ok2 := sqFromAlg(toAlg)
	if !ok1 || !ok2 {
		return MoveResult{Valid: false, Reason: "invalid square notation"}
	}
	piece := b.get(from)
	if piece == Empty {
		return MoveResult{Valid: false, Reason: "no piece on source square"}
	}
	if colorOf(piece) != b.Turn {
		return MoveResult{Valid: false, Reason: "not your turn"}
	}

	legal := b.legalMoves(from)
	found := false
	for _, sq := range legal {
		if sq == to {
			found = true
			break
		}
	}
	if !found {
		return MoveResult{Valid: false, Reason: "illegal move"}
	}

	promo := strings.ToLower(promoStr)
	if promo == "" || (promo != "q" && promo != "r" && promo != "b" && promo != "n") {
		promo = "q"
	}

	nb := b.applyUnchecked(from, to, promo)
	nextColor := nb.Turn
	inCheck := nb.isInCheck(nextColor)
	hasMove := nb.hasAnyLegalMove()

	return MoveResult{
		Board:       nb,
		Valid:       true,
		IsCheck:     inCheck,
		IsCheckmate: inCheck && !hasMove,
		IsStalemate: !inCheck && !hasMove,
	}
}

func (b *Board) applyUnchecked(from, to Sq, promo string) Board {
	nb := *b
	nb.CapturedByWhite = append([]string{}, b.CapturedByWhite...)
	nb.CapturedByBlack = append([]string{}, b.CapturedByBlack...)

	piece := nb.get(from)
	color := colorOf(piece)
	pt := strings.ToUpper(piece)
	captured := nb.get(to)

	if captured != Empty {
		if color == "white" {
			nb.CapturedByWhite = append(nb.CapturedByWhite, captured)
		} else {
			nb.CapturedByBlack = append(nb.CapturedByBlack, captured)
		}
	}

	nb.EnPassant = nil
	if pt == "P" {
		dir := 1
		if color == "black" {
			dir = -1
		}
		if abs(to.Row-from.Row) == 2 {
			ep := Sq{from.Row + dir, from.Col}
			nb.EnPassant = &ep
		}
		if b.EnPassant != nil && to == *b.EnPassant && from.Col != to.Col {
			capturePawn := Sq{from.Row, to.Col}
			pawnCap := nb.get(capturePawn)
			if color == "white" {
				nb.CapturedByWhite = append(nb.CapturedByWhite, pawnCap)
			} else {
				nb.CapturedByBlack = append(nb.CapturedByBlack, pawnCap)
			}
			nb.set(capturePawn, Empty)
		}
	}

	nb.set(to, piece)
	nb.set(from, Empty)

	if pt == "K" {
		dc := to.Col - from.Col
		if abs(dc) == 2 {
			if dc > 0 {
				nb.set(Sq{from.Row, 7}, Empty)
				nb.set(Sq{from.Row, 5}, rookOf(color))
			} else {
				nb.set(Sq{from.Row, 0}, Empty)
				nb.set(Sq{from.Row, 3}, rookOf(color))
			}
		}
		if color == "white" {
			nb.CastleRights[0] = false
			nb.CastleRights[1] = false
		} else {
			nb.CastleRights[2] = false
			nb.CastleRights[3] = false
		}
	}
	if pt == "R" {
		if from == (Sq{0, 7}) {
			nb.CastleRights[0] = false
		}
		if from == (Sq{0, 0}) {
			nb.CastleRights[1] = false
		}
		if from == (Sq{7, 7}) {
			nb.CastleRights[2] = false
		}
		if from == (Sq{7, 0}) {
			nb.CastleRights[3] = false
		}
	}
	if to == (Sq{0, 7}) {
		nb.CastleRights[0] = false
	}
	if to == (Sq{0, 0}) {
		nb.CastleRights[1] = false
	}
	if to == (Sq{7, 7}) {
		nb.CastleRights[2] = false
	}
	if to == (Sq{7, 0}) {
		nb.CastleRights[3] = false
	}

	if pt == "P" {
		backRank := 7
		if color == "black" {
			backRank = 0
		}
		if to.Row == backRank {
			promoPiece := strings.ToUpper(promo)
			if color == "black" {
				promoPiece = strings.ToLower(promo)
			}
			nb.set(to, promoPiece)
		}
	}

	if pt == "P" || captured != Empty {
		nb.HalfMove = 0
	} else {
		nb.HalfMove++
	}

	if color == "white" {
		nb.Turn = "black"
	} else {
		nb.Turn = "white"
		nb.FullMove++
	}

	return nb
}

func rookOf(color string) string {
	if color == "white" {
		return "R"
	}
	return "r"
}

func abs(x int) int {
	if x < 0 {
		return -x
	}
	return x
}
func sign(x int) int {
	if x > 0 {
		return 1
	}
	if x < 0 {
		return -1
	}
	return 0
}
