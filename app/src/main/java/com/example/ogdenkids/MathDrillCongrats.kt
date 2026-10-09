package com.example.ogdenkids

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Composable
internal fun CongratsCard(total: Int, subtitle: String, modifier: Modifier = Modifier) {
    val date = remember { SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date()) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFF97316), Color(0xFFEC4899))),
                RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AppText("🏆", fontSize = 72.sp)
        AppText("Congratulations!", color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 30.sp)
        AppText("全部答对，太厉害了！", color = Color.White, fontSize = 18.sp)
        AppText("$total / $total", color = Color.White, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 60.sp)
        AppText("⭐⭐⭐⭐⭐", fontSize = 26.sp)
        AppText(subtitle, color = Color.White.copy(alpha = 0.92f), fontSize = 14.sp, textAlign = TextAlign.Center)
        AppText("$date · Anna English 口算小游戏", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

private val ConfettiColors = listOf(
    Color(0xFFEF4444), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFFEC4899)
)

private class Piece(val x: Float, val delay: Float, val speed: Float, val drift: Float, val spin: Float, val size: Float, val color: Color)

/** 从顶部撒下一阵彩纸，约 4 秒落完。 */
@Composable
internal fun ConfettiBurst(modifier: Modifier = Modifier) {
    val pieces = remember {
        val r = Random(System.nanoTime())
        List(90) {
            Piece(
                x = r.nextFloat(),
                delay = r.nextFloat() * 0.35f,
                speed = 0.8f + r.nextFloat() * 0.6f,
                drift = (r.nextFloat() - 0.5f) * 0.25f,
                spin = (r.nextFloat() - 0.5f) * 1440f,
                size = 6f + r.nextFloat() * 8f,
                color = ConfettiColors[r.nextInt(ConfettiColors.size)]
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(4000, easing = LinearEasing)) }
    if (progress.value >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val density = this.density
        pieces.forEach { p ->
            val t = ((progress.value - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val y = -40f + t * p.speed * (size.height + 80f)
            if (y > size.height) return@forEach
            val x = (p.x + p.drift * t) * size.width
            val w = p.size * density
            rotate(p.spin * t, Offset(x, y)) {
                drawRect(p.color, Offset(x - w / 2, y - w / 4), Size(w, w / 2))
            }
        }
    }
}

/** 截取 [bounds]（Compose 根坐标）范围内的画面，四周留一圈纸色边。 */
internal fun captureRegion(view: View, bounds: Rect, marginPx: Int, background: Int): Bitmap {
    val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    android.graphics.Canvas(full).apply { drawColor(background); view.draw(this) }
    val left = (bounds.left.toInt() - marginPx).coerceAtLeast(0)
    val top = (bounds.top.toInt() - marginPx).coerceAtLeast(0)
    val right = (bounds.right.toInt() + marginPx).coerceAtMost(view.width)
    val bottom = (bounds.bottom.toInt() + marginPx).coerceAtMost(view.height)
    return Bitmap.createBitmap(full, left, top, right - left, bottom - top).also { full.recycle() }
}

/** 直接拉起微信「发送给朋友」；没装微信就提示。不走微信 SDK（要在开放平台注册 AppID）。 */
internal fun shareImageToWeChat(context: Context, bitmap: Bitmap) {
    val dir = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(dir, "congrats.png")
    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        component = ComponentName("com.tencent.mm", "com.tencent.mm.ui.tools.ShareImgUI")
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "没有找到微信", Toast.LENGTH_SHORT).show()
    }
}
