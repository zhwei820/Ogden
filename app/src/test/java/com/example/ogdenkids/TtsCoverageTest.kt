package com.example.ogdenkids

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class TtsCoverageTest {
    private fun json(name: String) = JSONArray(File("src/main/assets/$name").readText(Charsets.UTF_8).trimStart('﻿'))
    private fun strings(a: JSONArray?) = if (a == null) emptyList() else List(a.length()) { a.getString(it) }
    private fun lines(a: JSONArray) = List(a.length()) { SpeechLine(a.getJSONObject(it).getString("en"), a.getJSONObject(it).getString("zh")) }

    private val words = json("ogden_words.json").let { a ->
        List(a.length()) { a.getJSONObject(it) }.map {
            OgdenWord(it.getString("w"), Category.from(it.getString("c")), it.getString("zh"), it.getString("en"),
                it.getString("ex"), it.getString("exz"), strings(it.getJSONArray("s")), "", "")
        }
    }
    private val speeches = json("speeches.json").let { a ->
        List(a.length()) { a.getJSONObject(it) }.map {
            Speech(it.getString("id"), it.getInt("level"), it.getInt("unit"), SpeechTheme.from(it.getString("theme")),
                it.getString("title"), it.getString("titleZh"), lines(it.getJSONArray("lines")), lines(it.getJSONArray("patterns")),
                strings(it.optJSONArray("words")))
        }
    }
    private val modules = json("specials.json").let { a ->
        List(a.length()) { a.getJSONObject(it) }.map { m ->
            val groups = m.getJSONArray("groups")
            SpecialModule(m.getString("key"), m.getString("zh"), m.getString("en"), m.getString("icon"), null,
                List(groups.length()) { g -> groups.getJSONObject(g).let { WordGroup(it.getString("zh"), it.getString("en"), strings(it.getJSONArray("words"))) } },
                lines(m.getJSONArray("sentences")))
        }
    }
    private val texts = speakableTexts(words, speeches, modules)

    /** 供 scripts/gen_tts_assets.py 读取，生成缺失的音频。 */
    @Test
    fun writeManifest() {
        val out = File("build/tts-manifest.json")
        out.parentFile.mkdirs()
        out.writeText(JSONObject().put("en", JSONArray(texts.english.sorted())).put("zh", JSONArray(texts.chinese.sorted())).toString(1))
    }

    @Test
    fun specialPracticeOnlySpeaksEnumeratedTexts() {
        val (en, zh) = allSpecialUtterances()
        for (topic in SpecialTopic.values()) for (level in 1..3) repeat(200) { seed ->
            buildSpecialPractice(topic, level, Random(seed)).forEach { q ->
                listOfNotNull(q.speak, q.spoken, q.question).forEach { assertTrue("${topic.key} L$level: $it", it in en) }
                assertTrue("${topic.key} L$level: ${q.sentenceZh}", q.sentenceZh in zh)
            }
        }
    }

    @Test
    fun everySpeakableTextHasBundledAudio() {
        val missing = texts.english.flatMap { t -> listOf(AzureVoice.EnUs, AzureVoice.EnGb).map { it to t } } +
            texts.chinese.map { AzureVoice.ZhCn to it }
        val absent = missing.filter { (voice, text) -> !File("src/main/assets/${ttsAssetPath(voice, text)}").exists() }
        assertTrue("${absent.size} 条缺少预生成音频，运行 scripts/gen_tts_assets.py；例如 ${absent.take(5)}", absent.isEmpty())
    }
}
