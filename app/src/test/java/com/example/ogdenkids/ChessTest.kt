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

    @Test
    fun specialMovesAreExplained() {
        val castle = ChessPosition.fromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
        assertTrue(specialSelectHint(castle, parseSq("e1"))!!.contains("王车易位"))
        assertTrue(specialMoveNote(castle, ChessMove.parse("e1g1"), true)!!.contains("王车易位"))
        assertEquals(null, specialMoveNote(castle, ChessMove.parse("e1f1"), true))

        val ep = ChessPosition.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")
        assertTrue(specialSelectHint(ep, parseSq("e5"))!!.contains("过路兵"))
        assertTrue(specialMoveNote(ep, ChessMove.parse("e5d6"), false)!!.contains("对方吃过路兵"))
        assertEquals(null, specialMoveNote(ep, ChessMove.parse("e5e6"), true))

        val promo = ChessPosition.fromFen("4k3/1P6/8/8/8/8/8/4K3 w - - 0 1")
        assertTrue(specialSelectHint(promo, parseSq("b7"))!!.contains("升变"))
        assertTrue(specialMoveNote(promo, ChessMove.parse("b7b8n"), true)!!.contains("变成了马"))
    }

    @Test
    fun illegalMovesAreExplained() {
        // 白王 e1 不能走 d2/e2/f2：黑车 a2 控制第二横线
        val king = ChessPosition.fromFen("4k3/8/8/8/8/8/r7/4K3 w - - 0 1")
        val kingHint = illegalMoveHint(king, parseSq("e1"), parseSq("e2"))
        assertTrue(kingHint, kingHint.contains("王不能走到 e2") && kingHint.contains("a2 的车"))

        // 白象 d2 被 b4 的黑象牵制在 e1 王前
        val pin = ChessPosition.fromFen("4k3/8/8/8/1b6/8/3B4/4K3 w - - 0 1")
        assertTrue(illegalMoveHint(pin, parseSq("d2"), parseSq("e3")).contains("牵制"))

        // 被将军时走无关的子
        val check = ChessPosition.fromFen("4k3/8/8/8/8/8/P7/r3K3 w - - 0 1")
        assertTrue(illegalMoveHint(check, parseSq("a2"), parseSq("a3")).contains("正被将军"))

        val start = ChessPosition.fromFen(ChessPosition.START_FEN)
        assertTrue(illegalMoveHint(start, parseSq("g1"), parseSq("g3")).contains("日"))
        assertTrue(illegalMoveHint(start, parseSq("a1"), parseSq("a4")).contains("挡住"))
        assertTrue(stuckPieceHint(start, parseSq("a1"))!!.contains("挡住"))
        assertEquals(null, stuckPieceHint(start, parseSq("g1")))
    }
}
