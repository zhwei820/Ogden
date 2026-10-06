package com.example.ogdenkids

/** 语体标签：儿语 / 口语，并给出书面常用的正式说法（也是词库词）。 */
data class Register(val tag: String, val formal: String)

private val Registers = mapOf(
    "tummy" to Register("儿语", "stomach"),
    "yummy" to Register("儿语", "delicious"),
    "mommy" to Register("儿语", "mother"),
    "daddy" to Register("儿语", "father"),
    "granny" to Register("儿语", "grandmother"),
    "doggy" to Register("儿语", "dog"),
    "kitty" to Register("儿语", "cat"),
    "bunny" to Register("儿语", "rabbit"),
    "piggy" to Register("儿语", "pig"),
    "horsey" to Register("儿语", "horse"),
    "birdie" to Register("儿语", "bird"),
    "bye-bye" to Register("儿语", "goodbye"),
    "jammies" to Register("儿语", "pajamas"),
    "mom" to Register("口语", "mother"),
    "dad" to Register("口语", "father"),
    "grandma" to Register("口语", "grandmother"),
    "grandpa" to Register("口语", "grandfather")
)

fun registerOf(word: String): Register? = Registers[word.lowercase()]

/** 测试用：所有标了语体的词及其正式说法 */
fun allRegisters(): Map<String, Register> = Registers
