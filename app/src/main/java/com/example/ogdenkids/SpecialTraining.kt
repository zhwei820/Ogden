package com.example.ogdenkids

import kotlin.random.Random

enum class SpecialTopic(val key: String, val zh: String, val en: String, val icon: String) {
    Position("position", "方位", "Where Is It?", "📦"),
    Numbers("numbers", "数字", "Numbers", "🔢"),
    Colors("colors", "颜色", "Colors", "🎨"),
    Time("time", "时间", "What Time Is It?", "⏰")
}

data class Thing(val emoji: String, val name: String, val zh: String)

/** 界面按区域摆放物体；同区域的方位在「点一点放在哪」题里不会同时出现。 */
enum class Region { Top, Bottom, Center, Left, Right, Front, Back }

enum class Relation(val phrase: String, val zh: String, val level: Int, val region: Region) {
    In("in", "在……里面", 1, Region.Center),
    On("on", "在……上面", 1, Region.Top),
    Under("under", "在……下面", 1, Region.Bottom),
    Behind("behind", "在……后面", 1, Region.Back),
    InFrontOf("in front of", "在……前面", 1, Region.Front),
    NextTo("next to", "在……旁边", 1, Region.Right),
    Near("near", "在……附近", 2, Region.Right),
    Between("between", "在……中间", 2, Region.Center),
    Above("above", "在……上方（不挨着）", 2, Region.Top),
    Below("below", "在……下方（不挨着）", 2, Region.Bottom),
    Beside("beside", "在……旁边", 2, Region.Right),
    LeftOf("on the left of", "在……左边", 3, Region.Left),
    RightOf("on the right of", "在……右边", 3, Region.Right),
    Inside("inside", "在……里面", 3, Region.Center),
    Outside("outside", "在……外面", 3, Region.Right);

    /** 「点一点放在哪」只用画得出独立落点的方位 */
    val placeable: Boolean
        get() = region in setOf(Region.Top, Region.Bottom, Region.Center, Region.Left, Region.Right) &&
            this != Between && this != Inside && this != Outside
}

/** 画出来分不清的方位组：同组的不会同时出现在一道题的选项里。 */
private val RelationConflicts = listOf(
    setOf(Relation.NextTo, Relation.Beside, Relation.Near, Relation.RightOf, Relation.LeftOf, Relation.Outside),
    setOf(Relation.Under, Relation.Below),
    setOf(Relation.In, Relation.Inside)
)

private fun conflicts(a: Relation, b: Relation) = a == b || RelationConflicts.any { a in it && b in it }

enum class Direction(val sentence: String, val zh: String, val emoji: String) {
    Left("Turn left.", "向左转。", "⬅️"),
    Right("Turn right.", "向右转。", "➡️"),
    Straight("Go straight.", "直走。", "⬆️"),
    Stop("Stop.", "停下。", "🛑")
}

data class ColorName(val word: String, val zh: String, val argb: Long, val level: Int)

private val Colors = listOf(
    ColorName("red", "红色", 0xFFE53935, 1),
    ColorName("blue", "蓝色", 0xFF1E88E5, 1),
    ColorName("yellow", "黄色", 0xFFFDD835, 1),
    ColorName("green", "绿色", 0xFF43A047, 1),
    ColorName("black", "黑色", 0xFF212121, 1),
    ColorName("white", "白色", 0xFFFFFFFF, 1),
    ColorName("orange", "橙色", 0xFFFB8C00, 2),
    ColorName("pink", "粉色", 0xFFF48FB1, 2),
    ColorName("purple", "紫色", 0xFF8E24AA, 2),
    ColorName("brown", "棕色", 0xFF795548, 2),
    ColorName("grey", "灰色", 0xFF9E9E9E, 2),
    ColorName("light blue", "浅蓝色", 0xFF90CAF9, 3),
    ColorName("dark blue", "深蓝色", 0xFF0D47A1, 3),
    ColorName("light green", "浅绿色", 0xFFA5D6A7, 3),
    ColorName("dark green", "深绿色", 0xFF1B5E20, 3)
)

private fun color(word: String) = Colors.first { it.word == word }

