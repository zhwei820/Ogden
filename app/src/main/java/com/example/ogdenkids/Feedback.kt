package com.example.ogdenkids

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 意见反馈：发到企业微信群机器人。
 * webhook 来自 local.properties 的 wecom.feedback.webhook（构建时注入）；它会随 APK 分发，
 * 拿到的人都能往群里发消息，泄露后在群机器人设置里重置 key 即可。
 * 用 text 而不是 markdown 消息：用户输入里的 # * < 会被 markdown 渲染吞掉。
 */
object Feedback {
    /** 正文上限按字符计，保证整条消息低于 text 消息的 2048 字节（中文 3 字节/字）。 */
    const val MAX_CONTENT_CHARS = 500
    const val MAX_CONTACT_CHARS = 50

    val isConfigured: Boolean get() = BuildConfig.WECOM_FEEDBACK_WEBHOOK.isNotBlank()

    suspend fun send(content: String, contact: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val webhook = BuildConfig.WECOM_FEEDBACK_WEBHOOK
            check(webhook.isNotBlank()) { "未配置反馈通道" }
            val message = message(
                content = content,
                contact = contact,
                appVersion = BuildConfig.VERSION_NAME,
                device = "${Build.MANUFACTURER} ${Build.MODEL}",
                android = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
            )
            post(webhook, message)
        }
    }

    fun message(content: String, contact: String, appVersion: String, device: String, android: String): String =
        buildString {
            append("【Panda English 反馈】\n")
            append(content.trim().take(MAX_CONTENT_CHARS)).append("\n\n")
            contact.trim().take(MAX_CONTACT_CHARS).takeIf { it.isNotEmpty() }?.let { append("联系方式：").append(it).append('\n') }
            append("版本：").append(appVersion).append('\n')
            append("设备：").append(device).append(" · ").append(android)
        }

    private fun post(webhook: String, message: String) {
        val connection = URL(webhook).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val body = JSONObject()
                .put("msgtype", "text")
                .put("text", JSONObject().put("content", message))
                .toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "HTTP $code" }
            // 企业微信出错时 HTTP 仍是 200，要看 errcode（如限流 45009：每个机器人每分钟 20 条）
            val result = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val errcode = result.optInt("errcode", -1)
            check(errcode == 0) { "errcode $errcode: ${result.optString("errmsg")}" }
        } finally {
            connection.disconnect()
        }
    }
}
