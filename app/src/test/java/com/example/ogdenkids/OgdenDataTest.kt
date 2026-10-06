package com.example.ogdenkids

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OgdenDataTest {
    private val words = JSONArray(
        File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('\uFEFF')
    )

    @Test
    fun wordListHasExpectedTotalAndCategories() {
        val counts = mutableMapOf<String, Int>()
        repeat(words.length()) { index ->
            val item = words.getJSONObject(index)
            counts[item.getString("c")] = (counts[item.getString("c")] ?: 0) + 1
        }
        assertEquals(100, counts["op"])
        assertEquals(400, counts["gt"])
        assertEquals(200, counts["pt"])
        assertEquals(100, counts["qg"])
        assertEquals(50, counts["qo"])
        // 拓展词与变形词数量随内容增长，只要求存在；Ogden 850 原有五类不能变
        assertTrue((counts["ex"] ?: 0) >= 350)
        assertTrue((counts["fm"] ?: 0) > 0)
        assertTrue((counts["kd"] ?: 0) > 0)
        assertTrue((counts["ky"] ?: 0) > 0)
        assertEquals(words.length(), counts.values.sum())
        val headwords = List(words.length()) { words.getJSONObject(it).getString("w").lowercase() }
        assertEquals("Duplicate headwords", headwords.size, headwords.toSet().size)
    }

    @Test
    fun registerWordsAndFormalFormsAreHeadwords() {
        val headwords = List(words.length()) { words.getJSONObject(it).getString("w").lowercase() }.toSet()
        allRegisters().forEach { (word, register) ->
            assertTrue("register word $word", word in headwords)
            assertTrue("formal ${register.formal} of $word", register.formal.lowercase() in headwords)
        }
    }

    @Test
    fun requiredFieldsArePresent() {
        repeat(words.length()) { index ->
            val item = words.getJSONObject(index)
            listOf("w", "c", "zh", "en", "ex", "exz", "s").forEach { key ->
                assertFalse("Missing $key at $index", item.isNull(key))
            }
            assertFalse(item.getString("w").isBlank())
            assertFalse(item.getString("zh").isBlank())
            assertFalse(item.getString("en").isBlank())
            assertFalse(item.getString("ex").isBlank())
            assertEquals("ex2 / exz2 要成对出现 at $index", item.optString("ex2").isBlank(), item.optString("exz2").isBlank())
        }
    }
}
