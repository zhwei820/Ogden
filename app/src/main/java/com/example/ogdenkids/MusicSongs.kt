package com.example.ogdenkids

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** [degree] 为简谱数字（0 = 休止），[octave] 为高低八度点数 */
data class SongNote(val midi: Int?, val beats: Double, val degree: Int, val octave: Int, val accidental: String, val barAfter: Boolean)

/**
 * [notation] 是 C 调简谱：`5,` 低音、`1'` 高音、`#4` / `b7` 升降，
 * `_` 减半（八分）、`__` 十六分、`.` 附点、每个 `-` 延长一拍，`|` 小节线，`0` 休止。
 */
class Song(val key: String, val title: String, val origin: String, val level: Int, val meter: String, val bpm: Int, notation: String) {
    val notes: List<SongNote> = parseJianpu(notation)
}

private val MajorOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
private val NoteToken = Regex("""([#b]?)([0-7])([',]*)([_.\-]*)""")

fun parseJianpu(notation: String): List<SongNote> {
    val notes = mutableListOf<SongNote>()
    notation.trim().split(Regex("\\s+")).forEach { token ->
        if (token == "|") {
            notes.lastOrNull()?.let { notes[notes.lastIndex] = it.copy(barAfter = true) }
            return@forEach
        }
        val m = NoteToken.matchEntire(token) ?: error("简谱写错了：$token")
        val (acc, d, oct, dur) = m.destructured
        val degree = d.toInt()
        val octave = oct.count { it == '\'' } - oct.count { it == ',' }
        var beats = 1.0
        repeat(dur.count { it == '_' }) { beats /= 2 }
        if ('.' in dur) beats *= 1.5
        beats += dur.count { it == '-' }
        val shift = when (acc) { "#" -> 1; "b" -> -1; else -> 0 }
        val midi = if (degree == 0) null else 60 + MajorOffsets[degree - 1] + 12 * octave + shift
        notes += SongNote(midi, beats, degree, octave, acc.replace("#", "♯").replace("b", "♭"), false)
    }
    return notes
}

