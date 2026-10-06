package com.example.ogdenkids

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.icu.text.Transliterator
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.gestures.PressGestureScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.ceil
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.combinedClickable
import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioTrack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { OgdenKidsApp() }
    }
}

private val Paper = Color(0xFFFAF6ED)
private val PaperElevated = Color(0xFFFFFDF7)
private val Ink = Color(0xFF1C1917)
private val InkSoft = Color(0xFF44403C)
private val InkFaint = Color(0xFF78716C)
private val Line = Color(0xFFE7E2D4)
private val Success = Color(0xFF166534)
private val Error = Color(0xFFB91C1C)
private val LocalChineseMode = compositionLocalOf { ChineseMode.Hans }

enum class Category(
    val code: String,
    val label: String,
    val zh: String,
    val tint: Color,
    val soft: Color
) {
    Operations("op", "Operations", "操作词", Color(0xFFB45309), Color(0xFFFEF3C7)),
    GeneralThings("gt", "General Things", "通用词", Color(0xFF166534), Color(0xFFDCFCE7)),
    Picturable("pt", "Things", "物品词", Color(0xFFA16207), Color(0xFFFEF9C3)),
    Qualities("qg", "Qualities", "性质词", Color(0xFF1E40AF), Color(0xFFDBEAFE)),
    Opposites("qo", "Opposites", "反义对", Color(0xFF7C3AED), Color(0xFFEDE9FE)),
    Extended("ex", "Extended", "拓展词", Color(0xFF0F766E), Color(0xFFCCFBF1)),
    Forms("fm", "Word Forms", "变形词", Color(0xFFBE185D), Color(0xFFFCE7F3)),
    BabyTalk("kd", "Baby Talk", "儿语", Color(0xFFC2410C), Color(0xFFFFEDD5)),
    Informal("ky", "Informal", "口语", Color(0xFF475569), Color(0xFFE2E8F0));

    /** 拓展词、变形词的 s 字段是相关词（one→two、teacher→student、me→I），不是近义词：不出近义词题，详情页标「相关词」 */
    val hasTrueSynonyms get() = this != Extended && this != Forms

    companion object {
        fun from(code: String) = values().first { it.code == code }
    }
}

data class OgdenWord(
    val word: String,
    val category: Category,
    val zh: String,
    val englishDefinition: String,
    val example: String,
    val exampleZh: String,
    val synonyms: List<String>,
    val ipaUk: String,
    val ipaUs: String,
    /** 反义词，来自 antonyms.json；没有的为空，只在单词详情展示 */
    val antonyms: List<String> = emptyList(),
    /** 常见搭配，来自 collocations.json（覆盖课文「本课单词」和专项训练单词） */
    val collocations: List<Collocation> = emptyList(),
    /** 一词多义时第二个意思的例句（ex2 / exz2），没有的为空；只在单词详情和查词弹窗展示 */
    val example2: String = "",
    val exampleZh2: String = "",
    /** false 的词（ogden_words.json 里标 np）只在词库展示，不进任何单词练习 */
    val practice: Boolean = true
) {
    /** 英文例句与中文翻译，第一句之外最多再有一句 */
    val examples: List<Pair<String, String>>
        get() = listOf(example to exampleZh) + listOfNotNull((example2 to exampleZh2).takeIf { example2.isNotBlank() })
}

data class Collocation(val phrase: String, val zh: String, val example: String, val exampleZh: String)

/** 不在词库里的近义词 / 反义词的离线释义与例句（related_words.json）。 */
data class RelatedWord(val zh: String, val example: String, val exampleZh: String)

data class WordProgress(
    val favorite: Boolean,
    val mistake: Boolean,
    val mastery: Int,
    val attempts: Int,
    val correct: Int
) {
    /** 答对过且最近一次没答错（答错会进错词本）即算掌握；[mastery] 星级只用于展示熟练程度。 */
    val mastered: Boolean get() = correct > 0 && !mistake
}

enum class Tab(val title: String, val icon: ImageVector) {
    Speech("课文", Icons.Default.RecordVoiceOver),
    Challenge("闯关", Icons.Default.EmojiEvents),
    Library("词库", Icons.Default.Book),
    Review("复习", Icons.Default.Refresh)
}

enum class Accent(val label: String, val locale: Locale) {
    UK("UK 英式", Locale.UK),
    US("US 美式", Locale.US)
}

enum class ChineseMode(val label: String) {
    Hans("简"), Hant("繁")
}

enum class PracticeType(val title: String) {
    Listen("听音选词"),
    Meaning("看中文选英文"),
    Example("例句填空"),
    Spelling("拼写挑战"),
    Synonym("近义词配对")
}

sealed class Screen {
    object Main : Screen()
    data class Detail(val word: OgdenWord, val returnTo: Screen = Screen.Main) : Screen()
    data class Levels(val category: Category) : Screen()
    data class Practice(val category: Category, val level: Int, val reviewOnly: Boolean = false, val returnTo: Screen = Main) : Screen()
    data class WordCollection(val title: String, val kind: String) : Screen()
    data class SpeechReader(val speech: Speech) : Screen()
    data class ThemePractice(val level: Int, val theme: SpeechTheme) : Screen()
    data class WordPractice(val title: String, val words: List<String>, val returnTo: Screen) : Screen()
    data class SpecialPractice(val topic: SpecialTopic, val level: Int) : Screen()
    data class SpecialModulePage(val key: String) : Screen()
    data class ModulePractice(val key: String) : Screen()
    object SpeechWords : Screen()
    object Settings : Screen()
    object Privacy : Screen()
    object Feedback : Screen()
}

class OgdenRepository(private val context: Context) {
    fun loadWords(): List<OgdenWord> {
        val words = JSONArray(readAsset("ogden_words.json"))
        val ipa = JSONObject(readAsset("ogden_ipa.json"))
        val antonyms = JSONObject(readAsset("antonyms.json"))
        val collocations = JSONObject(readAsset("collocations.json"))
        // related_words.json 里标了 drop 的是描述性条目（如 "12 months"），不当近义词展示
        val dropped = JSONObject(readAsset("related_words.json")).let { r ->
            r.keys().asSequence().filter { r.getJSONObject(it).optBoolean("drop") }.toSet()
        }
        return List(words.length()) { index ->
            val item = words.getJSONObject(index)
            val word = item.getString("w")
            val ipaItem = ipa.optJSONObject(word)
            val synonyms = item.getJSONArray("s")
            OgdenWord(
                word = word,
                category = Category.from(item.getString("c")),
                zh = item.getString("zh"),
                englishDefinition = item.getString("en"),
                example = item.getString("ex"),
                exampleZh = item.getString("exz"),
                synonyms = List(synonyms.length()) { synonyms.getString(it) }.filter { it !in dropped },
                ipaUk = ipaItem?.optString("uk").orEmpty(),
                ipaUs = ipaItem?.optString("us").orEmpty(),
                antonyms = antonyms.optJSONArray(word)?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty(),
                collocations = collocations.optJSONArray(word)?.let { a ->
                    List(a.length()) {
                        val c = a.getJSONObject(it)
                        Collocation(c.getString("phrase"), c.getString("zh"), c.getString("ex"), c.getString("exz"))
                    }
                }.orEmpty(),
                example2 = item.optString("ex2"),
                exampleZh2 = item.optString("exz2"),
                practice = !item.optBoolean("np")
            )
        }
    }

    fun loadRelatedWords(): Map<String, RelatedWord> {
        val related = JSONObject(readAsset("related_words.json"))
        return related.keys().asSequence().mapNotNull { key ->
            val item = related.getJSONObject(key)
            if (item.optBoolean("drop")) null else key.lowercase() to RelatedWord(item.getString("zh"), item.getString("ex"), item.getString("exz"))
        }.toMap()
    }

    fun loadSpeeches(): List<Speech> {
        val speeches = JSONArray(readAsset("speeches.json"))
        fun lines(array: JSONArray) = List(array.length()) {
            val line = array.getJSONObject(it)
            SpeechLine(en = line.getString("en"), zh = line.getString("zh"))
        }
        return List(speeches.length()) { index ->
            val item = speeches.getJSONObject(index)
            Speech(
                id = item.getString("id"),
                level = item.getInt("level"),
                unit = item.getInt("unit"),
                theme = SpeechTheme.from(item.getString("theme")),
                title = item.getString("title"),
                titleZh = item.getString("titleZh"),
                lines = lines(item.getJSONArray("lines")),
                patterns = lines(item.getJSONArray("patterns")),
                words = item.optJSONArray("words")?.let { a -> List(a.length()) { a.getString(it) } }.orEmpty()
            )
        }.sortedWith(compareBy({ it.level }, { it.unit }))
    }

    fun loadSpecialModules(): List<SpecialModule> {
        val modules = JSONArray(readAsset("specials.json"))
        return List(modules.length()) { index ->
            val item = modules.getJSONObject(index)
            val groups = item.getJSONArray("groups")
            val sentences = item.getJSONArray("sentences")
            SpecialModule(
                key = item.getString("key"),
                zh = item.getString("zh"),
                en = item.getString("en"),
                icon = item.getString("icon"),
                scene = item.optString("scene").takeIf { it.isNotBlank() && it != "null" }?.let { key -> SpecialTopic.values().first { it.key == key } },
                groups = List(groups.length()) {
                    val g = groups.getJSONObject(it)
                    val words = g.getJSONArray("words")
                    WordGroup(g.getString("zh"), g.getString("en"), List(words.length()) { w -> words.getString(w) })
                },
                sentences = List(sentences.length()) {
                    val line = sentences.getJSONObject(it)
                    SpeechLine(line.getString("en"), line.getString("zh"))
                }
            )
        }
    }

    private fun readAsset(name: String): String =
        context.assets.open(name).bufferedReader().use { it.readText() }.trimStart('\uFEFF')
}

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("ogden-progress", Context.MODE_PRIVATE)

    fun progress(word: String) = WordProgress(
        favorite = set("favorites").contains(word),
        mistake = set("mistakes").contains(word),
        mastery = prefs.getInt("mastery.$word", 0),
        attempts = prefs.getInt("attempts.$word", 0),
        correct = prefs.getInt("correct.$word", 0)
    )

    fun toggleFavorite(word: String) = updateSet("favorites", word, !set("favorites").contains(word))

    fun record(word: String, correct: Boolean) {
        val current = progress(word)
        val nextMastery = when {
            correct -> (current.mastery + 1).coerceAtMost(3)
            else -> (current.mastery - 1).coerceAtLeast(0)
        }
        prefs.edit()
            .putInt("attempts.$word", current.attempts + 1)
            .putInt("correct.$word", current.correct + if (correct) 1 else 0)
            .putInt("mastery.$word", nextMastery)
            .putLong("last.$word", System.currentTimeMillis())
            .apply()
        updateSet("mistakes", word, !correct || nextMastery == 0)
        if (correct) bumpDailyStreak()
    }

    fun masteredCount(words: List<OgdenWord>) = words.count { progress(it.word).mastered }

    fun masteredWords(words: List<OgdenWord>) = words.filter { progress(it.word).mastered }

    fun mistakeWords(words: List<OgdenWord>) = words.filter { progress(it.word).mistake }

    fun removeMistake(word: String) = updateSet("mistakes", word, false)

    fun favoriteWords(words: List<OgdenWord>) = words.filter { progress(it.word).favorite }

    /** 课文里收藏的词表外单词（词表内的走 favorites），存小写原文。 */
    fun speechWords(): List<String> = set("speechWords").sorted()

    fun isSpeechWordSaved(word: String) = set("speechWords").contains(word)

    fun toggleSpeechWord(word: String) = updateSet("speechWords", word, !isSpeechWordSaved(word))

    fun addSpeechWord(word: String) = updateSet("speechWords", word, true)

    fun isSpeechLearned(id: String) = set("speechLearned").contains(id)

    fun toggleSpeechLearned(id: String) = updateSet("speechLearned", id, !isSpeechLearned(id))

    /** 跟读最好成绩，按句子英文存 */
    fun readAlongScore(sentence: String): Int? =
        prefs.getInt("readAlong.$sentence", -1).takeIf { it >= 0 }

    fun saveReadAlongScore(sentence: String, score: Int) {
        if (score > (readAlongScore(sentence) ?: -1)) prefs.edit().putInt("readAlong.$sentence", score).apply()
    }

    fun bestSpecialScore(key: String, level: Int): Int? =
        prefs.getInt("special.$key.$level", -1).takeIf { it >= 0 }

    fun saveSpecialScore(key: String, level: Int, correct: Int) {
        if (correct > (bestSpecialScore(key, level) ?: -1)) prefs.edit().putInt("special.$key.$level", correct).commit()
    }

    fun bestThemeScore(level: Int, theme: SpeechTheme): Int? =
        prefs.getInt("themePractice.$level.${theme.key}", -1).takeIf { it >= 0 }

    fun saveThemeScore(level: Int, theme: SpeechTheme, correct: Int) {
        if (correct > (bestThemeScore(level, theme) ?: -1)) {
            prefs.edit().putInt("themePractice.$level.${theme.key}", correct).commit()
        }
    }

    fun dailyStreak(): Int = prefs.getInt("streak", 0)

    fun lastCategory(): Category = runCatching {
        Category.from(prefs.getString("lastCategory", Category.Operations.code) ?: Category.Operations.code)
    }.getOrDefault(Category.Operations)

    fun lastLevel(): Int = prefs.getInt("lastLevel", 1).coerceAtLeast(1)

    fun saveLastLevel(category: Category, level: Int) {
        prefs.edit()
            .putString("lastCategory", category.code)
            .putInt("lastLevel", level)
            .commit()
    }

    fun savedAccent(): Accent = runCatching {
        Accent.valueOf(prefs.getString("accent", Accent.US.name) ?: Accent.US.name)
    }.getOrDefault(Accent.US)

    fun savedChineseMode(): ChineseMode = runCatching {
        ChineseMode.valueOf(prefs.getString("chineseMode", ChineseMode.Hans.name) ?: ChineseMode.Hans.name)
    }.getOrDefault(ChineseMode.Hans)

    fun saveAccent(accent: Accent) {
        prefs.edit().putString("accent", accent.name).commit()
    }

    fun saveChineseMode(mode: ChineseMode) {
        prefs.edit().putString("chineseMode", mode.name).commit()
    }

    fun isLevelComplete(category: Category, level: Int): Boolean =
        prefs.getBoolean("level.${category.code}.$level.complete", false)

    fun isLevelUnlocked(category: Category, level: Int): Boolean =
        level <= 1 || isLevelComplete(category, level - 1)

    fun markLevelComplete(category: Category, level: Int) {
        prefs.edit().putBoolean("level.${category.code}.$level.complete", true).commit()
    }

    private fun bumpDailyStreak() {
        val today = System.currentTimeMillis() / 86_400_000L
        val last = prefs.getLong("lastStudyDay", 0L)
        val streak = prefs.getInt("streak", 0)
        val next = when {
            last == today -> streak
            last == today - 1 -> streak + 1
            else -> 1
        }
        prefs.edit().putLong("lastStudyDay", today).putInt("streak", next).apply()
    }

    private fun set(key: String): Set<String> = prefs.getStringSet(key, emptySet()).orEmpty()

    private fun updateSet(key: String, word: String, present: Boolean) {
        val next = set(key).toMutableSet()
        if (present) next.add(word) else next.remove(word)
        prefs.edit().putStringSet(key, next).commit()
    }
}

