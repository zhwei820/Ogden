package com.example.ogdenkids

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 系统划词菜单（ACTION_PROCESS_TEXT）与 annalearning://lookup?text=<英文> 的入口：在别的 App 里选中英文后，浮在原 App 上弹出查词卡片，
 * 列出词表释义和出现该词的课文；点课文才跳进本 App。
 */
class TextLookupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = (intent?.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            ?: intent?.data?.takeIf { it.scheme == "annalearning" && it.host == "lookup" }?.getQueryParameter("text"))
            ?.trim().orEmpty()
        if (text.isEmpty()) {
            finish()
            return
        }
        setContent {
            AppTheme {
                TextLookupSheet(
                    text = text,
                    onDismiss = ::finish,
                    onOpenSpeech = { openInApp(OpenSpeechExtra, it.id) },
                    onOpenWord = { openInApp(OpenWordExtra, it.word) }
                )
            }
        }
    }

    private fun openInApp(extra: String, value: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra(extra, value)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}

internal const val OpenSpeechExtra = "open_speech"
internal const val OpenWordExtra = "open_word"

fun buildLemmaVocabulary(words: List<OgdenWord>): LemmaVocabulary {
    fun keysOf(vararg categories: Category) =
        words.filter { it.category in categories }.map { it.word.lowercase() }.toSet()
    return LemmaVocabulary(
        words = words.map { it.word.lowercase() }.toSet(),
        qualities = keysOf(Category.Qualities, Category.Opposites, Category.Extended, Category.BabyTalk, Category.Informal),
        things = keysOf(Category.GeneralThings, Category.Picturable, Category.Extended, Category.BabyTalk, Category.Informal)
    )
}

/** 一课和它命中的那句；[line] 为 null 表示只在「本课单词」里出现。 */
data class LessonHit(val speech: Speech, val line: SpeechLine?)

/** 选中文字在词库和课文里的查询结果。 */
data class LookupResult(
    /** 选中的词还原后命中词表的词条，按原文顺序去重 */
    val words: List<OgdenWord>,
    val lessons: List<LessonHit>
)

/**
 * 整段原文出现在某句课文里的排最前（选的是一句话时就能定位到那课）；
 * 否则按「覆盖选中词数 → 本课单词含该词 → 级别」排序。
 */
fun lookupSelection(text: String, words: List<OgdenWord>, speeches: List<Speech>, limit: Int = 8): LookupResult {
    val vocabulary = buildLemmaVocabulary(words)
    val index = words.associateBy { it.word.lowercase() }
    fun lemmasOf(s: String) = tokenizeSpeech(s).mapNotNull { lemmatize(it.text, vocabulary) }
    val selected = lemmasOf(text).distinct()
    val normalized = text.lowercase().replace(Regex("\\s+"), " ")
    val multiWord = tokenizeSpeech(text).size > 1

    data class Scored(val hit: LessonHit, val phrase: Boolean, val covered: Int, val listed: Boolean)

    val scored = speeches.mapNotNull { speech ->
        val lines = speech.lines + speech.patterns
        val phraseLine = if (multiWord) lines.firstOrNull { normalized in it.en.lowercase().replace(Regex("\\s+"), " ") } else null
        val lineLemmas = lines.map { it to lemmasOf(it.en).toSet() }
        val listed = speech.words.map { it.lowercase() }.toSet()
        val covered = selected.count { lemma -> lemma in listed || lineLemmas.any { lemma in it.second } }
        if (phraseLine == null && covered == 0) return@mapNotNull null
        // 指向覆盖选中词最多的那句
        val bestLine = phraseLine ?: lineLemmas.maxByOrNull { (_, ls) -> selected.count { it in ls } }
            ?.takeIf { (_, ls) -> selected.any { it in ls } }?.first
        Scored(LessonHit(speech, bestLine), phraseLine != null, covered, selected.any { it in listed })
    }
    val lessons = scored
        .sortedWith(compareByDescending<Scored> { it.phrase }.thenByDescending { it.covered }.thenByDescending { it.listed }.thenBy { it.hit.speech.level })
        .take(limit)
        .map { it.hit }
    return LookupResult(selected.mapNotNull { index[it] }, lessons)
}