/** 颜色固定、孩子一眼认得的东西，用于「The apple is red.」 */
private val ColoredThings = listOf(
    Thing("🍎", "apple", "苹果") to "red", Thing("🍌", "banana", "香蕉") to "yellow", Thing("🐸", "frog", "青蛙") to "green",
    Thing("🌊", "sea", "大海") to "blue", Thing("☁️", "cloud", "云") to "white", Thing("🍊", "orange", "橙子") to "orange",
    Thing("🐷", "pig", "小猪") to "pink", Thing("🍇", "grape", "葡萄") to "purple", Thing("🐻", "bear", "熊") to "brown",
    Thing("🐘", "elephant", "大象") to "grey"
)

private val ColorMixes = listOf(
    Triple("red", "yellow", "orange"), Triple("blue", "yellow", "green"), Triple("red", "blue", "purple"),
    Triple("red", "white", "pink"), Triple("black", "white", "grey")
)

/** 题面或选项要画出来的场景，全部用表情符号和简单图形绘制，不依赖图片素材。 */
sealed class Scene {
    data class Place(val item: Thing, val ref: Thing, val relation: Relation, val ref2: Thing? = null) : Scene()
    data class Count(val emoji: String, val count: Int) : Scene()
    data class Swatch(val color: ColorName) : Scene()
    data class Emoji(val emoji: String) : Scene()
    data class Clock(val hour: Int, val minute: Int) : Scene()
    data class Arrow(val direction: Direction) : Scene()
    data class Label(val text: String) : Scene()
}

enum class SpecialKind(val title: String, val titleEn: String) {
    LookChoose("看图选一选", "Look and Choose"),
    ListenPick("听一听，选出对的图", "Listen and Choose"),
    FillBlank("看图补句子", "Fill in the Blank"),
    Place("听指令，点一点放在哪", "Listen and Put"),
    Count("数一数", "Count"),
    ReadNumber("看数字，选单词", "Read and Choose"),
    Mix("颜色混一混", "Mix the Colors"),
    Direction("看路标，选指令", "Which Way?")
}

data class SpecialOption(val text: String? = null, val scene: Scene? = null)

/**
 * @param prompt 题面文字：中文提示，或带 ____ 的英文
 * @param scene 题面图；ListenPick 为 null（图在选项里）
 * @param speak 进题自动朗读的英文
 * @param sentence / [sentenceZh] 完整答案句，答题后展示与朗读；同组内唯一
 * @param question 英文问句（Where is the cat?），题面上方显示并可朗读
 */
data class SpecialQuestion(
    val kind: SpecialKind,
    val prompt: String,
    val scene: Scene?,
    val speak: String?,
    val options: List<SpecialOption>,
    val answer: Int,
    val sentence: String,
    val sentenceZh: String,
    val question: String = ""
) {
    /** 朗读用的答案：「7 = seven」只读 seven。 */
    val spoken: String get() = sentence.substringAfter("= ")
}

/** 按题型和场景生成英文问句；在出题后统一补上，免得每个出题分支各写一遍。 */
private fun englishQuestion(topic: SpecialTopic, q: SpecialQuestion): String {
    val place = (q.scene ?: q.options.firstOrNull()?.scene) as? Scene.Place
    val count = q.scene as? Scene.Count
    val words = q.sentence.removeSuffix(".").split(" ")
    return when (q.kind) {
        SpecialKind.LookChoose -> when (topic) {
            SpecialTopic.Position -> "Where is the ${place!!.item.name}?"
            SpecialTopic.Colors -> "What color is it?"
            else -> "What time is it?"
        }
        SpecialKind.ListenPick -> when (topic) {
            SpecialTopic.Numbers -> "Which number do you hear?"
            SpecialTopic.Colors -> "Which color do you hear?"
            SpecialTopic.Time -> "Which clock is right?"
            SpecialTopic.Position -> "Which picture is right?"
        }
        SpecialKind.FillBlank -> when (topic) {
            SpecialTopic.Position -> "Where is the ${place!!.item.name}?"
            SpecialTopic.Numbers -> "How many ${CountNouns.getValue(count!!.emoji)} do you have?"
            SpecialTopic.Colors -> "What color is the ${words[1]}?"
            SpecialTopic.Time -> "What time is it?"
        }
        SpecialKind.Place -> "Where do you put the ${place!!.item.name}?"
        SpecialKind.Count -> "How many ${CountNouns.getValue(count!!.emoji)} are there?"
        SpecialKind.ReadNumber -> {
            val label = (q.scene as Scene.Label).text
            when {
                topic == SpecialTopic.Time -> "What time is $label?"
                label.last().isLetter() -> "How do you say $label?"
                else -> "How do you say this number?"
            }
        }
        SpecialKind.Mix -> "What color do ${words[0].lowercase()} and ${words[2]} make?"
        SpecialKind.Direction -> "Which way do you go?"
    }
}

