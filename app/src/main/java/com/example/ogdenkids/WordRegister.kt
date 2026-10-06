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
    "grandpa" to Register("口语", "grandfather"),
    "mum" to Register("口语", "mother"),
    "kid" to Register("口语", "child"),
    "guy" to Register("口语", "man"),
    "yeah" to Register("口语", "yes"),
    "yep" to Register("口语", "yes"),
    "nope" to Register("口语", "no"),
    "hi" to Register("口语", "hello"),
    "hey" to Register("口语", "hello"),
    "bye" to Register("口语", "goodbye"),
    "buddy" to Register("口语", "friend"),
    "pal" to Register("口语", "friend"),
    "bro" to Register("口语", "brother"),
    "sis" to Register("口语", "sister"),
    "cop" to Register("口语", "police"),
    "veggie" to Register("口语", "vegetable"),
    "stuff" to Register("口语", "thing"),
    "awesome" to Register("口语", "great"),
    "okay" to Register("口语", "fine"),
    "thanks" to Register("口语", "thank"),
    "folks" to Register("口语", "parent")
)

fun registerOf(word: String): Register? = Registers[word.lowercase()]

/** 测试用：所有标了语体的词及其正式说法 */
fun allRegisters(): Map<String, Register> = Registers
