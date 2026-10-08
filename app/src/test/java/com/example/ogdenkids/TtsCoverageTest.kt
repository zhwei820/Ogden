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

    private val antonyms = JSONObject(File("src/main/assets/antonyms.json").readText(Charsets.UTF_8))
    private val collocations = JSONObject(File("src/main/assets/collocations.json").readText(Charsets.UTF_8))
    private fun collocationsOf(word: String) = collocations.optJSONArray(word)?.let { a ->
        List(a.length()) { a.getJSONObject(it).let { c -> Collocation(c.getString("phrase"), c.getString("zh"), c.getString("ex"), c.getString("exz")) } }
    }.orEmpty()
    private val relatedJson = JSONObject(File("src/main/assets/related_words.json").readText(Charsets.UTF_8))
    private val dropped = relatedJson.keys().asSequence().filter { relatedJson.getJSONObject(it).optBoolean("drop") }.toSet()
    private val related = relatedJson.keys().asSequence().filter { it !in dropped }.map {
        val r = relatedJson.getJSONObject(it)
        RelatedWord(r.getString("zh"), r.getString("ex"), r.getString("exz"))
    }.toList()
    private val words = json("ogden_words.json").let { a ->
        List(a.length()) { a.getJSONObject(it) }.map {
            OgdenWord(it.getString("w"), Category.from(it.getString("c")), it.getString("zh"), it.getString("en"),
                it.getString("ex"), it.getString("exz"), strings(it.getJSONArray("s")).filter { s -> s !in dropped }, "", "",
                strings(antonyms.optJSONArray(it.getString("w"))), collocationsOf(it.getString("w")),
                it.optString("ex2"), it.optString("exz2"))
        }
    }

    @Test
    fun collocationsReferToHeadwords() {
        val headwords = words.map { it.word }.toSet()
        collocations.keys().forEach { key ->
            assertTrue("collocations.json key not a headword: $key", key in headwords)
            assertTrue("$key collocations", collocationsOf(key).isNotEmpty())
        }
    }

    @Test
    fun antonymsReferToHeadwords() {
        val headwords = words.map { it.word }.toSet()
        antonyms.keys().forEach { key ->
            assertTrue("antonyms.json key not a headword: $key", key in headwords)
            val list = strings(antonyms.getJSONArray(key))
            assertTrue("$key antonyms", list.isNotEmpty() && list.none { it.equals(key, ignoreCase = true) })
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
    private val texts = speakableTexts(words, speeches, modules, related)

    @Test
    fun lessonAndSpecialWordsHaveCollocations() {
        val byLower = words.associateBy { it.word.lowercase() }
        val missing = (speeches.flatMap { it.words } + modules.flatMap { it.words })
            .map { byLower[it.lowercase()]?.word ?: it }
            .distinct()
            .filter { collocationsOf(it).isEmpty() }
        assertTrue("缺常见搭配：$missing", missing.isEmpty())
    }

    /** 供 scripts/gen_tts_assets.py 读取，生成缺失的音频。 */
    @Test
    fun writeManifest() {
        val out = File("build/tts-manifest.json")
        out.parentFile.mkdirs()
        out.writeText(
            JSONObject()
                .put("en", JSONArray((texts.english + mathDrillClips().first).sorted()))
                .put("en_us", JSONArray(texts.englishUsOnly.sorted()))
                .put("zh", JSONArray(mathDrillClips().second.sorted()))
                .toString(1)
        )
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
        // 只预生成英文；中文播放时在线合成
        val missing = texts.english.flatMap { t -> listOf(AzureVoice.EnUs, AzureVoice.EnGb).map { it to t } } +
            texts.englishUsOnly.map { AzureVoice.EnUs to it }
        val absent = missing.filter { (voice, text) -> !File("src/main/assets/${ttsAssetPath(voice, text)}").exists() }
        assertTrue("${absent.size} 条缺少预生成音频，运行 scripts/gen_tts_assets.py；例如 ${absent.take(5)}", absent.isEmpty())
    }

    @Test
    fun mathDrillClipsHaveBundledAudio() {
        val (en, zh) = mathDrillClips()
        val absent = en.flatMap { listOf(AzureVoice.EnUs to it, AzureVoice.EnGb to it) } + zh.map { AzureVoice.ZhCn to it }
        val missing = absent.filterNot { (voice, text) -> File("src/main/assets/${ttsAssetPath(voice, text)}").exists() }
        assertTrue("口算缺离线录音，运行 scripts/gen_tts_assets.py：$missing", missing.isEmpty())
    }
}
