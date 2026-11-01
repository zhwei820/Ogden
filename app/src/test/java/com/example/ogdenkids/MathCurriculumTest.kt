package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MathCurriculumTest {
    @Test
    fun everyGradeHasUnitsAndKeysAreUnique() {
        for (g in 3..6) assertTrue("grade $g", mathUnitsOf(g).size >= 8)
        assertEquals(MathUnits.size, MathUnits.map { it.key }.toSet().size)
    }

    @Test
    fun questionsHaveFourDistinctOptionsWithOneAnswer() {
        for (unit in MathUnits) repeat(300) { seed ->
            val q = unit.generate(Random(seed))
            val where = "${unit.key} seed=$seed ${q.prompt} ${q.options}"
            assertEquals(where, 4, q.options.size)
            assertEquals(where, 4, q.options.toSet().size)
            assertEquals(where, 1, q.options.count { it == q.answer })
            assertFalse(where, q.options.any { "'" in it || "null" in it || it.endsWith("/0") || it.isBlank() })
            assertTrue(where, q.explain.isNotBlank())
        }
    }

    @Test
    fun roundsAreFullAndNonRepeating() {
        for (unit in MathUnits) {
            val round = buildMathUnitRound(unit, Random(7))
            assertEquals(unit.key, MathUnitRoundSize, round.size)
            assertEquals(unit.key, round.size, round.map { it.prompt }.toSet().size)
        }
    }

    @Test
    fun spotCheckAnswers() {
        // 抽查几个单元：把题面里的算式重新算一遍，和答案对比
        val mul = Regex("""^(\d+) × (\d+) = \?$""")
        val div = Regex("""^(\d+) ÷ (\d+) = \?$""")
        for (key in listOf("g3-mul1", "g3-mul2", "g4-mul3", "g3-div1", "g4-div2")) repeat(200) { seed ->
            val q = mathUnit(key)!!.generate(Random(seed))
            mul.find(q.prompt)?.let { assertEquals(q.prompt, (it.groupValues[1].toLong() * it.groupValues[2].toLong()).toString(), q.answer) }
            div.find(q.prompt)?.let { assertEquals(q.prompt, (it.groupValues[1].toLong() / it.groupValues[2].toLong()).toString(), q.answer) }
        }
        val eq = Regex("""^解方程：(\d+)x \+ (\d+) = (\d+)""")
        repeat(200) { seed ->
            val q = mathUnit("g5-eq")!!.generate(Random(seed))
            eq.find(q.prompt)?.let { m ->
                val (a, b, c) = m.groupValues.drop(1).map { it.toLong() }
                assertEquals(q.prompt, c, a * q.answer.toLong() + b)
            }
        }
    }
}
