package com.example.ogdenkids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/** [en] 是 ogden_words.json 里的词条，介绍里点英文名打开词典。 */
data class StarSign(val symbol: String, val zh: String, val en: String, val startMonth: Int, val startDay: Int, val range: String, val intro: String)

/** [branch] 对应的地支，[pinyin] 是地支的读音。 */
data class ChineseZodiac(val emoji: String, val zh: String, val branch: String, val pinyin: String, val intro: String)

data class HeavenlyStem(val zh: String, val pinyin: String)

val HeavenlyStems = listOf(
    HeavenlyStem("甲", "jiǎ"), HeavenlyStem("乙", "yǐ"), HeavenlyStem("丙", "bǐng"), HeavenlyStem("丁", "dīng"),
    HeavenlyStem("戊", "wù"), HeavenlyStem("己", "jǐ"), HeavenlyStem("庚", "gēng"), HeavenlyStem("辛", "xīn"),
    HeavenlyStem("壬", "rén"), HeavenlyStem("癸", "guǐ")
)

val StarSigns = listOf(
    StarSign("♈", "白羊座", "Aries", 3, 21, "3月21日–4月19日", "勇敢直率、热情有活力，喜欢冲在最前面，做事说干就干。"),
    StarSign("♉", "金牛座", "Taurus", 4, 20, "4月20日–5月20日", "踏实稳重、有耐心，喜欢美食和舒服的东西，认定的事会坚持到底。"),
    StarSign("♊", "双子座", "Gemini", 5, 21, "5月21日–6月21日", "聪明好奇、反应快，爱说话也爱交朋友，对新鲜事总有很多问题。"),
    StarSign("♋", "巨蟹座", "Cancer", 6, 22, "6月22日–7月22日", "温柔体贴、很顾家，像小螃蟹一样外表坚强、内心柔软。"),
    StarSign("♌", "狮子座", "Leo", 7, 23, "7月23日–8月22日", "自信大方、热情开朗，喜欢被关注，也很愿意照顾身边的人。"),
    StarSign("♍", "处女座", "Virgo", 8, 23, "8月23日–9月22日", "细心认真、爱整洁，做事追求完美，是可靠的小帮手。"),
    StarSign("♎", "天秤座", "Libra", 9, 23, "9月23日–10月23日", "友善公平、讲道理，喜欢美好的东西，和谁都能好好相处。"),
    StarSign("♏", "天蝎座", "Scorpio", 10, 24, "10月24日–11月22日", "专注执着、感觉敏锐，对朋友非常忠诚，想做的事一定要做到。"),
    StarSign("♐", "射手座", "Sagittarius", 11, 23, "11月23日–12月21日", "乐观爱冒险、喜欢自由，对世界充满好奇，最爱去新地方探索。"),
    StarSign("♑", "摩羯座", "Capricorn", 12, 22, "12月22日–1月19日", "勤奋踏实、有责任心，一步一个脚印，目标定了就努力实现。"),
    StarSign("♒", "水瓶座", "Aquarius", 1, 20, "1月20日–2月18日", "想法独特、富有创意，爱思考、爱发明，是朋友眼里的点子王。"),
    StarSign("♓", "双鱼座", "Pisces", 2, 19, "2月19日–3月20日", "善良温柔、想象力丰富，喜欢画画、音乐和讲故事。")
)

/** 从子鼠开始，下标 = (农历年 - 4) mod 12；天干下标 = (农历年 - 4) mod 10。 */
val ChineseZodiacs = listOf(
    ChineseZodiac("🐭", "鼠", "子", "zǐ", "机灵聪明、反应快，到哪儿都能很快适应。"),
    ChineseZodiac("🐮", "牛", "丑", "chǒu", "勤劳踏实、有耐心，做事一步一个脚印。"),
    ChineseZodiac("🐯", "虎", "寅", "yín", "勇敢自信、有活力，敢于接受挑战。"),
    ChineseZodiac("🐰", "兔", "卯", "mǎo", "温和善良、细心可爱，人缘特别好。"),
    ChineseZodiac("🐲", "龙", "辰", "chén", "精力充沛、有领导力，志向远大。"),
    ChineseZodiac("🐍", "蛇", "巳", "sì", "冷静聪明、善于思考，观察力强。"),
    ChineseZodiac("🐴", "马", "午", "wǔ", "热情开朗、爱运动，喜欢自由自在地奔跑。"),
    ChineseZodiac("🐑", "羊", "未", "wèi", "温柔体贴、有艺术感，待人友善。"),
    ChineseZodiac("🐵", "猴", "申", "shēn", "聪明活泼、点子多，好奇心强。"),
    ChineseZodiac("🐔", "鸡", "酉", "yǒu", "勤快守时、认真负责，做事有条理。"),
    ChineseZodiac("🐶", "狗", "戌", "xū", "忠诚正直、讲义气，是值得信赖的朋友。"),
    ChineseZodiac("🐷", "猪", "亥", "hài", "憨厚善良、乐观大方，心态特别好。")
)

