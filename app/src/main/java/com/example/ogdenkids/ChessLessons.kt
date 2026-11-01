package com.example.ogdenkids

import android.content.Context
import kotlin.math.abs

/** 收星关：只有白子在走，落到星星格或吃掉黑子算收集一个目标；黑子不动。 */
class StarLevel(val pieces: String, starSquares: String = "") {
    val stars: Set<Int> = starSquares.split(' ').filter { it.isNotBlank() }.map(::parseSq).toSet()
    val start: ChessPosition get() = ChessPosition.fromPieces(pieces)

    /** 收齐所有目标的最少步数（BFS；入门关只有一个白子，状态空间很小） */
    val par: Int by lazy {
        val startPos = start
        val enemies = (0 until 64).filter { startPos[it] < 0 }
        val targets = stars.toList() + enemies
        val full = (1 shl targets.size) - 1
        data class State(val sq: Int, val piece: Int, val mask: Int)
        val from = (0 until 64).first { startPos[it] > 0 }
        var frontier = listOf(State(from, startPos[from], 0))
        val seen = hashSetOf(frontier[0])
        var depth = 0
        while (frontier.isNotEmpty() && depth < 30) {
            depth++
            val next = ArrayList<State>()
            for (s in frontier) {
                val b = IntArray(64)
                b[s.sq] = s.piece
                enemies.forEachIndexed { i, e -> if (s.mask and (1 shl (stars.size + i)) == 0) b[e] = startPos[e] }
                val pos = ChessPosition(b, true)
                for (m in pos.legalMoves()) {
                    if (m.promotion != 0 && m.promotion != Pc.QUEEN) continue
                    val idx = targets.indexOf(m.to)
                    val mask = if (idx >= 0) s.mask or (1 shl idx) else s.mask
                    if (mask == full) return@lazy depth
                    val ns = State(m.to, if (m.promotion != 0) Pc.QUEEN else s.piece, mask)
                    if (seen.add(ns)) next.add(ns)
                }
            }
            frontier = next
        }
        depth
    }
}

class ChessLesson(val key: String, val title: String, val en: String, val glyph: Int, val intro: String, val levels: List<StarLevel>)

val ChessLessons = listOf(
    ChessLesson(
        "rook", "车", "rook", Pc.ROOK, "车走直线：横着、竖着都行，一次想走多远就走多远，但不能跳过别的棋子。",
        listOf(
            StarLevel("Ra1", "a6"),
            StarLevel("Ra1", "a6 f6"),
            StarLevel("Rd4", "d8 h8 h1"),
            StarLevel("Ra1", "c1 c5 g5 g8"),
            StarLevel("Ra1 pa5 ph5", ""),
            StarLevel("Rh1 ph4 pc4 pc7 pe7", "")
        )
    ),
    ChessLesson(
        "bishop", "象", "bishop", Pc.BISHOP, "象走斜线，一次可以走很多格。象永远待在同一种颜色的格子上。",
        listOf(
            StarLevel("Bc1", "f4"),
            StarLevel("Bc1", "a3 d6"),
            StarLevel("Bf1", "b5 e8 h5"),
            StarLevel("Bd4 pb6 pg7 pg1", ""),
            StarLevel("Bc1", "h6 f8 a3 c5")
        )
    ),
    ChessLesson(
        "queen", "后", "queen", Pc.QUEEN, "后是最强的棋子：车的直线 + 象的斜线，八个方向都能走。",
        listOf(
            StarLevel("Qd1", "d6 h2"),
            StarLevel("Qd4", "a7 g7 g1 a1"),
            StarLevel("Qa1 pb7 pe4 ph1 pf8", ""),
            StarLevel("Qe1 pe5", "e8 a4 h4")
        )
    ),
    ChessLesson(
        "knight", "马", "knight", Pc.KNIGHT, "马走「日」字：先直走两格再拐一格。马是唯一能跳过别的棋子的。",
        listOf(
            StarLevel("Nb1", "c3"),
            StarLevel("Nb1", "c3 e4"),
            StarLevel("Ng1", "f3 e5 d7"),
            StarLevel("Nd4 pc6 pe6 pf5", ""),
            StarLevel("Nb1", "h8")
        )
    ),
    ChessLesson(
        "king", "王", "king", Pc.KING, "王每次只能走一格，八个方向都可以。王最重要：被将死就输了！",
        listOf(
            StarLevel("Ke1", "e2"),
            StarLevel("Ke1", "d3 f3"),
            StarLevel("Ke1 pc3 pe5", "g4")
        )
    ),
    ChessLesson(
        "pawn", "兵", "pawn", Pc.PAWN, "兵只能向前走一格，第一步可以走两格；吃子要斜着向前吃。兵走到对面底线会变成后！",
        listOf(
            StarLevel("Pe2", "e4"),
            StarLevel("Pe2", "e3 e5"),
            StarLevel("Pc2 pb3 pc5", ""),
            StarLevel("Pa6", "a8 h8"),
            StarLevel("Pd2 pe4 pd6 pc7", "")
        )
    )
)

