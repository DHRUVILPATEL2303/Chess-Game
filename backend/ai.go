package main

import (
	"strings"
)

type Move struct {
	From  Sq
	To    Sq
	Promo string
}

func getAllLegalMoves(b *Board, color string) []Move {
	var moves []Move
	for r := 0; r < 8; r++ {
		for c := 0; c < 8; c++ {
			from := Sq{r, c}
			p := b.get(from)
			if p != Empty && colorOf(p) == color {
				legal := b.legalMoves(from)
				for _, to := range legal {
					// Handle pawn promotion pseudo logic here for bot
					pt := strings.ToUpper(p)
					if pt == "P" && ((color == "white" && to.Row == 7) || (color == "black" && to.Row == 0)) {
						moves = append(moves, Move{from, to, "q"})
					} else {
						moves = append(moves, Move{from, to, "q"}) // Promo doesn't matter for non-pawns, "q" is safe.
					}
				}
			}
		}
	}
	return moves
}

func evaluateBoard(b *Board) int {
	score := 0
	for r := 0; r < 8; r++ {
		for c := 0; c < 8; c++ {
			p := b.get(Sq{r, c})
			if p != Empty {
				val := 0
				switch strings.ToUpper(p) {
				case "P":
					val = 10
				case "N", "B":
					val = 30
				case "R":
					val = 50
				case "Q":
					val = 90
				case "K":
					val = 900
				}
				if isWhite(p) {
					score -= val // White is min
				} else {
					score += val // Black is max (bot is playing black, we want to maximize bot score)
				}
			}
		}
	}
	return score
}

func minMax(b *Board, depth int, alpha, beta int, isMaximizing bool) int {
	color := "black"
	if !isMaximizing {
		color = "white"
	}

	moves := getAllLegalMoves(b, color)

	// Check for terminal states
	if len(moves) == 0 {
		if b.isInCheck(color) {
			if isMaximizing {
				return -9999 // Black is checkmated
			}
			return 9999 // White is checkmated
		}
		return 0 // Stalemate
	}

	if depth == 0 {
		return evaluateBoard(b)
	}

	if isMaximizing {
		maxEval := -10000
		for _, m := range moves {
			nb := b.applyUnchecked(m.From, m.To, m.Promo)
			eval := minMax(&nb, depth-1, alpha, beta, false)
			if eval > maxEval {
				maxEval = eval
			}
			if eval > alpha {
				alpha = eval
			}
			if beta <= alpha {
				break
			}
		}
		return maxEval
	} else {
		minEval := 10000
		for _, m := range moves {
			nb := b.applyUnchecked(m.From, m.To, m.Promo)
			eval := minMax(&nb, depth-1, alpha, beta, true)
			if eval < minEval {
				minEval = eval
			}
			if eval < beta {
				beta = eval
			}
			if beta <= alpha {
				break
			}
		}
		return minEval
	}
}

func getBestMove(b *Board, depth int) Move {
	moves := getAllLegalMoves(b, "black") // Bot is always black
	var bestMove Move
	bestEval := -10000

	alpha := -10000
	beta := 10000

	for _, m := range moves {
		nb := b.applyUnchecked(m.From, m.To, m.Promo)
		eval := minMax(&nb, depth-1, alpha, beta, false)
		if eval > bestEval {
			bestEval = eval
			bestMove = m
		}
		if eval > alpha {
			alpha = eval
		}
	}

	// Fallback to first move if something goes wrong
	if bestMove.From == bestMove.To && len(moves) > 0 {
		return moves[0]
	}

	return bestMove
}
