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
 * 单词走词典接口取置信度前三的义项：/translate 对单词只给一个不看语境的译文（acquire → 收购）。
 * key / region 来自 local.properties 的 azure.translator.key / azure.translator.region（构建时注入）。
 */
class Translator(context: Context) {
    private val prefs = context.getSharedPreferences("translations_v2", Context.MODE_PRIVATE)

    suspend fun translate(rawText: String): Result<String> = withContext(Dispatchers.IO) {
        val text = rawText.trim()
        prefs.getString(text, null)?.let { return@withContext Result.success(it) }
        runCatching {
            val senses = if (SINGLE_WORD.matches(text)) lookup(text) else emptyList()
            senses.takeIf { it.isNotEmpty() }?.joinToString("；") ?: translateText(text)
        }.onSuccess { prefs.edit().putString(text, it).apply() }
    }

    private fun translateText(text: String): String =
        post("translate", text).getJSONObject(0).getJSONArray("translations").getJSONObject(0).getString("text")

    private fun lookup(word: String): List<String> {
        val translations = post("dictionary/lookup", word).getJSONObject(0).getJSONArray("translations")
        return (0 until translations.length())
            .map { translations.getJSONObject(it) }
            .sortedByDescending { it.getDouble("confidence") }
            .map { it.getString("displayTarget") }
            .distinct()
            .take(3)
    }

    private fun post(path: String, text: String): JSONArray {
        val key = BuildConfig.AZURE_TRANSLATOR_KEY
        check(key.isNotBlank()) { "未配置翻译密钥" }
        val connection = URL("https://api.cognitive.microsofttranslator.com/$path?api-version=3.0&from=en&to=zh-Hans")
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
            JSONArray(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val SINGLE_WORD = Regex("[A-Za-z][A-Za-z'-]*")
    }
}
