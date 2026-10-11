package com.example.ogdenkids

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ZodiacTest {
    @Test
    fun starSignBoundaries() {
        assertEquals("Capricorn", starSignOf("2019-01-19").en)
        assertEquals("Aquarius", starSignOf("2019-01-20").en)
        assertEquals("Pisces", starSignOf("2019-03-20").en)
        assertEquals("Aries", starSignOf("2019-03-21").en)
        assertEquals("Leo", starSignOf("2018-08-01").en)
        assertEquals("Capricorn", starSignOf("2018-12-31").en)
    }

    @Test
    fun chineseZodiacSwitchesAtSpringFestival() {
        assertEquals("猪", chineseZodiacOf("2020-01-24").zh)
        assertEquals("鼠", chineseZodiacOf("2020-01-25").zh)
        assertEquals("龙", chineseZodiacOf("2024-05-01").zh)
        assertEquals("狗", chineseZodiacOf("2018-08-01").zh)
        assertEquals("马", chineseZodiacOf("1990-06-01").zh)
    }

    @Test
    fun ganzhiYear() {
        fun gz(birthday: String) = ganzhiOf(birthday).let { (stem, zodiac) -> stem.zh + zodiac.branch }
        assertEquals("戊戌", gz("2018-08-01"))
        assertEquals("甲辰", gz("2024-05-01"))
        assertEquals("己亥", gz("2020-01-24"))
        assertEquals("庚子", gz("2020-01-25"))
    }

    @Test
    fun starSignEnglishNamesAreHeadwords() {
        val words = JSONArray(File("src/main/assets/ogden_words.json").readText(Charsets.UTF_8).trimStart('﻿'))
        val headwords = List(words.length()) { words.getJSONObject(it).getString("w") }.toSet()
        StarSigns.forEach { assertTrue("${it.en} 不在词库", it.en in headwords) }
    }
}
