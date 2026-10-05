package com.example.ogdenkids

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SpecialTrainingTest {
    private val vocabulary: LemmaVocabulary = run {
        val words = JSONArray(File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('﻿'))
        val byCategory = List(words.length()) { words.getJSONObject(it) }
            .groupBy({ it.getString("c") }, { it.getString("w").lowercase() })
        fun keysOf(vararg codes: String) = codes.flatMap { byCategory[it].orEmpty() }.toSet()
        LemmaVocabulary(
            words = keysOf("op", "gt", "pt", "qg", "qo", "ex", "fm"),
            qualities = keysOf("qg", "qo", "ex"),
            things = keysOf("gt", "pt", "ex")
        )
    }

    @Test
    fun everyTopicAndLevelYieldsTenValidQuestions() {
        for (topic in SpecialTopic.values()) for (level in 1..3) repeat(20) { seed ->
            val where = "${topic.key} L$level seed=$seed"
            val questions = buildSpecialPractice(topic, level, Random(seed))
            assertEquals(where, 10, questions.size)
            assertEquals("$where duplicate", questions.size, questions.map { it.sentence }.toSet().size)
            questions.forEach { q ->
                assertTrue("$where ${q.kind} answer index", q.answer in q.options.indices)
                assertEquals("$where ${q.kind} options", 4, q.options.size)
                assertEquals("$where ${q.kind} duplicate options", q.options.size, q.options.toSet().size)
                if (q.prompt.contains("____")) assertTrue("$where ${q.kind} blank", q.options[q.answer].text != null)
            }
        }
    }

    @Test
    fun positionOptionsNeverLookAlike() {
        val lookAlike = listOf(
            setOf("next to", "beside", "near", "on the right of", "on the left of", "outside"),
            setOf("under", "below"),
            setOf("in", "inside")
        )
        for (level in 1..3) repeat(50) { seed ->
            buildSpecialPractice(SpecialTopic.Position, level, Random(seed))
                .filter { it.kind == SpecialKind.LookChoose || it.kind == SpecialKind.FillBlank }
                .forEach { q ->
                    val phrases = q.options.mapNotNull { it.text }
                    lookAlike.forEach { group -> assertTrue("$phrases", phrases.count { it in group } <= 1) }
                }
        }
    }

    @Test
    fun allEnglishResolvesToWordList() {
        for (topic in SpecialTopic.values()) for (level in 1..3) repeat(20) { seed ->
            buildSpecialPractice(topic, level, Random(seed)).forEach { q ->
                val english = listOfNotNull(q.sentence.takeIf { "=" !in it }, q.speak) + q.options.mapNotNull { it.text }
                english.flatMap { tokenizeSpeech(it) }.map { it.text }.forEach { token ->
                    // 数字里的连字符词 twenty-one 拆开查
                    token.split("-").forEach { part ->
                        assertNotNull("${topic.key} L$level: $part in \"$english\"", contractionOf(part) ?: lemmatize(part, vocabulary))
                    }
                }
            }
        }
    }

    private val modules: List<SpecialModule> = run {
        val array = JSONArray(File("src/main/assets/specials.json").readText(Charsets.UTF_8))
        List(array.length()) { i ->
            val m = array.getJSONObject(i)
            val groups = m.getJSONArray("groups")
            val sentences = m.getJSONArray("sentences")
            SpecialModule(
                key = m.getString("key"), zh = m.getString("zh"), en = m.getString("en"), icon = m.getString("icon"),
                scene = m.optString("scene").takeIf { it.isNotBlank() && it != "null" }?.let { key -> SpecialTopic.values().first { it.key == key } },
                groups = List(groups.length()) { g ->
                    val group = groups.getJSONObject(g)
                    val words = group.getJSONArray("words")
                    WordGroup(group.getString("zh"), group.getString("en"), List(words.length()) { words.getString(it) })
                },
                sentences = List(sentences.length()) { SpeechLine(sentences.getJSONObject(it).getString("en"), sentences.getJSONObject(it).getString("zh")) }
            )
        }
    }

    @Test
    fun modulesUseListedWordsAndYieldSentencePractice() {
        assertEquals(16, modules.size)
        assertEquals(SpecialTopic.values().toSet(), modules.mapNotNull { it.scene }.toSet())
        modules.forEach { module ->
            module.words.forEach { assertTrue("${module.key}: $it", it.lowercase() in vocabulary.words) }
            module.sentences.flatMap { tokenizeSpeech(it.en) }.forEach { token ->
                assertNotNull("${module.key}: ${token.text}", contractionOf(token.text) ?: lemmatize(token.text, vocabulary))
            }
            repeat(10) { seed ->
                val questions = buildThemePractice(listOf(module.asPracticeUnit()), Random(seed), vocabulary = emptyList())
                assertEquals("${module.key} seed=$seed", 10, questions.size)
            }
        }
    }

    @Test
    fun numberWordsAreSpelledCorrectly() {
        assertEquals("seven", numberWord(7))
        assertEquals("fifteen", numberWord(15))
        assertEquals("forty", numberWord(40))
        assertEquals("twenty-one", numberWord(21))
        assertEquals("one hundred", numberWord(100))
    }
}
