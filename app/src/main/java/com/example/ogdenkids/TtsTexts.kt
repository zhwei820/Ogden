package com.example.ogdenkids

import java.security.MessageDigest

/** 预生成 TTS 音频的文件名：与 AzureSpeaker 的运行时缓存同一规则（嗓音 + 文本的 SHA-1）。 */
fun ttsKey(voiceName: String, text: String): String =
    MessageDigest.getInstance("SHA-1").digest("$voiceName|${text.trim()}".toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

fun ttsAssetPath(voice: AzureVoice, text: String) = "tts/${voice.lang}/${ttsKey(voice.voiceName, text)}.mp3"

data class TtsTexts(val english: Set<String>)

/**
 * 应用里所有可能经 AzureSpeaker 朗读的英文（单词本身走 assets/audio 的离线录音，不在此列）。
 * 中文不预生成，播放时在线合成并缓存在本机。
 * 新增会朗读的内容时要同步加到这里，否则 TtsCoverageTest 会报缺音频。
 */
fun speakableTexts(
    words: List<OgdenWord>,
    speeches: List<Speech>,
    modules: List<SpecialModule>,
    related: Collection<RelatedWord> = emptyList()
): TtsTexts {
    val headwords = words.map { it.word.lowercase() }.toSet()
    val en = mutableSetOf<String>()
    val lines = speeches.flatMap { it.lines + it.patterns } + modules.flatMap { it.sentences }
    lines.forEach { en += it.en }
    words.forEach { word ->
        en += word.example
        // 近义词按钮和近义词题的答案；词表内的有离线录音
        (word.synonyms + word.antonyms).filter { it.lowercase() !in headwords }.forEach { en += it }
    }
    // 弹窗里朗读原文写法（boxes、Amy、I'm）、缩写完整写法，以及「课文生词」里存的小写键
    lines.flatMap { tokenizeSpeech(it.en) }.map { it.text }.distinct().forEach { token ->
        contractionOf(token)?.let { en += token; en += expandContraction(token, it) }
        if (token.lowercase() !in headwords) {
            en += token
            en += token.lowercase().removeSuffix("'s")
        }
    }
    related.forEach { en += it.example }
    en += allSpecialUtterances().first
    return TtsTexts(en.map { it.trim() }.filter { it.isNotEmpty() }.toSet())
}
