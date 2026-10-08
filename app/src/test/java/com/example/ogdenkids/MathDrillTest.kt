package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
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
    fun spokenClipsAreNumberOperatorNumber() {
        assertEquals(listOf("thirty-five", "plus", "seven"), MathProblem(35, 7, true).englishClips)
        assertEquals(listOf("one hundred", "minus", "twenty"), MathProblem(100, 20, false).englishClips)
        assertEquals(listOf("nineteen", "minus", "nine"), MathProblem(19, 9, false).englishClips)
        assertEquals(listOf("三十五", "加", "七"), MathProblem(35, 7, true).chineseClips)
        assertEquals(listOf("一百", "减", "二十"), MathProblem(100, 20, false).chineseClips)
        assertEquals(listOf("十九", "减", "十"), MathProblem(19, 10, false).chineseClips)
    }
}
