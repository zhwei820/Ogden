package com.example.ogdenkids

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

private enum class RestScene { Breathe, Balloons, Star, SleepyCat, Dance }

private val Cheers = listOf(
    "休息一下，马上回来！",
    "你算得真快，歇口气吧～",
    "动动手指，伸个懒腰！",
    "喝口水，再接再厉！",
    "坚持就是胜利，加油！",
    "大脑也要充充电 ⚡"
)

/** 暂停时盖住题目的休息页：每次随机一段动画和一句鼓励。 */
@Composable
fun RestOverlay(done: Int, total: Int, correct: Int, onResume: () -> Unit, modifier: Modifier = Modifier) {
    val scene = remember { RestScene.values().random() }
    val cheer = remember { Cheers.random() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8E7))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AppText("☕ 休息时间", fontWeight = FontWeight.Bold, fontSize = 26.sp)
        AppText(cheer, color = InkSoft, fontSize = 18.sp)
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            when (scene) {
                RestScene.Breathe -> BreatheScene()
                RestScene.Balloons -> BalloonScene()
                RestScene.Star -> StarScene()
                RestScene.SleepyCat -> SleepyCatScene()
                RestScene.Dance -> DanceScene()
            }
        }
        AppText(
            if (done == 0) "还没开始答题呢，准备好就继续吧" else "已完成 $done / $total 题，答对 $correct 题，真棒！",
            color = InkFaint,
            fontSize = 14.sp
        )
        Button(
            onClick = onResume,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) { Text("继续答题", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Emoji(text: String, size: Int, modifier: Modifier = Modifier) {
    Text(text, fontSize = size.sp, modifier = modifier)
}

/** 圆圈慢慢变大变小，跟着吸气、呼气 */
@Composable
private fun BreatheScene() {
    var inhale by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            inhale = !inhale
        }
    }
    val t = rememberInfiniteTransition(label = "breathe")
    val scale by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(220.dp)
                .scale(scale)
                .background(Color(0xFFBAE6FD), CircleShape)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Emoji("🌈", 56)
            AppText(if (inhale) "吸气……" else "呼气……", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0369A1))
        }
    }
}

/** 一串气球从下往上飘，左右轻轻摇 */
@Composable
private fun BalloonScene() {
    val balloons = listOf("🎈", "🎈", "🎈", "🎉", "🎈")
    val t = rememberInfiniteTransition(label = "balloons")
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val h = maxHeight.value
        val w = maxWidth.value
        balloons.forEachIndexed { i, b ->
            val rise by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(5200 + i * 700, easing = LinearEasing), initialStartOffset = StartOffset(i * 900)),
                label = "rise$i"
            )
            val x = w * (0.12f + i * 0.18f) + 14f * sin(rise * 12f + i)
            Emoji(b, 44, Modifier.offset(x = x.dp, y = (h - rise * (h + 60f)).dp))
        }
    }
}

/** 星星蹦跳旋转，周围小星星闪烁 */
@Composable
private fun StarScene() {
    val t = rememberInfiniteTransition(label = "star")
    val jump by t.animateFloat(0f, 1f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "jump")
    val spin by t.animateFloat(-12f, 12f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "spin")
    val twinkle by t.animateFloat(0.2f, 1f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "twinkle")
    Box(contentAlignment = Alignment.Center) {
        listOf(-110 to -70, 110 to -50, -90 to 80, 100 to 90).forEachIndexed { i, (x, y) ->
            Emoji("✨", 28, Modifier.offset(x.dp, y.dp).alpha(if (i % 2 == 0) twinkle else 1.2f - twinkle))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Emoji("⭐", 96, Modifier.offset(y = (-40 * jump).dp).rotate(spin))
            AppText("你是最棒的！", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
        }
    }
}

/** 小猫打盹，Z 一个个往上飘 */
@Composable
private fun SleepyCatScene() {
    val t = rememberInfiniteTransition(label = "cat")
    val breathe by t.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "cat")
    Box(contentAlignment = Alignment.Center) {
        repeat(3) { i ->
            val p by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(2400, easing = LinearEasing), initialStartOffset = StartOffset(i * 800)),
                label = "z$i"
            )
            AppText(
                "Z",
                fontSize = (18 + i * 6).sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6366F1),
                modifier = Modifier
                    .offset(x = (40 + p * 50).dp, y = (-50 - p * 90).dp)
                    .alpha(1f - p)
            )
        }
        Emoji("😺", 96, Modifier.scale(breathe))
    }
}

/** 小熊跳舞，音符飘来飘去 */
@Composable
private fun DanceScene() {
    val t = rememberInfiniteTransition(label = "dance")
    val sway by t.animateFloat(-18f, 18f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "sway")
    val hop by t.animateFloat(0f, 1f, infiniteRepeatable(tween(250), RepeatMode.Reverse), label = "hop")
    Box(contentAlignment = Alignment.Center) {
        listOf("🎵", "🎶", "🎵").forEachIndexed { i, n ->
            val p by t.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(2000, easing = LinearEasing), initialStartOffset = StartOffset(i * 650)),
                label = "note$i"
            )
            Emoji(
                n, 30,
                Modifier
                    .offset(x = ((i - 1) * 80 + 20 * sin(p * 6f)).dp, y = (-60 - p * 80).dp)
                    .alpha(1f - p)
            )
        }
        Emoji("🐻", 96, Modifier.offset(y = (-12 * hop).dp).rotate(sway))
    }
}
