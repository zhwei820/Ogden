package com.example.ogdenkids

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.SystemClock
import android.view.Surface
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin

private val SportsGreen = Color(0xFF15803D)
private val Overlay = Color(0x99000000)

// ---------- 首页 ----------

@Composable
fun SportsHomeScreen(
    store: SportsStore,
    padding: PaddingValues,
    onJump: (JumpMode) -> Unit,
    onDodge: () -> Unit,
    onFruit: () -> Unit
) {
    val records = remember { store.records() }
    val today = remember { store.todayJumps() }
    val format = remember { SimpleDateFormat("M月d日 HH:mm", Locale.CHINA) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    AppText("📱 怎么摆手机", fontWeight = FontWeight.Bold, color = SportsGreen)
                    AppText("把手机竖着靠在墙边或书上，屏幕朝向自己；人退到 2～3 米外，让画面里能看到从头到脚。光线亮一点，识别更准。", color = InkSoft, fontSize = 13.sp)
                }
            }
        }
        item { SportsSectionTitle("🪢 跳绳", if (today > 0) "今天已经跳了 $today 下" else "摄像头帮你数，跳一下数一下") }
        items(JumpMode.values().toList()) { mode ->
            val best = store.best(SportsStore.jumpKey(mode))
            SportsRowCard(
                emoji = if (mode == JumpMode.Free) "🪢" else "⏱️",
                title = mode.title,
                subtitle = mode.subtitle,
                trailing = if (best > 0) "最好 $best 下" else "",
                onClick = { onJump(mode) }
            )
        }
        item { SportsSectionTitle("🟥 躲避方块", "左右跑动躲开方块，碰一碰星星加分") }
        item {
            val best = store.best(SportsStore.DodgeGameKey)
            SportsRowCard(
                emoji = "🏃",
                title = "躲避方块",
                subtitle = "${DodgeGame.MaxLives} 颗心，被砸到就少一颗，越往后掉得越快",
                trailing = if (best > 0) "最高 $best 分" else "",
                onClick = onDodge
            )
        }
        item { SportsSectionTitle("🍉 切水果", "挥手切开飞起来的水果，小心炸弹") }
        item {
            val best = store.best(SportsStore.FruitGameKey)
            SportsRowCard(
                emoji = "🍉",
                title = "切水果",
                subtitle = "一局 60 秒，手挥得快才切得开；碰到炸弹扣 ${FruitGame.BombPenalty} 分",
                trailing = if (best > 0) "最高 $best 分" else "",
                onClick = onFruit
            )
        }
        if (records.isNotEmpty()) {
            item { SportsSectionTitle("📜 最近记录", "最近 ${minOf(records.size, 20)} 次") }
            items(records.take(20)) { r ->
                SportsRowCard(
                    emoji = when (r.game) {
                        SportsStore.DodgeGameKey -> "🏃"
                        SportsStore.FruitGameKey -> "🍉"
                        else -> "🪢"
                    },
                    title = recordTitle(r),
                    subtitle = "${format.format(Date(r.time))} · ${formatSeconds(r.seconds)}",
                    trailing = if (r.game.startsWith(SportsStore.JumpGamePrefix)) "${r.score} 下" else "${r.score} 分",
                    onClick = {
                        when (r.game) {
                            SportsStore.DodgeGameKey -> onDodge()
                            SportsStore.FruitGameKey -> onFruit()
                            else -> JumpMode.values().firstOrNull { SportsStore.jumpKey(it) == r.game }?.let(onJump)
                        }
                    }
                )
            }
        }
    }
}

private fun recordTitle(r: SportsRecord): String = when (r.game) {
    SportsStore.DodgeGameKey -> "躲避方块"
    SportsStore.FruitGameKey -> "切水果"
    else -> "跳绳 · " + (JumpMode.values().firstOrNull { SportsStore.jumpKey(it) == r.game }?.title ?: "")
}

private fun formatSeconds(s: Int) = if (s >= 60) "${s / 60} 分 ${s % 60} 秒" else "$s 秒"