@Composable
fun OgdenKidsApp() {
    val context = LocalContext.current
    val repository = remember { OgdenRepository(context) }
    val words = remember { repository.loadWords() }
    val speeches = remember { repository.loadSpeeches() }
    val lessonWords = remember(speeches) { speeches.flatMap { it.words }.map { it.lowercase() }.toSet() }
    val specialModules = remember { repository.loadSpecialModules() }
    val wordIndex = remember(words) { words.associateBy { it.word.lowercase() } }
    val practiceWords = remember(words) { words.filter { it.practice } }
    val lemmaVocabulary = remember(words) {
        fun keysOf(vararg categories: Category) =
            words.filter { it.category in categories }.map { it.word.lowercase() }.toSet()
        LemmaVocabulary(
            words = wordIndex.keys,
            qualities = keysOf(Category.Qualities, Category.Opposites, Category.Extended, Category.BabyTalk, Category.Informal),
            things = keysOf(Category.GeneralThings, Category.Picturable, Category.Extended, Category.BabyTalk, Category.Informal)
        )
    }
    val progressStore = remember { ProgressStore(context) }
    var screen by remember { mutableStateOf<Screen>(Screen.Main) }
    var selectedTab by remember { mutableStateOf(Tab.Speech) }
    var accent by remember { mutableStateOf(progressStore.savedAccent()) }
    var chineseMode by remember { mutableStateOf(progressStore.savedChineseMode()) }
    var version by remember { mutableStateOf(0) }
    var confirmExit by remember { mutableStateOf(false) }
    // 每个页面 / 标签各自保存 rememberSaveable 状态（含列表滚动位置），切回来时恢复
    val stateHolder = rememberSaveableStateHolder()
    var speechLevel by remember { mutableStateOf(1) }
    val azureSpeaker = remember { AzureSpeaker(context) }
    val speak = rememberSpeaker(accent, azureSpeaker)
    DisposableEffect(azureSpeaker) {
        onDispose { azureSpeaker.release() }
    }
    val speakEnglish: (String) -> Unit = { azureSpeaker.speak(it, AzureVoice.english(accent)) }
    val speakChinese: (String) -> Unit = { azureSpeaker.speak(it, AzureVoice.ZhCn) }
    val translator = remember { Translator(context) }
    val relatedWords = remember { repository.loadRelatedWords() }
    val selectionController = remember { SelectionController() }
    val speechServices = remember(accent) {
        SpeechServices(
            translator,
            speakEnglish,
            speakChinese,
            lookup = { token -> lemmatize(token, lemmaVocabulary)?.let { wordIndex[it] } },
            openWord = { word ->
                azureSpeaker.stop()
                screen = Screen.Detail(word, returnTo = screen)
            },
            related = { relatedWords[it.lowercase()] },
            stopSpeaking = { azureSpeaker.stop() }
        )
    }
    val goBack: () -> Unit = {
        when (val current = screen) {
            Screen.Main -> confirmExit = true
            is Screen.Detail -> {
                azureSpeaker.stop()
                screen = current.returnTo
            }
            // 离开练习就丢掉答题进度，否则下次进同一关会停在上次的最后一题
            is Screen.Practice -> {
                stateHolder.removeState(screenKey(current))
                version++
                screen = current.returnTo
            }
            is Screen.WordPractice -> {
                stateHolder.removeState(screenKey(current))
                version++
                screen = current.returnTo
            }
            is Screen.SpecialPractice -> screen = Screen.SpecialModulePage(current.topic.key)
            is Screen.ModulePractice -> {
                azureSpeaker.stop()
                screen = Screen.SpecialModulePage(current.key)
            }
            else -> {
                azureSpeaker.stop()
                screen = Screen.Main
            }
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Category.Operations.tint,
            secondary = Category.GeneralThings.tint,
            background = Paper,
            surface = PaperElevated,
            onPrimary = Color.White,
            onBackground = Ink,
            onSurface = Ink
        ),
        typography = MaterialTheme.typography.copy(
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = FontFamily.Serif),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
        )
    ) {
        CompositionLocalProvider(
            LocalChineseMode provides chineseMode,
            LocalSpeechServices provides speechServices,
            LocalSelectionController provides selectionController
        ) {
        Surface(color = Paper, modifier = Modifier.fillMaxSize().dismissSelectionOnOutsideTap(selectionController)) {
            BackHandler(onBack = goBack)
            if (confirmExit) {
                AlertDialog(
                    onDismissRequest = { confirmExit = false },
                    title = { Text("退出应用") },
                    text = { Text("确定要退出吗？") },
                    confirmButton = {
                        TextButton(onClick = {
                            confirmExit = false
                            (context as? Activity)?.finish()
                        }) { Text("退出") }
                    },
                    dismissButton = {
                        TextButton(onClick = { confirmExit = false }) { Text("取消") }
                    }
                )
            }
            stateHolder.SaveableStateProvider(screenKey(screen)) {
            when (val current = screen) {
                Screen.Main -> MainScaffold(
                    selectedTab = selectedTab,
                    onTab = { selectedTab = it },
                    onSettings = { screen = Screen.Settings },
                    onPrivacy = { screen = Screen.Privacy },
                    onFeedback = { screen = Screen.Feedback },
                    content = { padding ->
                        stateHolder.SaveableStateProvider("tab-${selectedTab.name}") {
                        when (selectedTab) {
                            Tab.Challenge -> ChallengeScreen(
                                words = words,
                                store = progressStore,
                                padding = padding,
                                onContinue = {
                                    screen = Screen.Practice(progressStore.lastCategory(), progressStore.lastLevel())
                                },
                                onCategory = { category -> screen = Screen.Levels(category) },
                                onOpenLibrary = { selectedTab = Tab.Library },
                                onCollection = { title, kind -> screen = Screen.WordCollection(title, kind) }
                            )
                            Tab.Library -> LibraryScreen(
                                words = words,
                                store = progressStore,
                                padding = padding,
                                accent = accent,
                                zh = chineseMode,
                                onSpeak = speak,
                                onOpen = { screen = Screen.Detail(it) }
                            )
                            Tab.Speech -> SpeechListScreen(
                                speeches = speeches,
                                store = progressStore,
                                level = speechLevel,
                                onLevel = { speechLevel = it },
                                onPractice = { theme -> screen = Screen.ThemePractice(speechLevel, theme) },
                                modules = specialModules,
                                onSpecial = { module -> screen = Screen.SpecialModulePage(module.key) },
                                padding = padding,
                                onOpen = { screen = Screen.SpeechReader(it) }
                            )
                            Tab.Review -> ReviewScreen(
                                words = words,
                                store = progressStore,
                                padding = padding,
                                onMistakes = { screen = Screen.WordCollection("错词本", "mistakes") },
                                onFavorites = { screen = Screen.WordCollection("收藏夹", "favorites") },
                                onSpeechWords = { screen = Screen.SpeechWords }
                            )
                        }
                        }
                    }
                )
                is Screen.Detail -> WordDetailScreen(
                    word = current.word,
                    progress = progressStore.progress(current.word.word),
                    accent = accent,
                    zh = chineseMode,
                    onBack = goBack,
                    onSpeak = speak,
                    onSpeakZh = speakChinese,
                    onFavorite = {
                        progressStore.toggleFavorite(current.word.word)
                        version++
                        progressStore.progress(current.word.word)
                    }
                )
                is Screen.Levels -> LevelSelectionScreen(
                    words = practiceWords,
                    store = progressStore,
                    category = current.category,
                    onBack = goBack,
                    onStart = { level -> screen = Screen.Practice(current.category, level) }
                )
                is Screen.Practice -> {
                    LaunchedEffect(current.category, current.level, current.reviewOnly) {
                        if (!current.reviewOnly) progressStore.saveLastLevel(current.category, current.level)
                    }
                    PracticeScreen(
                        allWords = practiceWords,
                        lessonWords = lessonWords,
                        store = progressStore,
                        version = version,
                        category = current.category,
                        level = current.level,
                        reviewOnly = current.reviewOnly,
                        zh = chineseMode,
                        onSpeak = speak,
                        onSpeakChinese = speakChinese,
                        onBack = goBack,
                        onComplete = {
                            if (!current.reviewOnly) progressStore.markLevelComplete(current.category, current.level)
                        },
                        onRecord = { word, correct ->
                            progressStore.record(word.word, correct)
                            version++
                        },
                        onNextLevel = if (!current.reviewOnly && current.level * 10 < practiceWords.count { it.category == current.category }) {
                            {
                                stateHolder.removeState(screenKey(current))
                                version++
                                screen = Screen.Practice(current.category, current.level + 1)
                            }
                        } else null
                    )
                }
                is Screen.SpecialModulePage -> specialModules.firstOrNull { it.key == current.key }?.let { module ->
                    SpecialModuleScreen(
                        module = module,
                        level = speechLevel,
                        store = progressStore,
                        wordIndex = wordIndex,
                        lemmaVocabulary = lemmaVocabulary,
                        accent = accent,
                        onBack = goBack,
                        onScene = { topic, level -> screen = Screen.SpecialPractice(topic, level) },
                        onWordPractice = { list ->
                            azureSpeaker.stop()
                            screen = Screen.WordPractice("${module.zh} · 单词练习", list.shuffled().take(10), current)
                        },
                        onSentencePractice = { azureSpeaker.stop(); screen = Screen.ModulePractice(module.key) },
                        onSpeakWord = { azureSpeaker.stop(); speak(it) },
                        onSpeakEnglish = { text, onDone -> azureSpeaker.speak(text, AzureVoice.english(accent), onDone) },
                        onSpeakChinese = { text, onDone -> azureSpeaker.speak(text, AzureVoice.ZhCn, onDone) },
                        onStopSpeaking = { azureSpeaker.stop() },
                        onOpenWord = {
                            azureSpeaker.stop()
                            screen = Screen.Detail(it, returnTo = current)
                        }
                    )
                }
                is Screen.ModulePractice -> specialModules.firstOrNull { it.key == current.key }?.let { module ->
                    val unit = module.asPracticeUnit()
                    ThemePracticeScreen(
                        title = "${module.icon} ${module.zh} · 句子练习",
                        subtitle = "专项训练",
                        units = listOf(unit),
                        vocabulary = unit.words.mapNotNull { wordIndex[it.lowercase()] },
                        bestScore = progressStore.bestSpecialScore("${module.key}-sentences", 0),
                        onSpeak = speakEnglish,
                        onSpeakWord = { azureSpeaker.stop(); speak(it) },
                        onSpeakChinese = speakChinese,
                        onFinish = { correct, _ ->
                            progressStore.saveSpecialScore("${module.key}-sentences", 0, correct)
                            version++
                        },
                        onBack = goBack
                    )
                }
                is Screen.SpecialPractice -> SpecialPracticeScreen(
                    topic = current.topic,
                    level = current.level,
                    bestScore = progressStore.bestSpecialScore(current.topic.key, current.level),
                    onSpeak = speakEnglish,
                    onSpeakChinese = speakChinese,
                    onFinish = { correct ->
                        progressStore.saveSpecialScore(current.topic.key, current.level, correct)
                        version++
                    },
                    onBack = goBack
                )
                is Screen.WordPractice -> PracticeScreen(
                    allWords = practiceWords,
                    lessonWords = lessonWords,
                    store = progressStore,
                    version = version,
                    category = Category.Extended,
                    level = 1,
                    reviewOnly = false,
                    zh = chineseMode,
                    onSpeak = speak,
                    onSpeakChinese = speakChinese,
                    onBack = goBack,
                    onComplete = {},
                    onRecord = { word, correct ->
                        progressStore.record(word.word, correct)
                        version++
                    },
                    customWords = current.words.mapNotNull { wordIndex[it.lowercase()]?.takeIf { it.practice } },
                    customTitle = current.title
                )
                is Screen.WordCollection -> {
                    val isMistakes = current.kind == "mistakes"
                    WordCollectionScreen(
                        title = current.title,
                        words = remember(version, current.kind) {
                            when (current.kind) {
                                "mistakes" -> progressStore.mistakeWords(words)
                                "mastered" -> progressStore.masteredWords(words)
                                else -> progressStore.favoriteWords(words)
                            }
                        },
                        store = progressStore,
                        zh = chineseMode,
                        onBack = goBack,
                        onOpen = { screen = Screen.Detail(it, returnTo = current) },
                        onPractice = if (isMistakes) {
                            { screen = Screen.Practice(progressStore.lastCategory(), 1, reviewOnly = true, returnTo = current) }
                        } else null,
                        onRemove = if (isMistakes) {
                            { word ->
                                progressStore.removeMistake(word.word)
                                version++
                            }
                        } else null
                    )
                }
                is Screen.ThemePractice -> ThemePracticeScreen(
                    title = "主题练习 · ${current.theme.zh}",
                    subtitle = "${SpeechLevelNames[current.level]}${SpeechLevelThemes[current.level]}",
                    units = speeches.filter { it.level == current.level && it.theme == current.theme },
                    vocabulary = speeches.filter { it.level == current.level && it.theme == current.theme }
                        .flatMap { it.words }
                        .mapNotNull { wordIndex[it.lowercase()] },
                    bestScore = progressStore.bestThemeScore(current.level, current.theme),
                    onSpeak = speakEnglish,
                    onSpeakWord = { azureSpeaker.stop(); speak(it) },
                    onSpeakChinese = speakChinese,
                    onFinish = { correct, _ ->
                        progressStore.saveThemeScore(current.level, current.theme, correct)
                        version++
                    },
                    onBack = goBack
                )
                is Screen.SpeechReader -> SpeechReaderScreen(
                    speech = current.speech,
                    wordIndex = wordIndex,
                    lemmaVocabulary = lemmaVocabulary,
                    store = progressStore,
                    accent = accent,
                    onBack = goBack,
                    onLearnedChange = { version++ },
                    onSpeakWord = { azureSpeaker.stop(); speak(it) },
                    onSpeakEnglish = { text, onDone -> azureSpeaker.speak(text, AzureVoice.english(accent), onDone) },
                    onSpeakChinese = { text, onDone -> azureSpeaker.speak(text, AzureVoice.ZhCn, onDone) },
                    onStopSpeaking = { azureSpeaker.stop() },
                    onOpenWord = {
                        azureSpeaker.stop()
                        screen = Screen.Detail(it, returnTo = current)
                    },
                    onPracticeWords = { list ->
                        azureSpeaker.stop()
                        screen = Screen.WordPractice("${current.speech.title} · 单词练习", list.shuffled().take(10), current)
                    }
                )
                Screen.SpeechWords -> SpeechWordListScreen(
                    words = remember(version) { progressStore.speechWords() },
                    lookup = { wordIndex[it.lowercase()] },
                    onBack = goBack,
                    // 词表内的词用离线录音，词表外的走在线合成
                    onSpeak = { w -> if (wordIndex.containsKey(w.lowercase())) { azureSpeaker.stop(); speak(w) } else speakEnglish(w) },
                    onRemove = {
                        progressStore.toggleSpeechWord(it)
                        version++
                    }
                )
                Screen.Settings -> SettingsScreen(
                    accent = accent,
                    chineseMode = chineseMode,
                    onAccent = { accent = it; progressStore.saveAccent(it) },
                    onChineseMode = { chineseMode = it; progressStore.saveChineseMode(it) },
                    onBack = goBack
                )
                Screen.Privacy -> LegalInfoScreen(
                    title = "隐私声明",
                    onBack = goBack,
                    sections = privacySections(),
                    links = emptyList()
                )
                Screen.Feedback -> FeedbackScreen(onBack = goBack)
            }
            }
        }
        }
    }
}

/**
 * 单词朗读：有 assets/audio 离线录音的直接播放；其余文本（例句、近义词）交给 [azure]，
 * 它会先用预生成的 assets/tts 音频，缺失时才在线合成。
 */
@Composable
fun rememberSpeaker(accent: Accent, azure: AzureSpeaker): (String) -> Unit {
    val context = LocalContext.current
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose { player?.release() }
    }
    return remember(accent, azure) {
        { rawText: String ->
            val text = rawText.trim()
            if (text.isNotEmpty()) {
                player?.release()
                player = null
                val localPath = localAudioPath(text, accent)
                val fd = localPath?.let { runCatching { context.assets.openFd(it) }.getOrNull() }
                if (fd == null) {
                    azure.speak(text, AzureVoice.english(accent))
                } else {
                    azure.stop()
                    runCatching {
                        val mediaPlayer = MediaPlayer()
                        player = mediaPlayer
                        mediaPlayer.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                        fd.close()
                        mediaPlayer.setOnPreparedListener { it.start() }
                        mediaPlayer.setOnCompletionListener {
                            it.release()
                            if (player === it) player = null
                        }
                        mediaPlayer.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            if (player === mp) player = null
                            azure.speak(text, AzureVoice.english(accent))
                            true
                        }
                        mediaPlayer.prepareAsync()
                    }.onFailure { azure.speak(text, AzureVoice.english(accent)) }
                }
            }
        }
    }
}

/** 页面的状态保存键：同一个词 / 课文 / 分类复用同一份滚动位置。 */
private fun screenKey(screen: Screen): String = when (screen) {
    Screen.Main -> "main"
    is Screen.Detail -> "detail-${screen.word.word}"
    is Screen.Levels -> "levels-${screen.category.code}"
    is Screen.Practice -> "practice-${screen.category.code}-${screen.level}-${screen.reviewOnly}"
    is Screen.WordCollection -> "collection-${screen.kind}"
    is Screen.SpeechReader -> "reader-${screen.speech.id}"
    is Screen.ThemePractice -> "theme-${screen.level}-${screen.theme.key}"
    is Screen.WordPractice -> "words-${screen.title}"
    is Screen.SpecialPractice -> "special-${screen.topic.key}-${screen.level}"
    is Screen.SpecialModulePage -> "module-${screen.key}"
    is Screen.ModulePractice -> "module-practice-${screen.key}"
    Screen.SpeechWords -> "speech-words"
    Screen.Settings -> "settings"
    Screen.Privacy -> "privacy"
    Screen.Feedback -> "feedback"
}

private fun localAudioPath(text: String, accent: Accent): String? {
    if (!text.matches(Regex("[A-Za-z][A-Za-z0-9'-]*"))) return null
    val file = text.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
    if (file.isBlank()) return null
    val dir = if (accent == Accent.US) "us" else "uk"
    return "audio/$dir/$file.mp3"
}

@Composable
fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    fontStyle: FontStyle? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    textAlign: TextAlign? = null,
    softWrap: Boolean = true
) {
    Text(
        convertZh(text, LocalChineseMode.current),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        fontStyle = fontStyle,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        textAlign = textAlign,
        softWrap = softWrap
    )
}

@Composable
fun MainScaffold(
    selectedTab: Tab,
    onTab: (Tab) -> Unit,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
    onFeedback: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    // 设置、隐私声明、反馈不常用，收进侧边抽屉，不占底部标签
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit) {
        scope.launch { drawerState.close() }
        action()
    }
    BackHandler(enabled = drawerState.isOpen) { scope.launch { drawerState.close() } }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = PaperElevated) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Panda English", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    AppText("英语单词学习 · 离线词库 · US/UK 单词发音", color = InkSoft, fontSize = 13.sp)
                    AppText("当前版本：${BuildConfig.VERSION_NAME}", color = InkFaint, fontSize = 12.sp)
                }
                NavigationDrawerItem(
                    label = { AppText("软件设置", fontSize = 16.sp) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { closeThen(onSettings) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { AppText("隐私声明", fontSize = 16.sp) },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    selected = false,
                    onClick = { closeThen(onPrivacy) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { AppText("意见反馈", fontSize = 16.sp) },
                    icon = { Icon(Icons.Default.Feedback, contentDescription = null) },
                    selected = false,
                    onClick = { closeThen(onFeedback) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            containerColor = Paper,
            topBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Paper)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                    AppText("Panda English", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = InkSoft)
                }
            },
            bottomBar = {
                Column {
                    NavigationBar(containerColor = PaperElevated) {
                        Tab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = selectedTab == tab,
                                onClick = { onTab(tab) },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { AppText(tab.title) }
                            )
                        }
                    }
                }
            },
            content = content
        )
    }
}

@Composable
fun SettingsToggleRow(
    accent: Accent,
    chineseMode: ChineseMode,
    onAccent: (Accent) -> Unit,
    onChineseMode: (ChineseMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(PaperElevated)
            .border(1.dp, Line)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TogglePill(Accent.UK.label, accent == Accent.UK, { onAccent(Accent.UK) }, Modifier.weight(1f))
        TogglePill(Accent.US.label, accent == Accent.US, { onAccent(Accent.US) }, Modifier.weight(1f))
        TogglePill(ChineseMode.Hans.label, chineseMode == ChineseMode.Hans, { onChineseMode(ChineseMode.Hans) })
        TogglePill(ChineseMode.Hant.label, chineseMode == ChineseMode.Hant, { onChineseMode(ChineseMode.Hant) })
    }
}

@Composable
fun ChallengeScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    padding: PaddingValues,
    onContinue: () -> Unit,
    onCategory: (Category) -> Unit,
    onOpenLibrary: () -> Unit,
    onCollection: (title: String, kind: String) -> Unit
) {
    val mastered = store.masteredCount(words)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeroCard(
                title = "Panda English",
                subtitle = "850 + ${words.count { it.category == Category.Extended }} 拓展词闯关 · 中英双语 · 离线可学",
                action = "继续之前",
                onAction = onContinue
            )
        }
        item {
            StatsRow(
                learned = mastered,
                mistakes = store.mistakeWords(words).size,
                favorites = store.favoriteWords(words).size,
                streak = store.dailyStreak(),
                onCollection = onCollection
            )
        }
        item {
            SectionTitle("分类闯关", "每 10 个词一关，先短跑，再复习")
        }
        items(Category.values()) { category ->
            val categoryWords = words.filter { it.category == category && it.practice }
            val learned = categoryWords.count { store.progress(it.word).mastered }
            CategoryProgressCard(
                category = category,
                learned = learned,
                total = categoryWords.size,
                onClick = { onCategory(category) }
            )
        }
        item {
            OutlinedButton(
                onClick = onOpenLibrary,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                AppText("打开完整词库")
            }
        }
    }
}

@Composable
fun HeroCard(title: String, subtitle: String, action: String, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                title,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            AppText(subtitle, color = InkSoft, fontWeight = FontWeight.Medium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAction, shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    AppText(action)
                }
                AppText("今日完成 10 词就很好", color = InkFaint, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun StatsRow(learned: Int, mistakes: Int, favorites: Int, streak: Int, onCollection: (title: String, kind: String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("已掌握", learned.toString(), Category.GeneralThings.tint, Modifier.weight(1f)) { onCollection("已掌握", "mastered") }
        StatCard("错词", mistakes.toString(), Error, Modifier.weight(1f)) { onCollection("错词本", "mistakes") }
        StatCard("收藏", favorites.toString(), Category.Opposites.tint, Modifier.weight(1f)) { onCollection("收藏夹", "favorites") }
        StatCard("连续", "${streak}天", Category.Picturable.tint, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = modifier
            .border(1.dp, Line, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp, maxLines = 1)
            AppText(label, color = InkFaint, fontSize = 12.sp)
        }
    }
}

@Composable
fun CategoryProgressCard(category: Category, learned: Int, total: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDot(category)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(category.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    AppText("${category.zh} · $total WORDS", color = InkFaint, fontSize = 12.sp)
                }
                AppText("${ceil(total / 10.0).toInt()} 关", color = category.tint, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(
                progress = learned / total.toFloat(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(99.dp)),
                color = category.tint,
                trackColor = category.soft
            )
            AppText("掌握 $learned / $total", color = InkSoft, fontSize = 13.sp)
        }
    }
}

@Composable
fun LevelSelectionScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    category: Category,
    onBack: () -> Unit,
    onStart: (Int) -> Unit
) {
    val categoryWords = words.filter { it.category == category }
    val levels = ceil(categoryWords.size / 10.0).toInt()
    Scaffold(containerColor = Paper, topBar = {
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
                Text(category.label, fontWeight = FontWeight.Bold)
                AppText("${category.zh} · $levels 个关卡", color = InkFaint, fontSize = 12.sp)
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionTitle("选择关卡", "通关上一关后，下一关才会解锁")
            }
            items((1..levels).toList()) { level ->
                val unlocked = store.isLevelUnlocked(category, level)
                val complete = store.isLevelComplete(category, level)
                val levelWords = categoryWords.drop((level - 1) * 10).take(10)
                val mastered = levelWords.count { store.progress(it.word).mastered }
                LevelCard(
                    category = category,
                    level = level,
                    complete = complete,
                    unlocked = unlocked,
                    mastered = mastered,
                    total = levelWords.size,
                    onClick = { if (unlocked) onStart(level) }
                )
            }
        }
    }
}