private val Items = listOf(
    Thing("🐱", "cat", "猫"), Thing("🐶", "dog", "狗"), Thing("⚽", "ball", "球"), Thing("🐦", "bird", "小鸟"),
    Thing("🐭", "mouse", "老鼠"), Thing("🍎", "apple", "苹果"), Thing("🦆", "duck", "鸭子"), Thing("🐰", "rabbit", "兔子")
)
private val Box = Thing("📦", "box", "箱子")
private val Basket = Thing("🧺", "basket", "篮子")
private val Chair = Thing("🪑", "chair", "椅子")
private val Bed = Thing("🛏️", "bed", "床")
private val Car = Thing("🚗", "car", "汽车")
private val House = Thing("🏠", "house", "房子")

private fun refsFor(relation: Relation): List<Thing> = when (relation) {
    // 「里面」「后面」要靠参照物挡住一部分来表现，只用不透明的箱子、篮子
    Relation.In -> listOf(Box, Basket)
    Relation.Behind -> listOf(Box)
    Relation.Inside, Relation.Outside -> listOf(House)
    Relation.On, Relation.Above -> listOf(Box, Chair, Bed, Car)
    else -> listOf(Box, Basket, Chair, Bed, Car)
}

fun positionSentence(scene: Scene.Place): String =
    if (scene.relation == Relation.Between) "The ${scene.item.name} is between the ${scene.ref.name} and the ${scene.ref2!!.name}."
    else "The ${scene.item.name} is ${scene.relation.phrase} the ${scene.ref.name}."

/** 「猫在箱子下面」：会被朗读，所以用中文名而不是表情符号，并去掉「（不挨着）」这类括注。 */
private fun positionZh(scene: Scene.Place): String =
    scene.item.zh + scene.relation.zh.substringBefore("（").replace("……", scene.ref.zh + (scene.ref2?.let { "和${it.zh}" } ?: "")) + "。"

private fun randomPlace(relation: Relation, random: Random, item: Thing = Items.random(random), ref: Thing? = null): Scene.Place {
    val r = ref?.takeIf { it in refsFor(relation) } ?: refsFor(relation).random(random)
    val ref2 = if (relation == Relation.Between) (refsFor(relation) - r).random(random) else null
    return Scene.Place(item, r, relation, ref2)
}

/** 从 [pool] 里挑 [n] 个与答案、彼此之间都不冲突的方位。 */
private fun distinctRelations(answer: Relation, pool: List<Relation>, n: Int, random: Random): List<Relation> {
    val picked = mutableListOf<Relation>()
    for (r in pool.shuffled(random)) {
        if (picked.size == n) break
        if (conflicts(r, answer) || picked.any { conflicts(it, r) }) continue
        picked += r
    }
    return picked
}

private fun <T> withAnswer(answer: T, distractors: List<T>, random: Random): Pair<List<T>, Int> {
    val all = (distractors + answer).shuffled(random)
    return all to all.indexOf(answer)
}

// ---------- 方位 ----------

