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
        for (unit in MathUnits) repeat(100) { seed ->
            val round = buildMathUnitRound(unit, Random(seed))
            assertEquals("${unit.key} seed=$seed", MathUnitRoundSize, round.size)
            assertEquals(unit.key, round.size, round.map { it.prompt }.toSet().size)
        }
    }

    @Test
    fun unitsFollowTextbookOrder() {
        assertEquals("g3-time", mathUnitsOf(3).first().key)
        val g3 = mathUnitsOf(3).map { it.key }
        assertTrue(g3.indexOf("g3-rem") == g3.indexOf("g3-div1") - 1)
        assertTrue(mathUnitsOf(3).first { it.key == "g3-rem" }.title.startsWith("复习"))
        val g4 = mathUnitsOf(4).map { it.key }
        assertTrue(g4.indexOf("g4-mixed") < g4.indexOf("g4-laws"))
        val g6 = mathUnitsOf(6).map { it.key }
        assertEquals(listOf("g6-fracmul", "g6-fracdiv", "g6-ratio", "g6-circle", "g6-percent", "g6-neg", "g6-cylinder", "g6-prop"), g6)
    }

    @Test
    fun unlikeFractionsKeepDifferentDenominators() {
        val q = Regex("""^(\d+)/(\d+) [+−] (\d+)/(\d+) = \?$""")
        repeat(500) { seed ->
            val prompt = mathUnit("g5-fracadd")!!.generate(Random(seed)).prompt
            val m = q.find(prompt) ?: error(prompt)
            assertTrue(prompt, m.groupValues[2] != m.groupValues[4])
        }
    }

    @Test
    fun explanationsMatchTheQuestion() {
        // 加法解析只在真的进位时才说进位
        repeat(300) { seed ->
            val q = mathUnit("g3-add")!!.generate(Random(seed))
            val m = Regex("""^(\d+) \+ (\d+)""").find(q.prompt) ?: return@repeat
            val a = m.groupValues[1].toInt()
            val b = m.groupValues[2].toInt()
            assertEquals(q.prompt, a % 10 + b % 10 >= 10, q.explain.contains("个位") )
        }
        // 小数乘除法题面不出现被去掉小数点的「整数小数」，解析里的整数和题面一致
        repeat(300) { seed ->
            val q = mathUnit("g5-decmul")!!.generate(Random(seed))
            val a = q.prompt.substringBefore(" ×")
            assertTrue(q.prompt, "." in a)
            assertTrue("${q.prompt} / ${q.explain}", q.explain.contains(a.replace(".", "").trimStart('0') + " ×"))
            val d = mathUnit("g5-decdiv")!!.generate(Random(seed))
            assertTrue(d.prompt, "." in d.prompt.substringAfter("÷ ").substringBefore(" ="))
        }
        // 负数统一用「−」
        repeat(300) { seed ->
            val q = mathUnit("g6-neg")!!.generate(Random(seed))
            assertFalse(q.prompt, (listOf(q.prompt, q.explain) + q.options).any { "-" in it })
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
