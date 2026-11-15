package com.example.ogdenkids

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** 归一化坐标：x、y 都在 0..1，相对于摆正、已按镜像翻转后的画面（孩子往自己右边走，点往右移） */
data class P(val x: Float, val y: Float)

/** 一帧人体姿态。下标沿用 ML Kit / MediaPipe 的 33 点编号；看不清的点为 null */
class BodyFrame(val timeMs: Long, val aspect: Float, private val points: Array<P?>) {
    operator fun get(i: Int): P? = points.getOrNull(i)

    private fun mid(a: Int, b: Int): P? {
        val p = this[a] ?: return null
        val q = this[b] ?: return null
        return P((p.x + q.x) / 2, (p.y + q.y) / 2)
    }

    val shoulders: P? get() = mid(LShoulder, RShoulder)
    val hips: P? get() = mid(LHip, RHip)

    /** 肩中点到髋中点的距离（按画面高度算），用来把动作幅度换算成和远近无关的比例 */
    val torso: Float? get() {
        val s = shoulders ?: return null
        val h = hips ?: return null
        return hypot((s.x - h.x) * aspect, s.y - h.y).takeIf { it > 0.02f }
    }

    /** 跳绳要能看到脚：肩、髋、脚踝都在画面里 */
    val fullBody: Boolean get() = shoulders != null && hips != null && (this[LAnkle] != null || this[RAnkle] != null)

    /** 手掌大致位置：手腕顺着小臂再往外延一点（ML Kit 的手腕点比手掌靠里） */
    fun hand(left: Boolean): P? {
        val w = this[if (left) LWrist else RWrist] ?: return null
        val e = this[if (left) LElbow else RElbow] ?: return w
        return P(w.x + (w.x - e.x) * 0.3f, w.y + (w.y - e.y) * 0.3f)
    }

    /** 身体占的区域（头顶到髋），躲方块用它判断碰撞 */
    fun bodyBox(): Area? {
        val ls = this[LShoulder] ?: return null
        val rs = this[RShoulder] ?: return null
        val h = hips ?: return null
        val shoulderY = (ls.y + rs.y) / 2
        val headTop = this[Nose]?.let { it.y - (shoulderY - it.y) * 0.8f } ?: (shoulderY - (h.y - shoulderY) * 0.6f)
        val half = abs(ls.x - rs.x) / 2
        val cx = (ls.x + rs.x) / 2
        return Area(cx - half * 1.2f, headTop, cx + half * 1.2f, h.y)
    }

    companion object {
        const val Nose = 0
        const val LShoulder = 11
        const val RShoulder = 12
        const val LElbow = 13
        const val RElbow = 14
        const val LWrist = 15
        const val RWrist = 16
        const val LHip = 23
        const val RHip = 24
        const val LKnee = 25
        const val RKnee = 26
        const val LAnkle = 27
        const val RAnkle = 28
        val Bones = listOf(
            LShoulder to RShoulder, LShoulder to LElbow, LElbow to LWrist, RShoulder to RElbow, RElbow to RWrist,
            LShoulder to LHip, RShoulder to RHip, LHip to RHip, LHip to LKnee, LKnee to LAnkle, RHip to RKnee, RKnee to RAnkle
        )
    }
}

data class Area(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun intersects(o: Area) = left < o.right && o.left < right && top < o.bottom && o.top < bottom
    fun contains(p: P) = p.x in left..right && p.y in top..bottom
}

/**
 * 跳绳计数：看肩、髋四点的平均高度相对「站地时高度」抬起多少（按躯干长度归一，离手机远近都一样）。
 * 抬起超过 [rise] 记为离地，回落到 [land] 以内记为落地，落地那一刻算一下。
 * 站地基线向下跟得快、向上跟得慢：人走近走远时基线能跟上，起跳又不会把基线带走。
 */
