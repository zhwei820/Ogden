package com.example.ogdenkids

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Solfege = listOf("do", "re", "mi", "fa", "sol", "la", "si")
val NoteLetters = listOf("C", "D", "E", "F", "G", "A", "B")

/** 中央 C（C4）开始的 C 大调七个音 */
val CMajorMidis = listOf(60, 62, 64, 65, 67, 69, 71)

fun isBlackKey(midi: Int) = midi % 12 in setOf(1, 3, 6, 8, 10)

/** 白键在 C 大调里的序号 0..6（do..si），黑键返回 null */
fun degreeOf(midi: Int): Int? = CMajorMidis.indexOf(60 + midi % 12).takeIf { it >= 0 }

/** 高音谱表上的位置：0 = 第一线 E4，每升一个白键 +1；C4 = -2（下加一线） */
fun staffStep(midi: Int): Int {
    val octave = midi / 12 - 5
    return octave * 7 + degreeOf(midi)!! - 2
}

enum class NoteKind(val zh: String, val beats: String, val beatValue: Double, val hollow: Boolean, val stem: Boolean, val flags: Int) {
    Whole("全音符", "4 拍", 4.0, true, false, 0),
    Half("二分音符", "2 拍", 2.0, true, true, 0),
    Quarter("四分音符", "1 拍", 1.0, false, true, 0),
    Eighth("八分音符", "半拍", 0.5, false, true, 1),
    Sixteenth("十六分音符", "四分之一拍", 0.25, false, true, 2)
}

sealed class StaffSymbol {
    data class Note(val step: Int, val kind: NoteKind = NoteKind.Quarter, val accidental: String? = null) : StaffSymbol()
    data class Rest(val kind: NoteKind) : StaffSymbol()
    data class Time(val top: Int, val bottom: Int) : StaffSymbol()
    object Bar : StaffSymbol()
}

private const val TrebleClef = "𝄞"

/**
 * 高音谱表。[lines] = false 时只画音符本身（介绍音符形状用）。
 * [colors] 按下标给符号着色，[onTap] 返回被点到的符号下标。
 */
@Composable
fun StaffView(
    symbols: List<StaffSymbol>,
    modifier: Modifier = Modifier,
    clef: Boolean = true,
    lines: Boolean = true,
    colors: Map<Int, Color> = emptyMap(),
    gap: Dp = 10.dp,
    onTap: ((Int) -> Unit)? = null
) {
    val tap = if (onTap == null) Modifier else Modifier.pointerInput(symbols) {
        detectTapGestures { offset ->
            val g = gap.toPx()
            val startX = if (clef) 3.4f * g else 0f
            val slot = (size.width - startX) / symbols.size
            val i = ((offset.x - startX) / slot).toInt()
            if (i in symbols.indices) onTap(i)
        }
    }
    Canvas(
        modifier
            .fillMaxWidth()
            .height(gap * 11)
            .then(tap)
    ) {
        val g = gap.toPx()
        // 第一线（step 0）的 y；上下各留出加线音符和符干的空间
        val baseY = g * 7.5f
        fun y(step: Float) = baseY - step * g / 2
        if (lines) {
            for (l in 0..4) drawLine(Ink, Offset(0f, y(l * 2f)), Offset(size.width, y(l * 2f)), strokeWidth = g * 0.09f)
        }
        val startX = if (clef) 3.4f * g else 0f
        if (clef) drawClef(g, y(-1.6f))
        val slot = (size.width - startX) / symbols.size
        symbols.forEachIndexed { i, symbol ->
            val x = startX + slot * (i + 0.5f)
            val color = colors[i] ?: Ink
            when (symbol) {
                is StaffSymbol.Note -> drawNote(x, symbol, color, g, ::y)
                is StaffSymbol.Rest -> drawRest(x, symbol.kind, color, g, ::y)
                is StaffSymbol.Time -> {
                    drawLabel(symbol.top.toString(), x, y(5f) + g * 0.85f, g * 2.3f, color, bold = true)
                    drawLabel(symbol.bottom.toString(), x, y(1f) + g * 0.85f, g * 2.3f, color, bold = true)
                }
                StaffSymbol.Bar -> drawLine(color, Offset(x, y(0f)), Offset(x, y(8f)), strokeWidth = g * 0.12f)
            }
        }
    }
}

private fun DrawScope.drawClef(g: Float, baseline: Float) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = g * 6.6f; color = Ink.toArgb() }
    drawIntoCanvas { canvas ->
        if (paint.hasGlyph(TrebleClef)) {
            canvas.nativeCanvas.drawText(TrebleClef, g * 0.3f, baseline, paint)
        } else {
            // 系统字体没有音乐符号时退回一个斜体 G，至少能看出「G 谱号」
            paint.textSize = g * 4.2f
            paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            canvas.nativeCanvas.drawText("G", g * 0.5f, baseline - g * 0.6f, paint)
        }
    }
}

private fun DrawScope.drawLabel(text: String, x: Float, baseline: Float, size: Float, color: Color, bold: Boolean = false) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color.toArgb()
        textAlign = Paint.Align.CENTER
        if (bold) typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
    }
    drawIntoCanvas { it.nativeCanvas.drawText(text, x, baseline, paint) }
}

