package com.example.ogdenkids

import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

enum class MathOp(val zh: String) { Add("加法"), Sub("减法"), Mixed("加减混合"), MulDivAddSub("乘除加减混合") }

enum class MathOperator(val sign: String, val en: String, val zh: String, val clip: String) {
    Plus("+", "plus", "加", "add"), Minus("−", "minus", "减", "sub"),
    Times("×", "times", "乘", "mul"), Divide("÷", "divided by", "除以", "div");

    fun apply(x: Int, y: Int): Int = when (this) {
        Plus -> x + y
        Minus -> x - y
        Times -> x * y
        Divide -> x / y
    }
}

/** 加法看个位是否进位，减法看个位是否退位。 */
enum class CarryMode(val zh: String) { With("含进退位"), Any("随机") }

/** 最好成绩按 ordinal 存，新难度只能往后加；seconds = 0 表示不计时。 */
enum class MathLevel(val zh: String, val seconds: Int) {
    Easy("初级", 20), Medium("中级", 15), Hard("高级", 8), Untimed("不计时", 0);

    val timed: Boolean get() = seconds > 0
    val label: String get() = if (timed) "$zh ${seconds}秒" else zh
}

enum class PromptMode(val zh: String) { Text("文字"), VoiceZh("中文语音"), VoiceEn("英文语音") }

/** 两个数一步运算，或三个数「一步乘除 + 一步加减」（先乘除后加减，不带括号）。 */
data class MathProblem(val numbers: List<Int>, val ops: List<MathOperator>) {
    constructor(a: Int, b: Int, plus: Boolean) : this(listOf(a, b), listOf(if (plus) MathOperator.Plus else MathOperator.Minus))

    /** 最后算的一步：三个数时先把乘除那步算成一个数。 */
    val finalStep: Triple<Int, MathOperator, Int>
        get() = when {
            ops.size == 1 -> Triple(numbers[0], ops[0], numbers[1])
            ops[0] == MathOperator.Times || ops[0] == MathOperator.Divide -> Triple(ops[0].apply(numbers[0], numbers[1]), ops[1], numbers[2])
            else -> Triple(numbers[0], ops[0], ops[1].apply(numbers[1], numbers[2]))
        }

    val answer: Int get() = finalStep.let { (x, op, y) -> op.apply(x, y) }
    val text: String get() = interleave({ "$it" }, { it.sign }).joinToString(" ")

    val englishClips: List<String> get() = interleave(::numberWord) { it.en }
    val chineseClips: List<String> get() = interleave(::chineseNumber) { it.zh }

    /** assets/math/<lang>/ 下的录音文件名（scripts/gen_math_audio.py 生成） */
    val clipFiles: List<String> get() = interleave({ "$it" }, { it.clip })

    private fun interleave(number: (Int) -> String, op: (MathOperator) -> String): List<String> =
        numbers.flatMapIndexed { i, n -> listOfNotNull(number(n), ops.getOrNull(i)?.let(op)) }
}

const val MathDrillSize = 20

/** 一天的口算打卡，存成 "局数,全对局数,答对题数,总题数"；只记做完的局。 */
data class MathDayLog(val rounds: Int = 0, val perfect: Int = 0, val correct: Int = 0, val total: Int = 0) {
    fun plusRound(correct: Int, total: Int) =
        MathDayLog(rounds + 1, perfect + if (correct == total) 1 else 0, this.correct + correct, this.total + total)

    fun encode() = "$rounds,$perfect,$correct,$total"

    companion object {
        fun parse(raw: String?): MathDayLog? =
            raw?.split(',')?.mapNotNull { it.toIntOrNull() }?.takeIf { it.size == 4 }?.let { MathDayLog(it[0], it[1], it[2], it[3]) }
    }
}

/** 本地时区的日期 "yyyy-MM-dd"，作打卡记录的键。 */
fun dayKey(day: Calendar): String =
    String.format(Locale.US, "%04d-%02d-%02d", day.get(Calendar.YEAR), day.get(Calendar.MONTH) + 1, day.get(Calendar.DAY_OF_MONTH))

/** 连续打卡天数；今天还没练不算断，从昨天往前数。 */
fun mathStreak(today: Calendar, checked: (String) -> Boolean): Int {
    val day = today.clone() as Calendar
    if (!checked(dayKey(day))) day.add(Calendar.DAY_OF_MONTH, -1)
    var count = 0
    while (checked(dayKey(day))) {
        count++
        day.add(Calendar.DAY_OF_MONTH, -1)
    }
    return count
}

private val ChineseDigits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")

fun chineseNumber(n: Int): String = when {
    n == 100 -> "一百"
    n < 10 -> ChineseDigits[n]
    else -> (if (n < 20) "" else ChineseDigits[n / 10]) + "十" + (if (n % 10 == 0) "" else ChineseDigits[n % 10])
}

/** 看最后那步加减；乘除加减混合题即「积 / 商」与另一个数相加减时的个位。 */
fun hasCarry(p: MathProblem): Boolean {
    val (x, op, y) = p.finalStep
    return if (op == MathOperator.Plus) x % 10 + y % 10 >= 10 else x % 10 < y % 10
}

/** 生成一组 100 以内的口算题：结果在 0..100，同一组不重复。 */
fun buildMathDrill(op: MathOp, carry: CarryMode, random: Random, size: Int = MathDrillSize): List<MathProblem> {
    val result = LinkedHashSet<MathProblem>()
    while (result.size < size) {
        val plus = when (op) {
            MathOp.Add -> true
            MathOp.Sub -> false
            MathOp.Mixed, MathOp.MulDivAddSub -> random.nextBoolean()
        }
        val wantCarry = when (carry) {
            CarryMode.With -> true
            CarryMode.Any -> random.nextBoolean()
        }
        result += randomProblem(plus, wantCarry, op == MathOp.MulDivAddSub, random)
    }
    return result.toList()
}

private fun randomProblem(plus: Boolean, carry: Boolean, mulDiv: Boolean, random: Random): MathProblem {
    while (true) {
        val p = if (mulDiv) randomMulDivAddSub(plus, random) else randomAddSub(plus, random)
        val (x, _, y) = p.finalStep
        // 不进位的加法和不能是 100（个位 0+0 也会向百位进）
        if (hasCarry(p) == carry && (carry || !plus || x + y < 100)) return p
    }
}

private fun randomAddSub(plus: Boolean, random: Random): MathProblem = if (plus) {
    val a = random.nextInt(1, 100)
    MathProblem(a, random.nextInt(1, 101 - a), true)
} else {
    val a = random.nextInt(2, 101)
    MathProblem(a, random.nextInt(1, a), false)
}

/** 表内乘除（因数、除数、商都在 2..9）得 m，再与 c 加减；乘除总写在前面。 */
private fun randomMulDivAddSub(plus: Boolean, random: Random): MathProblem {
    val x = random.nextInt(2, 10)
    val y = random.nextInt(2, 10)
    val (pair, mulOp) = if (random.nextBoolean()) listOf(x, y) to MathOperator.Times else listOf(x * y, y) to MathOperator.Divide
    val m = mulOp.apply(pair[0], pair[1])
    val c = if (plus) random.nextInt(1, 101 - m) else random.nextInt(1, m)
    return MathProblem(pair + c, listOf(mulOp, if (plus) MathOperator.Plus else MathOperator.Minus))
}