class JumpCounter(
    private val rise: Float = 0.09f,
    private val land: Float = 0.04f,
    private val minIntervalMs: Long = 160,
    private val maxAirMs: Long = 1200
) {
    var count = 0
        private set
    var inAir = false
        private set
    private var baseline: Float? = null
    private var airSince = 0L
    private var lastJumpAt = Long.MIN_VALUE / 2
    private var lastSeen = 0L

    /** 喂一帧，返回这一帧是否刚数到一下 */
    fun update(frame: BodyFrame?): Boolean {
        val center = frame?.let { centerY(it) }
        val torso = frame?.torso
        if (frame == null || center == null || torso == null) {
            inAir = false
            return false
        }
        // 人离开画面一会儿再回来，位置可能全变了：重来
        if (frame.timeMs - lastSeen > 800) baseline = null
        lastSeen = frame.timeMs
        val base = baseline
        if (base == null) {
            baseline = center
            inAir = false
            return false
        }
        val lift = (base - center) / torso
        if (!inAir) {
            baseline = if (center > base) base + (center - base) * 0.5f else base + (center - base) * 0.03f
            if (lift > rise) {
                inAir = true
                airSince = frame.timeMs
            }
            return false
        }
        if (frame.timeMs - airSince > maxAirMs) {
            // 太久没落回来：多半是人往后退 / 弯腰，不是跳
            inAir = false
            baseline = center
            return false
        }
        if (lift < land) {
            inAir = false
            baseline = base + (center - base) * 0.5f
            if (frame.timeMs - lastJumpAt >= minIntervalMs) {
                lastJumpAt = frame.timeMs
                count++
                return true
            }
        }
        return false
    }

    fun reset() {
        count = 0
        inAir = false
        baseline = null
    }

    private fun centerY(f: BodyFrame): Float? {
        val s = f.shoulders ?: return null
        val h = f.hips ?: return null
        return (s.y + h.y) / 2
    }
}

/** 跳绳模式：限时（秒）或自由跳（0） */
enum class JumpMode(val seconds: Int, val title: String, val subtitle: String) {
    Free(0, "自由跳", "想跳多久跳多久，跳完点结束"),
    Sec30(30, "30 秒挑战", "看看 30 秒能跳多少下"),
    Min1(60, "1 分钟挑战", "体育课测试就是 1 分钟")
}

enum class FallingKind { Block, Star }

class Falling(val kind: FallingKind, val x: Float, var y: Float, val size: Float, var done: Boolean = false)

/**
 * 躲方块：方块从画面上方落下，碰到身体扣一颗心；星星碰到身体或手加分。
 * 坐标同 [BodyFrame]（0..1），[size] 按画面宽度算，纵向尺寸要乘 [aspect]。
 */
class DodgeGame(private val aspect: Float, private val random: Random = Random.Default) {
    val items = mutableListOf<Falling>()
    var lives = MaxLives
        private set
    var score = 0
        private set
    var stars = 0
        private set
    var elapsed = 0f
        private set
    /** 刚被砸到后的无敌剩余秒数（画面上闪烁） */
    var hurtTimer = 0f
        private set
    val over get() = lives <= 0
    private var spawnTimer = 0.8f
    private var dodgedTimer = 0f

    /** 下落速度（每秒多少个画面高度），越玩越快 */
    val speed get() = min(0.28f + elapsed * 0.006f, 0.75f)
    private val spawnInterval get() = max(1.3f - elapsed * 0.012f, 0.5f)

    /** 推进 [dt] 秒；body 为 null（看不到人）时不该调用，由界面暂停 */
    fun step(dt: Float, body: Area, hands: List<P>): StepEvents {
        if (over) return StepEvents.None
        val t = dt.coerceIn(0f, 0.1f)
        elapsed += t
        hurtTimer = max(0f, hurtTimer - t)
        spawnTimer -= t
        if (spawnTimer <= 0f) {
            spawn()
            spawnTimer = spawnInterval
        }
        var hit = false
        var caught = false
        items.forEach { item ->
            item.y += speed * t
            val half = item.size / 2
            val box = Area(item.x - half, item.y - half * aspect, item.x + half, item.y + half * aspect)
            when (item.kind) {
                FallingKind.Block -> if (hurtTimer <= 0f && box.intersects(body)) {
                    item.done = true
                    hit = true
                }
                FallingKind.Star -> if (box.intersects(body) || hands.any { box.contains(it) }) {
                    item.done = true
                    caught = true
                }
            }
            if (item.y - half * aspect > 1f) item.done = true
        }
        items.removeAll { it.done }
        if (hit) {
            lives--
            hurtTimer = HurtSeconds
        }
        if (caught) {
            stars++
            score += 5
        }
        dodgedTimer += t
        if (dodgedTimer >= 1f) {
            dodgedTimer -= 1f
            score++
        }
        return StepEvents(hit = hit, caught = caught)
    }

