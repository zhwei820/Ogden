package com.example.ogdenkids

import kotlin.math.abs

object Pc {
    const val PAWN = 1
    const val KNIGHT = 2
    const val BISHOP = 3
    const val ROOK = 4
    const val QUEEN = 5
    const val KING = 6
}

fun sqName(sq: Int): String = "${'a' + sq % 8}${sq / 8 + 1}"
fun parseSq(s: String): Int = (s[1] - '1') * 8 + (s[0] - 'a')

data class ChessMove(val from: Int, val to: Int, val promotion: Int = 0) {
    fun uci(): String = sqName(from) + sqName(to) + if (promotion != 0) "nbrq"[promotion - 2].toString() else ""

    companion object {
        fun parse(uci: String): ChessMove = ChessMove(
            parseSq(uci.substring(0, 2)),
            parseSq(uci.substring(2, 4)),
            if (uci.length > 4) "nbrq".indexOf(uci[4]) + 2 else 0
        )
    }
}

/**
 * 不可变局面。board[rank*8+file]，rank 0 = 白方底线；正数白子、负数黑子，绝对值见 [Pc]。
 * castling 位：1=白短 2=白长 4=黑短 8=黑长。
 * 没有己方王的局面（入门课）跳过「走后不能被将」的合法性检查。
 */
