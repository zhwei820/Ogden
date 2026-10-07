package com.example.ogdenkids

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SpeechTest {
    private val vocabulary: LemmaVocabulary = run {
        val words = JSONArray(File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('﻿'))
        val byCategory = List(words.length()) { words.getJSONObject(it) }
            .groupBy({ it.getString("c") }, { it.getString("w").lowercase() })
        fun keysOf(vararg codes: String) = codes.flatMap { byCategory[it].orEmpty() }.toSet()
        LemmaVocabulary(
            words = keysOf("op", "gt", "pt", "qg", "qo", "ex", "fm", "kd", "ky"),
            qualities = keysOf("qg", "qo", "ex", "kd", "ky"),
            things = keysOf("gt", "pt", "ex", "kd", "ky")
        )
    }

    @Test
    fun speechesHaveRequiredFields() {
        val speeches = JSONArray(File("src/main/assets/speeches.json").readText(Charsets.UTF_8).trimStart('﻿'))
        assertTrue(speeches.length() > 0)
        val ids = mutableSetOf<String>()
        repeat(speeches.length()) { index ->
            val item = speeches.getJSONObject(index)
            listOf("id", "title", "titleZh").forEach { assertFalse("Missing $it at $index", item.optString(it).isBlank()) }
            assertTrue("Duplicate id at $index", ids.add(item.getString("id")))
            assertTrue("Bad level at $index", item.getInt("level") in 1..StemLevel)
            listOf("lines", "patterns").forEach { key ->
                val lines = item.getJSONArray(key)
                assertTrue("Empty $key at $index", lines.length() > 0)
                repeat(lines.length()) { p ->
                    val line = lines.getJSONObject(p)
                    assertFalse(line.optString("en").isBlank())
                    assertFalse(line.optString("zh").isBlank())
                }
            }
        }
        val headwords = JSONArray(File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('\uFEFF'))
            .let { a -> List(a.length()) { a.getJSONObject(it).getString("w").lowercase() }.toSet() }
        repeat(speeches.length()) { index ->
            val unitWords = speeches.getJSONObject(index).optJSONArray("words") ?: return@repeat
            repeat(unitWords.length()) { assertTrue("Unit word not in list: ${unitWords.getString(it)}", unitWords.getString(it).lowercase() in headwords) }
        }
        // 单元按 SpeechTheme 声明顺序连续编号，每个主题 5 个
        (1..StemLevel).forEach { level ->
            val expectedThemes = SpeechTheme.forLevel(level).flatMap { theme -> List(5) { theme.key } }
            val units = List(speeches.length()) { speeches.getJSONObject(it) }
                .filter { it.getInt("level") == level }
                .sortedBy { it.getInt("unit") }
            assertEquals("Level $level units", (1..expectedThemes.size).toList(), units.map { it.getInt("unit") })
            assertEquals("Level $level themes", expectedThemes, units.map { it.getString("theme") })
        }
    }

    @Test
    fun lemmatizeRestoresInflectedForms() {
        mapOf(
            // came/went/is/my 等已有变形词词条，直接命中自身；sent 仍靠 IrregularForms 还原
            "came" to "came", "Went" to "went", "sent" to "send", "boxes" to "box", "planes" to "plane", "cleaning" to "cleaning", "dried" to "dry", "warmed" to "warm", "opened" to "open", "liked" to "like", "toes" to "toe", "shoes" to "shoe", "tomatoes" to "tomato",
            "my" to "my", "children" to "children", "stories" to "story", "T-shirt" to "t-shirt", "o'clock" to "o'clock",
            "stopped" to "stop", "going" to "go", "making" to "make", "used" to "use",
            "happier" to "happy", "longer" to "long", "slowly" to "slow", "later" to "late",
            "Is" to "is", "father's" to "father", "I'm" to "i", "don't" to "do", "words" to "word"
        ).forEach { (token, expected) -> assertEquals(token, expected, lemmatize(token, vocabulary)) }
    }

    @Test
    fun lemmatizeRejectsFalseStems() {
        // 不加词类约束时 upper→up、inner→in 会被误还原（evening/forest 已是拓展词，不能再作反例）
        listOf("upper", "inner", "newspaper", "zzz", "longing").forEach {
            assertNull(it, lemmatize(it, vocabulary))
        }
        // 本身就是 850 词的不应被剥后缀
        assertEquals("thought", lemmatize("thought", vocabulary))
        assertEquals("early", lemmatize("early", vocabulary))
    }

    @Test
    fun tokenizeKeepsContractionsAndRanges() {
        val text = "I'm here, father's book."
        val tokens = tokenizeSpeech(text)
        assertEquals(listOf("I'm", "here", "father's", "book"), tokens.map { it.text })
        tokens.forEach { assertEquals(it.text, text.substring(it.range)) }
        assertEquals(
            listOf("My", "T-shirt", "at", "seven", "o'clock", "Mid-Autumn", "grown-up"),
            tokenizeSpeech("My T-shirt, at seven o'clock - Mid-Autumn grown-up.").map { it.text }
        )
    }

    @Test
    fun contractionsExpandAndResolve() {
        assertEquals("It is", expandContraction("It's", contractionOf("It's")!!))
        assertEquals("I am", expandContraction("I'm", contractionOf("i'm")!!))
        assertEquals("let us", expandContraction("let's", contractionOf("let's")!!))
        assertEquals(listOf("can", "not"), contractionOf("can't")!!.parts)
        assertNull(contractionOf("father's"))
        // 课文里出现的缩写都要在表里，且拆开后的每个词都能在词表里查到
        val speeches = JSONArray(File("src/main/assets/speeches.json").readText(Charsets.UTF_8))
        val tokens = (0 until speeches.length()).flatMap { i ->
            val unit = speeches.getJSONObject(i)
            listOf("lines", "patterns").flatMap { key ->
                val lines = unit.getJSONArray(key)
                (0 until lines.length()).flatMap { tokenizeSpeech(lines.getJSONObject(it).getString("en")) }
            }
        }.map { it.text }.filter { "'" in it && !it.endsWith("'s", ignoreCase = true) && !it.equals("o'clock", ignoreCase = true) }
        tokens.forEach { assertTrue("Missing contraction $it", contractionOf(it) != null) }
        tokens.mapNotNull { contractionOf(it) }.flatMap { it.parts }.forEach {
            assertTrue("Part not in list: $it", lemmatize(it, vocabulary) != null)
        }
    }

    @Test
    fun ssmlEscapesSpecialCharacters() {
        val ssml = buildSsml("Tom & Jerry's <box> \"go\"", "en-US-JennyNeural", "en-US")
        assertTrue(ssml.contains("Tom &amp; Jerry&apos;s &lt;box&gt; &quot;go&quot;"))
        assertTrue(ssml.contains("name='en-US-JennyNeural'"))
        assertTrue(ssml.contains("xml:lang='en-US'"))
    }

    @Test
    fun packLessonPlacementMustMatchLevelThemes() {
        assertTrue(isValidSpeechPlacement(1, "nature"))
        assertTrue(isValidSpeechPlacement(StemLevel, "physics"))
        assertFalse(isValidSpeechPlacement(1, "physics"))
        assertFalse(isValidSpeechPlacement(StemLevel, "nature"))
        assertFalse(isValidSpeechPlacement(5, "nature"))
        assertFalse(isValidSpeechPlacement(1, "ocean"))
    }

    @Test
    fun mockLessonPackIsValidAndDoesNotReuseBuiltInIds() {
        val builtIn = JSONArray(File("src/main/assets/speeches.json").readText().trimStart('\uFEFF'))
        val builtInIds = List(builtIn.length()) { builtIn.getJSONObject(it).getString("id") }.toSet()
        val pack = JSONArray(File("../scripts/mock_packs/lessons-sea-animals/speeches.json").readText())
        for (i in 0 until pack.length()) {
            val item = pack.getJSONObject(i)
            assertFalse(item.getString("id") in builtInIds)
            assertTrue(isValidSpeechPlacement(item.getInt("level"), item.getString("theme")))
        }
    }
}
