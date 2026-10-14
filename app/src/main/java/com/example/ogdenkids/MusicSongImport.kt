package com.example.ogdenkids

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** 用户粘贴的一首歌的原文；存原文而不是解析结果，改了解析规则也能重新解析 */
data class CustomSongSource(val key: String, val text: String, val bpm: Int, val lowUnderscore: Boolean)

/** 用户自己导入的曲谱只存在本机，各个用户共用 */
class CustomSongStore(context: Context) {
    private val prefs = context.getSharedPreferences("custom_songs", Context.MODE_PRIVATE)

    fun all(): List<CustomSongSource> {
        val array = JSONArray(prefs.getString("songs", "[]"))
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CustomSongSource(o.getString("key"), o.getString("text"), o.optInt("bpm", 90), o.optBoolean("lowUnderscore"))
        }
    }

    fun get(key: String) = all().firstOrNull { it.key == key }

    fun songs(): List<Song> = all().mapNotNull { src ->
        parseImport(src.text, src.lowUnderscore).firstOrNull()?.toSong(src.key, src.bpm)
    }

    /** [replaceKey] 不为空时，用 [sources] 的第一首替换它，其余插在它后面 */
    fun upsert(replaceKey: String?, sources: List<CustomSongSource>) {
        val list = all().toMutableList()
        val at = list.indexOfFirst { it.key == replaceKey }
        if (at >= 0) {
            list.removeAt(at)
            list.addAll(at, sources)
        } else {
            list.addAll(sources)
        }
        save(list)
    }

    fun delete(key: String) = save(all().filterNot { it.key == key })

    private fun save(list: List<CustomSongSource>) {
        val array = JSONArray()
        list.forEach {
            array.put(JSONObject().put("key", it.key).put("text", it.text).put("bpm", it.bpm).put("lowUnderscore", it.lowUnderscore))
        }
        prefs.edit().putString("songs", array.toString()).apply()
    }
}

class ImportLine(val lyric: String?, val notation: String, val notes: List<SongNote>, val syllables: List<String>, val bad: List<String>)

class ImportedSong(val titleLine: String, val title: String, val origin: String) {
    val lines = mutableListOf<ImportLine>()

    val noteCount get() = lines.sumOf { line -> line.notes.count { it.midi != null } }

    val warnings: List<String>
        get() = lines.flatMap { line ->
            val name = (line.lyric ?: line.notation).take(10)
            val playable = line.notes.count { it.midi != null }
            listOfNotNull(
                line.bad.takeIf { it.isNotEmpty() }?.let { "「$name」看不懂：${it.joinToString(" ")}" },
                if (line.lyric != null && line.syllables.size != playable) "「$name」${line.syllables.size} 个字，$playable 个音" else null
            )
        } + if (noteCount == 0) listOf("没有找到简谱") else emptyList()

    fun text(): String = (listOf(titleLine) + lines.flatMap { listOfNotNull(it.lyric, it.notation) }).joinToString("\n")

    fun toSong(key: String, bpm: Int): Song {
        val notes = mutableListOf<SongNote>()
        val lyrics = mutableListOf<String?>()
        val hasLyrics = lines.any { it.lyric != null }
        lines.forEach { line ->
            val words = line.syllables.iterator()
            line.notes.forEachIndexed { i, note ->
                // 每行歌词是一个乐句，行末画一条小节线隔开
                notes += if (i == line.notes.lastIndex) note.copy(barAfter = true) else note
                lyrics += if (note.midi != null && words.hasNext()) words.next() else null
            }
        }
        return Song(key, title, origin.ifBlank { "自己导入" }, 0, "4/4", bpm, notes, if (hasLyrics) lyrics else emptyList())
    }
}

private fun Char.isCjk() = this in '一'..'鿿' || this in '㐀'..'䶿'

private fun normalizeLine(line: String): String = buildString {
    line.forEach { c ->
        append(
            when (c) {
                in '０'..'９' -> '0' + (c - '０')
                '｜' -> '|'
                '’', '‘', '′', '＇', '`' -> '\''
                '－', '—', '–' -> '-'
                '＿' -> '_'
                '．', '·' -> '.'
                '＃', '♯' -> '#'
                '♭' -> 'b'
                else -> c
            }
        )
    }
}.replace("|", " | ").trim()

/** 传入 [normalizeLine] 之后的行 */
private fun isNotationLine(line: String) = line.none { it.isCjk() } && line.any { it.isDigit() } && '《' !in line

/** 歌词按字切：汉字一个字一个音，英文一个词一个音，标点空格不算 */
private fun syllables(lyric: String): List<String> =
    Regex("""[一-鿿㐀-䶿]|[A-Za-z']+""").findAll(lyric).map { it.value }.toList()

private fun parseTitle(line: String): Pair<String, String> {
    val clean = line.replace(Regex("""^\d+\s*[.、)）]\s*"""), "").trim()
    val book = Regex("""《(.+?)》""").find(clean) ?: return clean to ""
    val origin = clean.removeRange(book.range)
        .replace(Regex("""[（(]\s*(\d{4})\s*[)）]"""), " · $1")
        .trim(' ', '·', '-', '—')
    return book.groupValues[1] to origin
}

/**
 * 解析粘贴的曲谱：带《》或编号的行是歌名；其他不含简谱的行，下一行是简谱就当歌词，否则也当歌名。
 * [lowUnderscore] 为真时把 `_` 当低音点（很多网上的简谱这样写），此时没法表示半拍。
 */