class ChessPosition(
    val board: IntArray,
    val whiteToMove: Boolean,
    val castling: Int = 0,
    val ep: Int = -1,
    val halfmove: Int = 0,
    val fullmove: Int = 1
) {
    operator fun get(sq: Int) = board[sq]

    fun withSideToMove(white: Boolean) = ChessPosition(board, white, castling, -1, halfmove, fullmove)

    fun kingSquare(white: Boolean): Int = board.indexOf(if (white) Pc.KING else -Pc.KING)

    fun inCheck(): Boolean {
        val k = kingSquare(whiteToMove)
        return k >= 0 && isAttacked(k, !whiteToMove)
    }

    fun legalMoves(): List<ChessMove> {
        val us = whiteToMove
        if (kingSquare(us) < 0) return pseudoMoves()
        return pseudoMoves().filter { m ->
            val next = play(m)
            !next.isAttacked(next.kingSquare(us), !us)
        }
    }

    fun play(m: ChessMove): ChessPosition {
        val b = board.copyOf()
        val p = b[m.from]
        val sign = if (p > 0) 1 else -1
        val type = abs(p)
        val captured = b[m.to]
        b[m.to] = if (m.promotion != 0) sign * m.promotion else p
        b[m.from] = 0
        if (type == Pc.PAWN && m.to == ep && captured == 0) b[m.to - 8 * sign] = 0
        if (type == Pc.KING && abs(m.to - m.from) == 2) {
            if (m.to > m.from) {
                b[m.from + 1] = b[m.from + 3]; b[m.from + 3] = 0
            } else {
                b[m.from - 1] = b[m.from - 4]; b[m.from - 4] = 0
            }
        }
        val c = castling and castlingMask(m.from) and castlingMask(m.to)
        val newEp = if (type == Pc.PAWN && abs(m.to - m.from) == 16) (m.from + m.to) / 2 else -1
        val half = if (type == Pc.PAWN || captured != 0) 0 else halfmove + 1
        val full = if (!whiteToMove) fullmove + 1 else fullmove
        return ChessPosition(b, !whiteToMove, c, newEp, half, full)
    }

    fun isAttacked(sq: Int, byWhite: Boolean): Boolean {
        val s = if (byWhite) 1 else -1
        val f0 = sq % 8
        val r0 = sq / 8
        val pr = r0 - s
        if (pr in 0..7) {
            if (f0 > 0 && board[pr * 8 + f0 - 1] == s * Pc.PAWN) return true
            if (f0 < 7 && board[pr * 8 + f0 + 1] == s * Pc.PAWN) return true
        }
        for (d in KNIGHT_DIRS) if (pieceAt(f0 + d[0], r0 + d[1]) == s * Pc.KNIGHT) return true
        for (d in KING_DIRS) if (pieceAt(f0 + d[0], r0 + d[1]) == s * Pc.KING) return true
        for (d in ROOK_DIRS) {
            val q = firstPiece(f0, r0, d)
            if (q == s * Pc.ROOK || q == s * Pc.QUEEN) return true
        }
        for (d in BISHOP_DIRS) {
            val q = firstPiece(f0, r0, d)
            if (q == s * Pc.BISHOP || q == s * Pc.QUEEN) return true
        }
        return false
    }

    fun pseudoMoves(): List<ChessMove> {
        val out = ArrayList<ChessMove>(48)
        val sign = if (whiteToMove) 1 else -1
        for (sq in 0 until 64) {
            val p = board[sq]
            if (p * sign <= 0) continue
            when (abs(p)) {
                Pc.PAWN -> pawnMoves(sq, sign, out)
                Pc.KNIGHT -> steps(sq, sign, KNIGHT_DIRS, false, out)
                Pc.BISHOP -> steps(sq, sign, BISHOP_DIRS, true, out)
                Pc.ROOK -> steps(sq, sign, ROOK_DIRS, true, out)
                Pc.QUEEN -> {
                    steps(sq, sign, ROOK_DIRS, true, out)
                    steps(sq, sign, BISHOP_DIRS, true, out)
                }
                Pc.KING -> {
                    steps(sq, sign, KING_DIRS, false, out)
                    castleMoves(sq, sign, out)
                }
            }
        }
        return out
    }

    /** 判断用于重复局面的键（不含步数计数） */
    fun key(): String = board.joinToString(",") + "|$whiteToMove|$castling|$ep"

    fun insufficientMaterial(): Boolean {
        var minors = 0
        for (p in board) when (abs(p)) {
            Pc.PAWN, Pc.ROOK, Pc.QUEEN -> return false
            Pc.KNIGHT, Pc.BISHOP -> minors++
        }
        return minors <= 1
    }

    private fun pieceAt(f: Int, r: Int): Int = if (f in 0..7 && r in 0..7) board[r * 8 + f] else 0

    private fun firstPiece(f0: Int, r0: Int, d: IntArray): Int {
        var f = f0 + d[0]
        var r = r0 + d[1]
        while (f in 0..7 && r in 0..7) {
            val q = board[r * 8 + f]
            if (q != 0) return q
            f += d[0]; r += d[1]
        }
        return 0
    }

    private fun steps(sq: Int, sign: Int, dirs: Array<IntArray>, slide: Boolean, out: MutableList<ChessMove>) {
        val f0 = sq % 8
        val r0 = sq / 8
        for (d in dirs) {
            var f = f0 + d[0]
            var r = r0 + d[1]
            while (f in 0..7 && r in 0..7) {
                val t = r * 8 + f
                val q = board[t]
                if (q * sign > 0) break
                out.add(ChessMove(sq, t))
                if (q != 0 || !slide) break
                f += d[0]; r += d[1]
            }
        }
    }

    private fun pawnMoves(sq: Int, sign: Int, out: MutableList<ChessMove>) {
        val f = sq % 8
        val r = sq / 8
        val startRank = if (sign > 0) 1 else 6
        val nr = r + sign
        if (nr !in 0..7) return
        val one = nr * 8 + f
        if (board[one] == 0) {
            addPawn(sq, one, out)
            val two = one + 8 * sign
            if (r == startRank && board[two] == 0) out.add(ChessMove(sq, two))
        }
        for (df in intArrayOf(-1, 1)) {
            val nf = f + df
            if (nf !in 0..7) continue
            val t = nr * 8 + nf
            if (board[t] * sign < 0 || (t == ep && board[t] == 0)) addPawn(sq, t, out)
        }
    }

    private fun addPawn(from: Int, to: Int, out: MutableList<ChessMove>) {
        if (to / 8 == 0 || to / 8 == 7) {
            for (promo in Pc.QUEEN downTo Pc.KNIGHT) out.add(ChessMove(from, to, promo))
        } else {
            out.add(ChessMove(from, to))
        }
    }

    private fun castleMoves(sq: Int, sign: Int, out: MutableList<ChessMove>) {
        val home = if (sign > 0) 4 else 60
        if (sq != home) return
        val short = if (sign > 0) 1 else 4
        val long = if (sign > 0) 2 else 8
        val enemy = sign < 0
        val rook = sign * Pc.ROOK
        if (castling and short != 0 && board[home + 1] == 0 && board[home + 2] == 0 && board[home + 3] == rook &&
            !isAttacked(home, enemy) && !isAttacked(home + 1, enemy) && !isAttacked(home + 2, enemy)
        ) out.add(ChessMove(home, home + 2))
        if (castling and long != 0 && board[home - 1] == 0 && board[home - 2] == 0 && board[home - 3] == 0 && board[home - 4] == rook &&
            !isAttacked(home, enemy) && !isAttacked(home - 1, enemy) && !isAttacked(home - 2, enemy)
        ) out.add(ChessMove(home, home - 2))
    }

    companion object {
        const val START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        val KNIGHT_DIRS = arrayOf(
            intArrayOf(1, 2), intArrayOf(2, 1), intArrayOf(2, -1), intArrayOf(1, -2),
            intArrayOf(-1, -2), intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, 2)
        )
        val ROOK_DIRS = arrayOf(intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1))
        val BISHOP_DIRS = arrayOf(intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1))
        val KING_DIRS = ROOK_DIRS + BISHOP_DIRS

        private fun castlingMask(sq: Int): Int = when (sq) {
            0 -> 2.inv()
            7 -> 1.inv()
            4 -> 3.inv()
            56 -> 8.inv()
            63 -> 4.inv()
            60 -> 12.inv()
            else -> -1
        }

        fun pieceFromChar(c: Char): Int {
            val type = "pnbrqk".indexOf(c.lowercaseChar()) + 1
            return if (c.isUpperCase()) type else -type
        }

        fun fromFen(fen: String): ChessPosition {
            val parts = fen.trim().split(Regex("\\s+"))
            val board = IntArray(64)
            var rank = 7
            var file = 0
            for (c in parts[0]) {
                when {
                    c == '/' -> { rank--; file = 0 }
                    c.isDigit() -> file += c - '0'
                    else -> { board[rank * 8 + file] = pieceFromChar(c); file++ }
                }
            }
            var castling = 0
            parts.getOrElse(2) { "-" }.forEach {
                castling = castling or when (it) { 'K' -> 1; 'Q' -> 2; 'k' -> 4; 'q' -> 8; else -> 0 }
            }
            val ep = parts.getOrElse(3) { "-" }.let { if (it == "-") -1 else parseSq(it) }
            return ChessPosition(
                board,
                parts.getOrElse(1) { "w" } == "w",
                castling,
                ep,
                parts.getOrNull(4)?.toIntOrNull() ?: 0,
                parts.getOrNull(5)?.toIntOrNull() ?: 1
            )
        }

        /** 入门课用："Rd4 pe5" —— 大写白子、小写黑子，后跟格子名 */
        fun fromPieces(pieces: String, whiteToMove: Boolean = true): ChessPosition {
            val board = IntArray(64)
            pieces.split(' ').filter { it.isNotBlank() }.forEach { board[parseSq(it.substring(1))] = pieceFromChar(it[0]) }
            return ChessPosition(board, whiteToMove)
        }
    }
}

