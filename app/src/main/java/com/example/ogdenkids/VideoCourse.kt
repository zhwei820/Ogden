package com.example.ogdenkids

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONObject
import java.io.File

/** 已装的视频课包；[dir] 下 course/index.html 是课程网页。 */
data class VideoCourse(val title: String, val count: Int, val dir: File)

fun loadVideoCourses(store: PackStore): List<VideoCourse> = store.installedDirs(PackType.Course).mapNotNull { dir ->
    runCatching {
        val meta = JSONObject(File(dir, "course.json").readText())
        VideoCourse(meta.getString("title"), meta.optInt("count"), dir)
    }.getOrNull()
}

/**
 * 用 WebView 打开视频课网页。网页从包目录以 file:// 加载，视频用相对路径引用同包内的文件；
 * WebView 没有 speechSynthesis，注入 AppTts 让网页的「读一读」走 App 的朗读。
 * 返回键先在网页内回退（课程页 → 列表），退到头才离开。
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VideoCourseScreen(course: VideoCourse, onSpeak: (String) -> Unit, onStopSpeaking: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    // 视频全屏时 WebView 交出的播放视图，及退出全屏的回调
    var fullscreen by remember { mutableStateOf<Pair<View, WebChromeClient.CustomViewCallback>?>(null) }
    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.mediaPlaybackRequiresUserGesture = true
            addJavascriptInterface(object {
                // 在 WebView 的 JS 线程回调，切回主线程再朗读
                @JavascriptInterface
                fun speak(text: String) {
                    post { onSpeak(text) }
                }
            }, "AppTts")
            webChromeClient = object : WebChromeClient() {
                override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                    fullscreen = view to callback
                }

                override fun onHideCustomView() {
                    fullscreen = null
                }
            }
            loadUrl(File(course.dir, "course/index.html").toURI().toString())
        }
    }
    DisposableEffect(webView) {
        onDispose {
            onStopSpeaking()
            webView.destroy()
        }
    }
    BackHandler {
        when {
            fullscreen != null -> fullscreen?.second?.onCustomViewHidden()
            webView.canGoBack() -> webView.goBack()
            else -> onBack()
        }
    }
    Box(Modifier.fillMaxSize().background(Paper)) {
        AndroidView(factory = { webView }, modifier = Modifier.fillMaxSize())
        fullscreen?.let { (view, _) ->
            key(view) {
                AndroidView(
                    factory = { view.also { (it.parent as? ViewGroup)?.removeView(it) } },
                    modifier = Modifier.fillMaxSize().background(Color.Black)
                )
            }
        }
    }
}
