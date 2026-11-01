package com.example.ogdenkids

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val LightSquare = Color(0xFFF0D9B5)
private val DarkSquare = Color(0xFFB58863)
private val LastMoveTint = Color(0x66F6E05E)
private val SelectedTint = Color(0x8862B35B)
private val HintTint = Color(0x883B82F6)
private val TargetDot = Color(0x55000000)
private val SpecialRing = Color(0xFFF59E0B)

// ---------- 首页 ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessHomeScreen(
    store: ChessStore,
    padding: PaddingValues,
    onLesson: (Int) -> Unit,
    onPuzzles: (String) -> Unit,
    onPlay: (ChessBot, Boolean) -> Unit
) {
    val context = LocalContext.current
    val bank by produceState<Map<String, List<ChessPuzzle>>>(emptyMap()) {
        value = withContext(Dispatchers.IO) { PuzzleBank.load(context) }
    }
    var playWhite by rememberSaveable { mutableStateOf(true) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ChessSectionTitle("第一步 · 认识棋子", "收集星星，学会每个棋子怎么走") }
        items(ChessLessons.size) { i ->
            val lesson = ChessLessons[i]
            val got = lesson.levels.indices.sumOf { store.lessonStars(lesson.key, it) }
            ChessRowCard(
                leading = { PieceGlyph(lesson.glyph, 36.dp) },
                title = lesson.title,
                titleEn = lesson.en,
                subtitle = "${lesson.levels.size} 关",
                trailing = "⭐ $got/${lesson.levels.size * 3}",
                onClick = { onLesson(i) }
            )
        }
        item { ChessSectionTitle("第二步 · 做题练战术", "每章 400 题，越往后越难") }
        items(PuzzleChapters) { chapter ->
            val total = bank[chapter.key]?.size ?: 0
            ChessRowCard(
                leading = { AppText("🧩", fontSize = 28.sp) },
                title = chapter.title,
                subtitle = chapter.desc,
                trailing = if (total > 0) "${store.puzzleIndex(chapter.key).coerceAtMost(total)}/$total" else "",
                onClick = { onPuzzles(chapter.key) }
            )
        }
        item {
            ChessSectionTitle("第三步 · 和电脑下棋", "从小猫咪开始，赢了再挑战下一个")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppText("我执", color = InkSoft)
                FilterChip(selected = playWhite, onClick = { playWhite = true }, label = { AppText("白棋（先走）") })
                FilterChip(selected = !playWhite, onClick = { playWhite = false }, label = { AppText("黑棋") })
            }
        }
        items(ChessBot.values().toList()) { bot ->
            val wins = store.botWins(bot)
            ChessRowCard(
                leading = { AppText(bot.emoji, fontSize = 30.sp) },
                title = bot.title,
                subtitle = bot.desc,
                trailing = if (wins > 0) "🏆 $wins" else "",
                onClick = { onPlay(bot, playWhite) }
            )
        }
    }
}

@Composable
private fun ChessSectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        AppText(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        AppText(subtitle, color = InkFaint, fontSize = 13.sp)
    }
}

/** 可点击的英文单词，点开词典详情页（同生肖页的星座英文） */
@Composable
private fun DictionaryWord(word: String, fontSize: androidx.compose.ui.unit.TextUnit = 16.sp) {
    val services = LocalSpeechServices.current
    Text(
        word,
        color = Category.Operations.tint,
        fontSize = fontSize,
        fontWeight = FontWeight.SemiBold,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier.clickable { services.lookup(word)?.let(services.openWord) }
    )
}

@Composable
private fun ChessRowCard(
    leading: @Composable () -> Unit,
    title: String,
    subtitle: String,
    trailing: String,
    onClick: () -> Unit,
    titleEn: String? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { leading() }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppText(title, fontWeight = FontWeight.SemiBold, color = Ink)
                    titleEn?.let { DictionaryWord(it) }
                }
                AppText(subtitle, color = InkFaint, fontSize = 13.sp)
            }
            AppText(trailing, color = InkSoft, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ChessTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().background(PaperElevated).border(1.dp, Line).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        Column(Modifier.weight(1f)) {
            AppText(title, fontWeight = FontWeight.Bold)
            AppText(subtitle, color = InkFaint, fontSize = 12.sp)
        }
        CurrentUserChip()
    }
}

