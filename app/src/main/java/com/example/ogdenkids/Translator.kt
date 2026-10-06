package com.example.ogdenkids

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 划词翻译：Azure Translator（英 → 简体中文）。结果按原文缓存在本机，同一段只请求一次。
 * key / region 来自 local.properties 的 azure.translator.key / azure.translator.region（构建时注入）。
 */
class Translator(context: Context) {
    private val prefs = context.getSharedPreferences("translations", Context.MODE_PRIVATE)

    suspend fun translate(rawText: String): Result<String> = withContext(Dispatchers.IO) {
        val text = rawText.trim()
        prefs.getString(text, null)?.let { return@withContext Result.success(it) }
        runCatching { request(text) }.onSuccess { prefs.edit().putString(text, it).apply() }
    }

    private fun request(text: String): String {
        val key = BuildConfig.AZURE_TRANSLATOR_KEY
        check(key.isNotBlank()) { "未配置翻译密钥" }
        val connection = URL("https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=en&to=zh-Hans")
            .openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Ocp-Apim-Subscription-Key", key)
            // global 资源不需要区域头；区域资源必须带
            BuildConfig.AZURE_TRANSLATOR_REGION.takeIf { it.isNotBlank() && it != "global" }
                ?.let { connection.setRequestProperty("Ocp-Apim-Subscription-Region", it) }
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            val body = JSONArray().put(JSONObject().put("Text", text)).toString()
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            check(code == HttpURLConnection.HTTP_OK) { "HTTP $code" }
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONArray(response).getJSONObject(0).getJSONArray("translations").getJSONObject(0).getString("text")
        } finally {
            connection.disconnect()
        }
    }
}
