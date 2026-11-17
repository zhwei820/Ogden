package com.example.ogdenkids

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlin.random.Random

enum class CefrLevel(val key: String, val zh: String, val desc: String, val tint: Color, val soft: Color) {
    A1("A1", "入门", "最基础的日常词", Color(0xFF166534), Color(0xFFDCFCE7)),
    A2("A2", "基础", "出行、购物、学校里常用的词", Color(0xFF0F766E), Color(0xFFCCFBF1)),
    B1("B1", "中级", "表达观点、谈论社会话题", Color(0xFF1E40AF), Color(0xFFDBEAFE)),
    B2("B2", "中高级", "新闻、议论文里的常用词", Color(0xFF7C3AED), Color(0xFFEDE9FE)),
    C1("C1", "高级", "学术、职场里的抽象词", Color(0xFFB45309), Color(0xFFFEF3C7)),
    C2("C2", "精通", "接近普通母语者的书面词", Color(0xFFBE185D), Color(0xFFFCE7F3));

    val title get() = "$key $zh"
}

data class CefrWord(val level: CefrLevel, val word: String, val pos: String, val zh: String)

const val CefrGroupSize = 20

/** assets/cefr_words.tsv 由 scripts/cefr_words.py 从 leveled-words.md 生成；只读一次。 */
object CefrWords {
    @Volatile private var cache: List<CefrWord>? = null

    fun all(context: Context): List<CefrWord> = cache ?: synchronized(this) {
        cache ?: context.assets.open("cefr_words.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
            val levels = CefrLevel.values().associateBy { it.key }
            lines.mapNotNull { line ->
                val p = line.split('\t')
                val level = levels[p.getOrNull(0)]
                if (p.size == 4 && level != null) CefrWord(level, p[1], p[2], p[3]) else null
            }.toList()
        }.also { cache = it }
    }

    fun of(context: Context, level: CefrLevel) = all(context).filter { it.level == level }
}

private fun groupCount(size: Int) = ceil(size / CefrGroupSize.toDouble()).toInt()

private fun groupWords(words: List<CefrWord>, group: Int) = words.drop(group * CefrGroupSize).take(CefrGroupSize)

@Composable
fun CefrEntryCard(store: ProgressStore, onClick: () -> Unit) {
    val context = LocalContext.current
    val total = remember { CefrWords.all(context).size }
    val mastered = store.cefrMastered().size
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText("分级背词", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                AppText("A1 – C2 共 $total 词 · 每 $CefrGroupSize 词一组", color = InkFaint, fontSize = 13.sp)
                AppText("已掌握 $mastered", color = InkSoft, fontSize = 13.sp)
            }
            AppText("进入", color = CefrLevel.B1.tint, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CefrTopBar(title: String, subtitle: String, onBack: () -> Unit) {
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
        CurrentUserChip()
    }
}

@Composable
private fun ProgressBar(value: Float, level: CefrLevel) {
    LinearProgressIndicator(
        progress = value.coerceIn(0f, 1f),
        color = level.tint,
        trackColor = level.soft,
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(99.dp))
    )
}