fun starsFor(moves: Int, par: Int): Int = when {
    moves <= par -> 3
    moves <= par + 2 -> 2
    else -> 1
}

/** 一道 Lichess 谜题：moves[0] 是对手先走的一步，之后玩家与对手交替 */
class ChessPuzzle(val id: String, val fen: String, val moves: List<String>, val rating: Int)

class PuzzleChapter(val key: String, val title: String, val desc: String)

val PuzzleChapters = listOf(
    PuzzleChapter("mate1", "一步将杀", "找到一步就将死对方的棋"),
    PuzzleChapter("hanging", "吃掉白送的子", "对方有没保护的子，吃掉它"),
    PuzzleChapter("fork", "捉双", "一个子同时攻击两个目标"),
    PuzzleChapter("pin", "牵制", "让对方的子不敢动"),
    PuzzleChapter("skewer", "串击", "打跑前面的大子，吃掉后面的"),
    PuzzleChapter("backrank", "底线杀", "王被自己的兵堵在底线"),
    PuzzleChapter("discovered", "闪击", "挪开一个子，露出后面的攻击"),
    PuzzleChapter("mate2", "两步将杀", "连续两步将死对方"),
    PuzzleChapter("pawnend", "兵残局", "让兵走到底线升变"),
    PuzzleChapter("mate3", "三步将杀", "连续三步将死对方")
)

object PuzzleBank {
    @Volatile private var cache: Map<String, List<ChessPuzzle>>? = null

    fun load(context: Context): Map<String, List<ChessPuzzle>> = cache ?: synchronized(this) {
        cache ?: context.assets.open("chess/puzzles.csv").bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.map { it.split(',') }
                .groupBy({ it[0] }, { ChessPuzzle(it[1], it[2], it[3].split(' '), it[4].toInt()) })
        }.also { cache = it }
    }
}

class ChessStore(context: Context, profileId: String) {
    private val prefs = context.getSharedPreferences(ProfileStore.progressPrefsName(profileId), Context.MODE_PRIVATE)

    fun lessonStars(key: String, level: Int): Int = prefs.getInt("chess.lesson.$key.$level", 0)
    fun saveLessonStars(key: String, level: Int, stars: Int) {
        if (stars > lessonStars(key, level)) prefs.edit().putInt("chess.lesson.$key.$level", stars).apply()
    }

    /** 下一道要做的题的序号（同时也是已做完的题数） */
    fun puzzleIndex(chapter: String): Int = prefs.getInt("chess.puzzle.$chapter", 0)
    fun savePuzzleIndex(chapter: String, index: Int) = prefs.edit().putInt("chess.puzzle.$chapter", index).apply()

    fun botWins(bot: ChessBot): Int = prefs.getInt("chess.wins.${bot.name}", 0)
    fun recordBotWin(bot: ChessBot) = prefs.edit().putInt("chess.wins.${bot.name}", botWins(bot) + 1).apply()
}

internal fun isPromotionMove(pos: ChessPosition, from: Int, to: Int): Boolean =
    abs(pos[from]) == Pc.PAWN && (to / 8 == 0 || to / 8 == 7)
