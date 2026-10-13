package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Video
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiAssistant {
  private const val TAG = "GeminiAssistant"
  private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

  private val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()

  fun isConfigured(): Boolean {
    val key = try {
      BuildConfig.GEMINI_API_KEY
    } catch (e: Throwable) {
      ""
    }
    return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
  }

  suspend fun generateVideoSummary(video: Video): String = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext generateLocalSummary(video)
    }

    val prompt = """
      You are the Smart Watch Companion AI for the video streaming app Lumina.
      Generate a concise, insightful 3-point study summary and key takeaways for this video:
      Title: "${video.title}"
      Channel: "${video.channelTitle}"
      Category: "${video.category}"
      Description: "${video.description}"
      Duration: ${video.formattedDuration}
      
      Format with clear bullet points highlighting key concepts, timestamps relevance, and notable insights.
    """.trimIndent()

    try {
      callGeminiApi(prompt)
    } catch (e: Exception) {
      Log.w(TAG, "Gemini API call failed, falling back to local analysis", e)
      generateLocalSummary(video)
    }
  }

  suspend fun askQuestionAboutVideo(video: Video, userQuestion: String): String = withContext(Dispatchers.IO) {
    if (!isConfigured()) {
      return@withContext generateLocalAnswer(video, userQuestion)
    }

    val prompt = """
      You are the Smart Watch Companion AI for the video streaming app Lumina.
      The viewer is watching:
      Title: "${video.title}"
      Channel: "${video.channelTitle}"
      Description: "${video.description}"
      Chapters: ${video.chapters.joinToString { "${it.formattedTime} - ${it.title}" }}
      
      The viewer asks: "$userQuestion"
      
      Answer clearly, accurately, and informatively based on the video's subject matter. Keep it under 150 words.
    """.trimIndent()

    try {
      callGeminiApi(prompt)
    } catch (e: Exception) {
      Log.w(TAG, "Gemini Q&A call failed, falling back to local analysis", e)
      generateLocalAnswer(video, userQuestion)
    }
  }

  private fun callGeminiApi(promptText: String): String {
    val apiKey = BuildConfig.GEMINI_API_KEY
    val url = "$BASE_URL?key=$apiKey"

    val jsonBody = JSONObject().apply {
      put("contents", JSONArray().apply {
        put(JSONObject().apply {
          put("parts", JSONArray().apply {
            put(JSONObject().apply {
              put("text", promptText)
            })
          })
        })
      })
    }

    val request = Request.Builder()
      .url(url)
      .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
      .build()

    client.newCall(request).execute().use { response ->
      val body = response.body?.string() ?: ""
      if (!response.isSuccessful) {
        throw RuntimeException("Gemini HTTP ${response.code}: $body")
      }
      val json = JSONObject(body)
      val candidates = json.optJSONArray("candidates")
      if (candidates != null && candidates.length() > 0) {
        val firstCandidate = candidates.getJSONObject(0)
        val content = firstCandidate.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        if (parts != null && parts.length() > 0) {
          return parts.getJSONObject(0).optString("text", "No response generated.")
        }
      }
      return "No response received from model."
    }
  }

  private fun generateLocalSummary(video: Video): String {
    val keyPointsText = if (video.keyPoints.isNotEmpty()) {
      video.keyPoints.joinToString("\n") { "• $it" }
    } else {
      "• Core focus: In-depth exploration of ${video.title}."
    }

    return """
      💡 Smart Summary: ${video.title}
      
      $keyPointsText
      
      ⏱️ Chapters & Pacing:
      ${video.chapters.joinToString("\n") { "• ${it.formattedTime} - ${it.title}" }}
      
      📌 Recommendation:
      Great for enthusiasts of ${video.category}. Focuses on high-fidelity visuals and clear thematic progression.
    """.trimIndent()
  }

  private fun generateLocalAnswer(video: Video, question: String): String {
    val lower = question.lowercase()
    return when {
      "chapter" in lower || "timestamp" in lower || "when" in lower -> {
        val chapterList = video.chapters.joinToString(", ") { "${it.title} at ${it.formattedTime}" }
        "This video has ${video.chapters.size} structured chapters: $chapterList."
      }
      "who" in lower || "channel" in lower || "creator" in lower -> {
        "This video is produced by ${video.channelTitle}, featuring content in the ${video.category} category."
      }
      "summary" in lower || "about" in lower -> {
        "${video.title} explores: ${video.description}"
      }
      else -> {
        "Regarding '$question' in \"${video.title}\": This segment delves into ${video.category} themes with focus on cinematic production and technical insights by ${video.channelTitle}."
      }
    }
  }
}
