package com.example.ogdenkids

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

private val CheckedDay = Color(0xFFDCFCE7)

@Composable
internal fun MathCheckInCard(store: ProgressStore) {
    val today = remember { Calendar.getInstance() }
    val todayKey = dayKey(today)
    // 0 = 本月，-1 = 上个月…
    var monthOffset by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf(todayKey) }
    var expanded by remember { mutableStateOf(false) }
    val month = (today.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, monthOffset)
    }
    val days = (1..month.getActualMaximum(Calendar.DAY_OF_MONTH)).map { d ->
        val day = (month.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, d) }
        dayKey(day).let { it to store.mathDayLog(it) }
    }
    val streak = mathStreak(today) { store.mathDayLog(it) != null }
    // 周一开头：周一 → 0 … 周日 → 6
    val leading = (month.get(Calendar.DAY_OF_WEEK) + 5) % 7

    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                AppText("📅 打卡", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                AppText(
                    (if (store.mathDayLog(todayKey) != null) "今天已打卡 ✓" else "今天还没打卡") + " · 连续 $streak 天",
                    color = InkSoft,
                    fontSize = 14.sp
                )
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收起" else "展开",
                    tint = InkFaint
                )
            }
            if (expanded) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { monthOffset-- }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上个月") }
                    AppText(
                        "${month.get(Calendar.YEAR)}年${month.get(Calendar.MONTH) + 1}月 · 打卡 ${days.count { it.second != null }} 天",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    IconButton(onClick = { monthOffset++ }, enabled = monthOffset < 0) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下个月")
                    }
                }
                WeekRow(listOf("一", "二", "三", "四", "五", "六", "日").map { w -> { AppText(w, color = InkFaint, fontSize = 12.sp) } })
                (List(leading) { null } + days).chunked(7).forEach { week ->
                    WeekRow(week.map { entry ->
                        {
                            if (entry != null) {
                                val (key, log) = entry
                                DayCell(
                                    day = key.takeLast(2).trimStart('0'),
                                    log = log,
                                    isToday = key == todayKey,
                                    isSelected = key == selected,
                                    onClick = { selected = key }
                                )
                            }
                        }
                    })
                }
                val log = store.mathDayLog(selected)
                val (_, m, d) = selected.split('-').map { it.toInt() }
                AppText(
                    "${m}月${d}日：" + if (log == null) "没有打卡" else
                        "练了 ${log.rounds} 局" + (if (log.perfect > 0) "，全对 ${log.perfect} 局" else "") + "，答对 ${log.correct} / ${log.total} 题",
                    color = InkSoft,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun WeekRow(cells: List<@Composable () -> Unit>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(7) { i ->
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { cells.getOrNull(i)?.invoke() }
        }
    }
}

@Composable
private fun DayCell(day: String, log: MathDayLog?, isToday: Boolean, isSelected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(if (log != null) CheckedDay else Color.Transparent, shape)
            .border(if (isSelected) 2.dp else 1.dp, if (isSelected) Success else if (isToday) InkFaint else Color.Transparent, shape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppText(day, fontSize = 14.sp, fontWeight = if (isToday) FontWeight.Bold else null, color = if (log != null) Success else Ink)
        if (log != null) AppText(if (log.perfect > 0) "⭐" else "✓", fontSize = 11.sp, color = Success)
    }
}
