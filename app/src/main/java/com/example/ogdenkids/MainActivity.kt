package com.example.ogdenkids

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.icu.text.Transliterator
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.ceil
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
    Forms("fm", "Word Forms", "变形词", Color(0xFFBE185D), Color(0xFFFCE7F3));

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
    val ipaUs: String
)

data class WordProgress(
    val favorite: Boolean,
    val mistake: Boolean,
    val mastery: Int,
    val attempts: Int,
    val correct: Int
)

enum class Tab(val title: String, val icon: ImageVector) {
    Speech("课文", Icons.Default.RecordVoiceOver),
    Challenge("闯关", Icons.Default.Star),
    Library("词库", Icons.Default.Book),
    Review("复习", Icons.Default.Refresh),
    Software("软件", Icons.Default.Info)
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
    data class Practice(val category: Category, val level: Int, val reviewOnly: Boolean = false) : Screen()
    data class WordCollection(val title: String, val kind: String) : Screen()
    data class SpeechReader(val speech: Speech) : Screen()
    data class ThemePractice(val level: Int, val theme: SpeechTheme) : Screen()
    data class WordPractice(val title: String, val words: List<String>, val returnTo: Screen) : Screen()
    data class SpecialPractice(val topic: SpecialTopic, val level: Int) : Screen()
    object SpeechWords : Screen()
    object Settings : Screen()
    object Privacy : Screen()
    object About : Screen()
}

