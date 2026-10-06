package com.example.ogdenkids

import kotlin.random.Random

/** 声明顺序即出题轮换顺序，单词题穿插在句子题之间。 */
enum class SentenceQuestionType(val title: String, val titleEn: String) {
    Listen("听句子，选出你听到的", "Listen and Choose"),
    WordListen("听单词，选出你听到的", "Listen and Choose"),
    Meaning("看中文，选英文", "Read and Choose"),
    FillWord("句子填空", "Fill in the Blank"),
    WordMeaning("看中文，选单词", "Read and Choose"),
    Pattern("句型替换", "Change the Word"),
    Contraction("缩写配对", "Short Forms"),
    Order("连词成句", "Put the Words in Order")
}

/**
 * @param prompt Listen / WordListen：要朗读的英文；Meaning / WordMeaning / Order：中文；FillWord / Pattern：挖空后的英文；
 *               Contraction：「I'm = ?」
 * @param hint FillWord / Pattern / 单词题的中文；Contraction 为缩写所在原句及中文；其余为空
 * @param answer Order：词块按正确顺序以空格连接
 * @param options Order：打乱后的词块（可能有重复词）；其余为含答案的选项
 * @param sentence 完整原句（单词题为单词本身，缩写题为「I'm = I am」），答题后展示；同组内唯一
 * @param zh [sentence] 的中文，答题后展示
 */
data class SentenceQuestion(
    val type: SentenceQuestionType,
    val prompt: String,
    val hint: String,
    val answer: String,
    val options: List<String>,
    val sentence: String,
    val zh: String
)

private val Stopwords = setOf(
    "a", "an", "the", "i", "you", "he", "she", "it", "we", "they", "me", "my", "your", "his", "her", "our", "their",
    "is", "am", "are", "was", "were", "be", "do", "does", "did", "can", "to", "and", "or", "but", "so", "in", "on",
    "at", "of", "for", "with", "this", "that", "too", "very"
)

private fun isContentWord(token: String) = token.length >= 3 && token.all { it.isLetter() } && token.lowercase() !in Stopwords

/**
 * 从同一级同一主题的单元里出一组练习，题型按 [SentenceQuestionType] 轮换。
 * 某题型凑不出合格题（如句型没有替换位、没有本课单词）时跳过，所以返回数量可能少于 [count]。
 *
 * @param vocabulary 这些单元「本课单词」对应的词条，用于单词题
 */
fun buildThemePractice(
    units: List<Speech>,
    random: Random,
    count: Int = 10,
    vocabulary: List<OgdenWord> = emptyList()
): List<SentenceQuestion> {
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
            SentenceQuestionType.WordListen, SentenceQuestionType.WordMeaning -> word(type, vocabulary, used, random)
            SentenceQuestionType.Contraction -> contraction(units, used, random)
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
        sentence = line.en,
        zh = line.zh
    )
}

private fun word(type: SentenceQuestionType, vocabulary: List<OgdenWord>, used: Set<String>, random: Random): SentenceQuestion? {
    val words = vocabulary.distinctBy { it.word.lowercase() }
    val target = words.filter { it.word !in used }.randomOrNull(random) ?: return null
    val distractors = words.filter { it.word != target.word }.shuffled(random).take(3).map { it.word }
    if (distractors.size < 3) return null
    return SentenceQuestion(
        type = type,
        prompt = if (type == SentenceQuestionType.WordListen) target.word else target.zh,
        hint = target.zh,
        answer = target.word,
        options = (distractors + target.word).shuffled(random),
        sentence = target.word,
        zh = target.zh
    )
}

private val BeAndAux = listOf("am", "is", "are", "will", "have")
private val Negatives = listOf("is not", "are not", "do not", "does not", "did not", "cannot", "will not", "have not", "was not")

/** 干扰项：be/助动词类换助动词（I am → I is / I are），否定类换别的否定（is not → do not），let's 给形近写法。 */
private fun contractionDistractors(full: String, random: Random): List<String> {
    val words = full.split(" ")
    val pool = when {
        full == "cannot" || words.last() == "not" -> Negatives
        words.size == 2 && words[1] in BeAndAux -> BeAndAux.map { "${words[0]} $it" }
        else -> listOf("${words[0]} is", "${words[0]} it", "${words[0]} as")
    }
    return pool.filter { !it.equals(full, ignoreCase = true) }.shuffled(random).take(3)
}

private fun contraction(units: List<Speech>, used: Set<String>, random: Random): SentenceQuestion? {
    // 不按 used 过滤原句：缩写往往只出现在一两句里，被别的题型先用掉就出不了题；以「I'm = I am」去重
    val candidates = units.flatMap { it.lines + it.patterns }
        .distinctBy { it.en }
        .flatMap { line -> tokenizeSpeech(line.en).mapNotNull { t -> contractionOf(t.text)?.let { Triple(line, t.text, it) } } }
        .shuffled(random)
    val (line, token, full) = candidates
        .map { (line, token, contraction) -> Triple(line, token, expandContraction(token, contraction)) }
        .firstOrNull { (_, token, full) -> "$token = $full" !in used } ?: return null
    val distractors = contractionDistractors(full, random)
        .map { if (full.first().isUpperCase()) it.replaceFirstChar { c -> c.uppercaseChar() } else it }
    return SentenceQuestion(
        type = SentenceQuestionType.Contraction,
        prompt = "$token = ?",
        hint = "${line.en}\n${line.zh}",
        answer = full,
        options = (distractors + full).distinct().shuffled(random),
        sentence = "$token = $full",
        zh = contractionOf(token)?.zh ?: line.zh
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
            sentence = line.en,
            zh = line.zh
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
                sentence = line.en,
                zh = line.zh
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
        sentence = line.en,
        zh = line.zh
    )
}

/** 连词成句的词块：去掉标点、保留大小写（句首小写化会把 Tom 这类专名教错）。 */
fun orderChunks(sentence: String): List<String> = tokenizeSpeech(sentence).map { it.text }
