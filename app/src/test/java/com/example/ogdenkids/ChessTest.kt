package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ChessTest {
    private fun perft(pos: ChessPosition, depth: Int): Long =
        if (depth == 0) 1 else pos.legalMoves().sumOf { perft(pos.play(it), depth - 1) }

    @Test
    fun perftStartPosition() {
        val pos = ChessPosition.fromFen(ChessPosition.START_FEN)
        assertEquals(20L, perft(pos, 1))
        assertEquals(400L, perft(pos, 2))
        assertEquals(8902L, perft(pos, 3))
        assertEquals(197281L, perft(pos, 4))
    }

    @Test
    fun perftKiwipete() {
        // 覆盖易位、吃过路兵、升变、被钉住的子
        val pos = ChessPosition.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1")
        assertEquals(48L, perft(pos, 1))
        assertEquals(2039L, perft(pos, 2))
        assertEquals(97862L, perft(pos, 3))
    }

    @Test
    fun perftEndgameEnPassantPins() {
        val pos = ChessPosition.fromFen("8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1")
        assertEquals(14L, perft(pos, 1))
        assertEquals(191L, perft(pos, 2))
        assertEquals(2812L, perft(pos, 3))
        assertEquals(43238L, perft(pos, 4))
    }

    @Test
    fun everyLessonLevelIsSolvable() {
        for (lesson in ChessLessons) lesson.levels.forEachIndexed { i, level ->
            assertTrue("${lesson.key} #$i par=${level.par}", level.par in 1..12)
        }
    }

    @Test
    fun aiFindsMateInOne() {
        // 白后 h5→f7 将杀（学者杀）
        val pos = ChessPosition.fromFen("r1bqkbnr/pppp1ppp/2n5/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4")
        for (bot in listOf(ChessBot.Fox, ChessBot.Owl, ChessBot.Lion)) {
            assertEquals(bot.name, "h5f7", ChessAi(bot).choose(pos)?.uci())
        }
    }

    @Test
    fun puzzleSolutionsAreLegal() {
        val lines = File("src/main/assets/chess/puzzles.csv").readLines().filter { it.isNotBlank() }
        assertEquals(4000, lines.size)
        for (line in lines) {
            val cols = line.split(',')
            var pos = ChessPosition.fromFen(cols[2])
            for (uci in cols[3].split(' ')) {
                val m = pos.legalMoves().firstOrNull { it.uci() == uci }
                assertNotNull("${cols[1]} $uci", m)
                pos = pos.play(m!!)
            }
        }
    }
}
