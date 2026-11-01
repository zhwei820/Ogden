package com.example.ogdenkids

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/** 进入组合时播放一次欢呼声（assets/sfx/cheer.mp3，scripts/gen_cheer.py 生成），离开时释放 */
@Composable
fun CheerSound() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val player = runCatching {
            MediaPlayer().apply {
                context.assets.openFd("sfx/cheer.mp3").use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                prepare()
                start()
            }
        }.getOrNull()
        onDispose { player?.release() }
    }
}

/** 一局答题的星级：满分三星，七成两星，四成一星 */
fun roundStars(correct: Int, total: Int): String = when {
    correct == total -> "⭐⭐⭐"
    correct >= total * 0.7 -> "⭐⭐"
    correct >= total * 0.4 -> "⭐"
    else -> "💪"
}
