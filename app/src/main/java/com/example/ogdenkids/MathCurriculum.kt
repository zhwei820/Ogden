package com.example.ogdenkids

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.random.Random

/** 一道选择题；[options] 含 [answer] 恰好一次 */
class MathQuestion(val prompt: String, val answer: String, val options: List<String>, val explain: String)

class MathUnit(
    val key: String,
    val grade: Int,
    val title: String,
    /** 知识点：每条一句话 */
    val tips: List<String>,
    val generate: (Random) -> MathQuestion
)

const val MathUnitRoundSize = 10

fun mathUnit(key: String): MathUnit? = MathUnits.firstOrNull { it.key == key }

fun mathUnitsOf(grade: Int): List<MathUnit> = MathUnits.filter { it.grade == grade }

// ---------- 工具 ----------

private fun Random.ri(from: Int, to: Int) = nextInt(from, to + 1)

private fun gcd(a: Long, b: Long): Long = if (b == 0L) kotlin.math.abs(a) else gcd(b, a % b)

private fun dec(v: BigDecimal): String = v.stripTrailingZeros().toPlainString()
private fun dec(v: String) = BigDecimal(v)
private fun tenths(n: Int) = BigDecimal(n).movePointLeft(1)
private fun hundredths(n: Int) = BigDecimal(n).movePointLeft(2)
private val Pi = BigDecimal("3.14")

/** 最简分数；分母为 1 时显示整数 */
private class Fr(n: Long, d: Long) {
    val n: Long
    val d: Long

    init {
        val g = gcd(n, d).coerceAtLeast(1)
        val sign = if (d < 0) -1 else 1
        this.n = sign * n / g
        this.d = sign * d / g
    }

    operator fun plus(o: Fr) = Fr(n * o.d + o.n * d, d * o.d)
    operator fun minus(o: Fr) = Fr(n * o.d - o.n * d, d * o.d)
    operator fun times(o: Fr) = Fr(n * o.n, d * o.d)
    operator fun div(o: Fr) = Fr(n * o.d, d * o.n)
    override fun toString() = if (d == 1L) "$n" else "$n/$d"
    override fun equals(other: Any?) = other is Fr && other.n == n && other.d == d
    override fun hashCode() = 31 * n.hashCode() + d.hashCode()
}

/** 未约分的分数文字（如三年级同分母加减不要求约分） */
private fun raw(n: Int, d: Int) = if (n == d) "1" else "$n/$d"

/** 选项：答案 + 从候选里挑 3 个不同的错误答案；候选不够时用数字微调补齐 */
private fun choice(
    r: Random,
    prompt: String,
    answer: String,
    wrong: List<String>,
    explain: String,
    same: (String, String) -> Boolean = { a, b -> a == b }
): MathQuestion {
    val picked = mutableListOf<String>()
    for (w in wrong.shuffled(r)) {
        if (picked.size == 3) break
        if (w.isBlank()) continue
        if (same(w, answer) || picked.any { same(it, w) }) continue
        picked += w
    }
    var bump = 1
    while (picked.size < 3 && bump < 50) {
        val alt = nudge(answer, bump)
        if (alt != null && !same(alt, answer) && picked.none { same(it, alt) }) picked += alt
        bump++
    }
    return MathQuestion(prompt, answer, (picked + answer).shuffled(r), explain)
}

private val FractionText = Regex("""^(\d+)/(\d+)$""")
private val NumberWithUnit = Regex("""^(-?\d+(?:\.\d+)?)(\D*)$""")

/** 补位用的干扰项：分数调分子 / 分母，数字调大小并保留单位（℃、万、° 等） */
private fun nudge(answer: String, bump: Int): String? {
    FractionText.find(answer)?.let { m ->
        val n = m.groupValues[1].toLong()
        val d = m.groupValues[2].toLong()
        return if (bump % 2 == 1) "${n + (bump + 1) / 2}/$d" else "$n/${d + bump / 2}"
    }
    NumberWithUnit.find(answer)?.let { m ->
        val v = BigDecimal(m.groupValues[1])
        val step = if (v.scale() > 0) BigDecimal.ONE.movePointLeft(v.scale()) else BigDecimal.ONE
        val alt = v + step * BigDecimal(if (bump % 2 == 1) (bump + 1) / 2 else -(bump / 2))
        if (alt.signum() <= 0 && v.signum() > 0) return null
        return dec(alt) + m.groupValues[2]
    }
    return null
}

private fun sameNumber(a: String, b: String): Boolean {
    val x = a.toBigDecimalOrNull()
    val y = b.toBigDecimalOrNull()
    return if (x != null && y != null) x.compareTo(y) == 0 else a == b
}

private fun sameFraction(a: String, b: String): Boolean {
    fun parse(s: String): Fr? = runCatching {
        if ("/" in s) s.split("/").let { Fr(it[0].trim().toLong(), it[1].trim().toLong()) } else Fr(s.trim().toLong(), 1)
    }.getOrNull()
    val x = parse(a)
    val y = parse(b)
    return if (x != null && y != null) x == y else a == b
}

private fun intWrong(ans: Long, vararg extra: Long): List<String> =
    (extra.toList() + listOf(ans + 1, ans - 1, ans + 10, ans - 10, ans + 100, ans - 100))
        .filter { it > 0 && it != ans }.map { it.toString() }

private fun decWrong(ans: BigDecimal, vararg extra: BigDecimal): List<String> =
    (extra.toList() + listOf(ans.movePointRight(1), ans.movePointLeft(1), ans + BigDecimal.ONE, ans - BigDecimal("0.1"), ans + BigDecimal("0.1")))
        .filter { it.signum() > 0 }.map { dec(it) }

