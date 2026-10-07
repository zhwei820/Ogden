package com.example.ogdenkids

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * 可下载的资源包：词包（words.json + 发音）或语音包（tts/ 下的句子朗读）。
 * zip 内路径与 assets 一致（audio/us/…、tts/en-US/…），查找发音时同一个相对路径先查已装的包、再查 assets。
 */
enum class PackType(val code: String, val label: String) {
    Words("words", "词包"),
    Voice("voice", "语音包");

    companion object {
        fun fromCode(code: String) = values().firstOrNull { it.code == code }
    }
}

data class RemotePack(
    val id: String,
    val type: PackType,
    val title: String,
    val description: String,
    val version: Int,
    val size: Long,
    val sha256: String,
    val url: String
)

enum class PackStatus { NotInstalled, Installed, UpdateAvailable }

fun packStatus(remoteVersion: Int, installedVersion: Int?): PackStatus = when {
    installedVersion == null -> PackStatus.NotInstalled
    remoteVersion > installedVersion -> PackStatus.UpdateAvailable
    else -> PackStatus.Installed
}

/** 解析服务器清单；类型不认识或缺字段的条目跳过，不让一条坏数据拖垮整个列表。 */
fun parseManifest(json: String): List<RemotePack> {
    val packs = JSONObject(json).getJSONArray("packs")
    return List(packs.length()) { packs.getJSONObject(it) }.mapNotNull { item ->
        runCatching {
            RemotePack(
                id = item.getString("id").also { require(isSafePackId(it)) { "bad id $it" } },
                type = PackType.fromCode(item.getString("type")) ?: return@mapNotNull null,
                title = item.getString("title"),
                description = item.optString("description"),
                version = item.getInt("version"),
                size = item.getLong("size"),
                sha256 = item.getString("sha256").lowercase(),
                url = item.getString("url")
            )
        }.getOrNull()
    }
}

// id 会拼进本地目录名，只放行字母数字和 - _，防 "../" 之类写到 packs 目录外
private fun isSafePackId(id: String) = id.matches(Regex("[A-Za-z0-9_-]{1,64}"))

/**
 * 本机已装的资源包：filesDir/packs/<id>/ 下是解压后的文件，installed.json 记 id → {version, type}。
 * 读写都在同一把锁里；查找发音在主线程调用，只做内存查表 + File.exists。
 */
class PackStore(private val root: File) {
    private data class Installed(val version: Int, val type: PackType)

    private val indexFile = File(root, "installed.json")
    @Volatile private var installed: Map<String, Installed> = readIndex()

    fun installedVersion(id: String): Int? = installed[id]?.version

    /** 已装包里与 assets 同一相对路径的文件（如 "audio/us/penguin.mp3"），没有则 null。 */
    fun findFile(relativePath: String): File? =
        installed.keys.asSequence().map { File(File(root, it), relativePath) }.firstOrNull { it.isFile }

    /** 所有已装词包的 words.json 内容。 */
    fun readWordPackJsons(): List<String> = installed.filterValues { it.type == PackType.Words }.keys
        .mapNotNull { File(File(root, it), "words.json").takeIf(File::isFile)?.readText() }

    /** 把已校验的 zip 解压安装为 [pack]；先解到临时目录，整体改名后才生效，中途失败不留半个包。 */
    @Synchronized
    fun install(pack: RemotePack, zip: File) {
        val staging = File(root, "${pack.id}.tmp").apply { deleteRecursively() }
        try {
            unzipTo(zip, staging)
            if (pack.type == PackType.Words && !File(staging, "words.json").isFile) throw IOException("词包缺少 words.json")
            val target = File(root, pack.id)
            target.deleteRecursively()
            if (!staging.renameTo(target)) throw IOException("无法写入 ${target.path}")
            writeIndex(installed + (pack.id to Installed(pack.version, pack.type)))
        } finally {
            staging.deleteRecursively()
        }
    }

    @Synchronized
    fun remove(id: String) {
        writeIndex(installed - id)
        File(root, id).deleteRecursively()
    }