private fun positionQuestion(kind: SpecialKind, level: Int, random: Random, fixed: Relation? = null, fixedDir: Direction? = null): SpecialQuestion? {
    val pool = Relation.values().filter { it.level <= level }
    if (kind == SpecialKind.Direction) {
        val dir = fixedDir ?: Direction.values().random(random)
        val (options, answer) = withAnswer(dir, (Direction.values().toList() - dir).shuffled(random).take(3), random)
        return SpecialQuestion(kind, "按路标走，该怎么说？", Scene.Arrow(dir), null, options.map { SpecialOption(text = it.sentence) }, answer, dir.sentence, dir.zh)
    }
    // 新学的方位多出一些
    val relation = fixed ?: (pool.filter { it.level == level } + pool).random(random)
    val scene = randomPlace(relation, random)
    val sentence = positionSentence(scene)
    return when (kind) {
        SpecialKind.LookChoose -> {
            val (options, answer) = withAnswer(relation, distinctRelations(relation, pool, 3, random), random)
            if (options.size < 4) return null
            SpecialQuestion(kind, "${scene.item.emoji} 在哪里？", scene, null, options.map { SpecialOption(text = it.phrase) }, answer, sentence, positionZh(scene))
        }
        SpecialKind.ListenPick -> {
            val others = distinctRelations(relation, pool, 3, random).map { randomPlace(it, random, scene.item, scene.ref) }
            if (others.size < 3) return null
            val (options, answer) = withAnswer(scene, others, random)
            SpecialQuestion(kind, "听句子，选出对的图", null, sentence, options.map { SpecialOption(scene = it) }, answer, sentence, positionZh(scene))
        }
        SpecialKind.FillBlank -> {
            val (options, answer) = withAnswer(relation, distinctRelations(relation, pool, 3, random), random)
            if (options.size < 4) return null
            SpecialQuestion(kind, sentence.replace(" ${relation.phrase} ", " ${letterBlank(relation.phrase)} "), scene, null, options.map { SpecialOption(text = it.phrase) }, answer, sentence, positionZh(scene))
        }
        SpecialKind.Place -> {
            // 只用画得出独立落点的区域，每个区域一个方位
            val placeable = pool.filter { it.placeable }
            val target = if (fixed != null) fixed.takeIf { it in placeable } ?: return null else placeable.random(random)
            val ref = refsFor(Relation.In).random(random)
            val others = placeable.filter { it.region != target.region }.groupBy { it.region }.values.map { it.random(random) }.shuffled(random).take(3)
            if (others.size < 3) return null
            val item = Items.random(random)
            val (options, answer) = withAnswer(target, others, random)
            val placed = Scene.Place(item, ref, target)
            val command = "Put the ${item.name} ${target.phrase} the ${ref.name}."
            SpecialQuestion(kind, "听指令，点一点 ${item.emoji} 该放在哪", Scene.Place(item, ref, target), command,
                options.map { SpecialOption(scene = Scene.Place(item, ref, it)) }, answer, command, placeZh(placed))
        }
        else -> null
    }
}

private fun placeZh(scene: Scene.Place) = "把" + positionZh(scene).replaceFirst("在", "放在")

// ---------- 数字 ----------

private val Ones = listOf("zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen")
private val Tens = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")
private val Ordinals = listOf("", "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth")

fun numberWord(n: Int): String = when {
    n < 20 -> Ones[n]
    n == 100 -> "one hundred"
    n % 10 == 0 -> Tens[n / 10]
    else -> "${Tens[n / 10]}-${Ones[n % 10]}"
}

private fun ordinalMark(n: Int) = "$n" + when (n) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }

private fun numberRange(level: Int): List<Int> = when (level) {
    1 -> (1..10).toList()
    2 -> (11..20).toList() + (30..100 step 10)
    else -> (21..99).toList() + 100
}

/** 容易混的数：相邻、teen 与 ty（15 ↔ 50）、十位个位互换。 */
private fun confusableNumbers(n: Int, level: Int, random: Random): List<Int> {
    val near = listOf(n - 1, n + 1, n + 10, n - 10, if (n in 13..19) (n - 10) * 10 else -1,
        if (n % 10 == 0 && n in 30..90) n / 10 + 10 else -1, (n % 10) * 10 + n / 10)
    val allowed = (1..100).toSet()
    val picked = near.filter { it in allowed && it != n }.distinct().shuffled(random).take(2).toMutableList()
    val fill = (numberRange(level) + numberRange(maxOf(1, level - 1))).filter { it != n && it !in picked }.shuffled(random)
    picked += fill.take(3 - picked.size)
    return picked
}

private val CountEmojis = listOf("🍎", "⭐", "🐟", "🎈", "🐥", "🌸", "🍪", "🚗")
private val CountNouns = mapOf("🍎" to "apples", "⭐" to "stars", "🐟" to "fish", "🎈" to "balloons", "🐥" to "birds", "🌸" to "flowers", "🍪" to "cakes", "🚗" to "cars")