class OgdenRepository(private val context: Context) {
    fun loadWords(): List<OgdenWord> {
        val words = JSONArray(readAsset("ogden_words.json"))
        val ipa = JSONObject(readAsset("ogden_ipa.json"))
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
                synonyms = List(synonyms.length()) { synonyms.getString(it) },
                ipaUk = ipaItem?.optString("uk").orEmpty(),
                ipaUs = ipaItem?.optString("us").orEmpty()
            )
        }
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

    fun masteredCount(words: List<OgdenWord>) = words.count { progress(it.word).mastery >= 3 }

    fun mistakeWords(words: List<OgdenWord>) = words.filter { progress(it.word).mistake }

    fun favoriteWords(words: List<OgdenWord>) = words.filter { progress(it.word).favorite }

    /** 课文里收藏的词表外单词（词表内的走 favorites），存小写原文。 */
    fun speechWords(): List<String> = set("speechWords").sorted()

    fun isSpeechWordSaved(word: String) = set("speechWords").contains(word)

    fun toggleSpeechWord(word: String) = updateSet("speechWords", word, !isSpeechWordSaved(word))

    fun isSpeechLearned(id: String) = set("speechLearned").contains(id)

    fun toggleSpeechLearned(id: String) = updateSet("speechLearned", id, !isSpeechLearned(id))

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
    val wordIndex = remember(words) { words.associateBy { it.word.lowercase() } }
    val lemmaVocabulary = remember(words) {
        fun keysOf(vararg categories: Category) =
            words.filter { it.category in categories }.map { it.word.lowercase() }.toSet()
        LemmaVocabulary(
            words = wordIndex.keys,
            qualities = keysOf(Category.Qualities, Category.Opposites, Category.Extended),
            things = keysOf(Category.GeneralThings, Category.Picturable, Category.Extended)
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
    val speak = rememberSpeaker(accent)
    val azureSpeaker = remember { AzureSpeaker(context) }
    DisposableEffect(azureSpeaker) {
        onDispose { azureSpeaker.release() }
    }
    val speakEnglish: (String) -> Unit = { azureSpeaker.speak(it, AzureVoice.english(accent)) }
    val speakChinese: (String) -> Unit = { azureSpeaker.speak(it, AzureVoice.ZhCn) }
    val goBack: () -> Unit = {
        when (val current = screen) {
            Screen.Main -> confirmExit = true
            is Screen.Detail -> {
                azureSpeaker.stop()
                screen = current.returnTo
            }
            is Screen.Practice -> {
                version++
                screen = Screen.Main
            }
            is Screen.WordPractice -> {
                version++
                screen = current.returnTo
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
        CompositionLocalProvider(LocalChineseMode provides chineseMode) {
        Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
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
                    accent = accent,
                    onAccent = { accent = it; progressStore.saveAccent(it) },
                    chineseMode = chineseMode,
                    onChineseMode = { chineseMode = it; progressStore.saveChineseMode(it) },
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
                                onOpenLibrary = { selectedTab = Tab.Library }
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
                                onSpecial = { topic -> screen = Screen.SpecialPractice(topic, speechLevel) },
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
                            Tab.Software -> SoftwareScreen(
                                padding = padding,
                                onSettings = { screen = Screen.Settings },
                                onPrivacy = { screen = Screen.Privacy },
                                onAbout = { screen = Screen.About }
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
                    words = words,
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
                        allWords = words,
                        store = progressStore,
                        version = version,
                        category = current.category,
                        level = current.level,
                        reviewOnly = current.reviewOnly,
                        zh = chineseMode,
                        onSpeak = speak,
                        onBack = goBack,
                        onComplete = {
                            if (!current.reviewOnly) progressStore.markLevelComplete(current.category, current.level)
                        },
                        onRecord = { word, correct ->
                            progressStore.record(word.word, correct)
                            version++
                        }
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
                    allWords = words,
                    store = progressStore,
                    version = version,
                    category = Category.Extended,
                    level = 1,
                    reviewOnly = false,
                    zh = chineseMode,
                    onSpeak = speak,
                    onBack = goBack,
                    onComplete = {},
                    onRecord = { word, correct ->
                        progressStore.record(word.word, correct)
                        version++
                    },
                    customWords = current.words.mapNotNull { wordIndex[it.lowercase()] },
                    customTitle = current.title
                )
                is Screen.WordCollection -> WordCollectionScreen(
                    title = current.title,
                    words = if (current.kind == "mistakes") progressStore.mistakeWords(words) else progressStore.favoriteWords(words),
                    store = progressStore,
                    zh = chineseMode,
                    onBack = goBack,
                    onOpen = { screen = Screen.Detail(it) }
                )
                is Screen.ThemePractice -> ThemePracticeScreen(
                    level = current.level,
                    theme = current.theme,
                    units = speeches.filter { it.level == current.level && it.theme == current.theme },
                    vocabulary = speeches.filter { it.level == current.level && it.theme == current.theme }
                        .flatMap { it.words }
                        .mapNotNull { wordIndex[it.lowercase()] },
                    bestScore = progressStore.bestThemeScore(current.level, current.theme),
                    onSpeak = speakEnglish,
                    onSpeakWord = { azureSpeaker.stop(); speak(it) },
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
                    onBack = goBack,
                    onSpeak = speakEnglish,
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
                Screen.About -> LegalInfoScreen(
                    title = "关于作者",
                    onBack = goBack,
                    sections = aboutSections(),
                    links = listOf(
                        "原作地址" to "https://ogden.munch.love/",
                        "Skivein 的主页" to "https://longlong-skyligo.github.io/about/",
                        "创作初衷" to "https://longlong-skyligo.github.io/posts/basic-english/"
                    )
                )
            }
            }
        }
        }
    }
}

@Composable
fun rememberSpeaker(accent: Accent): (String) -> Unit {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var ready by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
            }
        }
        tts = engine
        onDispose {
            player?.release()
            engine.shutdown()
        }
    }
    LaunchedEffect(accent, tts) {
        tts?.setBestLanguage(accent)
    }
    return remember(accent, tts, ready, player) {
        { rawText: String ->
            val text = rawText.trim()
            val engine = tts
            if (text.isNotEmpty()) {
                player?.release()
                fun fallbackTts() {
                    if (engine != null) {
                        engine.setBestLanguage(accent)
                        engine.setSpeechRate(0.82f)
                        engine.setPitch(1.04f)
                        val params = Bundle().apply {
                            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                        }
                        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "ogden-${System.nanoTime()}")
                    }
                }
                fun playOnline() {
                    runCatching {
                        val mediaPlayer = MediaPlayer()
                        player = mediaPlayer
                        mediaPlayer.setDataSource(ogdenTtsUrl(text, accent))
                        mediaPlayer.setOnPreparedListener { it.start() }
                        mediaPlayer.setOnCompletionListener {
                            it.release()
                            if (player === it) player = null
                        }
                        mediaPlayer.setOnErrorListener { mp, _, _ ->
                            mp.release()
                            if (player === mp) player = null
                            fallbackTts()
                            true
                        }
                        mediaPlayer.prepareAsync()
                    }.onFailure { fallbackTts() }
                }
                val localPath = localAudioPath(text, accent)
                if (localPath == null) {
                    playOnline()
                } else {
                    runCatching {
                        val fd = context.assets.openFd(localPath)
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
                            playOnline()
                            true
                        }
                        mediaPlayer.prepareAsync()
                    }.onFailure { playOnline() }
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
    Screen.SpeechWords -> "speech-words"
    Screen.Settings -> "settings"
    Screen.Privacy -> "privacy"
    Screen.About -> "about"
}

private fun localAudioPath(text: String, accent: Accent): String? {
    if (!text.matches(Regex("[A-Za-z][A-Za-z0-9'-]*"))) return null
    val file = text.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_").trim('_')
    if (file.isBlank()) return null
    val dir = if (accent == Accent.US) "us" else "uk"
    return "audio/$dir/$file.mp3"
}

private fun ogdenTtsUrl(text: String, accent: Accent): String {
    val encoded = URLEncoder.encode(text, "UTF-8")
    val accentParam = if (accent == Accent.US) "us" else "uk"
    val rate = if (text.split(Regex("\\s+")).size <= 1) "+0%" else "-6%"
    return "https://ogden.munch.love/api/tts?text=$encoded&accent=$accentParam&rate=${URLEncoder.encode(rate, "UTF-8")}&v=android"
}

private fun TextToSpeech.setBestLanguage(accent: Accent) {
    val result = setLanguage(accent.locale)
    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED || result == TextToSpeech.ERROR) {
        setLanguage(Locale.ENGLISH)
    }
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
    accent: Accent,
    onAccent: (Accent) -> Unit,
    chineseMode: ChineseMode,
    onChineseMode: (ChineseMode) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = Paper,
        bottomBar = {
            Column {
                if (selectedTab == Tab.Library) {
                    SettingsToggleRow(
                        accent = accent,
                        chineseMode = chineseMode,
                        onAccent = onAccent,
                        onChineseMode = onChineseMode
                    )
                }
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
    onOpenLibrary: () -> Unit
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
                title = "Ogden's Basic English",
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
                streak = store.dailyStreak()
            )
        }
        item {
            SectionTitle("分类闯关", "每 10 个词一关，先短跑，再复习")
        }
        items(Category.values()) { category ->
            val categoryWords = words.filter { it.category == category }
            val learned = categoryWords.count { store.progress(it.word).mastery >= 3 }
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
fun StatsRow(learned: Int, mistakes: Int, favorites: Int, streak: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        StatCard("已掌握", learned.toString(), Category.GeneralThings.tint, Modifier.weight(1f))
        StatCard("错词", mistakes.toString(), Error, Modifier.weight(1f))
        StatCard("收藏", favorites.toString(), Category.Opposites.tint, Modifier.weight(1f))
        StatCard("连续", "${streak}天", Category.Picturable.tint, Modifier.weight(1f))
    }
}

@Composable
fun StatCard(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.border(1.dp, Line, RoundedCornerShape(14.dp)),
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
                val mastered = levelWords.count { store.progress(it.word).mastery >= 3 }
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
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<Category?>(null) }
    val filtered = words.filter { word ->
        (category == null || word.category == category) &&
            (query.isBlank() ||
                word.word.contains(query, ignoreCase = true) ||
                word.zh.contains(query) ||
                word.englishDefinition.contains(query, ignoreCase = true))
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
            AppText("显示 ${filtered.size} 个词", color = InkFaint, fontSize = 13.sp)
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
                        Spacer(Modifier.width(8.dp))
                        Text(if (accent == Accent.UK) word.ipaUk else word.ipaUs, color = InkFaint, fontStyle = FontStyle.Italic)
                    }
                    Text(convertZh(word.zh, zh), fontWeight = FontWeight.Medium, color = Ink)
                }
                IconButton(onClick = { onSpeak(word.word) }) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "读单词", tint = word.category.tint)
                }
            }
            Text(word.englishDefinition, color = InkFaint, fontStyle = FontStyle.Italic)
            Text(word.example, color = InkSoft, fontFamily = FontFamily.Serif)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                word.synonyms.take(3).forEach { AssistChip(onClick = { onSpeak(it) }, label = { Text(it) }) }
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
                            Text(word.word, fontSize = 46.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
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
                        Text(word.englishDefinition, color = InkSoft, fontStyle = FontStyle.Italic)
                    }
                }
            }
            item {
                InfoBlock(
                    "例句",
                    word.example,
                    convertZh(word.exampleZh, zh),
                    word.category.tint,
                    onSpeakZh = { onSpeakZh(word.exampleZh) }
                ) {
                    onSpeak(word.example)
                }
            }
            item {
                SectionTitle("近义词", "点击可听发音")
                FlowRowCompat(word.synonyms) { syn ->
                    AssistChip(onClick = { onSpeak(syn) }, label = { Text(syn) })
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
                subtitle = if (speechWords.isEmpty()) "在课文里长按单词即可收藏" else "课文中收藏的词表外的词",
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
    onOpen: (OgdenWord) -> Unit
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
                    CompactWordRow(word, store.progress(word.word), zh, onOpen)
                }
            }
        }
    }
}

