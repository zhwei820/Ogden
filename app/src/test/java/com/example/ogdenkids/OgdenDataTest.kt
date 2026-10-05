package com.example.ogdenkids

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.File

class OgdenDataTest {
    private val words = JSONArray(
        File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('\uFEFF')
    )

    @Test
    fun wordListHasExpectedTotalAndCategories() {
        assertEquals(1200, words.length())
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
        assertEquals(350, counts["ex"])
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
        }
    }
}