    private fun writeIndex(next: Map<String, Installed>) {
        root.mkdirs()
        val json = JSONObject()
        next.forEach { (id, item) -> json.put(id, JSONObject().put("version", item.version).put("type", item.type.code)) }
        val partial = File(indexFile.path + ".part")
        partial.writeText(json.toString())
        if (!partial.renameTo(indexFile)) throw IOException("无法写入 ${indexFile.path}")
        installed = next
    }

    private fun readIndex(): Map<String, Installed> {
        if (!indexFile.isFile) return emptyMap()
        val json = runCatching { JSONObject(indexFile.readText()) }.getOrElse { return emptyMap() }
        return json.keys().asSequence().mapNotNull { id ->
            val item = json.optJSONObject(id) ?: return@mapNotNull null
            val type = PackType.fromCode(item.optString("type")) ?: return@mapNotNull null
            id to Installed(item.optInt("version"), type)
        }.toMap()
    }

    companion object {
        @Volatile private var instance: PackStore? = null

        fun of(context: Context): PackStore = instance ?: synchronized(this) {
            instance ?: PackStore(File(context.filesDir, "packs")).also { instance = it }
        }
    }
}

/** 解压 [zip] 到 [dest]；条目路径落到 dest 之外（"../"、绝对路径）直接拒绝，防 zip slip。 */
fun unzipTo(zip: File, dest: File) {
    val base = dest.canonicalFile
    ZipInputStream(zip.inputStream().buffered()).use { input ->
        generateSequence { input.nextEntry }.forEach { entry ->
            val out = File(base, entry.name).canonicalFile
            if (!out.path.startsWith(base.path + File.separator)) throw IOException("包内路径越界：${entry.name}")
            if (entry.isDirectory) {
                out.mkdirs()
            } else {
                out.parentFile?.mkdirs()
                out.outputStream().use { input.copyTo(it) }
            }
        }
    }
}

/** 从 [baseUrl]（如 http://127.0.0.1:8765）拉清单、下载资源包。 */
class PackClient(private val baseUrl: String) {
    suspend fun fetchManifest(): Result<List<RemotePack>> = withContext(Dispatchers.IO) {
        runCatching { parseManifest(openOk("/manifest.json").inputStream.bufferedReader().use { it.readText() }) }
    }

    /**
     * 下载 [pack] 到临时文件，校验 sha256 后交给 [store] 安装。
     * @param onProgress 已下载字节数、总字节数（来自清单），在 IO 线程回调
     */
    suspend fun downloadAndInstall(pack: RemotePack, store: PackStore, tempDir: File, onProgress: (Long, Long) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val partial = File(tempDir, "${pack.id}.zip.part")
            runCatching {
                val connection = openOk(pack.url)
                val digest = MessageDigest.getInstance("SHA-256")
                var received = 0L
                try {
                    connection.inputStream.use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                // 离开下载页会取消协程：停在这里就不会装上一个页面已经不在的包
                                ensureActive()
                                output.write(buffer, 0, n)
                                digest.update(buffer, 0, n)
                                received += n
                                onProgress(received, pack.size)
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                verifySha256(digest.digest(), pack.sha256)
                store.install(pack, partial)
            }.also { partial.delete() }
        }

    private fun openOk(path: String): HttpURLConnection {
        val connection = URL(URL(baseUrl), path).openConnection() as HttpURLConnection
        connection.connectTimeout = 10_000
        connection.readTimeout = 30_000
        val code = connection.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            throw IOException("服务器返回 HTTP $code")
        }
        return connection
    }
}

fun verifySha256(actual: ByteArray, expectedHex: String) {
    val hex = actual.joinToString("") { "%02x".format(it) }
    if (hex != expectedHex) throw IOException("文件校验失败（sha256 不符）")
}

/**
 * 软件设置 → 资源下载：列出服务器上的资源包，可下载、更新、删除。
 * @param onPacksChanged 装好或删掉包之后调用，让词库重新加载
 */