private val SpeechLevelNames = mapOf(1 to "一级", 2 to "二级", 3 to "三级")
private val SpeechLevelThemes = mapOf(1 to "起步", 2 to "成长", 3 to "表达")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun SpeechListScreen(
    speeches: List<Speech>,
    store: ProgressStore,
    level: Int,
    onLevel: (Int) -> Unit,
    onPractice: (SpeechTheme) -> Unit,
    onSpecial: (SpecialTopic) -> Unit,
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
                AppText("专项训练", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecialTopic.values().forEach { topic ->
                        val best = store.bestSpecialScore(topic.key, level)
                        OutlinedButton(
                            onClick = { onSpecial(topic) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(topic.icon, fontSize = 22.sp)
                                AppText(topic.zh, fontSize = 15.sp, color = Ink)
                                if (best != null) AppText("最佳 $best", fontSize = 11.sp, color = InkFaint)
                            }
                        }
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
    val listState = rememberLazyListState()
    var showTranslation by remember(speech.id) { mutableStateOf(true) }
    var learned by remember(speech.id) { mutableStateOf(store.isSpeechLearned(speech.id)) }
    // 收藏存在 SharedPreferences 里，不是 Compose 状态；改动后递增它，让高亮与词卡重新读取
    var favoriteVersion by remember { mutableStateOf(0) }
    fun ogdenWordOf(token: SpeechToken) = lemmatize(token.text, lemmaVocabulary)?.let { wordIndex[it] }
    fun isSaved(token: SpeechToken): Boolean {
        val word = ogdenWordOf(token)
        return if (word != null) store.progress(word.word).favorite else store.isSpeechWordSaved(speechWordKey(token.text))
    }
    val allLines = speech.lines + speech.patterns
    val hasContraction = remember(speech.id) { allLines.any { line -> tokenizeSpeech(line.en).any { contractionOf(it.text) != null } } }

    fun speakOne(index: Int, chinese: Boolean) {
        playingAll = false
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
        speakingIndex = null
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
            onTokenClick = { selected = SelectedSpeechWord(index, it) }
        )
    }

    Scaffold(containerColor = Paper, floatingActionButtonPosition = FabPosition.Center, floatingActionButton = {
        // 全文朗读时滚动会把标题里的按钮滚出屏幕，停止按钮悬浮在底部始终可点；单句很短，不需要
        if (playingAll) {
            ExtendedFloatingActionButton(
                onClick = { stopSpeaking() },
                icon = { Icon(Icons.Default.Close, contentDescription = null) },
                text = { Text("停止朗读", fontSize = 18.sp) },
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
                    subtitle = if (hasContraction) "点句子听朗读，长按单词查释义；带下划线的是缩写，长按看完整写法"
                    else "点句子听朗读，长按单词查释义",
                    playingAll = playingAll,
                    onPlayAll = {
                        if (playingAll) {
                            stopSpeaking()
                        } else {
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
                            ) { Text(wordIndex[w.lowercase()]?.word ?: w, fontSize = 20.sp, fontFamily = FontFamily.Serif, color = Ink) }
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

    // 弹窗一出来就朗读标题上的词：词表词用离线录音，缩写和词表外的词读原文
    LaunchedEffect(selected) {
        val token = selected?.token ?: return@LaunchedEffect
        stopSpeaking()
        val word = ogdenWordOf(token)
        if (word != null && contractionOf(token.text) == null) onSpeakWord(word.word) else onSpeakEnglish(token.text, null)
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
                    if (word != null) store.toggleFavorite(word.word) else store.toggleSpeechWord(speechWordKey(current.token.text))
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
    onTokenClick: (SpeechToken) -> Unit
) {
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
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (speaking) accent.soft else PaperElevated),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .border(
                if (speaking) 2.dp else 1.dp,
                if (speaking) accent.tint else Line,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSpeakEnglish)
    ) {
        Row(Modifier.padding(start = 16.dp, top = 6.dp, bottom = 6.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // 点句子任意处朗读整句；长按某个词查释义
                Text(
                    text = text,
                    style = TextStyle(fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 36.sp, color = Ink),
                    onTextLayout = { layout = it },
                    modifier = Modifier.pointerInput(tokens) {
                        detectTapGestures(
                            onTap = { onSpeakEnglish() },
                            onLongPress = { position ->
                                val offset = layout?.getOffsetForPosition(position) ?: return@detectTapGestures
                                tokens.firstOrNull { offset in it.range }?.let(onTokenClick)
                            }
                        )
                    }
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
            }
            IconButton(onClick = onSpeakEnglish, modifier = Modifier.size(56.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = Category.Operations.tint, modifier = Modifier.size(32.dp))
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
            if (word.example.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Paper),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(word.example, fontFamily = FontFamily.Serif, fontSize = 20.sp, lineHeight = 28.sp, color = Ink, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onSpeakEnglish(word.example) }) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "读例句", tint = Category.Operations.tint)
                            }
                        }
                        if (word.exampleZh.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppText(word.exampleZh, color = InkSoft, fontSize = 17.sp, lineHeight = 24.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSpeakChinese(word.exampleZh) }) {
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
                AppText(if (saved) "已收藏" else "收藏")
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
                item { EmptyCard("在课文里长按词表外的词即可收藏到这里。") }
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
                        Text(word, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, fontSize = 22.sp, modifier = Modifier.weight(1f))
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
fun SoftwareScreen(
    padding: PaddingValues,
    onSettings: () -> Unit,
    onPrivacy: () -> Unit,
    onAbout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionTitle("关于软件", "应用说明、隐私声明与作者信息")
        }
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PaperElevated),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Panda English", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    AppText("英语单词学习 · 离线词库 · US/UK 单词发音", color = InkSoft)
                    AppText("当前版本：1.0", color = InkFaint, fontSize = 13.sp)
                }
            }
        }
        item {
            OutlinedButton(
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                AppText("软件设置")
            }
        }
        item {
            OutlinedButton(
                onClick = onPrivacy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                AppText("隐私声明")
            }
        }
        item {
            OutlinedButton(
                onClick = onAbout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                AppText("关于作者")
            }
        }
    }
}

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
        "应用已内置 US / UK 两套单词发音音频，用于离线播放。对于例句、长文本或本地音频不可用的情况，应用可能访问在线发音服务作为备用，该请求仅包含需要发音的英文文本，不包含用户身份信息。课文与中文释义的朗读使用 Microsoft Azure 语音服务，请求仅包含需要朗读的英文或中文文本，合成的音频缓存在本机。"
    )
)

