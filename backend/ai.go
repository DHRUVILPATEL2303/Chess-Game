package main

import (
	"sort"
	"strings"
)

var pawnEvalWhite = [8][8]int{
	{0,  0,  0,  0,  0,  0,  0,  0},
	{5, 10, 10,-20,-20, 10, 10,  5},
	{5, -5,-10,  0,  0,-10, -5,  5},
	{0,  0,  0, 20, 20,  0,  0,  0},
	{5,  5, 10, 25, 25, 10,  5,  5},
	{10, 10, 20, 30, 30, 20, 10, 10},
	{50, 50, 50, 50, 50, 50, 50, 50},
	{0,  0,  0,  0,  0,  0,  0,  0},
}

var knightEvalWhite = [8][8]int{
	{-50,-40,-30,-30,-30,-30,-40,-50},
	{-40,-20,  0,  5,  5,  0,-20,-40},
	{-30,  5, 10, 15, 15, 10,  5,-30},
	{-30,  0, 15, 20, 20, 15,  0,-30},
	{-30,  5, 15, 20, 20, 15,  5,-30},
	{-30,  0, 10, 15, 15, 10,  0,-30},
	{-40,-20,  0,  0,  0,  0,-20,-40},
	{-50,-40,-30,-30,-30,-30,-40,-50},
}

var bishopEvalWhite = [8][8]int{
	{-20,-10,-10,-10,-10,-10,-10,-20},
	{-10,  5,  0,  0,  0,  0,  5,-10},
	{-10, 10, 10, 10, 10, 10, 10,-10},
	{-10,  0, 10, 10, 10, 10,  0,-10},
	{-10,  5,  5, 10, 10,  5,  5,-10},
	{-10,  0,  5, 10, 10,  5,  0,-10},
	{-10,  0,  0,  0,  0,  0,  0,-10},
	{-20,-10,-10,-10,-10,-10,-10,-20},
}

var rookEvalWhite = [8][8]int{
	{ 0,  0,  0,  5,  5,  0,  0,  0},
	{-5,  0,  0,  0,  0,  0,  0, -5},
	{-5,  0,  0,  0,  0,  0,  0, -5},
	{-5,  0,  0,  0,  0,  0,  0, -5},
	{-5,  0,  0,  0,  0,  0,  0, -5},
	{-5,  0,  0,  0,  0,  0,  0, -5},
	{ 5, 10, 10, 10, 10, 10, 10,  5},
	{ 0,  0,  0,  0,  0,  0,  0,  0},
}

var queenEvalWhite = [8][8]int{
	{-20,-10,-10, -5, -5,-10,-10,-20},
	{-10,  0,  5,  0,  0,  0,  0,-10},
	{-10,  5,  5,  5,  5,  5,  0,-10},
	{  0,  0,  5,  5,  5,  5,  0, -5},
	{ -5,  0,  5,  5,  5,  5,  0, -5},
	{-10,  0,  5,  5,  5,  5,  0,-10},
	{-10,  0,  0,  0,  0,  0,  0,-10},
	{-20,-10,-10, -5, -5,-10,-10,-20},
}

var kingEvalWhite = [8][8]int{
	{ 20, 30, 10,  0,  0, 10, 30, 20},
	{ 20, 20,  0,  0,  0,  0, 20, 20},
	{-10,-20,-20,-20,-20,-20,-20,-10},
	{-20,-30,-30,-40,-40,-30,-30,-20},
	{-30,-40,-40,-50,-50,-40,-40,-30},
	{-30,-40,-40,-50,-50,-40,-40,-30},
	{-30,-40,-40,-50,-50,-40,-40,-30},
	{-30,-40,-40,-50,-50,-40,-40,-30},
}

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
				pstScore := 0
				pt := strings.ToUpper(p)
				isw := isWhite(p)
				
				// Calculate PST value based on color
				pstRow := r
				if !isw {
					pstRow = 7 - r // Flip for black
				}

				switch pt {
				case "P":
					val = 100
					pstScore = pawnEvalWhite[pstRow][c]
				case "N":
					val = 300
					pstScore = knightEvalWhite[pstRow][c]
				case "B":
					val = 300
					pstScore = bishopEvalWhite[pstRow][c]
				case "R":
					val = 500
					pstScore = rookEvalWhite[pstRow][c]
				case "Q":
					val = 900
					pstScore = queenEvalWhite[pstRow][c]
				case "K":
					val = 9000
					pstScore = kingEvalWhite[pstRow][c]
				}
				
				if isw {
					score -= (val + pstScore) // White is min
				} else {
					score += (val + pstScore) // Black is max (bot is playing black, we want to maximize bot score)
				}
			}
		}
	}
	return score
}

// Move ordering function to improve Alpha-Beta pruning
func orderMoves(moves []Move, b *Board) {
	moveScores := make([]int, len(moves))
	
	pieceValues := map[string]int{"P": 100, "N": 300, "B": 300, "R": 500, "Q": 900, "K": 9000}
	
	for i, m := range moves {
		score := 0
		movedPiece := b.get(m.From)
		targetPiece := b.get(m.To)
		
		// 1. Capture moves
		if targetPiece != Empty {
			// MVV-LVA (Most Valuable Victim - Least Valuable Attacker)
			victimValue := pieceValues[strings.ToUpper(targetPiece)]
			attackerValue := pieceValues[strings.ToUpper(movedPiece)]
			score += (10 * victimValue) - attackerValue
		}
		
		// 2. Promotion moves
		if m.Promo != "q" && strings.ToUpper(movedPiece) == "P" {
			score += 900
		}
		
		moveScores[i] = score
	}
	
	// Sort moves based on score (descending)
	sort.SliceStable(moves, func(i, j int) bool {
		return moveScores[i] > moveScores[j]
	})
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
				return -999999 // Black is checkmated
			}
			return 999999 // White is checkmated
		}
		return 0 // Stalemate
	}

	if depth == 0 {
		return evaluateBoard(b)
	}
	
	orderMoves(moves, b)

	if isMaximizing {
		maxEval := -999999
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
		minEval := 999999
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
	bestEval := -999999

	alpha := -999999
	beta := 999999

	orderMoves(moves, b)

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