@Composable
fun ResourcePacksScreen(onBack: () -> Unit, onPacksChanged: () -> Unit) {
    val context = LocalContext.current
    val store = remember { PackStore.of(context) }
    val client = remember { BuildConfig.PACK_SERVER_URL.takeIf { it.isNotBlank() }?.let(::PackClient) }
    val scope = rememberCoroutineScope()
    var packs by remember { mutableStateOf<List<RemotePack>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var reloadCount by remember { mutableStateOf(0) }
    // 安装记录在 PackStore 里，不是 Compose 状态；装 / 删后递增它让状态列重算
    var storeVersion by remember { mutableStateOf(0) }
    val progress = remember { mutableStateMapOf<String, Float>() }
    val errors = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(reloadCount) {
        if (client == null) return@LaunchedEffect
        loadError = null
        client.fetchManifest()
            .onSuccess { packs = it }
            .onFailure { loadError = "连不上资源服务器：${it.message}" }
    }

    fun download(pack: RemotePack) {
        if (client == null || pack.id in progress) return
        errors.remove(pack.id)
        progress[pack.id] = 0f
        scope.launch {
            client.downloadAndInstall(pack, store, context.cacheDir) { got, total ->
                if (total > 0) progress[pack.id] = (got.toFloat() / total).coerceIn(0f, 1f)
            }.onSuccess { onPacksChanged() }
                .onFailure { errors[pack.id] = it.message ?: "下载失败" }
            progress.remove(pack.id)
            storeVersion++
        }
    }

    fun remove(pack: RemotePack) {
        scope.launch {
            withContext(Dispatchers.IO) { store.remove(pack.id) }
            storeVersion++
            onPacksChanged()
        }
    }

    Scaffold(containerColor = Paper, topBar = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(PaperElevated)
                .border(1.dp, Line)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
            AppText("资源下载", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { SectionTitle("资源下载", "额外的单词和语音，下载后离线可用") }
            val list = packs
            when {
                client == null -> item { AppText("资源下载暂未开放", color = InkFaint) }
                loadError != null -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AppText(loadError!!, color = Error)
                        OutlinedButton(onClick = { reloadCount++ }) { AppText("重试") }
                    }
                }
                list == null -> item { AppText("正在获取资源列表……", color = InkFaint) }
                list.isEmpty() -> item { AppText("暂时没有可下载的资源", color = InkFaint) }
                else -> items(list, key = { it.id }) { pack ->
                    val status = remember(pack, storeVersion) { packStatus(pack.version, store.installedVersion(pack.id)) }
                    PackCard(pack, status, progress[pack.id], errors[pack.id], onDownload = { download(pack) }, onRemove = { remove(pack) })
                }
            }
        }
    }
}

@Composable
private fun PackCard(
    pack: RemotePack,
    status: PackStatus,
    progress: Float?,
    error: String?,
    onDownload: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PaperElevated),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Line, RoundedCornerShape(18.dp))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppText(pack.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            AppText("${pack.type.label} · ${formatPackSize(pack.size)} · v${pack.version}", color = InkFaint, fontSize = 13.sp)
            if (pack.description.isNotBlank()) AppText(pack.description, color = InkSoft, fontSize = 14.sp)
            error?.let { AppText(it, color = Error, fontSize = 14.sp) }
            if (progress != null) {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                AppText("下载中 ${(progress * 100).toInt()}%", color = InkFaint, fontSize = 13.sp)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    when (status) {
                        PackStatus.NotInstalled -> Button(onClick = onDownload) { AppText("下载") }
                        PackStatus.UpdateAvailable -> Button(onClick = onDownload) { AppText("更新") }
                        PackStatus.Installed -> AppText("已下载", color = Success, fontSize = 15.sp)
                    }
                    if (status != PackStatus.NotInstalled) OutlinedButton(onClick = onRemove) { AppText("删除") }
                }
            }
        }
    }
}

private fun formatPackSize(bytes: Long): String =
    if (bytes >= 1024 * 1024) "%.1f MB".format(bytes / 1024.0 / 1024.0) else "${(bytes + 1023) / 1024} KB"