/** 春节（正月初一）的公历月、日；生肖按春节换年，不按元旦。 */
private val SpringFestivals = mapOf(
    2000 to (2 to 5), 2001 to (1 to 24), 2002 to (2 to 12), 2003 to (2 to 1), 2004 to (1 to 22),
    2005 to (2 to 9), 2006 to (1 to 29), 2007 to (2 to 18), 2008 to (2 to 7), 2009 to (1 to 26),
    2010 to (2 to 14), 2011 to (2 to 3), 2012 to (1 to 23), 2013 to (2 to 10), 2014 to (1 to 31),
    2015 to (2 to 19), 2016 to (2 to 8), 2017 to (1 to 28), 2018 to (2 to 16), 2019 to (2 to 5),
    2020 to (1 to 25), 2021 to (2 to 12), 2022 to (2 to 1), 2023 to (1 to 22), 2024 to (2 to 10),
    2025 to (1 to 29), 2026 to (2 to 17), 2027 to (2 to 6), 2028 to (1 to 26), 2029 to (2 to 13),
    2030 to (2 to 3)
)

/** [birthday] 形如 2019-05-01。 */
fun starSignOf(birthday: String): StarSign {
    val (_, m, d) = birthday.split("-").map { it.toInt() }
    val key = m * 100 + d
    return StarSigns.sortedBy { it.startMonth * 100 + it.startDay }.lastOrNull { it.startMonth * 100 + it.startDay <= key }
        ?: StarSigns.first { it.en == "Capricorn" }
}

private fun lunarYearOf(birthday: String): Int {
    val (y, m, d) = birthday.split("-").map { it.toInt() }
    // 表外的年份按 2 月 4 日（约立春）近似
    val (fm, fd) = SpringFestivals[y] ?: (2 to 4)
    return if (m * 100 + d < fm * 100 + fd) y - 1 else y
}

fun chineseZodiacOf(birthday: String): ChineseZodiac = ganzhiOfYear(lunarYearOf(birthday)).second

/** 农历 [year] 年（春节到下一个春节前）的干支。 */
fun ganzhiOfYear(year: Int): Pair<HeavenlyStem, ChineseZodiac> =
    HeavenlyStems[Math.floorMod(year - 4, 10)] to ChineseZodiacs[Math.floorMod(year - 4, 12)]

/** 出生年的干支，如 戊戌。 */
fun ganzhiOf(birthday: String): Pair<HeavenlyStem, ChineseZodiac> = ganzhiOfYear(lunarYearOf(birthday))

/**
 * 天干地支页与生肖页会朗读的全部中文 → 拼音（空格分隔）：单个天干、地支，以及 60 个「干支年」。
 * 都有离线录音（scripts/gen_tts_assets.py 用拼音标注读音，避免单字读错）。
 */
fun ganzhiUtterances(): Map<String, String> =
    HeavenlyStems.associate { it.zh to it.pinyin } +
        ChineseZodiacs.associate { it.branch to it.pinyin } +
        (0 until 60).associate { i ->
            val stem = HeavenlyStems[i % 10]
            val zodiac = ChineseZodiacs[i % 12]
            "${stem.zh}${zodiac.branch}年" to "${stem.pinyin} ${zodiac.pinyin} nián"
        }