@Composable
fun CefrHomeScreen(store: ProgressStore, onBack: () -> Unit, onLevel: (CefrLevel) -> Unit) {
    val context = LocalContext.current
    val all = remember { CefrWords.all(context) }
    val mastered = store.cefrMastered()
    Scaffold(containerColor = Paper, topBar = { CefrTopBar("分级背词", "按 CEFR 从低到高 · 共 ${all.size} 词", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle("选择等级", "C1 – C2 大约相当于普通母语者的词汇量") }
            items(CefrLevel.values().toList()) { level ->
                val levelWords = all.filter { it.level == level }
                val learned = levelWords.count { it.word in mastered }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onLevel(level) }
                        .border(1.dp, Line, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(CircleShape).background(level.soft),
                                contentAlignment = Alignment.Center
                            ) {
                                AppText(level.key, color = level.tint, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                AppText(level.zh, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                AppText(level.desc, color = InkFaint, fontSize = 12.sp)
                            }
                            AppText("${groupCount(levelWords.size)} 组", color = level.tint, fontWeight = FontWeight.Bold)
                        }
                        ProgressBar(if (levelWords.isEmpty()) 0f else learned / levelWords.size.toFloat(), level)
                        AppText("掌握 $learned / ${levelWords.size}", color = InkSoft, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CefrLevelScreen(
    level: CefrLevel,
    store: ProgressStore,
    zh: ChineseMode,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
    onGroup: (Int) -> Unit,
    onReview: () -> Unit
) {
    val context = LocalContext.current
    val words = remember(level) { CefrWords.of(context, level) }
    val mastered = store.cefrMastered()
    val mistakes = store.cefrMistakes()
    val groups = groupCount(words.size)
    val nextGroup = (0 until groups).firstOrNull { g -> groupWords(words, g).any { it.word !in mastered } }
    val mistakeCount = words.count { it.word in mistakes }
    var expanded by rememberSaveable(level) { mutableStateOf(-1) }
    var showList by rememberSaveable(level) { mutableStateOf(false) }
    var filter by rememberSaveable(level) { mutableStateOf(0) }
    val listed = when (filter) {
        1 -> words.filter { it.word !in mastered }
        2 -> words.filter { it.word in mistakes }
        else -> words
    }
    Scaffold(containerColor = Paper, topBar = {
        CefrTopBar("${level.title}", "${words.size} 词 · $groups 组 · 掌握 ${words.count { it.word in mastered }}", onBack)
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onGroup(nextGroup ?: 0) },
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = level.tint)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        AppText(if (nextGroup == null) "全部掌握，从头复习" else "继续：第 ${nextGroup + 1} 组", color = Color.White)
                    }
                    if (mistakeCount > 0) {
                        OutlinedButton(
                            onClick = onReview,
                            modifier = Modifier.height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = Error)
                            Spacer(Modifier.width(4.dp))
                            AppText("错词 $mistakeCount", color = Error)
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TogglePill("分组背词", !showList, { showList = false })
                    TogglePill("单词列表", showList, { showList = true })
                }
            }
            if (showList) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        TogglePill("全部 ${words.size}", filter == 0, { filter = 0 })
                        TogglePill("没掌握 ${words.count { it.word !in mastered }}", filter == 1, { filter = 1 })
                        TogglePill("错词 $mistakeCount", filter == 2, { filter = 2 })
                    }
                }
                if (listed.isEmpty()) item { EmptyCard("这里还没有单词。") }
                items(listed, key = { it.word }) { w ->
                    CefrWordRow(w, w.word in mastered, w.word in mistakes, zh, onSpeak)
                }
            }
            if (!showList) items((0 until groups).toList()) { g ->
                val gw = groupWords(words, g)
                val learned = gw.count { it.word in mastered }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                Modifier
                                    .weight(1f)
                                    .clickable { expanded = if (expanded == g) -1 else g }
                            ) {
                                AppText("第 ${g + 1} 组", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                AppText(
                                    "${gw.first().word} – ${gw.last().word} · 掌握 $learned / ${gw.size} · ${if (expanded == g) "收起" else "看单词"}",
                                    color = InkFaint,
                                    fontSize = 12.sp
                                )
                            }
                            Button(
                                onClick = { onGroup(g) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = if (learned == gw.size) level.soft else level.tint)
                            ) {
                                AppText(if (learned == gw.size) "复习" else "背这组", color = if (learned == gw.size) level.tint else Color.White)
                            }
                        }
                        ProgressBar(learned / gw.size.toFloat(), level)
                        if (expanded == g) {
                            gw.forEach { w -> CefrWordRow(w, w.word in mastered, w.word in mistakes, zh, onSpeak) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CefrWordRow(w: CefrWord, mastered: Boolean, mistake: Boolean, zh: ChineseMode, onSpeak: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onSpeak(w.word) }.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            w.word,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (mistake) Error else Ink,
            modifier = Modifier.width(140.dp)
        )
        AppText("${w.pos} ${convertZh(w.zh, zh)}", color = InkSoft, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (mastered) Icon(Icons.Default.Check, contentDescription = "已掌握", tint = Success, modifier = Modifier.size(18.dp))
    }
}

private enum class CefrQuizType(val title: String) { EnToZh("看英文，选中文"), ZhToEn("看中文，选英文"), Listen("听音选词") }

private class CefrQuestion(val type: CefrQuizType, val word: CefrWord, val options: List<String>, val answer: String)

private fun buildCefrQuestion(type: CefrQuizType, word: CefrWord, pool: List<CefrWord>): CefrQuestion {
    val random = Random(word.word.hashCode() * 31 + type.ordinal)
    val pos = word.pos.substringBefore('/')
    val others = pool.filter { it.word != word.word && it.zh != word.zh }.shuffled(random)
    val distractors = (others.filter { it.pos.substringBefore('/') == pos } + others).distinctBy { it.word }.take(3)
    val pick: (CefrWord) -> String = if (type == CefrQuizType.EnToZh) { w -> w.zh } else { w -> w.word }
    val options = (distractors.map(pick) + pick(word)).distinct().shuffled(random)
    return CefrQuestion(type, word, options, pick(word))
}

/**
 * 一组词：先逐个看卡片（复习错词时跳过），再逐题测验；答对记为掌握、答错进本级错词。
 * [group] 为 null 表示复习本级错词（最多 [CefrGroupSize] 个）。
 */
@Composable
fun CefrStudyScreen(
    level: CefrLevel,
    group: Int?,
    store: ProgressStore,
    zh: ChineseMode,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
    onNextGroup: (Int) -> Unit
) {
    val context = LocalContext.current
    val levelWords = remember(level) { CefrWords.of(context, level) }
    val source = remember(level, group) {
        if (group == null) store.cefrMistakes().let { m -> levelWords.filter { it.word in m } }.take(CefrGroupSize)
        else groupWords(levelWords, group)
    }
    val hasNext = group != null && (group + 1) * CefrGroupSize < levelWords.size
    // 0 看卡片，1 测验，2 结果
    var phase by rememberSaveable(level, group) { mutableStateOf(if (group == null) 1 else 0) }
    var index by rememberSaveable(level, group) { mutableStateOf(0) }
    var selected by rememberSaveable(level, group) { mutableStateOf<String?>(null) }
    var correctCount by rememberSaveable(level, group) { mutableStateOf(0) }
    val title = if (group == null) "${level.key} 错词复习" else "${level.key} · 第 ${group + 1} 组"
    val subtitle = when (phase) {
        0 -> "认一认 ${index + 1} / ${source.size}"
        1 -> "测一测 ${index + 1} / ${source.size} · 答对 $correctCount"
        else -> "完成"
    }
    val word = source.getOrNull(index)
    val quizType = CefrQuizType.values()[index % CefrQuizType.values().size]
    val question = remember(word, quizType, phase) { if (word != null && phase == 1) buildCefrQuestion(quizType, word, levelWords) else null }

    LaunchedEffect(phase, index) {
        if (word != null && (phase == 0 || (phase == 1 && quizType == CefrQuizType.Listen))) onSpeak(word.word)
    }

    fun advance() {
        if (index < source.lastIndex) {
            index++
            selected = null
        } else if (phase == 0) {
            phase = 1
            index = 0
            selected = null
        } else {
            phase = 2
        }
    }

    Scaffold(containerColor = Paper, topBar = { CefrTopBar(title, subtitle, onBack) }, bottomBar = {
        if (phase == 0 || (phase == 1 && selected != null)) {
            Surface(color = PaperElevated, shadowElevation = 8.dp) {
                Button(
                    onClick = ::advance,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = level.tint)
                ) {
                    Text(
                        when {
                            index < source.lastIndex -> "下一个"
                            phase == 0 -> "开始测一测"
                            else -> "看结果"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (word == null) {
                item { EmptyCard("没有要复习的错词了。") }
            } else if (phase == 2) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PaperElevated),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("$correctCount / ${source.size}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = level.tint)
                            AppText(
                                when {
                                    correctCount == source.size -> "全对，太棒了！"
                                    correctCount * 2 >= source.size -> "不错！答错的词进了错词本"
                                    else -> "再来一遍会更熟"
                                },
                                color = InkSoft
                            )
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) { Text("返回", fontSize = 18.sp) }
                        if (hasNext) {
                            Button(
                                onClick = { onNextGroup(group!! + 1) },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = level.tint)
                            ) { Text("下一组", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                item { ProgressBar((index + if (phase == 0 || selected != null) 1 else 0) / source.size.toFloat(), level) }
                if (phase == 0) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PaperElevated),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                        ) {
                            Column(
                                Modifier.padding(28.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(word.word, fontSize = 38.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, color = Ink)
                                AppText(word.pos, color = level.tint, fontWeight = FontWeight.Bold)
                                Text(convertZh(word.zh, zh), fontSize = 22.sp, color = InkSoft)
                                IconButton(onClick = { onSpeak(word.word) }, modifier = Modifier.size(56.dp)) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "播放", tint = level.tint, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                    }
                } else if (question != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = PaperElevated),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                        ) {
                            Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                AppText(question.type.title, color = level.tint, fontWeight = FontWeight.Bold)
                                when (question.type) {
                                    CefrQuizType.EnToZh -> Text(word.word, fontSize = 30.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold)
                                    CefrQuizType.ZhToEn -> Text("${word.pos} ${convertZh(word.zh, zh)}", fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                                    CefrQuizType.Listen -> Button(
                                        onClick = { onSpeak(word.word) },
                                        shape = CircleShape,
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.size(64.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = level.tint)
                                    ) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "播放", modifier = Modifier.size(34.dp))
                                    }
                                }
                            }
                        }
                    }
                    items(question.options) { option ->
                        val answered = selected != null
                        val correctThis = answered && option == question.answer
                        val wrongThis = answered && option == selected && option != question.answer
                        OutlinedButton(
                            onClick = {
                                if (!answered) {
                                    selected = option
                                    val ok = option == question.answer
                                    if (ok) correctCount++
                                    store.recordCefr(word.word, ok)
                                    onSpeak(word.word)
                                } else if (correctThis) {
                                    onSpeak(word.word)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = when {
                                    correctThis -> Color(0xFFDCFCE7)
                                    wrongThis -> Color(0xFFFEE2E2)
                                    else -> PaperElevated
                                }
                            ),
                            border = BorderStroke(1.dp, Line)
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(convertZh(option, zh), modifier = Modifier.weight(1f), fontSize = 18.sp, color = Ink)
                                if (correctThis) Icon(Icons.Default.Check, contentDescription = null, tint = Success)
                                if (wrongThis) Icon(Icons.Default.Close, contentDescription = null, tint = Error)
                            }
                        }
                    }
                    if (selected != null) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = if (selected == question.answer) Color(0xFFECFDF5) else Color(0xFFFEF2F2)),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AppText(
                                        if (selected == question.answer) "答对了！" else "这个词先记到错词本",
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected == question.answer) Success else Error
                                    )
                                    Text("${word.word} · ${word.pos} ${convertZh(word.zh, zh)}", fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
