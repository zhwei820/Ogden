package com.example.ogdenkids

import android.content.Context
import android.media.MediaPlayer
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 顺序播放预生成的离线片段（数字 / 运算符 / 数字）：先全部 prepare，再用 setNextMediaPlayer 无缝接上，段间不卡顿。
 * 片段缺失（资源被删等）时整句交给 [azure] 兜底。
 */
private class ClipSequencePlayer(private val context: Context, private val azure: AzureSpeaker) {
    private val packs = PackStore.of(context)
    private var players: List<MediaPlayer> = emptyList()

    fun play(problem: MathProblem, mode: PromptMode, accent: Accent, onDone: () -> Unit) {
        stop()
        val voice = if (mode == PromptMode.VoiceZh) AzureVoice.ZhCn else AzureVoice.english(accent)
        val clips = if (mode == PromptMode.VoiceZh) problem.chineseClips else problem.englishClips
        val prepared = runCatching { clips.map { prepare(ttsAssetPath(voice, it)) } }.getOrNull()
        if (prepared == null || prepared.any { it == null }) {
            prepared?.forEach { it?.release() }
            azure.speak(clips.joinToString(" "), voice, onDone)
            return
        }
        val list = prepared.filterNotNull()
        players = list
        list.zipWithNext { a, b -> a.setNextMediaPlayer(b) }
        list.last().setOnCompletionListener { if (players === list) { stop(); onDone() } }
        list.first().start()
    }

    private fun prepare(path: String): MediaPlayer? {
        val downloaded = packs.findFile(path)
        val fd = if (downloaded != null) null else runCatching { context.assets.openFd(path) }.getOrNull()
        if (downloaded == null && fd == null) return null
        val mp = MediaPlayer()
        try {
            if (fd != null) mp.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length) else mp.setDataSource(downloaded!!.path)
            mp.prepare()
        } catch (e: Exception) {
            mp.release()
            return null
        } finally {
            fd?.close()
        }
        return mp
    }

    fun stop() {
        players.forEach { it.release() }
        players = emptyList()
        azure.stop()
    }
}

private data class MathAnswer(val problem: MathProblem, val given: Int?) {
    val correct: Boolean get() = given == problem.answer
}

private enum class DrillPhase { Asking, Feedback, Finished }

