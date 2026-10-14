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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/*
* Mutopia
* */
/** [degree] 为简谱数字（0 = 休止），[octave] 为高低八度点数 */
data class SongNote(val midi: Int?, val beats: Double, val degree: Int, val octave: Int, val accidental: String, val barAfter: Boolean)

/**
 * [notation] 是 C 调简谱：`5,` 低音、`1'` 高音、`#4` / `b7` 升降，
 * `_` 减半（八分）、`__` 十六分、`.` 附点、每个 `-` 延长一拍，`|` 小节线，`0` 休止。
 */
class Song(
    val key: String,
    val title: String,
    val origin: String,
    /** 难度 1..3；0 = 用户自己导入的 */
    val level: Int,
    val meter: String,
    val bpm: Int,
    val notes: List<SongNote>,
    /** 与 [notes] 一一对应的歌词字，休止和没配上字的为 null；没有歌词时为空 */
    val lyrics: List<String?> = emptyList()
) {
    constructor(key: String, title: String, origin: String, level: Int, meter: String, bpm: Int, notation: String) :
        this(key, title, origin, level, meter, bpm, parseJianpu(notation))
}

private val MajorOffsets = listOf(0, 2, 4, 5, 7, 9, 11)
private val NoteToken = Regex("""([#b]?)([0-7])([',]*)([_.\-]*)""")

/** 单个简谱记号，看不懂返回 null */
fun parseNoteToken(token: String): SongNote? {
    val m = NoteToken.matchEntire(token) ?: return null
    val (acc, d, oct, dur) = m.destructured
    val degree = d.toInt()
    val octave = oct.count { it == '\'' } - oct.count { it == ',' }
    var beats = 1.0
    repeat(dur.count { it == '_' }) { beats /= 2 }
    if ('.' in dur) beats *= 1.5
    beats += dur.count { it == '-' }
    val shift = when (acc) { "#" -> 1; "b" -> -1; else -> 0 }
    val midi = if (degree == 0) null else 60 + MajorOffsets[degree - 1] + 12 * octave + shift
    return SongNote(midi, beats, degree, octave, acc.replace("#", "♯").replace("b", "♭"), false)
}

fun parseJianpu(notation: String): List<SongNote> {
    val notes = mutableListOf<SongNote>()
    notation.trim().split(Regex("\\s+")).forEach { token ->
        if (token == "|") {
            notes.lastOrNull()?.let { notes[notes.lastIndex] = it.copy(barAfter = true) }
            return@forEach
        }
        notes += parseNoteToken(token) ?: error("简谱写错了：$token")
    }
    return notes
}