/** 只收录公版的传统民歌、儿歌和古典旋律 */
val Songs = listOf(
    Song("buns", "热十字面包", "英国童谣", 1, "4/4", 100,
        "3 2 1- | 3 2 1- | 1_ 1_ 1_ 1_ 2_ 2_ 2_ 2_ | 3 2 1- |"),
    Song("moon", "月光下", "法国民歌", 1, "4/4", 100,
        "1 1 1 2 | 3- 2- | 1 3 2 2 | 1--- | 1 1 1 2 | 3- 2- | 1 3 2 2 | 1--- |"),
    Song("twinkle", "小星星", "法国民歌", 1, "4/4", 100,
        "1 1 5 5 | 6 6 5- | 4 4 3 3 | 2 2 1- | 5 5 4 4 | 3 3 2- | 5 5 4 4 | 3 3 2- | 1 1 5 5 | 6 6 5- | 4 4 3 3 | 2 2 1- |"),
    Song("mary", "玛丽有只小羊羔", "美国童谣", 1, "4/4", 110,
        "3 2 1 2 | 3 3 3- | 2 2 2- | 3 5 5- | 3 2 1 2 | 3 3 3 3 | 2 2 3 2 | 1--- |"),
    Song("tigers", "两只老虎", "法国民歌", 1, "4/4", 120,
        "1 2 3 1 | 1 2 3 1 | 3 4 5- | 3 4 5- | 5_ 6_ 5_ 4_ 3 1 | 5_ 6_ 5_ 4_ 3 1 | 1 5, 1- | 1 5, 1- |"),
    Song("bee", "小蜜蜂", "德国民歌", 1, "4/4", 110,
        "5 3 3- | 4 2 2- | 1 2 3 4 | 5 5 5- | 5 3 3- | 4 2 2- | 1 3 5 5 | 3--- | 2 2 2 2 | 2 3 4- | 3 3 3 3 | 3 4 5- | 5 3 3- | 4 2 2- | 1 3 5 5 | 1--- |"),
    Song("joy", "欢乐颂", "贝多芬", 2, "4/4", 110,
        "3 3 4 5 | 5 4 3 2 | 1 1 2 3 | 3. 2_ 2- | 3 3 4 5 | 5 4 3 2 | 1 1 2 3 | 2. 1_ 1- | 2 2 3 1 | 2 3_ 4_ 3 1 | 2 3_ 4_ 3 2 | 1 2 5,- | 3 3 4 5 | 5 4 3 2 | 1 1 2 3 | 2. 1_ 1- |"),
    Song("farm", "王老先生有块地", "美国民歌", 2, "4/4", 120,
        "1 1 1 5, | 6, 6, 5,- | 3 3 2 2 | 1-- 5, | 1 1 1 5, | 6, 6, 5,- | 3 3 2 2 | 1--- |"),
    Song("bridge", "伦敦大桥", "英国童谣", 2, "4/4", 110,
        "5. 6_ 5 4 | 3 4 5- | 2 3 4- | 3 4 5- | 5. 6_ 5 4 | 3 4 5- | 2- 5- | 3 1-- |"),
    Song("newyear", "新年好", "英国民歌", 2, "3/4", 120,
        "1_ 1_ 1 5, | 3_ 3_ 3 1 | 1_ 3_ 5 5 | 4_ 3_ 2- | 2_ 3_ 4 4 | 3_ 2_ 3 1 | 1_ 3_ 2 5, | 7,_ 2_ 1- |"),
    Song("painter", "粉刷匠", "波兰民歌", 2, "2/4", 110,
        "5_ 3_ 5_ 3_ | 5_ 3_ 1 | 2_ 4_ 3_ 2_ | 5- | 5_ 3_ 5_ 3_ | 5_ 3_ 1 | 2_ 4_ 3_ 2_ | 1- | 2_ 2_ 4_ 4_ | 3_ 1_ 5 | 2_ 4_ 3_ 2_ | 5- | 5_ 3_ 5_ 3_ | 5_ 3_ 1 | 2_ 4_ 3_ 2_ | 1- |"),
    Song("yankee", "扬基歌", "美国民歌", 2, "4/4", 120,
        "1 1 2 3 | 1 3 2 5, | 1 1 2 3 | 1- 7,- | 1 1 2 3 | 4 3 2 1 | 7, 5, 6, 7, | 1- 1- |"),
    Song("birthday", "生日快乐", "美国民歌", 2, "3/4", 100,
        "5,_ 5,_ | 6, 5, 1 | 7,- 5,_ 5,_ | 6, 5, 2 | 1- 5,_ 5,_ | 5 3 1 | 7, 6, 4_ 4_ | 3 1 2 | 1-- |"),
    Song("boat", "划小船", "美国童谣", 3, "6/8", 100,
        "1. 1. | 1 2_ 3. | 3 2_ 3 4_ | 5-- | 1'_ 1'_ 1'_ 5_ 5_ 5_ | 3_ 3_ 3_ 1_ 1_ 1_ | 5 4_ 3 2_ | 1-- |"),
    Song("jingle", "铃儿响叮当", "美国民歌", 3, "4/4", 120,
        "3 3 3- | 3 3 3- | 3 5 1. 2_ | 3--- | 4 4 4. 4_ | 4 3 3 3_ 3_ | 3 2 2 3 | 2- 5- | 3 3 3- | 3 3 3- | 3 5 1. 2_ | 3--- | 4 4 4 4 | 4 3 3 3_ 3_ | 5 5 4 2 | 1--- |"),
    Song("christmas", "祝你圣诞快乐", "英国民歌", 3, "3/4", 120,
        "5, | 1 1_ 2_ 1_ 7,_ | 6, 6, 6, | 2 2_ 3_ 2_ 1_ | 7, 5, 5, | 3 3_ 4_ 3_ 2_ | 1 6, 5,_ 5,_ | 6, 2 7, | 1-- |"),
    Song("farewell", "送别", "奥德威 曲", 3, "4/4", 76,
        "5 3_ 5_ 1'- | 6 1' 5- | 5 1_ 2_ 3 2_ 1_ | 2--- | 5 3_ 5_ 1'. 7_ | 6 1' 5- | 5 2_ 3_ 4. 7,_ | 1--- |"),
    Song("jasmine", "茉莉花", "江苏民歌", 3, "4/4", 76,
        "3 3_ 5_ 6_ 1'_ 1'_ 6_ | 5 5_ 6_ 5- | 3 3_ 5_ 6_ 1'_ 1'_ 6_ | 5 5_ 6_ 5- | 5 5 5 3_ 5_ | 6 6 5- | 3 2_ 3_ 5 3_ 2_ | 1 1_ 2_ 1- |")
)