// ---------- 三年级 ----------

private val Grade3 = listOf(
    MathUnit(
        "g3-add", 3, "万以内的加减法",
        listOf("相同数位对齐，从个位算起。", "加法：哪一位满十，就向前一位进一。", "减法：哪一位不够减，就向前一位借一当十。")
    ) { r ->
        val a = r.ri(125, 899)
        val b = r.ri(108, 899)
        if (r.nextBoolean()) {
            val ans = (a + b).toLong()
            choice(r, "$a + $b = ?", "$ans", intWrong(ans, ans - 10, ans - 100), "$a + $b = $ans。个位、十位满十都要向前进一。")
        } else {
            val big = maxOf(a, b) + 100
            val small = minOf(a, b)
            val ans = (big - small).toLong()
            choice(r, "$big − $small = ?", "$ans", intWrong(ans, ans + 10, ans + 100), "$big − $small = $ans。不够减时向前一位借一当十，别忘了前一位要少 1。")
        }
    },
    MathUnit(
        "g3-mul1", 3, "多位数乘一位数",
        listOf("用一位数依次去乘多位数的每一位，从个位乘起。", "哪一位乘得的积满几十，就向前一位进几。")
    ) { r ->
        val a = r.ri(23, 987)
        val b = r.ri(3, 9)
        val ans = (a * b).toLong()
        choice(r, "$a × $b = ?", "$ans", intWrong(ans, ans - b, ans + b * 10L), "$a × $b = $ans。每一位都要乘，进位的数要加到前一位的积里。")
    },
    MathUnit(
        "g3-div1", 3, "除数是一位数的除法",
        listOf("从被除数的最高位除起。", "除到哪一位，商就写在哪一位上面；不够商 1 就写 0。", "验算：商 × 除数 = 被除数。")
    ) { r ->
        val b = r.ri(2, 9)
        val q = r.ri(12, 199)
        val a = b * q
        choice(r, "$a ÷ $b = ?", "$q", intWrong(q.toLong(), q * 10L, (q / 10).toLong()), "$a ÷ $b = $q。验算：$q × $b = $a。")
    },
    MathUnit(
        "g3-rem", 3, "有余数的除法",
        listOf("余数一定要比除数小。", "被除数 = 商 × 除数 + 余数。")
    ) { r ->
        val b = r.ri(3, 9)
        val q = r.ri(3, 12)
        val rem = r.ri(1, b - 1)
        val a = b * q + rem
        choice(
            r, "$a ÷ $b = ?", "$q……$rem",
            listOf("${q - 1}……${rem + b}", "${q + 1}……$rem", "$q……${rem + 1}", "${q + 1}……${b - rem}"),
            "$b × $q = ${b * q}，$a − ${b * q} = $rem，余数 $rem 比除数 $b 小，所以商 $q 余 $rem。"
        )
    },
    MathUnit(
        "g3-mul2", 3, "两位数乘两位数",
        listOf("先用第二个因数个位上的数去乘，再用十位上的数去乘。", "用十位去乘时，积的末位要和十位对齐（相当于乘几十）。", "最后把两次的积加起来。")
    ) { r ->
        val a = r.ri(12, 99)
        val b = r.ri(13, 99)
        val ans = (a * b).toLong()
        val ones = a * (b % 10)
        val tens = a * (b / 10) * 10
        choice(r, "$a × $b = ?", "$ans", intWrong(ans, (ones + a * (b / 10)).toLong(), ans + a), "$a × ${b % 10} = $ones，$a × ${b / 10 * 10} = $tens，$ones + $tens = $ans。")
    },
    MathUnit(
        "g3-time", 3, "时、分、秒",
        listOf("1 时 = 60 分，1 分 = 60 秒。", "算经过的时间：结束时刻 − 开始时刻。")
    ) { r ->
        if (r.nextBoolean()) {
            val h = r.ri(1, 4)
            val m = r.ri(5, 55)
            val ans = (h * 60 + m).toLong()
            choice(r, "$h 时 $m 分 = ? 分", "$ans", intWrong(ans, (h * 100 + m).toLong(), (h * 10 + m).toLong(), (h * 60).toLong()), "$h 时 = ${h * 60} 分，${h * 60} + $m = $ans 分。")
        } else {
            val start = r.ri(7 * 60, 15 * 60) / 5 * 5
            val len = r.ri(25, 150) / 5 * 5
            val end = start + len
            fun t(x: Int) = "${x / 60}:${"%02d".format(x % 60)}"
            choice(r, "从 ${t(start)} 到 ${t(end)}，经过了多少分钟？", "$len", intWrong(len.toLong(), (end / 60 * 100 + end % 60 - (start / 60 * 100 + start % 60)).toLong(), len + 60L, len - 5L), "${t(end)} − ${t(start)}：一共经过 $len 分钟。注意 1 时是 60 分，不是 100 分。")
        }
    },
    MathUnit(
        "g3-perimeter", 3, "长方形和正方形的周长",
        listOf("周长是围成图形一周的长度。", "长方形周长 = (长 + 宽) × 2。", "正方形周长 = 边长 × 4。")
    ) { r ->
        if (r.nextBoolean()) {
            val l = r.ri(6, 30)
            val w = r.ri(2, l - 1)
            val ans = 2L * (l + w)
            choice(r, "长方形长 $l 厘米、宽 $w 厘米，周长是多少厘米？", "$ans", intWrong(ans, (l * w).toLong(), (l + w).toLong(), (2 * l + w).toLong()), "($l + $w) × 2 = $ans 厘米。")
        } else {
            val a = r.ri(3, 25)
            val ans = 4L * a
            choice(r, "正方形边长 $a 分米，周长是多少分米？", "$ans", intWrong(ans, (a * a).toLong(), 2L * a, ans + 4), "$a × 4 = $ans 分米。")
        }
    },
    MathUnit(
        "g3-area", 3, "面积",
        listOf("长方形面积 = 长 × 宽；正方形面积 = 边长 × 边长。", "1 平方米 = 100 平方分米，1 平方分米 = 100 平方厘米。")
    ) { r ->
        when (r.nextInt(3)) {
            0 -> {
                val l = r.ri(5, 20)
                val w = r.ri(2, l - 1)
                val ans = (l * w).toLong()
                choice(r, "长方形长 $l 米、宽 $w 米，面积是多少平方米？", "$ans", intWrong(ans, 2L * (l + w), (l + w).toLong()), "$l × $w = $ans 平方米。周长才是 (长 + 宽) × 2，别弄混。")
            }
            1 -> {
                val a = r.ri(3, 15)
                val ans = (a * a).toLong()
                choice(r, "正方形边长 $a 厘米，面积是多少平方厘米？", "$ans", intWrong(ans, 4L * a, 2L * a), "$a × $a = $ans 平方厘米。")
            }
            else -> {
                val x = r.ri(2, 9)
                choice(r, "$x 平方米 = ? 平方分米", "${x * 100}", listOf("${x * 10}", "${x * 1000}", "${x * 100 + 10}", "$x"), "1 平方米 = 100 平方分米，所以 $x 平方米 = ${x * 100} 平方分米。")
            }
        }
    },
    MathUnit(
        "g3-frac", 3, "分数的初步认识",
        listOf("把一个整体平均分成几份，每份就是它的几分之一。", "分母相同的分数相加减：分母不变，分子相加减。")
    ) { r ->
        val d = r.ri(5, 12)
        val a = r.ri(1, d - 2)
        val b = r.ri(1, d - 1 - a)
        if (r.nextBoolean()) {
            choice(r, "$a/$d + $b/$d = ?", raw(a + b, d), listOf("${a + b}/${2 * d}", raw(a + b + 1, d), raw(a + b - 1, d), "${a * b}/$d"), "分母不变，分子相加：$a + $b = ${a + b}，所以是 ${raw(a + b, d)}。", ::sameFraction)
        } else {
            val big = a + b
            choice(r, "$big/$d − $a/$d = ?", raw(b, d), listOf(raw(b + 1, d), "$b/${2 * d}", raw(big + a, d), "${b + 2}/$d"), "分母不变，分子相减：$big − $a = $b，所以是 ${raw(b, d)}。", ::sameFraction)
        }
    },
    MathUnit(
        "g3-dec", 3, "小数的初步认识",
        listOf("小数点要对齐，也就是相同数位对齐。", "一位小数：0.1 就是 1/10。")
    ) { r ->
        val a = tenths(r.ri(11, 99))
        val b = tenths(r.ri(11, 99))
        if (r.nextBoolean()) {
            val ans = a + b
            choice(r, "${dec(a)} + ${dec(b)} = ?", dec(ans), decWrong(ans, ans - BigDecimal.ONE), "小数点对齐再相加：${dec(a)} + ${dec(b)} = ${dec(ans)}。", ::sameNumber)
        } else {
            val big = a.max(b) + BigDecimal.ONE
            val small = a.min(b)
            val ans = big - small
            choice(r, "${dec(big)} − ${dec(small)} = ?", dec(ans), decWrong(ans), "小数点对齐再相减：${dec(big)} − ${dec(small)} = ${dec(ans)}。", ::sameNumber)
        }
    }
)