/** 只收录公版的传统民歌、儿歌和古典旋律；按难度排列 */
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
        "3 3_ 5_ 6_ 1'_ 1'_ 6_ | 5 5_ 6_ 5- | 3 3_ 5_ 6_ 1'_ 1'_ 6_ | 5 5_ 6_ 5- | 5 5 5 3_ 5_ | 6 6 5- | 3 2_ 3_ 5 3_ 2_ | 1 1_ 2_ 1- |"),
    Song("home", "念故乡", "德沃夏克", 1, "4/4", 72,
        "3 5 5- | 3 2 1- | 2 3 5 3 | 2--- | 3 5 5- | 3 2 1- | 2 3 2 1 | 1--- |"),
    Song("oldman", "这个老人", "英国童谣", 1, "4/4", 110,
        "5 3 5- | 5 3 5- | 6 5 4 3 | 2 3 4- | 3 4 5 1 | 1_ 1_ 1 1_ 2_ 3_ 4_ | 5 2 2 4 | 3 2 1- |"),
    Song("fate", "命运交响曲（片段）", "贝多芬", 1, "2/4", 108,
        "0_ 5_ 5_ 5_ | b3- | b3- | 0_ 4_ 4_ 4_ | 2- | 2- |"),
    Song("susanna", "噢！苏珊娜", "福斯特", 2, "4/4", 120,
        "1_ 2_ | 3 5 5. 6_ | 5 3 1. 2_ | 3 3 2 1 | 2-- 1_ 2_ | 3 5 5. 6_ | 5 3 1. 2_ | 3 3 2 2 | 1-- |"),
    Song("saints", "圣徒进行曲", "美国灵歌", 2, "4/4", 120,
        "0 1 3 4 | 5--- | 0 1 3 4 | 5--- | 0 1 3 4 | 5- 3- | 1- 3- | 2--- | 0 3 3 2 | 1- 1 3 | 5 5 4- | 4- 3 4 | 5- 3- | 1- 2- | 1--- |"),
    Song("grace", "奇异恩典", "美国圣歌", 2, "3/4", 80,
        "5, | 1- 3_ 1_ | 3- 2 | 1- 6, | 5,- 5, | 1- 3_ 1_ | 3- 2_ 3_ | 5-- | 5- 3 | 5- 3_ 1_ | 3- 2 | 1- 6, | 5,- 5, | 1- 3_ 1_ | 3- 2 | 1- |"),
    Song("auld", "友谊地久天长", "苏格兰民歌", 2, "4/4", 90,
        "5, | 1. 1_ 1 3 | 2. 1_ 2 3 | 1. 1_ 3 5 | 6-- 6 | 5. 3_ 3 1 | 2. 1_ 2 3 | 1. 6,_ 6, 5, | 1-- |"),
    Song("nacht", "弦乐小夜曲（片段）", "莫扎特", 2, "4/4", 110,
        "1 0_ 5,_ 1 0_ 5,_ | 1_ 5,_ 1_ 3_ 5- | 4 0_ 2_ 4 0_ 2_ | 4_ 2_ 7,_ 2_ 5,- |"),
    Song("minuet", "G 大调小步舞曲", "巴赫曲集", 2, "3/4", 120,
        "5 1_ 2_ 3_ 4_ | 5 1 1 | 6 4_ 5_ 6_ 7_ | 1' 1 1 | 4 5_ 4_ 3_ 2_ | 3 4_ 3_ 2_ 1_ | 7, 1_ 2_ 3_ 1_ | 2-- | 5 1_ 2_ 3_ 4_ | 5 1 1 | 6 4_ 5_ 6_ 7_ | 1' 1 1 | 4 5_ 4_ 3_ 2_ | 3 4_ 3_ 2_ 1_ | 2 3_ 2_ 1_ 7,_ | 1-- |"),
    Song("silent", "平安夜", "格鲁伯", 2, "3/4", 72,
        "5. 6_ 5 | 3-- | 5. 6_ 5 | 3-- | 2'- 2' | 7-- | 1'- 1' | 5-- | 6- 6 | 1'. 7_ 6 | 5. 6_ 5 | 3-- | 6- 6 | 1'. 7_ 6 | 5. 6_ 5 | 3-- | 2'- 2' | 4'. 2'_ 7 | 1'-- | 3'-- | 1'. 5_ 3 | 5. 4_ 2 | 1-- |"),
    Song("deck", "装饰大厅", "威尔士民歌", 3, "4/4", 120,
        "5. 4_ 3 2 | 1 2 3 1 | 2_ 3_ 4_ 2_ 3. 2_ | 1 7, 1- | 5. 4_ 3 2 | 1 2 3 1 | 2_ 3_ 4_ 2_ 3. 2_ | 1 7, 1- | 2. 3_ 4 2 | 3. 4_ 5 2 | 3_ #4_ 5 6_ 7_ 1' | 7 6 5- | 5. 4_ 3 2 | 1 2 3 1 | 6_ 6_ 6_ 6_ 5. 4_ | 3 2 1- |"),
    Song("green", "绿袖子", "英国民歌", 3, "6/8", 66,
        "6_ | 1' 2'_ 3'._ 4'__ 3'_ | 2' 7_ 5._ 6__ 7_ | 1' 6_ 6._ #5__ 6_ | 7 #5_ 3 6_ | 1' 2'_ 3'._ 4'__ 3'_ | 2' 7_ 5._ 6__ 7_ | 1'._ 7__ 6_ #5._ #4__ #5_ | 6. 6. |"),
    Song("elise", "致爱丽丝（片段）", "贝多芬", 3, "3/8", 66,
        "3'__ #2'__ | 3'__ #2'__ 3'__ 7__ 2'__ 1'__ | 6_ 0__ 1__ 3__ 6__ | 7_ 0__ 3__ #5__ 7__ | 1'_ 0__ 3__ 3'__ #2'__ | 3'__ #2'__ 3'__ 7__ 2'__ 1'__ | 6_ 0__ 1__ 3__ 6__ | 7_ 0__ 3__ 1'__ 7__ | 6. |"),
    Song("surprise", "惊愕交响曲（片段）", "海顿", 1, "2/4", 100,
        "1_ 1_ 3_ 3_ | 5_ 5_ 3 | 4_ 4_ 2_ 2_ | 7,_ 7,_ 5, | 1_ 1_ 3_ 3_ | 5_ 5_ 3 | 4_ 4_ 2_ 2_ | 7,_ 7,_ 5, |"),
    Song("cancan", "康康舞曲", "奥芬巴赫", 2, "4/4", 120,
        "1- 2_ 4_ 3_ 2_ | 5 5 5_ 6_ 3_ 4_ | 2 2 2_ 4_ 3_ 2_ | 1_ 1'_ 7_ 6_ 5_ 4_ 3_ 2_ | 1- 2_ 4_ 3_ 2_ | 5 5 5_ 6_ 3_ 4_ | 2 2 2_ 4_ 3_ 2_ | 1_ 5,_ 2_ 3_ 1 0 |"),
    Song("danube", "蓝色多瑙河（片段）", "小约翰·施特劳斯", 2, "3/4", 150,
        "0 0 1 | 1 3 5 | 5-- | 0 5' 5' | 0 3' 3' | 0 0 1 | 1 3 5 | 5-- | 0 6' 6' | 0 4' 4' | 0 0 7, | 7, 2 6 | 6-- | 0 6' 6' | 0 4' 4' | 0 0 7, | 7, 2 6 | 6-- | 0 5' 5' | 0 3' 3' |"),
    Song("mountain", "山魔王的宫殿", "格里格", 3, "4/4", 100,
        "6,_ 7,_ 1_ 2_ 3_ 1_ 3 | b3_ 7,_ b3 2_ b7,_ 2 | 6,_ 7,_ 1_ 2_ 3_ 1_ 3_ 6_ | 5_ 3_ 1_ 3_ 5- | 6,_ 7,_ 1_ 2_ 3_ 1_ 3 | b3_ 7,_ b3 2_ b7,_ 2 | 6,_ 7,_ 1_ 2_ 3_ 1_ 3_ 6_ | 5_ 3_ 1_ 3_ 5- |"),
    Song("turkish", "土耳其进行曲（片段）", "莫扎特", 3, "2/4", 100,
        "7,__ 6,__ #5,__ 6,__ | 1_ 0_ 2__ 1__ 7,__ 1__ | 3_ 0_ 4__ 3__ #2__ 3__ | 7__ 6__ #5__ 6__ 7__ 6__ #5__ 6__ | 1'- |"),
    Song("prelude", "C 大调前奏曲（片段）", "巴赫", 3, "4/4", 66,
        "1__ 3__ 5__ 1'__ 3'__ 5__ 1'__ 3'__ 1__ 3__ 5__ 1'__ 3'__ 5__ 1'__ 3'__ | 1__ 2__ 6__ 2'__ 4'__ 6__ 2'__ 4'__ 1__ 2__ 6__ 2'__ 4'__ 6__ 2'__ 4'__ | 7,__ 2__ 5__ 2'__ 4'__ 5__ 2'__ 4'__ 7,__ 2__ 5__ 2'__ 4'__ 5__ 2'__ 4'__ | 1__ 3__ 5__ 1'__ 3'__ 5__ 1'__ 3'__ 1__ 3__ 5__ 1'__ 3'__ 5__ 1'__ 3'__ | 1__ 3__ 6__ 3'__ 6'__ 6__ 3'__ 6'__ 1__ 3__ 6__ 3'__ 6'__ 6__ 3'__ 6'__ |")
).sortedBy { it.level }

