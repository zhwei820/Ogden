package com.example.ogdenkids

import kotlin.random.Random

enum class MathOp(val zh: String) { Add("加法"), Sub("减法"), Mixed("加减混合") }

/** 加法看个位是否进位，减法看个位是否退位。 */
enum class CarryMode(val zh: String) { Without("不进退位"), With("含进退位"), Any("随机") }

enum class MathLevel(val zh: String, val seconds: Int) { Easy("初级", 20), Medium("中级", 15), Hard("高级", 8) }

enum class PromptMode(val zh: String) { Text("文字"), VoiceZh("中文语音"), VoiceEn("英文语音") }

data class MathProblem(val a: Int, val b: Int, val plus: Boolean) {
    val answer: Int get() = if (plus) a + b else a - b
    val text: String get() = "$a ${if (plus) "+" else "−"} $b"

    val englishClips: List<String>
        get() = listOf(numberWord(a), if (plus) "plus" else "minus", numberWord(b))

    val chineseClips: List<String>
        get() = listOf(chineseNumber(a), if (plus) "加" else "减", chineseNumber(b))

    /** assets/math/<lang>/ 下的录音文件名（scripts/gen_math_audio.py 生成） */
    val clipFiles: List<String>
        get() = listOf("$a", if (plus) "add" else "sub", "$b")
}

const val MathDrillSize = 20

private val ChineseDigits = listOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")

fun chineseNumber(n: Int): String = when {
    n == 100 -> "一百"
    n < 10 -> ChineseDigits[n]
    else -> (if (n < 20) "" else ChineseDigits[n / 10]) + "十" + (if (n % 10 == 0) "" else ChineseDigits[n % 10])
}

fun hasCarry(p: MathProblem): Boolean =
    if (p.plus) p.a % 10 + p.b % 10 >= 10 else p.a % 10 < p.b % 10

/** 生成一组 100 以内的口算题：结果在 0..100，同一组不重复。 */
fun buildMathDrill(op: MathOp, carry: CarryMode, random: Random, size: Int = MathDrillSize): List<MathProblem> {
    val result = LinkedHashSet<MathProblem>()
    while (result.size < size) {
        val plus = when (op) {
            MathOp.Add -> true
            MathOp.Sub -> false
            MathOp.Mixed -> random.nextBoolean()
        }
        val wantCarry = when (carry) {
            CarryMode.Without -> false
            CarryMode.With -> true
            CarryMode.Any -> random.nextBoolean()
        }
        result += randomProblem(plus, wantCarry, random)
    }
    return result.toList()
}

private fun randomProblem(plus: Boolean, carry: Boolean, random: Random): MathProblem {
    while (true) {
        val p = if (plus) {
            val a = random.nextInt(1, 100)
            MathProblem(a, random.nextInt(1, 101 - a), true)
        } else {
            val a = random.nextInt(2, 101)
            MathProblem(a, random.nextInt(1, a), false)
        }
        // 不进位的加法和不能是 100（个位 0+0 也会向百位进）
        if (hasCarry(p) == carry && (carry || !p.plus || p.answer < 100)) return p
    }
}