private fun numberQuestion(kind: SpecialKind, level: Int, random: Random, fixedN: Int? = null): SpecialQuestion? {
    if (level == 3 && kind == SpecialKind.Count) {
        // 三级的「数一数」换成序数：4th → fourth
        val n = fixedN ?: (1..10).random(random)
        // 容易混的：同数的基数词（four）、加十 / 乘十（fourteen、forty）、相邻序数
        val distractors = listOfNotNull(
            numberWord(n),
            (n + 10).takeIf { it <= 100 }?.let(::numberWord),
            (n * 10).takeIf { it in 11..100 }?.let(::numberWord),
            Ordinals.getOrNull(n + 1)?.takeIf { it.isNotEmpty() },
            Ordinals.getOrNull(n - 1)?.takeIf { it.isNotEmpty() }
        ).distinct()
        val (options, answer) = withAnswer(Ordinals[n], distractors.filter { it != Ordinals[n] }.shuffled(random).take(3), random)
        if (options.size < 4) return null
        return SpecialQuestion(SpecialKind.ReadNumber, "${ordinalMark(n)} 是哪个词？", Scene.Label(ordinalMark(n)), Ordinals[n],
            options.map { SpecialOption(text = it) }, answer, "${ordinalMark(n)} = ${Ordinals[n]}", "第 $n")
    }
    val range = if (kind == SpecialKind.Count) (1..(if (level == 1) 10 else 20)).toList() else numberRange(level)
    val n = fixedN ?: range.random(random)
    val word = numberWord(n)
    val wrong = confusableNumbers(n, level, random)
    return when (kind) {
        SpecialKind.Count -> {
            val emoji = CountEmojis.random(random)
            val (options, answer) = withAnswer(word, wrong.map(::numberWord), random)
            SpecialQuestion(kind, "一共有几个？", Scene.Count(emoji, n), null, options.map { SpecialOption(text = it) }, answer,
                "There ${if (n == 1) "is" else "are"} $word ${if (n == 1) CountNouns.getValue(emoji).removeSuffix("s") else CountNouns.getValue(emoji)}.", "一共有 $n 个")
        }
        SpecialKind.ListenPick -> {
            val (options, answer) = withAnswer(n, wrong, random)
            SpecialQuestion(kind, "听一听，选出听到的数字", null, word, options.map { SpecialOption(scene = Scene.Label("$it")) }, answer, "$n = $word", "数字 $n")
        }
        SpecialKind.ReadNumber -> {
            val (options, answer) = withAnswer(word, wrong.map(::numberWord), random)
            SpecialQuestion(kind, "这个数字怎么读？", Scene.Label("$n"), null, options.map { SpecialOption(text = it) }, answer, "$n = $word", "数字 $n")
        }
        SpecialKind.FillBlank -> {
            val count = if (level == 1 || fixedN != null) n else (2..20).random(random)
            val countWord = numberWord(count)
            val emoji = CountEmojis.random(random)
            val noun = CountNouns.getValue(emoji)
            val (options, answer) = withAnswer(countWord, confusableNumbers(count, level, random).map(::numberWord), random)
            val sentence = "I have $countWord ${if (count == 1) noun.removeSuffix("s") else noun}."
            SpecialQuestion(kind, sentence.replace(countWord, letterBlank(countWord)), Scene.Count(emoji, count), null, options.map { SpecialOption(text = it) }, answer, sentence, "我有 $count 个")
        }
        else -> null
    }
}

// ---------- 颜色 ----------