/** 整首歌连同休止的时间排成音序 */
fun songTones(song: Song): List<Tone> {
    val beat = 60.0 / song.bpm
    var at = 0.0
    return song.notes.mapNotNull { note ->
        val tone = note.midi?.let { Tone(at, note.beats * beat * 0.95, it) }
        at += note.beats * beat
        tone
    }
}

private fun songStars(stars: Int?) = when (stars) {
    null -> ""
    else -> " · " + "⭐".repeat(stars)
}

@Composable
fun MusicSongListScreen(store: ProgressStore, onSong: (Song) -> Unit, onImport: (editKey: String?) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val customStore = remember { CustomSongStore(context) }
    var version by remember { mutableStateOf(0) }
    val custom = remember(version) { customStore.songs() }
    var deleting by remember { mutableStateOf<Song?>(null) }
    deleting?.let { song ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { AppText("删除曲谱") },
            text = { AppText("确定删除「${song.title}」吗？") },
            confirmButton = { TextButton(onClick = { customStore.delete(song.key); version++; deleting = null }) { AppText("删除", color = Error) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { AppText("取消") } }
        )
    }
    Scaffold(containerColor = Paper, topBar = { MusicTopBar("🎹 跟着弹", "${Songs.size + custom.size} 首 · 先听一遍，再跟着弹", onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionTitle("我的曲谱", "把歌名、歌词、简谱粘贴进来，只存在这台手机上") }
            items(custom, key = { it.key }) { song ->
                MusicEntryCard(
                    badge = "♪",
                    title = song.title,
                    subtitle = "${song.origin} · ${song.notes.count { it.midi != null }} 个音" + songStars(store.bestSpecialScore("song-${song.key}", 0)),
                    onClick = { onSong(song) },
                    trailing = {
                        IconButton(onClick = { onImport(song.key) }) { Icon(Icons.Default.Edit, contentDescription = "编辑", tint = InkSoft) }
                        IconButton(onClick = { deleting = song }) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = InkSoft) }
                    }
                )
            }
            item {
                MusicEntryCard(badge = "＋", title = "粘贴导入曲谱", subtitle = "一行歌名，然后歌词、简谱一行一行交替", onClick = { onImport(null) })
            }
            item { SectionTitle("内置曲库", "传统民歌、童谣和古典名曲片段") }
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
        // 不能用外面的 finished：键盘的点击回调可能还是弹完前那次组合里的
        if (pos >= notes.size) return
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
            if (!landscape) JianpuStrip(notes, song.lyrics, current, played = if (demo) -1 else pos)
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
            if (landscape) JianpuStrip(notes, song.lyrics, current, played = if (demo) -1 else pos, compact = true)
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
private fun JianpuStrip(notes: List<SongNote>, lyrics: List<String?>, current: Int, played: Int, compact: Boolean = false) {
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
                    if (lyrics.isNotEmpty()) AppText(lyrics.getOrNull(i) ?: " ", fontSize = if (compact) 11.sp else 13.sp, color = color, maxLines = 1)
                }
                if (note.barAfter) Box(Modifier.padding(horizontal = 4.dp).width(1.dp).height(if (compact) 28.dp else 36.dp).background(Line))
            }
        }
    }
}
