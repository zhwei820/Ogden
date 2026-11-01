package com.example.ogdenkids

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
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

    /** 最近的对局，新的在前 */
    fun games(): List<SavedGame> = runCatching {
        val arr = JSONArray(prefs.getString("chess.games", "[]"))
        List(arr.length()) { SavedGame.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun saveGame(game: SavedGame) {
        val list = listOf(game) + games().filter { it.id != game.id }
        val arr = JSONArray()
        list.take(MaxSavedGames).forEach { arr.put(it.toJson()) }
        prefs.edit().putString("chess.games", arr.toString()).apply()
    }

    /** 做过的题，新的在前；同一道题只留最近一次 */
    fun puzzleLog(): List<PuzzleAttempt> = runCatching {
        val arr = JSONArray(prefs.getString("chess.puzzleLog", "[]"))
        List(arr.length()) { PuzzleAttempt.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    fun lastAttempt(puzzleId: String): PuzzleAttempt? = puzzleLog().firstOrNull { it.puzzleId == puzzleId }

    fun recordPuzzle(attempt: PuzzleAttempt) {
        val arr = JSONArray()
        (listOf(attempt) + puzzleLog().filter { it.puzzleId != attempt.puzzleId })
            .take(MaxPuzzleLog).forEach { arr.put(it.toJson()) }
        prefs.edit().putString("chess.puzzleLog", arr.toString()).apply()
    }

    private companion object {
        const val MaxSavedGames = 50
        const val MaxPuzzleLog = 500
    }
}

/** 一次做题结果。[mistakes] 是答错次数；[revealed] 表示最后看了答案 */
class PuzzleAttempt(
    val puzzleId: String,
    val chapter: String,
    val index: Int,
    val rating: Int,
    val mistakes: Int,
    val revealed: Boolean,
    val time: Long
) {
    val label: String
        get() = when {
            revealed -> "👀 看了答案"
            mistakes == 0 -> "✅ 一次做对"
            else -> "👍 错 $mistakes 次后做对"
        }

    fun toJson(): JSONObject = JSONObject()
        .put("id", puzzleId).put("chapter", chapter).put("index", index).put("rating", rating)
        .put("mistakes", mistakes).put("revealed", revealed).put("time", time)

    companion object {
        fun fromJson(o: JSONObject) = PuzzleAttempt(
            o.getString("id"), o.getString("chapter"), o.getInt("index"), o.getInt("rating"),
            o.getInt("mistakes"), o.getBoolean("revealed"), o.getLong("time")
        )
    }
}

/** 一盘和 AI 的对局；[outcome] 为 null 表示没下完就离开了 */
class SavedGame(
    val id: Long,
    val bot: ChessBot,
    val playerWhite: Boolean,
    val moves: List<String>,
    val outcome: ChessOutcome?
) {
    val playerWon: Boolean get() = outcome == if (playerWhite) ChessOutcome.WhiteMates else ChessOutcome.BlackMates
    val playerLost: Boolean get() = outcome == if (playerWhite) ChessOutcome.BlackMates else ChessOutcome.WhiteMates

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("bot", bot.name)
        .put("white", playerWhite)
        .put("moves", moves.joinToString(" "))
        .put("outcome", outcome?.name ?: "")

    companion object {
        fun fromJson(o: JSONObject) = SavedGame(
            o.getLong("id"),
            ChessBot.valueOf(o.getString("bot")),
            o.getBoolean("white"),
            o.getString("moves").split(' ').filter { it.isNotBlank() },
            o.optString("outcome").takeIf { it.isNotBlank() }?.let { ChessOutcome.valueOf(it) }
        )
    }
}

/** 给孩子看的一步棋：「白 马 g1→f3」「黑 兵 e5 吃 d4」「白 王车易位」 */
fun describeMove(pos: ChessPosition, m: ChessMove): String {
    val piece = pos[m.from]
    val side = if (piece > 0) "白" else "黑"
    val name = AllPieceNames[abs(piece)]
    val capture = pos[m.to] != 0 || (abs(piece) == Pc.PAWN && m.to == pos.ep && m.from % 8 != m.to % 8)
    val base = when {
        abs(piece) == Pc.KING && abs(m.to - m.from) == 2 -> "$side 王车易位"
        capture -> "$side $name ${sqName(m.from)} 吃 ${sqName(m.to)}"
        else -> "$side $name ${sqName(m.from)}→${sqName(m.to)}"
    }
    val promo = if (m.promotion != 0) "，升变为${AllPieceNames[m.promotion]}" else ""
    val check = pos.play(m).let { if (it.inCheck()) if (it.legalMoves().isEmpty()) " 将杀！" else " 将军" else "" }
    return base + promo + check
}

internal fun isPromotionMove(pos: ChessPosition, from: Int, to: Int): Boolean =
    abs(pos[from]) == Pc.PAWN && (to / 8 == 0 || to / 8 == 7)

private val PieceNames = mapOf(Pc.QUEEN to "后", Pc.ROOK to "车", Pc.BISHOP to "象", Pc.KNIGHT to "马")

private fun isCastle(pos: ChessPosition, m: ChessMove) = abs(pos[m.from]) == Pc.KING && abs(m.to - m.from) == 2

private fun isEnPassant(pos: ChessPosition, m: ChessMove) =
    abs(pos[m.from]) == Pc.PAWN && m.to == pos.ep && pos[m.to] == 0 && m.from % 8 != m.to % 8

fun isSpecialMove(pos: ChessPosition, m: ChessMove) = isCastle(pos, m) || isEnPassant(pos, m) || m.promotion != 0

/** 特殊走法走完之后的讲解；普通走法返回 null */
fun specialMoveNote(pos: ChessPosition, m: ChessMove, byPlayer: Boolean): String? {
    val who = if (byPlayer) "你" else "对方"
    return when {
        isCastle(pos, m) -> "🏰 ${who}王车易位了！王向车的方向走两格，车跳到王的另一边，王更安全、车也出来了。"
        isEnPassant(pos, m) -> "👻 ${who}吃过路兵！对方的兵刚才一下走两格，从旁边「路过」，可以像它只走一格那样斜着吃掉它。只有紧接着的这一步能吃。"
        m.promotion != 0 -> "👑 兵升变！${who}的兵走到了底线，变成了${PieceNames[m.promotion]}。"
        else -> null
    }
}

/** 选中一个子时，如果它有特殊走法，提示怎么走 */
fun specialSelectHint(pos: ChessPosition, sq: Int): String? {
    val moves = pos.legalMoves().filter { it.from == sq }
    return when {
        moves.any { isCastle(pos, it) } -> "🏰 可以王车易位：点金色圈，王向车走两格，车会自动跳到王的另一边。要求王和车都没动过、中间没有棋子、王没被将军。"
        moves.any { isEnPassant(pos, it) } -> "👻 可以吃过路兵：对方的兵刚走两格停在你的兵旁边，点金色圈斜着走过去就能吃掉它。"
        moves.any { it.promotion != 0 } -> "👑 这个兵再走一步就到底线，可以升变成后、车、象或马。后最强，一般选后。"
        else -> null
    }
}

private val AllPieceNames = mapOf(Pc.PAWN to "兵", Pc.KNIGHT to "马", Pc.BISHOP to "象", Pc.ROOK to "车", Pc.QUEEN to "后", Pc.KING to "王")

/** 走完 [m] 后攻击己方王的对方棋子所在格；王本来就安全时为 null */
private fun checkerAfter(pos: ChessPosition, m: ChessMove): Int? {
    val us = pos.whiteToMove
    val next = pos.play(m)
    val king = next.kingSquare(us)
    if (king < 0) return null
    return ChessPosition(next.board, !us).pseudoMoves().firstOrNull { it.to == king }?.from
}

private fun describe(pos: ChessPosition, sq: Int) = "对方 ${sqName(sq)} 的${AllPieceNames[abs(pos[sq])]}"

/** 选了 [from] 的子却点了不能去的 [to]：说明为什么不行 */
fun illegalMoveHint(pos: ChessPosition, from: Int, to: Int): String {
    val type = abs(pos[from])
    val name = AllPieceNames[type]
    val pseudo = pos.pseudoMoves().firstOrNull { it.from == from && it.to == to }
    if (pseudo != null) {
        val by = checkerAfter(pos, pseudo)?.let { describe(pos.play(pseudo), it) } ?: "对方"
        return when {
            type == Pc.KING -> "🚫 王不能走到 ${sqName(to)}：那里会被${by}吃掉。王永远不能走到被攻击的格子。"
            pos.inCheck() -> "🚫 你正被将军！这一步没有解除将军，${by}还在攻击你的王。要让王躲开、用子挡住，或者吃掉将军的子。"
            else -> "🚫 这个${name}被牵制了：它一走开，${by}就能直接吃你的王。"
        }
    }
    val df = abs(to % 8 - from % 8)
    val dr = abs(to / 8 - from / 8)
    val straight = df == 0 || dr == 0
    val diagonal = df == dr
    return when (type) {
        Pc.KING -> if (dr == 0 && df == 2) "🚫 现在不能王车易位：王或车已经动过、中间有棋子、王正被将军或要经过被攻击的格子，都不能易位。"
            else "🚫 王每次只能走一格。"
        Pc.ROOK -> if (straight) "🚫 车走直线，但中间有棋子挡住了，不能跳过去。" else "🚫 车只能横着或竖着走直线。"
        Pc.BISHOP -> if (diagonal) "🚫 象走斜线，但中间有棋子挡住了，不能跳过去。" else "🚫 象只能斜着走。"
        Pc.QUEEN -> if (straight || diagonal) "🚫 后的路上有棋子挡住了，不能跳过去。" else "🚫 后只能走直线或斜线，不能走「日」字。"
        Pc.KNIGHT -> "🚫 马走「日」字：先直走两格，再拐一格。"
        Pc.PAWN -> {
            val forward = if (pos[from] > 0) to / 8 > from / 8 else to / 8 < from / 8
            when {
                !forward -> "🚫 兵只能向前走，不能后退或横着走。"
                df == 0 && pos[to] != 0 -> "🚫 兵不能向前吃子，前面有子挡着就走不了。"
                df == 1 && dr == 1 -> "🚫 兵只有吃子的时候才能斜着走。"
                df == 0 && dr == 2 -> "🚫 兵只有第一步能走两格，而且中间不能有子。"
                else -> "🚫 兵每次只能向前走一格（第一步可以走两格）。"
            }
        }
        else -> "🚫 这里不能走。"
    }
}

/** 选中的子一步都走不了时说明原因；能走则返回 null */
fun stuckPieceHint(pos: ChessPosition, sq: Int): String? {
    if (pos.legalMoves().any { it.from == sq }) return null
    val name = AllPieceNames[abs(pos[sq])]
    val pseudo = pos.pseudoMoves().filter { it.from == sq }
    return when {
        pseudo.isEmpty() -> "🚫 这个${name}被挡住了，现在一步也走不了。"
        abs(pos[sq]) == Pc.KING -> "🚫 王周围的格子都被对方攻击着，王现在不能动。"
        pos.inCheck() -> "🚫 你正被将军！这个${name}没法解除将军，换一个子，或者让王躲开。"
        else -> "🚫 这个${name}被牵制了：它一走开，你的王就会被吃，所以不能动。"
    }
}

class MateExplanation(val lines: List<String>, val checkers: Set<Int>, val deniedSquares: Set<Int>)

/** 正在将军行棋方的对方棋子 */
fun checkersOf(pos: ChessPosition): Set<Int> {
    val king = pos.kingSquare(pos.whiteToMove)
    if (king < 0) return emptySet()
    return ChessPosition(pos.board, !pos.whiteToMove).pseudoMoves().filter { it.to == king }.map { it.from }.toSet()
}

/** 讲解 [pos]（行棋方已被将杀）为什么是将杀；不是将杀返回 null。[matedIsPlayer] 决定用「你」还是「对方」。 */
fun explainMate(pos: ChessPosition, matedIsPlayer: Boolean): MateExplanation? {
    if (!pos.inCheck() || pos.legalMoves().isNotEmpty()) return null
    val us = pos.whiteToMove
    val king = pos.kingSquare(us)
    val checkers = checkersOf(pos)
    val mine = if (matedIsPlayer) "你" else "对方"
    val theirs = if (matedIsPlayer) "对方" else "你"
    fun name(sq: Int) = "${sqName(sq)} 的${AllPieceNames[abs(pos[sq])]}"
    val lines = mutableListOf<String>()

    lines += if (checkers.size >= 2) {
        "⚔️ 双将！${theirs}的 ${checkers.joinToString("、") { name(it) }} 同时在将军${mine}的王（${sqName(king)}）。双将挡不住也吃不完，王只能自己逃。"
    } else {
        "⚔️ ${theirs}的 ${name(checkers.first())} 正在将军${mine}的王（${sqName(king)}）。"
    }

    // 王的每个邻格：被自己人堵住，还是被对方控制
    val ownBlocked = mutableListOf<Int>()
    val controlledBy = linkedMapOf<Int, MutableList<Int>>()
    val f0 = king % 8
    val r0 = king / 8
    for (d in ChessPosition.KING_DIRS) {
        val f = f0 + d[0]
        val r = r0 + d[1]
        if (f !in 0..7 || r !in 0..7) continue
        val sq = r * 8 + f
        if (pos[sq] != 0 && (pos[sq] > 0) == us) {
            ownBlocked += sq
            continue
        }
        val after = pos.play(ChessMove(king, sq))
        val attacker = ChessPosition(after.board, !us).pseudoMoves().firstOrNull { it.to == sq }?.from ?: continue
        controlledBy.getOrPut(attacker) { mutableListOf() } += sq
    }
    if (ownBlocked.isNotEmpty()) {
        lines += "🧱 王旁边的 ${ownBlocked.sorted().joinToString("、") { sqName(it) }} 被${mine}自己的棋子堵住了。"
    }
    controlledBy.forEach { (attacker, squares) ->
        lines += "❌ ${squares.sorted().joinToString("、") { sqName(it) }} 被${theirs}的 ${name(attacker)} 控制着，王走过去会被吃。"
    }

    if (checkers.size == 1) {
        val checker = checkers.first()
        lines += "🚫 ${mine}没有棋子能安全地吃掉 ${name(checker)}。"
        val df = abs(checker % 8 - f0)
        val dr = abs(checker / 8 - r0)
        lines += when {
            abs(pos[checker]) == Pc.KNIGHT -> "🐴 马的将军是跳过来的，中间没法挡。"
            maxOf(df, dr) <= 1 -> "🚫 贴身将军，中间没有空格可以挡。"
            else -> "🚫 也没有棋子能挡在将军的路线上。"
        }
    }
    lines += "所以王无路可逃，这就是将杀！"
    return MateExplanation(lines, checkers, controlledBy.values.flatten().toSet())
}