    private fun spawn() {
        val star = random.nextFloat() < 0.25f
        val size = if (star) 0.12f else 0.14f + random.nextFloat() * 0.08f
        val x = size / 2 + random.nextFloat() * (1f - size)
        items += Falling(if (star) FallingKind.Star else FallingKind.Block, x, -size * aspect, size)
        // 后期偶尔一次掉两块，逼孩子多跑几步
        if (!star && elapsed > 40f && random.nextFloat() < 0.35f) {
            val x2 = if (x < 0.5f) x + 0.45f else x - 0.45f
            items += Falling(FallingKind.Block, x2.coerceIn(size / 2, 1f - size / 2), -size * aspect * 2.2f, size)
        }
    }

    data class StepEvents(val hit: Boolean, val caught: Boolean) {
        companion object { val None = StepEvents(false, false) }
    }

    companion object {
        const val MaxLives = 3
        const val HurtSeconds = 1.2f
    }
}

data class SportsRecord(val time: Long, val game: String, val score: Int, val seconds: Int) {
    fun toJson(): JSONObject = JSONObject().put("t", time).put("g", game).put("s", score).put("d", seconds)

    companion object {
        fun fromJson(o: JSONObject) = SportsRecord(o.getLong("t"), o.getString("g"), o.getInt("s"), o.optInt("d"))
    }
}

/** 运动成绩，按孩子分开存（与其他模块同一个 prefs 文件） */
class SportsStore(context: Context, profileId: String) {
    private val prefs = context.getSharedPreferences(ProfileStore.progressPrefsName(profileId), Context.MODE_PRIVATE)

    fun best(game: String): Int = prefs.getInt("sports.best.$game", 0)

    fun records(): List<SportsRecord> = runCatching {
        val arr = JSONArray(prefs.getString("sports.records", "[]"))
        List(arr.length()) { SportsRecord.fromJson(arr.getJSONObject(it)) }
    }.getOrDefault(emptyList())

    /** 记一次成绩，返回是否破了纪录 */
    fun save(record: SportsRecord): Boolean {
        val newBest = record.score > best(record.game)
        val arr = JSONArray()
        (listOf(record) + records()).take(MaxRecords).forEach { arr.put(it.toJson()) }
        val edit = prefs.edit().putString("sports.records", arr.toString())
        if (newBest) edit.putInt("sports.best.${record.game}", record.score)
        edit.apply()
        return newBest
    }

    /** 今天一共跳了多少下（所有跳绳模式合计） */
    fun todayJumps(now: Long = System.currentTimeMillis()): Int {
        val day = dayOf(now)
        return records().filter { it.game.startsWith(JumpGamePrefix) && dayOf(it.time) == day }.sumOf { it.score }
    }

    private fun dayOf(t: Long): Long {
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = t }
        return cal.get(java.util.Calendar.YEAR) * 1000L + cal.get(java.util.Calendar.DAY_OF_YEAR)
    }

    companion object {
        const val MaxRecords = 200
        const val JumpGamePrefix = "jump-"
        const val DodgeGameKey = "dodge"
        const val FruitGameKey = "fruit"
        fun jumpKey(mode: JumpMode) = "$JumpGamePrefix${mode.name}"
    }
}

/** 手一帧内划过的一段：[from] → [to]，用时 [seconds] */
class Swipe(val from: P, val to: P, val seconds: Float)

enum class FruitKind(val emoji: String, val juice: Long) {
    Watermelon("🍉", 0xFFEF4444), Apple("🍎", 0xFFF87171), Orange("🍊", 0xFFFB923C), Lemon("🍋", 0xFFFDE047),
    Peach("🍑", 0xFFFDBA74), Pineapple("🍍", 0xFFFACC15), Kiwi("🥝", 0xFF84CC16), Strawberry("🍓", 0xFFF43F5E),
    Banana("🍌", 0xFFFEF08A), Grapes("🍇", 0xFFA855F7)
}

/** 飞在空中的水果 / 炸弹（[kind] 为 null 是炸弹）；切开后变成两半继续飞 */
class Flying(val kind: FruitKind?, var x: Float, var y: Float, var vx: Float, var vy: Float, val radius: Float) {
    var sliced = false
    /** 切开后两半各自往左右分开的横向距离，和切的方向（弧度，画面里的角度） */
    var split = 0f
    var cutAngle = 0f
    var spin = 0f
}

/**
 * 切水果：水果从画面下方抛上来，手（手腕往前延一点）快速划过就切开。一局 [roundSeconds] 秒。
 * 坐标同 [BodyFrame]；[Flying.radius] 按画面宽度算，判碰撞时把 y 换算到同一尺度（除以 [aspect]）。
 */
