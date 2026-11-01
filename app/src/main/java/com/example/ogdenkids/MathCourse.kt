package com.example.ogdenkids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

private val MathTint = Color(0xFF1E40AF)
private val RightBg = Color(0xFFDCFCE7)
private val WrongBg = Color(0xFFFEE2E2)

private fun scoreKey(unit: MathUnit) = "math-${unit.key}"

private val GradeNames = mapOf(3 to "三年级", 4 to "四年级", 5 to "五年级", 6 to "六年级")

/** 数学首页里的「小学数学课堂」：年级选择 + 单元列表 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathCourseSection(store: ProgressStore, grade: Int, onGrade: (Int) -> Unit, onUnit: (MathUnit) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("小学数学课堂", "按人教版教材顺序，先看知识点，再做 $MathUnitRoundSize 道题")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GradeNames.forEach { (g, name) ->
                FilterChip(selected = grade == g, onClick = { onGrade(g) }, label = { AppText(name) })
            }
        }
        mathUnitsOf(grade).forEachIndexed { i, unit ->
            val best = store.bestSpecialScore(scoreKey(unit), 0)
            MusicEntryCard(
                badge = "${i + 1}",
                title = unit.title,
                subtitle = unit.tips.first() + if (best != null) " · 最好 $best / $MathUnitRoundSize" else "",
                onClick = { onUnit(unit) },
                trailing = if (best == MathUnitRoundSize) ({ Text("⭐", fontSize = 20.sp) }) else null
            )
        }
    }
}

@Composable
fun MathUnitScreen(unit: MathUnit, store: ProgressStore, onPractice: () -> Unit, onBack: () -> Unit) {
    val examples = remember(unit.key) { buildMathUnitRound(unit, Random(System.nanoTime())).take(2) }
    val best = store.bestSpecialScore(scoreKey(unit), 0)
    Column(Modifier.fillMaxSize().background(Paper)) {
        MusicTopBar("${GradeNames[unit.grade]} · ${unit.title}", "先看知识点和例题，再去练习", onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CourseCard("📌 知识点") {
                unit.tips.forEach { AppText("• $it", color = InkSoft, fontSize = 16.sp, lineHeight = 25.sp) }
            }
            examples.forEachIndexed { i, q ->
                CourseCard("✏️ 例题 ${i + 1}") {
                    AppText(q.prompt, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                    AppText("答案：${q.answer}", color = Success, fontWeight = FontWeight.Bold)
                    AppText(q.explain, color = InkSoft, fontSize = 15.sp, lineHeight = 23.sp)
                }
            }
            Button(onClick = onPractice, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                AppText("开始练习（$MathUnitRoundSize 题）", fontWeight = FontWeight.Bold)
            }
            if (best != null) AppText("最好成绩 $best / $MathUnitRoundSize", color = InkFaint, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun CourseCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth()
            .background(PaperElevated, RoundedCornerShape(16.dp))
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppText(title, fontWeight = FontWeight.Bold, color = MathTint)
        content()
    }
}

@Composable
fun MathUnitPracticeScreen(unit: MathUnit, store: ProgressStore, onBack: () -> Unit) {
    var round by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(Paper)) {
        MusicTopBar("${unit.title} · 练习", "${GradeNames[unit.grade]} · 每局 $MathUnitRoundSize 题", onBack)
        key(round) { PracticeRound(unit, store, onAgain = { round++ }, onBack = onBack) }
    }
}

@Composable
private fun PracticeRound(unit: MathUnit, store: ProgressStore, onAgain: () -> Unit, onBack: () -> Unit) {
    // 题目由种子决定，转屏或进程被回收后用同一个种子重建，进度不丢
    val seed by rememberSaveable { mutableStateOf(System.nanoTime()) }
    val questions = remember(seed) { buildMathUnitRound(unit, Random(seed)) }
    var index by rememberSaveable { mutableStateOf(0) }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    var correct by rememberSaveable { mutableStateOf(0) }
    val finished = index >= questions.size
    var best by remember { mutableStateOf(store.bestSpecialScore(scoreKey(unit), 0)) }
    LaunchedEffect(finished) {
        if (finished) {
            store.saveSpecialScore(scoreKey(unit), 0, correct)
            best = store.bestSpecialScore(scoreKey(unit), 0)
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (finished) {
            if (correct == questions.size) CheerSound()
            val stars = roundStars(correct, questions.size)
            Spacer(Modifier.height(24.dp))
            Text(stars, fontSize = 48.sp)
            AppText("答对 $correct / ${questions.size}", fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif)
            best?.let { AppText("最好成绩 $it / $MathUnitRoundSize", color = InkFaint) }
            Button(onClick = onAgain, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                AppText("再来一局", fontWeight = FontWeight.Bold)
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) { AppText("回到知识点") }
        } else {
            val q = questions[index]
            val answered = chosen != null
            LinearProgressIndicator(progress = index / questions.size.toFloat(), modifier = Modifier.fillMaxWidth())
            AppText("第 ${index + 1} / ${questions.size} 题 · 答对 $correct", color = InkFaint, fontSize = 13.sp)
            Box(
                Modifier.fillMaxWidth().heightIn(min = 110.dp)
                    .background(PaperElevated, RoundedCornerShape(18.dp))
                    .border(1.dp, Line, RoundedCornerShape(18.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                AppText(q.prompt, fontSize = 24.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.Center)
            }
            q.options.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { option ->
                        val bg = when {
                            !answered -> PaperElevated
                            option == q.answer -> RightBg
                            option == chosen -> WrongBg
                            else -> PaperElevated
                        }
                        Box(
                            Modifier.weight(1f).heightIn(min = 64.dp)
                                .background(bg, RoundedCornerShape(14.dp))
                                .border(1.5.dp, if (answered && option == q.answer) Success else Line, RoundedCornerShape(14.dp))
                                .clickable(enabled = !answered) {
                                    chosen = option
                                    if (option == q.answer) correct++
                                }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppText(option, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
            if (answered) {
                val right = chosen == q.answer
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 640.dp)
                        .background(if (right) RightBg else WrongBg, RoundedCornerShape(14.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppText(if (right) "✅ 答对了！" else "❌ 正确答案是 ${q.answer}", fontWeight = FontWeight.Bold, color = if (right) Success else Error)
                    AppText(q.explain, color = Ink, fontSize = 15.sp, lineHeight = 23.sp)
                }
                Button(
                    onClick = { chosen = null; index++ },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { AppText(if (index == questions.lastIndex) "看成绩" else "下一题", fontWeight = FontWeight.Bold) }
            }
        }
    }
}
