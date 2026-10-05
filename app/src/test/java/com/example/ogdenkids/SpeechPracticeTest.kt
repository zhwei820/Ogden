package com.example.ogdenkids

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class SpeechPracticeTest {
    private val speeches: List<Speech> = run {
        val array = JSONArray(File("src/main/assets/speeches.json").readText(Charsets.UTF_8).trimStart('﻿'))
        fun lines(a: JSONArray) = List(a.length()) { SpeechLine(a.getJSONObject(it).getString("en"), a.getJSONObject(it).getString("zh")) }
        List(array.length()) {
            val item = array.getJSONObject(it)
            Speech(
                id = item.getString("id"),
                level = item.getInt("level"),
                unit = item.getInt("unit"),
                theme = SpeechTheme.from(item.getString("theme")),
                title = item.getString("title"),
                titleZh = item.getString("titleZh"),
                lines = lines(item.getJSONArray("lines")),
                patterns = lines(item.getJSONArray("patterns"))
            )
        }
    }

    @Test
    fun everyThemeYieldsTenValidQuestions() {
        for (level in 1..3) for (theme in SpeechTheme.values()) {
            val units = speeches.filter { it.level == level && it.theme == theme }
            repeat(20) { seed ->
                val questions = buildThemePractice(units, Random(seed))
                val where = "L$level ${theme.key} seed=$seed"
                assertEquals(where, 10, questions.size)
                assertEquals(where, SentenceQuestionType.values().toSet(), questions.map { it.type }.toSet())
                assertEquals("$where duplicate sentence", questions.size, questions.map { it.sentence }.toSet().size)
                questions.forEach { q ->
                    if (q.type == SentenceQuestionType.Order) {
                        assertEquals(where, orderChunks(q.sentence).joinToString(" "), q.answer)
                        assertEquals(where, q.answer.split(" ").sorted(), q.options.sorted())
                    } else {
                        assertTrue("$where ${q.type} answer missing", q.answer in q.options)
                        assertEquals("$where ${q.type} duplicate options", q.options.size, q.options.toSet().size)
                        assertTrue("$where ${q.type} too few options", q.options.size >= 3)
                    }
                    if (q.type == SentenceQuestionType.FillWord || q.type == SentenceQuestionType.Pattern) {
                        assertEquals(where, q.sentence, q.prompt.replace("____", q.answer))
                    }
                }
            }
        }
    }
}
