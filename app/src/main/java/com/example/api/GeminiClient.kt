package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini API Models ---

data class GeminiRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>
)

data class GeminiPart(
    @Json(name = "text") val text: String
)

data class GeminiGenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "maxOutputTokens") val maxOutputTokens: Int? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null
)

data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent?
)

// Retrofit Interface
interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// Client Object
object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    private const val TAG = "GeminiClient"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiService: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    /**
     * Sends the transcript and alert metadata to Gemini for dynamic danger assessment and summary notes.
     */
    suspend fun analyzeAlertIncident(
        transcript: String,
        trigger: String,
        location: String,
        timestampStr: String
    ): AlertAssessment = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is not configured or uses placeholder.")
            return@withContext getFallbackAssessment(transcript, trigger)
        }

        val prompt = """
            You are an AI Threat Assessment Engine for a Personal Safety App named PanicLink.
            Assess the following emergency incident details and provide an assessment.
            
            Incident Context:
            - Trigger Method: $trigger
            - Location: $location
            - Timestamp: $timestampStr
            - 10-Second Audio Transcript: "$transcript"
            
            Your response must be in plain text parsed with these exact tags:
            [BRIEF] ... Write a 1-sentence action summary of what is happening ...
            [THREAT_LEVEL] ... Write one of: LOW, MEDIUM, HIGH, CRITICAL ...
            [SUGGESTED_ACTION] ... Write a 1-sentence priority action instruction for responders ...
            
            Ensure you include each tag exactly once on a new line.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = listOf(GeminiPart(text = prompt)))),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = "You are a crisis responder and emergency analyst safety bot. Be factual, immediate and strictly follow XML-style tags format in response."))),
            generationConfig = GeminiGenerationConfig(temperature = 0.2f)
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            Log.d(TAG, "Response from Gemini: $responseText")
            parseAssessment(responseText, transcript, trigger)
        } catch (e: Exception) {
            Log.e(TAG, "Failed calling Gemini API", e)
            getFallbackAssessment(transcript, trigger)
        }
    }

    private fun parseAssessment(text: String, originalTranscript: String, trigger: String): AlertAssessment {
        var brief = ""
        var level = "HIGH"
        var suggested = ""

        val briefRegex = "\\[BRIEF\\](.*?)(?=\\[|$)".toRegex(RegexOption.DOT_MATCHES_ALL)
        val threatRegex = "\\[THREAT_LEVEL\\](.*?)(?=\\[|$)".toRegex(RegexOption.DOT_MATCHES_ALL)
        val suggestedRegex = "\\[SUGGESTED_ACTION\\](.*?)(?=\\[|$)".toRegex(RegexOption.DOT_MATCHES_ALL)

        briefRegex.find(text)?.groupValues?.get(1)?.trim()?.let { brief = it }
        threatRegex.find(text)?.groupValues?.get(1)?.trim()?.let { level = it.uppercase() }
        suggestedRegex.find(text)?.groupValues?.get(1)?.trim()?.let { suggested = it }

        if (brief.isEmpty()) {
            brief = "Emergency alert triggered via $trigger with audio feedback content."
        }
        if (suggested.isEmpty()) {
            suggested = "Immediately contact user and dispatch physical check to GPS location."
        }
        if (level != "LOW" && level != "MEDIUM" && level != "HIGH" && level != "CRITICAL") {
            level = "HIGH"
        }

        return AlertAssessment(brief, level, suggested)
    }

    private fun getFallbackAssessment(transcript: String, trigger: String): AlertAssessment {
        val lower = transcript.lowercase()
        val (level, brief) = when {
            lower.contains("fire") || lower.contains("burning") -> "CRITICAL" to "Possible fire emergency detected."
            lower.contains("help") || lower.contains("please stop") || lower.contains("no!") || lower.contains("danger") -> "CRITICAL" to "Urgent distress call captured via audio recording."
            lower.contains("medical") || lower.contains("heart") || lower.contains("doctor") || lower.contains("pain") -> "HIGH" to "Potential sudden health or medical emergency."
            trigger == "POWER_BUTTON_TAPS" -> "HIGH" to "Discreet panic alert activated via rapid power button tap pattern."
            else -> "MEDIUM" to "Manual personal safety trigger report established by user."
        }
        val suggested = when (level) {
            "CRITICAL" -> "Dispatch immediate first responders. Alert all designated emergency contacts."
            "HIGH" -> "Coordinate medical/security dispatch team. Attempt urgent call verification."
            else -> "Initiate silent monitoring and notify emergency contacts with real-time tracking link."
        }
        return AlertAssessment(brief, level, suggested)
    }
}

data class AlertAssessment(
    val brief: String,
    val threatLevel: String,
    val suggestedAction: String
)
