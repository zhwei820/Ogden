package com.example.ogdenkids

import android.content.Context
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

/** 本地评分模型的状态：没下载 / 下载中（0~1）/ 下载失败 / 可用 */
sealed class AsrModelState {
    object Missing : AsrModelState()
    data class Downloading(val progress: Float) : AsrModelState()
    data class Failed(val message: String) : AsrModelState()
    object Ready : AsrModelState()
}

/**
 * 本地跟读评分：sherpa-onnx 离线识别（zipformer-small-en，int8，约 28MB），识别结果交给 [ReadingScore] 打分。
 * 模型不进 APK，首次跟读时下载到 filesDir，逐个文件校验 sha256。录音只在内存里用，不落盘。
 *
 * 模型只用成人朗读（LibriSpeech）训练过，孩子的声音容易被听成别的词或漏掉小词。所以解码用 modified_beam_search，
 * 并把参考句里的词作为热词传给识别器：拿不准时偏向孩子该读的词。热词只加分不强制，读成别的句子照样识别成别的句子。
 * 热词要把英文词切成模型的 BPE 片段，切分表 assets/asr/bpe.vocab 由该模型配套的 icefall lang_bpe_500/bpe.model 导出
 * （500 个片段与模型 tokens.txt 逐个一致）。
 */
object LocalAsr {
    private const val ModelName = "sherpa-onnx-zipformer-small-en-2023-06-26"
    private const val BpeVocabAsset = "asr/bpe.vocab"
    /** 热词加分：sherpa-onnx 默认 1.5；调大更容易认成参考句里的词，但读错也更可能被"听"成读对 */
    private const val HotwordsScore = 1.5f

    private class ModelFile(val name: String, val size: Long, val sha256: String)

    private val Files = listOf(
        ModelFile("encoder-epoch-99-avg-1.int8.onnx", 26_015_366, "3a6ac78a31cc2c60ca8c1e2e2f43c878fbbcaf051ada4900e0a42ef8ba53d375"),
        ModelFile("decoder-epoch-99-avg-1.int8.onnx", 1_307_236, "f462ab9189ba6f9b2658774e6bf3d651913de54a3833655dc5e093e2f5e4c2b6"),
        ModelFile("joiner-epoch-99-avg-1.int8.onnx", 259_335, "6b183b6ec656e4d3ca6b86d0aeca992dac14df80aae6b416d7c068d0ff2bd4d7"),
        ModelFile("tokens.txt", 5_048, "49e3c2646595fd907228b3c6787069658f67b17377c60aeb8619c4551b2316fb")
    )
    /** 下载体积（MB，四舍五入），给界面提示用 */
    val DownloadMb = (Files.sumOf { it.size } + 500_000) / 1_000_000

