package com.burton.builds.data.github

import com.burton.builds.data.parse.TinyJson
import com.burton.builds.data.parse.TinyJson.objList
import com.burton.builds.data.parse.TinyJson.parseList
import com.burton.builds.data.parse.TinyJson.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubApi @Inject constructor(
    private val client: OkHttpClient,
) {
    suspend fun getObject(path: String, params: Map<String, String> = emptyMap()): Map<String, Any?> =
        request("GET", path, params, body = null).first

    suspend fun getList(path: String, params: Map<String, String> = emptyMap()): List<Map<String, Any?>> {
        val (obj, raw) = request("GET", path, params, body = null)
        if (obj.containsKey("items") || obj.containsKey("message")) {
            return obj.objList("items").ifEmpty {
                if (raw.trimStart().startsWith("[")) parseList(raw) else emptyList()
            }
        }
        return if (raw.trimStart().startsWith("[")) parseList(raw) else emptyList()
    }

    suspend fun post(path: String, body: Map<String, Any?> = emptyMap(), params: Map<String, String> = emptyMap()): Map<String, Any?> =
        request("POST", path, params, TinyJson.stringify(body)).first

    suspend fun formPost(url: String, fields: Map<String, String>): Map<String, Any?> = withContext(Dispatchers.IO) {
        val encoded = fields.entries.joinToString("&") { (key, value) ->
            "${GitHubOAuth.encode(key)}=${GitHubOAuth.encode(value)}"
        }
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(encoded.toRequestBody("application/x-www-form-urlencoded; charset=utf-8".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            val parsed = if (text.isBlank()) emptyMap() else TinyJson.parseObject(text)
            if (!response.isSuccessful) {
                throw GitHubApiException(
                    url,
                    parsed.str("error").ifBlank { "http_${response.code}" },
                    parsed.str("error_description").ifBlank { parsed.str("message").ifBlank { "http_${response.code}" } },
                )
            }
            parsed
        }
    }

    private suspend fun request(
        method: String,
        path: String,
        params: Map<String, String>,
        body: String?,
    ): Pair<Map<String, Any?>, String> = withContext(Dispatchers.IO) {
        val url = if (path.startsWith("http")) {
            path.toHttpUrl().newBuilder()
        } else {
            "$HOST/$path".toHttpUrl().newBuilder()
        }
        params.forEach { (key, value) -> if (value.isNotBlank()) url.addQueryParameter(key, value) }
        val builder = Request.Builder()
            .url(url.build())
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
        when (method) {
            "POST" -> builder.post((body ?: "{}").toRequestBody(JSON))
            else -> builder.get()
        }
        client.newCall(builder.build()).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (response.code == 204 || text.isBlank()) {
                if (!response.isSuccessful && response.code != 204) {
                    throw GitHubApiException(path, "http_${response.code}")
                }
                return@use emptyMap<String, Any?>() to text
            }
            val parsed = if (text.trimStart().startsWith("[")) {
                mapOf("items" to TinyJson.parse(text))
            } else {
                TinyJson.parseObject(text)
            }
            if (!response.isSuccessful) {
                val message = parsed.str("message").ifBlank { "http_${response.code}" }
                val detail = parsed.objList("errors").firstOrNull()?.str("message").orEmpty()
                throw GitHubApiException(path, "http_${response.code}", detail.ifBlank { message })
            }
            parsed to text
        }
    }

    companion object {
        const val HOST = "https://api.github.com"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
