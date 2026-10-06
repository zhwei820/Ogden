package com.example.ogdenkids

data class SpeechLine(val en: String, val zh: String)

/** 三个级别共用同一套主题，每级每个主题 5 个单元；声明顺序即单元编号顺序。 */
enum class SpeechTheme(val key: String, val zh: String, val en: String) {
    Me("me", "我和家人", "Me and My Family"),
    School("school", "学校与朋友", "School and Friends"),
    Food("food", "食物与健康", "Food and Health"),
    Nature("nature", "动物与自然", "Animals and Nature"),
    Seasons("seasons", "季节与天气", "Seasons and Weather"),
    Hobbies("hobbies", "爱好与运动", "Hobbies and Sports"),
    Places("places", "家与旅行", "Home and Travel"),
    City("city", "城市", "My City"),
    Jobs("jobs", "职业", "Jobs"),
    Festivals("festivals", "节日与成长", "Festivals and Growing Up"),
    Algebra("algebra", "代数", "Algebra"),
    Geometry("geometry", "几何", "Geometry"),
    Physics("physics", "物理", "Physics");

    companion object {
        fun from(key: String) = values().first { it.key == key }

        /** 学科主题只在 [StemLevel] 里用，一到三级用其余的生活主题 */
        val Stem = listOf(Algebra, Geometry, Physics)

        fun forLevel(level: Int) = if (level == StemLevel) Stem else values().filter { it !in Stem }
    }
}

/** 课文第 4 级：按学科编排的 STEM 课文 */
const val StemLevel = 4

/** 一个单元：[lines] 是课文（一句一行），[patterns] 是替换句型，[words] 是可选的本课单词（词表原形）。 */
data class Speech(
    val id: String,
    val level: Int,
    val unit: Int,
    val theme: SpeechTheme,
    val title: String,
    val titleZh: String,
    val lines: List<SpeechLine>,
    val patterns: List<SpeechLine>,
    val words: List<String> = emptyList()
)

/** 正文中一个可点击的英文词：[text] 为原文，[range] 为它在段落字符串中的位置。 */
data class SpeechToken(val text: String, val range: IntRange)

// 撇号与连字符连接的部分算同一个词：don't、father's、o'clock、T-shirt、Mid-Autumn
private val TokenPattern = Regex("[A-Za-z]+(?:['-][A-Za-z]+)*")

fun tokenizeSpeech(text: String): List<SpeechToken> =
    TokenPattern.findAll(text).map { SpeechToken(it.value, it.range) }.toList()

/**
 * 缩写的完整写法。[full] 按小写书写，展示时由 [expandContraction] 按原文首字母大小写调整；
 * [parts] 是拆开后的各个词表词，供「拆开看」逐个查词。
 */
data class Contraction(val full: String, val zh: String, val note: String) {
    val parts: List<String> get() = (if (full == "cannot") "can not" else full).split(" ")
}

private const val NoteIs = "'s 是 is 的缩写，撇号 ' 表示省掉了字母 i"
private const val NoteAre = "'re 是 are 的缩写，撇号 ' 表示省掉了字母 a"
private const val NoteNot = "n't 是 not 的缩写，撇号 ' 表示省掉了 not 里的 o"
private const val NoteWill = "'ll 是 will 的缩写，撇号 ' 表示省掉了 wi"
private const val NoteHave = "'ve 是 have 的缩写，撇号 ' 表示省掉了 ha"