enum class ChessOutcome(val text: String) {
    WhiteMates("白方将杀，白方胜"),
    BlackMates("黑方将杀，黑方胜"),
    Stalemate("逼和：没有王被将，却无棋可走"),
    FiftyMove("50 回合没有吃子或动兵，和棋"),
    Repetition("同一局面出现三次，和棋"),
    Insufficient("双方子力都不够将杀，和棋")
}

/** 一盘棋的历史；[positions] 首元素为起始局面 */
class ChessGameRecord(start: ChessPosition) {
    val positions = mutableListOf(start)
    val moves = mutableListOf<ChessMove>()
    val current: ChessPosition get() = positions.last()

    fun play(m: ChessMove) {
        positions.add(current.play(m))
        moves.add(m)
    }

    fun undo(): Boolean {
        if (moves.isEmpty()) return false
        positions.removeAt(positions.lastIndex)
        moves.removeAt(moves.lastIndex)
        return true
    }

    fun outcome(): ChessOutcome? {
        val pos = current
        if (pos.legalMoves().isEmpty()) {
            return if (!pos.inCheck()) ChessOutcome.Stalemate
            else if (pos.whiteToMove) ChessOutcome.BlackMates else ChessOutcome.WhiteMates
        }
        if (pos.insufficientMaterial()) return ChessOutcome.Insufficient
        if (pos.halfmove >= 100) return ChessOutcome.FiftyMove
        val key = pos.key()
        if (positions.count { it.key() == key } >= 3) return ChessOutcome.Repetition
        return null
    }
}