fun parseImport(text: String, lowUnderscore: Boolean): List<ImportedSong> {
    // 歌名、歌词保留原样，只有简谱行用规整后的写法
    val raws = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val lines = raws.map(::normalizeLine)
    val songs = mutableListOf<ImportedSong>()
    var pendingLyric: String? = null
    lines.forEachIndexed { i, normalized ->
        val line = if (isNotationLine(normalized)) normalized else raws[i]
        if (!isNotationLine(normalized)) {
            val next = lines.getOrNull(i + 1)
            // 带书名号或「1.」编号的行一定是歌名，哪怕下一行直接就是简谱
            val titleLike = '《' in line || Regex("""^\d+\s*[.、)）]""").containsMatchIn(line)
            if (!titleLike && next != null && isNotationLine(next)) {
                pendingLyric = line
            } else {
                val (title, origin) = parseTitle(line)
                songs += ImportedSong(line, title, origin)
            }
            return@forEachIndexed
        }
        val song = songs.lastOrNull() ?: ImportedSong("未命名", "未命名", "").also { songs += it }
        val notes = mutableListOf<SongNote>()
        val bad = mutableListOf<String>()
        line.split(Regex("\\s+")).forEach { raw ->
            if (raw == "|") {
                notes.lastOrNull()?.let { notes[notes.lastIndex] = it.copy(barAfter = true) }
                return@forEach
            }
            val token = if (lowUnderscore) raw.replace('_', ',') else raw
            parseNoteToken(token)?.let { notes += it } ?: run { bad += raw }
        }
        song.lines += ImportLine(pendingLyric, line, notes, pendingLyric?.let(::syllables).orEmpty(), bad)
        pendingLyric = null
    }
    return songs
}

private const val ImportExample = """1. 小星星
一闪一闪亮晶晶
1 1 5 5 6 6 5-
满天都是小星星
4 4 3 3 2 2 1-"""

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MusicSongImportScreen(editKey: String?, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { CustomSongStore(context) }
    val editing = remember(editKey) { editKey?.let { store.get(it) } }
    var text by rememberSaveable { mutableStateOf(editing?.text.orEmpty()) }
    var bpm by rememberSaveable { mutableStateOf(editing?.bpm ?: 90) }
    var lowUnderscore by rememberSaveable { mutableStateOf(editing?.lowUnderscore ?: false) }
    val parsed = remember(text, lowUnderscore) { parseImport(text, lowUnderscore) }
    val savable = parsed.filter { it.noteCount > 0 }
    val synth = rememberToneSynth()
    // 从 txt 文件读进来，接在已粘贴内容的后面，保存前照样能预览和改
    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.openInputStream(uri)?.use { it.bufferedReader(Charsets.UTF_8).readText() } }
            .onSuccess { loaded -> if (!loaded.isNullOrBlank()) text = if (text.isBlank()) loaded.trim() else text.trimEnd() + "\n" + loaded.trim() }
            .onFailure { Toast.makeText(context, "读取文件失败", Toast.LENGTH_SHORT).show() }
    }

    Scaffold(containerColor = Paper, topBar = {
        MusicTopBar(if (editing != null) "✏️ 编辑曲谱" else "＋ 粘贴导入曲谱", "只存在这台手机上", onBack)
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MusicSoft)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AppText("格式：一行歌名，然后「歌词一行、简谱一行」交替；可以一次粘贴好几首。", color = MusicTint, fontSize = 14.sp)
                    AppText("简谱：1-7 音，0 休止，1' 高音，5, 低音，#4 升音，- 延长一拍，_ 半拍，. 附点，| 小节线。", color = MusicTint, fontSize = 14.sp)
                    AppText("歌词每个字对一个音，对不上会在下面提示。", color = MusicTint, fontSize = 14.sp)
                }
            }
            item {
                OutlinedButton(onClick = { openFile.launch(arrayOf("text/*")) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.FileOpen, contentDescription = null)
                    AppText("  从 txt 文件导入")
                }
            }
            item {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    placeholder = { Text(ImportExample, color = InkFaint, fontFamily = FontFamily.Monospace) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp)
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppText("速度（每分钟拍数）", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(60, 72, 90, 110, 130).forEach { b ->
                            FilterChip(selected = bpm == b, onClick = { bpm = b }, label = { AppText("♩=$b") })
                        }
                    }
                    FilterChip(
                        selected = lowUnderscore,
                        onClick = { lowUnderscore = !lowUnderscore },
                        label = { AppText("把 _ 当作低音点（如 6_ = 低音 6）") }
                    )
                }
            }
            if (text.isNotBlank()) {
                item { AppText("识别到 ${parsed.size} 首", fontWeight = FontWeight.Bold) }
            }
            items(parsed) { song ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PaperElevated)
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            AppText(song.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            AppText("${song.origin.ifBlank { "自己导入" }} · ${song.lines.size} 句 · ${song.noteCount} 个音", color = InkFaint, fontSize = 13.sp)
                        }
                        if (song.noteCount > 0) {
                            IconButton(onClick = { synth.play(songTones(song.toSong("", bpm))) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "试听", tint = MusicTint)
                            }
                        }
                    }
                    song.warnings.forEach { AppText("⚠️ $it", color = Error, fontSize = 13.sp) }
                }
            }
            item {
                Button(
                    onClick = {
                        val sources = savable.mapIndexed { i, song ->
                            CustomSongSource(if (i == 0 && editing != null) editing.key else "custom-${UUID.randomUUID()}", song.text(), bpm, lowUnderscore)
                        }
                        store.upsert(editing?.key, sources)
                        synth.stop()
                        onDone()
                    },
                    enabled = savable.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MusicTint)
                ) { AppText(if (savable.size > 1) "保存 ${savable.size} 首" else "保存", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