private val Contractions = mapOf(
    "i'm" to Contraction("I am", "我是", "'m 是 am 的缩写，撇号 ' 表示省掉了字母 a"),
    "it's" to Contraction("it is", "它是", NoteIs),
    "he's" to Contraction("he is", "他是", NoteIs),
    "she's" to Contraction("she is", "她是", NoteIs),
    "that's" to Contraction("that is", "那是", NoteIs),
    "there's" to Contraction("there is", "有……", NoteIs),
    "what's" to Contraction("what is", "……是什么", NoteIs),
    "where's" to Contraction("where is", "……在哪里", NoteIs),
    "who's" to Contraction("who is", "……是谁", NoteIs),
    "here's" to Contraction("here is", "这是……", NoteIs),
    "let's" to Contraction("let us", "让我们……", "'s 是 us 的缩写，撇号 ' 表示省掉了字母 u"),
    "they're" to Contraction("they are", "他们是", NoteAre),
    "we're" to Contraction("we are", "我们是", NoteAre),
    "you're" to Contraction("you are", "你是；你们是", NoteAre),
    "isn't" to Contraction("is not", "不是", NoteNot),
    "aren't" to Contraction("are not", "不是", NoteNot),
    "wasn't" to Contraction("was not", "（过去）不是", NoteNot),
    "weren't" to Contraction("were not", "（过去）不是", NoteNot),
    "don't" to Contraction("do not", "不（做某事）", NoteNot),
    "doesn't" to Contraction("does not", "不（做某事）", NoteNot),
    "didn't" to Contraction("did not", "没有（做某事）", NoteNot),
    "haven't" to Contraction("have not", "还没有", NoteNot),
    "hasn't" to Contraction("has not", "还没有", NoteNot),
    "couldn't" to Contraction("could not", "不能；没能", NoteNot),
    "shouldn't" to Contraction("should not", "不应该", NoteNot),
    "can't" to Contraction("cannot", "不能；不会", "can't 是 cannot 的缩写，撇号 ' 表示省掉了 no"),
    "won't" to Contraction("will not", "不会；将不", "won't 是 will not 的特殊缩写，要单独记住"),
    "i'll" to Contraction("I will", "我将要", NoteWill),
    "you'll" to Contraction("you will", "你将要", NoteWill),
    "we'll" to Contraction("we will", "我们将要", NoteWill),
    "they'll" to Contraction("they will", "他们将要", NoteWill),
    "i've" to Contraction("I have", "我已经；我有", NoteHave),
    "we've" to Contraction("we have", "我们已经；我们有", NoteHave),
    "you've" to Contraction("you have", "你已经；你有", NoteHave),
    "they've" to Contraction("they have", "他们已经；他们有", NoteHave)
)

fun contractionOf(token: String): Contraction? = Contractions[token.lowercase()]

/** It's → It is；I'm → I am（I 始终大写）。 */
fun expandContraction(token: String, contraction: Contraction): String {
    val full = contraction.full
    return if (token.firstOrNull()?.isUpperCase() == true) full.replaceFirstChar { it.uppercaseChar() } else full
}

/**
 * 只有当 key 本身不在词表里时才会查这张表，所以 thought / left 这类本身就是 850 词的不会被误还原。
 * 值不在词表里时同样视为未命中。
 */
private val IrregularForms = mapOf(
    "is" to "be", "am" to "be", "are" to "be", "was" to "be", "were" to "be", "been" to "be", "being" to "be",
    "has" to "have", "had" to "have",
    "does" to "do", "did" to "do", "done" to "do",
    "came" to "come", "went" to "go", "gone" to "go", "goes" to "go",
    "gave" to "give", "given" to "give", "got" to "get", "gotten" to "get",
    "kept" to "keep", "made" to "make", "took" to "take", "taken" to "take",
    "said" to "say", "saw" to "see", "seen" to "see", "sent" to "send",
    "me" to "i", "my" to "i", "mine" to "i",
    "men" to "man", "women" to "woman", "feet" to "foot", "teeth" to "tooth",
    "better" to "good", "best" to "good", "worse" to "bad", "worst" to "bad",
    "won't" to "will", "can't" to "can"
)

/**
 * 词形还原用的词表视图（全部小写）。按后缀限定原形词类，避免 upper→up、inner→in 这类误还原。
 *
 * @param words 全部词（850 + 拓展词）
 * @param qualities 性质词（qg/qo）：-er/-est/-ly 的原形必须在这里；拓展词不分词类，同时放进两组
 * @param things 名词（gt/pt）：与 [OperatorVerbs] 一起作为 -ing/-ed 的合法原形
 */
class LemmaVocabulary(val words: Set<String>, val qualities: Set<String>, val things: Set<String>) {
    fun canTakeVerbSuffix(base: String) = base in things || base in OperatorVerbs || base in QualityVerbs
}

/**
 * 也常作动词用的性质词（cleaning、opened、liked）。不对全部性质词放开：longing 会被误还原成 long。
 */