@Composable
private fun SportsSectionTitle(title: String, subtitle: String) {
    Column(Modifier.padding(top = 8.dp, bottom = 2.dp)) {
        AppText(title, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink)
        AppText(subtitle, color = InkFaint, fontSize = 13.sp)
    }
}

@Composable
private fun SportsRowCard(emoji: String, title: String, subtitle: String, trailing: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth().border(1.dp, Line, RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { AppText(emoji, fontSize = 28.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                AppText(title, fontWeight = FontWeight.SemiBold, color = Ink)
                AppText(subtitle, color = InkFaint, fontSize = 13.sp)
            }
            AppText(trailing, color = InkSoft, fontSize = 14.sp)
        }
    }
}

// ---------- 摄像头 + 姿态识别 ----------

/** 一局的阶段：等人站好 → 倒数 → 进行中 → 结束 */
private enum class Phase { Waiting, Countdown, Running, Done }

/** 没有摄像头权限时先申请，拒绝了给提示 */
@Composable
private fun CameraPermissionGate(onBack: () -> Unit, content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var asked by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        granted = it
        asked = true
    }
    LaunchedEffect(Unit) { if (!granted) launcher.launch(Manifest.permission.CAMERA) }
    if (granted) {
        content()
    } else {
        Column(
            Modifier.fillMaxSize().background(Paper).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AppText("📷", fontSize = 56.sp)
            Spacer(Modifier.size(12.dp))
            AppText(
                if (asked) "需要打开摄像头才能看到你的动作。\n照片不会保存，也不会上传。" else "正在申请摄像头权限……",
                textAlign = TextAlign.Center,
                color = InkSoft
            )
            Spacer(Modifier.size(16.dp))
            if (asked) Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) { AppText("再试一次") }
            OutlinedButton(onClick = onBack) { AppText("返回") }
        }
    }
}

/**
 * 前置摄像头预览（FIT_CENTER，四周可能留黑边），每帧识别出的姿态回调到主线程；看不到人回调 null。
 * 预览与分析都用 4:3，叠加层按同一比例算出画面所在矩形（[fitRect]）。
 */
