package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MusicTheoryTest {
    private fun rounds(game: MusicGame, seeds: Int = 50) = (0 until seeds).flatMap { buildRound(game, Random(it), 0) }

    @Test
    fun everyRoundIsFullAndAnswersAreValid() {
        for (game in MusicGame.values()) repeat(50) { seed ->
            val round = buildRound(game, Random(seed), 0)
            assertEquals(game.name, MusicRoundSize, round.size)
            round.forEach { q ->
                if (q.visual == Visual.Keys) assertTrue("${game.name} ${q.prompt}", q.answer in 60..71)
                else assertTrue("${game.name} ${q.prompt}", q.answer in q.options.indices && q.options.toSet().size == q.options.size)
            }
        }
    }

    @Test
    fun lessonsPracticeWhatTheyTeach() {
        assertEquals(MusicGame.HalfStep, MusicLesson.Accidentals.practice)
        assertEquals(MusicGame.Interval, MusicLesson.Intervals.practice)
        val half = rounds(MusicGame.HalfStep)
        assertTrue(half.any { it.visual == Visual.Keys && it.answer in setOf(61, 63, 66, 68, 70) })
        assertTrue(half.any { q -> q.options.any { "半音" in it } })
        assertTrue(rounds(MusicGame.Interval).all { q -> q.options.all { it.endsWith("度") } })
    }

    @Test
    fun readStaffStaysWithinTaughtRange() {
        rounds(MusicGame.ReadStaff).forEach { q -> assertTrue(q.prompt, q.reveal.all { it.midi in 60..72 }) }
    }

    @Test
    fun listeningGamesReplayTheAnswer() {
        for (game in listOf(MusicGame.HighLow, MusicGame.Meter, MusicGame.Chord, MusicGame.Ear, MusicGame.Interval)) {
            rounds(game, 5).filter { it.sound.isNotEmpty() }.forEach { assertTrue(game.name, it.reveal.isNotEmpty()) }
        }
    }

    @Test
    fun gamesAreListedInLessonOrder() {
        val firstLesson = MusicGame.values().map { g -> MusicLesson.values().indexOfFirst { it.practice == g }.let { if (it < 0) Int.MAX_VALUE else it } }
        assertEquals(firstLesson.sorted(), firstLesson)
    }
}