private val QualityVerbs = setOf(
    "clean", "open", "close", "dry", "warm", "cool", "empty", "like", "free", "cut", "wet", "clear", "complete", "slow", "shut"
)

/** Ogden 操作词里真正的动词；其余操作词（for/even/up…）不接 -ing/-ed。 */
private val OperatorVerbs = setOf(
    "come", "get", "give", "go", "keep", "let", "make", "put", "seem", "take",
    "be", "do", "have", "say", "see", "send", "may", "will"
)

private enum class BaseKind { Any, Quality, Verb }

/**
 * 把正文里的词还原成词表中的原形。
 *
 * @param token 正文原词，如 "Came"、"boxes"、"don't"
 * @return 命中的小写原形；对不上词表时返回 null
 *
 * Example: lemmatize("stopped", vocab) == "stop"; lemmatize("Is", vocab) == "be"; lemmatize("upper", vocab) == null
 */
fun lemmatize(token: String, vocabulary: LemmaVocabulary): String? {
    val word = token.lowercase()
    if (word in vocabulary.words) return word
    IrregularForms[word]?.let { if (it in vocabulary.words) return it }
    val base = stripContraction(word)
    if (base != word) return lemmatize(base, vocabulary)
    return suffixCandidates(word).firstOrNull { (candidate, kind) ->
        candidate in vocabulary.words && when (kind) {
            BaseKind.Any -> true
            BaseKind.Quality -> candidate in vocabulary.qualities
            BaseKind.Verb -> vocabulary.canTakeVerbSuffix(candidate)
        }
    }?.first
}

private fun stripContraction(word: String): String = when {
    word.endsWith("n't") -> word.dropLast(3)
    word.contains('\'') -> word.substringBefore('\'')
    else -> word
}

/** 按「越具体越优先」排列的候选原形及其词类约束；调用方负责用词表过滤。 */
private fun suffixCandidates(word: String): List<Pair<String, BaseKind>> {
    val candidates = mutableListOf<Pair<String, BaseKind>>()
    fun stem(suffix: String) = word.dropLast(suffix.length).takeIf { word.endsWith(suffix) && it.length >= 2 }
    fun addStem(suffix: String, kind: BaseKind, withE: Boolean = false, doubled: Boolean = false) {
        val s = stem(suffix) ?: return
        candidates += s to kind
        if (withE) candidates += s + "e" to kind
        // stopped → stop：去掉双写的末尾辅音
        if (doubled && s.length >= 3 && s.last() == s[s.length - 2] && s.last() !in "aeiou") {
            candidates += s.dropLast(1) to kind
        }
    }
    listOf("ies" to BaseKind.Any, "ied" to BaseKind.Verb, "ier" to BaseKind.Quality, "iest" to BaseKind.Quality, "ily" to BaseKind.Quality)
        .forEach { (suffix, kind) -> stem(suffix)?.let { candidates += it + "y" to kind } }
    // 先试 -s 再试 -es：toes→toe、shoes→shoe（反过来会剥成 to、sho）；-s 对不上时 boxes、tomatoes 再走 -es。
    // -es 只跟在 s/x/z/ch/sh/o 后面，否则 planes 会被剥成 plan
    addStem("s", BaseKind.Any)
    if (stem("es")?.let { it.endsWith("s") || it.endsWith("x") || it.endsWith("z") || it.endsWith("ch") || it.endsWith("sh") || it.endsWith("o") } == true) {
        addStem("es", BaseKind.Any)
    }
    addStem("ing", BaseKind.Verb, withE = true, doubled = true)
    addStem("ed", BaseKind.Verb, withE = true, doubled = true)
    addStem("est", BaseKind.Quality, withE = true, doubled = true)
    addStem("er", BaseKind.Quality, withE = true, doubled = true)
    addStem("ly", BaseKind.Quality)
    return candidates
}

fun buildSsml(text: String, voiceName: String, lang: String): String {
    val escaped = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
    return "<speak version='1.0' xml:lang='$lang'><voice xml:lang='$lang' name='$voiceName'>" +
        "<prosody rate='-6%'>$escaped</prosody></voice></speak>"
}
