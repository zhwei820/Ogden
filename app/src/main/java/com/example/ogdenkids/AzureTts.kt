package com.example.ogdenkids

import android.content.Context
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

enum class AzureVoice(val voiceName: String, val lang: String, val fallbackLocale: Locale) {
    EnUs("en-US-JennyNeural", "en-US", Locale.US),
    EnGb("en-GB-SoniaNeural", "en-GB", Locale.UK),
    ZhCn("zh-CN-XiaoxiaoNeural", "zh-CN", Locale.SIMPLIFIED_CHINESE);

    companion object {
        fun english(accent: Accent) = if (accent == Accent.UK) EnGb else EnUs
    }
}

/**
 * Microsoft Azure 语音合成：先查本地缓存，未命中再请求 REST 接口；
 * 未配置 key、断网、额度耗尽等任何失败都回退到系统 TTS，保证有声音。
 */
class AzureSpeaker(context: Context) {
    private val cacheDir = File(context.cacheDir, "azure-tts")
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    // 每次 speak 递增；网络回来时编号已过期说明用户点了别的，丢弃结果
    private val requestSeq = AtomicInteger(0)
    private var player: MediaPlayer? = null
    private var systemTtsReady = false
    private val systemTts = TextToSpeech(context.applicationContext) { status ->
        systemTtsReady = status == TextToSpeech.SUCCESS
    }
    // 系统 TTS 当前这句的 utteranceId 与它对应的 speak 编号；监听回调在 binder 线程，需切回主线程再比对
    private var systemUtterance: Pair<String, Int>? = null
    private var onFinished: (() -> Unit)? = null

    init {
        systemTts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = Unit
            override fun onDone(utteranceId: String?) = finishSystemUtterance(utteranceId)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = finishSystemUtterance(utteranceId)
        })
    }

    /**
     * @param onDone 这句正常播完（含回退到系统 TTS 播完、或无声可放）后在主线程回调；
     *               被 [stop] 或下一次 [speak] 打断时不回调
     */
    fun speak(rawText: String, voice: AzureVoice, onDone: (() -> Unit)? = null) {
        val text = rawText.trim()
        if (text.isEmpty()) return
        stop()
        val seq = requestSeq.incrementAndGet()
        onFinished = onDone
        executor.execute {
            val file = cachedOrSynthesize(text, voice)
            mainHandler.post {
                if (seq != requestSeq.get()) return@post
                if (file != null) playFile(file, text, voice, seq) else speakWithSystemTts(text, voice, seq)
            }
        }
    }

    fun stop() {
        requestSeq.incrementAndGet()
        onFinished = null
        systemUtterance = null
        player?.release()
        player = null
        systemTts.stop()
    }

    fun release() {
        stop()
        systemTts.shutdown()
        executor.shutdown()
    }

    private fun cachedOrSynthesize(text: String, voice: AzureVoice): File? {
        val target = File(cacheDir, sha1("${voice.voiceName}|$text") + ".mp3")
        if (target.length() > 0) return target
        return if (synthesizeToFile(text, voice, target)) target else null
    }

    private fun synthesizeToFile(text: String, voice: AzureVoice, target: File): Boolean {
        val key = BuildConfig.AZURE_SPEECH_KEY
        val region = BuildConfig.AZURE_SPEECH_REGION
        if (key.isBlank() || region.isBlank()) return false
        val connection = runCatching {
            URL("https://$region.tts.speech.microsoft.com/cognitiveservices/v1").openConnection() as HttpURLConnection
        }.getOrElse { return false }
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", key)
            connection.setRequestProperty("Content-Type", "application/ssml+xml")
            connection.setRequestProperty("X-Microsoft-OutputFormat", "audio-24khz-48kbitrate-mono-mp3")
            connection.setRequestProperty("User-Agent", "OgdenBasic")
            connection.outputStream.use { it.write(buildSsml(text, voice.voiceName, voice.lang).toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                val body = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.w(TAG, "Azure TTS HTTP $code voice=${voice.voiceName} body=${body.take(200)}")
                return false
            }
            cacheDir.mkdirs()
            // 先写临时文件再改名，避免中断留下半截 mp3 被当成缓存命中
            val partial = File(target.path + ".part")
            connection.inputStream.use { input -> partial.outputStream().use { input.copyTo(it) } }
            partial.renameTo(target)
        } catch (e: IOException) {
            Log.w(TAG, "Azure TTS request failed voice=${voice.voiceName}", e)
            false
        } finally {
            connection.disconnect()
        }
    }

    private fun finish(seq: Int) {
        if (seq != requestSeq.get()) return
        val callback = onFinished
        onFinished = null
        callback?.invoke()
    }

    private fun finishSystemUtterance(utteranceId: String?) {
        mainHandler.post {
            val (id, seq) = systemUtterance ?: return@post
            if (id != utteranceId) return@post
            systemUtterance = null
            finish(seq)
        }
    }

    private fun playFile(file: File, text: String, voice: AzureVoice, seq: Int) {
        runCatching {
            val mediaPlayer = MediaPlayer()
            player = mediaPlayer
            mediaPlayer.setDataSource(file.path)
            mediaPlayer.setOnPreparedListener { it.start() }
            mediaPlayer.setOnCompletionListener {
                it.release()
                if (player === it) player = null
                finish(seq)
            }
            mediaPlayer.setOnErrorListener { mp, what, _ ->
                Log.w(TAG, "Cached audio unplayable what=$what, deleting ${file.name}")
                mp.release()
                if (player === mp) player = null
                file.delete()
                speakWithSystemTts(text, voice, seq)
                true
            }
            mediaPlayer.prepareAsync()
        }.onFailure {
            Log.w(TAG, "MediaPlayer setup failed", it)
            speakWithSystemTts(text, voice, seq)
        }
    }

    private fun speakWithSystemTts(text: String, voice: AzureVoice, seq: Int) {
        if (!systemTtsReady) {
            // 没有声音可放也要回调，否则全文朗读会停在这一句
            finish(seq)
            return
        }
        val result = systemTts.setLanguage(voice.fallbackLocale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "System TTS lacks ${voice.fallbackLocale}")
        }
        val utteranceId = "azure-fallback-${System.nanoTime()}"
        systemUtterance = utteranceId to seq
        if (systemTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId) != TextToSpeech.SUCCESS) {
            systemUtterance = null
            finish(seq)
        }
    }

    private fun sha1(value: String): String =
        MessageDigest.getInstance("SHA-1").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    private companion object {
        const val TAG = "AzureSpeaker"
    }
}