@Composable
private fun PoseCamera(modifier: Modifier, onFrame: (BodyFrame?) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val orientation = LocalConfiguration.current.orientation
    val latest = rememberUpdatedState(onFrame)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FIT_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    // 横竖屏切换不重建 Activity，要按新方向重新绑定，否则识别坐标是歪的
    DisposableEffect(lifecycleOwner, orientation) {
        val executor = Executors.newSingleThreadExecutor()
        val detector = PoseDetection.getClient(
            PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()
        )
        val main = ContextCompat.getMainExecutor(context)
        val future = ProcessCameraProvider.getInstance(context)
        var disposed = false
        future.addListener({
            if (!disposed) {
                val provider = future.get()
                @Suppress("DEPRECATION")
                val rotation = previewView.display?.rotation
                    ?: (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay?.rotation
                    ?: Surface.ROTATION_0
                val front = runCatching { provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) }.getOrDefault(false)
                val selector = if (front) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                val preview = Preview.Builder().setTargetAspectRatio(AspectRatio.RATIO_4_3).setTargetRotation(rotation).build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val analysis = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy ->
                    val media = proxy.image
                    if (media == null) {
                        proxy.close()
                    } else {
                        val rot = proxy.imageInfo.rotationDegrees
                        val w = if (rot % 180 == 0) proxy.width else proxy.height
                        val h = if (rot % 180 == 0) proxy.height else proxy.width
                        detector.process(InputImage.fromMediaImage(media, rot))
                            .addOnSuccessListener(main) { pose -> if (!disposed) latest.value(pose.toFrame(w, h, mirror = front)) }
                            .addOnFailureListener(main) { if (!disposed) latest.value(null) }
                            .addOnCompleteListener(main) { proxy.close() }
                    }
                }
                runCatching {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
                }
            }
        }, main)
        onDispose {
            disposed = true
            if (future.isDone) runCatching { future.get().unbindAll() }
            detector.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun Pose.toFrame(w: Int, h: Int, mirror: Boolean): BodyFrame? {
    val all = allPoseLandmarks
    if (all.isEmpty() || w <= 0 || h <= 0) return null
    val points = arrayOfNulls<P>(33)
    all.forEach { lm ->
        if (lm.landmarkType in points.indices && lm.inFrameLikelihood >= 0.5f) {
            val x = lm.position.x / w
            points[lm.landmarkType] = P(if (mirror) 1f - x else x, lm.position.y / h)
        }
    }
    val frame = BodyFrame(SystemClock.uptimeMillis(), w.toFloat() / h, points)
    return if (frame.shoulders == null) null else frame
}

/** 宽高比为 [aspect] 的画面在 [size] 里居中缩放后的矩形（与 PreviewView FIT_CENTER 一致） */
private fun fitRect(size: Size, aspect: Float): Rect {
    val w = minOf(size.width, size.height * aspect)
    val h = w / aspect
    val left = (size.width - w) / 2
    val top = (size.height - h) / 2
    return Rect(left, top, left + w, top + h)
}

private fun Rect.map(p: P) = Offset(left + p.x * width, top + p.y * height)

private fun DrawScope.drawSkeleton(frame: BodyFrame, rect: Rect, color: Color) {
    BodyFrame.Bones.forEach { (a, b) ->
        val p = frame[a]
        val q = frame[b]
        if (p != null && q != null) drawLine(color, rect.map(p), rect.map(q), strokeWidth = 6f)
    }
    listOf(BodyFrame.Nose, BodyFrame.LWrist, BodyFrame.RWrist, BodyFrame.LAnkle, BodyFrame.RAnkle).forEach { i ->
        frame[i]?.let { drawCircle(color, 9f, rect.map(it)) }
    }
}

/** 运动页通用骨架：全屏摄像头 + 顶栏 + 常亮屏幕 + 提示音 */
@Composable
private fun SportsCameraPage(
    title: String,
    onBack: () -> Unit,
    onFrame: (BodyFrame?) -> Unit,
    overlay: @Composable () -> Unit
) {
    val view = LocalView.current
    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    CameraPermissionGate(onBack) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            PoseCamera(Modifier.fillMaxSize(), onFrame)
            overlay()
            Row(
                Modifier.fillMaxWidth().background(Overlay).padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回", tint = Color.White) }
                AppText(title, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                CurrentUserChip()
            }
        }
    }
}

@Composable
private fun rememberBeeper(): ToneGenerator? {
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() }
    DisposableEffect(Unit) { onDispose { tone?.release() } }
    return tone
}

/** 底部大提示条 */
@Composable
private fun BottomBanner(text: String) {
    Box(Modifier.fillMaxSize().padding(bottom = 28.dp, start = 16.dp, end = 16.dp), contentAlignment = Alignment.BottomCenter) {
        AppText(
            text,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.background(Overlay, RoundedCornerShape(16.dp)).padding(horizontal = 18.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun CountdownNumber(n: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AppText(if (n > 0) "$n" else "开始！", color = Color.White, fontSize = 120.sp, fontWeight = FontWeight.Black)
    }
}

/** 结束卡片 */
@Composable
private fun ResultCard(emoji: String, headline: String, detail: String, newBest: Boolean, onAgain: () -> Unit, onBack: () -> Unit) {
    if (newBest) CheerSound()
    Box(Modifier.fillMaxSize().background(Overlay), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = PaperElevated),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppText(emoji, fontSize = 56.sp)
                AppText(headline, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                if (newBest) AppText("🏆 新纪录！", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = SportsGreen)
                AppText(detail, color = InkSoft, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) { AppText("不玩了") }
                    Button(onClick = onAgain) { AppText("再来一次") }
                }
            }
        }
    }
}

/** 人在画面里站稳 [holdMs] 后进入倒数；倒数 3 秒后开始 */
@Composable
private fun WaitAndCountdown(
    phase: Phase,
    ready: Boolean,
    beeper: ToneGenerator?,
    onPhase: (Phase) -> Unit,
    onCount: (Int) -> Unit,
    holdMs: Long = 1000
) {
    LaunchedEffect(phase, ready) {
        if (phase == Phase.Waiting && ready) {
            delay(holdMs)
            onPhase(Phase.Countdown)
        }
    }
    LaunchedEffect(phase) {
        if (phase == Phase.Countdown) {
            for (n in 3 downTo 1) {
                onCount(n)
                beeper?.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                delay(1000)
            }
            onCount(0)
            beeper?.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
            onPhase(Phase.Running)
        }
    }
}

