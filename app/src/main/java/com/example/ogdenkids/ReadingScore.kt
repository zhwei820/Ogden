package com.example.ogdenkids

import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

/**
 * 发音词典：把 ogden_ipa.json 的美音 / 英音音标按词收起来，用来认同音词。
 * 识别引擎听到 their 可能写成 there，两者音标相同就算读对。
 */
class Homophones(private val ipa: Map<String, Set<String>>) {
    fun same(a: String, b: String): Boolean {
        val x = ipa[a] ?: return false
        val y = ipa[b] ?: return false
        return x.any { it in y }
    }

    companion object {
        val Empty = Homophones(emptyMap())

        fun fromJson(json: String): Homophones {
            val root = JSONObject(json.trimStart('﻿'))
            val map = HashMap<String, MutableSet<String>>()
            for (word in root.keys()) {
                val item = root.optJSONObject(word) ?: continue
                for (accent in item.keys()) {
                    val v = item.optString(accent).trim('/', ' ')
                    if (v.isNotEmpty()) map.getOrPut(word.lowercase()) { mutableSetOf() } += v
                }
            }
            return Homophones(map)
        }
    }
}

/**
 * 本地跟读打分：把识别结果和参考句按词对齐。
 *
 * - 每个参考词：读对（含同音词、缩写去撇号、复合词被拆开/合并）100 分；读成别的词按拼写相似度给 0~85 分；没读到算漏读 0 分。
 * - 准确 = 读到的词的平均分；完整 = 读到的词占比；流利 = 100 减去词间长停顿和多读词的扣分。
 * - 总分 = 全部参考词平均分（漏读按 0）× 0.8 + 流利 × 0.2。
 *
 * 识别引擎会把读音不准的词"纠正"成它认为最像的真词，所以这里只能判断读没读对词，评不到音素细节。
 * [tokens] / [timestamps] 是 sherpa-onnx 的 BPE 片段和每个片段的起始秒数，词首片段以空格或 ▁ 开头。
 * 一个词都没识别出来时返回 null。
 */
object ReadingScore {
    private const val MergeThreshold = 0.85
    private const val MergePenalty = 0.05
    /** 识别时间戳只标片段起点，估一个片段的时长，用来算词后停顿 */
    private const val TokenTail = 0.25f
    private const val PauseAllowed = 0.4f
    /** 句中标点后允许多停一会儿 */
    private const val PauseAllowedAtPunct = 1.0f

    private class Part(val token: Int, val text: String, val pauseAllowedAfter: Boolean)
    private class Heard(var text: String, val start: Float, var last: Float)
    private class Match(val sim: Double, val firstHeard: Int, val lastHeard: Int)

