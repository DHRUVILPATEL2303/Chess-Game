package com.example.chess_app.engine

import com.example.chess_app.model.Square

object ChessEngine {

    fun isWhite(piece: String) = piece != "." && piece == piece.uppercase()
    fun isBlack(piece: String) = piece != "." && piece == piece.lowercase()
    fun isEmpty(piece: String) = piece == "."
    fun colorOf(piece: String) = if (isWhite(piece)) "white" else "black"

    fun legalMovesFor(board: Array<Array<String>>, from: Square, currentTurn: String): List<Square> {
        val piece = board[from.row][from.col]
        if (isEmpty(piece)) return emptyList()
        if (colorOf(piece) != currentTurn) return emptyList()

        val pseudo = pseudoMoves(board, from)
        return pseudo.filter { to ->
            val nb = applyMove(board, from, to)
            !isInCheck(nb, currentTurn)
        }
    }

    private fun isInCheck(board: Array<Array<String>>, color: String): Boolean {
        val kingPiece = if (color == "white") "K" else "k"
        var kingPos: Square? = null
        for (r in 0..7) for (c in 0..7) {
            if (board[r][c] == kingPiece) { kingPos = Square(r, c); break }
        }
        val king = kingPos ?: return false
        val enemy = if (color == "white") "black" else "white"
        for (r in 0..7) for (c in 0..7) {
            val p = board[r][c]
            if (!isEmpty(p) && colorOf(p) == enemy) {
                if (attacks(board, Square(r, c), king)) return true
            }
        }
        return false
    }

    private fun attacks(board: Array<Array<String>>, from: Square, to: Square): Boolean {
        val piece = board[from.row][from.col]
        if (isEmpty(piece)) return false
        val color = colorOf(piece)
        val pt = piece.uppercase()
        val dr = to.row - from.row
        val dc = to.col - from.col

        return when (pt) {
            "P" -> {
                val dir = if (color == "white") 1 else -1
                dr == dir && (dc == 1 || dc == -1)
            }
            "N" -> (Math.abs(dr) == 2 && Math.abs(dc) == 1) || (Math.abs(dr) == 1 && Math.abs(dc) == 2)
            "B" -> Math.abs(dr) == Math.abs(dc) && dr != 0 && clearPath(board, from, to)
            "R" -> (dr == 0 || dc == 0) && (dr != 0 || dc != 0) && clearPath(board, from, to)
            "Q" -> ((Math.abs(dr) == Math.abs(dc) && dr != 0) ||
                    ((dr == 0 || dc == 0) && (dr != 0 || dc != 0))) && clearPath(board, from, to)
            "K" -> Math.abs(dr) <= 1 && Math.abs(dc) <= 1 && (dr != 0 || dc != 0)
            else -> false
        }
    }

    private fun clearPath(board: Array<Array<String>>, from: Square, to: Square): Boolean {
        val dr = Integer.signum(to.row - from.row)
        val dc = Integer.signum(to.col - from.col)
        var cur = Square(from.row + dr, from.col + dc)
        while (cur != to) {
            if (board[cur.row][cur.col] != ".") return false
            cur = Square(cur.row + dr, cur.col + dc)
        }
        return true
    }

    private fun pseudoMoves(board: Array<Array<String>>, from: Square): List<Square> {
        val piece = board[from.row][from.col]
        val color = colorOf(piece)
        val pt = piece.uppercase()
        val targets = mutableListOf<Square>()

        fun add(r: Int, c: Int): Boolean {
            if (r !in 0..7 || c !in 0..7) return false
            val t = board[r][c]
            if (t == ".") { targets += Square(r, c); return true }
            if (colorOf(t) != color) targets += Square(r, c)
            return false
        }

        fun slide(drs: IntArray, dcs: IntArray) {
            for (i in drs.indices) {
                var r = from.row + drs[i]; var c = from.col + dcs[i]
                while (r in 0..7 && c in 0..7) {
                    val t = board[r][c]
                    if (t == ".") { targets += Square(r, c); r += drs[i]; c += dcs[i] }
                    else { if (colorOf(t) != color) targets += Square(r, c); break }
                }
            }
        }

        when (pt) {
            "P" -> {
                val dir = if (color == "white") 1 else -1
                val start = if (color == "white") 1 else 6
                val fr = from.row; val fc = from.col
                if (fr + dir in 0..7 && board[fr + dir][fc] == ".") {
                    targets += Square(fr + dir, fc)
                    if (fr == start && board[fr + 2 * dir][fc] == ".") targets += Square(fr + 2 * dir, fc)
                }
                for (dc in intArrayOf(-1, 1)) {
                    val cr = fr + dir; val cc = fc + dc
                    if (cr in 0..7 && cc in 0..7) {
                        val t = board[cr][cc]
                        if (t != "." && colorOf(t) != color) targets += Square(cr, cc)
                    }
                }
            }
            "N" -> {
                for ((dr, dc) in listOf(-2 to -1, -2 to 1, -1 to -2, -1 to 2,
                    1 to -2, 1 to 2, 2 to -1, 2 to 1)) add(from.row + dr, from.col + dc)
            }
            "B" -> slide(intArrayOf(1,1,-1,-1), intArrayOf(1,-1,1,-1))
            "R" -> slide(intArrayOf(1,-1,0,0), intArrayOf(0,0,1,-1))
            "Q" -> slide(intArrayOf(1,1,-1,-1,1,-1,0,0), intArrayOf(1,-1,1,-1,0,0,1,-1))
            "K" -> {
                for ((dr, dc) in listOf(-1 to -1,-1 to 0,-1 to 1,0 to -1,0 to 1,1 to -1,1 to 0,1 to 1))
                    add(from.row + dr, from.col + dc)
            }
        }
        return targets
    }

    private fun applyMove(board: Array<Array<String>>, from: Square, to: Square): Array<Array<String>> {
        val nb = Array(8) { r -> Array(8) { c -> board[r][c] } }
        nb[to.row][to.col] = nb[from.row][from.col]
        nb[from.row][from.col] = "."
        return nb
    }

    fun pieceUnicode(piece: String): String = when (piece) {
        "K" -> "♔"; "Q" -> "♕"; "R" -> "♖"; "B" -> "♗"; "N" -> "♘"; "P" -> "♙"
        "k" -> "♚"; "q" -> "♛"; "r" -> "♜"; "b" -> "♝"; "n" -> "♞"; "p" -> "♟"
        else -> ""
    }

    fun pieceDisplayName(piece: String): String = when (piece.uppercase()) {
        "K" -> "King"; "Q" -> "Queen"; "R" -> "Rook"
        "B" -> "Bishop"; "N" -> "Knight"; "P" -> "Pawn"
        else -> ""
    }
}