fun aboutSections() = listOf(
    LegalSection(
        "应用来源",
        "本应用基于 Panda English 850 词学习内容进行二次创作，并补充 350 个儿童生活常用拓展词，面向中文英语学习场景重新设计为 Android App。"
    ),
    LegalSection(
        "原作说明",
        "原始内容与视觉风格参考 Ogden's Basic English · 850 词学习手册。原网站与词汇学习内容由 ogden.munch.love 的原创作者整理与设计。本应用尊重原作内容与设计风格，并保留原作地址以便用户访问原始版本。"
    ),
    LegalSection(
        "二次创作",
        "本 Android App 由 Skivein 基于原作内容进行二次创作，主要包括 Android 原生界面、离线词库、学习进度、收藏、错词本、熟练度记录、闯关复习流程，以及 US / UK 单词离线发音音频。"
    ),
    LegalSection(
        "作者",
        "二次创作者：Skivein。个人主页：Aetheris，记录科技、人文、人工智能、金融市场与地缘历史相关思考。"
    )
)

@Composable
fun PracticeScreen(
    allWords: List<OgdenWord>,
    store: ProgressStore,
    version: Int,
    category: Category,
    level: Int,
    reviewOnly: Boolean,
    zh: ChineseMode,
    onSpeak: (String) -> Unit,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onRecord: (OgdenWord, Boolean) -> Unit,
    customWords: List<OgdenWord>? = null,
    customTitle: String? = null
) {
    val source = remember(version, category, level, reviewOnly, customWords) {
        val reviewWords = store.mistakeWords(allWords)
        if (customWords != null) customWords
        else if (reviewOnly && reviewWords.isNotEmpty()) reviewWords.take(10)
        else allWords.filter { it.category == category }.drop((level - 1) * 10).take(10)
    }
    var index by remember(source) { mutableStateOf(0) }
    var selected by remember(source) { mutableStateOf<String?>(null) }
    var answerShown by remember(source) { mutableStateOf(false) }
    var correctCount by remember(source) { mutableStateOf(0) }
    val word = source.getOrNull(index)

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
        val question = remember(word, type) { buildQuestion(type, word, allWords) }
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
                        CategoryBadge(word.category)
                        AppText(type.title, color = word.category.tint, fontWeight = FontWeight.Bold)
                        if (type == PracticeType.Listen) {
                            Button(onClick = { onSpeak(word.word) }, shape = CircleShape, modifier = Modifier.size(96.dp)) {
                                Icon(Icons.Default.VolumeUp, contentDescription = "播放", modifier = Modifier.size(44.dp))
                            }
                        } else {
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
                            if (ok) correctCount++
                            onRecord(word, ok)
                            answerShown = true
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
                            Text("${word.word} · ${convertZh(word.zh, zh)}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Text(word.example, fontFamily = FontFamily.Serif, color = InkSoft)
                            Button(
                                onClick = {
                                    if (index >= source.lastIndex) {
                                        onComplete()
                                        onBack()
                                    }
                                    else {
                                        index++
                                        selected = null
                                        answerShown = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                AppText(if (index >= source.lastIndex) "完成并返回" else "下一题")
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
    level: Int,
    theme: SpeechTheme,
    units: List<Speech>,
    vocabulary: List<OgdenWord>,
    bestScore: Int?,
    onSpeak: (String) -> Unit,
    onSpeakWord: (String) -> Unit,
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

    fun submit(ok: Boolean) {
        answered = true
        if (ok) correctCount++
    }

    LaunchedEffect(session, index) {
        when (question?.type) {
            SentenceQuestionType.Listen -> onSpeak(question.prompt)
            SentenceQuestionType.WordListen -> onSpeakWord(question.prompt)
            else -> Unit
        }
    }

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
                AppText("主题练习 · ${theme.zh}", fontWeight = FontWeight.Bold)
                AppText(
                    "${SpeechLevelNames[level]}${SpeechLevelThemes[level]} · ${(index + 1).coerceAtMost(questions.size)} / ${questions.size} · 答对 $correctCount",
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
                        AppText(question.type.title, color = accent.tint, fontWeight = FontWeight.Bold)
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
                                Column(Modifier.weight(1f)) {
                                    Text(question.sentence, fontFamily = FontFamily.Serif, fontSize = 22.sp)
                                    if (isWordQuestion(question)) AppText(question.hint, color = InkSoft, fontSize = 18.sp)
                                }
                                IconButton(onClick = { speakQuestion(question) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = accent.tint)
                                }
                            }
                            Button(
                                onClick = {
                                    if (index >= questions.lastIndex) {
                                        onFinish(correctCount, questions.size)
                                        finished = true
                                    } else {
                                        index++
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                AppText(if (index >= questions.lastIndex) "看成绩" else "下一题", fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- 专项训练：场景绘制 ----------

private const val SceneWidth = 240f
private const val SceneHeight = 190f

/** 物体中心点（基准画布 240×190 dp 内）、大小，以及是否画在参照物之前（被挡住）。 */
private data class Placement(val x: Float, val y: Float, val size: Float, val underRef: Boolean = false)

private fun placementOf(relation: Relation): Placement = when (relation) {
    Relation.On -> Placement(120f, 46f, 40f)
    Relation.Above -> Placement(120f, 20f, 36f)
    Relation.Under -> Placement(120f, 144f, 40f)
    Relation.Below -> Placement(120f, 172f, 36f)
    Relation.In -> Placement(120f, 80f, 30f, underRef = true)
    Relation.Behind -> Placement(152f, 66f, 34f, underRef = true)
    Relation.InFrontOf -> Placement(108f, 128f, 46f)
    Relation.NextTo, Relation.Beside, Relation.RightOf -> Placement(172f, 100f, 40f)
    Relation.Near -> Placement(212f, 100f, 36f)
    Relation.LeftOf -> Placement(66f, 100f, 40f)
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

@Composable
private fun PlaceSceneView(scene: Scene.Place, scale: Float, showItem: Boolean = true) {
    Box(Modifier.size((SceneWidth * scale).dp, (SceneHeight * scale).dp)) {
        val p = placementOf(scene.relation)
        val refSize = if (scene.relation == Relation.Inside || scene.relation == Relation.Outside) 84f else 66f
        if (showItem && p.underRef) EmojiAt(scene.item.emoji, p.x, p.y, p.size, scale)
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
                    .clickable(enabled = !answered) { onPick(index) },
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

    fun pick(i: Int) {
        if (answered || question == null) return
        selected = i
        if (i == question.answer) correctCount++
    }

    LaunchedEffect(session, index) { question?.speak?.let(onSpeak) }

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
                            AppText(question.kind.title, color = accent.tint, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (question.speak != null) {
                                IconButton(onClick = { onSpeak(question.speak) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "再听一遍", tint = accent.tint, modifier = Modifier.size(32.dp))
                                }
                            }
                        }
                        if (question.kind == SpecialKind.Place) {
                            PlaceSlotsView(question, selected, answered, ::pick)
                        } else {
                            question.scene?.let { SceneView(it) }
                        }
                        Text(
                            question.prompt,
                            fontSize = if ("____" in question.prompt) 26.sp else 22.sp,
                            fontFamily = if ("____" in question.prompt) FontFamily.Serif else null,
                            fontWeight = FontWeight.SemiBold,
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
                                Text(question.sentence, fontFamily = FontFamily.Serif, fontSize = 22.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSpeak(question.sentence) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读", tint = accent.tint)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AppText(question.sentenceZh, color = InkSoft, fontSize = 17.sp, modifier = Modifier.weight(1f))
                                IconButton(onClick = { onSpeakChinese(question.sentenceZh) }) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = "朗读中文", tint = InkFaint)
                                }
                            }
                            Button(
                                onClick = {
                                    if (index >= questions.lastIndex) {
                                        onFinish(correctCount)
                                        finished = true
                                    } else {
                                        index++
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) { AppText(if (index >= questions.lastIndex) "看成绩" else "下一题", fontSize = 18.sp) }
                        }
                    }
                }
            }
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
            .clickable(enabled = !answered, onClick = onClick)
    ) {
        Column(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = if (option.scene != null) Alignment.CenterHorizontally else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            option.scene?.let { SceneView(it, scale = 0.62f) }
            option.text?.let { Text(it, fontSize = 20.sp, lineHeight = 26.sp, color = Ink) }
        }
    }
}

data class Question(val prompt: String, val answer: String, val options: List<String>)

fun buildQuestion(type: PracticeType, word: OgdenWord, allWords: List<OgdenWord>): Question {
    // 选项顺序也用固定种子：本函数随界面重组反复调用，未加种子的 shuffled() 会让选项每次刷新都换位置
    val random = Random(word.word.hashCode() + type.ordinal)
    val distractors = allWords
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
            prompt = word.example.replace(Regex("\\b${Regex.escape(word.word)}\\b", RegexOption.IGNORE_CASE), "____"),
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled(random)
        )
        PracticeType.Spelling -> Question(
            prompt = "${word.zh}\n${word.englishDefinition}",
            answer = word.word,
            options = (distractors.take(3).map { it.word } + word.word).shuffled(random)
        )
        PracticeType.Synonym -> {
            val answer = word.synonyms.firstOrNull() ?: word.word
            val synOptions = distractors.flatMap { it.synonyms.take(1) }.take(3)
            Question(
                prompt = "哪个词接近 ${word.word} 的意思？",
                answer = answer,
                options = (synOptions + answer).distinct().shuffled(random)
            )
        }
    }
}

fun nextLevel(words: List<OgdenWord>, category: Category, store: ProgressStore): Int {
    val categoryWords = words.filter { it.category == category }
    val firstUnmastered = categoryWords.indexOfFirst { store.progress(it.word).mastery < 3 }
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
            Text(en, fontFamily = FontFamily.Serif, fontSize = 20.sp, color = Ink)
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
fun CompactWordRow(word: OgdenWord, progress: WordProgress, zh: ChineseMode, onOpen: (OgdenWord) -> Unit) {
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