class FruitGame(private val aspect: Float, private val random: Random = Random.Default, val roundSeconds: Int = 60) {
    val items = mutableListOf<Flying>()
    var score = 0
        private set
    var sliced = 0
        private set
    var bombs = 0
        private set
    var elapsed = 0f
        private set
    /** 最近一次连切的个数和剩余显示秒数 */
    var combo = 0
        private set
    var comboTimer = 0f
        private set
    var bombTimer = 0f
        private set
    val over get() = elapsed >= roundSeconds
    val remaining get() = max(0, roundSeconds - elapsed.toInt())
    private var spawnTimer = 0.6f

    fun step(dt: Float, swipes: List<Swipe>): Events {
        if (over) return Events(0, false)
        val t = dt.coerceIn(0f, 0.1f)
        elapsed += t
        comboTimer = max(0f, comboTimer - t)
        bombTimer = max(0f, bombTimer - t)
        spawnTimer -= t
        if (spawnTimer <= 0f) {
            spawnWave()
            spawnTimer = max(1.6f - elapsed * 0.012f, 0.9f) + random.nextFloat() * 0.4f
        }
        val fast = swipes.filter { speed(it) >= MinSwipeSpeed }
        var cut = 0
        var boom = false
        items.forEach { item ->
            item.vy += Gravity * t
            item.x += item.vx * t
            item.y += item.vy * t
            item.spin += t * (if (item.vx >= 0) 2f else -2f)
            if (item.sliced) {
                item.split += t * 0.35f
            } else {
                fast.firstOrNull { hits(it, item) }?.let { s ->
                    item.sliced = true
                    item.cutAngle = kotlin.math.atan2((s.to.y - s.from.y) / aspect, s.to.x - s.from.x)
                    if (item.kind == null) boom = true else cut++
                }
            }
        }
        items.removeAll { it.y - it.radius / aspect > 1.05f && it.vy > 0 }
        if (cut > 0) {
            sliced += cut
            score += cut + if (cut >= 3) cut else 0
            if (cut >= 3) {
                combo = cut
                comboTimer = 1.2f
            }
        }
        if (boom) {
            bombs++
            score = max(0, score - BombPenalty)
            bombTimer = 0.6f
        }
        return Events(cut, boom)
    }

    private fun speed(s: Swipe): Float {
        if (s.seconds <= 0f) return 0f
        return hypot(s.to.x - s.from.x, (s.to.y - s.from.y) / aspect) / s.seconds
    }

    /** 线段到圆心的最近距离小于半径（都换算成画面宽度单位） */
    private fun hits(s: Swipe, f: Flying): Boolean {
        val ax = s.from.x; val ay = s.from.y / aspect
        val bx = s.to.x; val by = s.to.y / aspect
        val cx = f.x; val cy = f.y / aspect
        val dx = bx - ax; val dy = by - ay
        val len2 = dx * dx + dy * dy
        val u = if (len2 == 0f) 0f else (((cx - ax) * dx + (cy - ay) * dy) / len2).coerceIn(0f, 1f)
        return hypot(ax + u * dx - cx, ay + u * dy - cy) <= f.radius * 1.15f
    }

    private fun spawnWave() {
        val n = 1 + random.nextInt(if (elapsed < 15f) 2 else 3)
        repeat(n) {
            val bomb = elapsed > 8f && random.nextFloat() < 0.15f
            val r = 0.075f + random.nextFloat() * 0.02f
            val x = 0.15f + random.nextFloat() * 0.7f
            val apex = 0.12f + random.nextFloat() * 0.25f
            val y0 = 1f + r / aspect
            val vy = -kotlin.math.sqrt(2 * Gravity * (y0 - apex))
            // 往画面中间偏一点，别飞出两侧
            val vx = (0.5f - x) * (0.25f + random.nextFloat() * 0.35f)
            items += Flying(if (bomb) null else FruitKind.values()[random.nextInt(FruitKind.values().size)], x, y0, vx, vy, r)
        }
    }

    data class Events(val cut: Int, val bomb: Boolean)

    companion object {
        /** 重力：每秒² 多少个画面高度 */
        const val Gravity = 1.5f
        /** 手划过的最低速度（每秒多少个画面宽度），慢慢伸手碰到不算切 */
        const val MinSwipeSpeed = 0.9f
        const val BombPenalty = 5
    }
}