private fun songStars(stars: Int?) = when (stars) {
    null -> ""
    else -> " · " + "⭐".repeat(stars)
}

@Composable
fun MusicSongListScreen(store: ProgressStore, onSong: (Song) -> Unit, onBack: () -> Unit) {
    Scaffold(containerColor = Paper, topBar = { MusicTopBar("🎹 跟着弹", "${Songs.size} 首儿歌 · 先听一遍，再跟着弹", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Songs) { song ->
                MusicEntryCard(
                    badge = "${Songs.indexOf(song) + 1}",
                    title = song.title,
                    subtitle = "${song.origin} · ${"★".repeat(song.level)}${"☆".repeat(3 - song.level)} · ${song.meter}" +
                        songStars(store.bestSpecialScore("song-${song.key}", 0)),
                    onClick = { onSong(song) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicSongScreen(song: Song, store: ProgressStore, onBack: () -> Unit) {
    val synth = rememberToneSynth()
    val notes = song.notes
    fun nextPlayable(from: Int) = (from until notes.size).firstOrNull { notes[it].midi != null } ?: notes.size
    var pos by remember { mutableStateOf(nextPlayable(0)) }
    var mistakes by remember { mutableStateOf(0) }
    var wrongKey by remember { mutableStateOf<Int?>(null) }
    var demo by remember { mutableStateOf(false) }
    var demoPos by remember { mutableStateOf(-1) }
    var hint by remember { mutableStateOf(true) }
    var names by remember { mutableStateOf(true) }
    val finished = pos >= notes.size
    val current = if (demo) demoPos else pos

    val midis = notes.mapNotNull { it.midi }
    // 键盘至少一个八度，从歌里最低的白键开始
    val low = minOf(60, midis.min()).let { if (isBlackKey(it)) it - 1 else it }
    val high = maxOf(72, midis.max()).let { if (isBlackKey(it)) it + 1 else it }

    fun restart() {
        demo = false
        synth.stop()
        pos = nextPlayable(0)
        mistakes = 0
    }

    LaunchedEffect(demo) {
        if (!demo) return@LaunchedEffect
        val beat = 60_000L / song.bpm
        for (i in notes.indices) {
            demoPos = i
            notes[i].midi?.let { synth.note(it, notes[i].beats * beat / 1000.0 * 0.95) }
            delay((notes[i].beats * beat).toLong())
        }
        demo = false
        demoPos = -1
    }
    LaunchedEffect(wrongKey) {
        if (wrongKey != null) { delay(500); wrongKey = null }
    }
    LaunchedEffect(finished) {
        if (finished) store.saveSpecialScore("song-${song.key}", 0, starsFor(mistakes))
    }

    fun press(midi: Int) {
        if (demo) { demo = false; demoPos = -1 }
        synth.note(midi)
        if (finished) return
        if (midi == notes[pos].midi) pos = nextPlayable(pos + 1) else { mistakes++; wrongKey = midi }
    }

    val landscape = keyboardScreenLandscape()
    val status = when {
        demo -> "听一听，看键盘上亮起的键"
        finished -> "弹完啦！错了 $mistakes 次 " + "⭐".repeat(starsFor(mistakes))
        else -> "第 ${notes.take(pos).count { it.midi != null } + 1} / ${midis.size} 个音 · 错 $mistakes 次"
    }
    Scaffold(containerColor = Paper, topBar = {
        if (!landscape) MusicTopBar("🎵 ${song.title}", "${song.origin} · ${song.meter} · ♩=${song.bpm}", onBack, rotatable = true)
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = if (landscape) 8.dp else 12.dp, vertical = if (landscape) 4.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(if (landscape) 4.dp else 12.dp)
        ) {
            if (!landscape) JianpuStrip(notes, current, played = if (demo) -1 else pos)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { if (demo) { demo = false; synth.stop() } else { restart(); demo = true } }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(if (demo) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                    AppText(if (demo) "停止" else "听一遍")
                }
                OutlinedButton(onClick = { restart() }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    AppText("重弹")
                }
                FilterChip(selected = hint, onClick = { hint = !hint }, label = { AppText("提示") })
                FilterChip(selected = names, onClick = { names = !names }, label = { AppText("唱名") })
                if (landscape) {
                    AppText(
                        "${song.title} · $status",
                        color = if (finished && !demo) Success else InkSoft,
                        fontSize = 13.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    LandscapeToggle()
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "返回") }
                }
            }
            if (landscape) JianpuStrip(notes, current, played = if (demo) -1 else pos, compact = true)
            else AppText(
                if (demo || finished) status else "跟着弹：$status",
                color = if (finished && !demo) Success else InkSoft,
                fontWeight = if (finished) FontWeight.Bold else FontWeight.Normal
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                val target = notes.getOrNull(current)?.midi
                val marks = buildMap {
                    if (target != null && (demo || hint)) put(target, if (demo) MusicSoft else RightSoft)
                    wrongKey?.let { put(it, WrongSoft) }
                }
                PianoKeyboard(
                    low, high,
                    // 竖屏键太长不好按，限高；横屏剩多少给多少
                    modifier = (if (landscape) Modifier else Modifier.heightIn(max = 260.dp)).fillMaxHeight(),
                    height = Dp.Unspecified,
                    label = { m -> if (names) degreeOf(m)?.let { Solfege[it] } else null },
                    marks = marks,
                    onKey = ::press
                )
            }
        }
    }
}

private fun starsFor(mistakes: Int) = when {
    mistakes == 0 -> 3
    mistakes <= 3 -> 2
    else -> 1
}

/** 简谱条：当前音高亮，已弹过的变淡，自动滚到当前位置 */
@Composable
private fun JianpuStrip(notes: List<SongNote>, current: Int, played: Int, compact: Boolean = false) {
    val state = rememberLazyListState()
    LaunchedEffect(current) {
        if (current >= 0) state.animateScrollToItem(maxOf(0, current - 3))
    }
    LazyRow(
        state = state,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(PaperElevated)
            .padding(vertical = if (compact) 0.dp else 6.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        itemsIndexed(notes) { i, note ->
            val active = i == current
            val color = when {
                active -> Color.White
                i < played -> InkFaint
                else -> Ink
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) MusicTint else Color.Transparent)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (note.octave > 0) "•" else " ", fontSize = 9.sp, color = color, lineHeight = 9.sp)
                    val dashes = " –".repeat(maxOf(0, note.beats.toInt() - 1))
                    val dot = if (note.beats == 1.5 || note.beats == 0.75) "·" else ""
                    Text("${note.accidental}${note.degree}$dot$dashes", fontSize = if (compact) 17.sp else 22.sp, fontWeight = FontWeight.Bold, color = color)
                    // 减时线：八分音符一条、十六分两条
                    val lines = when {
                        note.beats < 0.5 -> 2
                        note.beats < 1 -> 1
                        else -> 0
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.height(6.dp)) {
                        repeat(lines) { Box(Modifier.width(12.dp).height(2.dp).background(color)) }
                    }
                    Text(if (note.octave < 0) "•" else " ", fontSize = 9.sp, color = color, lineHeight = 9.sp)
                }
                if (note.barAfter) Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(if (compact) 28.dp else 36.dp).background(Line))
            }
        }
    }
}