private fun currentAccent(context: Context): Accent {
    val profiles = ProfileStore(context)
    val id = profiles.currentId() ?: profiles.profiles().firstOrNull()?.id ?: return Accent.US
    return ProgressStore(context, id).savedAccent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextLookupSheet(
    text: String,
    onDismiss: () -> Unit,
    onOpenSpeech: (Speech) -> Unit,
    onOpenWord: (OgdenWord) -> Unit
) {
    val context = LocalContext.current
    val accent = remember { currentAccent(context) }
    val azure = remember { AzureSpeaker(context) }
    DisposableEffect(azure) { onDispose { azure.release() } }
    val speakWord = rememberSpeaker(accent, azure)
    val speakEnglish: (String) -> Unit = { azure.speak(it, AzureVoice.english(accent)) }
    val result by produceState<LookupResult?>(null, text) {
        value = withContext(Dispatchers.Default) {
            val repository = OgdenRepository(context)
            lookupSelection(text, repository.loadWords(), repository.loadSpeeches())
        }
    }
    val translation by produceState<String?>(null, text, result) {
        val r = result ?: return@produceState
        // 单个词表词有离线释义，不必联网
        if (r.words.size == 1 && tokenizeSpeech(text).size == 1) return@produceState
        value = Translator(context).translate(text).getOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PaperElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(start = 22.dp, end = 22.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text, fontSize = if (text.length > 24) 22.sp else 32.sp, fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { speakEnglish(text) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = Category.Operations.tint)
                }
            }
            translation?.let { AppText(it, color = InkSoft, fontSize = 18.sp) }
            val r = result
            if (r == null) {
                AppText("查找中…", color = InkFaint)
            } else {
                if (r.words.size == 1) {
                    SingleWordCard(r.words.first(), accent, speakWord, speakEnglish, onOpenWord)
                } else {
                    r.words.forEach { WordRow(it, speakWord, onOpenWord) }
                }
                if (r.words.isEmpty() && r.lessons.isEmpty()) {
                    AppText("词库和课文里都没有找到", color = InkFaint)
                }
                if (r.lessons.isNotEmpty()) {
                    AppText("相关课文", color = InkFaint, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    r.lessons.forEach { LessonRow(it) { onOpenSpeech(it.speech) } }
                }
            }
        }
    }
}

@Composable
private fun SingleWordCard(
    word: OgdenWord,
    accent: Accent,
    speakWord: (String) -> Unit,
    speakEnglish: (String) -> Unit,
    onOpenWord: (OgdenWord) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(word.word, fontSize = 24.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, color = word.category.tint)
        Spacer(Modifier.width(10.dp))
        Text(if (accent == Accent.UK) word.ipaUk else word.ipaUs, color = InkFaint, modifier = Modifier.weight(1f))
        IconButton(onClick = { speakWord(word.word) }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = word.category.tint)
        }
    }
    AppText(word.zh, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
    if (word.example.isNotBlank()) {
        Card(colors = CardDefaults.cardColors(containerColor = Paper), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(word.example, fontFamily = FontFamily.Serif, fontSize = 18.sp, color = Ink)
                    AppText(word.exampleZh, color = InkSoft, fontSize = 15.sp)
                }
                IconButton(onClick = { speakEnglish(word.example) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = Category.Operations.tint)
                }
            }
        }
    }
    Button(onClick = { onOpenWord(word) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        AppText("在 App 中查看详情")
    }
}

@Composable
private fun WordRow(word: OgdenWord, speakWord: (String) -> Unit, onOpenWord: (OgdenWord) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable { onOpenWord(word) }
    ) {
        Text(word.word, fontFamily = FontFamily.Serif, fontSize = 20.sp, color = word.category.tint, modifier = Modifier.width(110.dp))
        AppText(word.zh, color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        IconButton(onClick = { speakWord(word.word) }) {
            Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = InkFaint)
        }
    }
}

@Composable
private fun LessonRow(hit: LessonHit, onClick: () -> Unit) {
    val speech = hit.speech
    val place = listOfNotNull(
        SpeechLevelNames[speech.level],
        speech.theme?.zh ?: speech.groupZh ?: speech.book,
        "Unit ${speech.unit}"
    ).joinToString(" · ")
    Card(
        colors = CardDefaults.cardColors(containerColor = Paper),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(speech.title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = Ink)
            AppText("${speech.titleZh}　$place", color = InkFaint, fontSize = 13.sp)
            hit.line?.let {
                Text(it.en, fontFamily = FontFamily.Serif, color = InkSoft, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}