@Composable
fun LevelCard(
    category: Category,
    level: Int,
    complete: Boolean,
    unlocked: Boolean,
    mastered: Int,
    total: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = unlocked, onClick = onClick)
            .border(1.dp, if (unlocked) Line else Color(0xFFE8E1D4), RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = if (unlocked) PaperElevated else Color(0xFFF2EDE4)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (complete) category.tint else category.soft),
                contentAlignment = Alignment.Center
            ) {
                    AppText(
                    if (complete) "✓" else level.toString(),
                    color = if (complete) Color.White else category.tint,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                AppText("第 $level 关", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = if (unlocked) Ink else InkFaint)
                AppText(
                    when {
                        complete -> "已通关 · 可重复练习"
                        unlocked -> "本关 $total 个词 · 已掌握 $mastered"
                        else -> "先完成上一关"
                    },
                    color = InkFaint,
                    fontSize = 13.sp
                )
                LinearProgressIndicator(
                    progress = if (total == 0) 0f else mastered / total.toFloat(),
                    color = category.tint,
                    trackColor = category.soft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
            Spacer(Modifier.width(8.dp))
            AppText(
                if (unlocked) "进入" else "锁定",
                color = if (unlocked) category.tint else InkFaint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    padding: PaddingValues,
    accent: Accent,
    zh: ChineseMode,
    onSpeak: (String) -> Unit,
    onOpen: (OgdenWord) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    // 随机模式：打乱当前筛选结果；「换一批」换种子重新打乱
    var randomOrder by rememberSaveable { mutableStateOf(false) }
    var shuffleSeed by rememberSaveable { mutableStateOf(0) }
    // 前缀匹配：英文按单词开头，中文按任一义项开头（zh 形如「来,前来」）
    val prefix = query.trim()
    val matched = words.filter { word ->
        (category == null || word.category == category) &&
            (prefix.isEmpty() ||
                word.word.startsWith(prefix, ignoreCase = true) ||
                word.zh.split(',', '，', ';', '；').any { it.trim().startsWith(prefix) })
    }
    val filtered = remember(matched, randomOrder, shuffleSeed) {
        if (randomOrder) matched.shuffled(Random(shuffleSeed)) else matched
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { AppText("搜索单词、中文或释义") },
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = category == null, onClick = { category = null }, label = { Text("All · ${words.size}") })
                Category.values().forEach {
                    FilterChip(
                        selected = category == it,
                        onClick = { category = it },
                        label = { AppText("${it.zh} ${words.count { w -> w.category == it }}") }
                    )
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText("显示 ${filtered.size} 个词", color = InkFaint, fontSize = 13.sp, modifier = Modifier.weight(1f))
                if (randomOrder) {
                    TextButton(onClick = { shuffleSeed++ }) { AppText("换一批") }
                }
                FilterChip(
                    selected = randomOrder,
                    onClick = {
                        randomOrder = !randomOrder
                        if (randomOrder) shuffleSeed = (0..Int.MAX_VALUE).random()
                    },
                    leadingIcon = { Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text("随机") }
                )
            }
        }
        items(filtered, key = { it.word }) { word ->
            WordListCard(
                word = word,
                progress = store.progress(word.word),
                accent = accent,
                zh = zh,
                onSpeak = onSpeak,
                onClick = { onOpen(word) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WordListCard(
    word: OgdenWord,
    progress: WordProgress,
    accent: Accent,
    zh: ChineseMode,
    onSpeak: (String) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(word.word, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                        if (progress.favorite) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.Favorite, contentDescription = "已收藏", tint = Error, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        // 音标让位：长音标省略，保证后面的口语标签不被挤成竖排
                        Text(
                            if (accent == Accent.UK) word.ipaUk else word.ipaUs,
                            color = InkFaint,
                            fontStyle = FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        registerOf(word.word)?.let { Spacer(Modifier.width(8.dp)); RegisterBadge(it.tag) }
                    }
                    Text(convertZh(word.zh, zh), fontWeight = FontWeight.Medium, color = Ink)
                }
                IconButton(onClick = { onSpeak(word.word) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = word.category.tint)
                }
            }
            TranslatableText(AnnotatedString(word.example), TextStyle(color = InkSoft, fontFamily = FontFamily.Serif, fontSize = 16.sp))
            if (word.exampleZh.isNotBlank()) AppText(convertZh(word.exampleZh, zh), color = InkFaint, fontSize = 14.sp)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                word.synonyms.take(3).forEach { SynonymChip(it, onSpeak) }
                repeat(progress.mastery) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Category.Picturable.tint, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun WordDetailScreen(
    word: OgdenWord,
    progress: WordProgress,
    accent: Accent,
    zh: ChineseMode,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onSpeakZh: (String) -> Unit,
    onFavorite: () -> WordProgress
) {
    var visibleProgress by remember(word.word, progress.favorite, progress.mastery, progress.attempts, progress.correct) {
        mutableStateOf(progress)
    }
    // 一进详情页就读一遍单词；换词时重新读
    LaunchedEffect(word.word) { onSpeak(word.word) }
    Scaffold(containerColor = Paper, topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            AppText("单词详情", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { visibleProgress = onFavorite() }) {
                Icon(
                    if (visibleProgress.favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "收藏",
                    tint = if (visibleProgress.favorite) Error else InkFaint
                )
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // 长词（adjustment 等）46sp 放不下一行：保持单行，逐步缩小字号直到放下，缩好前不绘制避免闪烁
                            var wordFontSize by remember(word.word) { mutableStateOf(46.sp) }
                            var wordFitted by remember(word.word) { mutableStateOf(false) }
                            Text(
                                word.word,
                                fontSize = wordFontSize,
                                lineHeight = wordFontSize * 1.2f,
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                onTextLayout = {
                                    if (it.didOverflowWidth && wordFontSize > 24.sp) wordFontSize *= 0.9f else wordFitted = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .alpha(if (wordFitted) 1f else 0f)
                            )
                            IconButton(onClick = { onSpeak(word.word) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = word.category.tint)
                            }
                        }
                        Text(if (accent == Accent.UK) word.ipaUk else word.ipaUs, color = InkFaint, fontSize = 16.sp)
                        CategoryBadge(word.category)
                        Text(
                            convertZh(word.zh, zh),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onSpeakZh(word.zh) }
                        )
                        RegisterNote(word.word)
                        Text(word.englishDefinition, color = InkSoft, fontStyle = FontStyle.Italic)
                    }
                }
            }
            items(word.examples.withIndex().toList()) { (index, example) ->
                val (en, exampleZh) = example
                InfoBlock(
                    if (word.examples.size > 1) "例句 ${index + 1}" else "例句",
                    en,
                    convertZh(exampleZh, zh),
                    word.category.tint,
                    onSpeakZh = { onSpeakZh(exampleZh) }
                ) {
                    onSpeak(en)
                }
            }
            if (word.collocations.isNotEmpty()) {
                item {
                    SectionTitle("常见搭配", "点短语或例句听朗读")
                }
                items(word.collocations) { c ->
                    CollocationCard(c, zh, onSpeak, onSpeakZh)
                }
            }
            if (word.synonyms.isNotEmpty()) {
                item {
                    SectionTitle(if (word.category.hasTrueSynonyms) "近义词" else "相关词", "点击听发音，长按查看")
                    FlowRowCompat(word.synonyms) { syn -> SynonymChip(syn, onSpeak) }
                }
            }
            if (word.antonyms.isNotEmpty()) {
                item {
                    SectionTitle("反义词", "点击听发音，长按查看")
                    FlowRowCompat(word.antonyms) { ant -> SynonymChip(ant, onSpeak) }
                }
            }
            item {
                SectionTitle("熟练度", "答对会增加星星，答错会进入复习")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(3) { index ->
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < visibleProgress.mastery) Category.Picturable.tint else Line,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                AppText("练习 ${visibleProgress.attempts} 次 · 答对 ${visibleProgress.correct} 次", color = InkFaint)
            }
        }
    }
}

@Composable
fun ReviewScreen(
    words: List<OgdenWord>,
    store: ProgressStore,
    padding: PaddingValues,
    onMistakes: () -> Unit,
    onFavorites: () -> Unit,
    onSpeechWords: () -> Unit
) {
    val mistakes = store.mistakeWords(words)
    val favorites = store.favoriteWords(words)
    val speechWords = store.speechWords()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("复习中心", "错词与收藏先收好，需要时再打开")
        }
        item {
            ReviewEntryCard(
                title = "错词本",
                subtitle = if (mistakes.isEmpty()) "暂时没有错词" else "先把不熟的词变熟",
                count = mistakes.size,
                tint = Error,
                icon = Icons.Default.Refresh,
                onClick = onMistakes
            )
        }
        item {
            ReviewEntryCard(
                title = "收藏夹",
                subtitle = if (favorites.isEmpty()) "还没有收藏" else "适合睡前再看一遍",
                count = favorites.size,
                tint = Category.Opposites.tint,
                icon = Icons.Default.Favorite,
                onClick = onFavorites
            )
        }
        item {
            ReviewEntryCard(
                title = "课文生词",
                subtitle = if (speechWords.isEmpty()) "在课文里长按查过的单词会自动收进来" else "课文里长按查过的单词",
                count = speechWords.size,
                tint = Category.Qualities.tint,
                icon = Icons.Default.RecordVoiceOver,
                onClick = onSpeechWords
            )
        }
    }
}

@Composable
fun ReviewEntryCard(
    title: String,
    subtitle: String,
    count: Int,
    tint: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(1.dp, Line, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                AppText(title, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                AppText(subtitle, color = InkFaint, fontSize = 13.sp)
            }
            Text("$count", color = tint, fontWeight = FontWeight.Bold, fontSize = 28.sp)
        }
    }
}

@Composable
fun WordCollectionScreen(
    title: String,
    words: List<OgdenWord>,
    store: ProgressStore,
    zh: ChineseMode,
    onBack: () -> Unit,
    onOpen: (OgdenWord) -> Unit,
    onPractice: (() -> Unit)? = null,
    onRemove: ((OgdenWord) -> Unit)? = null
) {
    Scaffold(containerColor = Paper, topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            AppText("$title · ${words.size}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (onPractice != null && words.isNotEmpty()) {
                TextButton(onClick = onPractice) { AppText("练习错词", fontWeight = FontWeight.Bold) }
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (words.isEmpty()) {
                item { EmptyCard("这里暂时还没有单词。") }
            } else {
                items(words, key = { it.word }) { word ->
                    CompactWordRow(word, store.progress(word.word), zh, onOpen, onRemove)
                }
            }
        }
    }
}

