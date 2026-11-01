package com.example.ogdenkids

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val LightSquare = Color(0xFFF3E4C6)
private val DarkSquare = Color(0xFFB8875B)
private val FrameLight = Color(0xFF8A5A33)
private val FrameDark = Color(0xFF4E2F18)
private val FrameText = Color(0xDDF3E4C6)
private val PieceShadow = Color(0x40000000)
private val LastMoveTint = Color(0x80F5D76E)
private val SelectedTint = Color(0x9982C77F)
private val HintTint = Color(0x8870A9F5)
private val TargetDot = Color(0x47000000)
private val CheckGlow = Color(0xFFE53935)
private val CheckerTint = Color(0x66E53935)
private val SpecialRing = Color(0xFFF59E0B)

// ---------- 首页 ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessHomeScreen(
    store: ChessStore,
    padding: PaddingValues,
    onLesson: (Int) -> Unit,
    onPuzzles: (String) -> Unit,
    onPlay: (ChessBot, Boolean) -> Unit,
    onHistory: () -> Unit,
    onPuzzleHistory: () -> Unit,
    onRules: (Int) -> Unit
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
        item { ChessSectionTitle("第二步 · 基本规则", "将军、将杀、易位……下棋前先弄懂") }
        items(RuleLessons.size) { i ->
            val lesson = RuleLessons[i]
            val got = lesson.levels.indices.sumOf { store.lessonStars("rule-${lesson.key}", it) }
            ChessRowCard(
                leading = { AppText(lesson.emoji, fontSize = 28.sp) },
                title = lesson.title,
                subtitle = "${lesson.levels.size} 关",
                trailing = "⭐ $got/${lesson.levels.size * 3}",
                onClick = { onRules(i) }
            )
        }
        item {
            val sizes = bank.values.map { it.size }.distinct()
            ChessSectionTitle(
                "第三步 · 做题练战术",
                when {
                    sizes.size == 1 -> "每章 ${sizes[0]} 题，越往后越难"
                    sizes.isNotEmpty() -> "共 ${bank.values.sumOf { it.size }} 题，越往后越难"
                    else -> "越往后越难"
                }
            )
        }
        items(PuzzleChapters) { chapter ->
            val list = bank[chapter.key].orEmpty()
            val total = list.size
            val solved = remember(list) { store.solvedPuzzles(chapter.key).let { ids -> list.count { it.id in ids } } }
            ChessRowCard(
                leading = { AppText("🧩", fontSize = 28.sp) },
                title = chapter.title,
                subtitle = chapter.desc,
                trailing = if (total > 0) "$solved/$total" else "",
                onClick = { onPuzzles(chapter.key) }
            )
        }
        item {
            val count = remember { store.puzzleLog().size }
            ChessRowCard(
                leading = { AppText("📜", fontSize = 28.sp) },
                title = "做题记录",
                subtitle = "看看做过的题，错过的再做一遍",
                trailing = if (count > 0) "$count 题" else "",
                onClick = onPuzzleHistory
            )
        }
        item {
            ChessSectionTitle("第四步 · 和电脑下棋", "从小猫咪开始，赢了再挑战下一个")
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
        item {
            val count = remember { store.games().size }
            ChessRowCard(
                leading = { AppText("📜", fontSize = 28.sp) },
                title = "对局记录",
                subtitle = "回看下过的棋，一步一步复盘",
                trailing = if (count > 0) "$count 盘" else "",
                onClick = onHistory
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

@DrawableRes
private fun pieceRes(piece: Int): Int {
    val white = piece > 0
    return when (abs(piece)) {
        Pc.PAWN -> if (white) R.drawable.chess_w_pawn else R.drawable.chess_b_pawn
        Pc.KNIGHT -> if (white) R.drawable.chess_w_knight else R.drawable.chess_b_knight
        Pc.BISHOP -> if (white) R.drawable.chess_w_bishop else R.drawable.chess_b_bishop
        Pc.ROOK -> if (white) R.drawable.chess_w_rook else R.drawable.chess_b_rook
        Pc.QUEEN -> if (white) R.drawable.chess_w_queen else R.drawable.chess_b_queen
        else -> if (white) R.drawable.chess_w_king else R.drawable.chess_b_king
    }
}

@Composable
fun PieceGlyph(piece: Int, size: Dp, modifier: Modifier = Modifier) {
    Image(painterResource(pieceRes(piece)), contentDescription = null, modifier = modifier.size(size))
}

/** 带投影的棋子；选中时略微放大「拿起来」 */
@Composable
private fun BoardPiece(piece: Int, size: Dp, lifted: Boolean, modifier: Modifier) {
    val painter = painterResource(pieceRes(piece))
    val scale by animateFloatAsState(if (lifted) 1.12f else 1f, label = "lift")
    Box(modifier.size(size).graphicsLayer { scaleX = scale; scaleY = scale }) {
        Image(
            painter,
            contentDescription = null,
            colorFilter = ColorFilter.tint(PieceShadow),
            modifier = Modifier.fillMaxSize().offset(x = size * 0.03f, y = size * (if (lifted) 0.09f else 0.05f))
        )
        Image(painter, contentDescription = null, modifier = Modifier.fillMaxSize())
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
    denied: Set<Int> = emptySet(),
    modifier: Modifier = Modifier
) {
    // 走子滑动动画：只在局面变化时播放刚走的那一步
    val slide = remember { Animatable(1f) }
    LaunchedEffect(pos) {
        if (lastMove != null) {
            slide.snapTo(0f)
            slide.animateTo(1f, tween(220))
        }
    }
    val currentOnTap by rememberUpdatedState(onTap)
    val checkedKing = if (pos.inCheck()) pos.kingSquare(pos.whiteToMove) else -1
    val checkers = remember(pos) { if (checkedKing >= 0) checkersOf(pos) else emptySet() }
    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f)) {
        val frame = maxWidth * 0.05f
        val boardSize = maxWidth - frame * 2
        val cell = boardSize / 8
        fun colOf(sq: Int) = if (flipped) 7 - sq % 8 else sq % 8
        fun rowOf(sq: Int) = if (flipped) sq / 8 else 7 - sq / 8

        Box(
            Modifier.fillMaxSize()
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .background(Brush.linearGradient(listOf(FrameLight, FrameDark)), RoundedCornerShape(16.dp))
        )
        val labelSize = with(LocalDensity.current) { (frame * 0.55f).toSp() }
        for (i in 0..7) {
            val file = if (flipped) 7 - i else i
            val rank = if (flipped) i + 1 else 8 - i
            Box(Modifier.offset(x = frame + cell * i, y = frame + boardSize).size(cell, frame), contentAlignment = Alignment.Center) {
                Text("${'a' + file}", color = FrameText, fontSize = labelSize, fontWeight = FontWeight.Bold)
            }
            Box(Modifier.offset(y = frame + cell * i).size(frame, cell), contentAlignment = Alignment.Center) {
                Text("$rank", color = FrameText, fontSize = labelSize, fontWeight = FontWeight.Bold)
            }
        }

        Box(
            Modifier.offset(frame, frame).size(boardSize).clip(RoundedCornerShape(4.dp))
                .pointerInput(flipped) {
                    detectTapGestures { o ->
                        val c = (o.x / (size.width / 8f)).toInt().coerceIn(0, 7)
                        val r = (o.y / (size.height / 8f)).toInt().coerceIn(0, 7)
                        currentOnTap(if (flipped) r * 8 + (7 - c) else (7 - r) * 8 + c)
                    }
                }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val c = size.width / 8
                for (sq in 0 until 64) {
                    val tl = Offset(colOf(sq) * c, rowOf(sq) * c)
                    val light = (sq / 8 + sq % 8) % 2 == 1
                    drawRect(if (light) LightSquare else DarkSquare, tl, Size(c, c))
                    // 每格一点由上到下的光泽，比纯色更像木纹棋盘
                    drawRect(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.10f), Color.Transparent), tl.y, tl.y + c),
                        tl, Size(c, c)
                    )
                    val tint = when {
                        sq == selected -> SelectedTint
                        sq == hint -> HintTint
                        lastMove != null && (sq == lastMove.from || sq == lastMove.to) -> LastMoveTint
                        else -> null
                    }
                    if (tint != null) drawRect(tint, tl, Size(c, c))
                    val center = tl + Offset(c / 2, c / 2)
                    if (sq == checkedKing) {
                        drawCircle(Brush.radialGradient(listOf(CheckGlow, CheckGlow.copy(alpha = 0f)), center, c * 0.6f), c * 0.6f, center)
                    }
                    if (sq in checkers) drawRect(CheckerTint, tl, Size(c, c))
                    if (sq in denied) {
                        val r = c * 0.22f
                        drawLine(CheckGlow, center + Offset(-r, -r), center + Offset(r, r), c * 0.08f, StrokeCap.Round)
                        drawLine(CheckGlow, center + Offset(-r, r), center + Offset(r, -r), c * 0.08f, StrokeCap.Round)
                    }
                    if (sq in special) drawCircle(SpecialRing, c * 0.44f, center, style = Stroke(c * 0.07f))
                    if (sq in targets) {
                        if (pos[sq] != 0) drawCircle(TargetDot, c * 0.46f, center, style = Stroke(c * 0.08f))
                        else drawCircle(TargetDot, c * 0.15f, center)
                    }
                }
            }
            for (sq in stars) {
                Box(Modifier.offset(cell * colOf(sq), cell * rowOf(sq)).size(cell), contentAlignment = Alignment.Center) {
                    Text("⭐", fontSize = with(LocalDensity.current) { (cell * 0.55f).toSp() })
                }
            }
            // Compose 编译器 1.4 下 composable 循环里不能用 continue / 提前 return，会导致组不平衡崩溃
            for (sq in (0 until 64).filter { pos[it] != 0 }) {
                val piece = pos[sq]
                var x = cell * colOf(sq)
                var y = cell * rowOf(sq)
                if (lastMove != null && sq == lastMove.to && slide.value < 1f) {
                    val t = slide.value
                    x = cell * colOf(lastMove.from) * (1 - t) + x * t
                    y = cell * rowOf(lastMove.from) * (1 - t) + y * t
                }
                BoardPiece(piece, cell, lifted = sq == selected, modifier = Modifier.offset(x, y).zIndex(if (sq == lastMove?.to) 1f else 0f))
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
        val p = pendingPromotion
        if (p != null) {
            PromotionDialog(white, onPick = {
                pendingPromotion = null
                val m = ChessMove(p.first, p.second, it)
                promotionPos?.let { pos -> move(pos, m) } ?: onMove(m)
            }, onDismiss = { pendingPromotion = null })
        }
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

/**
 * 棋盘和提示分区摆放、互不遮挡：横屏棋盘靠左、提示栏在右（至少 [SidePanelWidth]）；
 * 竖屏棋盘在上、提示栏在下（至少 [BottomPanelHeight]）。棋盘取剩余空间里最大的正方形。
 * [expandKey] 变化时（换关、答对 / 答错等）自动展开提示。
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
    LaunchedEffect(expandKey) { expanded = true }
    if (note != null) {
        LaunchedEffect(note) {
            delay(8000)
            onNoteDismiss()
        }
    }
    val panel: @Composable ColumnScope.() -> Unit = {
        if (note != null) {
            Box(
                Modifier.fillMaxWidth()
                    .background(Color(0xFFFFF7E0), RoundedCornerShape(14.dp))
                    .border(2.dp, SpecialRing, RoundedCornerShape(14.dp))
                    .clickable(onClick = onNoteDismiss)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) { AppText(note, color = Ink, fontWeight = FontWeight.Medium) }
        }
        if (expanded) {
            Card(
                colors = CardDefaults.cardColors(containerColor = PaperElevated),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false).border(1.dp, Line, RoundedCornerShape(16.dp))
            ) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppText("💡 提示", fontWeight = FontWeight.SemiBold, color = InkSoft, modifier = Modifier.weight(1f))
                    IconButton(onClick = { expanded = false }) { Icon(Icons.Default.ExpandLess, contentDescription = "收起") }
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
                Modifier.background(PaperElevated, RoundedCornerShape(50))
                    .border(1.dp, Line, RoundedCornerShape(50))
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) { AppText("💡 展开提示", color = InkSoft, fontWeight = FontWeight.SemiBold) }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val gap = 12.dp
        if (maxWidth > maxHeight) {
            val boardSize = minOf(maxHeight, maxWidth - SidePanelWidth - gap)
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                board(Modifier.size(boardSize))
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = panel
                )
            }
        } else {
            val boardSize = minOf(maxWidth, maxHeight - BottomPanelHeight - gap)
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
                board(Modifier.size(boardSize))
                Column(
                    Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = panel
                )
            }
        }
    }
}

private val SidePanelWidth = 300.dp
private val BottomPanelHeight = 220.dp

@Composable
private fun MateExplanationCard(explanation: MateExplanation) {
    Column(
        Modifier.fillMaxWidth().background(Color(0xFFFDECEA), RoundedCornerShape(12.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppText("为什么是将杀？", fontWeight = FontWeight.Bold, color = Error)
        explanation.lines.forEach { AppText(it, color = Ink, fontSize = 14.sp) }
        AppText("棋盘上：红格是将军的子，红色 × 是王逃不过去的格子。", color = InkFaint, fontSize = 12.sp)
    }
}

// ---------- 认识棋子 ----------

@Composable
fun ChessLessonScreen(lessonIndex: Int, store: ChessStore, onLesson: (Int) -> Unit, onBack: () -> Unit) {
    ChessBackgroundMusic()
    val lesson = ChessLessons[lessonIndex]
    var level by rememberSaveable { mutableStateOf(firstUnfinished(lesson.levels.size) { store.lessonStars(lesson.key, it) }) }
    var attempt by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        ChessTopBar("${lesson.title} 怎么走", "第 ${level + 1} / ${lesson.levels.size} 关", onBack)
        key(level, attempt) {
            StarRound(
                lesson = lesson,
                levelIndex = level,
                store = store,
                onPick = { level = it },
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
private fun StarRound(lesson: ChessLesson, levelIndex: Int, store: ChessStore, onPick: (Int) -> Unit, onRetry: () -> Unit, onNext: () -> Unit) {
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
        LevelPicker(lesson.levels.size, levelIndex, { store.lessonStars(lesson.key, it) }, onPick)
        AppText(lesson.intro, color = InkSoft)
        val hasEnemies = level.start.board.any { it < 0 }
        AppText(
            when {
                level.stars.isNotEmpty() && hasEnemies -> "收集所有星星 ⭐，还要吃掉所有黑子"
                level.stars.isNotEmpty() -> "收集所有星星 ⭐"
                else -> "吃掉所有黑子"
            },
            fontWeight = FontWeight.SemiBold
        )
        if (!done) AppText("还剩 $remaining 个目标", color = InkSoft, fontSize = 14.sp)
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
fun ChessPuzzleScreen(chapterKey: String, store: ChessStore, onBack: () -> Unit, startPuzzleId: String? = null) {
    ChessBackgroundMusic()
    val context = LocalContext.current
    val chapter = PuzzleChapters.first { it.key == chapterKey }
    val puzzles by produceState<List<ChessPuzzle>?>(null, chapterKey) {
        value = withContext(Dispatchers.IO) { PuzzleBank.load(context)[chapterKey].orEmpty() }
    }
    // 从指定的题，或者本章第一道还没做对的题开始；题库加载完才知道是第几题
    var index by rememberSaveable { mutableStateOf(-1) }
    val list = puzzles
    LaunchedEffect(list) {
        if (!list.isNullOrEmpty() && index < 0) {
            val solved = store.solvedPuzzles(chapterKey)
            index = startPuzzleId?.let { id -> list.indexOfFirst { it.id == id } }?.takeIf { it >= 0 }
                ?: list.indexOfFirst { it.id !in solved }.takeIf { it >= 0 }
                ?: 0
        }
    }
    Column(Modifier.fillMaxSize().background(Paper)) {
        val total = list?.size ?: 0
        ChessTopBar(chapter.title, if (total > 0 && index >= 0) "第 ${(index % total) + 1} / $total 题" else chapter.desc, onBack)
        if (!list.isNullOrEmpty() && index >= 0) {
            val puzzle = list[index % list.size]
            key(puzzle.id) {
                PuzzleRound(
                    puzzle,
                    chapter,
                    previous = remember(puzzle.id) { store.lastAttempt(puzzle.id) },
                    onPrevious = if (index > 0) ({ index-- }) else null,
                    onFinished = { mistakes, revealed ->
                        store.recordPuzzle(PuzzleAttempt(puzzle.id, chapterKey, index, puzzle.rating, mistakes, revealed, System.currentTimeMillis()))
                        // 只有自己做对才算进度；看答案的题下次还会从这里开始
                        if (!revealed) store.markSolved(chapterKey, puzzle.id)
                    },
                    onNext = { index++ }
                )
            }
        }
    }
}

private enum class PuzzleState { Opponent, Solving, Wrong, Solved, Revealed }

@Composable
private fun PuzzleRound(
    puzzle: ChessPuzzle,
    chapter: PuzzleChapter,
    previous: PuzzleAttempt?,
    onPrevious: (() -> Unit)?,
    onFinished: (mistakes: Int, revealed: Boolean) -> Unit,
    onNext: () -> Unit
) {
    var mistakes by remember { mutableStateOf(0) }
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
                mistakes++
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
    val finished = state == PuzzleState.Solved || state == PuzzleState.Revealed
    LaunchedEffect(finished) { if (finished) onFinished(mistakes, state == PuzzleState.Revealed) }
    val mate = remember(pos, finished) { if (finished) explainMate(pos, matedIsPlayer = false) else null }
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
            denied = mate?.deniedSquares.orEmpty(),
            onTap = { if (active) mover.tap(pos, it, movable = pos.whiteToMove == playerWhite) }
        )
    }) {
        // 按钮固定在提示框顶部、始终占位（不能点时置灰），下面的文字变长变短都不会让它们挪位置
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null) { AppText("‹ 上一题") }
            Spacer(Modifier.weight(1f))
            // 做完了，或者以前做过的题，可以往后翻
            val canNext = finished || previous != null
            if (finished) {
                Button(onClick = onNext) { AppText("下一题 ›") }
            } else {
                TextButton(onClick = onNext, enabled = canNext) { AppText("下一题 ›") }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = { hint = ChessMove.parse(puzzle.moves[step]).from },
                enabled = active,
                modifier = Modifier.weight(1f)
            ) { AppText("提示") }
            OutlinedButton(
                onClick = {
                    mover.selected = null
                    hint = null
                    while (step < puzzle.moves.size) applyNext()
                    state = PuzzleState.Revealed
                },
                enabled = active,
                modifier = Modifier.weight(1f)
            ) { AppText("看答案") }
        }
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
        AppText(
            "难度 ${puzzle.rating}" + if (previous != null) " · 上次：${previous.label}" else "",
            color = InkFaint,
            fontSize = 13.sp
        )
        mate?.let { MateExplanationCard(it) }
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
    val gameId = remember { System.currentTimeMillis() }
    fun save() = store.saveGame(SavedGame(gameId, bot, playerWhite, record.moves.map { it.uci() }, record.outcome()))
    LaunchedEffect(outcome) { if (outcome != null) save() }
    // 没下完就离开 / 重新开始：走过几步的也留个记录
    DisposableEffect(Unit) { onDispose { if (record.outcome() == null && record.moves.size >= 4) save() } }

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
    val mate = remember(pos) { explainMate(pos, matedIsPlayer = pos.whiteToMove == playerWhite) }
    ChessLayout(expandKey = outcome, note = mover.note, onNoteDismiss = { mover.note = null }, board = { m ->
        ChessBoardView(
            modifier = m,
            pos = pos,
            flipped = !playerWhite,
            selected = mover.selected,
            targets = mover.targets(pos),
            special = mover.specialTargets(pos),
            lastMove = record.moves.lastOrNull(),
            denied = mate?.deniedSquares.orEmpty(),
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
            mate?.let { MateExplanationCard(it) }
        }
    }
}


// ---------- 对局记录 ----------

@Composable
fun ChessHistoryScreen(store: ChessStore, onOpen: (Long) -> Unit, onBack: () -> Unit) {
    val games = remember { store.games() }
    val format = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINA) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        ChessTopBar("📜 对局记录", "最近 ${games.size} 盘，点开可以复盘", onBack)
        if (games.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppText("还没有对局记录，去和电脑下一盘吧！", color = InkFaint)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(games, key = { it.id }) { game ->
                    ChessRowCard(
                        leading = { AppText(game.bot.emoji, fontSize = 30.sp) },
                        title = "对战${game.bot.title} · 执${if (game.playerWhite) "白" else "黑"}",
                        subtitle = "${format.format(Date(game.id))} · ${(game.moves.size + 1) / 2} 回合",
                        trailing = resultLabel(game),
                        onClick = { onOpen(game.id) }
                    )
                }
            }
        }
    }
}

private fun resultLabel(game: SavedGame): String = when {
    game.outcome == null -> "未下完"
    game.playerWon -> "🏆 赢"
    game.playerLost -> "输"
    else -> "🤝 和"
}

@Composable
fun ChessReplayScreen(gameId: Long, store: ChessStore, onBack: () -> Unit) {
    val game = remember { store.games().firstOrNull { it.id == gameId } }
    Column(Modifier.fillMaxSize().background(Paper)) {
        ChessTopBar(
            if (game != null) "复盘：对战${game.bot.title} ${game.bot.emoji}" else "复盘",
            if (game != null) "你执${if (game.playerWhite) "白" else "黑"} · ${resultLabel(game)}" else "",
            onBack
        )
        if (game != null) ReplayBody(game)
    }
}

@Composable
private fun ReplayBody(game: SavedGame) {
    // 按记录重放；遇到不合法的步（理论上不会）就停在那里
    val replay = remember(game.id) {
        val positions = mutableListOf(ChessPosition.fromFen(ChessPosition.START_FEN))
        val moves = mutableListOf<ChessMove>()
        for (uci in game.moves) {
            val m = positions.last().legalMoves().firstOrNull { it.uci() == uci } ?: break
            moves += m
            positions += positions.last().play(m)
        }
        positions to moves
    }
    val (positions, moves) = replay
    val descriptions = remember(game.id) { moves.mapIndexed { i, m -> describeMove(positions[i], m) } }
    var ply by rememberSaveable { mutableStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    LaunchedEffect(playing, ply) {
        if (playing) {
            if (ply < moves.size) {
                delay(1200)
                ply++
            } else {
                playing = false
            }
        }
    }
    val pos = positions[ply]
    val mate = remember(ply) { explainMate(pos, matedIsPlayer = pos.whiteToMove == game.playerWhite) }
    ChessLayout(expandKey = Unit, board = { m ->
        ChessBoardView(
            modifier = m,
            pos = pos,
            flipped = !game.playerWhite,
            selected = null,
            targets = emptySet(),
            lastMove = moves.getOrNull(ply - 1),
            denied = mate?.deniedSquares.orEmpty(),
            onTap = {}
        )
    }) {
        AppText(
            if (ply == 0) "开局" else "第 $ply / ${moves.size} 步：${descriptions[ply - 1]}",
            fontWeight = FontWeight.SemiBold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { playing = false; ply = 0 }, enabled = ply > 0) { Icon(Icons.Default.SkipPrevious, contentDescription = "回到开局") }
            IconButton(onClick = { playing = false; ply-- }, enabled = ply > 0) { Icon(Icons.Default.ChevronLeft, contentDescription = "上一步") }
            IconButton(onClick = { playing = !playing }, enabled = ply < moves.size || playing) {
                Icon(if (playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (playing) "暂停" else "自动播放")
            }
            IconButton(onClick = { playing = false; ply++ }, enabled = ply < moves.size) { Icon(Icons.Default.ChevronRight, contentDescription = "下一步") }
            IconButton(onClick = { playing = false; ply = moves.size }, enabled = ply < moves.size) { Icon(Icons.Default.SkipNext, contentDescription = "最后一步") }
        }
        if (ply == moves.size) {
            AppText(game.outcome?.text ?: "这盘没有下完", color = InkSoft)
        }
        mate?.let { MateExplanationCard(it) }
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            descriptions.forEachIndexed { i, text ->
                val current = i + 1 == ply
                AppText(
                    "${i / 2 + 1}${if (i % 2 == 0) "." else "…"} $text",
                    color = if (current) Ink else InkSoft,
                    fontWeight = if (current) FontWeight.Bold else null,
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                        .background(if (current) LastMoveTint else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { playing = false; ply = i + 1 }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}


// ---------- 做题记录 ----------

private enum class PuzzleFilter(val label: String) { All("全部"), Missed("错过的"), Perfect("一次做对") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChessPuzzleHistoryScreen(store: ChessStore, onOpen: (chapter: String, puzzleId: String) -> Unit, onBack: () -> Unit) {
    val log = remember { store.puzzleLog() }
    val format = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINA) }
    var filter by rememberSaveable { mutableStateOf(PuzzleFilter.All) }
    val shown = log.filter {
        when (filter) {
            PuzzleFilter.All -> true
            PuzzleFilter.Missed -> it.revealed || it.mistakes > 0
            PuzzleFilter.Perfect -> !it.revealed && it.mistakes == 0
        }
    }
    val perfect = log.count { !it.revealed && it.mistakes == 0 }
    Column(Modifier.fillMaxSize().background(Paper)) {
        ChessTopBar("📜 做题记录", "做过 ${log.size} 题 · 一次做对 $perfect 题", onBack)
        Row(Modifier.padding(horizontal = 18.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PuzzleFilter.values().forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { AppText(f.label) })
            }
        }
        if (shown.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AppText(if (log.isEmpty()) "还没做过题，去「做题练战术」试试吧！" else "这里没有题目", color = InkFaint)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shown, key = { it.puzzleId }) { attempt ->
                    val chapter = PuzzleChapters.firstOrNull { it.key == attempt.chapter }
                    ChessRowCard(
                        leading = { AppText("🧩", fontSize = 28.sp) },
                        title = "${chapter?.title ?: attempt.chapter} 第 ${attempt.index + 1} 题",
                        subtitle = "${format.format(Date(attempt.time))} · 难度 ${attempt.rating}",
                        trailing = attempt.label,
                        onClick = { onOpen(attempt.chapter, attempt.puzzleId) }
                    )
                }
            }
        }
    }
}

/** 第一个还没拿满三星的关卡；全满就从第 1 关开始 */
private fun firstUnfinished(count: Int, stars: (Int) -> Int): Int = (0 until count).firstOrNull { stars(it) < 3 } ?: 0

@Composable
private fun LevelPicker(count: Int, current: Int, stars: (Int) -> Int, onPick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        for (i in 0 until count) {
            val got = stars(i)
            Column(
                Modifier.weight(1f)
                    .background(if (i == current) SelectedTint else PaperElevated, RoundedCornerShape(10.dp))
                    .border(1.dp, Line, RoundedCornerShape(10.dp))
                    .clickable { onPick(i) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText("${i + 1}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                AppText(if (got > 0) "⭐".repeat(got) else "·", fontSize = 9.sp)
            }
        }
    }
}

// ---------- 基本规则 ----------

@Composable
fun ChessRulesScreen(lessonIndex: Int, store: ChessStore, onLesson: (Int) -> Unit, onBack: () -> Unit) {
    ChessBackgroundMusic()
    val lesson = RuleLessons[lessonIndex]
    val starKey = "rule-${lesson.key}"
    var level by rememberSaveable { mutableStateOf(firstUnfinished(lesson.levels.size) { store.lessonStars(starKey, it) }) }
    var attempt by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        ChessTopBar("${lesson.emoji} ${lesson.title}", "第 ${level + 1} / ${lesson.levels.size} 关", onBack)
        key(level, attempt) {
            RuleRound(
                lesson = lesson,
                levelIndex = level,
                store = store,
                onPick = { level = it },
                onRetry = { attempt++ },
                onNext = {
                    when {
                        level < lesson.levels.lastIndex -> level++
                        lessonIndex < RuleLessons.lastIndex -> onLesson(lessonIndex + 1)
                        else -> onBack()
                    }
                }
            )
        }
    }
}

@Composable
private fun RuleRound(lesson: RuleLesson, levelIndex: Int, store: ChessStore, onPick: (Int) -> Unit, onRetry: () -> Unit, onNext: () -> Unit) {
    val level = lesson.levels[levelIndex]
    val starKey = "rule-${lesson.key}"
    var pos by remember { mutableStateOf(level.start) }
    var lastMove by remember { mutableStateOf<ChessMove?>(null) }
    var mistakes by remember { mutableStateOf(0) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    val mover = remember {
        TapMover { m ->
            val problem = ruleMoveFeedback(pos, m, level.goal)
            if (problem == null) {
                pos = pos.play(m)
                lastMove = m
                feedback = null
                done = true
            } else {
                mistakes++
                feedback = problem
            }
        }
    }
    val earned = when (mistakes) { 0 -> 3; 1 -> 2; else -> 1 }
    LaunchedEffect(done) { if (done) store.saveLessonStars(starKey, levelIndex, earned) }
    if (done) CheerSound()
    val mate = remember(pos, done) { if (done) explainMate(pos, matedIsPlayer = false) else null }
    mover.Dialog(true)
    ChessLayout(expandKey = done to feedback, note = mover.note, onNoteDismiss = { mover.note = null }, board = { m ->
        ChessBoardView(
            modifier = m,
            pos = pos,
            flipped = false,
            selected = mover.selected,
            targets = if (done) emptySet() else mover.targets(pos),
            special = if (done) emptySet() else mover.specialTargets(pos),
            lastMove = lastMove,
            denied = mate?.deniedSquares.orEmpty(),
            onTap = { if (!done) mover.tap(pos, it, movable = pos.whiteToMove) }
        )
    }) {
        LevelPicker(lesson.levels.size, levelIndex, { store.lessonStars(starKey, it) }, onPick)
        AppText(lesson.intro, color = InkSoft)
        AppText("你执白棋：${level.goal.task}", fontWeight = FontWeight.SemiBold)
        feedback?.let { AppText(it, color = Error, fontWeight = FontWeight.Medium) }
        if (done) {
            AppText("⭐".repeat(earned) + "☆".repeat(3 - earned), fontSize = 32.sp)
            AppText(if (earned == 3) "一次就做对了！" else "做对了！", fontWeight = FontWeight.SemiBold, color = Success)
            mate?.let { MateExplanationCard(it) }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onRetry) { AppText("再来一次") }
                Button(onClick = onNext) { AppText("下一关") }
            }
        } else {
            OutlinedButton(onClick = onRetry) { AppText("重新开始") }
        }
    }
}
