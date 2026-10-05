package com.example.ogdenkids

import kotlin.random.Random

enum class SentenceQuestionType(val title: String) {
    Listen("听句子，选出你听到的"),
    Meaning("看中文，选英文"),
    FillWord("句子填空"),
    Pattern("句型替换"),
    Order("连词成句")
}

/**
 * @param prompt Listen：要朗读的英文；Meaning / Order：中文；FillWord / Pattern：挖空后的英文
 * @param hint FillWord / Pattern 的中文提示，其余为空
 * @param answer Order：词块按正确顺序以空格连接
 * @param options Order：打乱后的词块（可能有重复词）；其余为含答案的选项
 * @param sentence 完整原句，答题后展示与朗读
 */
data class SentenceQuestion(
    val type: SentenceQuestionType,
    val prompt: String,
    val hint: String,
    val answer: String,
    val options: List<String>,
    val sentence: String
)

private val Stopwords = setOf(
    "a", "an", "the", "i", "you", "he", "she", "it", "we", "they", "me", "my", "your", "his", "her", "our", "their",
    "is", "am", "are", "was", "were", "be", "do", "does", "did", "can", "to", "and", "or", "but", "so", "in", "on",
    "at", "of", "for", "with", "this", "that", "too", "very"
)

private fun isContentWord(token: String) = token.length >= 3 && token.all { it.isLetter() } && token.lowercase() !in Stopwords

/**
 * 从同一级同一主题的单元里出一组句子练习，题型按 [SentenceQuestionType] 轮换。
 * 某题型凑不出合格题（如句型没有替换位）时跳过，所以返回数量可能少于 [count]。
 */
fun buildThemePractice(units: List<Speech>, random: Random, count: Int = 10): List<SentenceQuestion> {
    val lines = units.flatMap { it.lines }.distinctBy { it.en }
    val used = mutableSetOf<String>()
    val questions = mutableListOf<SentenceQuestion>()
    val types = SentenceQuestionType.values()
    var attempt = 0
    while (questions.size < count && attempt < count * 3) {
        val type = types[attempt % types.size]
        attempt++
        val question = when (type) {
            SentenceQuestionType.Listen, SentenceQuestionType.Meaning -> chooseSentence(type, lines, used, random)
            SentenceQuestionType.FillWord -> fillWord(lines, used, random)
            SentenceQuestionType.Pattern -> pattern(units, used, random)
            SentenceQuestionType.Order -> order(lines, used, random)
        } ?: continue
        used += question.sentence
        questions += question
    }
    return questions
}

private fun chooseSentence(type: SentenceQuestionType, lines: List<SpeechLine>, used: Set<String>, random: Random): SentenceQuestion? {
    val line = lines.filter { it.en !in used }.randomOrNull(random) ?: return null
    val distractors = lines.filter { it.en != line.en }.shuffled(random).take(3).map { it.en }
    if (distractors.size < 3) return null
    return SentenceQuestion(
        type = type,
        prompt = if (type == SentenceQuestionType.Listen) line.en else line.zh,
        hint = "",
        answer = line.en,
        options = (distractors + line.en).shuffled(random),
        sentence = line.en
    )
}

private fun fillWord(lines: List<SpeechLine>, used: Set<String>, random: Random): SentenceQuestion? {
    val pool = lines.flatMap { line -> tokenizeSpeech(line.en).drop(1).filter { isContentWord(it.text) }.map { line to it } }
    val candidates = pool.filter { (line, _) -> line.en !in used }.shuffled(random)
    for ((line, token) in candidates) {
        val distractors = pool.map { it.second.text.lowercase() }
            .filter { it != token.text.lowercase() }
            .distinct()
            .shuffled(random)
            .take(3)
        if (distractors.size < 3) continue
        return SentenceQuestion(
            type = SentenceQuestionType.FillWord,
            prompt = line.en.replaceRange(token.range, "____"),
            hint = line.zh,
            answer = token.text,
            options = (distractors + token.text).shuffled(random),
            sentence = line.en
        )
    }
    return null
}

/** 句型的替换位 = 本句中、同单元其他句型都没有的第一个词（跳过句首）。 */
private fun pattern(units: List<Speech>, used: Set<String>, random: Random): SentenceQuestion? {
    for (unit in units.shuffled(random)) {
        val tokenSets = unit.patterns.map { p -> tokenizeSpeech(p.en).map { it.text.lowercase() }.toSet() }
        fun slotOf(index: Int): SpeechToken? {
            val others = tokenSets.filterIndexed { i, _ -> i != index }.flatten().toSet()
            return tokenizeSpeech(unit.patterns[index].en).drop(1).firstOrNull { it.text.lowercase() !in others }
        }
        for (index in unit.patterns.indices.shuffled(random)) {
            val line = unit.patterns[index]
            if (line.en in used) continue
            val slot = slotOf(index) ?: continue
            val distractors = unit.patterns.indices
                .filter { it != index }
                .mapNotNull { slotOf(it)?.text }
                .filter { !it.equals(slot.text, ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .take(3)
            if (distractors.size < 2) continue
            return SentenceQuestion(
                type = SentenceQuestionType.Pattern,
                prompt = line.en.replaceRange(slot.range, "____"),
                hint = line.zh,
                answer = slot.text,
                options = (distractors + slot.text).shuffled(random),
                sentence = line.en
            )
        }
    }
    return null
}

private fun order(lines: List<SpeechLine>, used: Set<String>, random: Random): SentenceQuestion? {
    val line = lines.filter { it.en !in used && orderChunks(it.en).size in 3..8 }.randomOrNull(random) ?: return null
    val chunks = orderChunks(line.en)
    var shuffled = chunks.shuffled(random)
    // 词块全相同时无法打乱；否则保证不是原顺序
    repeat(5) { if (shuffled == chunks) shuffled = chunks.shuffled(random) }
    return SentenceQuestion(
        type = SentenceQuestionType.Order,
        prompt = line.zh,
        hint = "",
        answer = chunks.joinToString(" "),
        options = shuffled,
        sentence = line.en
    )
}

/** 连词成句的词块：去掉标点、保留大小写（句首小写化会把 Tom 这类专名教错）。 */
fun orderChunks(sentence: String): List<String> = tokenizeSpeech(sentence).map { it.text }