// ---------- 四年级 ----------

private val Grade4 = listOf(
    MathUnit(
        "g4-big", 4, "大数的认识",
        listOf("改写成以「万」为单位：去掉末尾 4 个 0，加上「万」字。", "求近似数（省略万位后面的尾数）：看千位，四舍五入。")
    ) { r ->
        if (r.nextBoolean()) {
            val w = r.ri(12, 980)
            choice(r, "${w * 10000L} 改写成以「万」为单位的数是？", "${w}万", listOf("${w * 10}万", "${w * 100}万", "${(w / 10).coerceAtLeast(1)}万", "${w}亿"), "${w * 10000L} 末尾去掉 4 个 0，就是 ${w} 万。")
        } else {
            val n = r.ri(1_023_456, 9_876_543)
            val down = n / 10000
            val ans = if (n % 10000 >= 5000) down + 1 else down
            choice(r, "$n 省略万位后面的尾数，约是多少万？", "${ans}万", listOf("${if (ans == down) down + 1 else down}万", "${n / 1000}万", "${n / 100000}万"), "看千位：$n 的千位是 ${n / 1000 % 10}，${if (n % 10000 >= 5000) "大于等于 5 要进一" else "小于 5 舍去"}，约是 $ans 万。")
        }
    },
    MathUnit(
        "g4-mul3", 4, "三位数乘两位数",
        listOf("和两位数乘两位数一样：先用个位乘，再用十位乘，积的末位对齐十位。", "因数末尾有 0 的，可以先把 0 前面的数相乘，再在积的末尾添上 0。")
    ) { r ->
        val a = r.ri(102, 999)
        val b = r.ri(12, 99)
        val ans = a.toLong() * b
        choice(r, "$a × $b = ?", "$ans", intWrong(ans, a.toLong() * (b % 10) + a.toLong() * (b / 10), ans + a, ans - a), "$a × ${b % 10} = ${a * (b % 10)}，$a × ${b / 10 * 10} = ${a * (b / 10) * 10}，相加得 $ans。")
    },
    MathUnit(
        "g4-div2", 4, "除数是两位数的除法",
        listOf("把除数看成和它接近的整十数来试商。", "试商大了就调小，小了就调大；每次余数要比除数小。")
    ) { r ->
        val b = r.ri(12, 89)
        val q = r.ri(3, 99)
        val a = b * q
        choice(r, "$a ÷ $b = ?", "$q", intWrong(q.toLong(), q * 10L, q + 2L), "$b 看作 ${(b + 5) / 10 * 10} 试商，最后得 $q。验算：$q × $b = $a。")
    },
    MathUnit(
        "g4-laws", 4, "运算定律与简便计算",
        listOf("乘法交换律、结合律：25 × 4 = 100，125 × 8 = 1000，先凑整。", "乘法分配律：a × 99 = a × 100 − a，a × 101 = a × 100 + a。", "加法凑整：先把能凑成整十、整百的数加起来。")
    ) { r ->
        when (r.nextInt(4)) {
            0 -> {
                val k = r.ri(3, 49)
                choice(r, "25 × $k × 4 = ?", "${k * 100}", intWrong(k * 100L, k * 10L, 25L * k + 4, k * 1000L), "先算 25 × 4 = 100，再乘 $k，等于 ${k * 100}。")
            }
            1 -> {
                val k = r.ri(3, 19)
                choice(r, "125 × $k × 8 = ?", "${k * 1000}", intWrong(k * 1000L, k * 100L, k * 10000L), "先算 125 × 8 = 1000，再乘 $k，等于 ${k * 1000}。")
            }
            2 -> {
                val a = r.ri(12, 89)
                val plus = r.nextBoolean()
                val ans = if (plus) a * 101L else a * 99L
                choice(r, "$a × ${if (plus) 101 else 99} = ?", "$ans", intWrong(ans, a * 100L, if (plus) a * 99L else a * 101L), if (plus) "$a × 101 = $a × 100 + $a = $ans。" else "$a × 99 = $a × 100 − $a = $ans。")
            }
            else -> {
                val a = r.ri(11, 89)
                val c = 100 - a
                val b = r.ri(12, 99)
                val ans = (a + b + c).toLong()
                choice(r, "$a + $b + $c = ?", "$ans", intWrong(ans, ans + 10, ans - 10), "先算 $a + $c = 100，再加 $b，等于 $ans。")
            }
        }
    },
    MathUnit(
        "g4-mixed", 4, "四则混合运算",
        listOf("没有括号：先算乘除，后算加减。", "有括号：先算括号里面的。", "同级运算从左往右算。")
    ) { r ->
        when (r.nextInt(3)) {
            0 -> {
                val c = r.ri(2, 9)
                val b = c * r.ri(2, 12)
                val d = r.ri(2, 9)
                val a = r.ri(10, 99)
                val ans = (a + b / c * d).toLong()
                val leftToRight = ((a + b) / c.toDouble() * d).let { if (it % 1.0 == 0.0) it.toLong() else ans + 7 }
                choice(r, "$a + $b ÷ $c × $d = ?", "$ans", intWrong(ans, leftToRight, ans + d), "先算 $b ÷ $c = ${b / c}，再算 ${b / c} × $d = ${b / c * d}，最后 $a + ${b / c * d} = $ans。")
            }
            1 -> {
                val a = r.ri(5, 30)
                val b = r.ri(5, 30)
                val c = r.ri(2, 9)
                val ans = ((a + b) * c).toLong()
                choice(r, "($a + $b) × $c = ?", "$ans", intWrong(ans, (a + b * c).toLong(), ans + c), "先算括号里 $a + $b = ${a + b}，再乘 $c，等于 $ans。")
            }
            else -> {
                val b = r.ri(2, 9)
                val c = r.ri(2, 9)
                val a = r.ri(b * c + 5, 200)
                val ans = (a - b * c).toLong()
                choice(r, "$a − $b × $c = ?", "$ans", intWrong(ans, ((a - b) * c).toLong(), ans + 10), "先算乘法 $b × $c = ${b * c}，再算 $a − ${b * c} = $ans。")
            }
        }
    },
    MathUnit(
        "g4-dec", 4, "小数加减法",
        listOf("小数点对齐，相同数位就对齐了。", "位数不同时，可以在末尾补 0 再算。", "得数末尾的 0 可以去掉。")
    ) { r ->
        val a = hundredths(r.ri(305, 2999))
        val b = tenths(r.ri(11, 299))
        if (r.nextBoolean()) {
            val ans = a + b
            choice(r, "${dec(a)} + ${dec(b)} = ?", dec(ans), decWrong(ans, a + b.movePointLeft(1), ans + BigDecimal("0.01")), "小数点对齐（${dec(b)} 可以看作 ${b.setScale(2).toPlainString()}），得 ${dec(ans)}。", ::sameNumber)
        } else {
            val big = a.max(b) + BigDecimal.ONE
            val small = a.min(b)
            val ans = big - small
            choice(r, "${dec(big)} − ${dec(small)} = ?", dec(ans), decWrong(ans, ans - BigDecimal("0.01")), "小数点对齐，位数不够补 0，${dec(big)} − ${dec(small)} = ${dec(ans)}。", ::sameNumber)
        }
    },
    MathUnit(
        "g4-triangle", 4, "三角形",
        listOf("三角形的内角和是 180°。", "等腰三角形的两个底角相等。", "四边形的内角和是 360°。")
    ) { r ->
        if (r.nextBoolean()) {
            val a = r.ri(25, 100)
            val b = r.ri(20, 150 - a)
            val ans = (180 - a - b).toLong()
            choice(r, "三角形的两个角分别是 $a° 和 $b°，第三个角是多少度？", "${ans}°", intWrong(ans, 360L - a - b, 90L - a - b + 90, ans + 10).map { "$it°" }, "180° − $a° − $b° = $ans°。")
        } else {
            val top = r.ri(10, 80) * 2
            val ans = ((180 - top) / 2).toLong()
            choice(r, "等腰三角形的顶角是 $top°，一个底角是多少度？", "${ans}°", listOf("${180 - top}°", "${top / 2}°", "${ans + 10}°", "${(360 - top) / 2}°"), "两个底角相等：(180° − $top°) ÷ 2 = $ans°。")
        }
    },
    MathUnit(
        "g4-avg", 4, "平均数",
        listOf("平均数 = 总数 ÷ 份数。", "平均数在最大数和最小数之间。")
    ) { r ->
        val n = r.ri(3, 5)
        val avg = r.ri(60, 95)
        val devs = MutableList(n - 1) { r.ri(-8, 8) }
        devs += -devs.sum()
        val nums = devs.map { avg + it }
        val sum = nums.sum()
        choice(r, "${nums.joinToString("、")} 的平均数是多少？", "$avg", intWrong(avg.toLong(), sum.toLong(), avg + 2L, avg - 2L), "总数 $sum ÷ $n 个 = $avg。")
    }
)

