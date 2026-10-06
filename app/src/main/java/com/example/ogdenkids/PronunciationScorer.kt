package com.example.ogdenkids

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext
import kotlin.math.abs

data class WordScore(val word: String, val score: Double, val errorType: String)

data class Assessment(
    val pron: Double,
    val accuracy: Double,
    val fluency: Double,
    val completeness: Double,
    /** 与参考句的词一一对应（已去掉多读的词）；对不齐时为空 */
    val words: List<WordScore>,
    val recognized: String
)

/**
 * 跟读：录 16kHz 单声道 PCM，交给 Azure 发音评估（REST 短音频接口，用语音服务的 key / region）。
 * 录音只用于这一次评分，不落盘。
 */
object PronunciationScorer {
    private const val SAMPLE_RATE = 16000
    private const val MAX_MS = 15_000
    // 开口之后连续这么久没声音就自动结束
    private const val TRAILING_SILENCE_MS = 1_500
    private const val VOICE_PEAK = 1_500

    /** 录到 [shouldStop] 为真、开口后静音够久或满 15 秒为止。调用方负责先拿到录音权限。 */
    @SuppressLint("MissingPermission")
    suspend fun record(shouldStop: () -> Boolean, onLevel: (Float) -> Unit): ByteArray = withContext(Dispatchers.IO) {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBuffer, SAMPLE_RATE / 5 * 2) * 2
        )
        check(recorder.state == AudioRecord.STATE_INITIALIZED) { "麦克风不可用" }
        val out = ByteArrayOutputStream()
        val chunk = ShortArray(SAMPLE_RATE / 10) // 100ms
        var heard = false
        var silentMs = 0
        var totalMs = 0
        try {
            recorder.startRecording()
            while (coroutineContext.isActive && !shouldStop() && totalMs < MAX_MS) {
                val n = recorder.read(chunk, 0, chunk.size)
                if (n <= 0) continue
                var peak = 0
                for (i in 0 until n) {
                    val v = chunk[i].toInt()
                    out.write(v and 0xff)
                    out.write((v shr 8) and 0xff)
                    peak = maxOf(peak, abs(v))
                }
                onLevel(peak / 32768f)
                totalMs += 100
                if (peak >= VOICE_PEAK) {
                    heard = true
                    silentMs = 0
                } else if (heard) {
                    silentMs += 100
                    if (silentMs >= TRAILING_SILENCE_MS) break
                }
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
        out.toByteArray()
    }

    suspend fun assess(pcm: ByteArray, reference: String): Result<Assessment> = withContext(Dispatchers.IO) {
        runCatching {
            val key = BuildConfig.AZURE_SPEECH_KEY
            val region = BuildConfig.AZURE_SPEECH_REGION
            check(key.isNotBlank() && region.isNotBlank()) { "未配置语音服务" }
            val config = JSONObject()
                .put("ReferenceText", reference)
                .put("GradingSystem", "HundredMark")
                .put("Granularity", "Word")
                .put("Dimension", "Comprehensive")
                .put("EnableMiscue", true)
            val connection = URL("https://$region.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=en-US&format=detailed")
                .openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 10_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", key)
                connection.setRequestProperty("Content-Type", "audio/wav; codecs=audio/pcm; samplerate=16000")
                connection.setRequestProperty(
                    "Pronunciation-Assessment",
                    Base64.encodeToString(config.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                )
                connection.outputStream.use { it.write(wav(pcm)) }
                check(connection.responseCode == HttpURLConnection.HTTP_OK) { "HTTP ${connection.responseCode}" }
                parse(connection.inputStream.bufferedReader().use { it.readText() }, reference)
            } finally {
                connection.disconnect()
            }
        }
    }

    /** 回放自己的录音 */
    fun play(pcm: ByteArray): AudioTrack {
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(SAMPLE_RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(maxOf(pcm.size, 2))
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        return track
    }

    private fun parse(json: String, reference: String): Assessment {
        val root = JSONObject(json)
        check(root.optString("RecognitionStatus") == "Success") { "没有听清楚" }
        val best = root.getJSONArray("NBest").getJSONObject(0)
        // 新旧版本的返回结构不同：分数可能平铺在 NBest 上，也可能在 PronunciationAssessment 里
        fun scores(o: JSONObject) = o.optJSONObject("PronunciationAssessment") ?: o
        val pa = scores(best)
        val wordsJson = best.optJSONArray("Words")
        val words = (0 until (wordsJson?.length() ?: 0)).map { i ->
            val w = wordsJson!!.getJSONObject(i)
            val s = scores(w)
            WordScore(w.optString("Word"), s.optDouble("AccuracyScore", 0.0), s.optString("ErrorType", "None"))
        }.filter { it.errorType != "Insertion" }
        val referenceCount = tokenizeSpeech(reference).size
        return Assessment(
            pron = pa.optDouble("PronScore", 0.0),
            accuracy = pa.optDouble("AccuracyScore", 0.0),
            fluency = pa.optDouble("FluencyScore", 0.0),
            completeness = pa.optDouble("CompletenessScore", 0.0),
            words = if (words.size == referenceCount) words else emptyList(),
            recognized = best.optString("Display")
        )
    }

    private fun wav(pcm: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        fun int(v: Int) { for (i in 0..3) out.write((v shr (8 * i)) and 0xff) }
        fun short(v: Int) { out.write(v and 0xff); out.write((v shr 8) and 0xff) }
        out.write("RIFF".toByteArray()); int(36 + pcm.size); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); int(16); short(1); short(1); int(SAMPLE_RATE); int(SAMPLE_RATE * 2); short(2); short(16)
        out.write("data".toByteArray()); int(pcm.size); out.write(pcm)
        return out.toByteArray()
    }
}
