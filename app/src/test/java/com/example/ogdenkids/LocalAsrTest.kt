package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class LocalAsrTest {
    @Test
    fun hotwordsAreReferenceWordsUppercasedOnePerLine() {
        assertEquals("MY\nT\nSHIRT\nIS\nRED", LocalAsr.hotwordsFor("My T-shirt is red. My T-shirt!"))
        assertEquals("I\nDON'T\nKNOW", LocalAsr.hotwordsFor("I don't know."))
    }

    /** bpe.vocab 必须和模型的 tokens.txt 前 500 行逐个一致（tokens.txt 末尾的 #0、#1 是解码图辅助符号） */
    @Test
    fun bpeVocabIsAValidPieceTable() {
        val lines = File("src/main/assets/asr/bpe.vocab").readLines(Charsets.UTF_8)
        assertEquals(500, lines.size)
        assertEquals("<blk>", lines[0].substringBefore('\t'))
        assertEquals("▁THE", lines[4].substringBefore('\t'))
        lines.forEach { it.substringAfter('\t').toFloat() }
    }
}
