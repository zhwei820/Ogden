package com.example.ogdenkids

import kotlin.math.abs
import kotlin.random.Random

/**
 * 对战 AI 档位。[noise] 是给根节点每步分数加的随机扰动（单位：兵=100），
 * 用来模拟「看得到但偶尔走错」；[quiesce] 关掉时叶子节点不算吃子交换，会贪吃被保护的子。
 */
enum class ChessBot(
    val title: String,
    val emoji: String,
    val desc: String,
    val depth: Int,
    val noise: Int,
    val quiesce: Boolean
) {
    Kitten("小猫咪", "🐱", "只看一步，会贪吃", 1, 150, false),
    Puppy("小狗", "🐶", "看两步，偶尔犯糊涂", 2, 60, false),
    Fox("狐狸", "🦊", "会算吃子交换", 2, 20, true),
    Owl("猫头鹰", "🦉", "看三步，很少送子", 3, 8, true),
    Lion("狮子", "🦁", "看四步，最厉害", 4, 0, true)
}

class ChessAi(private val bot: ChessBot, private val random: Random = Random.Default) {
    private var deadline = 0L
    private var aborted = false
    private var nodes = 0

    fun choose(pos: ChessPosition, timeLimitMs: Long = 3000): ChessMove? {
        val moves = pos.legalMoves()
        if (moves.isEmpty()) return null
        deadline = System.currentTimeMillis() + timeLimitMs
        aborted = false
        val margin = bot.noise * 2
        var scored: List<Pair<ChessMove, Int>> = moves.map { it to 0 }
        var ordered = order(pos, moves)
        for (depth in 1..bot.depth) {
            val result = ArrayList<Pair<ChessMove, Int>>(ordered.size)
            var best = -INF
            for (m in ordered) {
                // 比当前最好分低 2*noise 以上的步，加了扰动也不可能被选中，只需要上界
                val alpha = if (best == -INF) -INF else best - margin
                val s = -search(pos.play(m), depth - 1, -INF, -alpha, 1)
                if (aborted) break
                result.add(m to s)
                if (s > best) best = s
            }
            if (aborted) break
            scored = result
            ordered = result.sortedByDescending { it.second }.map { it.first }
        }
        return scored.maxByOrNull { (_, s) ->
            s + if (bot.noise > 0 && abs(s) < MATE / 2) random.nextInt(-bot.noise, bot.noise + 1) else 0
        }?.first
    }

    private fun search(pos: ChessPosition, depth: Int, alpha: Int, beta: Int, ply: Int): Int {
        if ((++nodes and 511) == 0 && System.currentTimeMillis() > deadline) aborted = true
        if (aborted) return 0
        if (pos.halfmove >= 100) return 0
        if (depth <= 0) return if (bot.quiesce) quiesce(pos, alpha, beta, 0) else evaluate(pos)
        val moves = pos.legalMoves()
        if (moves.isEmpty()) return if (pos.inCheck()) -MATE + ply else 0
        var a = alpha
        var best = -INF
        for (m in order(pos, moves)) {
            val s = -search(pos.play(m), depth - 1, -beta, -a, ply + 1)
            if (s > best) best = s
            if (s > a) a = s
            if (a >= beta) break
        }
        return best
    }

    private fun quiesce(pos: ChessPosition, alpha: Int, beta: Int, qd: Int): Int {
        val stand = evaluate(pos)
        if (stand >= beta || qd >= 6) return stand
        var a = maxOf(alpha, stand)
        val captures = pos.legalMoves().filter { pos[it.to] != 0 || it.promotion != 0 }
        var best = stand
        for (m in order(pos, captures)) {
            val s = -quiesce(pos.play(m), -beta, -a, qd + 1)
            if (s > best) best = s
            if (s > a) a = s
            if (a >= beta) break
        }
        return best
    }

    private fun order(pos: ChessPosition, moves: List<ChessMove>): List<ChessMove> =
        moves.sortedByDescending { m ->
            val victim = abs(pos[m.to])
            (if (victim != 0) VALUES[victim] * 10 - VALUES[abs(pos[m.from])] else 0) + if (m.promotion != 0) VALUES[m.promotion] * 10 else 0
        }

    companion object {
        const val INF = 1_000_000
        const val MATE = 100_000
        private val VALUES = intArrayOf(0, 100, 320, 330, 500, 900, 2000)

        /** 以行棋方视角的静态评估 */
        fun evaluate(pos: ChessPosition): Int {
            var heavy = 0
            for (p in pos.board) if (abs(p) in Pc.KNIGHT..Pc.QUEEN) heavy += VALUES[abs(p)]
            val endgame = heavy <= 2600
            var score = 0
            for (sq in 0 until 64) {
                val p = pos.board[sq]
                if (p == 0) continue
                val white = p > 0
                val type = abs(p)
                val f = sq % 8
                val r = sq / 8
                val adv = if (white) r else 7 - r
                val center = (7 - abs(2 * f - 7) + 7 - abs(2 * r - 7)) / 2 // 0..7
                val v = VALUES[type] + when (type) {
                    Pc.PAWN -> adv * adv * 2 + if (f in 3..4 && adv >= 2) 12 else 0
                    Pc.KNIGHT -> (center - 4) * 8
                    Pc.BISHOP -> (center - 4) * 4
                    Pc.ROOK -> if (adv == 6) 20 else 0
                    Pc.QUEEN -> (center - 4) * 2
                    Pc.KING -> if (endgame) (center - 4) * 8 else -adv * 15 + if (adv == 0 && f in setOf(1, 2, 6)) 15 else 0
                    else -> 0
                }
                score += if (white) v else -v
            }
            return if (pos.whiteToMove) score else -score
        }
    }
}
