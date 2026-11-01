package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TextLookupTest {
    private fun word(w: String, c: Category = Category.Picturable) =
        OgdenWord(w, c, "", "", "", "", emptyList(), "", "")

    private fun speech(id: String, level: Int, vararg lines: String, words: List<String> = emptyList()) =
        Speech(id, level, 1, SpeechTheme.Me, id, id, lines.map { SpeechLine(it, "") }, listOf(SpeechLine("x", "")), words)

    private val words = listOf(word("box"), word("cat"), word("red", Category.Qualities))

    @Test
    fun inflectedWordFindsLessonsAndPointsAtTheLine() {
        val speeches = listOf(
            speech("a", 2, "I see a dog."),
            speech("b", 1, "Hello.", "There are two boxes.")
        )
        val result = lookupSelection("Boxes", words, speeches)
        assertEquals(listOf("box"), result.words.map { it.word })
        assertEquals(listOf("b"), result.lessons.map { it.speech.id })
        assertEquals("There are two boxes.", result.lessons.single().line?.en)
    }

    @Test
    fun exactPhraseRanksFirstThenCoverage() {
        val speeches = listOf(
            speech("cat-only", 1, "A cat."),
            speech("both", 1, "The cat sat. A box is here."),
            speech("phrase", 3, "Look, a  red   box!")
        )
        val result = lookupSelection("red box", words, speeches)
        assertEquals(listOf("phrase"), result.lessons.take(1).map { it.speech.id })
        assertTrue(result.lessons.none { it.speech.id == "cat-only" })
    }

    @Test
    fun unitWordListCountsWithoutLine() {
        val result = lookupSelection("cat", words, listOf(speech("u", 1, "Hello.", words = listOf("cat"))))
        assertEquals(null, result.lessons.single().line)
    }
}