    fun score(reference: String, tokens: List<String>, timestamps: List<Float>, homophones: Homophones = Homophones.Empty): Assessment? {
        val refTokens = tokenizeSpeech(reference)
        if (refTokens.isEmpty()) return null
        // 连字符词（T-shirt）拆成段逐段对齐，段分取平均
        val parts = ArrayList<Part>()
        refTokens.forEachIndexed { i, t ->
            val segments = t.text.split('-').map(::normalize).filter { it.isNotEmpty() }
            val punct = Regex("^\\s*[,.;:!?]").containsMatchIn(reference.substring(min(t.range.last + 1, reference.length)))
            segments.forEachIndexed { k, s -> parts += Part(i, s, punct && k == segments.lastIndex) }
        }

        val heard = ArrayList<Heard>()
        tokens.forEachIndexed { i, t ->
            val ts = timestamps.getOrElse(i) { heard.lastOrNull()?.last ?: 0f }
            if (t.startsWith(" ") || t.startsWith("▁") || heard.isEmpty()) heard += Heard(t.trimStart(' ', '▁'), ts, ts)
            else heard.last().apply { text += t; last = ts }
        }
        heard.forEach { it.text = normalize(it.text) }
        heard.removeAll { it.text.isEmpty() }
        if (heard.isEmpty()) return null

        fun sim(r: String, h: String): Double = when {
            r == h || r.replace("'", "") == h.replace("'", "") || homophones.same(r, h) -> 1.0
            else -> 1.0 - editDistance(r, h).toDouble() / max(r.length, h.length)
        }

        // 编辑距离式对齐；除了一对一，还允许 1 个参考词对 2 个识别词（super market）和 2 对 1（every day → everyday）
        val n = parts.size
        val m = heard.size
        val cost = Array(n + 1) { DoubleArray(m + 1) { Double.MAX_VALUE } }
        val back = Array(n + 1) { arrayOfNulls<IntArray>(m + 1) }
        val backSim = Array(n + 1) { DoubleArray(m + 1) }
        cost[0][0] = 0.0
        for (i in 0..n) for (j in 0..m) {
            if (i == 0 && j == 0) continue
            fun offer(c: Double, di: Int, dj: Int, s: Double) {
                if (c < cost[i][j]) { cost[i][j] = c; back[i][j] = intArrayOf(di, dj); backSim[i][j] = s }
            }
            if (i >= 1) offer(cost[i - 1][j] + 1, 1, 0, 0.0)
            if (j >= 1) offer(cost[i][j - 1] + 1, 0, 1, 0.0)
            if (i >= 1 && j >= 1) {
                val s = sim(parts[i - 1].text, heard[j - 1].text)
                offer(cost[i - 1][j - 1] + 1 - s, 1, 1, s)
            }
            if (i >= 1 && j >= 2) {
                val s = sim(parts[i - 1].text, heard[j - 2].text + heard[j - 1].text)
                if (s >= MergeThreshold) offer(cost[i - 1][j - 2] + 2 * (1 - s) + MergePenalty, 1, 2, s)
            }
            if (i >= 2 && j >= 1) {
                val s = sim(parts[i - 2].text + parts[i - 1].text, heard[j - 1].text)
                if (s >= MergeThreshold) offer(cost[i - 2][j - 1] + 2 * (1 - s) + MergePenalty, 2, 1, s)
            }
        }
        val matches = arrayOfNulls<Match>(n)
        var inserted = 0
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            val (di, dj) = back[i][j]!!.let { it[0] to it[1] }
            if (di > 0 && dj > 0) for (k in i - di until i) matches[k] = Match(backSim[i][j], j - dj, j - 1)
            else if (dj > 0) inserted++
            i -= di
            j -= dj
        }

        val partScores = matches.map { mt -> mt?.let { if (it.sim >= 1.0) 100.0 else 85 * it.sim } }
        val words = refTokens.mapIndexed { t, token ->
            val own = parts.indices.filter { parts[it].token == t }
            if (own.all { partScores[it] == null }) WordScore(token.text, 0.0, "Omission")
            else {
                val s = own.sumOf { partScores[it] ?: 0.0 } / own.size
                WordScore(token.text, s, if (s < 60) "Mispronunciation" else "None")
            }
        }
        val said = words.filter { it.errorType != "Omission" }
        val accuracy = if (said.isEmpty()) 0.0 else said.sumOf { it.score } / said.size
        val completeness = 100.0 * said.size / words.size

        var penalty = 8.0 * inserted
        val matched = matches.withIndex().filter { it.value != null }.map { it.index to it.value!! }
        matched.zipWithNext().forEach { (a, b) ->
            if (a.second.firstHeard == b.second.firstHeard) return@forEach
            val pause = heard[b.second.firstHeard].start - (heard[a.second.lastHeard].last + TokenTail)
            val allowed = if (parts[a.first].pauseAllowedAfter) PauseAllowedAtPunct else PauseAllowed
            if (pause > allowed) penalty += min(20.0, (pause - allowed) * 25.0)
        }
        val fluency = if (matched.isEmpty()) 0.0 else (100 - penalty).coerceAtLeast(0.0)
        val pron = 0.8 * words.sumOf { it.score } / words.size + 0.2 * fluency

        return Assessment(
            pron = pron,
            accuracy = accuracy,
            fluency = fluency,
            completeness = completeness,
            words = words,
            recognized = heard.joinToString(" ") { it.text }.replaceFirstChar { it.uppercase() }
        )
    }

    private fun normalize(word: String) = word.lowercase().filter { it in 'a'..'z' || it == '\'' }.trim('\'')

    private fun editDistance(a: String, b: String): Int {
        var prev = IntArray(b.length + 1) { it }
        for (x in 1..a.length) {
            val cur = IntArray(b.length + 1)
            cur[0] = x
            for (y in 1..b.length) cur[y] = minOf(prev[y] + 1, cur[y - 1] + 1, prev[y - 1] + if (a[x - 1] == b[y - 1]) 0 else 1)
            prev = cur
        }
        return prev[b.length]
    }
}