// ---------- 五年级 ----------

private val Grade5 = listOf(
    MathUnit(
        "g5-decmul", 5, "小数乘法",
        listOf("先按整数乘法算出积。", "再数两个因数一共有几位小数，就从积的右边数出几位点上小数点。")
    ) { r ->
        val a = tenths(r.ri(11, 99))
        val b = if (r.nextBoolean()) tenths(r.ri(11, 59)) else BigDecimal(r.ri(3, 9))
        val ans = a * b
        val places = a.scale() + b.scale()
        choice(r, "${dec(a)} × ${dec(b)} = ?", dec(ans), decWrong(ans), "按整数算 ${a.unscaledValue()} × ${b.unscaledValue()} = ${a.unscaledValue() * b.unscaledValue()}，因数一共 $places 位小数，积就是 ${dec(ans)}。", ::sameNumber)
    },
    MathUnit(
        "g5-decdiv", 5, "小数除法",
        listOf("除数是小数：把除数和被除数同时扩大相同的倍数，使除数变成整数。", "商的小数点和被除数的小数点对齐。")
    ) { r ->
        val b = tenths(r.ri(2, 49))
        val q = if (r.nextBoolean()) tenths(r.ri(11, 99)) else BigDecimal(r.ri(2, 30))
        val a = b * q
        choice(r, "${dec(a)} ÷ ${dec(b)} = ?", dec(q), decWrong(q), "除数和被除数同时乘 10：${dec(a.movePointRight(1))} ÷ ${dec(b.movePointRight(1))} = ${dec(q)}。", ::sameNumber)
    },
    MathUnit(
        "g5-eq", 5, "简易方程",
        listOf("方程两边同时加、减、乘、除同一个数（0 除外），左右仍然相等。", "解完把 x 代回去检验。")
    ) { r ->
        val x = r.ri(2, 25)
        when (r.nextInt(3)) {
            0 -> {
                val a = r.ri(2, 9)
                val b = r.ri(3, 40)
                val c = a * x + b
                choice(r, "解方程：${a}x + $b = $c，x = ?", "$x", intWrong(x.toLong(), ((c + b) / a).toLong(), (c - b).toLong()), "两边同时减 $b：${a}x = ${c - b}；两边同时除以 $a：x = $x。")
            }
            1 -> {
                val b = r.ri(3, 40)
                val c = x + b
                val wrong = listOf("${c + b}", "$c", "${x + 1}", "${x - 1}")
                val ans = c - b
                choice(r, "解方程：x + $b = $c，x = ?", "$ans", wrong, "两边同时减 $b：x = $c − $b = $ans。")
            }
            else -> {
                val a = r.ri(2, 9)
                val c = a * x
                choice(r, "解方程：${a}x = $c，x = ?", "$x", intWrong(x.toLong(), (c - a).toLong(), c.toLong() * a), "两边同时除以 $a：x = $c ÷ $a = $x。")
            }
        }
    },
    MathUnit(
        "g5-area", 5, "多边形的面积",
        listOf("平行四边形面积 = 底 × 高。", "三角形面积 = 底 × 高 ÷ 2。", "梯形面积 = (上底 + 下底) × 高 ÷ 2。")
    ) { r ->
        when (r.nextInt(3)) {
            0 -> {
                val b = r.ri(4, 20)
                val h = r.ri(3, 15)
                val ans = (b * h).toLong()
                choice(r, "平行四边形底 $b 厘米、高 $h 厘米，面积是多少平方厘米？", "$ans", intWrong(ans, ans / 2, 2L * (b + h)), "底 × 高：$b × $h = $ans 平方厘米。")
            }
            1 -> {
                val b = r.ri(2, 10) * 2
                val h = r.ri(3, 15)
                val ans = (b * h / 2).toLong()
                choice(r, "三角形底 $b 米、高 $h 米，面积是多少平方米？", "$ans", intWrong(ans, ans * 2, (b + h).toLong()), "底 × 高 ÷ 2：$b × $h ÷ 2 = $ans 平方米。")
            }
            else -> {
                val a = r.ri(3, 12)
                val b = a + r.ri(2, 10)
                val h = r.ri(2, 8) * 2
                val ans = ((a + b) * h / 2).toLong()
                choice(r, "梯形上底 $a 分米、下底 $b 分米、高 $h 分米，面积是多少平方分米？", "$ans", intWrong(ans, ans * 2, (a * b * h / 2).toLong()), "($a + $b) × $h ÷ 2 = $ans 平方分米。")
            }
        }
    },
    MathUnit(
        "g5-factor", 5, "因数与倍数",
        listOf("最大公因数：两个数公有的因数中最大的一个。", "最小公倍数：两个数公有的倍数中最小的一个。", "两数之积 = 最大公因数 × 最小公倍数。")
    ) { r ->
        val pairs = listOf(2 to 3, 2 to 5, 3 to 4, 3 to 5, 2 to 7, 4 to 5, 3 to 7, 5 to 6)
        val (m, n) = pairs.random(r)
        val g = r.ri(2, 8)
        val a = g * m
        val b = g * n
        val lcm = g * m * n
        if (r.nextBoolean()) {
            choice(r, "$a 和 $b 的最大公因数是多少？", "$g", listOf("$lcm", "${a * b}", "$m", "${g * 2}", "1"), "$a = $g × $m，$b = $g × $n，公有的最大因数是 $g。")
        } else {
            choice(r, "$a 和 $b 的最小公倍数是多少？", "$lcm", listOf("${a * b}", "$g", "${lcm * 2}", "${lcm / m}"), "最大公因数是 $g，最小公倍数 = $g × $m × $n = $lcm。")
        }
    },
    MathUnit(
        "g5-reduce", 5, "约分",
        listOf("分子和分母同时除以它们的公因数，分数大小不变。", "约分要约到分子分母只有公因数 1，叫最简分数。")
    ) { r ->
        val f = Fr(r.ri(1, 8).toLong(), r.ri(2, 9).toLong())
        val (n, d) = if (f.n < f.d) f.n to f.d else 1L to (f.n + 1)
        val k = r.ri(2, 9)
        val simplest = Fr(n, d)
        val half = if (k % 2 == 0 && k > 2) "${n * k / 2}/${d * k / 2}" else "${n + 1}/$d"
        choice(r, "${n * k}/${d * k} 约分成最简分数是？", "$simplest", listOf(half, "$d/$n", "${simplest.n}/${simplest.d + 1}", "${simplest.n + 1}/${simplest.d}"), "分子分母同时除以 $k（最大公因数）：${n * k}/${d * k} = $simplest。", ::sameFraction)
    },
    MathUnit(
        "g5-fracadd", 5, "异分母分数加减法",
        listOf("分母不同，先通分成分母相同的分数，再加减。", "结果能约分的要约成最简分数。")
    ) { r ->
        val b = r.ri(2, 9)
        var d = r.ri(2, 9)
        if (d == b) d = if (b < 9) b + 1 else b - 1
        val x = Fr(r.ri(1, b - 1).toLong(), b.toLong())
        val y = Fr(r.ri(1, d - 1).toLong(), d.toLong())
        val plus = r.nextBoolean() || x == y
        val (p, q) = if (plus) x to y else if (x.n * y.d > y.n * x.d) x to y else y to x
        val ans = if (plus) p + q else p - q
        val sign = if (plus) "+" else "−"
        val naive = if (plus) "${p.n + q.n}/${p.d + q.d}" else "${kotlin.math.abs(p.n - q.n)}/${p.d + q.d}"
        choice(r, "$p $sign $q = ?", "$ans", listOf(naive, "${ans.n + 1}/${ans.d}", "${ans.n}/${ans.d + 1}", "${ans.n * 2}/${ans.d}"), "通分：分母 ${p.d} 和 ${q.d} 通分成 ${p.d * q.d / gcd(p.d, q.d)}，算出 $ans。分母不能直接相加！", ::sameFraction)
    },
    MathUnit(
        "g5-cuboid", 5, "长方体和正方体",
        listOf("长方体体积 = 长 × 宽 × 高；正方体体积 = 棱长 × 棱长 × 棱长。", "长方体表面积 = (长×宽 + 长×高 + 宽×高) × 2；正方体表面积 = 棱长 × 棱长 × 6。")
    ) { r ->
        if (r.nextBoolean()) {
            val l = r.ri(3, 12)
            val w = r.ri(2, 10)
            val h = r.ri(2, 10)
            val v = (l * w * h).toLong()
            val s = 2L * (l * w + l * h + w * h)
            if (r.nextBoolean()) {
                choice(r, "长方体长 $l 厘米、宽 $w 厘米、高 $h 厘米，体积是多少立方厘米？", "$v", intWrong(v, s, (l + w + h).toLong(), 4L * (l + w + h)), "$l × $w × $h = $v 立方厘米。")
            } else {
                choice(r, "长方体长 $l 厘米、宽 $w 厘米、高 $h 厘米，表面积是多少平方厘米？", "$s", intWrong(s, v, s / 2), "($l×$w + $l×$h + $w×$h) × 2 = $s 平方厘米。")
            }
        } else {
            val a = r.ri(2, 9)
            if (r.nextBoolean()) {
                choice(r, "正方体棱长 $a 分米，体积是多少立方分米？", "${a * a * a}", intWrong(a * a * a.toLong(), 6L * a * a, a * a.toLong(), 12L * a), "$a × $a × $a = ${a * a * a} 立方分米。")
            } else {
                choice(r, "正方体棱长 $a 分米，表面积是多少平方分米？", "${6 * a * a}", intWrong(6L * a * a, a * a * a.toLong(), 4L * a * a), "$a × $a × 6 = ${6 * a * a} 平方分米。")
            }
        }
    }
)

