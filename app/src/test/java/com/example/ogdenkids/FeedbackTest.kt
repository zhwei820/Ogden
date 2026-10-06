package com.example.ogdenkids

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackTest {
    @Test
    fun longestMessageFitsWecomTextLimit() {
        val message = Feedback.message(
            content = "汉".repeat(Feedback.MAX_CONTENT_CHARS + 100),
            contact = "联".repeat(Feedback.MAX_CONTACT_CHARS + 100),
            appVersion = "10.10.10",
            device = "x".repeat(80),
            android = "Android 14 (API 34)"
        )
        assertTrue(message.toByteArray(Charsets.UTF_8).size <= 2048)
    }

    @Test
    fun blankContactIsOmitted() {
        val message = Feedback.message("发音不对", "  ", "1.0.0", "Xiaomi 14", "Android 14 (API 34)")
        assertFalse(message.contains("联系方式"))
    }
}