private val SpeechLevelNames = mapOf(1 to "一级", 2 to "二级", 3 to "三级", StemLevel to "数理")
private val SpeechLevelThemes = mapOf(1 to "起步", 2 to "成长", 3 to "表达", StemLevel to "启蒙")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SpeechListScreen(
    speeches: List<Speech>,
    store: ProgressStore,
    level: Int,
    onLevel: (Int) -> Unit,
    onPractice: (SpeechTheme) -> Unit,
    modules: List<SpecialModule>,
    onSpecial: (SpecialModule) -> Unit,
    padding: PaddingValues,
    onOpen: (Speech) -> Unit
) {
    val units = speeches.filter { it.level == level }
    val learned = units.count { store.isSpeechLearned(it.id) }
    val groups = units.groupBy { it.theme }.toList()
    // 列表头部依次是：标题、吸顶选择栏、进度行、专项训练，之后每个主题占 1 个标题项 + 若干单元项
    val themeStarts = remember(groups) {
        var index = 4
        groups.map { (_, themeUnits) -> index.also { index += 1 + themeUnits.size } }
    }
    val listState = rememberLazyListState()
    val chipState = rememberLazyListState()
    var themesExpanded by remember { mutableStateOf(false) }
    var specialsExpanded by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentTheme by remember(themeStarts) {
        derivedStateOf { themeStarts.indexOfLast { it <= listState.firstVisibleItemIndex + 1 }.coerceAtLeast(0) }
    }
    LaunchedEffect(currentTheme, themesExpanded) { if (!themesExpanded) chipState.animateScrollToItem(currentTheme) }
    // 只在切换级别时回到顶部；从别的页面返回时保留原滚动位置
    var shownLevel by rememberSaveable { mutableStateOf(level) }
    LaunchedEffect(level) {
        if (level != shownLevel) {
            shownLevel = level
            listState.scrollToItem(0)
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionTitle("课文", "先听课文跟读，再用句型替换练说")
        }
        stickyHeader {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Paper)
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpeechLevelNames.forEach { (value, name) ->
                        FilterChip(
                            selected = level == value,
                            onClick = { onLevel(value) },
                            label = { Text(name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                @Composable
                fun themeChip(index: Int, theme: SpeechTheme, themeUnits: List<Speech>, modifier: Modifier = Modifier) {
                    FilterChip(
                        selected = index == currentTheme,
                        onClick = {
                            themesExpanded = false
                            scope.launch { listState.animateScrollToItem(themeStarts[index]) }
                        },
                        label = { AppText("${theme.zh} ${themeUnits.count { store.isSpeechLearned(it.id) }}/${themeUnits.size}") },
                        modifier = modifier
                    )
                }
                Row(verticalAlignment = Alignment.Top) {
                    if (themesExpanded) {
                        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            groups.forEachIndexed { index, (theme, themeUnits) ->
                                themeChip(index, theme, themeUnits, Modifier.padding(bottom = 4.dp))
                            }
                        }
                    } else {
                        LazyRow(state = chipState, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(groups, key = { _, (theme, _) -> theme.key }) { index, (theme, themeUnits) ->
                                themeChip(index, theme, themeUnits)
                            }
                        }
                    }
                    IconButton(onClick = { themesExpanded = !themesExpanded }) {
                        Icon(
                            if (themesExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (themesExpanded) "收起主题" else "展开全部主题"
                        )
                    }
                }
            }
        }
        item {
            AppText("${SpeechLevelThemes[level]} · 已学 $learned / ${units.size} 单元", color = InkFaint, fontSize = 13.sp)
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { specialsExpanded = !specialsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText("专项训练", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                    AppText(" · ${modules.size} 个模块", color = InkFaint, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Icon(
                        if (specialsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (specialsExpanded) "收起专项训练" else "展开专项训练"
                    )
                }
                if (specialsExpanded) modules.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { module ->
                            OutlinedButton(
                                onClick = { onSpecial(module) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 8.dp)
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(module.icon, fontSize = 22.sp)
                                    AppText(module.zh, fontSize = 13.sp, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        groups.forEach { (theme, themeUnits) ->
            item(key = "theme-${theme.key}") {
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        AppText(theme.zh, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                        val best = store.bestThemeScore(level, theme)
                        AppText(
                            "${theme.en} · 已学 ${themeUnits.count { store.isSpeechLearned(it.id) }} / ${themeUnits.size}" +
                                (best?.let { " · 练习最佳 $it" } ?: ""),
                            color = InkFaint,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(onClick = { onPractice(theme) }) { Text("主题练习") }
                }
            }
            items(themeUnits, key = { it.id }) { speech ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(speech) }
                        .border(1.dp, Line, RoundedCornerShape(16.dp))
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppText(
                            "${speech.unit}",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Category.Operations.tint,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(44.dp)
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(speech.title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = Ink)
                            AppText(speech.titleZh, color = InkSoft)
                            AppText("${speech.lines.size} 句 · ${speech.patterns.size} 个句型", color = InkFaint, fontSize = 12.sp)
                        }
                        if (store.isSpeechLearned(speech.id)) {
                            Icon(Icons.Default.Check, contentDescription = "已学", tint = Success)
                        }
                    }
                }
            }
        }
    }
}

/** [line] 在课文中是句序号，句型接在其后编号，这样两段共用一个选中状态。 */
private data class SelectedSpeechWord(val line: Int, val token: SpeechToken)

/** 词表外单词的收藏键：小写并去掉所有格。 */
private fun speechWordKey(token: String) = token.lowercase().removeSuffix("'s")

/** 课文生词的存储键：词表内的词存原形（boxes → box），词表外的存原文。 */
private fun lessonWordKey(token: SpeechToken, word: OgdenWord?) = word?.word?.lowercase() ?: speechWordKey(token.text)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpeechReaderScreen(
    speech: Speech,
    wordIndex: Map<String, OgdenWord>,
    lemmaVocabulary: LemmaVocabulary,
    store: ProgressStore,
    accent: Accent,
    onBack: () -> Unit,
    onLearnedChange: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakEnglish: (text: String, onDone: (() -> Unit)?) -> Unit,
    onSpeakChinese: (text: String, onDone: (() -> Unit)?) -> Unit,
    onStopSpeaking: () -> Unit,
    onOpenWord: (OgdenWord) -> Unit,
    onPracticeWords: (List<String>) -> Unit
) {
    var selected by remember(speech.id) { mutableStateOf<SelectedSpeechWord?>(null) }
    // 正在朗读的句子（下标同 SelectedSpeechWord.line）；playingAll 为 true 时读完一句自动接下一句
    var speakingIndex by remember(speech.id) { mutableStateOf<Int?>(null) }
    // 本课单词里最近点过的词，关掉词卡后仍保持选中，方便孩子知道读到哪了
    var pickedWord by rememberSaveable(speech.id) { mutableStateOf<String?>(null) }
    var playingAll by remember(speech.id) { mutableStateOf(false) }
    // 正在反复朗读的句子；读完一遍停一会儿再读，直到被停止或被别的朗读打断
    var repeatIndex by remember(speech.id) { mutableStateOf<Int?>(null) }
    val repeatScope = rememberCoroutineScope()
    var readAlong by remember(speech.id) { mutableStateOf<SpeechLine?>(null) }
    // 跟读成绩存在 SharedPreferences，不是 Compose 状态；保存后递增它让卡片重读
    var readAlongVersion by remember { mutableStateOf(0) }
    val listState = rememberLazyListState()
    var showTranslation by remember(speech.id) { mutableStateOf(true) }
    var learned by remember(speech.id) { mutableStateOf(store.isSpeechLearned(speech.id)) }
    // 收藏存在 SharedPreferences 里，不是 Compose 状态；改动后递增它，让高亮与词卡重新读取
    var favoriteVersion by remember { mutableStateOf(0) }
    fun ogdenWordOf(token: SpeechToken) = lemmatize(token.text, lemmaVocabulary)?.let { wordIndex[it] }
    fun isSaved(token: SpeechToken): Boolean {
        val word = ogdenWordOf(token)
        return (word != null && store.progress(word.word).favorite) || store.isSpeechWordSaved(lessonWordKey(token, word))
    }
    val allLines = speech.lines + speech.patterns
    val hasContraction = remember(speech.id) { allLines.any { line -> tokenizeSpeech(line.en).any { contractionOf(it.text) != null } } }

    fun speakOne(index: Int, chinese: Boolean) {
        playingAll = false
        repeatIndex = null
        speakingIndex = index
        val line = allLines[index]
        val onDone = { if (speakingIndex == index && !playingAll) speakingIndex = null }
        if (chinese) onSpeakChinese(line.zh, onDone) else onSpeakEnglish(line.en, onDone)
    }

    fun playFrom(index: Int) {
        speakingIndex = index
        onSpeakEnglish(speech.lines[index].en) {
            if (!playingAll) return@onSpeakEnglish
            if (index + 1 < speech.lines.size) {
                playFrom(index + 1)
            } else {
                playingAll = false
                speakingIndex = null
            }
        }
    }

    fun stopSpeaking() {
        onStopSpeaking()
        playingAll = false
        repeatIndex = null
        speakingIndex = null
    }

    fun repeatLoop(index: Int) {
        speakingIndex = index
        onSpeakEnglish(allLines[index].en) {
            if (repeatIndex == index) repeatScope.launch {
                delay(700)
                if (repeatIndex == index) repeatLoop(index)
            }
        }
    }

    fun startRepeat(index: Int) {
        stopSpeaking()
        repeatIndex = index
        repeatLoop(index)
    }

    LaunchedEffect(speakingIndex, playingAll) {
        // 列表第 0 项是课文标题，句子从第 1 项开始
        val index = speakingIndex
        if (playingAll && index != null) listState.animateScrollToItem(index + 1)
    }

    @Composable
    fun lineRow(index: Int) {
        val line = allLines[index]
        val tokens = remember(line.en) { tokenizeSpeech(line.en) }
        val savedRanges = remember(line.en, favoriteVersion) { tokens.filter { isSaved(it) }.map { it.range } }
        SpeechLineRow(
            line = line,
            tokens = tokens,
            savedRanges = savedRanges,
            selectedRange = selected?.takeIf { it.line == index }?.token?.range,
            showTranslation = showTranslation,
            speaking = speakingIndex == index,
            onSpeakEnglish = { speakOne(index, chinese = false) },
            onSpeakChinese = { speakOne(index, chinese = true) },
            onTokenClick = { selected = SelectedSpeechWord(index, it) },
            repeating = repeatIndex == index,
            onRepeat = { startRepeat(index) },
            onReadAlong = { stopSpeaking(); readAlong = line },
            readAlongScore = remember(line.en, readAlongVersion) { store.readAlongScore(line.en) }
        )
    }

    Scaffold(containerColor = Paper, floatingActionButtonPosition = FabPosition.Center, floatingActionButton = {
        // 全文朗读、重复播放时悬浮在底部的停止按钮；单句很短，不需要
        if (playingAll || repeatIndex != null) {
            ExtendedFloatingActionButton(
                onClick = { stopSpeaking() },
                icon = { Icon(Icons.Default.Close, contentDescription = null) },
                text = { Text(if (repeatIndex != null) "停止重复" else "停止朗读", fontSize = 18.sp) },
                containerColor = Category.Operations.tint,
                contentColor = Color.White
            )
        }
    }, topBar = {
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
                Text(speech.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                AppText("${SpeechLevelNames[speech.level]}${SpeechLevelThemes[speech.level]} · Unit ${speech.unit} · ${speech.titleZh}", color = InkFaint, fontSize = 12.sp)
            }
            TextButton(onClick = { showTranslation = !showTranslation }) {
                AppText(if (showTranslation) "收起译文" else "显示译文", fontSize = 13.sp)
            }
        }
    }) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                SpeechTrackHeader(
                    title = "课文 · Listen and Read",
                    subtitle = if (hasContraction) "点句子听朗读，长按喇叭重复，长按单词选词，长按空白处跟读评分；带下划线的是缩写"
                    else "点句子听朗读，长按喇叭重复，长按单词选词，长按空白处跟读评分",
                    playingAll = playingAll,
                    onPlayAll = {
                        if (playingAll) {
                            stopSpeaking()
                        } else {
                            stopSpeaking()
                            playingAll = true
                            playFrom(0)
                        }
                    }
                )
            }
            items(speech.lines.size) { lineRow(it) }
            item {
                Spacer(Modifier.height(8.dp))
                SpeechTrackHeader(
                    title = "句型 · Listen and Speak",
                    subtitle = "句型练习：听一遍，换上自己的词说一说",
                    playingAll = false,
                    onPlayAll = null
                )
            }
            items(speech.patterns.size) { lineRow(speech.lines.size + it) }
            if (speech.words.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(8.dp))
                    SpeechTrackHeader(
                        title = "Words · 本课单词",
                        subtitle = "点单词听发音、看释义",
                        playingAll = false,
                        onPlayAll = null
                    )
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        speech.words.forEach { w ->
                            val picked = pickedWord == w
                            val saved = remember(w, favoriteVersion) { isSaved(SpeechToken(w, w.indices)) }
                            OutlinedButton(
                                onClick = {
                                    stopSpeaking()
                                    pickedWord = w
                                    selected = SelectedSpeechWord(-1, SpeechToken(w, w.indices))
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (picked) Category.Operations.soft else Color.Transparent
                                ),
                                border = BorderStroke(if (picked) 2.dp else 1.dp, if (picked) Category.Operations.tint else Line),
                                modifier = Modifier.padding(bottom = 8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                if (saved) {
                                    Icon(Icons.Default.Favorite, contentDescription = "已收藏", tint = Error, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    wordIndex[w.lowercase()]?.word ?: w,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = if (saved) FontWeight.Bold else null,
                                    color = if (saved) Category.Opposites.tint else Ink
                                )
                                registerOf(w)?.let { Spacer(Modifier.width(6.dp)); RegisterBadge(it.tag) }
                            }
                        }
                    }
                }
                item {
                    Button(onClick = { stopSpeaking(); onPracticeWords(speech.words) }, modifier = Modifier.fillMaxWidth()) {
                        Text("单词练习（${speech.words.size} 词，每次 10 题）", fontSize = 18.sp)
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                if (learned) {
                    OutlinedButton(
                        onClick = { store.toggleSpeechLearned(speech.id); learned = false; onLearnedChange() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("已学会 · 点此取消") }
                } else {
                    Button(
                        onClick = { store.toggleSpeechLearned(speech.id); learned = true; onLearnedChange() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("我学会了") }
                }
            }
        }
    }

    readAlong?.let { line ->
        ReadAlongSheet(line, onScored = { store.saveReadAlongScore(line.en, it); readAlongVersion++ }) { readAlong = null }
    }

    // 弹窗一出来就朗读标题上的词：词表词用离线录音，缩写和词表外的词读原文
    LaunchedEffect(selected) {
        val current = selected ?: return@LaunchedEffect
        val token = current.token
        stopSpeaking()
        val word = ogdenWordOf(token)
        if (word != null && contractionOf(token.text) == null) onSpeakWord(word.word) else onSpeakEnglish(token.text, null)
        // 在句子里长按查过的词自动记入「课文生词」；点本课单词（line = -1）和缩写不记
        if (current.line >= 0 && contractionOf(token.text) == null) {
            store.addSpeechWord(lessonWordKey(token, word))
            favoriteVersion++
        }
    }

    selected?.let { current ->
        val word = ogdenWordOf(current.token)
        val saved = remember(current, favoriteVersion) { isSaved(current.token) }
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = PaperElevated) {
            SpeechWordSheet(
                surface = current.token.text,
                word = word,
                contraction = contractionOf(current.token.text),
                lookup = { wordIndex[it.lowercase()] },
                accent = accent,
                saved = saved,
                onToggleSave = {
                    store.toggleSpeechWord(lessonWordKey(current.token, word))
                    favoriteVersion++
                },
                onSpeakWord = { stopSpeaking(); onSpeakWord(it) },
                onSpeakEnglish = { stopSpeaking(); onSpeakEnglish(it, null) },
                onSpeakChinese = { stopSpeaking(); onSpeakChinese(it, null) },
                onOpenWord = { selected = null; onOpenWord(it) }
            )
        }
    }
}

@Composable
private fun SpeechTrackHeader(title: String, subtitle: String, playingAll: Boolean, onPlayAll: (() -> Unit)?) {
    Column {
        AppText(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppText(subtitle, color = InkFaint, fontSize = 12.sp, modifier = Modifier.weight(1f))
            if (onPlayAll != null) {
                TextButton(onClick = onPlayAll) {
                    Icon(if (playingAll) Icons.Default.Close else Icons.Default.PlayArrow, contentDescription = null)
                    AppText(if (playingAll) "停止朗读" else "全文朗读", fontSize = 13.sp)
                }
            }
        }
    }
}

/** 划词翻译与弹窗朗读要用的服务，由根部提供，避免一层层传参。 */
class SpeechServices(
    val translator: Translator?,
    val speakEnglish: (String) -> Unit,
    val speakChinese: (String) -> Unit,
    /** 原文词 → 词库词条（含词形还原），查不到为 null */
    val lookup: (String) -> OgdenWord?,
    /** 打开词条详情页，返回时回到当前页面 */
    val openWord: (OgdenWord) -> Unit,
    /** 词库外近义词 / 反义词的离线释义与例句 */
    val related: (String) -> RelatedWord? = { null },
    val stopSpeaking: () -> Unit = {}
)

val LocalSpeechServices = staticCompositionLocalOf { SpeechServices(null, {}, {}, { null }, {}) }

/** 当前处于选词状态的那段文字：根部据此判断「点在外面」并取消选中。同一时间只有一处选中。 */
class SelectionController {
    var owner: Any? = null
    var bounds: Rect? = null
    var clear: (() -> Unit)? = null

    fun release(who: Any) {
        if (owner === who) {
            owner = null
            bounds = null
            clear = null
        }
    }
}

val LocalSelectionController = staticCompositionLocalOf { SelectionController() }

/** 有选中时，点在选区（含工具条）外面只取消选中，整次点击被吞掉，不触发底下的按钮或滚动。 */
private fun Modifier.dismissSelectionOnOutsideTap(controller: SelectionController) = pointerInput(controller) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val clear = controller.clear ?: return@awaitEachGesture
        if (controller.bounds?.contains(down.position) == true) return@awaitEachGesture
        clear()
        down.consume()
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            event.changes.forEach { it.consume() }
        } while (event.changes.any { it.pressed })
    }
}

/**
 * 可划词的英文。点击走 [onTap]；长按一个词进入选词状态（高亮 + 震动 + 下方工具条），
 * 之后点别的词、拖两端圆点或长按拖动都能调整选区，再从工具条选「查词 / 翻译 / 取消」。
 * 松手不会自动出结果，选错了可以接着改。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TranslatableText(
    text: AnnotatedString,
    style: TextStyle,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    onWord: ((SpeechToken) -> Unit)? = null
) {
    val services = LocalSpeechServices.current
    val controller = LocalSelectionController.current
    val owner = remember { Any() }
    var myBounds by remember { mutableStateOf<Rect?>(null) }
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val tokens = remember(text.text) { tokenizeSpeech(text.text) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    // 选区端点（词下标）；selFrom 为锚点，selTo 为正在移动的一端
    var selFrom by remember(text.text) { mutableStateOf<Int?>(null) }
    var selTo by remember(text.text) { mutableStateOf<Int?>(null) }
    var phrase by remember { mutableStateOf<String?>(null) }
    val active = selFrom != null && selTo != null
    val first = if (active) minOf(selFrom!!, selTo!!) else -1
    val last = if (active) maxOf(selFrom!!, selTo!!) else -1
    val selectedRange = if (active) tokens[first].range.first..tokens[last].range.last else null
    val selectedText = selectedRange?.let { text.text.substring(it) }
    // 只选了一个词库里的词：用查词或直接显示释义，不再给「翻译」
    val dictionaryWord = if (active && first == last) services.lookup(tokens[first].text) else null

    fun clear() {
        selFrom = null
        selTo = null
    }

    /**
     * 手指位置 → 词下标，带防抖：越过目标词一半才切过去；上下偏离当前行不到一行高时仍按当前行算。
     * [current] 为正在移动的那一端当前所在的词。
     */
    fun tokenAt(position: Offset, current: Int?): Int? {
        val l = layout ?: return null
        if (tokens.isEmpty()) return null
        var y = position.y
        if (current != null) {
            val line = l.getLineForOffset(tokens[current].range.first)
            val top = l.getLineTop(line)
            val bottom = l.getLineBottom(line)
            if (abs(y - (top + bottom) / 2) < (bottom - top) * 0.9f) y = (top + bottom) / 2
        }
        val offset = l.getOffsetForPosition(Offset(position.x, y))
        val nearest = tokens.indices.minByOrNull { i ->
            val r = tokens[i].range
            when {
                offset < r.first -> r.first - offset
                offset > r.last -> offset - r.last
                else -> 0
            }
        } ?: return null
        if (current == null || nearest == current) return nearest
        val r = tokens[nearest].range
        val half = (r.last - r.first + 1) / 2
        val crossed = if (nearest > current) offset >= r.first + half else offset <= r.last - half
        return if (crossed) nearest else current
    }

    fun moveEnd(to: Int?, anchorIsFrom: Boolean = true) {
        if (to == null) return
        if (anchorIsFrom) {
            if (to != selTo) { selTo = to; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        } else {
            if (to != selFrom) { selFrom = to; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
        }
    }

    val shown = remember(text, selectedRange) {
        buildAnnotatedString {
            append(text)
            selectedRange?.let {
                addStyle(SpanStyle(background = Category.Qualities.soft, color = Category.Qualities.tint), it.first, it.last + 1)
            }
        }
    }

    SideEffect {
        if (active) {
            // 别处已有选中时先取消它
            if (controller.owner !== owner) controller.clear?.invoke()
            controller.owner = owner
            controller.bounds = myBounds
            controller.clear = { clear() }
        } else {
            controller.release(owner)
        }
    }
    DisposableEffect(owner) { onDispose { controller.release(owner) } }

    Column(modifier.onGloballyPositioned { myBounds = it.boundsInRoot() }) {
        Box {
            Text(
                text = shown,
                style = style,
                onTextLayout = { layout = it },
                modifier = Modifier
                    .then(
                        // 无选区且调用方不需要点击时不装点击手势，免得吞掉外层卡片的点击
                        if (active || onTap != null) Modifier.pointerInput(tokens, active) {
                            detectTapGestures(onTap = { position ->
                                if (active) {
                                    // 点另一个词：选区扩到那个词（以离它更远的一端为锚点）
                                    tokenAt(position, null)?.let { t ->
                                        if (abs(t - first) > abs(t - last)) { selFrom = first; moveEnd(t) } else { selFrom = last; moveEnd(t) }
                                    }
                                } else {
                                    onTap?.invoke()
                                }
                            })
                        } else Modifier
                    )
                    .pointerInput(tokens) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { position ->
                                tokenAt(position, null)?.let {
                                    selFrom = it
                                    selTo = it
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                moveEnd(tokenAt(change.position, selTo))
                            }
                        )
                    }
            )
            // 两端圆点：拖动微调选区
            val l = layout
            if (active && l != null) {
                val handle = 22.dp
                val handlePx = with(density) { handle.toPx() }
                listOf(true, false).forEach { isStart ->
                    val box = l.getBoundingBox(if (isStart) tokens[first].range.first else tokens[last].range.last)
                    val anchor = Offset(if (isStart) box.left else box.right, box.bottom)
                    var dragPos by remember(isStart, first, last) { mutableStateOf(anchor) }
                    Box(
                        Modifier
                            .offset { IntOffset((anchor.x - handlePx / 2).roundToInt(), anchor.y.roundToInt()) }
                            .size(handle)
                            .clip(CircleShape)
                            .background(Category.Qualities.tint)
                            .pointerInput(isStart, first, last) {
                                detectDragGestures(
                                    onDragStart = {
                                        // 拖起点时以终点为锚，反之亦然
                                        if (isStart) { selFrom = last; selTo = first } else { selFrom = first; selTo = last }
                                        dragPos = Offset(anchor.x, anchor.y - handlePx)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragPos += amount
                                        moveEnd(tokenAt(dragPos, selTo))
                                    }
                                )
                            }
                    )
                }
            }
        }
        if (active && selectedText != null) {
            Spacer(Modifier.height(18.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Category.Qualities.soft),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppText(
                        "已选：$selectedText" + (dictionaryWord?.takeIf { onWord == null }?.let { " · ${it.zh}" } ?: ""),
                        color = Category.Qualities.tint,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 有查词弹窗的地方用弹窗；其余地方词库里的词直接打开词典详情
                        if (first == last && (onWord != null || dictionaryWord != null)) {
                            Button(
                                onClick = {
                                    val t = tokens[first]
                                    clear()
                                    if (onWord != null) onWord(t) else dictionaryWord?.let(services.openWord)
                                },
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) { Text("查词") }
                        }
                        if (dictionaryWord == null) {
                            Button(onClick = { phrase = selectedText }, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("翻译") }
                        }
                        TextButton(onClick = { clear() }) { Text("取消") }
                    }
                }
            }
        }
    }

    phrase?.let { selected ->
        ModalBottomSheet(onDismissRequest = { phrase = null; clear() }, containerColor = PaperElevated) {
            PhraseTranslationSheet(selected)
        }
    }
}

/** 近义词：单击朗读；长按进入词典详情，不在词库里的弹出翻译。 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SynonymChip(text: String, onSpeak: (String) -> Unit) {
    val services = LocalSpeechServices.current
    val haptic = LocalHapticFeedback.current
    var translating by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Line, RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = { onSpeak(text) },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val entry = services.lookup(text)
                    if (entry != null) services.openWord(entry) else translating = true
                }
            )
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(text, fontSize = 15.sp, color = Ink)
    }
    if (translating) {
        ModalBottomSheet(onDismissRequest = { translating = false }, containerColor = PaperElevated) {
            val related = services.related(text)
            if (related != null) RelatedWordSheet(text, related) else PhraseTranslationSheet(text)
        }
    }
}

/** 词库外的近义词 / 反义词：离线释义 + 例句，不需要联网。 */
@Composable
private fun RelatedWordSheet(word: String, related: RelatedWord) {
    val services = LocalSpeechServices.current
    LaunchedEffect(word) { services.speakEnglish(word) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(word, fontSize = 32.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { services.speakEnglish(word) }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = Category.Operations.tint)
            }
        }
        AppText(related.zh, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Card(colors = CardDefaults.cardColors(containerColor = Paper), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TranslatableText(
                        AnnotatedString(related.example),
                        TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 28.sp, color = Ink),
                        modifier = Modifier.weight(1f),
                        onTap = { services.speakEnglish(related.example) }
                    )
                    IconButton(onClick = { services.speakEnglish(related.example) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = Category.Operations.tint)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText(related.exampleZh, color = InkSoft, fontSize = 17.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                    IconButton(onClick = { services.speakChinese(related.exampleZh) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "读例句中文", tint = InkFaint)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhraseTranslationSheet(phrase: String) {
    val services = LocalSpeechServices.current
    var result by remember(phrase) { mutableStateOf<Result<String>?>(null) }
    LaunchedEffect(phrase) {
        services.speakEnglish(phrase)
        result = services.translator?.translate(phrase) ?: Result.failure(IllegalStateException("未配置翻译"))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppText("划词翻译", color = InkFaint, fontSize = 13.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                phrase,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 34.sp,
                color = Ink,
                modifier = Modifier.weight(1f).clickable { services.speakEnglish(phrase) }
            )
            IconButton(onClick = { services.speakEnglish(phrase) }) {
                Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = Category.Operations.tint)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Paper), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            val current = result
            Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    current == null -> AppText("翻译中……", color = InkFaint, fontSize = 18.sp, modifier = Modifier.weight(1f))
                    current.isSuccess -> {
                        val zh = current.getOrThrow()
                        AppText(zh, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp, modifier = Modifier.weight(1f).clickable { services.speakChinese(zh) })
                        IconButton(onClick = { services.speakChinese(zh) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "朗读中文", tint = InkFaint)
                        }
                    }
                    else -> AppText("翻译暂时不可用，请检查网络或翻译密钥", color = Error, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
                }
            }
        }
    }
}

private sealed class ReadAlongState {
    object Idle : ReadAlongState()
    object Recording : ReadAlongState()
    object Scoring : ReadAlongState()
    data class Done(val result: Assessment, val pcm: ByteArray) : ReadAlongState()
    data class Failed(val message: String, val pcm: ByteArray?) : ReadAlongState()
}

/** 跟读评分：录一遍、交给 Azure 发音评估，按词标颜色并给出总分。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReadAlongSheet(line: SpeechLine, onScored: (Int) -> Unit = {}, onDismiss: () -> Unit) {
    val services = LocalSpeechServices.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember(line.en) { mutableStateOf<ReadAlongState>(ReadAlongState.Idle) }
    var stopRequested by remember { mutableStateOf(false) }
    var level by remember { mutableStateOf(0f) }
    var playback by remember { mutableStateOf<AudioTrack?>(null) }
    DisposableEffect(Unit) { onDispose { playback?.release() } }

    fun startRecording() {
        playback?.release()
        playback = null
        stopRequested = false
        state = ReadAlongState.Recording
        scope.launch {
            val pcm = runCatching {
                PronunciationScorer.record({ stopRequested }, PronunciationScorer.maxMsFor(line.en)) { level = it }
            }.getOrElse {
                state = ReadAlongState.Failed("麦克风打不开，请检查录音权限", null)
                return@launch
            }
            if (pcm.size < 16000) {
                state = ReadAlongState.Failed("录音太短了，按住话筒读完再松开", null)
                return@launch
            }
            state = ReadAlongState.Scoring
            state = PronunciationScorer.assess(pcm, line.en).fold(
                onSuccess = {
                    onScored(it.pron.roundToInt())
                    ReadAlongState.Done(it, pcm)
                },
                onFailure = { ReadAlongState.Failed(if (it.message == "没有听清楚") "没有听清楚，靠近一点再读一遍" else "评分暂时不可用，请检查网络", pcm) }
            )
        }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // 授权弹窗打断了这次按住，要重新按
        if (!granted) state = ReadAlongState.Failed("需要麦克风权限才能跟读", null)
    }
    /** 按下话筒时调用；返回 false 表示还没有录音权限（已发起申请），这次按住不录。 */
    fun requestRecording(): Boolean {
        // 录音前停掉正在播放的朗读，免得录进去
        services.stopSpeaking()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permission.launch(Manifest.permission.RECORD_AUDIO)
            return false
        }
        startRecording()
        return true
    }

    ModalBottomSheet(onDismissRequest = { stopRequested = true; onDismiss() }, containerColor = PaperElevated) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText("跟读评分", color = InkFaint, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
            val result = (state as? ReadAlongState.Done)?.result
            // 句子：评分后按词上色
            val tokens = remember(line.en) { tokenizeSpeech(line.en) }
            val colored = buildAnnotatedString {
                append(line.en)
                result?.words?.takeIf { it.size == tokens.size }?.forEachIndexed { i, w ->
                    val color = when {
                        w.errorType == "Omission" -> InkFaint
                        w.score >= 80 -> Success
                        w.score >= 60 -> Category.Operations.tint
                        else -> Error
                    }
                    addStyle(
                        SpanStyle(color = color, textDecoration = if (w.errorType == "Omission") TextDecoration.LineThrough else null),
                        tokens[i].range.first,
                        tokens[i].range.last + 1
                    )
                }
            }
            Text(colored, fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, color = Ink)
            AppText(line.zh, color = InkSoft, fontSize = 17.sp, textAlign = TextAlign.Center)
            OutlinedButton(onClick = { services.speakEnglish(line.en) }) {
                Icon(Icons.Default.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("听原句")
            }

            when (val s = state) {
                is ReadAlongState.Done -> {
                    val pron = s.result.pron.roundToInt()
                    val (stars, cheer) = when {
                        pron >= 90 -> "★★★" to "太棒了！"
                        pron >= 75 -> "★★☆" to "读得不错！"
                        pron >= 60 -> "★☆☆" to "继续加油！"
                        else -> "☆☆☆" to "再试一次吧"
                    }
                    Text("$pron", fontSize = 56.sp, fontWeight = FontWeight.Bold, color = if (pron >= 75) Success else if (pron >= 60) Category.Operations.tint else Error)
                    AppText("$stars  $cheer", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    AppText(
                        "准确 ${s.result.accuracy.roundToInt()} · 流利 ${s.result.fluency.roundToInt()} · 完整 ${s.result.completeness.roundToInt()}",
                        color = InkFaint,
                        fontSize = 14.sp
                    )
                    if (s.result.words.isNotEmpty()) AppText("绿色读得好，橙色一般，红色要多练，灰色是漏读", color = InkFaint, fontSize = 12.sp)
                    OutlinedButton(onClick = { playback?.release(); playback = PronunciationScorer.play(s.pcm) }) { Text("听我的录音") }
                }
                is ReadAlongState.Failed -> {
                    AppText(s.message, color = Error, fontSize = 16.sp, textAlign = TextAlign.Center)
                    s.pcm?.let { pcm -> OutlinedButton(onClick = { playback?.release(); playback = PronunciationScorer.play(pcm) }) { Text("听我的录音") } }
                }
                ReadAlongState.Scoring -> AppText("评分中……", color = InkFaint, fontSize = 18.sp)
                ReadAlongState.Idle, ReadAlongState.Recording -> Unit
            }

            // 按住话筒录音、松开结束并评分；评完分或失败后再按住就是再读一遍
            val current = state
            if (current != ReadAlongState.Scoring) {
                val recording = current == ReadAlongState.Recording
                HoldToTalkMic(recording, level) {
                    if (requestRecording()) {
                        tryAwaitRelease()
                        stopRequested = true
                    }
                }
                AppText(
                    when {
                        recording -> "正在听……读完松开话筒"
                        current == ReadAlongState.Idle -> "按住话筒跟着读，读完松开"
                        else -> "按住话筒再读一遍"
                    },
                    color = InkSoft,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/** 跟读话筒：录音时向外扩散波纹，按钮随音量起伏；[onPress] 在按下时调用，挂起到松开。 */
@Composable
private fun HoldToTalkMic(recording: Boolean, level: Float, onPress: suspend PressGestureScope.() -> Unit) {
    val color by animateColorAsState(if (recording) Error else Category.Operations.tint, label = "micColor")
    val scale by animateFloatAsState(if (recording) 1.08f + level * 0.3f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "micScale")
    val ripple = rememberInfiniteTransition(label = "micRipple")
    val phase by ripple.animateFloat(0f, 1f, infiniteRepeatable(tween(1_400, easing = LinearEasing)), label = "micRipplePhase")
    // 外框固定大小，波纹和缩放都画在里面，弹窗高度不跟着跳
    Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
        if (recording) {
            Canvas(Modifier.matchParentSize()) {
                val base = 48.dp.toPx()
                repeat(3) { i ->
                    val t = (phase + i / 3f) % 1f
                    drawCircle(color.copy(alpha = 0.35f * (1f - t)), radius = base + (size.minDimension / 2 - base) * t)
                }
            }
        }
        Box(
            modifier = Modifier
                .size(96.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(color)
                .pointerInput(Unit) { detectTapGestures(onPress = { onPress() }) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Mic, contentDescription = "按住跟读", tint = Color.White, modifier = Modifier.size(44.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpeechLineRow(
    line: SpeechLine,
    tokens: List<SpeechToken>,
    savedRanges: List<IntRange>,
    selectedRange: IntRange?,
    showTranslation: Boolean,
    speaking: Boolean,
    onSpeakEnglish: () -> Unit,
    onSpeakChinese: () -> Unit,
    onTokenClick: (SpeechToken) -> Unit,
    repeating: Boolean = false,
    /** 长按右侧喇叭：反复朗读这一句 */
    onRepeat: (() -> Unit)? = null,
    /** 长按卡片空白处：跟读评分（长按在单词上仍是选词） */
    onReadAlong: (() -> Unit)? = null,
    /** 跟读最好成绩；跟读过的卡片换底色 */
    readAlongScore: Int? = null
) {
    val haptic = LocalHapticFeedback.current
    val text = buildAnnotatedString {
        append(line.en)
        // 缩写加下划线，提示可以长按看完整写法
        tokens.filter { contractionOf(it.text) != null }.forEach {
            addStyle(SpanStyle(textDecoration = TextDecoration.Underline), it.range.first, it.range.last + 1)
        }
        savedRanges.forEach {
            addStyle(SpanStyle(color = Category.Opposites.tint, fontWeight = FontWeight.SemiBold), it.first, it.last + 1)
        }
        selectedRange?.let { addStyle(SpanStyle(background = Category.Picturable.soft), it.first, it.last + 1) }
    }
    val accent = Category.Operations
    val background = when {
        speaking -> accent.soft
        readAlongScore == null -> PaperElevated
        readAlongScore >= 75 -> Color(0xFFE8F5E9)
        readAlongScore >= 60 -> Color(0xFFFFF3E0)
        else -> Color(0xFFFFEBEE)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = background),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .border(
                if (speaking) 2.dp else 1.dp,
                if (speaking) accent.tint else Line,
                RoundedCornerShape(14.dp)
            )
            .combinedClickable(
                onClick = onSpeakEnglish,
                onLongClick = onReadAlong?.let { readAlong ->
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        readAlong()
                    }
                }
            )
    ) {
        Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 点句子任意处朗读整句；长按进入选词，可查词 / 翻译
                TranslatableText(
                    text = text,
                    style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 36.sp, color = Ink),
                    onTap = onSpeakEnglish,
                    onWord = onTokenClick
                )
                AnimatedVisibility(showTranslation) {
                    AppText(
                        line.zh,
                        color = InkSoft,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        modifier = Modifier.clickable(onClick = onSpeakChinese)
                    )
                }
                if (repeating) AppText("🔁 重复播放中", color = accent.tint, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                readAlongScore?.let {
                    AppText(
                        "🎤 跟读最佳 $it",
                        color = if (it >= 75) Success else if (it >= 60) accent.tint else Error,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (onReadAlong != null && readAlongScore == null) {
                    AppText(
                        "长按跟读",
                        color = InkFaint.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            // 单击读一遍，长按反复朗读
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onSpeakEnglish,
                        onLongClick = onRepeat?.let { repeat ->
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                repeat()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (repeating) Icons.Default.Repeat else Icons.Default.VolumeUp,
                    contentDescription = "朗读，长按重复",
                    tint = Category.Operations.tint,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
fun SpeechWordSheet(
    surface: String,
    word: OgdenWord?,
    contraction: Contraction?,
    lookup: (String) -> OgdenWord?,
    accent: Accent,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSpeakChinese: (String) -> Unit,
    onOpenWord: (OgdenWord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 22.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (contraction != null) {
            val full = expandContraction(surface, contraction)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(surface, fontSize = 34.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSpeakEnglish(surface) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读缩写", tint = Category.Operations.tint)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("= $full", fontSize = 30.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, color = Category.Operations.tint, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSpeakEnglish(full) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读完整写法", tint = Category.Operations.tint)
                }
            }
            AppText(contraction.zh, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Card(colors = CardDefaults.cardColors(containerColor = Paper), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(start = 14.dp, top = 8.dp, bottom = 4.dp)) {
                    AppText("拆开看", color = InkFaint, fontSize = 13.sp)
                    contraction.parts.forEach { part ->
                        val partWord = lookup(part)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(partWord?.word ?: part, fontFamily = FontFamily.Serif, fontSize = 22.sp, modifier = Modifier.width(90.dp))
                            AppText(partWord?.zh.orEmpty(), color = InkSoft, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (partWord != null) {
                                TextButton(onClick = { onOpenWord(partWord) }) { AppText("查看") }
                            }
                        }
                    }
                }
            }
            AppText(contraction.note, color = InkFaint, fontSize = 14.sp)
        } else {
            // 内联布局里不能 return@Column 提前返回：旧版 Compose 会破坏分组栈并崩溃
            WordSheetBody(surface, word, accent, saved, onToggleSave, onSpeakWord, onSpeakEnglish, onSpeakChinese, onOpenWord)
        }
    }
}

@Composable
private fun WordSheetBody(
    surface: String,
    word: OgdenWord?,
    accent: Accent,
    saved: Boolean,
    onToggleSave: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakEnglish: (String) -> Unit,
    onSpeakChinese: (String) -> Unit,
    onOpenWord: (OgdenWord) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (word != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(word.word, fontSize = 34.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSpeakWord(word.word) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = word.category.tint)
                }
            }
            Text(if (accent == Accent.UK) word.ipaUk else word.ipaUs, color = InkFaint)
            if (!surface.equals(word.word, ignoreCase = true)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppText("原文：$surface", color = InkSoft, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onSpeakEnglish(surface) }) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "读原文", tint = InkFaint)
                    }
                }
            }
            AppText(word.zh, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            RegisterNote(word.word)
            word.examples.filter { it.first.isNotBlank() }.forEach { (example, exampleZh) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Paper),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TranslatableText(
                                AnnotatedString(example),
                                TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 28.sp, color = Ink),
                                modifier = Modifier.weight(1f),
                                onTap = { onSpeakEnglish(example) }
                            )
                            IconButton(onClick = { onSpeakEnglish(example) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = Category.Operations.tint)
                            }
                        }
                        if (exampleZh.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppText(exampleZh, color = InkSoft, fontSize = 17.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSpeakChinese(exampleZh) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "读例句中文", tint = InkFaint)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(surface, fontSize = 34.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSpeakEnglish(surface) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = InkSoft)
                }
            }
            AppText("不在词表中，收藏后可在「复习 · 课文生词」查看", color = InkFaint, fontSize = 13.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onToggleSave, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                Icon(
                    if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    tint = if (saved) Error else InkFaint
                )
                Spacer(Modifier.width(6.dp))
                AppText(if (saved) "已在课文生词" else "加入课文生词")
            }
            if (word != null) {
                Button(onClick = { onOpenWord(word) }, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f)) {
                    AppText("查看详情")
                }
            }
        }
    }
}

@Composable
fun SpeechWordListScreen(
    words: List<String>,
    lookup: (String) -> OgdenWord?,
    onBack: () -> Unit,
    onSpeak: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Scaffold(containerColor = Paper, topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            AppText("课文生词 · ${words.size}", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (words.isEmpty()) {
                item { EmptyCard("在课文里长按单词查释义，查过的词会自动收到这里。") }
            } else {
                items(words, key = { it }) { word ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(PaperElevated)
                            .border(1.dp, Line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val entry = lookup(word)
                        Column(Modifier.weight(1f)) {
                            Text(entry?.word ?: word, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 22.sp)
                            entry?.let { AppText(it.zh, color = InkSoft, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        IconButton(onClick = { onSpeak(word) }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = InkSoft)
                        }
                        IconButton(onClick = { onRemove(word) }) {
                            Icon(Icons.Default.Close, contentDescription = "取消收藏", tint = InkFaint)
                        }
                    }
                }
            }
        }
    }
}

data class LegalSection(val heading: String, val body: String)

@Composable
fun SettingsScreen(
    accent: Accent,
    chineseMode: ChineseMode,
    onAccent: (Accent) -> Unit,
    onChineseMode: (ChineseMode) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(containerColor = Paper, topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            AppText("软件设置", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionTitle("软件设置", "全局发音与文字显示")
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingsToggleRow(
                            accent = accent,
                            chineseMode = chineseMode,
                            onAccent = onAccent,
                            onChineseMode = onChineseMode
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var content by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sent by rememberSaveable { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Scaffold(containerColor = Paper, topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            AppText("意见反馈", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionTitle("意见反馈", "遇到问题或有建议，直接写给作者")
            }
            if (sent) {
                item {
                    AppText("已收到，谢谢你的反馈！", fontSize = 18.sp)
                }
                item {
                    OutlinedButton(onClick = onBack, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        AppText("返回")
                    }
                }
            } else {
                item {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it.take(Feedback.MAX_CONTENT_CHARS); error = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        placeholder = { AppText("请描述问题或建议，例如哪一课、哪个单词、发生了什么") },
                        supportingText = { AppText("${content.length} / ${Feedback.MAX_CONTENT_CHARS}", color = InkFaint, fontSize = 12.sp) },
                        enabled = !sending,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                item {
                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it.take(Feedback.MAX_CONTACT_CHARS) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { AppText("联系方式（选填，微信 / 邮箱）") },
                        singleLine = true,
                        enabled = !sending,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                item {
                    AppText("提交时会附带应用版本、手机型号和系统版本，便于定位问题。", color = InkFaint, fontSize = 12.sp)
                }
                error?.let { message ->
                    item { AppText(message, color = MaterialTheme.colorScheme.error, fontSize = 14.sp) }
                }
                item {
                    Button(
                        onClick = {
                            sending = true
                            error = null
                            scope.launch {
                                Feedback.send(content, contact)
                                    .onSuccess { sent = true }
                                    .onFailure { error = if (Feedback.isConfigured) "发送失败，请检查网络后重试" else "当前版本未开通反馈" }
                                sending = false
                            }
                        },
                        enabled = content.isNotBlank() && !sending,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        AppText(if (sending) "正在提交…" else "提交反馈")
                    }
                }
            }
        }
    }
}

@Composable
fun LegalInfoScreen(
    title: String,
    onBack: () -> Unit,
    sections: List<LegalSection>,
    links: List<Pair<String, String>>
) {
    val context = LocalContext.current
    Scaffold(containerColor = Paper, topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            AppText(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(sections) { section ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Line, RoundedCornerShape(14.dp))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppText(section.heading, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        AppText(section.body, color = InkSoft, lineHeight = 22.sp)
                    }
                }
            }
            if (links.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PaperElevated),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Line, RoundedCornerShape(14.dp))
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppText("相关链接", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                            links.forEach { (label, url) ->
                                OutlinedButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    AppText(label)
                                }
                                Text(url, color = InkFaint, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun privacySections() = listOf(
    LegalSection(
        "基本说明",
        "Panda English 是一款面向英语单词学习的应用。本应用尊重并保护用户隐私，不会收集、上传、出售或共享任何用户个人信息。"
    ),
    LegalSection(
        "可能使用的权限",
        "网络访问权限：用于在例句、长文本或本地音频不可用时访问在线发音服务作为备用。音频播放能力：用于播放单词和例句发音。应用不会录音，不会访问麦克风。本地存储能力：用于在设备本地保存学习进度、收藏、错词和熟练度。"
    ),
    LegalSection(
        "不会进行的行为",
        "本应用不会收集姓名、手机号、邮箱、账号、定位等个人身份信息；不会读取通讯录、短信、相册、摄像头、麦克风等敏感权限；不会追踪用户用于广告或商业分析；不会向第三方共享学习记录。"
    ),
    LegalSection(
        "适用范围",
        "本应用适合希望学习 Panda English 850 词及拓展词的用户使用，不要求提供个人信息，也不会主动收集身份、位置、联系方式或其他敏感数据。"
    ),
    LegalSection(
        "本地数据与删除",
        "学习记录、收藏、错词和熟练度均保存在本机。用户可以通过系统设置清除应用数据，或卸载应用来删除全部本地数据。"
    ),
    LegalSection(
        "发音服务",
        "应用内置了单词（US / UK）、课文、例句、练习题与中文释义的全部朗读音频，离线即可播放。仅当个别音频缺失时，才会请求 Microsoft Azure 语音服务现场合成作为备用，请求仅包含需要朗读的英文或中文文本，不包含用户身份信息，合成的音频缓存在本机；网络不可用时使用系统自带的语音引擎。"
    ),
    LegalSection(
        "意见反馈",
        "仅在你主动提交「意见反馈」时，应用会把你填写的内容、选填的联系方式，以及应用版本、手机型号和系统版本发送给开发者（经企业微信），用于处理反馈，不做其他用途。"
    ),
    LegalSection(
        "跟读录音",
        "使用「跟读评分」时，应用会请求麦克风权限并录下这一次跟读，录音连同参考句子发送到 Microsoft Azure 语音服务进行发音评估，评分完成后不在本机保存，也不包含用户身份信息。"
    )
)

@Composable
fun PracticeScreen(
    allWords: List<OgdenWord>,
    lessonWords: Set<String>,
    store: ProgressStore,
    version: Int,
    category: Category,
    level: Int,
    reviewOnly: Boolean,
    zh: ChineseMode,
    onSpeak: (String) -> Unit,
    onSpeakChinese: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onRecord: (OgdenWord, Boolean) -> Unit,
    customWords: List<OgdenWord>? = null,
    customTitle: String? = null,
    onNextLevel: (() -> Unit)? = null
) {
    // 不随 version 重算：错词复习答对一题就会移出错词本，重算会让题目在答题中途变掉
    val source = remember(category, level, reviewOnly, customWords) {
        val reviewWords = store.mistakeWords(allWords)
        if (customWords != null) customWords
        else if (reviewOnly && reviewWords.isNotEmpty()) reviewWords.take(10)
        else allWords.filter { it.category == category }.drop((level - 1) * 10).take(10)
    }
    val services = LocalSpeechServices.current
    // 用 rememberSaveable：答完点「查词典」会跳到单词详情，返回时要回到原题
    var index by rememberSaveable(source) { mutableStateOf(0) }
    var selected by rememberSaveable(source) { mutableStateOf<String?>(null) }
    var answerShown by rememberSaveable(source) { mutableStateOf(false) }
    var correctCount by rememberSaveable(source) { mutableStateOf(0) }
    val word = source.getOrNull(index)
    fun goNext() {
        if (index >= source.lastIndex) {
            onComplete()
            onBack()
        } else {
            index++
            selected = null
            answerShown = false
        }
    }

    Scaffold(containerColor = Paper, bottomBar = {
        if (answerShown && word != null) {
            if (index >= source.lastIndex && onNextLevel != null) {
                FinishLevelBar(
                    onBack = { onComplete(); onBack() },
                    onNext = { onComplete(); onNextLevel() }
                )
            } else {
                NextQuestionBar(if (index >= source.lastIndex) "完成并返回" else "下一题", ::goNext)
            }
        }
    }, topBar = {
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
                AppText(customTitle ?: if (reviewOnly) "错词复习" else "${category.zh} · 第 $level 关", fontWeight = FontWeight.Bold)
                AppText("${index.coerceAtMost(source.size)} / ${source.size} · 答对 $correctCount", color = InkFaint, fontSize = 12.sp)
            }
        }
    }) { padding ->
        if (word == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyCard("这一关没有词了。")
            }
            return@Scaffold
        }
        val type = PracticeType.values()[index % PracticeType.values().size]
        val question = remember(word, type) { buildQuestion(type, word, allWords, lessonWords) }
        // 听音选词一进题就播放
        LaunchedEffect(word, type) { if (type == PracticeType.Listen && !answerShown) onSpeak(word.word) }
        val isCorrect = selected == question.answer

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LinearProgressIndicator(
                    progress = (index + if (answerShown) 1 else 0) / source.size.toFloat(),
                    color = category.tint,
                    trackColor = category.soft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        if (type == PracticeType.Listen) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                CategoryBadge(word.category)
                                AppText(type.title, color = word.category.tint, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Button(
                                    onClick = { onSpeak(word.word) },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "播放", modifier = Modifier.size(30.dp))
                                }
                            }
                        } else {
                            CategoryBadge(word.category)
                            AppText(question.title ?: type.title, color = word.category.tint, fontWeight = FontWeight.Bold)
                            Text(convertZh(question.prompt, zh), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp)
                        }
                        if (type == PracticeType.Spelling) {
                            AppText("拼出这个单词", color = InkFaint)
                        }
                    }
                }
            }
            items(question.options) { option ->
                val selectedThis = selected == option
                val correctThis = answerShown && option == question.answer
                val wrongThis = answerShown && selectedThis && option != question.answer
                OutlinedButton(
                    onClick = {
                        if (!answerShown) {
                            selected = option
                            val ok = option == question.answer
                            if (ok) {
                                correctCount++
                                // 答对立刻读出正确答案（近义词题的答案是近义词，不是本词）
                                onSpeak(question.answer)
                            }
                            onRecord(word, ok)
                            answerShown = true
                        } else if (option == question.answer) {
                            // 答完后（尤其答错时）点正确选项可以再听一遍
                            onSpeak(question.answer)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = when {
                            correctThis -> Color(0xFFDCFCE7)
                            wrongThis -> Color(0xFFFEE2E2)
                            selectedThis -> word.category.soft
                            else -> PaperElevated
                        }
                    ),
                    border = BorderStroke(1.dp, Line)
                ) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(option, modifier = Modifier.weight(1f), fontSize = 18.sp)
                        if (correctThis) Icon(Icons.Default.Check, contentDescription = null, tint = Success)
                        if (wrongThis) Icon(Icons.Default.Close, contentDescription = null, tint = Error)
                        // 答题前不给查词，免得直接看到答案
                        if (answerShown) services.lookup(option)?.let { entry ->
                            IconButton(onClick = { services.openWord(entry) }) {
                                Icon(Icons.Default.Book, contentDescription = "查词典", tint = InkFaint)
                            }
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(answerShown) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppText(if (isCorrect) "答对了！" else "这题先记到错词本", fontWeight = FontWeight.Bold, color = if (isCorrect) Success else Error)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("${word.word} · ${convertZh(word.zh, zh)}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { services.openWord(word) }) {
                                    Icon(Icons.Default.Book, contentDescription = "查词典", tint = Category.Operations.tint)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TranslatableText(
                                    AnnotatedString(word.example),
                                    TextStyle(fontFamily = FontFamily.Serif, fontSize = 19.sp, lineHeight = 26.sp, color = Ink),
                                    modifier = Modifier.weight(1f),
                                    onTap = { onSpeak(word.example) }
                                )
                                IconButton(onClick = { onSpeak(word.example) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = Category.Operations.tint)
                                }
                            }
                            if (word.exampleZh.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AppText(convertZh(word.exampleZh, zh), color = InkSoft, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { onSpeakChinese(word.exampleZh) }) {
                                        Icon(Icons.Default.VolumeUp, contentDescription = "读例句中文", tint = InkFaint)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePracticeScreen(
    title: String,
    subtitle: String,
    units: List<Speech>,
    vocabulary: List<OgdenWord>,
    bestScore: Int?,
    onSpeak: (String) -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakChinese: (String) -> Unit,
    onFinish: (correct: Int, total: Int) -> Unit,
    onBack: () -> Unit
) {
    // 每次进入或「再练一次」换一组题
    var session by remember { mutableStateOf(0) }
    val questions = remember(session) { buildThemePractice(units, Random(System.nanoTime()), vocabulary = vocabulary) }
    val isWordQuestion = { q: SentenceQuestion -> q.type == SentenceQuestionType.WordListen || q.type == SentenceQuestionType.WordMeaning }
    val speakQuestion = { q: SentenceQuestion ->
        when {
            isWordQuestion(q) -> onSpeakWord(q.sentence)
            q.type == SentenceQuestionType.Contraction -> onSpeak(q.answer)
            else -> onSpeak(q.sentence)
        }
    }
    var index by remember(session) { mutableStateOf(0) }
    var selected by remember(session, index) { mutableStateOf<String?>(null) }
    // 连词成句：已点选的词块下标（按点选顺序）
    var picked by remember(session, index) { mutableStateOf(emptyList<Int>()) }
    var answered by remember(session, index) { mutableStateOf(false) }
    var correctCount by remember(session) { mutableStateOf(0) }
    var finished by remember(session) { mutableStateOf(false) }
    val question = questions.getOrNull(index)
    val accent = Category.Operations

    fun goNext() {
        if (index >= questions.lastIndex) {
            onFinish(correctCount, questions.size)
            finished = true
        } else {
            index++
        }
    }

    fun submit(ok: Boolean) {
        answered = true
        if (ok) {
            correctCount++
            question?.let(speakQuestion)
        }
    }

    LaunchedEffect(session, index) {
        when (question?.type) {
            SentenceQuestionType.Listen -> onSpeak(question.prompt)
            SentenceQuestionType.WordListen -> onSpeakWord(question.prompt)
            else -> Unit
        }
    }

    Scaffold(containerColor = Paper, bottomBar = {
        if (answered && !finished && question != null) NextQuestionBar(if (index >= questions.lastIndex) "看成绩" else "下一题", ::goNext)
    }, topBar = {
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
                AppText(
                    "$subtitle · ${(index + 1).coerceAtMost(questions.size)} / ${questions.size} · 答对 $correctCount",
                    color = InkFaint,
                    fontSize = 12.sp
                )
            }
        }
    }) { padding ->
        if (finished || question == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText("答对 $correctCount / ${questions.size}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                AppText(
                    when {
                        questions.isEmpty() -> "这个主题暂时出不了题"
                        correctCount == questions.size -> "全对！太棒了！"
                        correctCount * 10 >= questions.size * 7 -> "很不错，再练一次争取全对"
                        else -> "回到课文多听几遍，再来挑战"
                    },
                    color = InkSoft,
                    fontSize = 18.sp
                )
                bestScore?.let { AppText("最好成绩 $it / ${questions.size}", color = InkFaint) }
                Button(onClick = { session++ }, modifier = Modifier.fillMaxWidth()) { Text("再练一次", fontSize = 18.sp) }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回", fontSize = 18.sp) }
            }
            return@Scaffold
        }
        val isCorrect = when (question.type) {
            SentenceQuestionType.Order -> picked.map { question.options[it] }.joinToString(" ") == question.answer
            else -> selected == question.answer
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LinearProgressIndicator(
                    progress = (index + if (answered) 1 else 0) / questions.size.toFloat(),
                    color = accent.tint,
                    trackColor = accent.soft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        AppText("${question.type.title} · ${question.type.titleEn}", color = accent.tint, fontWeight = FontWeight.Bold)
                        when (question.type) {
                            SentenceQuestionType.Listen, SentenceQuestionType.WordListen -> Button(
                                onClick = { speakQuestion(question) },
                                shape = CircleShape,
                                modifier = Modifier.size(96.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "再听一遍", modifier = Modifier.size(44.dp))
                            }
                            SentenceQuestionType.FillWord, SentenceQuestionType.Pattern, SentenceQuestionType.Contraction -> {
                                Text(question.prompt, fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 36.sp)
                                AppText(question.hint, color = InkSoft, fontSize = 18.sp)
                            }
                            else -> Text(question.prompt, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
                        }
                    }
                }
            }
            if (question.type == SentenceQuestionType.Order) {
                item {
                    // 已拼好的部分；点其中的词可以退回
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (answered) (if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2)) else accent.soft),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FlowRow(
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 6.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (picked.isEmpty()) AppText("按顺序点下面的词", color = InkFaint, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                            picked.forEach { chunk ->
                                OutlinedButton(
                                    onClick = { if (!answered) picked = picked - chunk },
                                    modifier = Modifier.padding(bottom = 8.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                ) { Text(question.options[chunk], fontSize = 20.sp) }
                            }
                        }
                    }
                }
                item {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        question.options.indices.forEach { chunk ->
                            // 已选的词块原位留同尺寸空位，其余词块不挪位置，孩子不用重新找
                            val used = chunk in picked
                            Button(
                                onClick = {
                                    if (answered || used) return@Button
                                    picked = picked + chunk
                                    if (picked.size == question.options.size) {
                                        submit(picked.map { question.options[it] }.joinToString(" ") == question.answer)
                                    }
                                },
                                enabled = !used,
                                colors = ButtonDefaults.buttonColors(disabledContainerColor = Line),
                                modifier = Modifier.padding(bottom = 8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                            ) { Text(question.options[chunk], fontSize = 20.sp, color = if (used) Color.Transparent else Color.Unspecified) }
                        }
                    }
                }
            } else {
                items(question.options) { option ->
                    val selectedThis = selected == option
                    val correctThis = answered && option == question.answer
                    val wrongThis = answered && selectedThis && option != question.answer
                    OutlinedButton(
                        onClick = {
                            if (!answered) {
                                selected = option
                                submit(option == question.answer)
                            } else if (option == question.answer) {
                                speakQuestion(question)
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
                        border = BorderStroke(1.dp, Line),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(option, modifier = Modifier.weight(1f), fontSize = 20.sp, lineHeight = 26.sp, color = Ink)
                            if (correctThis) Icon(Icons.Default.Check, contentDescription = null, tint = Success)
                            if (wrongThis) Icon(Icons.Default.Close, contentDescription = null, tint = Error)
                        }
                    }
                }
            }
            item {
                AnimatedVisibility(answered) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppText(if (isCorrect) "答对了！" else "再看看正确答案", fontWeight = FontWeight.Bold, color = if (isCorrect) Success else Error)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TranslatableText(
                                    AnnotatedString(question.sentence),
                                    TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 30.sp, color = Ink),
                                    modifier = Modifier.weight(1f),
                                    onTap = { speakQuestion(question) }
                                )
                                IconButton(onClick = { speakQuestion(question) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = accent.tint)
                                }
                            }
                            // 答对答错都给出中文，听力题也能确认自己听到的意思
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppText(question.zh, color = InkSoft, fontSize = 18.sp, lineHeight = 26.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSpeakChinese(question.zh) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读中文", tint = InkFaint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SpecialModuleScreen(
    module: SpecialModule,
    level: Int,
    store: ProgressStore,
    wordIndex: Map<String, OgdenWord>,
    lemmaVocabulary: LemmaVocabulary,
    accent: Accent,
    onBack: () -> Unit,
    onScene: (SpecialTopic, Int) -> Unit,
    onWordPractice: (List<String>) -> Unit,
    onSentencePractice: () -> Unit,
    onSpeakWord: (String) -> Unit,
    onSpeakEnglish: (text: String, onDone: (() -> Unit)?) -> Unit,
    onSpeakChinese: (text: String, onDone: (() -> Unit)?) -> Unit,
    onStopSpeaking: () -> Unit,
    onOpenWord: (OgdenWord) -> Unit
) {
    var selected by remember(module.key) { mutableStateOf<SelectedSpeechWord?>(null) }
    var pickedWord by rememberSaveable(module.key) { mutableStateOf<String?>(null) }
    var speakingIndex by remember(module.key) { mutableStateOf<Int?>(null) }
    var repeatIndex by remember(module.key) { mutableStateOf<Int?>(null) }
    val repeatScope = rememberCoroutineScope()
    var readAlong by remember(module.key) { mutableStateOf<SpeechLine?>(null) }
    var readAlongVersion by remember { mutableStateOf(0) }
    var sceneLevel by rememberSaveable(module.key) { mutableStateOf(level) }
    var favoriteVersion by remember { mutableStateOf(0) }
    fun ogdenWordOf(token: SpeechToken) = lemmatize(token.text, lemmaVocabulary)?.let { wordIndex[it] }
    /** 收藏夹里的词或「课文生词」里的词都算已收藏，用紫色标出 */
    fun isMarked(token: SpeechToken): Boolean {
        val word = ogdenWordOf(token)
        return (word != null && store.progress(word.word).favorite) || store.isSpeechWordSaved(lessonWordKey(token, word))
    }
    fun stopSpeaking() {
        onStopSpeaking()
        repeatIndex = null
        speakingIndex = null
    }

    fun repeatLoop(index: Int) {
        speakingIndex = index
        onSpeakEnglish(module.sentences[index].en) {
            if (repeatIndex == index) repeatScope.launch {
                delay(700)
                if (repeatIndex == index) repeatLoop(index)
            }
        }
    }

    readAlong?.let { line ->
        ReadAlongSheet(line, onScored = { store.saveReadAlongScore(line.en, it); readAlongVersion++ }) { readAlong = null }
    }

    LaunchedEffect(selected) {
        val current = selected ?: return@LaunchedEffect
        val token = current.token
        stopSpeaking()
        val word = ogdenWordOf(token)
        if (word != null && contractionOf(token.text) == null) onSpeakWord(word.word) else onSpeakEnglish(token.text, null)
        // 在句子里长按查过的词自动记入「课文生词」；点本课单词（line = -1）和缩写不记
        if (current.line >= 0 && contractionOf(token.text) == null) {
            store.addSpeechWord(lessonWordKey(token, word))
            favoriteVersion++
        }
    }

    Scaffold(containerColor = Paper, floatingActionButtonPosition = FabPosition.Center, floatingActionButton = {
        if (repeatIndex != null) {
            ExtendedFloatingActionButton(
                onClick = { stopSpeaking() },
                icon = { Icon(Icons.Default.Close, contentDescription = null) },
                text = { Text("停止重复", fontSize = 18.sp) },
                containerColor = Category.Operations.tint,
                contentColor = Color.White
            )
        }
    }, topBar = {
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
                Text("${module.icon} ${module.zh}", fontWeight = FontWeight.Bold)
                AppText("${module.en} · ${module.words.size} 个词 · ${module.sentences.size} 个句子", color = InkFaint, fontSize = 12.sp)
            }
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            module.scene?.let { topic ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Category.Operations.soft),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AppText("看图练习", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SpeechLevelNames.filterKeys { it != StemLevel }.forEach { (value, name) ->
                                    val best = store.bestSpecialScore(topic.key, value)
                                    FilterChip(
                                        selected = sceneLevel == value,
                                        onClick = { sceneLevel = value },
                                        label = { Text(if (best != null) "$name · $best" else name, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Button(onClick = { stopSpeaking(); onScene(topic, sceneLevel) }, modifier = Modifier.fillMaxWidth()) {
                                Text("开始${SpeechLevelNames[sceneLevel]}看图练习 · 共 ${specialPracticeSize(topic, sceneLevel)} 题", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { stopSpeaking(); onWordPractice(module.words) }, modifier = Modifier.weight(1f)) {
                        Text("单词练习", fontSize = 18.sp)
                    }
                    Button(onClick = { stopSpeaking(); onSentencePractice() }, modifier = Modifier.weight(1f)) {
                        Text("句子练习", fontSize = 18.sp)
                    }
                }
            }
            module.groups.forEach { group ->
                item(key = "group-${group.zh}") {
                    Column(Modifier.padding(top = 8.dp)) {
                        AppText(group.zh, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                        AppText("${group.en} · 点单词听发音、看释义", color = InkFaint, fontSize = 12.sp)
                    }
                }
                item(key = "words-${group.zh}") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        group.words.forEach { w ->
                            val picked = pickedWord == w
                            val marked = remember(w, favoriteVersion) { isMarked(SpeechToken(w, w.indices)) }
                            OutlinedButton(
                                onClick = {
                                    pickedWord = w
                                    selected = SelectedSpeechWord(-1, SpeechToken(w, w.indices))
                                },
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (picked) Category.Operations.soft else Color.Transparent),
                                border = BorderStroke(if (picked) 2.dp else 1.dp, if (picked) Category.Operations.tint else Line),
                                modifier = Modifier.padding(bottom = 8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                if (marked) {
                                    Icon(Icons.Default.Favorite, contentDescription = "已收藏", tint = Error, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                }
                                Text(
                                    wordIndex[w.lowercase()]?.word ?: w,
                                    fontSize = 20.sp,
                                    fontFamily = FontFamily.Serif,
                                    fontWeight = if (marked) FontWeight.Bold else null,
                                    color = if (marked) Category.Opposites.tint else Ink
                                )
                                registerOf(w)?.let { Spacer(Modifier.width(6.dp)); RegisterBadge(it.tag) }
                            }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
                SpeechTrackHeader(title = "组合句子", subtitle = "点句子听朗读，长按喇叭重复，长按单词选词，长按空白处跟读评分", playingAll = false, onPlayAll = null)
            }
            items(module.sentences.size) { index ->
                val line = module.sentences[index]
                val tokens = remember(line.en) { tokenizeSpeech(line.en) }
                SpeechLineRow(
                    line = line,
                    tokens = tokens,
                    savedRanges = remember(line.en, favoriteVersion) { tokens.filter { isMarked(it) }.map { it.range } },
                    selectedRange = selected?.takeIf { it.line == index }?.token?.range,
                    showTranslation = true,
                    speaking = speakingIndex == index,
                    onSpeakEnglish = {
                        repeatIndex = null
                        speakingIndex = index
                        onSpeakEnglish(line.en) { if (speakingIndex == index) speakingIndex = null }
                    },
                    onSpeakChinese = {
                        repeatIndex = null
                        speakingIndex = index
                        onSpeakChinese(line.zh) { if (speakingIndex == index) speakingIndex = null }
                    },
                    onTokenClick = { selected = SelectedSpeechWord(index, it) },
                    repeating = repeatIndex == index,
                    onRepeat = {
                        stopSpeaking()
                        repeatIndex = index
                        repeatLoop(index)
                    },
                    onReadAlong = { stopSpeaking(); readAlong = line },
                    readAlongScore = remember(line.en, readAlongVersion) { store.readAlongScore(line.en) }
                )
            }
        }
    }

    selected?.let { current ->
        val word = ogdenWordOf(current.token)
        val saved = remember(current, favoriteVersion) {
            store.isSpeechWordSaved(lessonWordKey(current.token, word))
        }
        ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = PaperElevated) {
            SpeechWordSheet(
                surface = current.token.text,
                word = word,
                contraction = contractionOf(current.token.text),
                lookup = { wordIndex[it.lowercase()] },
                accent = accent,
                saved = saved,
                onToggleSave = {
                    store.toggleSpeechWord(lessonWordKey(current.token, word))
                    favoriteVersion++
                },
                onSpeakWord = { stopSpeaking(); onSpeakWord(it) },
                onSpeakEnglish = { stopSpeaking(); onSpeakEnglish(it, null) },
                onSpeakChinese = { stopSpeaking(); onSpeakChinese(it, null) },
                onOpenWord = { selected = null; onOpenWord(it) }
            )
        }
    }
}

// ---------- 专项训练：场景绘制 ----------

private const val SceneWidth = 240f
private const val SceneHeight = 190f

/** 物体中心点（基准画布 240×190 dp 内）、大小，是否画在参照物之前（被挡住），以及透明度（后面的调淡显出纵深）。 */
private data class Placement(val x: Float, val y: Float, val size: Float, val underRef: Boolean = false, val alpha: Float = 1f)

private fun placementOf(relation: Relation): Placement = when (relation) {
    Relation.On -> Placement(120f, 46f, 40f)
    Relation.Above -> Placement(120f, 22f, 34f)
    Relation.Under -> Placement(120f, 152f, 40f)
    Relation.Below -> Placement(120f, 174f, 30f)
    Relation.In -> Placement(120f, 78f, 34f, underRef = true)
    // 「后面 / 前面」放在三维辅助线的前后轴上：后面的小一些、淡一些，被参照物挡住一部分
    Relation.Behind -> Placement(156f, 72f, 34f, underRef = true, alpha = 0.75f)
    Relation.InFrontOf -> Placement(82f, 129f, 46f)
    Relation.NextTo, Relation.Beside, Relation.RightOf -> Placement(188f, 100f, 40f)
    Relation.Near -> Placement(218f, 100f, 34f)
    Relation.LeftOf -> Placement(52f, 100f, 40f)
    Relation.Between -> Placement(120f, 100f, 38f)
    Relation.Inside -> Placement(120f, 110f, 26f)
    Relation.Outside -> Placement(208f, 150f, 36f)
}

@Composable
private fun EmojiAt(emoji: String, x: Float, y: Float, size: Float, scale: Float, alpha: Float = 1f) {
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset(((x - size / 2) * scale).dp, ((y - size / 2) * scale).dp)
            .size((size * scale).dp)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        // 用 dp 换算字号，不受系统字体缩放影响，否则表情会溢出场景
        Text(emoji, fontSize = with(density) { (size * scale * 0.8f).dp.toSp() })
    }
}

// 参照物外框立方体（基准画布坐标）：正面框住参照物，背面向右上偏移表现纵深
private const val CubeLeft = 85f
private const val CubeTop = 68f
private const val CubeRight = 155f
private const val CubeBottom = 132f
private const val CubeDepthX = 18f
private const val CubeDepthY = -14f
// 三条轴都从参照物中心出发，物体沿对应的轴摆放
private const val OriginX = 120f
private const val OriginY = 100f

private enum class AxisEnd { Up, Down, Left, Right, Back, Front }

private fun axisEndOf(relation: Relation): AxisEnd? = when (relation) {
    Relation.On, Relation.Above -> AxisEnd.Up
    Relation.Under, Relation.Below -> AxisEnd.Down
    Relation.NextTo, Relation.Beside, Relation.RightOf, Relation.Near -> AxisEnd.Right
    Relation.LeftOf -> AxisEnd.Left
    Relation.Behind -> AxisEnd.Back
    Relation.InFrontOf -> AxisEnd.Front
    else -> null
}

/**
 * 三维辅助线：线框立方体 + 地面阴影 + 上下 / 左右 / 前后三条轴（轴端标中文）。
 * 物体所在的那一面填浅色，所在方向的半条轴画成箭头、轴端文字加粗变色；全部画在表情底下，
 * 物体像停在轴上。[relation] 为 null（「点一点放在哪」）时只画立方体和轴，不提示答案。
 * [labels] 为 false 时（缩略图）不标字。
 */
private fun DrawScope.drawSpatialGuides(relation: Relation?, labels: Boolean) {
    val u = size.width / SceneWidth
    fun pt(x: Float, y: Float) = Offset(x * u, y * u)
    val guide = Color(0xFF94A3B8)
    val axis = Color(0xFFCBD5E1)
    val tint = Category.Qualities.tint
    val dash = PathEffect.dashPathEffect(floatArrayOf(6f * u, 5f * u))
    val l = CubeLeft; val t = CubeTop; val r = CubeRight; val b = CubeBottom
    val dx = CubeDepthX; val dy = CubeDepthY
    val ends = mapOf(
        AxisEnd.Up to Offset(OriginX, 10f),
        AxisEnd.Down to Offset(OriginX, 182f),
        AxisEnd.Left to Offset(12f, OriginY),
        AxisEnd.Right to Offset(228f, OriginY),
        AxisEnd.Back to Offset(OriginX + dx * 3.4f, OriginY + dy * 3.4f),
        AxisEnd.Front to Offset(OriginX - dx * 4.4f, OriginY - dy * 4.4f)
    )
    fun face(p: List<Offset>, alpha: Float = 0.16f) {
        drawPath(Path().apply { moveTo(p[0].x, p[0].y); p.drop(1).forEach { lineTo(it.x, it.y) }; close() }, tint.copy(alpha = alpha))
    }
    val frontFace = listOf(pt(l, t), pt(r, t), pt(r, b), pt(l, b))
    val backFace = listOf(pt(l + dx, t + dy), pt(r + dx, t + dy), pt(r + dx, b + dy), pt(l + dx, b + dy))
    val topFace = listOf(pt(l, t), pt(r, t), pt(r + dx, t + dy), pt(l + dx, t + dy))
    val bottomFace = listOf(pt(l, b), pt(r, b), pt(r + dx, b + dy), pt(l + dx, b + dy))
    val rightFace = listOf(pt(r, t), pt(r + dx, t + dy), pt(r + dx, b + dy), pt(r, b))

    drawOval(Color(0x18000000), topLeft = pt(l - 8, b - 10), size = Size((r - l + dx + 16) * u, 22 * u))
    when (relation) {
        Relation.On -> face(topFace, 0.22f)
        Relation.Under -> face(bottomFace, 0.22f)
        Relation.Behind -> face(backFace)
        Relation.InFrontOf -> face(frontFace)
        Relation.NextTo, Relation.Beside, Relation.RightOf -> face(rightFace, 0.22f)
        Relation.In, Relation.Inside -> { face(backFace, 0.10f); face(topFace, 0.10f); face(rightFace, 0.10f); face(frontFace, 0.14f) }
        else -> Unit
    }
    val origin = pt(OriginX, OriginY)
    listOf(AxisEnd.Up to AxisEnd.Down, AxisEnd.Left to AxisEnd.Right, AxisEnd.Front to AxisEnd.Back).forEach { (a, c) ->
        drawLine(axis, ends.getValue(a) * u, ends.getValue(c) * u, 1.5f * u, pathEffect = dash)
    }
    listOf(
        pt(l + dx, t + dy) to pt(l + dx, b + dy),
        pt(l + dx, b + dy) to pt(r + dx, b + dy),
        pt(l, b) to pt(l + dx, b + dy)
    ).forEach { (a, c) -> drawLine(guide, a, c, 1.5f * u, pathEffect = dash) }
    listOf(
        pt(l, t) to pt(r, t), pt(r, t) to pt(r, b), pt(r, b) to pt(l, b), pt(l, b) to pt(l, t),
        pt(l, t) to pt(l + dx, t + dy), pt(r, t) to pt(r + dx, t + dy), pt(l + dx, t + dy) to pt(r + dx, t + dy),
        pt(r, b) to pt(r + dx, b + dy), pt(r + dx, t + dy) to pt(r + dx, b + dy)
    ).forEach { (a, c) -> drawLine(guide, a, c, 2f * u) }

    fun arrow(from: Offset, to: Offset) {
        val dir = to - from
        val unit = dir / dir.getDistance()
        val normal = Offset(-unit.y, unit.x)
        drawLine(tint, from, to, 3f * u, cap = StrokeCap.Round)
        drawPath(Path().apply {
            moveTo(to.x + unit.x * 9f * u, to.y + unit.y * 9f * u)
            lineTo(to.x + normal.x * 7f * u, to.y + normal.y * 7f * u)
            lineTo(to.x - normal.x * 7f * u, to.y - normal.y * 7f * u)
            close()
        }, tint)
    }
    val activeEnd = relation?.let(::axisEndOf)
    if (activeEnd != null) {
        // 离得远的（上方、下方、附近）箭头只画到物体边上，指着物体；其余穿到轴端，物体像停在轴上
        val target = if (relation == Relation.Above || relation == Relation.Below || relation == Relation.Near) {
            val p = placementOf(relation)
            val end = ends.getValue(activeEnd)
            val dir = Offset(end.x - OriginX, end.y - OriginY)
            val unit = dir / dir.getDistance()
            Offset(p.x, p.y) - unit * (p.size / 2 + 8f)
        } else {
            ends.getValue(activeEnd)
        }
        arrow(origin, target * u)
    }
    if (relation == Relation.Outside) placementOf(relation).let { arrow(origin, pt(it.x - 18f, it.y - 12f)) }
    // 「上方 / 下方」不挨着：在参照物和物体之间画一段带端点的虚线标出空隙
    if (relation == Relation.Above || relation == Relation.Below) {
        val p = placementOf(relation)
        val (from, to) = if (relation == Relation.Above) (t + dy / 2) to (p.y + p.size / 2) else b to (p.y - p.size / 2)
        val x = OriginX + 26f
        drawLine(tint, pt(x, from), pt(x, to), 2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f * u, 3f * u)))
        drawLine(tint, pt(x - 5f, from), pt(x + 5f, from), 2f * u)
        drawLine(tint, pt(x - 5f, to), pt(x + 5f, to), 2f * u)
    }
    if (labels) {
        drawIntoCanvas { canvas ->
            fun label(text: String, at: Offset, active: Boolean) {
                val paint = android.graphics.Paint().apply {
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    textSize = (if (active) 14f else 11f) * u
                    isFakeBoldText = active
                    color = if (active) android.graphics.Color.rgb(30, 64, 175) else android.graphics.Color.rgb(148, 163, 184)
                }
                canvas.nativeCanvas.drawText(text, at.x * u, at.y * u + paint.textSize / 3, paint)
            }
            fun end(e: AxisEnd) = ends.getValue(e)
            // 「上 / 下」放在轴右边、「右」放在轴上方，避开沿轴摆放的物体
            label("上", end(AxisEnd.Up) + Offset(24f, 4f), activeEnd == AxisEnd.Up)
            label("下", end(AxisEnd.Down) + Offset(24f, -2f), activeEnd == AxisEnd.Down)
            label("左", end(AxisEnd.Left) + Offset(6f, -12f), activeEnd == AxisEnd.Left)
            label("右", end(AxisEnd.Right) + Offset(-4f, -26f), activeEnd == AxisEnd.Right)
            label("后", end(AxisEnd.Back) + Offset(14f, -4f), activeEnd == AxisEnd.Back)
            label("前", end(AxisEnd.Front) + Offset(-10f, 12f), activeEnd == AxisEnd.Front)
            if (relation == Relation.In || relation == Relation.Inside) label("里", Offset(OriginX + 52f, OriginY + 26f), true)
            if (relation == Relation.Outside) label("外", Offset(222f, 168f), true)
        }
    }
}

/** 「在中间」：两个参照物之间画一条连线，物体停在中点。 */
private fun DrawScope.drawBetweenGuides() {
    val u = size.width / SceneWidth
    fun pt(x: Float, y: Float) = Offset(x * u, y * u)
    val tint = Category.Qualities.tint
    drawOval(Color(0x18000000), topLeft = pt(22f, 118f), size = Size(56f * u, 16f * u))
    drawOval(Color(0x18000000), topLeft = pt(162f, 118f), size = Size(56f * u, 16f * u))
    drawLine(tint, pt(50f, 100f), pt(190f, 100f), 3f * u, cap = StrokeCap.Round)
    listOf(84f, 156f).forEach { x -> drawLine(tint, pt(x, 92f), pt(x, 108f), 2f * u) }
}

@Composable
private fun PlaceSceneView(scene: Scene.Place, scale: Float, showItem: Boolean = true) {
    Box(Modifier.size((SceneWidth * scale).dp, (SceneHeight * scale).dp)) {
        Canvas(Modifier.matchParentSize()) {
            if (scene.ref2 != null) drawBetweenGuides() else drawSpatialGuides(scene.relation, labels = scale >= 1f)
        }
        val p = placementOf(scene.relation)
        val refSize = if (scene.relation == Relation.Inside || scene.relation == Relation.Outside) 84f else 66f
        if (showItem && p.underRef) EmojiAt(scene.item.emoji, p.x, p.y, p.size, scale, p.alpha)
        if (scene.ref2 != null) {
            EmojiAt(scene.ref.emoji, 50f, 100f, 60f, scale)
            EmojiAt(scene.ref2.emoji, 190f, 100f, 60f, scale)
        } else {
            EmojiAt(scene.ref.emoji, 120f, 100f, refSize, scale)
        }
        if (showItem && !p.underRef) EmojiAt(scene.item.emoji, p.x, p.y, p.size, scale)
    }
}

@Composable
private fun ClockView(hour: Int, minute: Int, scale: Float) {
    val tint = Category.Operations.tint
    Canvas(Modifier.size((150 * scale).dp)) {
        val r = size.minDimension / 2
        val c = center
        drawCircle(Color.White, r)
        drawCircle(Ink, r, style = Stroke(width = 4.dp.toPx() * scale))
        repeat(12) { i ->
            val a = Math.toRadians(i * 30.0 - 90)
            val outer = r * 0.92f
            val inner = if (i % 3 == 0) r * 0.78f else r * 0.85f
            drawLine(
                Ink,
                Offset(c.x + (inner * cos(a)).toFloat(), c.y + (inner * sin(a)).toFloat()),
                Offset(c.x + (outer * cos(a)).toFloat(), c.y + (outer * sin(a)).toFloat()),
                strokeWidth = (if (i % 3 == 0) 4 else 2).dp.toPx() * scale
            )
        }
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                textSize = r * 0.24f
                color = android.graphics.Color.rgb(28, 25, 23)
            }
            listOf(12, 3, 6, 9).forEach { n ->
                val a = Math.toRadians(n * 30.0 - 90)
                val d = r * 0.6f
                canvas.nativeCanvas.drawText("$n", c.x + (d * cos(a)).toFloat(), c.y + (d * sin(a)).toFloat() + paint.textSize / 3, paint)
            }
        }
        fun hand(angleDeg: Double, length: Float, width: Float, color: Color) {
            val a = Math.toRadians(angleDeg - 90)
            drawLine(color, c, Offset(c.x + (length * cos(a)).toFloat(), c.y + (length * sin(a)).toFloat()), strokeWidth = width, cap = StrokeCap.Round)
        }
        hand((hour % 12 + minute / 60.0) * 30, r * 0.5f, 7.dp.toPx() * scale, Ink)
        hand(minute * 6.0, r * 0.78f, 4.dp.toPx() * scale, tint)
        drawCircle(Ink, 5.dp.toPx() * scale)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SceneView(scene: Scene, scale: Float = 1f) {
    when (scene) {
        is Scene.Place -> PlaceSceneView(scene, scale)
        is Scene.Count -> FlowRow(
            modifier = Modifier.width((SceneWidth * scale).dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val size = if (scene.count > 10) 30f else 40f
            repeat(scene.count) { Box(Modifier.size((size * scale).dp), contentAlignment = Alignment.Center) { EmojiInBox(scene.emoji, size * scale) } }
        }
        is Scene.Swatch -> Box(
            Modifier
                .size((110 * scale).dp)
                .clip(CircleShape)
                .background(Color(scene.color.argb))
                .border(2.dp, Line, CircleShape)
        )
        is Scene.Emoji -> EmojiInBox(scene.emoji, 110 * scale)
        is Scene.Arrow -> EmojiInBox(scene.direction.emoji, 110 * scale)
        is Scene.Clock -> ClockView(scene.hour, scene.minute, scale)
        is Scene.Label -> Text(scene.text, fontSize = (56 * scale).sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = Ink)
    }
}

@Composable
private fun EmojiInBox(emoji: String, size: Float) {
    val density = LocalDensity.current
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Text(emoji, fontSize = with(density) { (size * 0.8f).dp.toSp() })
    }
}

/** 「点一点放在哪」：参照物四周画虚线圈，点圈作答；答完在正确位置画出物体。 */
@Composable
private fun PlaceSlotsView(question: SpecialQuestion, selected: Int?, answered: Boolean, onPick: (Int) -> Unit) {
    val first = question.options.first().scene as Scene.Place
    val scale = 1.3f
    Box(Modifier.size((SceneWidth * scale).dp, (SceneHeight * scale).dp)) {
        Canvas(Modifier.matchParentSize()) { drawSpatialGuides(null, labels = true) }
        EmojiAt(first.ref.emoji, 120f, 100f, 66f, scale)
        question.options.forEachIndexed { index, option ->
            val p = placementOf((option.scene as Scene.Place).relation)
            val size = 46f
            val color = when {
                answered && index == question.answer -> Success
                answered && index == selected -> Error
                else -> Category.Operations.tint
            }
            Box(
                modifier = Modifier
                    .offset(((p.x - size / 2) * scale).dp, ((p.y - size / 2) * scale).dp)
                    .size((size * scale).dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f))
                    .border(BorderStroke(3.dp, color), CircleShape)
                    .clickable { onPick(index) },
                contentAlignment = Alignment.Center
            ) {
                if (answered && index == question.answer) EmojiInBox(first.item.emoji, size * scale * 0.8f)
            }
        }
    }
}

// ---------- 专项训练：练习页 ----------

@Composable
fun SpecialPracticeScreen(
    topic: SpecialTopic,
    level: Int,
    bestScore: Int?,
    onSpeak: (String) -> Unit,
    onSpeakChinese: (String) -> Unit,
    onFinish: (correct: Int) -> Unit,
    onBack: () -> Unit
) {
    var session by remember { mutableStateOf(0) }
    val questions = remember(session) { buildSpecialPractice(topic, level, Random(System.nanoTime())) }
    var index by remember(session) { mutableStateOf(0) }
    var selected by remember(session, index) { mutableStateOf<Int?>(null) }
    var correctCount by remember(session) { mutableStateOf(0) }
    var finished by remember(session) { mutableStateOf(false) }
    val question = questions.getOrNull(index)
    val accent = Category.Operations
    val answered = selected != null

    fun goNext() {
        if (index >= questions.lastIndex) {
            onFinish(correctCount)
            finished = true
        } else {
            index++
        }
    }

    fun pick(i: Int) {
        if (question == null) return
        if (answered) {
            // 答完后点正确选项再听一遍
            if (i == question.answer) onSpeak(question.spoken)
            return
        }
        selected = i
        if (i == question.answer) {
            correctCount++
            onSpeak(question.spoken)
        }
    }

    // 有听力内容先读听力，否则读英文问句
    LaunchedEffect(session, index) { (question?.speak ?: question?.question)?.let(onSpeak) }

    Scaffold(containerColor = Paper, bottomBar = {
        if (answered && !finished && question != null) NextQuestionBar(if (index >= questions.lastIndex) "看成绩" else "下一题", ::goNext)
    }, topBar = {
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
                AppText("${topic.icon} ${topic.zh} · 看图练习", fontWeight = FontWeight.Bold)
                AppText(
                    "${SpeechLevelNames[level]} · ${(index + 1).coerceAtMost(questions.size)} / ${questions.size} · 答对 $correctCount",
                    color = InkFaint,
                    fontSize = 12.sp
                )
            }
        }
    }) { padding ->
        if (finished || question == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AppText("答对 $correctCount / ${questions.size}", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp)
                AppText(if (correctCount == questions.size) "全对！太棒了！" else "再练一次，争取全对", color = InkSoft, fontSize = 18.sp)
                bestScore?.let { AppText("最好成绩 $it / ${questions.size}", color = InkFaint) }
                Button(onClick = { session++ }, modifier = Modifier.fillMaxWidth()) { Text("再练一次", fontSize = 18.sp) }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("返回", fontSize = 18.sp) }
            }
            return@Scaffold
        }
        val isCorrect = selected == question.answer
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LinearProgressIndicator(
                    progress = (index + if (answered) 1 else 0) / questions.size.toFloat(),
                    color = accent.tint,
                    trackColor = accent.soft,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(99.dp))
                )
            }
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PaperElevated),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.border(1.dp, Line, RoundedCornerShape(18.dp))
                ) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AppText("${question.kind.title} · ${question.kind.titleEn}", color = accent.tint, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (question.speak != null) {
                                IconButton(onClick = { onSpeak(question.speak) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "再听一遍", tint = accent.tint, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(question.question, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp, lineHeight = 30.sp, color = Ink, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onSpeak(question.question) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "读问题", tint = InkFaint)
                            }
                        }
                        if (question.kind == SpecialKind.Place) {
                            PlaceSlotsView(question, selected, answered, ::pick)
                        } else {
                            question.scene?.let { SceneView(it) }
                        }
                        Text(
                            question.prompt,
                            fontSize = if (question.kind == SpecialKind.FillBlank) 26.sp else 18.sp,
                            fontFamily = if (question.kind == SpecialKind.FillBlank) FontFamily.Serif else null,
                            fontWeight = if (question.kind == SpecialKind.FillBlank) FontWeight.SemiBold else null,
                            color = if (question.kind == SpecialKind.FillBlank) Ink else InkSoft,
                            lineHeight = 34.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            if (question.kind != SpecialKind.Place) {
                if (question.options.all { it.scene != null }) {
                    // 图片选项：两两一行
                    items(question.options.indices.chunked(2)) { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            pair.forEach { i -> SpecialOptionCard(question.options[i], i, question.answer, selected, Modifier.weight(1f)) { pick(i) } }
                        }
                    }
                } else {
                    items(question.options.indices.toList()) { i ->
                        SpecialOptionCard(question.options[i], i, question.answer, selected, Modifier.fillMaxWidth()) { pick(i) }
                    }
                }
            }
            item {
                AnimatedVisibility(answered) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = if (isCorrect) Color(0xFFECFDF5) else Color(0xFFFEF2F2)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppText(if (isCorrect) "答对了！" else "再看看正确答案", fontWeight = FontWeight.Bold, color = if (isCorrect) Success else Error)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TranslatableText(
                                    AnnotatedString(question.sentence),
                                    TextStyle(fontFamily = FontFamily.Serif, fontSize = 22.sp, lineHeight = 30.sp, color = Ink),
                                    modifier = Modifier.weight(1f),
                                    onTap = { onSpeak(question.spoken) }
                                )
                                IconButton(onClick = { onSpeak(question.spoken) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = accent.tint)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppText(question.sentenceZh, color = InkSoft, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSpeakChinese(question.sentenceZh) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读中文", tint = InkFaint)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 答完题后固定在底部的大按钮，不用滚到反馈卡片下面去找。 */
@Composable
private fun NextQuestionBar(label: String, onClick: () -> Unit) {
    Surface(color = PaperElevated, shadowElevation = 8.dp) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun FinishLevelBar(onBack: () -> Unit, onNext: () -> Unit) {
    Surface(color = PaperElevated, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("完成并返回", fontSize = 18.sp) }
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("下一关", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun SpecialOptionCard(option: SpecialOption, index: Int, answer: Int, selected: Int?, modifier: Modifier, onClick: () -> Unit) {
    val answered = selected != null
    val color = when {
        answered && index == answer -> Success
        answered && index == selected -> Error
        else -> Line
    }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                answered && index == answer -> Color(0xFFDCFCE7)
                answered && index == selected -> Color(0xFFFEE2E2)
                else -> PaperElevated
            }
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(BorderStroke(if (answered && (index == answer || index == selected)) 2.dp else 1.dp, color), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = if (option.scene != null) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            option.scene?.let { SceneView(it, scale = 0.72f) }
            option.text?.let { Text(it, fontSize = 20.sp, lineHeight = 26.sp, color = Ink) }
        }
    }
}

/** [title] 覆盖题型标题（数字的近义词题改成算术题，标题不能还叫「近义词配对」） */
data class Question(val prompt: String, val answer: String, val options: List<String>, val title: String? = null)

private val NumberValues: Map<String, Int> = (0..99).associateBy(::numberWord) + ("hundred" to 100)

/**
 * 数字没有真正的近义词（词表里 nine 的近义词是 eight / ten），改出算术题：nine minus one = ?
 * 答案和选项都取词表里的数字；凑不出 1–10 以内的加减就返回 null。
 */
private fun arithmeticQuestion(word: OgdenWord, allWords: List<OgdenWord>, random: Random): Question? {
    val n = NumberValues[word.word.lowercase()] ?: return null
    val numbers = allWords.mapNotNull { w -> NumberValues[w.word.lowercase()]?.let { w.word to it } }
    val (answer, value) = numbers.filter { (_, v) -> v != n && kotlin.math.abs(v - n) in 1..10 }.randomOrNull(random) ?: return null
    val op = if (value > n) "plus" else "minus"
    val distractors = numbers
        .filter { (w, _) -> w != answer }
        .sortedBy { (_, v) -> kotlin.math.abs(v - value) }
        .take(3)
        .map { it.first }
    return Question(
        prompt = "${word.word} $op ${numberWord(kotlin.math.abs(value - n))} = ?",
        answer = answer,
        options = (distractors + answer).shuffled(random),
        title = "算一算"
    )
}

/**
 * 填空的空位：每个字母一条下划线，用不换行窄空格隔开，孩子能数出字母数；
 * 词组（in front of）的词间保留普通空格，也看得出有几个词。
 */
fun letterBlank(text: String): String =
    text.split(' ').joinToString(" ") { part -> List(part.length) { "_" }.joinToString("\u202F") }

/** @param lessonWords 课文「本课单词」（小写）；干扰项只从这里挑，避免出现拓展词里的生僻词 */
fun buildQuestion(type: PracticeType, word: OgdenWord, allWords: List<OgdenWord>, lessonWords: Set<String> = emptySet()): Question {
    // 选项顺序也用固定种子：本函数随界面重组反复调用，未加种子的 shuffled() 会让选项每次刷新都换位置
    val random = Random(word.word.hashCode() + type.ordinal)
    val optionPool = allWords.filter { it.word.lowercase() in lessonWords }.takeIf { it.size > 6 } ?: allWords
    val distractors = optionPool
        .filter { it.word != word.word }
        .shuffled(random)
        .take(6)
    return when (type) {
        PracticeType.Listen -> Question(
            prompt = "听声音，选出正确单词",
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled(random)
        )
        PracticeType.Meaning -> Question(
            prompt = word.zh,
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled(random)
        )
        PracticeType.Example -> Question(
            prompt = word.example.replace(Regex("\\b${Regex.escape(word.word)}\\b", RegexOption.IGNORE_CASE)) { letterBlank(it.value) },
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled(random)
        )
        PracticeType.Spelling -> Question(
            prompt = "${word.zh}\n${word.englishDefinition}",
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled(random)
        )
        PracticeType.Synonym -> arithmeticQuestion(word, allWords, random) ?: run {
            // 选项只用词表里的词：近义词列表里混有词表外的生僻词（scarlet、crease…），不拿来出题
            val headwords = allWords.associateBy { it.word.lowercase() }
            val answer = word.synonyms.firstNotNullOfOrNull { headwords[it.lowercase()]?.word }
                .takeIf { word.category.hasTrueSynonyms }
            if (answer == null) buildQuestion(PracticeType.Meaning, word, allWords, lessonWords).copy(title = PracticeType.Meaning.title) else {
                // 干扰项不能和题目词互为近义词，否则出现两个正确答案
                val related = (word.synonyms + word.word).map { it.lowercase() }.toSet()
                val synOptions = optionPool
                    .shuffled(random)
                    .filter { it.word.lowercase() !in related && it.synonyms.none { s -> s.lowercase() == word.word.lowercase() } }
                    .map { it.word }
                    .take(3)
                Question(
                    prompt = "哪个词接近 ${word.word} 的意思？",
                    answer = answer,
                    options = (synOptions + answer).shuffled(random)
                )
            }
        }
    }
}

fun nextLevel(words: List<OgdenWord>, category: Category, store: ProgressStore): Int {
    val categoryWords = words.filter { it.category == category }
    val firstUnmastered = categoryWords.indexOfFirst { !store.progress(it.word).mastered }
    return if (firstUnmastered < 0) 1 else firstUnmastered / 10 + 1
}

@Composable
fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        AppText(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 24.sp)
        AppText(subtitle, color = InkFaint, fontSize = 13.sp)
    }
}

@Composable
fun CategoryDot(category: Category) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(category.tint)
    )
}

@Composable
fun CategoryBadge(category: Category) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(category.soft)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryDot(category)
        Spacer(Modifier.width(7.dp))
        AppText(category.zh, color = category.tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun TogglePill(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(99.dp))
            .background(if (selected) Ink else PaperElevated)
            .border(1.dp, if (selected) Ink else Line, RoundedCornerShape(99.dp)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
    ) {
        AppText(label, color = if (selected) Color.White else InkSoft, maxLines = 1, fontSize = 12.sp)
    }
}

/** 「儿语 / 口语」小标签 */
@Composable
fun RegisterBadge(tag: String) {
    val category = if (tag == Category.BabyTalk.zh) Category.BabyTalk else Category.Informal
    Text(
        tag,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = category.tint,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(category.soft)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/** 标签 + 「正式说法：stomach」，点正式说法跳到它的词条 */
@Composable
fun RegisterNote(word: String) {
    val register = registerOf(word) ?: return
    val services = LocalSpeechServices.current
    val formal = services.lookup(register.formal)
    Row(verticalAlignment = Alignment.CenterVertically) {
        AppText("正式说法：", color = InkSoft, fontSize = 15.sp)
        Text(
            formal?.word ?: register.formal,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = Category.Qualities.tint,
            modifier = Modifier.clickable(enabled = formal != null) { formal?.let(services.openWord) }
        )
        formal?.let { AppText("  ${it.zh}", color = InkFaint, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun CollocationCard(c: Collocation, zh: ChineseMode, onSpeak: (String) -> Unit, onSpeakZh: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    c.phrase,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = Category.Operations.tint,
                    modifier = Modifier.clickable { onSpeak(c.phrase) }
                )
                AppText("  ${convertZh(c.zh, zh)}", color = InkSoft, fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSpeak(c.phrase) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读搭配", tint = Category.Operations.tint)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TranslatableText(
                    AnnotatedString(c.example),
                    TextStyle(fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 26.sp, color = Ink),
                    modifier = Modifier.weight(1f),
                    onTap = { onSpeak(c.example) }
                )
                IconButton(onClick = { onSpeak(c.example) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = InkFaint)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(convertZh(c.exampleZh, zh), color = InkFaint, fontSize = 15.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { onSpeakZh(c.exampleZh) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读例句中文", tint = InkFaint)
                }
            }
        }
    }
}

@Composable
fun InfoBlock(title: String, en: String, zh: String, tint: Color, onSpeakZh: (() -> Unit)? = null, onSpeak: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.border(1.dp, Line, RoundedCornerShape(14.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppText(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onSpeak) { Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = tint) }
            }
            TranslatableText(AnnotatedString(en), TextStyle(fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 28.sp, color = Ink), onTap = onSpeak)
            Text(
                zh,
                color = InkSoft,
                modifier = if (onSpeakZh != null) Modifier.clickable(onClick = onSpeakZh) else Modifier
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FlowRowCompat(items: List<String>, chip: @Composable (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { chip(it) }
    }
}

@Composable
fun CompactWordRow(word: OgdenWord, progress: WordProgress, zh: ChineseMode, onOpen: (OgdenWord) -> Unit, onRemove: ((OgdenWord) -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(PaperElevated)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable { onOpen(word) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryDot(word.category)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(word.word, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 22.sp)
            Text(convertZh(word.zh, zh), color = InkSoft, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row {
            repeat(progress.mastery) {
                Icon(Icons.Default.Star, contentDescription = null, tint = Category.Picturable.tint, modifier = Modifier.size(16.dp))
            }
        }
        if (onRemove != null) {
            IconButton(onClick = { onRemove(word) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "移出", tint = InkFaint, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun EmptyCard(text: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(14.dp))
    ) {
        AppText(text, color = InkFaint, modifier = Modifier.padding(18.dp))
    }
}

fun convertZh(text: String, mode: ChineseMode): String {
    if (mode == ChineseMode.Hans) return text
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        runCatching { return ChineseTransliterator.hansToHant.transliterate(text) }
    }
    val map = mapOf(
        '来' to '來', '为' to '為', '后' to '後', '发' to '發', '个' to '個', '这' to '這',
        '那' to '那', '们' to '們', '说' to '說', '见' to '見', '书' to '書', '车' to '車',
        '门' to '門', '开' to '開', '关' to '關', '学' to '學', '习' to '習', '读' to '讀',
        '词' to '詞', '义' to '義', '类' to '類', '时' to '時', '间' to '間', '过' to '過',
        '边' to '邊', '还' to '還', '进' to '進', '远' to '遠', '气' to '氣', '声' to '聲',
        '头' to '頭', '轻' to '輕', '对' to '對', '错' to '錯', '简' to '簡', '体' to '體',
        '长' to '長', '鱼' to '魚', '鸟' to '鳥', '马' to '馬', '贝' to '貝', '叶' to '葉',
        '风' to '風', '云' to '雲', '电' to '電', '画' to '畫', '圆' to '圓',
        '复' to '複', '习' to '習', '软' to '軟', '隐' to '隱', '私' to '私', '页' to '頁',
        '显' to '顯', '示' to '示', '数' to '數', '据' to '據', '与' to '與', '双' to '雙',
        '语' to '語', '离' to '離', '线' to '線', '儿' to '兒', '闯' to '闖', '关' to '關',
        '续' to '續', '之' to '之', '前' to '前', '应' to '應', '用' to '用', '说' to '說',
        '明' to '明', '权' to '權', '限' to '限', '获' to '獲', '取' to '取', '基' to '基',
        '础' to '礎', '户' to '戶', '信' to '信', '息' to '息', '操' to '操', '作' to '作',
        '选' to '選', '择' to '擇', '锁' to '鎖', '定' to '定', '进' to '進', '入' to '入',
        '错' to '錯', '题' to '題', '记' to '記', '录' to '錄', '夹' to '夾', '收' to '收',
        '藏' to '藏', '软' to '軟', '件' to '件', '设' to '設', '置' to '置', '发' to '發',
        '音' to '音', '隐' to '隱', '关' to '關', '于' to '於', '者' to '者', '总' to '總',
        '暂' to '暫', '没' to '沒', '颗' to '顆', '随' to '隨', '机' to '機', '战' to '戰',
        '场' to '場', '景' to '景', '儿' to '兒', '爱' to '愛', '护' to '護', '卖' to '賣',
        '传' to '傳', '务' to '務', '习' to '習', '历' to '歷', '创' to '創', '链' to '鏈',
        '接' to '接', '调' to '調', '整' to '整', '验' to '驗', '证' to '證', '览' to '覽',
        '览' to '覽', '览' to '覽', '后' to '後', '会' to '會', '变' to '變', '声' to '聲',
        '桥' to '橋', '门' to '門', '种' to '種', '练' to '練', '实' to '實', '际' to '際',
        '备' to '備', '尝' to '嘗', '试' to '試', '觉' to '覺', '拥' to '擁', '护' to '護'
    )
    return buildString(text.length) {
        text.forEach { append(map[it] ?: it) }
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.N)
private object ChineseTransliterator {
    val hansToHant: Transliterator = Transliterator.getInstance("Simplified-Traditional")
}
