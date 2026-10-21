package com.ovehbe.junkboy.llm

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Minimal blocking POST to OpenRouter. Call from a background thread. */
internal object OpenRouterHttp {

    private const val BASE_URL = "https://openrouter.ai"
    private const val CONNECT_TIMEOUT_MS = 5_000
    private const val READ_TIMEOUT_MS = 8_000

    /** Returns the response body, or throws an [IOException] such as "HTTP 401 No auth credentials found". */
    fun post(path: String, apiKey: String, body: JsonObject): String {
        val connection = URL(BASE_URL + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("X-Title", "Junkboy")
            connection.outputStream.use { it.write(body.toString().toByteArray()) }

            val code = connection.responseCode
            if (code !in 200..299) {
                val detail = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code ${errorMessage(detail)}".trim())
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(json: String): String =
        try {
            JsonParser.parseString(json).asJsonObject.getAsJsonObject("error")?.get("message")?.asString.orEmpty()
        } catch (e: Exception) {
            ""
        }
}
