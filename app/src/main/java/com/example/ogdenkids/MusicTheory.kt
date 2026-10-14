package com.example.ogdenkids

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

internal val MusicTint = Color(0xFF7C3AED)
internal val MusicSoft = Color(0xFFEDE9FE)
internal val RightSoft = Color(0xFFDCFCE7)
internal val WrongSoft = Color(0xFFFEE2E2)

const val MusicRoundSize = 10

enum class MusicGame(val key: String, val emoji: String, val title: String, val desc: String, val hasKeyboard: Boolean = false) {
    Names("names", "🔤", "唱名对对碰", "唱名、音名、简谱互相配对"),
    FindKey("key", "🎹", "找琴键", "看唱名，在键盘上找到它", hasKeyboard = true),
    ReadStaff("staff", "🎼", "认五线谱", "看五线谱上的音符，选出唱名"),
    HighLow("pitch", "👂", "听音高低", "听两个音，第二个更高还是更低"),
    NoteValue("value", "⏱️", "音符拍数", "看音符或休止符，说出几拍"),
    Meter("meter", "🥁", "听拍子", "听强弱节拍，判断几拍子"),
    Chord("chord", "🎭", "听和弦", "大三和弦明亮，小三和弦柔和"),
    Ear("ear", "🎵", "听音认唱名", "先听 do，再听一个音，猜唱名")
}

enum class MusicLesson(val emoji: String, val title: String, val summary: String, val practice: MusicGame?, val hasKeyboard: Boolean = false) {
    Notes("🎵", "七个基本音", "do re mi fa sol la si，音名和简谱", MusicGame.Names),
    Keyboard("🎹", "认识钢琴键盘", "白键黑键，找到 do，什么是八度", MusicGame.FindKey, hasKeyboard = true),
    Pitch("👂", "音的高低", "高音、低音，旋律往上走还是往下走", MusicGame.HighLow),
    Staff("🎼", "五线谱与高音谱号", "线和间，音符住在哪里", MusicGame.ReadStaff),
    Values("⏱️", "音符与时值", "全音符、二分、四分、八分、十六分音符", MusicGame.NoteValue),
    Rests("🤫", "休止符", "音乐里的安静时刻", MusicGame.NoteValue),
    Meter("🥁", "节拍与拍号", "2/4、3/4、4/4 拍的强弱规律", MusicGame.Meter),
    Accidentals("♯", "升号、降号与半音", "黑键的名字，半音和全音", MusicGame.FindKey, hasKeyboard = true),
    Scale("🪜", "C 大调音阶", "全全半全全全半", MusicGame.Ear),
    Intervals("📏", "音程", "两个音之间的距离", MusicGame.Ear),
    Chords("🎭", "三和弦", "大三和弦明亮，小三和弦柔和", MusicGame.Chord, hasKeyboard = true),
    Expression("🚀", "速度与力度", "Allegro、Andante，p 与 f", null)
}

/** 小星星：一闪一闪亮晶晶 */
private val Twinkle = listOf(60, 60, 67, 67, 69, 69, 67, 65, 65, 64, 64, 62, 62, 60)
    .mapIndexed { i, m -> m to if (i == 6 || i == 13) 2.0 else 1.0 }

@Composable
internal fun rememberToneSynth(): ToneSynth {
    val synth = remember { ToneSynth() }
    DisposableEffect(synth) { onDispose { synth.release() } }
    return synth
}

@Composable
fun MusicHomeScreen(store: ProgressStore, padding: PaddingValues, onLesson: (MusicLesson) -> Unit, onGame: (MusicGame) -> Unit, onSongs: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            HeroCard(
                title = "乐理小课堂",
                subtitle = "${MusicLesson.values().size} 节乐理课 · ${MusicGame.values().size} 个听音认谱小游戏",
                action = "从第一课开始",
                onAction = { onLesson(MusicLesson.Notes) },
                note = "每节课都能点一点、听一听"
            )
        }
        item { SectionTitle("乐理课堂", "从认识音符到三和弦，按顺序学最轻松") }
        items(MusicLesson.values().toList()) { lesson ->
            MusicEntryCard(
                badge = "${lesson.ordinal + 1}",
                title = "${lesson.emoji} ${lesson.title}",
                subtitle = lesson.summary,
                onClick = { onLesson(lesson) }
            )
        }
        item { SectionTitle("跟着弹", "先听一遍，再看着简谱在键盘上弹") }
        item {
            MusicEntryCard(
                badge = "🎹",
                title = "儿歌弹奏",
                subtitle = "${Songs.size} 首儿歌 · 已弹完 ${Songs.count { store.bestSpecialScore("song-${it.key}", 0) != null }} 首",
                onClick = onSongs
            )
        }
        item { SectionTitle("乐理小游戏", "每局 $MusicRoundSize 题，边玩边记") }
        items(MusicGame.values().toList()) { game ->
            val best = store.bestSpecialScore("music-${game.key}", 0)
            val perfect = NameGroups.indices.count { store.bestSpecialScore("music-${game.key}", it) == MusicRoundSize }
            MusicEntryCard(
                badge = game.emoji,
                title = game.title,
                subtitle = game.desc + when {
                    game == MusicGame.Names -> " · ${NameGroups.size} 组，满分 $perfect 组"
                    best != null -> " · 最好 $best / $MusicRoundSize"
                    else -> ""
                },
                onClick = { onGame(game) }
            )
        }
    }
}

