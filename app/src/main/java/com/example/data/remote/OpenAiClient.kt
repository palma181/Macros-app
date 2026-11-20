package com.example.data.remote

import com.example.data.model.AiConfig
import com.example.data.model.MacroEstimation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class OpenAiClient {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun estimateFromText(config: AiConfig, description: String): Result<MacroEstimation> {
        val prompt = buildString {
            append("Estima los macros de esta comida (usa una porción típica si no hay cantidades). ")
            append("Responde ÚNICAMENTE con un JSON válido con números enteros: ")
            append("""{"kcal":0,"p":0,"c":0,"f":0} """)
            append("(p=proteína en gramos, c=carbohidratos en gramos, f=grasa en gramos). ")
            append("Comida: ").append(description.trim())
        }
        return sendChatCompletion(
            config = config,
            content = prompt,
            maxTokens = 250
        ).mapCatching { rawText ->
            parseMacroJson(rawText, defaultName = null)
        }
    }

    suspend fun estimateFromImage(
        config: AiConfig,
        dataUrl: String,
        hint: String? = null
    ): Result<MacroEstimation> {
        val instruction = buildString {
            append("Mira la foto de esta comida. Identifica los alimentos y estima los macros de la porción visible. ")
            if (!hint.isNullOrBlank()) {
                append("Contexto adicional del usuario: ").append(hint.trim()).append(". ")
            }
            append("Responde ÚNICAMENTE con un JSON válido: ")
            append("""{"name":"descripción breve en español","kcal":0,"p":0,"c":0,"f":0} """)
            append("(p=proteína g, c=carbohidratos g, f=grasa g).")
        }
        val contentArray = JSONArray().apply {
            put(JSONObject().apply {
                put("type", "text")
                put("text", instruction)
            })
            put(JSONObject().apply {
                put("type", "image_url")
                put("image_url", JSONObject().apply {
                    put("url", dataUrl)
                })
            })
        }
        return sendChatCompletion(
            config = config,
            content = contentArray,
            maxTokens = 350
        ).mapCatching { rawText ->
            parseMacroJson(rawText, defaultName = "Comida analizada")
        }
    }

    suspend fun askCoach(config: AiConfig, prompt: String): Result<String> {
        return sendChatCompletion(
            config = config,
            content = prompt,
            maxTokens = 700
        )
    }

    suspend fun testConnection(config: AiConfig): Result<String> {
        return sendChatCompletion(
            config = config,
            content = "Responde únicamente con: OK",
            maxTokens = 20
        ).map { reply ->
            "Conexión exitosa con ${config.model.ifBlank { "el modelo" }} (${reply.trim().take(30)})"
        }
    }

    private suspend fun sendChatCompletion(
        config: AiConfig,
        content: Any,
        maxTokens: Int
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = config.baseUrl.trim().trimEnd('/')
            if (baseUrl.isBlank()) {
                throw IllegalStateException("Configura la URL base de tu IA en la pestaña IA.")
            }
            val model = config.model.trim()
            if (model.isBlank()) {
                throw IllegalStateException("Configura el nombre del modelo en la pestaña IA.")
            }
            val isLocalHost = baseUrl.contains("localhost", ignoreCase = true) ||
                baseUrl.contains("127.0.0.1") ||
                baseUrl.contains("10.0.2.2") ||
                baseUrl.contains("192.168.")
            if (config.apiKey.isBlank() && !isLocalHost) {
                throw IllegalStateException("Falta tu clave de API. Configúrala en la pestaña IA.")
            }

            val endpoint = if (baseUrl.endsWith("/chat/completions")) {
                baseUrl
            } else {
                "$baseUrl/chat/completions"
            }

            val bodyJson = JSONObject().apply {
                put("model", model)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", content)
                    })
                })
                put("max_tokens", maxTokens)
            }

            val requestBuilder = Request.Builder()
                .url(endpoint)
                .post(bodyJson.toString().toRequestBody(jsonMediaType))
                .addHeader("Content-Type", "application/json")

            if (config.apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer ${config.apiKey.trim()}")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = extractApiErrorMessage(rawBody) ?: rawBody.take(160)
                    val friendlyPrefix = when (response.code) {
                        401 -> "Clave de API inválida o sin autorización (401)"
                        403 -> "Acceso denegado (403)"
                        404 -> "Endpoint o modelo no encontrado (404)"
                        429 -> "Límite de peticiones alcanzado o sin saldo (429)"
                        else -> "Error HTTP ${response.code}"
                    }
                    throw IOException("$friendlyPrefix: $detail")
                }

                val json = JSONObject(rawBody)
                val choices = json.optJSONArray("choices")
                    ?: throw IOException("Respuesta inesperada del proveedor (sin choices).")
                if (choices.length() == 0) {
                    throw IOException("El proveedor devolvió una respuesta vacía.")
                }
                val message = choices.getJSONObject(0).getJSONObject("message")
                extractMessageText(message)
                    ?: throw IOException("No se pudo leer el contenido de la respuesta de la IA.")
            }
        }
    }

    private fun extractMessageText(message: JSONObject): String? {
        val rawContent = message.opt("content") ?: return null
        return when (rawContent) {
            is String -> rawContent
            is JSONArray -> {
                buildString {
                    for (i in 0 until rawContent.length()) {
                        val part = rawContent.optJSONObject(i)
                        val text = part?.optString("text")
                        if (!text.isNullOrBlank()) {
                            append(text)
                        }
                    }
                }.ifBlank { null }
            }
            else -> rawContent.toString()
        }
    }

    private fun extractApiErrorMessage(rawBody: String): String? {
        return try {
            val root = JSONObject(rawBody)
            root.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: root.optString("message").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseMacroJson(rawText: String, defaultName: String?): MacroEstimation {
        val match = Regex("""\{[\s\S]*\}""").find(rawText)
            ?: throw IllegalStateException("La IA no devolvió un JSON válido. Respuesta: ${rawText.take(120)}")
        val obj = JSONObject(match.value)
        val name = obj.optString("name", "").trim().ifBlank { defaultName }
        return MacroEstimation(
            name = name,
            kcal = obj.optDouble("kcal", 0.0).roundToInt().coerceAtLeast(0),
            protein = obj.optDouble("p", obj.optDouble("protein", 0.0)).roundToInt().coerceAtLeast(0),
            carbs = obj.optDouble("c", obj.optDouble("carbs", 0.0)).roundToInt().coerceAtLeast(0),
            fat = obj.optDouble("f", obj.optDouble("fat", 0.0)).roundToInt().coerceAtLeast(0)
        )
    }
}