private fun DrawScope.drawNote(x: Float, note: StaffSymbol.Note, color: Color, g: Float, y: (Float) -> Float) {
    val step = note.step
    val cy = y(step.toFloat())
    // 加线：低于第一线、高于第五线的线位
    val ledger = when {
        step <= -2 -> (-2 downTo step).filter { it % 2 == 0 }
        step >= 10 -> (10..step).filter { it % 2 == 0 }
        else -> emptyList()
    }
    ledger.forEach { drawLine(color, Offset(x - g * 1.1f, y(it.toFloat())), Offset(x + g * 1.1f, y(it.toFloat())), strokeWidth = g * 0.09f) }
    rotate(-20f, pivot = Offset(x, cy)) {
        drawOval(
            color,
            topLeft = Offset(x - g * 0.66f, cy - g * 0.46f),
            size = Size(g * 1.32f, g * 0.92f),
            style = if (note.kind.hollow) Stroke(g * 0.2f) else androidx.compose.ui.graphics.drawscope.Fill
        )
    }
    if (note.kind.stem) {
        // 第三线（B4）以下符干朝上写在右边，以上朝下写在左边
        val up = step < 4
        val sx = if (up) x + g * 0.6f else x - g * 0.6f
        val end = if (up) cy - g * 3.5f else cy + g * 3.5f
        drawLine(color, Offset(sx, cy), Offset(sx, end), strokeWidth = g * 0.13f)
        repeat(note.kind.flags) { f ->
            val fy = if (up) end + f * g * 0.8f else end - f * g * 0.8f
            val path = Path().apply {
                moveTo(sx, fy)
                if (up) cubicTo(sx + g * 0.2f, fy + g * 0.9f, sx + g * 1.2f, fy + g * 1.1f, sx + g * 0.8f, fy + g * 2.2f)
                else cubicTo(sx + g * 0.2f, fy - g * 0.9f, sx + g * 1.2f, fy - g * 1.1f, sx + g * 0.8f, fy - g * 2.2f)
            }
            drawPath(path, color, style = Stroke(g * 0.2f))
        }
    }
    note.accidental?.let { drawLabel(it, x - g * 1.7f, cy + g * 0.55f, g * 2f, color) }
}

private fun DrawScope.drawRest(x: Float, kind: NoteKind, color: Color, g: Float, y: (Float) -> Float) {
    when (kind) {
        // 全休止符挂在第四线下面，二分休止符坐在第三线上面
        NoteKind.Whole -> drawRect(color, Offset(x - g * 0.6f, y(6f)), Size(g * 1.2f, g * 0.5f))
        NoteKind.Half -> drawRect(color, Offset(x - g * 0.6f, y(4f) - g * 0.5f), Size(g * 1.2f, g * 0.5f))
        NoteKind.Quarter -> {
            val path = Path().apply {
                moveTo(x - g * 0.3f, y(7.6f))
                lineTo(x + g * 0.35f, y(6.2f))
                lineTo(x - g * 0.3f, y(4.8f))
                lineTo(x + g * 0.35f, y(3.4f))
                quadraticBezierTo(x - g * 0.7f, y(3.2f), x + g * 0.1f, y(1.6f))
            }
            drawPath(path, color, style = Stroke(g * 0.28f))
        }
        NoteKind.Eighth, NoteKind.Sixteenth -> {
            repeat(kind.flags) { f ->
                val top = 5.6f - f * 2f
                drawCircle(color, g * 0.24f, Offset(x - g * 0.35f - f * g * 0.25f, y(top)))
                drawLine(color, Offset(x - g * 0.35f - f * g * 0.25f, y(top - 0.3f)), Offset(x + g * 0.4f, y(top + 0.4f)), strokeWidth = g * 0.15f)
            }
            drawLine(color, Offset(x + g * 0.4f, y(6f)), Offset(x - g * 0.2f, y(1f)), strokeWidth = g * 0.15f)
        }
    }
}

/**
 * 钢琴键盘，[low]..[high] 应以白键开头和结尾。
 * [label] 给每个键配文字（返回 null 不显示），[marks] 给键着色。
 */
@Composable
fun PianoKeyboard(
    low: Int,
    high: Int,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    label: (Int) -> String? = { null },
    marks: Map<Int, Color> = emptyMap(),
    onKey: ((Int) -> Unit)? = null
) {
    val whites = (low..high).filterNot(::isBlackKey)
    val blacks = (low..high).filter(::isBlackKey)
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val w = maxWidth / whites.size
        val whiteShape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
        Row(Modifier.fillMaxSize()) {
            whites.forEach { m ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = 1.dp)
                        .clip(whiteShape)
                        .background(marks[m] ?: Color.White)
                        .border(1.dp, Line, whiteShape)
                        .clickable(enabled = onKey != null) { onKey?.invoke(m) },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    label(m)?.let { AppText(it, fontSize = 12.sp, color = InkSoft, modifier = Modifier.padding(bottom = 6.dp), maxLines = 1) }
                }
            }
        }
        blacks.forEach { m ->
            val left = whites.indexOf(m - 1)
            Box(
                Modifier
                    .offset(x = w * (left + 1) - w * 0.3f)
                    .width(w * 0.6f)
                    .fillMaxHeight(0.6f)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(marks[m] ?: Ink)
                    .clickable(enabled = onKey != null) { onKey?.invoke(m) },
                contentAlignment = Alignment.BottomCenter
            ) {
                label(m)?.let { Text(it, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp), maxLines = 1) }
            }
        }
    }
}