@Composable
fun MathDrillScreen(
    store: ProgressStore,
    accent: Accent,
    azure: AzureSpeaker,
    onBack: () -> Unit
) {
    var op by rememberSaveable { mutableStateOf(MathOp.Add) }
    var carry by rememberSaveable { mutableStateOf(CarryMode.Any) }
    var level by rememberSaveable { mutableStateOf(MathLevel.Easy) }
    var prompt by rememberSaveable { mutableStateOf(PromptMode.Text) }
    var playing by remember { mutableStateOf(false) }
    // 每开一局递增，重新出题
    var session by remember { mutableStateOf(0) }

    if (playing) {
        MathDrillRound(
            key = session,
            op = op,
            carry = carry,
            level = level,
            prompt = prompt,
            accent = accent,
            azure = azure,
            bestScore = { store.bestSpecialScore("math-${op.name}-${carry.name}", level.ordinal) },
            onFinish = { correct -> store.saveSpecialScore("math-${op.name}-${carry.name}", level.ordinal, correct) },
            onRestart = { session++ },
            onExit = { playing = false }
        )
        return
    }

    Scaffold(containerColor = Paper, topBar = { DrillTopBar("🧮 口算小游戏", "100 以内加减法 · 每局 $MathDrillSize 题", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { OptionCard("题型", MathOp.values().toList(), op, { it.zh }) { op = it } }
            item { OptionCard("进位 / 退位", CarryMode.values().toList(), carry, { it.zh }) { carry = it } }
            item { OptionCard("难度", MathLevel.values().toList(), level, { "${it.zh} ${it.seconds}秒" }) { level = it } }
            item { OptionCard("出题方式", PromptMode.values().toList(), prompt, { it.zh }) { prompt = it } }
            item {
                val best = store.bestSpecialScore("math-${op.name}-${carry.name}", level.ordinal)
                AppText(
                    when (prompt) {
                        PromptMode.Text -> "看算式，在规定时间内输入得数"
                        PromptMode.VoiceZh -> "中文读出数字和运算符号，读完开始计时"
                        PromptMode.VoiceEn -> "英文读出数字和运算符号（如 thirty-five plus seven），读完开始计时"
                    },
                    color = InkFaint,
                    fontSize = 13.sp
                )
                if (best != null) AppText("本组合最好成绩 $best / $MathDrillSize", color = InkFaint, fontSize = 13.sp)
            }
            item {
                Button(
                    onClick = { session++; playing = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("开始训练", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> OptionCard(title: String, options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppText(title, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { AppText(label(option)) })
                }
            }
        }
    }
}

@Composable
private fun DrillTopBar(title: String, subtitle: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperElevated)
            .border(1.dp, Line)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
        Column(Modifier.weight(1f)) {
            AppText(title, fontWeight = FontWeight.Bold)
            AppText(subtitle, color = InkFaint, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MathDrillRound(
    key: Int,
    op: MathOp,
    carry: CarryMode,
    level: MathLevel,
    prompt: PromptMode,
    accent: Accent,
    azure: AzureSpeaker,
    bestScore: () -> Int?,
    onFinish: (correct: Int) -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    val context = LocalContext.current
    val clips = remember { ClipSequencePlayer(context, azure) }
    DisposableEffect(clips) { onDispose { clips.stop() } }
    BackHandler(onBack = onExit)

    val problems = remember(key) { buildMathDrill(op, carry, Random(System.nanoTime())) }
    val answers = remember(key) { mutableStateListOf<MathAnswer>() }
    var index by remember(key) { mutableStateOf(0) }
    var input by remember(key, index) { mutableStateOf("") }
    var phase by remember(key) { mutableStateOf(DrillPhase.Asking) }
    // 语音出题时读完才开始计时
    var timerRunning by remember(key, index) { mutableStateOf(prompt == PromptMode.Text) }
    var remainingMs by remember(key, index) { mutableStateOf(level.seconds * 1000L) }
    val problem = problems.getOrNull(index)
    val correctCount = answers.count { it.correct }

    fun submit(given: Int?) {
        if (phase != DrillPhase.Asking || problem == null) return
        clips.stop()
        answers += MathAnswer(problem, given)
        phase = DrillPhase.Feedback
    }

    LaunchedEffect(key, index) {
        if (prompt != PromptMode.Text && problem != null) clips.play(problem, prompt, accent) { timerRunning = true }
    }
    LaunchedEffect(key, index, timerRunning, phase) {
        if (!timerRunning || phase != DrillPhase.Asking) return@LaunchedEffect
        while (remainingMs > 0) {
            delay(100)
            remainingMs -= 100
        }
        submit(null)
    }
    LaunchedEffect(key, index, phase) {
        if (phase != DrillPhase.Feedback) return@LaunchedEffect
        delay(if (answers.lastOrNull()?.correct == true) 600 else 1500)
        if (index >= problems.lastIndex) {
            onFinish(answers.count { it.correct })
            phase = DrillPhase.Finished
        } else {
            index++
            phase = DrillPhase.Asking
        }
    }

    val subtitle = "${op.zh} · ${carry.zh} · ${level.zh} ${level.seconds}秒 · ${prompt.zh}"
    Scaffold(containerColor = Paper, topBar = {
        DrillTopBar(
            if (phase == DrillPhase.Finished) "训练结果" else "第 ${index + 1} / ${problems.size} 题 · 答对 $correctCount",
            subtitle,
            onExit
        )
    }) { padding ->
        if (phase == DrillPhase.Finished) {
            DrillResult(answers, bestScore(), Modifier.padding(padding), onRestart, onExit)
            return@Scaffold
        }
        if (problem == null) return@Scaffold
        val last = answers.lastOrNull().takeIf { phase == DrillPhase.Feedback }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = remainingMs / (level.seconds * 1000f),
                modifier = Modifier.fillMaxWidth(),
                color = if (remainingMs <= 3000) Error else Category.Operations.tint
            )
            AppText(
                if (timerRunning) "剩余 ${(remainingMs + 999) / 1000} 秒" else "正在读题…",
                color = InkFaint,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(8.dp))
            // 语音出题答完才亮出算式
            val showText = prompt == PromptMode.Text || last != null
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(
                    if (showText) "${problem.text} =" else "? ? =",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp
                )
                Spacer(Modifier.width(12.dp))
                AnswerBox(
                    text = last?.given?.toString() ?: input.ifEmpty { " " },
                    color = when {
                        last == null -> Ink
                        last.correct -> Success
                        else -> Error
                    }
                )
            }
            if (prompt != PromptMode.Text && last == null) {
                OutlinedButton(onClick = { clips.play(problem, prompt, accent) { timerRunning = true } }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    AppText("再听一遍")
                }
            }
            Box(Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                when {
                    last == null -> Unit
                    last.correct -> AppText("✓ 答对了", color = Success, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    else -> AppText(
                        (if (last.given == null) "⏰ 超时了" else "✗ 答错了") + "，正确答案是 ${problem.answer}",
                        color = Error,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            NumberPad(
                enabled = phase == DrillPhase.Asking,
                onDigit = { d -> if (input.length < 3) input = (input + d).trimStart('0').ifEmpty { "0" } },
                onDelete = { input = input.dropLast(1) },
                onSubmit = { input.toIntOrNull()?.let(::submit) }
            )
        }
    }
}

@Composable
private fun AnswerBox(text: String, color: Color) {
    Box(
        modifier = Modifier
            .width(96.dp)
            .height(64.dp)
            .border(2.dp, if (color == Ink) Line else color, RoundedCornerShape(12.dp))
            .background(PaperElevated, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        AppText(text, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 36.sp, color = color)
    }
}

@Composable
private fun NumberPad(enabled: Boolean, onDigit: (Char) -> Unit, onDelete: () -> Unit, onSubmit: () -> Unit) {
    val rows = listOf("123", "456", "789")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d -> PadKey(Modifier.weight(1f), enabled, { onDigit(d) }) { Text("$d", fontSize = 26.sp) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PadKey(Modifier.weight(1f), enabled, onDelete) { Icon(Icons.Default.Backspace, contentDescription = "删除") }
            PadKey(Modifier.weight(1f), enabled, { onDigit('0') }) { Text("0", fontSize = 26.sp) }
            Button(
                onClick = onSubmit,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text("确定", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PadKey(modifier: Modifier, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(0.dp)
    ) { content() }
}

@Composable
private fun DrillResult(answers: List<MathAnswer>, best: Int?, modifier: Modifier, onRestart: () -> Unit, onExit: () -> Unit) {
    val correct = answers.count { it.correct }
    val wrong = answers.filterNot { it.correct }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            AppText("答对 $correct / ${answers.size}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp)
        }
        item { AppText(if (wrong.isEmpty()) "全对！太棒了！" else "再练一次，争取全对", color = InkSoft, fontSize = 18.sp) }
        best?.let { item { AppText("最好成绩 $it / ${answers.size}", color = InkFaint) } }
        if (wrong.isNotEmpty()) {
            item { AppText("错题", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()) }
            items(wrong) { a ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    AppText("${a.problem.text} = ${a.problem.answer}", fontSize = 18.sp)
                    AppText(a.given?.let { "你答 $it" } ?: "超时", color = Error, fontSize = 16.sp)
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onRestart, modifier = Modifier.fillMaxWidth()) { Text("再练一次", fontSize = 18.sp) }
                OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) { Text("返回", fontSize = 18.sp) }
            }
        }
    }
}
