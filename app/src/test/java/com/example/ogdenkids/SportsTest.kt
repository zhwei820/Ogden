package com.example.ogdenkids

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

class SportsTest {
    /** 站在画面中间、躯干约占 0.2 个画面高的人，整体上移 [lift]（画面高度比例），叠加 [noise] 抖动 */
    private fun body(t: Long, lift: Float, noise: Float = 0f, rnd: Random = Random(1), dx: Float = 0f): BodyFrame {
        fun n() = (rnd.nextFloat() - 0.5f) * 2 * noise
        val pts = arrayOfNulls<P>(33)
        val y0 = 0.4f - lift
        pts[BodyFrame.Nose] = P(0.5f + dx, y0 - 0.1f + n())
        pts[BodyFrame.LShoulder] = P(0.45f + dx, y0 + n())
        pts[BodyFrame.RShoulder] = P(0.55f + dx, y0 + n())
        pts[BodyFrame.LHip] = P(0.47f + dx, y0 + 0.2f + n())
        pts[BodyFrame.RHip] = P(0.53f + dx, y0 + 0.2f + n())
        pts[BodyFrame.LAnkle] = P(0.47f + dx, y0 + 0.5f + n())
        pts[BodyFrame.RAnkle] = P(0.53f + dx, y0 + 0.5f + n())
        return BodyFrame(t, 0.75f, pts)
    }

    /** 以 [fps] 帧率、每秒 [hz] 下、离地高度 [height]（躯干的比例）跳 [seconds] 秒 */
    private fun jumpSeries(seconds: Float, hz: Float, height: Float, fps: Int = 20, noise: Float = 0.003f): List<BodyFrame> {
        val rnd = Random(7)
        val torso = 0.2f
        val frames = (seconds * fps).toInt()
        return List(frames) { i ->
            val t = i * 1000L / fps
            val phase = (t / 1000f * hz) % 1f
            // 半个周期在空中（正弦上半），半个周期在地上
            val lift = max(0f, sin(phase * 2 * PI).toFloat()) * height * torso
            body(1000 + t, lift, noise, rnd)
        }
    }

    @Test
    fun countsSteadyJumps() {
        val counter = JumpCounter()
        jumpSeries(seconds = 10f, hz = 2f, height = 0.3f).forEach { counter.update(it) }
        assertTrue("counted ${counter.count}", counter.count in 19..20)
    }

    @Test
    fun countsFastSmallHops() {
        // 熟练的孩子 1 分钟 150 多下：每秒 2.6 下、只离地一点点
        val counter = JumpCounter()
        jumpSeries(seconds = 10f, hz = 2.6f, height = 0.2f, fps = 25).forEach { counter.update(it) }
        assertTrue("counted ${counter.count}", counter.count in 25..26)
    }

    @Test
    fun standingStillWithJitterCountsNothing() {
        val counter = JumpCounter()
        val rnd = Random(3)
        repeat(400) { counter.update(body(1000L + it * 50, 0f, noise = 0.006f, rnd = rnd)) }
        assertEquals(0, counter.count)
    }

    @Test
    fun walkingAwayDoesNotCount() {
        // 慢慢往后退：画面里人整体上移（y 变小）
        val counter = JumpCounter()
        repeat(200) { counter.update(body(1000L + it * 50, it * 0.0008f)) }
        assertEquals(0, counter.count)
    }

    @Test
    fun leavingFrameResets() {
        val counter = JumpCounter()
        jumpSeries(seconds = 2f, hz = 2f, height = 0.3f).forEach { counter.update(it) }
        val before = counter.count
        repeat(10) { counter.update(null) }
        // 回来时站在另一个高度，不应误数
        counter.update(body(10_000, 0.1f))
        counter.update(body(10_050, 0.1f))
        assertEquals(before, counter.count)
        assertFalse(counter.inAir)
    }

    @Test
    fun dodgeBlockHitsBodyOnce() {
        val game = DodgeGame(aspect = 0.75f, random = Random(1))
        val body = Area(0f, 0f, 1f, 1f) // 占满画面：必然被砸
        var hits = 0
        repeat(20 * 30) { if (game.step(0.05f, body, emptyList()).hit) hits++ }
        assertTrue(game.over)
        assertEquals(DodgeGame.MaxLives, hits)
    }

    @Test
    fun dodgeNoBodyContactNoDamage() {
        val game = DodgeGame(aspect = 0.75f, random = Random(1))
        val away = Area(5f, 5f, 6f, 6f) // 画面外：永远碰不到
        repeat(20 * 60) { game.step(0.05f, away, emptyList()) }
        assertEquals(DodgeGame.MaxLives, game.lives)
        assertEquals(0, game.stars)
        assertTrue("score ${game.score}", game.score >= 59)
    }

    /** 等第一个水果 / 炸弹飞到画面中部，返回它 */
    private fun firstFlying(game: FruitGame): Flying {
        repeat(200) {
            game.step(0.02f, emptyList())
            game.items.firstOrNull { it.y < 0.7f }?.let { return it }
        }
        error("nothing spawned")
    }

    private fun swipeThrough(f: Flying, seconds: Float) =
        Swipe(P(f.x - 0.2f, f.y), P(f.x + 0.2f, f.y), seconds)

    @Test
    fun fastSwipeSlicesFruit() {
        val game = FruitGame(aspect = 0.75f, random = Random(2))
        val f = firstFlying(game)
        assertTrue(f.kind != null)
        val ev = game.step(0.001f, listOf(swipeThrough(f, 0.05f)))
        assertEquals(1, ev.cut)
        assertTrue(f.sliced)
        assertEquals(1, game.score)
    }

    @Test
    fun slowHandDoesNotSlice() {
        val game = FruitGame(aspect = 0.75f, random = Random(2))
        val f = firstFlying(game)
        // 0.4 宽度用了 1 秒：只是慢慢伸手
        val ev = game.step(0.001f, listOf(swipeThrough(f, 1f)))
        assertEquals(0, ev.cut)
        assertFalse(f.sliced)
    }

    @Test
    fun missingSwipeDoesNotSlice() {
        val game = FruitGame(aspect = 0.75f, random = Random(2))
        val f = firstFlying(game)
        val ev = game.step(0.001f, listOf(Swipe(P(f.x - 0.2f, f.y - 0.4f), P(f.x + 0.2f, f.y - 0.4f), 0.05f)))
        assertEquals(0, ev.cut)
    }

    @Test
    fun roundEndsAfterSixtySeconds() {
        val game = FruitGame(aspect = 0.75f, random = Random(2))
        repeat(60 * 20 + 5) { game.step(0.05f, emptyList()) }
        assertTrue(game.over)
        assertEquals(0, game.remaining)
        assertEquals(0, game.score)
    }
}