// ---------- 棋盘 ----------

@Composable
fun PieceGlyph(piece: Int, size: Dp) {
    val type = abs(piece)
    val filled = Character.toChars(0x265A + (Pc.KING - type)).concatToString()
    val fontSize = with(LocalDensity.current) { (size * 0.78f).toSp() }
    val style = TextStyle(fontSize = fontSize, lineHeight = fontSize, platformStyle = PlatformTextStyle(includeFontPadding = false))
    Box(contentAlignment = Alignment.Center) {
        if (piece > 0) {
            val outline = Character.toChars(0x2654 + (Pc.KING - type)).concatToString()
            Text(filled + "︎", style = style, color = Color.White)
            Text(outline + "︎", style = style, color = Color(0xFF111111))
        } else {
            Text(filled + "︎", style = style, color = Color(0xFF111111))
        }
    }
}

@Composable
fun ChessBoardView(
    pos: ChessPosition,
    flipped: Boolean,
    selected: Int?,
    targets: Set<Int>,
    lastMove: ChessMove?,
    onTap: (Int) -> Unit,
    stars: Set<Int> = emptySet(),
    hint: Int? = null,
    special: Set<Int> = emptySet(),
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f).border(2.dp, DarkSquare)) {
        val cell = maxWidth / 8
        Column {
            for (row in 0..7) Row {
                for (col in 0..7) {
                    val sq = if (flipped) row * 8 + (7 - col) else (7 - row) * 8 + col
                    val light = (sq / 8 + sq % 8) % 2 == 1
                    val tint = when {
                        sq == selected -> SelectedTint
                        sq == hint -> HintTint
                        lastMove != null && (sq == lastMove.from || sq == lastMove.to) -> LastMoveTint
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier.size(cell).background(if (light) LightSquare else DarkSquare).background(tint).clickable { onTap(sq) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (sq in stars) AppText("⭐", fontSize = with(LocalDensity.current) { (cell * 0.55f).toSp() })
                        if (pos[sq] != 0) PieceGlyph(pos[sq], cell)
                        if (sq in special) Box(Modifier.size(cell * 0.86f).border(4.dp, SpecialRing, CircleShape))
                        if (sq in targets) {
                            if (pos[sq] != 0) {
                                Box(Modifier.size(cell).border(3.dp, TargetDot, CircleShape))
                            } else {
                                Box(Modifier.size(cell * 0.3f).background(TargetDot, CircleShape))
                            }
                        }
                        val labelColor = if (light) DarkSquare else LightSquare
                        if (col == 0) AppText("${sq / 8 + 1}", fontSize = 9.sp, color = labelColor, modifier = Modifier.align(Alignment.TopStart).padding(2.dp))
                        if (row == 7) AppText("${'a' + sq % 8}", fontSize = 9.sp, color = labelColor, modifier = Modifier.align(Alignment.BottomEnd).padding(2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PromotionDialog(white: Boolean, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AppText("兵升变：选一个棋子") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (p in listOf(Pc.QUEEN, Pc.ROOK, Pc.BISHOP, Pc.KNIGHT)) {
                    Box(
                        Modifier.size(56.dp).background(LightSquare, RoundedCornerShape(8.dp)).clickable { onPick(p) },
                        contentAlignment = Alignment.Center
                    ) { PieceGlyph(if (white) p else -p, 52.dp) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { AppText("取消") } }
    )
}

/**
 * 点选走子：先点己方棋子再点目标格；落子需要选升变时弹框。
 * [movable] 决定哪方棋子可被选中。
 */
private class TapMover(val onMove: (ChessMove) -> Unit) {
    var selected by mutableStateOf<Int?>(null)
    var pendingPromotion by mutableStateOf<Pair<Int, Int>?>(null)
    /** 特殊走法的提示浮条 */
    var note by mutableStateOf<String?>(null)
    private var promotionPos: ChessPosition? = null

    /** 记一步棋（包括对手 / AI 的），是特殊走法就弹讲解 */
    fun announce(pos: ChessPosition, m: ChessMove, byPlayer: Boolean) {
        specialMoveNote(pos, m, byPlayer)?.let { note = it }
    }

    private fun move(pos: ChessPosition, m: ChessMove) {
        announce(pos, m, byPlayer = true)
        onMove(m)
    }

    fun specialTargets(pos: ChessPosition): Set<Int> {
        val s = selected ?: return emptySet()
        return pos.legalMoves().filter { it.from == s && isSpecialMove(pos, it) }.map { it.to }.toSet()
    }

    fun targets(pos: ChessPosition): Set<Int> {
        val s = selected ?: return emptySet()
        return pos.legalMoves().filter { it.from == s }.map { it.to }.toSet()
    }

    fun tap(pos: ChessPosition, sq: Int, movable: Boolean, autoQueen: Boolean = false) {
        val s = selected
        val mine = pos[sq] != 0 && (pos[sq] > 0) == pos.whiteToMove
        if (s != null && sq in targets(pos)) {
            selected = null
            if (isPromotionMove(pos, s, sq)) {
                if (autoQueen) move(pos, ChessMove(s, sq, Pc.QUEEN)) else {
                    promotionPos = pos
                    pendingPromotion = s to sq
                }
            } else {
                move(pos, ChessMove(s, sq))
            }
        } else if (movable && mine && s != sq) {
            selected = sq
            (stuckPieceHint(pos, sq) ?: specialSelectHint(pos, sq))?.let { note = it }
        } else if (movable && s != null && s != sq) {
            // 保留选中，孩子看完提示可以直接换个格子
            note = illegalMoveHint(pos, s, sq)
        } else {
            selected = null
        }
    }

    @Composable
    fun Dialog(white: Boolean) {
        val p = pendingPromotion ?: return
        PromotionDialog(white, onPick = {
            pendingPromotion = null
            val m = ChessMove(p.first, p.second, it)
            promotionPos?.let { pos -> move(pos, m) } ?: onMove(m)
        }, onDismiss = { pendingPromotion = null })
    }
}

/** 进入棋盘页面时循环播放数学挑战的背景音乐；切到后台暂停 */
@Composable
private fun ChessBackgroundMusic() {
    val context = LocalContext.current
    val bgm = remember { BackgroundMusic(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(bgm, lifecycle) {
        bgm.start()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) bgm.pause()
            if (event == Lifecycle.Event.ON_START) bgm.resume()
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            bgm.stop()
        }
    }
}

@Composable
private fun CheerSound() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val player = runCatching {
            MediaPlayer().apply {
                context.assets.openFd("sfx/cheer.mp3").use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                prepare()
                start()
            }
        }.getOrNull()
        onDispose { player?.release() }
    }
}

/**
 * 棋盘占满可用区域（取宽高较小者做正方形）；说明和按钮放在可拖动、可收起的浮窗里。
 * [expandKey] 变化时（换关、答对 / 答错等）自动展开浮窗。
 */
@Composable
private fun ChessLayout(
    expandKey: Any?,
    note: String? = null,
    onNoteDismiss: () -> Unit = {},
    board: @Composable (Modifier) -> Unit,
    info: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(expandKey) { expanded = true }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val landscape = maxWidth > maxHeight
        val panelMaxHeight = maxHeight * 0.6f
        board(Modifier.align(Alignment.Center).size(minOf(maxWidth, maxHeight)))
        if (note != null) {
            LaunchedEffect(note) {
                delay(8000)
                onNoteDismiss()
            }
            Box(
                Modifier.align(Alignment.TopCenter).padding(12.dp).widthIn(max = 520.dp)
                    .background(Color(0xF2FFF7E0), RoundedCornerShape(14.dp))
                    .border(2.dp, SpecialRing, RoundedCornerShape(14.dp))
                    .clickable(onClick = onNoteDismiss)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) { AppText(note, color = Ink, fontWeight = FontWeight.Medium) }
        }
        val panelAlign = if (landscape) Alignment.CenterEnd else Alignment.BottomCenter
        val drag = Modifier.pointerInput(Unit) {
            detectDragGestures { change, amount ->
                change.consume()
                offset += amount
            }
        }
        Box(
            Modifier.align(panelAlign).padding(12.dp)
                .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
        ) {
            if (expanded) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier.widthIn(max = 360.dp).heightIn(max = panelMaxHeight)
                ) {
                    Row(drag.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppText("💡 提示", fontWeight = FontWeight.SemiBold, color = InkSoft, modifier = Modifier.weight(1f))
                        IconButton(onClick = { expanded = false }) { Icon(Icons.Default.ExpandMore, contentDescription = "收起") }
                    }
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        content = info
                    )
                }
            } else {
                Box(
                    drag.background(PaperElevated.copy(alpha = 0.95f), RoundedCornerShape(50))
                        .border(1.dp, Line, RoundedCornerShape(50))
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) { AppText("💡 展开提示", color = InkSoft, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ---------- 认识棋子 ----------

@Composable
fun ChessLessonScreen(lessonIndex: Int, store: ChessStore, onLesson: (Int) -> Unit, onBack: () -> Unit) {
    ChessBackgroundMusic()
    val lesson = ChessLessons[lessonIndex]
    var level by rememberSaveable { mutableStateOf(0) }
    var attempt by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        ChessTopBar("${lesson.title} 怎么走", "第 ${level + 1} / ${lesson.levels.size} 关", onBack)
        key(level, attempt) {
            StarRound(
                lesson = lesson,
                levelIndex = level,
                store = store,
                onRetry = { attempt++ },
                onNext = {
                    when {
                        level < lesson.levels.lastIndex -> level++
                        lessonIndex < ChessLessons.lastIndex -> onLesson(lessonIndex + 1)
                        else -> onBack()
                    }
                }
            )
        }
    }
}

@Composable
private fun StarRound(lesson: ChessLesson, levelIndex: Int, store: ChessStore, onRetry: () -> Unit, onNext: () -> Unit) {
    val level = lesson.levels[levelIndex]
    var pos by remember { mutableStateOf(level.start) }
    var stars by remember { mutableStateOf(level.stars) }
    var moves by remember { mutableStateOf(0) }
    var lastMove by remember { mutableStateOf<ChessMove?>(null) }
    val remaining = stars.size + pos.board.count { it < 0 }
    val done = remaining == 0
    val mover = remember {
        TapMover { m ->
            pos = pos.play(m).withSideToMove(true)
            stars = stars - m.to
            moves++
            lastMove = m
        }
    }
    val earned = if (done) starsFor(moves, level.par) else 0
    LaunchedEffect(done) { if (done) store.saveLessonStars(lesson.key, levelIndex, earned) }
    if (done) CheerSound()
    ChessLayout(expandKey = done, note = mover.note, onNoteDismiss = { mover.note = null }, board = { m ->
        ChessBoardView(
            modifier = m,
            pos = pos,
            flipped = false,
            selected = mover.selected,
            targets = mover.targets(pos),
            special = mover.specialTargets(pos),
            lastMove = lastMove,
            stars = stars,
            onTap = { if (!done) mover.tap(pos, it, movable = true, autoQueen = true) }
        )
    }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PieceGlyph(lesson.glyph, 32.dp)
            AppText(lesson.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            DictionaryWord(lesson.en, 18.sp)
        }
        AppText(lesson.intro, color = InkSoft)
        AppText(
            if (level.stars.isNotEmpty()) "收集所有星星 ⭐" else "吃掉所有黑子",
            fontWeight = FontWeight.SemiBold
        )
        AppText("已走 $moves 步 · 最少 ${level.par} 步", color = InkFaint)
        if (done) {
            AppText("⭐".repeat(earned) + "☆".repeat(3 - earned), fontSize = 32.sp)
            AppText(if (earned == 3) "太棒了，用了最少的步数！" else "完成！试试用更少的步数？", fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRetry) { AppText("再来一次") }
                Button(onClick = onNext) { AppText("下一关") }
            }
        } else {
            OutlinedButton(onClick = onRetry) { AppText("重新开始") }
        }
    }
}

// ---------- 战术题 ----------

@Composable
fun ChessPuzzleScreen(chapterKey: String, store: ChessStore, onBack: () -> Unit) {
    ChessBackgroundMusic()
    val context = LocalContext.current
    val chapter = PuzzleChapters.first { it.key == chapterKey }
    val puzzles by produceState<List<ChessPuzzle>?>(null, chapterKey) {
        value = withContext(Dispatchers.IO) { PuzzleBank.load(context)[chapterKey].orEmpty() }
    }
    var index by rememberSaveable { mutableStateOf(store.puzzleIndex(chapterKey)) }
    val list = puzzles
    Column(Modifier.fillMaxSize().background(Paper)) {
        val total = list?.size ?: 0
        ChessTopBar(chapter.title, if (total > 0) "第 ${(index % total) + 1} / $total 题" else chapter.desc, onBack)
        if (!list.isNullOrEmpty()) {
            val puzzle = list[index % list.size]
            key(puzzle.id) {
                PuzzleRound(puzzle, chapter, onNext = {
                    index++
                    store.savePuzzleIndex(chapterKey, index)
                })
            }
        }
    }
}

private enum class PuzzleState { Opponent, Solving, Wrong, Solved, Revealed }

@Composable
private fun PuzzleRound(puzzle: ChessPuzzle, chapter: PuzzleChapter, onNext: () -> Unit) {
    val initial = remember { ChessPosition.fromFen(puzzle.fen) }
    var pos by remember { mutableStateOf(initial) }
    var step by remember { mutableStateOf(0) }
    var lastMove by remember { mutableStateOf<ChessMove?>(null) }
    var state by remember { mutableStateOf(PuzzleState.Opponent) }
    var hint by remember { mutableStateOf<Int?>(null) }
    val playerWhite = !initial.whiteToMove

    val mover = remember {
        TapMover { m ->
            val expected = puzzle.moves[step]
            val last = step == puzzle.moves.lastIndex
            val after = pos.play(m)
            // 最后一步：任何将杀都算对（Lichess 也这么判）
            val mates = last && after.legalMoves().isEmpty() && after.inCheck()
            if (m.uci() == expected || mates) {
                pos = after
                lastMove = m
                step++
                hint = null
                state = if (step >= puzzle.moves.size) PuzzleState.Solved else PuzzleState.Opponent
            } else {
                state = PuzzleState.Wrong
            }
        }
    }

    fun applyNext() {
        val m = ChessMove.parse(puzzle.moves[step])
        mover.announce(pos, m, byPlayer = step % 2 == 1)
        pos = pos.play(m)
        lastMove = m
        step++
    }

    LaunchedEffect(step, state) {
        if (state == PuzzleState.Opponent && step < puzzle.moves.size && step % 2 == 0) {
            delay(if (step == 0) 700 else 450)
            applyNext()
            state = PuzzleState.Solving
        }
    }

    val active = state == PuzzleState.Solving || state == PuzzleState.Wrong
    mover.Dialog(playerWhite)
    if (state == PuzzleState.Solved) CheerSound()
    ChessLayout(expandKey = state, note = mover.note, onNoteDismiss = { mover.note = null }, board = { m ->
        ChessBoardView(
            modifier = m,
            pos = pos,
            flipped = !playerWhite,
            selected = mover.selected,
            targets = if (active) mover.targets(pos) else emptySet(),
            special = if (active) mover.specialTargets(pos) else emptySet(),
            lastMove = lastMove,
            hint = hint,
            onTap = { if (active) mover.tap(pos, it, movable = pos.whiteToMove == playerWhite) }
        )
    }) {
        AppText(
            when (state) {
                PuzzleState.Opponent -> if (step == 0) "对手先走一步…" else "对手应着…"
                PuzzleState.Solving -> "轮到你（${if (playerWhite) "白棋" else "黑棋"}）：${chapter.desc}"
                PuzzleState.Wrong -> "不对哦，再想想 🤔"
                PuzzleState.Solved -> "答对了！🎉"
                PuzzleState.Revealed -> "这是答案，记住它，下一题加油！"
            },
            fontWeight = FontWeight.SemiBold,
            color = when (state) {
                PuzzleState.Wrong -> Error
                PuzzleState.Solved -> Success
                else -> Ink
            }
        )
        AppText("难度 ${puzzle.rating}", color = InkFaint, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (active) {
                OutlinedButton(onClick = { hint = ChessMove.parse(puzzle.moves[step]).from }) { AppText("提示") }
                OutlinedButton(onClick = {
                    mover.selected = null
                    hint = null
                    while (step < puzzle.moves.size) applyNext()
                    state = PuzzleState.Revealed
                }) { AppText("看答案") }
            }
            if (state == PuzzleState.Solved || state == PuzzleState.Revealed) {
                Button(onClick = onNext) { AppText("下一题") }
            }
        }
    }
}

// ---------- 对战 ----------

@Composable
fun ChessPlayScreen(bot: ChessBot, playerWhite: Boolean, store: ChessStore, onBack: () -> Unit) {
    ChessBackgroundMusic()
    var game by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        ChessTopBar("${bot.emoji} 对战${bot.title}", "你执${if (playerWhite) "白" else "黑"} · ${bot.desc}", onBack)
        key(game) { PlayRound(bot, playerWhite, store, onRestart = { game++ }) }
    }
}

@Composable
private fun PlayRound(bot: ChessBot, playerWhite: Boolean, store: ChessStore, onRestart: () -> Unit) {
    val record = remember { ChessGameRecord(ChessPosition.fromFen(ChessPosition.START_FEN)) }
    var version by remember { mutableStateOf(0) }
    var thinking by remember { mutableStateOf(false) }
    val pos = remember(version) { record.current }
    val outcome = remember(version) { record.outcome() }
    val ai = remember { ChessAi(bot) }
    val mover = remember {
        TapMover { m ->
            record.play(m)
            version++
        }
    }

    LaunchedEffect(version) {
        if (outcome == null && pos.whiteToMove != playerWhite) {
            thinking = true
            val started = System.currentTimeMillis()
            val m = withContext(Dispatchers.Default) { ai.choose(pos) }
            delay((500 - (System.currentTimeMillis() - started)).coerceAtLeast(0))
            thinking = false
            if (m != null) {
                mover.announce(pos, m, byPlayer = false)
                record.play(m)
                version++
            }
        }
    }

    val playerWon = outcome == if (playerWhite) ChessOutcome.WhiteMates else ChessOutcome.BlackMates
    LaunchedEffect(outcome) { if (playerWon) store.recordBotWin(bot) }

    mover.Dialog(playerWhite)
    if (playerWon) CheerSound()
    ChessLayout(expandKey = outcome, note = mover.note, onNoteDismiss = { mover.note = null }, board = { m ->
        ChessBoardView(
            modifier = m,
            pos = pos,
            flipped = !playerWhite,
            selected = mover.selected,
            targets = mover.targets(pos),
            special = mover.specialTargets(pos),
            lastMove = record.moves.lastOrNull(),
            onTap = { if (outcome == null && !thinking) mover.tap(pos, it, movable = pos.whiteToMove == playerWhite) }
        )
    }) {
        AppText(
            when {
                outcome != null -> outcome.text
                thinking -> "${bot.title}在想… ${bot.emoji}"
                pos.inCheck() -> "你被将军了！快保护王 ⚠️"
                else -> "轮到你走"
            },
            fontWeight = FontWeight.SemiBold,
            color = if (pos.inCheck() && outcome == null) Error else Ink
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                enabled = !thinking && record.moves.isNotEmpty(),
                onClick = {
                    // 退回到轮到自己走的局面
                    record.undo()
                    if (record.current.whiteToMove != playerWhite) record.undo()
                    mover.selected = null
                    version++
                }
            ) { AppText("悔棋") }
            OutlinedButton(onClick = onRestart) { AppText("重新开始") }
        }
        if (outcome != null) {
            Spacer(Modifier.height(4.dp))
            AppText(
                when {
                    playerWon -> "你赢了！🏆"
                    outcome == ChessOutcome.WhiteMates || outcome == ChessOutcome.BlackMates -> "这盘输了，悔棋看看哪一步走错了？"
                    else -> "和棋 🤝"
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