@Composable
internal fun MusicEntryCard(badge: String, title: String, subtitle: String, onClick: () -> Unit, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(PaperElevated)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MusicSoft),
            contentAlignment = Alignment.Center
        ) { Text(badge, color = MusicTint, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
        Column(Modifier.weight(1f)) {
            AppText(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            AppText(subtitle, color = InkFaint, fontSize = 13.sp)
        }
        trailing?.invoke()
    }
}

@Composable
internal fun MusicTopBar(title: String, subtitle: String, onBack: () -> Unit, rotatable: Boolean = false) {
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
        if (rotatable) LandscapeToggle()
        CurrentUserChip()
    }
}

/** 键盘竖屏下琴键太窄，允许手动横过来；方向由 [keyboardScreenLandscape] 在离开页面时恢复 */
@Composable
internal fun LandscapeToggle() {
    val activity = LocalContext.current as? Activity ?: return
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    IconButton(onClick = {
        activity.requestedOrientation =
            if (landscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }) {
        Icon(Icons.Default.ScreenRotation, contentDescription = if (landscape) "恢复竖屏" else "横屏", tint = if (landscape) MusicTint else InkSoft)
    }
}

/**
 * 带钢琴键盘的页面调用：横屏时隐藏系统状态栏 / 导航栏，把空间都给键盘（页面自己也不画顶栏）；
 * 离开页面恢复系统栏和跟随系统的方向。返回当前是否横屏。
 */
@Composable
internal fun keyboardScreenLandscape(): Boolean {
    val activity = LocalContext.current as? Activity
    val view = LocalView.current
    val landscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    DisposableEffect(activity) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    DisposableEffect(activity, landscape) {
        val controller = activity?.window?.let { WindowCompat.getInsetsController(it, view) }
        if (landscape) {
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller?.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    return landscape
}

/** 横屏去掉顶栏后，返回和转屏按钮浮在右上角 */
@Composable
internal fun LandscapeCorner(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(PaperElevated.copy(alpha = 0.9f))
    ) {
        LandscapeToggle()
        IconButton(onClick = onBack) { Icon(Icons.Default.Close, contentDescription = "返回") }
    }
}

// ---------------------------------------------------------------- 课堂

@Composable
fun MusicLessonScreen(lesson: MusicLesson, onLesson: (MusicLesson) -> Unit, onPractice: (MusicGame) -> Unit, onBack: () -> Unit) {
    val synth = rememberToneSynth()
    val lessons = MusicLesson.values()
    val landscape = if (lesson.hasKeyboard) keyboardScreenLandscape() else false
    Box {
    Scaffold(containerColor = Paper, topBar = {
        if (!landscape) MusicTopBar("${lesson.emoji} ${lesson.title}", "第 ${lesson.ordinal + 1} / ${lessons.size} 课 · ${lesson.summary}", onBack, rotatable = lesson.hasKeyboard)
    }) { padding ->
        // 换课时回到顶部
        val scroll = remember(lesson) { androidx.compose.foundation.ScrollState(0) }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scroll)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LessonContent(lesson, synth)
            lesson.practice?.let { game ->
                Button(
                    onClick = { synth.stop(); onPractice(game) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MusicTint)
                ) { AppText("去练习：${game.emoji} ${game.title}", fontWeight = FontWeight.Bold) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                lessons.getOrNull(lesson.ordinal - 1)?.let { prev ->
                    OutlinedButton(onClick = { synth.stop(); onLesson(prev) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        AppText("← ${prev.title}", maxLines = 1)
                    }
                } ?: Spacer(Modifier.weight(1f))
                lessons.getOrNull(lesson.ordinal + 1)?.let { next ->
                    OutlinedButton(onClick = { synth.stop(); onLesson(next) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                        AppText("${next.title} →", maxLines = 1)
                    }
                } ?: Spacer(Modifier.weight(1f))
            }
        }
    }
    if (landscape) LandscapeCorner(onBack, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun LessonCard(title: String? = null, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            title?.let { AppText(it, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp) }
            content()
        }
    }
}

@Composable
private fun Para(text: String) = AppText(text, color = InkSoft, fontSize = 16.sp, lineHeight = 25.sp)

@Composable
private fun Tip(text: String) {
    AppText(
        "💡 $text",
        color = MusicTint,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MusicSoft)
            .padding(10.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlayButtons(vararg buttons: Pair<String, () -> Unit>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        buttons.forEach { (label, onClick) ->
            OutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                AppText(label, fontSize = 14.sp)
            }
        }
    }
}

/** 两三列的小表格 */
@Composable
private fun InfoTable(header: List<String>, rows: List<List<String>>, onRow: ((Int) -> Unit)? = null) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
    ) {
        Row(Modifier.background(MusicSoft).padding(vertical = 8.dp)) {
            header.forEach { AppText(it, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        rows.forEachIndexed { i, row ->
            Row(
                Modifier
                    .then(if (onRow != null) Modifier.clickable { onRow(i) } else Modifier)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { AppText(it, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun LessonContent(lesson: MusicLesson, synth: ToneSynth) {
    when (lesson) {
        MusicLesson.Notes -> {
            LessonCard("音乐里的七兄弟") {
                Para("所有好听的歌，都是由一些「音」排在一起组成的。最基本的音只有七个，它们一个比一个高，就像七级台阶。")
                Para("唱歌时我们叫它们 do、re、mi、fa、sol、la、si，这叫「唱名」；钢琴和五线谱上用字母 C、D、E、F、G、A、B，这叫「音名」；简谱里写成数字 1 到 7。")
                InfoTable(
                    listOf("唱名", "音名", "简谱"),
                    Solfege.indices.map { listOf(Solfege[it], NoteLetters[it], "${it + 1}") }
                ) { synth.note(CMajorMidis[it]) }
                Tip("点表格里的任意一行，听听这个音。")
            }
            LessonCard("唱完一圈又回到 do") {
                Para("si 再往上一级，又是一个 do，只是更高。音就这样一圈一圈循环下去：… la si do re mi …")
                PlayButtons(
                    "do 到高音 do" to { synth.play(melody(CMajorMidis.map { it to 1.0 } + (72 to 2.0), bpm = 120)) },
                    "小星星" to { synth.play(melody(Twinkle, bpm = 110)) }
                )
                Para("《小星星》用简谱写出来是：")
                Text(
                    "1 1 5 5 | 6 6 5 – | 4 4 3 3 | 2 2 1 –",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MusicTint,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MusicSoft)
                        .padding(vertical = 10.dp)
                )
            }
        }
        MusicLesson.Keyboard -> {
            LessonCard("白键和黑键") {
                Para("钢琴上有白键也有黑键。黑键是两个一组、三个一组交替排列的。")
                Para("找 do 的秘诀：两个黑键一组，它左下方的白键就是 do（C）。")
                PianoKeyboard(48, 71, label = { m -> degreeOf(m)?.let { if (it == 0) "do" else null } }, onKey = { synth.note(it) })
                Tip("每组两个黑键左边都有一个 do，试试从左往右依次点它们，听听有什么不同。")
            }
            LessonCard("一个八度") {
                Para("从一个 do 到下一个 do，一共跨过 8 个白键，这个距离叫「八度」。两个 do 名字一样、听起来很像，只是一高一低。")
                PianoKeyboard(60, 72, label = { m -> degreeOf(m)?.let { Solfege[it] } }, onKey = { synth.note(it) })
                PlayButtons(
                    "低 do 和高 do" to { synth.play(listOf(Tone(0.0, 0.8, 60), Tone(0.9, 0.8, 72))) },
                    "一起弹" to { synth.play(chord(listOf(60, 72))) }
                )
                Para("钢琴最中间的那个 do 叫「中央 C」，我们后面学五线谱都从它开始。")
            }
        }
        MusicLesson.Pitch -> {
            LessonCard("高音和低音") {
                Para("音有高有低。小鸟叫声很高，大象的叫声很低。在钢琴上，越往右边音越高，越往左边音越低。")
                PlayButtons(
                    "很低的音" to { synth.note(36, 1.2) },
                    "中间的音" to { synth.note(60, 1.2) },
                    "很高的音" to { synth.note(84, 1.2) }
                )
            }
            LessonCard("旋律往哪里走") {
                Para("一个一个往上走的旋律叫「上行」，像爬楼梯；一个一个往下走叫「下行」，像滑滑梯。")
                PlayButtons(
                    "上行 ↗" to { synth.play(melody(CMajorMidis.map { it to 0.5 }, bpm = 100)) },
                    "下行 ↘" to { synth.play(melody(CMajorMidis.reversed().map { it to 0.5 }, bpm = 100)) },
                    "跳进 ⤴" to { synth.play(melody(listOf(60 to 1.0, 67 to 1.0, 64 to 1.0, 72 to 2.0), bpm = 100)) }
                )
                Tip("闭上眼睛听：两个音，第二个比第一个高还是低？这就是「听音高低」小游戏。")
            }
        }
        MusicLesson.Staff -> {
            LessonCard("五条线的家") {
                Para("五线谱有五条横线。从下往上数，分别叫第一线、第二线……第五线；线和线之间的空隙叫「间」，一共四个间。")
                Para("音符就住在线上或者间里。位置越高，音就越高。")
                StaffView(listOf(StaffSymbol.Note(0, NoteKind.Whole), StaffSymbol.Note(2, NoteKind.Whole), StaffSymbol.Note(4, NoteKind.Whole), StaffSymbol.Note(6, NoteKind.Whole), StaffSymbol.Note(8, NoteKind.Whole)), clef = false) { synth.note(listOf(64, 67, 71, 74, 77)[it]) }
                Para("线上的音：E G B D F")
                StaffView(listOf(StaffSymbol.Note(1, NoteKind.Whole), StaffSymbol.Note(3, NoteKind.Whole), StaffSymbol.Note(5, NoteKind.Whole), StaffSymbol.Note(7, NoteKind.Whole)), clef = false) { synth.note(listOf(65, 69, 72, 76)[it]) }
                Para("间里的音：F A C E")
            }
            LessonCard("高音谱号") {
                Para("五线谱最前面那个弯弯绕绕的符号叫「高音谱号」，也叫 G 谱号。它的圈圈绕着第二线，告诉我们：第二线是 G（sol）。知道了一个，别的就能数出来。")
                Para("中央 C（do）比五条线还低，要在下面加一条短线，叫「下加一线」。")
                val notes = (0..7).map { listOf(60, 62, 64, 65, 67, 69, 71, 72)[it] }
                StaffView(notes.map { StaffSymbol.Note(staffStep(it)) }) { synth.note(notes[it]) }
                Para("do re mi fa sol la si do —— 点一下五线谱上的音符听一听。")
                Tip("记住：do 在下加一线，sol 在第二线，高音 do 在第三间。")
            }
        }
        MusicLesson.Values -> {
            LessonCard("音符的长短") {
                Para("音不但有高低，还有长短。音符的样子告诉我们这个音要唱多久。我们用「拍」来数长短，就像跟着心跳一下一下地数。")
                val beat = 60.0 / 80
                NoteKind.values().forEach { kind ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StaffView(listOf(StaffSymbol.Note(3, kind)), clef = false, lines = false, modifier = Modifier.width(56.dp))
                        Column(Modifier.weight(1f)) {
                            AppText(kind.zh, fontWeight = FontWeight.Bold)
                            AppText("唱 ${kind.beats}", color = InkFaint, fontSize = 13.sp)
                        }
                        IconButton(onClick = {
                            // 一拍敲一下节拍器；短于一拍的音符在一拍里连着弹几个
                            val beats = maxOf(1, kind.beatValue.toInt())
                            val count = if (kind.beatValue < 1) (1 / kind.beatValue).toInt() else 1
                            val notes = (0 until count).map { Tone(it * kind.beatValue * beat, kind.beatValue * beat * 0.9, 69) }
                            synth.play(notes + metronome(beats, 80, 4))
                        }) { Icon(Icons.Default.VolumeUp, contentDescription = "听", tint = MusicTint) }
                    }
                }
            }
            LessonCard("一个比一个短一半") {
                Para("全音符 = 2 个二分音符 = 4 个四分音符 = 8 个八分音符 = 16 个十六分音符。")
                Para("空心没有杆的是全音符；空心加符杆是二分音符；涂黑加符杆是四分音符；再加一条小尾巴（符尾）是八分音符，两条尾巴是十六分音符。")
                StaffView(Twinkle.map { (m, beats) -> StaffSymbol.Note(staffStep(m), if (beats == 2.0) NoteKind.Half else NoteKind.Quarter) }.take(7))
                PlayButtons("小星星第一句" to { synth.play(melody(Twinkle.take(7), bpm = 100)) })
                Tip("「亮晶晶」的「晶」是二分音符，要唱两拍。")
            }
        }
        MusicLesson.Rests -> {
            LessonCard("安静也是音乐") {
                Para("音乐里有时候要停一停、休息一下，用「休止符」表示。休止符和音符一一对应，停的时间一样长。")
                listOf(NoteKind.Whole, NoteKind.Half, NoteKind.Quarter, NoteKind.Eighth).forEach { kind ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StaffView(listOf(StaffSymbol.Rest(kind)), clef = false, modifier = Modifier.width(56.dp), gap = 8.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            AppText(kind.zh.replace("音符", "休止符"), fontWeight = FontWeight.Bold)
                            AppText("停 ${kind.beats}", color = InkFaint, fontSize = 13.sp)
                        }
                    }
                }
                Tip("全休止符像倒挂的帽子，挂在第四线下面；二分休止符像正戴的帽子，坐在第三线上面。")
            }
            LessonCard("听一听休止") {
                Para("下面这句：唱、唱、停、唱。第三拍是四分休止符，什么也不弹，但节拍器照样数着。")
                StaffView(listOf(StaffSymbol.Note(staffStep(67)), StaffSymbol.Note(staffStep(67)), StaffSymbol.Rest(NoteKind.Quarter), StaffSymbol.Note(staffStep(72))))
                PlayButtons("听一听" to {
                    val b = 60.0 / 90
                    synth.play(listOf(Tone(0.0, b * 0.9, 67), Tone(b, b * 0.9, 67), Tone(3 * b, b * 1.5, 72)) + metronome(4, 90, 4))
                })
            }
        }
        MusicLesson.Meter -> {
            LessonCard("强拍和弱拍") {
                Para("音乐的拍子有强有弱，按规律循环。几拍一循环，就叫「几拍子」，每一组用竖线（小节线）隔开，叫一个「小节」。")
                Para("乐谱开头的两个数字叫「拍号」。下面的 4 表示以四分音符为一拍，上面的数字表示每小节有几拍。")
                StaffView(listOf(StaffSymbol.Time(3, 4), StaffSymbol.Note(2, NoteKind.Half), StaffSymbol.Note(4), StaffSymbol.Bar, StaffSymbol.Note(3), StaffSymbol.Note(4), StaffSymbol.Note(5), StaffSymbol.Bar))
            }
            LessonCard("三种常见拍子") {
                InfoTable(
                    listOf("拍号", "强弱规律", "像什么"),
                    listOf(
                        listOf("2/4", "强 弱", "进行曲、走路"),
                        listOf("3/4", "强 弱 弱", "圆舞曲、转圈圈"),
                        listOf("4/4", "强 弱 次强 弱", "大多数儿歌")
                    )
                ) { synth.play(metronome(listOf(2, 3, 4)[it] * 3, 100, listOf(2, 3, 4)[it])) }
                Tip("点表格里的一行，听节拍器敲三个小节，跟着拍手：强拍拍手，弱拍拍腿。")
                PlayButtons(
                    "2/4 一二一" to { synth.play(metronome(8, 110, 2) + melody(listOf(60 to 1.0, 64 to 1.0, 67 to 1.0, 64 to 1.0, 60 to 1.0, 64 to 1.0, 67 to 2.0), bpm = 110, volume = 0.6f)) },
                    "3/4 圆舞曲" to { synth.play(metronome(9, 120, 3) + (0 until 3).flatMap { bar -> val s = bar * 1.5; listOf(Tone(s, 0.45, 48, 0.6f)) + chord(listOf(64, 67), s + 0.5, 0.4) + chord(listOf(64, 67), s + 1.0, 0.4) }) }
                )
            }
        }
        MusicLesson.Accidentals -> {
            LessonCard("黑键叫什么") {
                Para("黑键夹在两个白键中间。它比左边的白键高一点点，也比右边的白键低一点点。")
                Para("♯（升号）表示升高一点点，♭（降号）表示降低一点点。所以 C 和 D 中间的黑键，既可以叫 C♯（升 do），也可以叫 D♭（降 re）。")
                val names = mapOf(61 to "C♯", 63 to "D♯", 66 to "F♯", 68 to "G♯", 70 to "A♯")
                PianoKeyboard(60, 72, label = { m -> names[m] ?: degreeOf(m)?.let { NoteLetters[it] } }, onKey = { synth.note(it) })
                StaffView(listOf(StaffSymbol.Note(-2), StaffSymbol.Note(-2, accidental = "♯"), StaffSymbol.Note(-1, accidental = "♭"), StaffSymbol.Note(-1))) { synth.note(listOf(60, 61, 61, 62)[it]) }
                Para("五线谱上把 ♯ 或 ♭ 写在音符左边。中间两个音名字不同，弹的却是同一个黑键！")
                Tip("还原记号 ♮ 表示取消升降，弹回原来的白键。")
            }
            LessonCard("半音和全音") {
                Para("钢琴上相邻的两个键（不管黑白）离得最近，叫「半音」。两个半音合在一起叫「全音」。")
                Para("E 和 F、B 和 C 之间没有黑键，所以它们挨着，是半音；其他相邻白键中间隔着一个黑键，是全音。")
                PlayButtons(
                    "半音 mi-fa" to { synth.play(listOf(Tone(0.0, 0.7, 64), Tone(0.8, 0.7, 65))) },
                    "全音 do-re" to { synth.play(listOf(Tone(0.0, 0.7, 60), Tone(0.8, 0.7, 62))) },
                    "一串半音" to { synth.play(melody((60..72).map { it to 0.5 }, bpm = 120)) }
                )
            }
        }
        MusicLesson.Scale -> {
            LessonCard("音阶就是音的楼梯") {
                Para("从 do 开始，一级一级按顺序走到高音 do，就是一条「音阶」。从 C 开始、只走白键的叫「C 大调音阶」。")
                StaffView(listOf(60, 62, 64, 65, 67, 69, 71, 72).map { StaffSymbol.Note(staffStep(it)) })
                PlayButtons(
                    "上行" to { synth.play(melody(listOf(60, 62, 64, 65, 67, 69, 71, 72).map { it to 1.0 }, bpm = 120)) },
                    "下行" to { synth.play(melody(listOf(72, 71, 69, 67, 65, 64, 62, 60).map { it to 1.0 }, bpm = 120)) }
                )
            }
            LessonCard("全全半全全全半") {
                Para("大调音阶的每一级之间的距离是有规律的：全音、全音、半音、全音、全音、全音、半音。")
                InfoTable(
                    listOf("从", "到", "距离"),
                    listOf(
                        listOf("do", "re", "全音"), listOf("re", "mi", "全音"), listOf("mi", "fa", "半音 ✦"),
                        listOf("fa", "sol", "全音"), listOf("sol", "la", "全音"), listOf("la", "si", "全音"), listOf("si", "do", "半音 ✦")
                    )
                ) { i -> val a = listOf(60, 62, 64, 65, 67, 69, 71, 72); synth.play(listOf(Tone(0.0, 0.6, a[i]), Tone(0.7, 0.7, a[i + 1]))) }
                Tip("只要按这个规律走，从任何一个音开始都能弹出大调音阶。比如从 G 开始，需要把 F 换成 F♯。")
                PlayButtons("G 大调音阶" to { synth.play(melody(listOf(67, 69, 71, 72, 74, 76, 78, 79).map { it to 1.0 }, bpm = 120)) })
            }
        }
        MusicLesson.Intervals -> {
            LessonCard("音程：两个音隔多远") {
                Para("两个音之间的距离叫「音程」，用「度」来数：从自己数起，数到另一个音一共几个白键，就是几度。do 到 mi：do、re、mi，三个，所以是三度。")
                val names = listOf("一度（同音）", "二度", "三度", "四度", "五度", "六度", "七度", "八度")
                val tops = listOf(60, 62, 64, 65, 67, 69, 71, 72)
                InfoTable(
                    listOf("音程", "例子", "听起来"),
                    names.indices.map { i ->
                        listOf(
                            names[i],
                            "do-${if (i == 7) "do" else Solfege[i]}",
                            listOf("完全一样", "挨着，有点挤", "甜美和谐", "开阔", "空旷稳定", "温柔", "紧张想解决", "像一个音")[i]
                        )
                    }
                ) { i -> synth.play(listOf(Tone(0.0, 0.6, 60), Tone(0.7, 0.6, tops[i])) + chord(listOf(60, tops[i]), 1.5, 1.0)) }
                Tip("点每一行：先分开弹两个音，再一起弹。")
            }
            LessonCard("用歌记音程") {
                Para("很多熟悉的歌开头就是一个音程：《小星星》开头 1 1 5 5，do 跳到 sol 就是五度；《两只老虎》1 2 3 1，一步一步是二度。")
                PlayButtons(
                    "小星星（五度）" to { synth.play(melody(Twinkle.take(4), bpm = 100)) },
                    "两只老虎（二度）" to { synth.play(melody(listOf(60 to 1.0, 62 to 1.0, 64 to 1.0, 60 to 1.0), bpm = 100)) }
                )
            }
        }
        MusicLesson.Chords -> {
            LessonCard("三个音叠在一起") {
                Para("隔一个音取一个，比如 do、mi、sol，三个音同时弹，就是一个「三和弦」。最下面的音叫「根音」，它的名字就是和弦的名字：do mi sol 是 C 和弦。")
                StaffView(listOf(StaffSymbol.Note(-2, NoteKind.Whole), StaffSymbol.Note(0, NoteKind.Whole), StaffSymbol.Note(2, NoteKind.Whole), StaffSymbol.Bar)) { synth.play(chord(listOf(60, 64, 67))) }
                PianoKeyboard(60, 72, label = { m -> if (m in setOf(60, 64, 67)) Solfege[degreeOf(m)!!] else null }, marks = mapOf(60 to MusicSoft, 64 to MusicSoft, 67 to MusicSoft), onKey = { synth.note(it) })
                PlayButtons("C 和弦" to { synth.play(brokenChord(listOf(60, 64, 67))) })
            }
            LessonCard("大三和弦和小三和弦") {
                Para("大三和弦听起来明亮、开心，像晴天；小三和弦听起来柔和、有点忧伤，像下雨天。")
                Para("区别只在中间那个音：C 大三和弦是 C E G，把 E 降半音成 E♭，就变成 C 小三和弦 C E♭ G。")
                PlayButtons(
                    "C 大三和弦 ☀️" to { synth.play(brokenChord(listOf(60, 64, 67))) },
                    "C 小三和弦 🌧" to { synth.play(brokenChord(listOf(60, 63, 67))) },
                    "A 小三和弦" to { synth.play(brokenChord(listOf(57, 60, 64))) },
                    "G 大三和弦" to { synth.play(brokenChord(listOf(55, 59, 62))) }
                )
                Tip("C、F、G 三个大三和弦，可以给很多儿歌伴奏。")
                PlayButtons("C - F - G - C" to {
                    synth.play(listOf(listOf(48, 60, 64, 67), listOf(53, 60, 65, 69), listOf(55, 59, 62, 67), listOf(48, 60, 64, 67)).flatMapIndexed { i, c -> chord(c, i * 1.0, 0.9) })
                })
            }
        }
        MusicLesson.Expression -> {
            LessonCard("速度：快还是慢") {
                Para("乐谱上常用意大利文写速度，数字表示每分钟敲几拍（♩= 拍数）。")
                val tempos = listOf(
                    Triple("Largo", "广板 · 很慢", 50), Triple("Andante", "行板 · 像散步", 76),
                    Triple("Moderato", "中板 · 不快不慢", 100), Triple("Allegro", "快板 · 轻快", 132), Triple("Presto", "急板 · 非常快", 180)
                )
                InfoTable(listOf("术语", "意思", "♩="), tempos.map { listOf(it.first, it.second, "${it.third}") }) { i ->
                    val bpm = tempos[i].third
                    synth.play(metronome(8, bpm, 4) + melody(listOf(60, 62, 64, 65, 67, 65, 64, 62).map { it to 1.0 }, bpm = bpm, volume = 0.6f))
                }
                Tip("点一行，听同一段旋律用不同速度演奏。")
            }
            LessonCard("力度：强还是弱") {
                Para("力度记号写在音符下面：p 是 piano（弱），f 是 forte（强）。")
                val dyns = listOf("pp" to "很弱", "p" to "弱", "mp" to "中弱", "mf" to "中强", "f" to "强", "ff" to "很强")
                val vols = listOf(0.12f, 0.22f, 0.38f, 0.55f, 0.8f, 1f)
                InfoTable(listOf("记号", "意思"), dyns.map { listOf(it.first, it.second) }) { i ->
                    synth.play(melody(listOf(67 to 1.0, 64 to 1.0, 60 to 2.0), bpm = 100, volume = vols[i]))
                }
                Para("「<」叫渐强，越来越响；「>」叫渐弱，越来越轻。")
                PlayButtons(
                    "渐强 <" to { synth.play((0 until 8).map { Tone(it * 0.4, 0.38, CMajorMidis.getOrElse(it) { 72 }, 0.1f + it * 0.12f) }) },
                    "渐弱 >" to { synth.play((0 until 8).map { Tone(it * 0.4, 0.38, 72 - listOf(0, 1, 3, 5, 7, 8, 10, 12)[it], 0.95f - it * 0.12f) }) }
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 小游戏

private sealed class Visual {
    object None : Visual()
    data class Staff(val symbols: List<StaffSymbol>) : Visual()
    data class Big(val text: String) : Visual()
    /** 不出选项，点 C4..B4 的键作答，[MusicQuestion.answer] 为目标 midi */
    object Keys : Visual()
}

private data class MusicQuestion(
    val prompt: String,
    val visual: Visual = Visual.None,
    val options: List<String> = emptyList(),
    val answer: Int,
    /** 出题时播放，可重听 */
    val sound: List<Tone> = emptyList(),
    /** 作答后播放 */
    val reveal: List<Tone> = emptyList()
)

private fun <T> pickOptions(correct: T, pool: List<T>, rng: Random, count: Int = 4): Pair<List<T>, Int> {
    val options = (pool.filter { it != correct }.shuffled(rng).take(count - 1) + correct).shuffled(rng)
    return options to options.indexOf(correct)
}

private val NameSystems = listOf("唱名" to Solfege, "音名" to NoteLetters, "简谱" to (1..7).map { "$it" })

/** 唱名对对碰：三套名字里挑两套、分先后，共 6 组，每组单独记成绩（[ProgressStore.bestSpecialScore] 的 level 即组号） */
private val NameGroups = listOf(0 to 1, 0 to 2, 1 to 0, 1 to 2, 2 to 0, 2 to 1)

private fun nameGroupTitle(group: Int) = NameGroups[group].let { (f, t) -> "${NameSystems[f].first} → ${NameSystems[t].first}" }

private fun buildRound(game: MusicGame, rng: Random, group: Int): List<MusicQuestion> {
    // 同一局里尽量不连着出同一题
    var last: Any? = null
    fun <T> pick(pool: List<T>): T = pool.filter { it != last || pool.size == 1 }.random(rng).also { last = it }
    return List(MusicRoundSize) {
        when (game) {
            MusicGame.Names -> {
                val d = pick(Solfege.indices.toList())
                val (from, to) = NameGroups[group].let { (f, t) -> NameSystems[f] to NameSystems[t] }
                val (opts, ans) = pickOptions(to.second[d], to.second, rng)
                MusicQuestion("${from.first}「${from.second[d]}」对应的${to.first}是？", Visual.Big(from.second[d]), opts, ans, reveal = listOf(Tone(0.0, 0.8, CMajorMidis[d])))
            }
            MusicGame.FindKey -> {
                val d = pick(Solfege.indices.toList())
                val ask = listOf("唱名 ${Solfege[d]}", "音名 ${NoteLetters[d]}", "简谱 ${d + 1}").random(rng)
                MusicQuestion("在键盘上找到 $ask", Visual.Keys, answer = CMajorMidis[d])
            }
            MusicGame.ReadStaff -> {
                val midi = pick(listOf(60, 62, 64, 65, 67, 69, 71, 72, 74, 76, 77, 79))
                val (opts, ans) = pickOptions(Solfege[degreeOf(midi)!!], Solfege, rng)
                MusicQuestion("这个音符的唱名是？", Visual.Staff(listOf(StaffSymbol.Note(staffStep(midi)))), opts, ans, reveal = listOf(Tone(0.0, 0.8, midi)))
            }
            MusicGame.HighLow -> {
                val a = rng.nextInt(55, 80)
                val diff = rng.nextInt(2, 10) * if (rng.nextBoolean()) 1 else -1
                MusicQuestion(
                    "第二个音比第一个音？", Visual.Big("🎵 → 🎵"), listOf("更高 ↗", "更低 ↘"), if (diff > 0) 0 else 1,
                    sound = listOf(Tone(0.0, 0.7, a), Tone(0.9, 0.7, a + diff))
                )
            }
            MusicGame.NoteValue -> {
                val kind = pick(NoteKind.values().toList())
                val rest = kind != NoteKind.Sixteenth && rng.nextBoolean()
                val (opts, ans) = pickOptions(kind.beats, NoteKind.values().map { it.beats }, rng)
                MusicQuestion(
                    if (rest) "这个休止符停几拍？" else "这个音符唱几拍？",
                    Visual.Staff(listOf(if (rest) StaffSymbol.Rest(kind) else StaffSymbol.Note(3, kind))),
                    opts, ans
                )
            }
            MusicGame.Meter -> {
                val meter = pick(listOf(2, 3, 4))
                MusicQuestion(
                    "听节拍器，这是几拍子？", Visual.Big("🥁"),
                    listOf("2 拍子 · 强 弱", "3 拍子 · 强 弱 弱", "4 拍子 · 强 弱 次强 弱"), meter - 2,
                    sound = metronome(meter * 3 + 1, 108, meter)
                )
            }
            MusicGame.Chord -> {
                val root = rng.nextInt(55, 68)
                val major = rng.nextBoolean()
                MusicQuestion(
                    "这个和弦听起来是？", Visual.Big("🎹"), listOf("大三和弦 · 明亮 ☀️", "小三和弦 · 柔和 🌧"), if (major) 0 else 1,
                    sound = brokenChord(listOf(root, root + if (major) 4 else 3, root + 7))
                )
            }
            MusicGame.Ear -> {
                // 先弹 do 做参照，再弹目标音
                val d = pick(Solfege.indices.toList())
                val (opts, ans) = pickOptions(Solfege[d], Solfege, rng)
                MusicQuestion(
                    "先听 do，第二个音是？", Visual.Big("do → ?"), opts, ans,
                    sound = listOf(Tone(0.0, 0.7, 60), Tone(1.0, 0.9, CMajorMidis[d]))
                )
            }
        }
    }
}

private enum class MusicPhase { Asking, Feedback, Finished }

@Composable
fun MusicGameScreen(game: MusicGame, store: ProgressStore, onBack: () -> Unit) {
    // 唱名对对碰先选组，其他游戏只有一组
    var group by remember { mutableStateOf(if (game == MusicGame.Names) null else 0) }
    val g = group
    if (g == null) {
        NameGroupPicker(store, onGroup = { group = it }, onBack = onBack)
    } else {
        key(g) {
            MusicGameRound(game, g, store, onLeave = { if (game == MusicGame.Names) group = null else onBack() })
        }
    }
}

@Composable
private fun MusicGameRound(game: MusicGame, g: Int, store: ProgressStore, onLeave: () -> Unit) {
    val synth = rememberToneSynth()
    var session by remember { mutableStateOf(0) }
    val questions = remember(session) { buildRound(game, Random(System.nanoTime()), g) }
    val results = remember(session) { mutableStateListOf<Boolean>() }
    var index by remember(session) { mutableStateOf(0) }
    var phase by remember(session) { mutableStateOf(MusicPhase.Asking) }
    var chosen by remember(session, index) { mutableStateOf<Int?>(null) }
    val question = questions.getOrNull(index)
    val correct = results.count { it }
    val leave: () -> Unit = { synth.stop(); onLeave() }
    BackHandler(onBack = leave)

    fun answer(choice: Int) {
        if (phase != MusicPhase.Asking || question == null) return
        chosen = choice
        results += choice == question.answer
        phase = MusicPhase.Feedback
        if (question.visual == Visual.Keys) synth.note(choice) else synth.play(question.reveal)
    }

    LaunchedEffect(session, index) {
        if (phase == MusicPhase.Asking) question?.let { synth.play(it.sound) }
    }
    LaunchedEffect(session, index, phase) {
        if (phase != MusicPhase.Feedback) return@LaunchedEffect
        delay(if (results.lastOrNull() == true) 1000 else 2200)
        if (index >= questions.lastIndex) {
            store.saveSpecialScore("music-${game.key}", g, results.count { it })
            phase = MusicPhase.Finished
        } else {
            index++
            phase = MusicPhase.Asking
        }
    }

    val landscape = if (game.hasKeyboard) keyboardScreenLandscape() else false
    val progress = if (phase == MusicPhase.Finished) "本局结束" else "第 ${index + 1} / ${questions.size} 题 · 答对 $correct"
    Box {
    Scaffold(containerColor = Paper, topBar = {
        if (!landscape) MusicTopBar(
            "${game.emoji} ${game.title}" + if (game == MusicGame.Names) " · ${nameGroupTitle(g)}" else "",
            progress,
            leave,
            rotatable = game.hasKeyboard
        )
    }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = if (landscape) 6.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (landscape) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (landscape) AppText(progress, color = InkFaint, fontSize = 13.sp)
            if (phase == MusicPhase.Finished || question == null) {
                MusicResult(correct, store.bestSpecialScore("music-${game.key}", g), onAgain = { synth.stop(); session++ }, onBack = leave, backLabel = if (game == MusicGame.Names) "换一组" else "返回")
            } else {
            AppText(question.prompt, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            when (val v = question.visual) {
                Visual.None -> Unit
                is Visual.Big -> Text(v.text, fontSize = 44.sp, fontWeight = FontWeight.Bold, color = MusicTint, fontFamily = FontFamily.Serif)
                is Visual.Staff -> Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(PaperElevated)
                        .border(1.dp, Line, RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp)
                ) { StaffView(v.symbols, gap = 12.dp, clef = v.symbols.any { it is StaffSymbol.Note }) }
                Visual.Keys -> {
                    val marks = if (phase == MusicPhase.Feedback) buildMap {
                        chosen?.let { put(it, if (it == question.answer) RightSoft else WrongSoft) }
                        put(question.answer, RightSoft)
                    } else emptyMap()
                    // 横屏时题目一行、反馈一行，剩下的高度都给键盘
                    val height = if (landscape) (LocalConfiguration.current.screenHeightDp - 110).coerceAtLeast(150).dp else 190.dp
                    PianoKeyboard(60, 71, height = height, marks = marks, onKey = { answer(it) })
                }
            }
            if (question.sound.isNotEmpty()) {
                OutlinedButton(onClick = { synth.play(question.sound) }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    AppText("再听一遍")
                }
            }
            question.options.forEachIndexed { i, option ->
                val bg = when {
                    phase != MusicPhase.Feedback -> PaperElevated
                    i == question.answer -> RightSoft
                    i == chosen -> WrongSoft
                    else -> PaperElevated
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(bg)
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .clickable(enabled = phase == MusicPhase.Asking) { answer(i) },
                    contentAlignment = Alignment.Center
                ) { AppText(option, fontSize = 18.sp, fontWeight = FontWeight.SemiBold) }
            }
            if (phase == MusicPhase.Feedback) {
                val right = results.lastOrNull() == true
                AppText(
                    if (right) "答对啦！" else "正确答案：" + if (question.visual == Visual.Keys) "绿色的键" else question.options[question.answer],
                    color = if (right) Success else Error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            }
        }
    }
    if (landscape) LandscapeCorner(leave, Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun NameGroupPicker(store: ProgressStore, onGroup: (Int) -> Unit, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Scaffold(containerColor = Paper, topBar = {
        MusicTopBar("${MusicGame.Names.emoji} ${MusicGame.Names.title}", "选一组练习 · 每组 $MusicRoundSize 题", onBack)
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(NameGroups.indices.toList()) { i ->
                val (f, t) = NameGroups[i]
                val best = store.bestSpecialScore("music-${MusicGame.Names.key}", i)
                MusicEntryCard(
                    badge = "${i + 1}",
                    title = nameGroupTitle(i),
                    subtitle = "看 ${NameSystems[f].second[4]}，选 ${NameSystems[t].second[4]}" +
                        if (best != null) " · 最好 $best / $MusicRoundSize" + if (best == MusicRoundSize) " ⭐" else "" else "",
                    onClick = { onGroup(i) }
                )
            }
        }
    }
}

@Composable
private fun MusicResult(correct: Int, best: Int?, onAgain: () -> Unit, onBack: () -> Unit, backLabel: String) {
    val stars = when {
        correct == MusicRoundSize -> "⭐⭐⭐"
        correct >= MusicRoundSize * 0.7 -> "⭐⭐"
        correct >= MusicRoundSize * 0.4 -> "⭐"
        else -> "💪"
    }
    Spacer(Modifier.height(24.dp))
    Text(stars, fontSize = 48.sp)
    AppText("答对 $correct / $MusicRoundSize", fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
    if (best != null) AppText("最好成绩 $best / $MusicRoundSize", color = InkFaint)
    Button(
        onClick = onAgain,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MusicTint)
    ) { AppText("再来一局", fontWeight = FontWeight.Bold) }
    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { AppText(backLabel) }
}
