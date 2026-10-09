package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar
import kotlin.random.Random

class MathDrillTest {
    @Test
    fun drillsRespectOpAndCarryAndStayWithin100() {
        for (op in MathOp.values()) for (carry in CarryMode.values()) repeat(50) { seed ->
            val drill = buildMathDrill(op, carry, Random(seed))
            assertEquals(MathDrillSize, drill.size)
            assertEquals(drill.size, drill.toSet().size)
            drill.forEach { p ->
                assertTrue(p.toString(), p.a in 1..100 && p.b in 1..99 && p.answer in 0..100)
                when (op) {
                    MathOp.Add -> assertTrue(p.plus)
                    MathOp.Sub -> assertTrue(!p.plus)
                    MathOp.Mixed -> Unit
                }
                when (carry) {
                    CarryMode.With -> assertTrue(p.toString(), hasCarry(p))
                    CarryMode.Without -> assertTrue(p.toString(), !hasCarry(p) && p.answer < 100)
                    CarryMode.Any -> Unit
                }
            }
        }
    }

    @Test
    fun carryAndBorrowFollowOnesDigit() {
        assertTrue(hasCarry(MathProblem(38, 5, true)))
        assertTrue(!hasCarry(MathProblem(32, 5, true)))
        assertTrue(hasCarry(MathProblem(42, 7, false)))
        assertTrue(!hasCarry(MathProblem(47, 2, false)))
    }

    @Test
    fun everyClipHasBundledAudio() {
        val names = (0..100).map { "$it" } + "add" + "sub"
        val missing = listOf("zh-CN", "en-US", "en-GB").flatMap { lang -> names.map { "math/$lang/$it.mp3" } }
            .filterNot { File("src/main/assets/$it").exists() }
        assertTrue("缺口算录音，运行 scripts/gen_math_audio.py：$missing", missing.isEmpty())
        assertEquals(listOf("35", "add", "48"), MathProblem(35, 48, true).clipFiles)
    }

    @Test
    fun spokenClipsAreNumberOperatorNumber() {
        assertEquals(listOf("thirty-five", "plus", "seven"), MathProblem(35, 7, true).englishClips)
        assertEquals(listOf("one hundred", "minus", "twenty"), MathProblem(100, 20, false).englishClips)
        assertEquals(listOf("nineteen", "minus", "nine"), MathProblem(19, 9, false).englishClips)
        assertEquals(listOf("三十五", "加", "七"), MathProblem(35, 7, true).chineseClips)
        assertEquals(listOf("一百", "减", "二十"), MathProblem(100, 20, false).chineseClips)
        assertEquals(listOf("十九", "减", "十"), MathProblem(19, 10, false).chineseClips)
    }

    @Test
    fun dayLogAccumulatesRoundsAndRoundTrips() {
        val log = MathDayLog().plusRound(20, 20).plusRound(17, 20)
        assertEquals(MathDayLog(rounds = 2, perfect = 1, correct = 37, total = 40), log)
        assertEquals(log, MathDayLog.parse(log.encode()))
        assertEquals(null, MathDayLog.parse("1,2,3"))
        assertEquals(null, MathDayLog.parse(null))
    }

    @Test
    fun streakCountsBackFromTodayOrYesterday() {
        val today = Calendar.getInstance().apply { set(2026, Calendar.MARCH, 2) }
        // 跨月：2/27、2/28、3/1 连续
        val days = mutableSetOf("2026-02-27", "2026-02-28", "2026-03-01")
        assertEquals("今天没练不断签", 3, mathStreak(today) { it in days })
        days += "2026-03-02"
        assertEquals(4, mathStreak(today) { it in days })
        days -= "2026-03-01"
        assertEquals(1, mathStreak(today) { it in days })
        assertEquals(0, mathStreak(today) { false })
    }
}