// ---------- 六年级 ----------

private val Grade6 = listOf(
    MathUnit(
        "g6-fracmul", 6, "分数乘法",
        listOf("分数乘分数：分子乘分子，分母乘分母，能约分的先约分。", "求一个数的几分之几是多少，用乘法。")
    ) { r ->
        if (r.nextBoolean()) {
            val x = Fr(r.ri(1, 6).toLong(), r.ri(2, 9).toLong()).let { if (it.d == 1L) Fr(1, 2) else it }
            val y = Fr(r.ri(1, 6).toLong(), r.ri(2, 9).toLong()).let { if (it.d == 1L) Fr(2, 3) else it }
            val ans = x * y
            choice(r, "$x × $y = ?", "$ans", listOf("${x.n * y.n}/${x.d + y.d}", "${x.n * y.d}/${x.d * y.n}", "${x.n + y.n}/${x.d * y.d}", "${ans.n + 1}/${ans.d}"), "分子乘分子、分母乘分母：$x × $y = $ans。", ::sameFraction)
        } else {
            val d = r.ri(2, 9)
            val n = r.ri(1, d - 1)
            val whole = d * r.ri(2, 15)
            val ans = whole / d * n
            choice(r, "$whole 的 $n/$d 是多少？", "$ans", intWrong(ans.toLong(), (whole / d).toLong(), (whole * d / n).toLong()), "$whole × $n/$d = $whole ÷ $d × $n = $ans。")
        }
    },
    MathUnit(
        "g6-fracdiv", 6, "分数除法",
        listOf("除以一个不为 0 的数，等于乘这个数的倒数。", "倒数：分子分母交换位置，如 3/4 的倒数是 4/3。")
    ) { r ->
        val x = Fr(r.ri(1, 7).toLong(), r.ri(2, 9).toLong()).let { if (it.d == 1L) Fr(3, 4) else it }
        val y = Fr(r.ri(1, 7).toLong(), r.ri(2, 9).toLong()).let { if (it.d == 1L || it == x) Fr(2, 5) else it }
        val ans = x / y
        choice(r, "$x ÷ $y = ?", "$ans", listOf("${(x * y)}", "${Fr(x.d * y.n, x.n * y.d)}", "${ans.n + 1}/${ans.d}", "${ans.n}/${ans.d + 1}"), "$x ÷ $y = $x × ${Fr(y.d, y.n)} = $ans。", ::sameFraction)
    },
    MathUnit(
        "g6-ratio", 6, "比",
        listOf("比的前项和后项同时乘或除以相同的数（0 除外），比值不变。", "按比分配：先求总份数，再求每份是多少。")
    ) { r ->
        val pairs = listOf(2 to 3, 3 to 4, 1 to 2, 3 to 5, 2 to 5, 4 to 5, 1 to 3, 5 to 7)
        val (m, n) = pairs.random(r)
        if (r.nextBoolean()) {
            val k = r.ri(2, 12)
            choice(r, "化简比 ${m * k} : ${n * k} = ?", "$m : $n", listOf("$n : $m", "${m * 2} : ${n * 2}".takeIf { k % 2 != 0 } ?: "${m + 1} : $n", "${m * k} : $n", "$m : ${n + 1}"), "前项后项同时除以 $k：${m * k} : ${n * k} = $m : $n。")
        } else {
            val each = r.ri(2, 20)
            val total = (m + n) * each
            choice(r, "把 $total 个苹果按 $m : $n 分给两人，多的那人分到几个？", "${n * each}", intWrong((n * each).toLong(), (m * each).toLong(), (total / 2).toLong(), (total / n).toLong()), "总份数 $m + $n = ${m + n}，每份 $total ÷ ${m + n} = $each，多的人 $n × $each = ${n * each} 个。")
        }
    },
    MathUnit(
        "g6-percent", 6, "百分数",
        listOf("求一个数的百分之几是多少：这个数 × 百分数。", "打几折就是原价的百分之几十，如八折 = 80%。")
    ) { r ->
        when (r.nextInt(3)) {
            0 -> {
                val a = r.ri(2, 25) * 20
                val p = r.ri(1, 19) * 5
                val ans = a * p / 100
                choice(r, "$a 的 $p% 是多少？", "$ans", intWrong(ans.toLong(), (a * p / 10).toLong(), (a - ans).toLong(), (a / p.coerceAtLeast(1)).toLong()), "$a × $p% = $a × $p ÷ 100 = $ans。")
            }
            1 -> {
                val price = r.ri(3, 30) * 10
                val zhe = r.ri(5, 9)
                val ans = price * zhe / 10
                choice(r, "原价 $price 元，打 $zhe 折后是多少元？", "$ans", intWrong(ans.toLong(), (price - zhe).toLong(), (price - ans).toLong(), (price * (10 - zhe) / 10 + 1).toLong()), "打 $zhe 折就是原价的 ${zhe * 10}%：$price × ${zhe * 10}% = $ans 元。")
            }
            else -> {
                val b = listOf(20, 25, 40, 50, 80).random(r)
                val p = listOf(10, 20, 25, 40, 50, 60, 75, 80).filter { b * it % 100 == 0 }.random(r)
                val a = b * p / 100
                choice(r, "$a 是 $b 的百分之几？", "$p%", listOf("${b * 100 / a.coerceAtLeast(1)}%", "${100 - p}%", "${p / 10}%", "${p + 5}%"), "$a ÷ $b = ${dec(BigDecimal(a).divide(BigDecimal(b), 2, RoundingMode.HALF_UP))} = $p%。")
            }
        }
    },
    MathUnit(
        "g6-circle", 6, "圆",
        listOf("圆的周长 = 2 × π × r = π × d。", "圆的面积 = π × r × r。", "π 取 3.14。")
    ) { r ->
        val radius = r.ri(1, 10)
        val rr = BigDecimal(radius)
        if (r.nextBoolean()) {
            val ans = Pi * rr * BigDecimal(2)
            choice(r, "半径 $radius 厘米的圆，周长是多少厘米？（π 取 3.14）", dec(ans), listOf(dec(Pi * rr), dec(Pi * rr * rr), dec(ans + BigDecimal.ONE), dec(Pi * rr * BigDecimal(4))), "2 × 3.14 × $radius = ${dec(ans)} 厘米。", ::sameNumber)
        } else {
            val ans = Pi * rr * rr
            choice(r, "半径 $radius 米的圆，面积是多少平方米？（π 取 3.14）", dec(ans), listOf(dec(Pi * rr * BigDecimal(2)), dec(Pi * rr * BigDecimal(4) * rr), dec(Pi * rr), dec(ans + BigDecimal.TEN)), "3.14 × $radius × $radius = ${dec(ans)} 平方米。", ::sameNumber)
        }
    },
    MathUnit(
        "g6-prop", 6, "比例",
        listOf("在比例里，两个外项的积等于两个内项的积。", "解比例：x : a = b : c，就是 x × c = a × b。")
    ) { r ->
        val x = r.ri(2, 20)
        val a = r.ri(2, 12)
        val k = r.ri(2, 6)
        val b = x * k
        val c = a * k
        choice(r, "解比例：x : $a = $b : $c，x = ?", "$x", intWrong(x.toLong(), (a * c / b.coerceAtLeast(1)).toLong(), (b * c / a).toLong(), x * 2L), "外项积 = 内项积：x × $c = $a × $b，x = ${a * b} ÷ $c = $x。")
    },
    MathUnit(
        "g6-neg", 6, "负数",
        listOf("零下 3℃ 记作 −3℃。", "在数轴上，越往右的数越大：−2 > −5。", "从 −3 升高 8，就是 −3 + 8 = 5。")
    ) { r ->
        if (r.nextBoolean()) {
            val start = r.ri(-12, -1)
            val up = r.ri(3, 15)
            val ans = start + up
            choice(r, "早上气温 ${start}℃，中午升高了 ${up}℃，中午气温是多少？", "${ans}℃", listOf("${-ans}℃", "${start - up}℃", "${-start + up}℃", "${ans + 1}℃").distinct(), "$start + $up = $ans，中午是 ${ans}℃。")
        } else {
            val a = r.ri(-9, -1)
            var b = r.ri(-9, -1)
            if (a == b) b = if (a > -9) a - 1 else a + 1
            val bigger = maxOf(a, b)
            choice(r, "$a 和 $b 哪个大？", "$bigger", listOf("${minOf(a, b)}", "一样大", "${-bigger}"), "负数离 0 越近越大，所以 $bigger 更大。")
        }
    },
    MathUnit(
        "g6-cylinder", 6, "圆柱与圆锥",
        listOf("圆柱体积 = 底面积 × 高 = π × r × r × h。", "圆锥体积 = 和它等底等高的圆柱体积的 1/3。")
    ) { r ->
        val radius = r.ri(1, 6)
        val h = r.ri(1, 5) * 3
        val cylinder = Pi * BigDecimal(radius * radius * h)
        if (r.nextBoolean()) {
            choice(r, "圆柱底面半径 $radius 厘米、高 $h 厘米，体积是多少立方厘米？（π 取 3.14）", dec(cylinder), listOf(dec(cylinder.divide(BigDecimal(3))), dec(Pi * BigDecimal(2 * radius * h)), dec(Pi * BigDecimal(4 * radius * radius * h))), "3.14 × $radius × $radius × $h = ${dec(cylinder)} 立方厘米。", ::sameNumber)
        } else {
            val cone = cylinder.divide(BigDecimal(3))
            choice(r, "圆锥底面半径 $radius 厘米、高 $h 厘米，体积是多少立方厘米？（π 取 3.14）", dec(cone), listOf(dec(cylinder), dec(cone.divide(BigDecimal(2))), dec(cone + BigDecimal.ONE)), "1/3 × 3.14 × $radius × $radius × $h = ${dec(cone)} 立方厘米。", ::sameNumber)
        }
    }
)

val MathUnits: List<MathUnit> = Grade3 + Grade4 + Grade5 + Grade6

fun buildMathUnitRound(unit: MathUnit, random: Random): List<MathQuestion> {
    val seen = mutableSetOf<String>()
    val out = mutableListOf<MathQuestion>()
    var tries = 0
    while (out.size < MathUnitRoundSize && tries < 200) {
        tries++
        val q = unit.generate(random)
        if (seen.add(q.prompt)) out += q
    }
    return out
}
