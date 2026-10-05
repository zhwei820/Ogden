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
            words = keysOf("op", "gt", "pt", "qg", "qo"),
            qualities = keysOf("qg", "qo"),
            things = keysOf("gt", "pt")
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
            val paragraphs = item.getJSONArray("paragraphs")
            assertTrue(paragraphs.length() > 0)
            repeat(paragraphs.length()) { p ->
                val paragraph = paragraphs.getJSONObject(p)
                assertFalse(paragraph.optString("en").isBlank())
                assertFalse(paragraph.optString("zh").isBlank())
            }
        }
    }

    @Test
    fun lemmatizeRestoresInflectedForms() {
        mapOf(
            "came" to "come", "Went" to "go", "boxes" to "box", "stories" to "story",
            "stopped" to "stop", "going" to "go", "making" to "make", "used" to "use",
            "happier" to "happy", "longer" to "long", "slowly" to "slow", "later" to "late",
            "Is" to "be", "father's" to "father", "I'm" to "i", "don't" to "do", "words" to "word"
        ).forEach { (token, expected) -> assertEquals(token, expected, lemmatize(token, vocabulary)) }
    }

    @Test
    fun lemmatizeRejectsFalseStems() {
        // 不加词类约束时 evening→even、forest→for 会被误还原
        listOf("evening", "forest", "afraid", "newspaper", "zzz").forEach {
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
    }

    @Test
    fun ssmlEscapesSpecialCharacters() {
        val ssml = buildSsml("Tom & Jerry's <box> \"go\"", "en-US-JennyNeural", "en-US")
        assertTrue(ssml.contains("Tom &amp; Jerry&apos;s &lt;box&gt; &quot;go&quot;"))
        assertTrue(ssml.contains("name='en-US-JennyNeural'"))
        assertTrue(ssml.contains("xml:lang='en-US'"))
    }
}