    /** 依次尝试的下载地址：local.properties 里自建的镜像优先，然后是 hf-mirror 和 Hugging Face */
    private val BaseUrls = listOfNotNull(
        BuildConfig.ASR_MODEL_BASE_URL.trim().takeIf { it.isNotEmpty() }?.let { if (it.endsWith("/")) it else "$it/" },
        "https://hf-mirror.com/csukuangfj/$ModelName/resolve/main/",
        "https://huggingface.co/csukuangfj/$ModelName/resolve/main/"
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var downloadJob: Job? = null
    private val _state = MutableStateFlow<AsrModelState>(AsrModelState.Missing)
    val state: StateFlow<AsrModelState> = _state.asStateFlow()

    private val recognizerLock = Mutex()
    private var recognizer: OfflineRecognizer? = null
    private var homophones: Homophones? = null

    private fun dir(context: Context) = File(context.filesDir, "asr/$ModelName")

    /** 检查模型是否已下载齐；进入跟读界面时调一次 */
    fun refresh(context: Context) {
        if (_state.value is AsrModelState.Downloading) return
        val ready = Files.all { File(dir(context), it.name).length() == it.size }
        _state.value = if (ready) AsrModelState.Ready else AsrModelState.Missing
    }

    /** 开始下载；已在下载时忽略。关掉跟读弹窗不会中断下载。 */
    fun startDownload(context: Context) {
        if (downloadJob?.isActive == true) return
        val app = context.applicationContext
        downloadJob = scope.launch {
            _state.value = AsrModelState.Downloading(0f)
            _state.value = runCatching { download(app); AsrModelState.Ready }
                .getOrElse { AsrModelState.Failed("下载失败，请检查网络后重试") }
        }
    }

    private suspend fun download(context: Context) {
        val target = dir(context).apply { mkdirs() }
        val total = Files.sumOf { it.size }.toFloat()
        var done = 0L
        for (file in Files) {
            val out = File(target, file.name)
            if (out.length() != file.size) {
                var lastError: Throwable? = null
                val ok = BaseUrls.any { base ->
                    runCatching { fetch(base + file.name, out, file) { done + it } }
                        .onFailure { lastError = it; coroutineContext.ensureActive() }
                        .isSuccess
                }
                if (!ok) throw lastError ?: IllegalStateException("下载失败")
            }
            done += file.size
            _state.value = AsrModelState.Downloading(done / total)
        }
    }

    /** 下到 .part，边下边算 sha256，对上了才改名；[overall] 把本文件已下字节换算成总进度字节 */
    private suspend fun fetch(url: String, out: File, file: ModelFile, overall: (Long) -> Long) {
        val part = File(out.path + ".part")
        val total = Files.sumOf { it.size }.toFloat()
        val connection = open(url)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var read = 0L
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        coroutineContext.ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        read += n
                        _state.value = AsrModelState.Downloading((overall(read) / total).coerceAtMost(1f))
                    }
                }
            }
            val sha = digest.digest().joinToString("") { "%02x".format(it) }
            check(read == file.size && sha == file.sha256) { "${file.name} 校验失败" }
            out.delete()
            check(part.renameTo(out)) { "${file.name} 保存失败" }
        } finally {
            connection.disconnect()
            part.delete()
        }
    }

    /** 手动跟随跳转：hf-mirror 用 308 跳到 Hugging Face，系统 HttpURLConnection 不一定认 308 */
    private fun open(url: String): HttpURLConnection {
        var current = URL(url)
        repeat(6) {
            val connection = current.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            when (val code = connection.responseCode) {
                HttpURLConnection.HTTP_OK -> return connection
                301, 302, 303, 307, 308 -> {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    checkNotNull(location) { "HTTP $code 没有 Location" }
                    current = URL(current, location)
                }
                else -> {
                    connection.disconnect()
                    error("HTTP $code")
                }
            }
        }
        error("跳转次数过多")
    }

    /**
     * 识别并打分。模型未就绪时失败；没识别出词时失败信息为"没有听清楚"（与 Azure 评分一致，界面按它提示）。
     * 本机不支持 sherpa-onnx（如没有 ARM 转译的 x86 模拟器）时抛 [UnsupportedOperationException]。
     */
    suspend fun assess(context: Context, pcm: ByteArray, reference: String): Result<Assessment> = withContext(Dispatchers.Default) {
        runCatching {
            refresh(context)
            check(_state.value == AsrModelState.Ready) { "评分模型还没下载" }
            val samples = FloatArray(pcm.size / 2) { i ->
                ((pcm[2 * i].toInt() and 0xff) or (pcm[2 * i + 1].toInt() shl 8)).toShort() / 32768f
            }
            val (tokens, timestamps, dict) = recognizerLock.withLock {
                val r = recognizer ?: try {
                    create(context).also { recognizer = it }
                } catch (e: Throwable) {
                    // UnsatisfiedLinkError 等 Error 也要接住，交给调用方回退
                    throw UnsupportedOperationException("本地评分引擎不可用", e)
                }
                val stream = r.createStream(hotwordsFor(reference))
                try {
                    stream.acceptWaveform(samples, 16000)
                    r.decode(stream)
                    val result = r.getResult(stream)
                    val h = homophones ?: Homophones.fromJson(context.assets.open("ogden_ipa.json").bufferedReader().use { it.readText() })
                        .also { homophones = it }
                    Triple(result.tokens.toList(), result.timestamps.toList(), h)
                } finally {
                    stream.release()
                }
            }
            ReadingScore.score(reference, tokens, timestamps, dict) ?: error("没有听清楚")
        }
    }

    /** 参考句里的词（连字符词拆开、去重、转大写，和模型词表一致），一行一个；编码失败的热词 sherpa-onnx 会跳过 */
    internal fun hotwordsFor(reference: String): String =
        tokenizeSpeech(reference)
            .flatMap { it.text.split('-') }
            .map { w -> w.uppercase().filter { it in 'A'..'Z' || it == '\'' }.trim('\'') }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")

    private fun create(context: Context): OfflineRecognizer {
        val d = dir(context).path
        val bpeVocab = File(d, "bpe.vocab")
        context.assets.open(BpeVocabAsset).use { input -> bpeVocab.outputStream().use { input.copyTo(it) } }
        return OfflineRecognizer(
            assetManager = null,
            config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    transducer = OfflineTransducerModelConfig(
                        encoder = "$d/encoder-epoch-99-avg-1.int8.onnx",
                        decoder = "$d/decoder-epoch-99-avg-1.int8.onnx",
                        joiner = "$d/joiner-epoch-99-avg-1.int8.onnx"
                    ),
                    tokens = "$d/tokens.txt",
                    modelType = "transducer",
                    modelingUnit = "bpe",
                    bpeVocab = bpeVocab.path,
                    numThreads = 2
                ),
                decodingMethod = "modified_beam_search",
                hotwordsScore = HotwordsScore
            )
        )
    }
}