/** 全部星座 / 生肖 / 天干地支的介绍（[tab] 依次为 0 / 1 / 2），高亮自己的那个。点星座英文名打开词典，点汉字朗读。 */
@Composable
fun ZodiacIntroDialog(birthday: String, tab: Int, onTab: (Int) -> Unit, onDismiss: () -> Unit) {
    val services = LocalSpeechServices.current
    val mySign = starSignOf(birthday)
    val myZodiac = chineseZodiacOf(birthday)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
        title = {
            TabRow(selectedTabIndex = tab, containerColor = PaperElevated) {
                listOf("星座", "生肖", "天干地支").forEachIndexed { index, label ->
                    Tab(selected = tab == index, onClick = { onTab(index) }, text = { Text(label, fontSize = 15.sp, maxLines = 1) })
                }
            }
        },
        text = {
            Column(
                Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (tab) {
                0 -> StarSigns.forEach { sign ->
                    IntroRow(mine = sign == mySign, icon = sign.symbol, intro = sign.intro) {
                        AppText(sign.zh, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            sign.en,
                            color = Category.Operations.tint,
                            fontSize = 16.sp,
                            textDecoration = TextDecoration.Underline,
                            modifier = Modifier.clickable {
                                services.lookup(sign.en)?.let(services.openWord)
                            }
                        )
                        AppText(sign.range, color = InkFaint, fontSize = 12.sp)
                    }
                }
                1 -> ChineseZodiacs.forEach { zodiac ->
                    IntroRow(mine = zodiac == myZodiac, icon = zodiac.emoji, intro = zodiac.intro) {
                        SpokenChar(zodiac.branch, zodiac.pinyin, services.speakChinese)
                        AppText("属${zodiac.zh}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                else -> GanzhiPage(birthday, services.speakChinese)
                }
            }
        },
        containerColor = PaperElevated
    )
}

@Composable
private fun IntroRow(mine: Boolean, icon: String, intro: String, title: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (mine) Category.Operations.soft else PaperElevated)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(icon, fontSize = 24.sp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                title()
                if (mine) AppText("我", color = Category.Operations.tint, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            AppText(intro, color = InkSoft, fontSize = 14.sp)
        }
    }
}

/** 汉字 + 拼音（[note] 写在汉字下方），点一下朗读。 */
@Composable
private fun SpokenChar(zh: String, pinyin: String, speak: (String) -> Unit, highlight: Boolean = false, note: String? = null) {
    Column(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlight) Category.Operations.soft else PaperElevated)
            .clickable { speak(zh) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppText(pinyin, color = Category.Operations.tint, fontSize = 12.sp)
        AppText(zh, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        if (note != null) AppText(note, color = InkSoft, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GanzhiPage(birthday: String, speak: (String) -> Unit) {
    val (stem, zodiac) = ganzhiOf(birthday)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AppText("天干有 10 个，地支有 12 个，各取一个配成一对，就是一年的名字，60 年轮一圈。地支和十二生肖一一对应，比如子年就是鼠年。", color = InkSoft, fontSize = 14.sp)
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Category.Operations.soft)
                .clickable { speak("${stem.zh}${zodiac.branch}年") }.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppText("我出生在", color = InkSoft, fontSize = 14.sp)
            AppText("${stem.zh}${zodiac.branch}年", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            AppText("${stem.pinyin} ${zodiac.pinyin} nián", color = Category.Operations.tint, fontSize = 14.sp)
        }
        YearLookup(speak)
        AppText("十天干", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            HeavenlyStems.forEachIndexed { i, it -> SpokenChar(it.zh, it.pinyin, speak, highlight = it == stem, note = "${i + 1}") }
        }
        AppText("十二地支", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChineseZodiacs.forEach { SpokenChar(it.branch, it.pinyin, speak, highlight = it == zodiac, note = "${it.emoji}${it.zh}") }
        }
        AppText("点汉字可以听读音", color = InkFaint, fontSize = 12.sp)
    }
}

/** 输入或加减公历年份，查这一年的干支和生肖；点结果朗读。 */
@Composable
private fun YearLookup(speak: (String) -> Unit) {
    var input by rememberSaveable { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }
    val year = input.toIntOrNull()?.takeIf { it in 1..9999 }
    fun step(delta: Int) {
        year?.let { input = (it + delta).coerceIn(1, 9999).toString() }
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AppText("查一查哪一年", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { step(-1) }) { Icon(Icons.Default.Remove, contentDescription = "上一年") }
            OutlinedTextField(
                input,
                { input = it.filter(Char::isDigit).take(4) },
                suffix = { Text("年") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(120.dp)
            )
            IconButton(onClick = { step(1) }) { Icon(Icons.Default.Add, contentDescription = "下一年") }
        }
        if (year != null) {
            val (stem, zodiac) = ganzhiOfYear(year)
            val name = "${stem.zh}${zodiac.branch}年"
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(PaperElevated)
                    .border(1.dp, Line, RoundedCornerShape(12.dp))
                    .clickable { speak(name) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AppText(name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                AppText("${stem.pinyin} ${zodiac.pinyin} nián", color = Category.Operations.tint, fontSize = 14.sp)
                AppText("${zodiac.emoji}${zodiac.zh}年", fontSize = 16.sp)
            }
            AppText("从这一年的春节开始算，春节前出生的属上一年。", color = InkFaint, fontSize = 12.sp)
        }
    }
}
