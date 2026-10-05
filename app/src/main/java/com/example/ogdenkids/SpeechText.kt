package com.example.ogdenkids

data class SpeechLine(val en: String, val zh: String)

/** 一个单元：[lines] 是 Track1 示范演讲（一句一行），[patterns] 是 Track2 替换句型。 */
data class Speech(
    val id: String,
    val level: Int,
    val unit: Int,
    val title: String,
    val titleZh: String,
    val lines: List<SpeechLine>,
    val patterns: List<SpeechLine>
)

/** 正文中一个可点击的英文词：[text] 为原文，[range] 为它在段落字符串中的位置。 */
data class SpeechToken(val text: String, val range: IntRange)

private val TokenPattern = Regex("[A-Za-z]+(?:'[A-Za-z]+)?")

fun tokenizeSpeech(text: String): List<SpeechToken> =
    TokenPattern.findAll(text).map { SpeechToken(it.value, it.range) }.toList()

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
 * 词形还原用的词表视图（全部小写）。按后缀限定原形词类，避免 evening→even、forest→for 这类误还原。
 *
 * @param words 全部词（850 + 拓展词）
 * @param qualities 性质词（qg/qo）：-er/-est/-ly 的原形必须在这里；拓展词不分词类，同时放进两组
 * @param things 名词（gt/pt）：与 [OperatorVerbs] 一起作为 -ing/-ed 的合法原形
 */
class LemmaVocabulary(val words: Set<String>, val qualities: Set<String>, val things: Set<String>) {
    fun canTakeVerbSuffix(base: String) = base in things || base in OperatorVerbs
}

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
 * Example: lemmatize("stopped", vocab) == "stop"; lemmatize("Is", vocab) == "be"; lemmatize("evening", vocab) == null
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
    addStem("es", BaseKind.Any)
    addStem("s", BaseKind.Any)
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
