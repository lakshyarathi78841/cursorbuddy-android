package com.cursorbuddy.android.service

import android.graphics.RectF
import com.cursorbuddy.android.model.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ClaudeAIPlanner(private var apiKey: String) {

    companion object {
        private const val API_URL = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-sonnet-4-20250514"
    }

    fun updateApiKey(key: String) {
        apiKey = key
    }

    suspend fun planFromScreenshot(
        question: String,
        screenshotBase64: String?,
        uiTreeJson: String,
        packageName: String?,
        appName: String?
    ): List<TutorialStep> = withContext(Dispatchers.IO) {
        try {
            val response = callClaude(question, screenshotBase64, uiTreeJson, packageName, appName)
            parseSteps(response)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun callClaude(
        question: String,
        screenshotBase64: String?,
        uiTreeJson: String,
        packageName: String?,
        appName: String?
    ): String {
        val systemPrompt = "You are CursorBuddy, an AI assistant that helps users navigate Android apps.\n" +
            "You analyze the current screen (via UI accessibility tree and optionally a screenshot) and produce step-by-step instructions.\n\n" +
            "RULES:\n" +
            "1. Each step MUST have: action, target_text (the text/label of the UI element to interact with), bounds (screen coordinates [left,top,right,bottom]), and caption (friendly instruction, <15 words).\n" +
            "2. Actions: \"tap\", \"long_press\", \"type\", \"scroll_down\", \"scroll_up\", \"swipe_left\", \"swipe_right\", \"wait\"\n" +
            "3. For \"type\" actions, include \"input_text\" with what to type.\n" +
            "4. bounds come from the UI tree. Use the exact bounds from the matching element.\n" +
            "5. Keep captions friendly, concise, encouraging.\n" +
            "6. If you cannot determine the steps confidently, return fewer steps with what you DO know.\n" +
            "7. Return ONLY valid JSON array, no markdown, no explanation.\n\n" +
            "RESPONSE FORMAT (JSON array):\n" +
            "[{\"action\": \"tap\", \"target_text\": \"Settings\", \"bounds\": [0, 100, 540, 200], \"caption\": \"First, tap Settings\"}]"

        val content = JSONArray()
        
        // Add screenshot if available
        if (screenshotBase64 != null) {
            val imageContent = JSONObject().apply {
                put("type", "image")
                put("source", JSONObject().apply {
                    put("type", "base64")
                    put("media_type", "image/jpeg")
                    put("data", screenshotBase64)
                })
            }
            content.put(imageContent)
        }
        
        // Add text context
        val appContext = if (appName != null) "App: $appName ($packageName)" else "App: $packageName"
        val textContent = JSONObject().apply {
            put("type", "text")
            put("text", "$appContext\n\nUSER QUESTION: $question\n\nUI ACCESSIBILITY TREE (JSON):\n$uiTreeJson\n\nBased on the screen content above, provide step-by-step instructions to answer the user question. Return ONLY a JSON array of steps.")
        }
        content.put(textContent)

        val message = JSONObject().apply {
            put("role", "user")
            put("content", content)
        }

        val body = JSONObject().apply {
            put("model", MODEL)
            put("max_tokens", 1024)
            put("system", systemPrompt)
            put("messages", JSONArray().put(message))
        }

        val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-api-key", apiKey)
            setRequestProperty("anthropic-version", "2023-06-01")
            doOutput = true
            connectTimeout = 15000
            readTimeout = 30000
        }

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(body.toString())
            writer.flush()
        }

        val responseCode = connection.responseCode
        val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
        val response = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        connection.disconnect()

        if (responseCode !in 200..299) {
            throw Exception("Claude API error $responseCode: $response")
        }

        // Extract text from response
        val responseJson = JSONObject(response)
        val contentArray = responseJson.getJSONArray("content")
        for (i in 0 until contentArray.length()) {
            val block = contentArray.getJSONObject(i)
            if (block.getString("type") == "text") {
                return block.getString("text")
            }
        }
        
        throw Exception("No text in Claude response")
    }

    private fun parseSteps(jsonText: String): List<TutorialStep> {
        // Extract JSON array from response (might have markdown wrapping)
        val cleaned = jsonText
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
        
        val array = JSONArray(cleaned)
        val steps = mutableListOf<TutorialStep>()
        
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            
            val action = when (obj.optString("action", "tap")) {
                "tap" -> StepAction.TAP
                "long_press" -> StepAction.LONG_PRESS
                "type" -> StepAction.TYPE
                "scroll_down", "scroll_up" -> StepAction.SCROLL
                "swipe_left", "swipe_right" -> StepAction.SWIPE
                "wait" -> StepAction.WAIT
                else -> StepAction.TAP
            }
            
            val bounds = if (obj.has("bounds")) {
                val b = obj.getJSONArray("bounds")
                RectF(
                    b.getDouble(0).toFloat(),
                    b.getDouble(1).toFloat(),
                    b.getDouble(2).toFloat(),
                    b.getDouble(3).toFloat()
                )
            } else null
            
            steps.add(TutorialStep(
                stepNumber = i + 1,
                totalSteps = array.length(),
                action = action,
                targetBounds = bounds,
                targetDescription = obj.optString("target_text", ""),
                caption = obj.optString("caption", "Tap here"),
                confidence = obj.optDouble("confidence", 0.8).toFloat()
            ))
        }
        
        return steps
    }
}