private fun colorQuestion(kind: SpecialKind, level: Int, random: Random, fixedColor: ColorName? = null, fixedMix: Triple<String, String, String>? = null): SpecialQuestion? {
    val pool = Colors.filter { it.level <= level }
    if (kind == SpecialKind.Mix) {
        val (a, b, result) = fixedMix ?: ColorMixes.random(random)
        val wrong = pool.filter { it.word !in setOf(a, b, result) && " " !in it.word }.shuffled(random).take(3).map { it.word }
        val (options, answer) = withAnswer(result, wrong, random)
        val sentence = "${a.replaceFirstChar { it.uppercaseChar() }} and $b make $result."
        return SpecialQuestion(kind, "${color(a).zh} + ${color(b).zh} = ?", null, null,
            options.map { SpecialOption(scene = Scene.Swatch(color(it)), text = it) }, answer, sentence, "${color(a).zh}和${color(b).zh}调成${color(result).zh}。")
    }
    if (kind == SpecialKind.FillBlank) {
        val (thing, word) = if (fixedColor != null) ColoredThings.firstOrNull { it.second == fixedColor.word } ?: return null
        else ColoredThings.filter { (_, w) -> pool.any { it.word == w } }.random(random)
        val wrong = pool.filter { it.word != word && " " !in it.word }.shuffled(random).take(3).map { it.word }
        val (options, answer) = withAnswer(word, wrong, random)
        val sentence = "The ${thing.name} is $word."
        return SpecialQuestion(kind, "The ${thing.name} is ${letterBlank(word)}.", Scene.Emoji(thing.emoji), null, options.map { SpecialOption(text = it) }, answer, sentence, "${thing.zh}是${color(word).zh}的。")
    }
    val target = fixedColor ?: (pool.filter { it.level == level } + pool).random(random)
    val wrong = pool.filter { it != target }.shuffled(random).take(3)
    val (options, answer) = withAnswer(target, wrong, random)
    return when (kind) {
        SpecialKind.LookChoose -> SpecialQuestion(kind, "这是什么颜色？", Scene.Swatch(target), null, options.map { SpecialOption(text = it.word) }, answer, "It's ${target.word}.", "这是${target.zh}。")
        SpecialKind.ListenPick -> SpecialQuestion(kind, "听一听，选出听到的颜色", null, target.word, options.map { SpecialOption(scene = Scene.Swatch(it)) }, answer, "It's ${target.word}.", "这是${target.zh}。")
        else -> null
    }
}

// ---------- 时间 ----------

private fun timeSentence(hour: Int, minute: Int): String = when (minute) {
    0 -> "It's ${numberWord(hour)} o'clock."
    30 -> "It's half past ${numberWord(hour)}."
    15 -> "It's a quarter past ${numberWord(hour)}."
    else -> "It's a quarter to ${numberWord(hour % 12 + 1)}."
}

private fun timeZh(hour: Int, minute: Int): String = when (minute) {
    0 -> "现在是 $hour 点。"
    30 -> "现在是 $hour 点半。"
    15 -> "现在是 $hour 点一刻。"
    else -> "现在是 $hour 点三刻。"
}

private fun minutesFor(level: Int) = when (level) { 1 -> listOf(0); 2 -> listOf(0, 30); else -> listOf(0, 30, 15, 45) }

private fun timeQuestion(kind: SpecialKind, level: Int, random: Random, fixed: Pair<Int, Int>? = null): SpecialQuestion? {
    val minutes = minutesFor(level)
    val hour = fixed?.first ?: (1..12).random(random)
    val minute = fixed?.second ?: minutes.random(random)
    // 干扰：同一种说法换钟点，或同一钟点换说法
    val candidates = (minutes.map { hour to it } + (1..12).map { it to minute } + (1..12).map { it to minutes.random(random) })
        .filter { it != hour to minute }.distinct().shuffled(random).take(3)
    val (options, answer) = withAnswer(hour to minute, candidates, random)
    val sentence = timeSentence(hour, minute)
    return when (kind) {
        SpecialKind.LookChoose -> SpecialQuestion(kind, "现在几点？", Scene.Clock(hour, minute), null, options.map { (h, m) -> SpecialOption(text = timeSentence(h, m)) }, answer, sentence, timeZh(hour, minute))
        SpecialKind.ListenPick -> SpecialQuestion(kind, "听一听，选出对的钟", null, sentence, options.map { (h, m) -> SpecialOption(scene = Scene.Clock(h, m)) }, answer, sentence, timeZh(hour, minute))
        SpecialKind.FillBlank -> {
            val hourWord = numberWord(if (minute == 45) hour % 12 + 1 else hour)
            val wrongHours = (1..12).map(::numberWord).filter { it != hourWord }.shuffled(random).take(3)
            val (hourOptions, hourAnswer) = withAnswer(hourWord, wrongHours, random)
            SpecialQuestion(kind, sentence.replace(Regex("\\b$hourWord\\b"), letterBlank(hourWord)), Scene.Clock(hour, minute), null, hourOptions.map { SpecialOption(text = it) }, hourAnswer, sentence, timeZh(hour, minute))
        }
        SpecialKind.ReadNumber -> {
            // 三级：电子钟 3:45 对应说法
            val digital = "$hour:${"%02d".format(minute)}"
            SpecialQuestion(kind, "$digital 怎么说？", Scene.Label(digital), null, options.map { (h, m) -> SpecialOption(text = timeSentence(h, m)) }, answer, sentence, timeZh(hour, minute))
        }
        else -> null
    }
}

