package com.scanpilot.app.network

import com.scanpilot.app.data.AiSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * AI summary via a plain HTTP call to an OpenAI-compatible API.
 * Default: CoArena — but no key is in the code; the user enters their
 * own key in Settings. Without a key, this service is never called and
 * an "API key required" message is shown.
 */
class AiSummaryService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun summarize(text: String, settings: AiSettings): Result<String> {
        if (settings.apiKey.isBlank()) {
            return Result.failure(
                IllegalStateException("For the AI summary, first enter your API key in Settings.")
            )
        }
        if (text.isBlank()) {
            return Result.failure(
                IllegalStateException("This document has no readable text; it is probably scanned. Summarizing scanned documents needs OCR, which has not been added yet.")
            )
        }
        return withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("model", settings.model)
                    put("temperature", 0.3)
                    put("max_tokens", 1000)
                    put("messages", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", "You are a summarization assistant. Summarize the document text in English, briefly and clearly, in a few bullet points.")
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", text)
                        })
                    })
                }
                val request = Request.Builder()
                    .url("${settings.baseUrl}/chat/completions")
                    .addHeader("Authorization", "Bearer ${settings.apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(
                            RuntimeException("Error from the AI server (${response.code}); check your key and address in Settings.")
                        )
                    }
                    val content = JSONObject(responseBody)
                        .optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content")
                        .orEmpty()
                        .trim()
                    if (content.isEmpty()) {
                        Result.failure(RuntimeException("Received an empty response from the server."))
                    } else {
                        Result.success(content)
                    }
                }
            } catch (e: Exception) {
                Result.failure(RuntimeException("Could not connect to the AI server; check your internet connection and address."))
            }
        }
    }
}