// ---------- 跳绳 ----------

@Composable
fun JumpRopeScreen(mode: JumpMode, store: SportsStore, onBack: () -> Unit) {
    val beeper = rememberBeeper()
    val counter = remember { JumpCounter() }
    var phase by remember { mutableStateOf(Phase.Waiting) }
    var frame by remember { mutableStateOf<BodyFrame?>(null) }
    var countdown by remember { mutableStateOf(3) }
    var count by remember { mutableStateOf(0) }
    var startedAt by remember { mutableStateOf(0L) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var newBest by remember { mutableStateOf(false) }
    var round by remember { mutableStateOf(0) }

    WaitAndCountdown(phase, frame?.fullBody == true, beeper, onPhase = {
        if (it == Phase.Running) {
            count = 0
            startedAt = SystemClock.uptimeMillis()
            elapsedMs = 0
        }
        phase = it
    }, onCount = { countdown = it })

    LaunchedEffect(phase, round) {
        if (phase == Phase.Running) {
            while (true) {
                elapsedMs = SystemClock.uptimeMillis() - startedAt
                if (mode.seconds > 0 && elapsedMs >= mode.seconds * 1000L) {
                    phase = Phase.Done
                    break
                }
                delay(100)
            }
        }
    }
    LaunchedEffect(phase) {
        if (phase == Phase.Done) {
            val seconds = if (mode.seconds > 0) mode.seconds else (elapsedMs / 1000).toInt()
            newBest = count > 0 && store.save(SportsRecord(System.currentTimeMillis(), SportsStore.jumpKey(mode), count, seconds))
        }
    }

    SportsCameraPage(
        title = "🪢 跳绳 · ${mode.title}",
        onBack = onBack,
        onFrame = { f ->
            frame = f
            if (counter.update(f) && phase == Phase.Running) {
                count++
                beeper?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
            }
        }
    ) {
        val f = frame
        Canvas(Modifier.fillMaxSize()) {
            if (f != null) {
                val rect = fitRect(size, f.aspect)
                drawSkeleton(f, rect, if (counter.inAir) Color(0xFFFACC15) else Color(0xFF4ADE80))
            }
        }
        when (phase) {
            Phase.Waiting -> BottomBanner(
                when {
                    f == null -> "站到手机前面，让摄像头看到你"
                    !f.fullBody -> "再往后退一点，要看到脚哦"
                    else -> "很好，站着别动……"
                }
            )
            Phase.Countdown -> CountdownNumber(countdown)
            Phase.Running, Phase.Done -> {
                Column(
                    Modifier.fillMaxSize().padding(top = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppText("$count", color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Black)
                    val secs = if (mode.seconds > 0) maxOf(0L, mode.seconds - elapsedMs / 1000) else elapsedMs / 1000
                    AppText(
                        if (mode.seconds > 0) "还剩 $secs 秒" else formatSeconds(secs.toInt()),
                        color = Color.White,
                        fontSize = 22.sp,
                        modifier = Modifier.background(Overlay, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                if (phase == Phase.Running) {
                    if (f == null) BottomBanner("看不到你啦，回到画面里")
                    if (mode == JumpMode.Free) {
                        Box(Modifier.fillMaxSize().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
                            Button(onClick = { phase = Phase.Done }) { AppText("跳完了", fontSize = 18.sp) }
                        }
                    }
                }
            }
        }
        if (phase == Phase.Done) {
            val seconds = if (mode.seconds > 0) mode.seconds else (elapsedMs / 1000).toInt()
            ResultCard(
                emoji = if (count > 0) "🎉" else "🙂",
                headline = "$count 下",
                detail = if (count > 0) "用时 ${formatSeconds(seconds)}，今天一共跳了 ${store.todayJumps()} 下" else "这次没数到，检查一下摄像头能不能看到全身",
                newBest = newBest,
                onAgain = {
                    newBest = false
                    round++
                    phase = Phase.Waiting
                },
                onBack = onBack
            )
        }
    }
}

// ---------- 躲避方块 ----------

@Composable
fun DodgeScreen(store: SportsStore, onBack: () -> Unit) {
    val beeper = rememberBeeper()
    var phase by remember { mutableStateOf(Phase.Waiting) }
    var frame by remember { mutableStateOf<BodyFrame?>(null) }
    var countdown by remember { mutableStateOf(3) }
    var game by remember { mutableStateOf<DodgeGame?>(null) }
    var tick by remember { mutableStateOf(0L) }
    var newBest by remember { mutableStateOf(false) }
    var round by remember { mutableStateOf(0) }

    WaitAndCountdown(phase, frame?.bodyBox() != null, beeper, onPhase = {
        if (it == Phase.Running) game = DodgeGame(frame?.aspect ?: 0.75f)
        phase = it
    }, onCount = { countdown = it })

    LaunchedEffect(phase, round) {
        if (phase == Phase.Running) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val dt = (now - last) / 1e9f
                last = now
                val g = game
                val f = frame
                val body = f?.bodyBox()
                // 姿态超过半秒没更新（人走出画面 / 识别卡住）就暂停
                if (g != null && f != null && body != null && SystemClock.uptimeMillis() - f.timeMs < 500) {
                    val ev = g.step(dt, body, listOfNotNull(f[BodyFrame.LWrist], f[BodyFrame.RWrist]))
                    if (ev.hit) beeper?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
                    if (ev.caught) beeper?.startTone(ToneGenerator.TONE_PROP_ACK, 80)
                }
                tick = now
                if (g == null || g.over) break
            }
            phase = Phase.Done
        }
    }
    LaunchedEffect(phase) {
        val g = game
        if (phase == Phase.Done && g != null) {
            newBest = g.score > 0 && store.save(SportsRecord(System.currentTimeMillis(), SportsStore.DodgeGameKey, g.score, g.elapsed.toInt()))
        }
    }

    SportsCameraPage(title = "🏃 躲避方块", onBack = onBack, onFrame = { frame = it }) {
        val f = frame
        val g = game
        Canvas(Modifier.fillMaxSize()) {
            tick // 每帧重画
            val aspect = f?.aspect ?: 0.75f
            val rect = fitRect(size, aspect)
            val body = f?.bodyBox()
            if (f != null) drawSkeleton(f, rect, Color(0x884ADE80))
            if (body != null && phase == Phase.Running && g != null) {
                val hurt = g.hurtTimer > 0f && ((g.hurtTimer * 8).toInt() % 2 == 0)
                drawRoundRect(
                    if (hurt) Color(0x88EF4444) else Color(0x4422D3EE),
                    topLeft = rect.map(P(body.left, body.top)),
                    size = Size((body.right - body.left) * rect.width, (body.bottom - body.top) * rect.height),
                    cornerRadius = CornerRadius(24f)
                )
            }
            g?.items?.forEach { item ->
                val c = rect.map(P(item.x, item.y))
                val half = item.size / 2 * rect.width
                when (item.kind) {
                    FallingKind.Block -> {
                        drawRoundRect(Color(0xFFEF4444), Offset(c.x - half, c.y - half), Size(half * 2, half * 2), CornerRadius(12f))
                        drawRoundRect(Color(0xFF7F1D1D), Offset(c.x - half, c.y - half), Size(half * 2, half * 2), CornerRadius(12f), style = Stroke(6f))
                    }
                    FallingKind.Star -> drawPath(starPath(c, half), Color(0xFFFACC15))
                }
            }
        }
        when (phase) {
            Phase.Waiting -> BottomBanner(
                if (f?.bodyBox() == null) "站到手机前面，让摄像头看到你的上半身" else "很好，站着别动……"
            )
            Phase.Countdown -> CountdownNumber(countdown)
            Phase.Running, Phase.Done -> if (g != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 60.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText("❤️".repeat(g.lives) + "🤍".repeat(DodgeGame.MaxLives - g.lives), fontSize = 26.sp)
                    Spacer(Modifier.weight(1f))
                    AppText(
                        "${g.score} 分 · ⭐${g.stars}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Overlay, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                if (phase == Phase.Running && f?.bodyBox() == null) BottomBanner("看不到你啦，回到画面里（游戏暂停中）")
            }
        }
        if (phase == Phase.Done && g != null) {
            ResultCard(
                emoji = "🏃",
                headline = "${g.score} 分",
                detail = "坚持了 ${formatSeconds(g.elapsed.toInt())}，碰到 ${g.stars} 颗星星",
                newBest = newBest,
                onAgain = {
                    newBest = false
                    game = null
                    round++
                    phase = Phase.Waiting
                },
                onBack = onBack
            )
        }
    }
}

private fun starPath(c: Offset, r: Float): Path = Path().apply {
    for (i in 0 until 10) {
        val radius = if (i % 2 == 0) r else r * 0.45f
        val a = Math.PI / 5 * i - Math.PI / 2
        val x = c.x + (radius * cos(a)).toFloat()
        val y = c.y + (radius * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

// ---------- 切水果 ----------

/** 两只手最近一小段轨迹，画刀光用 */
private class HandTrail {
    val points = ArrayDeque<Pair<P, Long>>()
    fun add(p: P, t: Long) {
        points.addLast(p to t)
        while (points.size > 6 || (points.isNotEmpty() && t - points.first().second > 250)) points.removeFirst()
    }
}

@Composable
fun FruitScreen(store: SportsStore, onBack: () -> Unit) {
    val beeper = rememberBeeper()
    var phase by remember { mutableStateOf(Phase.Waiting) }
    var frame by remember { mutableStateOf<BodyFrame?>(null) }
    var countdown by remember { mutableStateOf(3) }
    var game by remember { mutableStateOf<FruitGame?>(null) }
    var tick by remember { mutableStateOf(0L) }
    var newBest by remember { mutableStateOf(false) }
    var round by remember { mutableStateOf(0) }
    // 两帧姿态之间手划过的线段，攒到下一次游戏步进时一起判
    val pending = remember { mutableListOf<Swipe>() }
    val trails = remember { listOf(HandTrail(), HandTrail()) }
    val emojiPaint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { textAlign = android.graphics.Paint.Align.CENTER } }

    WaitAndCountdown(phase, frame?.bodyBox() != null, beeper, onPhase = {
        if (it == Phase.Running) {
            pending.clear()
            game = FruitGame(frame?.aspect ?: 0.75f)
        }
        phase = it
    }, onCount = { countdown = it })

    LaunchedEffect(phase, round) {
        if (phase == Phase.Running) {
            var last = withFrameNanos { it }
            while (true) {
                val now = withFrameNanos { it }
                val dt = (now - last) / 1e9f
                last = now
                val g = game
                val f = frame
                if (g != null && f != null && SystemClock.uptimeMillis() - f.timeMs < 500) {
                    val ev = g.step(dt, pending.toList())
                    if (ev.cut > 0) beeper?.startTone(ToneGenerator.TONE_PROP_ACK, 60)
                    if (ev.bomb) beeper?.startTone(ToneGenerator.TONE_PROP_NACK, 300)
                }
                pending.clear()
                tick = now
                if (g == null || g.over) break
            }
            phase = Phase.Done
        }
    }
    LaunchedEffect(phase) {
        val g = game
        if (phase == Phase.Done && g != null) {
            newBest = g.score > 0 && store.save(SportsRecord(System.currentTimeMillis(), SportsStore.FruitGameKey, g.score, g.roundSeconds))
        }
    }

    SportsCameraPage(title = "🍉 切水果", onBack = onBack, onFrame = { f ->
        frame = f
        if (f != null) {
            listOf(true, false).forEachIndexed { i, left ->
                val p = f.hand(left)
                val trail = trails[i]
                if (p != null) {
                    trail.points.lastOrNull()?.let { (q, t) ->
                        if (f.timeMs - t in 1..250) pending += Swipe(q, p, (f.timeMs - t) / 1000f)
                    }
                    trail.add(p, f.timeMs)
                }
            }
        }
    }) {
        val f = frame
        val g = game
        Canvas(Modifier.fillMaxSize()) {
            tick
            val aspect = f?.aspect ?: 0.75f
            val rect = fitRect(size, aspect)
            if (f != null) drawSkeleton(f, rect, Color(0x664ADE80))
            if (g != null && g.bombTimer > 0f) drawRect(Color(0x55EF4444), rect.topLeft, rect.size)
            drawIntoCanvas { canvas ->
                val nc = canvas.nativeCanvas
                g?.items?.forEach { item ->
                    val c = rect.map(P(item.x, item.y))
                    val r = item.radius * rect.width
                    emojiPaint.textSize = r * 1.9f
                    val baseline = c.y + r * 0.65f
                    val text = item.kind?.emoji ?: if (item.sliced) "💥" else "💣"
                    if (!item.sliced || item.kind == null) {
                        nc.save()
                        nc.rotate(Math.toDegrees(item.spin.toDouble()).toFloat() * 0.3f, c.x, c.y)
                        nc.drawText(text, c.x, baseline, emojiPaint)
                        nc.restore()
                    } else {
                        // 沿切口方向劈成两半，各自往两边分开
                        val deg = Math.toDegrees(item.cutAngle.toDouble()).toFloat()
                        val off = item.split * rect.width
                        listOf(-1f, 1f).forEach { side ->
                            nc.save()
                            nc.rotate(deg, c.x, c.y)
                            nc.translate(0f, side * off)
                            if (side < 0) nc.clipRect(c.x - r * 2, c.y - r * 2, c.x + r * 2, c.y)
                            else nc.clipRect(c.x - r * 2, c.y, c.x + r * 2, c.y + r * 2)
                            nc.rotate(-deg, c.x, c.y)
                            nc.drawText(text, c.x, baseline, emojiPaint)
                            nc.restore()
                        }
                        val juice = Color(item.kind.juice)
                        drawCircle(juice.copy(alpha = (0.6f - item.split * 3).coerceAtLeast(0f)), r * (1f + item.split * 6), c)
                    }
                }
            }
            // 刀光
            trails.forEach { trail ->
                val pts = trail.points.toList()
                pts.zipWithNext().forEachIndexed { i, (a, b) ->
                    val w = 6f + 14f * (i + 1) / pts.size
                    drawLine(Color.White.copy(alpha = 0.85f), rect.map(a.first), rect.map(b.first), strokeWidth = w, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
                pts.lastOrNull()?.let { drawCircle(Color(0xFF38BDF8), 14f, rect.map(it.first)) }
            }
        }
        when (phase) {
            Phase.Waiting -> BottomBanner(
                if (f?.bodyBox() == null) "站到手机前面，让摄像头看到你的上半身和两只手" else "很好，准备挥手切水果……"
            )
            Phase.Countdown -> CountdownNumber(countdown)
            Phase.Running, Phase.Done -> if (g != null) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 60.dp, start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppText(
                        "⏱ ${g.remaining}",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Overlay, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    AppText(
                        "${g.score} 分",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.background(Overlay, RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
                if (g.comboTimer > 0f) {
                    Box(Modifier.fillMaxSize().padding(top = 140.dp), contentAlignment = Alignment.TopCenter) {
                        AppText("连切 ×${g.combo}！+${g.combo}", color = Color(0xFFFACC15), fontSize = 36.sp, fontWeight = FontWeight.Black)
                    }
                }
                if (phase == Phase.Running && f == null) BottomBanner("看不到你啦，回到画面里（游戏暂停中）")
            }
        }
        if (phase == Phase.Done && g != null) {
            ResultCard(
                emoji = "🍉",
                headline = "${g.score} 分",
                detail = "切开 ${g.sliced} 个水果" + if (g.bombs > 0) "，碰到 ${g.bombs} 个炸弹" else "，一个炸弹都没碰！",
                newBest = newBest,
                onAgain = {
                    newBest = false
                    game = null
                    round++
                    phase = Phase.Waiting
                },
                onBack = onBack
            )
        }
    }
}