/** 题型轮换出 [count] 道题；同一答案句不重复，凑不出时换下一种题型。 */
/** 一道题考的知识点：每个知识点出一题 */
private sealed class Target {
    data class Rel(val relation: Relation) : Target()
    data class Dir(val direction: Direction) : Target()
    data class Num(val n: Int) : Target()
    data class Ord(val n: Int) : Target()
    data class Col(val color: ColorName) : Target()
    data class Mix(val mix: Triple<String, String, String>) : Target()
    data class Clock(val hour: Int, val minute: Int) : Target()
}

private fun targetsFor(topic: SpecialTopic, level: Int): List<Target> = when (topic) {
    SpecialTopic.Position -> Relation.values().filter { it.level <= level }.map { Target.Rel(it) } +
        if (level == 3) Direction.values().map { Target.Dir(it) } else emptyList()
    SpecialTopic.Numbers -> numberRange(level).map { Target.Num(it) } +
        if (level == 3) (1..10).map { Target.Ord(it) } else emptyList()
    SpecialTopic.Colors -> Colors.filter { it.level <= level }.map { Target.Col(it) } +
        if (level == 3) ColorMixes.map { Target.Mix(it) } else emptyList()
    SpecialTopic.Time -> (1..12).flatMap { h -> minutesFor(level).map { m -> Target.Clock(h, m) } }
}

/** 某个知识点能出哪些题型 */
private fun kindsFor(target: Target, level: Int): List<SpecialKind> = when (target) {
    is Target.Rel -> listOf(SpecialKind.LookChoose, SpecialKind.ListenPick, SpecialKind.FillBlank) +
        if (target.relation.placeable) listOf(SpecialKind.Place) else emptyList()
    is Target.Dir -> listOf(SpecialKind.Direction)
    // 「数一数」「补句子」要把数量画出来，只给 20 以内的数
    is Target.Num -> (if (target.n <= 20) listOf(SpecialKind.Count, SpecialKind.FillBlank) else emptyList()) +
        listOf(SpecialKind.ListenPick, SpecialKind.ReadNumber)
    is Target.Ord -> listOf(SpecialKind.Count)
    is Target.Col -> listOf(SpecialKind.LookChoose, SpecialKind.ListenPick) +
        if (ColoredThings.any { it.second == target.color.word }) listOf(SpecialKind.FillBlank) else emptyList()
    is Target.Mix -> listOf(SpecialKind.Mix)
    is Target.Clock -> listOf(SpecialKind.LookChoose, SpecialKind.ListenPick, SpecialKind.FillBlank) +
        if (level == 3) listOf(SpecialKind.ReadNumber) else emptyList()
}

private fun questionFor(topic: SpecialTopic, level: Int, target: Target, kind: SpecialKind, random: Random): SpecialQuestion? = when (target) {
    is Target.Rel -> positionQuestion(kind, level, random, fixed = target.relation)
    is Target.Dir -> positionQuestion(kind, level, random, fixedDir = target.direction)
    is Target.Num -> numberQuestion(kind, level, random, fixedN = target.n)
    // 序数题走 numberQuestion 三级的 Count 分支
    is Target.Ord -> numberQuestion(SpecialKind.Count, 3, random, fixedN = target.n)
    is Target.Col -> colorQuestion(kind, level, random, fixedColor = target.color)
    is Target.Mix -> colorQuestion(kind, level, random, fixedMix = target.mix)
    is Target.Clock -> timeQuestion(kind, level, random, fixed = target.hour to target.minute)
}?.let { it.copy(question = englishQuestion(topic, it)) }

/**
 * 看图练习：本级每个知识点各出一题，顺序随机；题型在该知识点适用的题型间轮换，
 * 某题型出不了（如干扰项凑不齐）就换下一种。
 */
fun buildSpecialPractice(topic: SpecialTopic, level: Int, random: Random): List<SpecialQuestion> =
    targetsFor(topic, level).shuffled(random).mapIndexedNotNull { index, target ->
        val kinds = kindsFor(target, level)
        kinds.indices.asSequence()
            .map { kinds[(index + it) % kinds.size] }
            .mapNotNull { questionFor(topic, level, target, it, random) }
            .firstOrNull()
    }

/** 本级看图练习的题数（每个知识点一题） */
fun specialPracticeSize(topic: SpecialTopic, level: Int) = targetsFor(topic, level).size

