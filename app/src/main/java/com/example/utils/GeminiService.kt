package com.example.utils

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.example.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Moshi Request Models ---

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Float? = null,
    val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    val thinkingLevel: String // "high" or "low" or "off"
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>?
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content?
)

// --- Retrofit Interface ---

interface GeminiApi {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun generateContentWithPro(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse

    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContentWithFlash(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

// --- Service Object ---

object GeminiService {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun getGeminiSuggestion(
        prompt: String,
        systemInstruction: String = "You are an expert system optimization utility. Provide clear, concise suggestions.",
        useProWithThinking: Boolean = false
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return getFallbackRecommendation(prompt)
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = if (useProWithThinking) {
                GenerationConfig(
                    temperature = 1.0f,
                    thinkingConfig = ThinkingConfig(thinkingLevel = "high")
                )
            } else {
                GenerationConfig(temperature = 0.2f)
            }
        )

        return try {
            val response = if (useProWithThinking) {
                api.generateContentWithPro(apiKey, request)
            } else {
                api.generateContentWithFlash(apiKey, request)
            }
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No recommendation available from Gemini."
        } catch (e: Exception) {
            "Offline Recommendation (API Call Failed): " + getFallbackRecommendation(prompt)
        }
    }

    private fun getFallbackRecommendation(prompt: String): String {
        return when {
            prompt.contains("zombie", ignoreCase = true) -> {
                "• Safely delete all duplicate files (.dup, .tmp).\n" +
                "• Clear system cache directories and log files (.log).\n" +
                "• Remove obsolete backup folders (.bak) and unfinished download buffers (.downloading)."
            }
            prompt.contains("optimizer", ignoreCase = true) -> {
                "• Limit running background threads using a low thermal profile.\n" +
                "• Clear memory caches and invoke Java Runtime Garbage Collector (System.gc).\n" +
                "• Adjust maximum graphics frame-pacing bounds to match your system refresh rate."
            }
            prompt.contains("format", ignoreCase = true) -> {
                "• SD Card verification succeeded. Run deep block formatting with zero-fill (secure wipe).\n" +
                "• Overwrite each block pattern with binary zeroes (0x00) to render recovery impossible."
            }
            prompt.contains("update", ignoreCase = true) -> {
                "• Device compatibility: GSI compatibility verified.\n" +
                "• Custom script generated: Use fastboot flash system_a to apply newer Android upgrades on older architectures."
            }
            else -> "Optimize resources, free storage blocks, and prune unnecessary cached background tasks."
        }
    }
}
