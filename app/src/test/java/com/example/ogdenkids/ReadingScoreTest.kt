package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

class ReadingScoreTest {
    private val homophones = Homophones.fromJson(File("src/main/assets/ogden_ipa.json").readText(Charsets.UTF_8))

    /** 模拟识别结果：每个词一个片段（词首带空格，和 sherpa-onnx 一致），默认每 0.3 秒一个词 */
    private fun score(reference: String, heard: String, starts: List<Float>? = null): Assessment? {
        val words = heard.split(" ").filter { it.isNotBlank() }
        return ReadingScore.score(reference, words.map { " " + it.uppercase() }, starts ?: words.indices.map { it * 0.3f }, homophones)
    }

    private fun Assessment.scores() = words.map { it.score.toInt() }

    @Test
    fun perfectReadingScoresFull() {
        val r = score("My name is Amy.", "my name is amy")!!
        assertEquals(100, r.pron.toInt())
        assertEquals(listOf(100, 100, 100, 100), r.scores())
        assertEquals(100, r.completeness.toInt())
    }

    @Test
    fun bpePiecesJoinIntoWords() {
        val r = ReadingScore.score("My name is Amy.", listOf(" MY", " NAME", " IS", " A", "M", "Y"), listOf(0f, 0.28f, 0.56f, 0.72f, 0.92f, 1.0f))!!
        assertEquals(listOf(100, 100, 100, 100), r.scores())
        assertEquals("My name is amy", r.recognized)
    }

    @Test
    fun compoundSplitsAndMergesStillCount() {
        assertEquals(100, score("Today I go to the supermarket.", "to day i go to the super market")!!.pron.toInt())
        assertEquals(100, score("I play every day.", "i play everyday")!!.pron.toInt())
        assertEquals(100, score("My T-shirt is red.", "my t shirt is red")!!.pron.toInt())
    }

    @Test
    fun homophonesAndContractionsCount() {
        assertEquals(100, score("Their dog is big.", "there dog is big")!!.pron.toInt())
        assertEquals(100, score("I see the sea.", "i see the see")!!.pron.toInt())
        assertEquals(100, score("I don't know.", "i dont no")!!.pron.toInt())
    }

    @Test
    fun omissionAndMisreadAreMarked() {
        val missing = score("I like books.", "i like")!!
        assertEquals(listOf("None", "None", "Omission"), missing.words.map { it.errorType })
        assertEquals(67, missing.completeness.roundToInt())
        assertTrue(missing.pron < 75)

        val misread = score("I like books.", "i lack books")!!
        assertEquals("Mispronunciation", misread.words[1].errorType)
        assertTrue(misread.words[1].score < 60)
    }

    @Test
    fun extraWordDoesNotSwallowNeighbours() {
        val r = score("I like books.", "i like the books")!!
        assertEquals(listOf(100, 100, 100), r.scores())
        assertTrue(r.fluency < 100)
    }

    @Test
    fun longPauseLowersFluencyButPunctuationAllowsSome() {
        val pausedMidPhrase = score("I like red apples.", "i like red apples", listOf(0f, 0.3f, 2.3f, 2.6f))!!
        assertTrue(pausedMidPhrase.fluency <= 80)
        val pausedAtComma = score("Hello, friends!", "hello friends", listOf(0f, 1.2f))!!
        assertEquals(100, pausedAtComma.fluency.toInt())
    }

    @Test
    fun wrongSentenceScoresLow() {
        assertTrue(score("Books make me happy.", "what is your name")!!.pron < 40)
    }

    @Test
    fun nothingHeardReturnsNull() {
        assertNull(ReadingScore.score("Hello!", emptyList(), emptyList()))
    }
}