data class WordGroup(val zh: String, val en: String, val words: List<String>)

/** 专项模块：分组单词 + 组合句；[scene] 不为空时另有看图练习。 */
data class SpecialModule(
    val key: String,
    val zh: String,
    val en: String,
    val icon: String,
    val scene: SpecialTopic?,
    val groups: List<WordGroup>,
    val sentences: List<SpeechLine>
) {
    val words: List<String> get() = groups.flatMap { it.words }.distinctBy { it.lowercase() }

    /** 句子练习复用课文的主题练习出题器：组合句当课文，模块单词当本课单词。 */
    fun asPracticeUnit() = Speech(
        id = "special-$key", level = 0, unit = 0, theme = SpeechTheme.Me,
        title = en, titleZh = zh, lines = sentences, patterns = emptyList(), words = words
    )
}

private val PlaceableRelations = Relation.values().filter {
    it.region in setOf(Region.Top, Region.Bottom, Region.Center, Region.Left, Region.Right) &&
        it != Relation.Between && it != Relation.Inside && it != Relation.Outside
}

/**
 * 看图练习可能朗读的全部英文 / 中文（穷举所有物体 × 方位 × 参照物等组合），用于预生成 TTS 音频。
 * 出题逻辑新增句式时要同步加到这里，否则 TtsCoverageTest 会报缺音频。
 */
fun allSpecialUtterances(): Pair<Set<String>, Set<String>> {
    val en = mutableSetOf<String>()
    val zh = mutableSetOf<String>()
    // 方位
    for (relation in Relation.values()) for (item in Items) for (ref in refsFor(relation)) {
        val ref2s = if (relation == Relation.Between) refsFor(relation) - ref else listOf(null)
        for (ref2 in ref2s) {
            val scene = Scene.Place(item, ref, relation, ref2)
            en += positionSentence(scene)
            zh += positionZh(scene)
        }
    }
    for (item in Items) {
        en += "Where is the ${item.name}?"
        en += "Where do you put the ${item.name}?"
        for (target in PlaceableRelations) for (ref in refsFor(Relation.In)) {
            en += "Put the ${item.name} ${target.phrase} the ${ref.name}."
            zh += placeZh(Scene.Place(item, ref, target))
        }
    }
    en += "Which picture is right?"
    en += "Which way do you go?"
    Direction.values().forEach { en += it.sentence; zh += it.zh }
    // 数字
    for (n in 1..100) en += numberWord(n)
    for (n in 1..10) {
        en += Ordinals[n]
        en += "How do you say ${ordinalMark(n)}?"
        zh += "第 $n"
    }
    for (n in 1..100) zh += "数字 $n"
    en += "Which number do you hear?"
    en += "How do you say this number?"
    for (emoji in CountEmojis) {
        val noun = CountNouns.getValue(emoji)
        en += "How many $noun are there?"
        en += "How many $noun do you have?"
        for (n in 1..20) {
            val word = numberWord(n)
            en += "There ${if (n == 1) "is" else "are"} $word ${if (n == 1) noun.removeSuffix("s") else noun}."
            en += "I have $word ${if (n == 1) noun.removeSuffix("s") else noun}."
            zh += "一共有 $n 个"
            zh += "我有 $n 个"
        }
    }
    // 颜色
    for (c in Colors) {
        en += c.word
        en += "It's ${c.word}."
        zh += "这是${c.zh}。"
    }
    for ((thing, word) in ColoredThings) {
        en += "The ${thing.name} is $word."
        en += "What color is the ${thing.name}?"
        zh += "${thing.zh}是${color(word).zh}的。"
    }
    for ((a, b, result) in ColorMixes) {
        en += "${a.replaceFirstChar { it.uppercaseChar() }} and $b make $result."
        en += "What color do $a and $b make?"
        zh += "${color(a).zh}和${color(b).zh}调成${color(result).zh}。"
    }
    en += "What color is it?"
    en += "Which color do you hear?"
    // 时间
    for (hour in 1..12) for (minute in listOf(0, 30, 15, 45)) {
        en += timeSentence(hour, minute)
        en += "What time is $hour:${"%02d".format(minute)}?"
        zh += timeZh(hour, minute)
    }
    en += "What time is it?"
    en += "Which clock is right?"
    return en to zh
}
