package com.example.data.remote

import com.example.data.model.AiConfig
import com.example.data.model.MacroEstimation
import com.example.data.model.RecipeDifficulty
import com.example.data.model.RecipeOption
import com.example.data.model.RecipeSection
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

    private sealed interface AttemptOutcome {
        data class Success(val text: String) : AttemptOutcome
        data class ModelNotFound(val code: Int, val detail: String) : AttemptOutcome
        data class OtherError(val error: Throwable) : AttemptOutcome
    }

    suspend fun estimateFromText(
        config: AiConfig,
        description: String,
        onModelResolved: suspend (String) -> Unit = {}
    ): Result<MacroEstimation> {
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
            maxTokens = 250,
            onModelResolved = onModelResolved
        ).mapCatching { rawText ->
            parseMacroJson(rawText, defaultName = null)
        }
    }

    suspend fun estimateFromImage(
        config: AiConfig,
        dataUrl: String,
        hint: String? = null,
        onModelResolved: suspend (String) -> Unit = {}
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
            maxTokens = 350,
            onModelResolved = onModelResolved
        ).mapCatching { rawText ->
            parseMacroJson(rawText, defaultName = "Comida analizada")
        }
    }

    suspend fun askCoach(
        config: AiConfig,
        prompt: String,
        onModelResolved: suspend (String) -> Unit = {}
    ): Result<String> {
        return sendChatCompletion(
            config = config,
            content = prompt,
            maxTokens = 700,
            onModelResolved = onModelResolved
        )
    }

    suspend fun testConnection(
        config: AiConfig,
        onModelResolved: suspend (String) -> Unit = {}
    ): Result<String> {
        var resolvedModel = config.model.trim()
        return sendChatCompletion(
            config = config,
            content = "Responde únicamente con: OK",
            maxTokens = 20,
            onModelResolved = { newModel ->
                resolvedModel = newModel
                onModelResolved(newModel)
            }
        ).map { reply ->
            val autoNote = if (!resolvedModel.equals(config.model.trim(), ignoreCase = true)) {
                " · ajustado automáticamente"
            } else {
                ""
            }
            "Conexión exitosa con ${resolvedModel.ifBlank { "el modelo" }}$autoNote (${reply.trim().take(30)})"
        }
    }

    suspend fun generateRecipesFromIngredients(
        config: AiConfig,
        ingredients: List<String>,
        difficulty: RecipeDifficulty?,
        section: RecipeSection = RecipeSection.MEALS,
        onModelResolved: suspend (String) -> Unit = {}
    ): Result<List<RecipeOption>> {
        val levelInstruction = when (difficulty) {
            null -> "Incluye exactamente 3 opciones: una de nivel Básico, una Intermedio y una Avanzado."
            else -> "Incluye 3 opciones variadas de nivel ${difficulty.label}."
        }
        val kindInstruction = when (section) {
            RecipeSection.MEALS ->
                "Eres chef y nutricionista. Crea platillos completos de cocina con estos ingredientes disponibles: "
            RecipeSection.SNACKS_DRINKS ->
                "Eres nutricionista deportivo y mixólogo saludable. Crea snacks, bowls de fruta, batidos o bebidas funcionales (aprovechando los suplementos si se incluyen) con estos ingredientes disponibles: "
        }
        val prompt = buildString {
            append(kindInstruction)
            append(ingredients.joinToString(", "))
            append(". ")
            append(levelInstruction)
            append(" Responde ÚNICAMENTE con un JSON válido con esta estructura exacta: ")
            append(
                """{"recipes":[{"title":"...","difficulty":"basico|intermedio|avanzado","prepMinutes":10,"ingredients":["..."],"steps":["Paso 1...","Paso 2..."],"tips":"...","kcal":320,"p":28,"c":35,"f":8}]}"""
            )
        }
        return sendChatCompletion(
            config = config,
            content = prompt,
            maxTokens = 1400,
            onModelResolved = onModelResolved
        ).mapCatching { rawText ->
            val match = Regex("""\{[\s\S]*\}""").find(rawText)
                ?: throw IllegalStateException("La IA no devolvió un JSON válido de recetas.")
            val root = JSONObject(match.value)
            val arr = root.optJSONArray("recipes")
                ?: throw IllegalStateException("No se encontró el campo recipes en la respuesta.")
            val list = mutableListOf<RecipeOption>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ingArr = obj.optJSONArray("ingredients") ?: JSONArray()
                val ingList = (0 until ingArr.length()).map { idx -> ingArr.optString(idx) }.filter { it.isNotBlank() }
                val stepsArr = obj.optJSONArray("steps") ?: JSONArray()
                val stepList = (0 until stepsArr.length()).map { idx -> stepsArr.optString(idx) }.filter { it.isNotBlank() }
                val diff = RecipeDifficulty.fromId(obj.optString("difficulty", "basico"))
                val title = obj.optString("title", "Receta sugerida").trim()
                list.add(
                    RecipeOption(
                        id = "ai_${System.currentTimeMillis()}_$i",
                        title = title,
                        difficulty = diff,
                        prepMinutes = obj.optInt("prepMinutes", 15).coerceIn(2, 180),
                        matchTags = ingredients,
                        ingredients = ingList,
                        steps = stepList,
                        tips = obj.optString("tips", "Ajusta al gusto."),
                        kcal = obj.optDouble("kcal", 350.0).roundToInt().coerceAtLeast(0),
                        protein = obj.optDouble("p", 25.0).roundToInt().coerceAtLeast(0),
                        carbs = obj.optDouble("c", 35.0).roundToInt().coerceAtLeast(0),
                        fat = obj.optDouble("f", 10.0).roundToInt().coerceAtLeast(0),
                        section = section,
                        isAiGenerated = true
                    )
                )
            }
            if (list.isEmpty()) throw IllegalStateException("La IA no generó opciones de recetas.")
            list
        }
    }

    suspend fun fetchAvailableModels(config: AiConfig): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val baseUrl = config.baseUrl.trim().trimEnd('/')
            if (baseUrl.isBlank()) {
                throw IllegalStateException("Configura la URL base de tu IA primero.")
            }
            val apiKey = config.apiKey.trim()
            if (apiKey.isBlank() && !isKeyOptionalHost(baseUrl)) {
                throw IllegalStateException("Ingresa tu clave de API para consultar modelos disponibles.")
            }
            val discovered = fetchModelsBlocking(baseUrl, apiKey)
            if (discovered.isEmpty()) {
                val fallback = fallbackModelsFor(baseUrl)
                if (fallback.isNotEmpty()) fallback
                else throw IOException("El proveedor no devolvió una lista de modelos.")
            } else {
                discovered
            }
        }
    }

    private suspend fun sendChatCompletion(
        config: AiConfig,
        content: Any,
        maxTokens: Int,
        onModelResolved: suspend (String) -> Unit
    ): Result<String> {
        val baseUrl = config.baseUrl.trim().trimEnd('/')
        if (baseUrl.isBlank()) {
            return Result.failure(IllegalStateException("Configura la URL base de tu IA en la pestaña IA."))
        }
        val requestedModel = config.model.trim()
        if (requestedModel.isBlank()) {
            return Result.failure(IllegalStateException("Configura el nombre del modelo en la pestaña IA."))
        }
        val apiKey = config.apiKey.trim()
        if (apiKey.isBlank() && !isKeyOptionalHost(baseUrl)) {
            return Result.failure(IllegalStateException("Falta tu clave de API. Configúrala en la pestaña IA o usa Pollinations (sin key)."))
        }

        val endpoint = if (baseUrl.endsWith("/chat/completions")) {
            baseUrl
        } else {
            "$baseUrl/chat/completions"
        }

        val initialOutcome = withContext(Dispatchers.IO) {
            executeCompletionOnce(endpoint, apiKey, requestedModel, content, maxTokens)
        }

        return when (initialOutcome) {
            is AttemptOutcome.Success -> Result.success(initialOutcome.text)
            is AttemptOutcome.OtherError -> Result.failure(initialOutcome.error)
            is AttemptOutcome.ModelNotFound -> {
                val liveModels = withContext(Dispatchers.IO) {
                    runCatching { fetchModelsBlocking(baseUrl, apiKey) }.getOrDefault(emptyList())
                }
                val candidates = (liveModels + fallbackModelsFor(baseUrl))
                    .distinct()
                    .filter { !it.equals(requestedModel, ignoreCase = true) }
                    .take(5)

                var lastFailureDetail = initialOutcome.detail
                for (candidate in candidates) {
                    val retryOutcome = withContext(Dispatchers.IO) {
                        executeCompletionOnce(endpoint, apiKey, candidate, content, maxTokens)
                    }
                    when (retryOutcome) {
                        is AttemptOutcome.Success -> {
                            onModelResolved(candidate)
                            return Result.success(retryOutcome.text)
                        }
                        is AttemptOutcome.ModelNotFound -> {
                            lastFailureDetail = retryOutcome.detail
                        }
                        is AttemptOutcome.OtherError -> {
                            return Result.failure(retryOutcome.error)
                        }
                    }
                }

                val availableHint = if (liveModels.isNotEmpty()) {
                    " Modelos disponibles en tu cuenta: ${liveModels.take(4).joinToString(", ")}"
                } else {
                    ""
                }
                Result.failure(
                    IOException(
                        "Modelo '$requestedModel' no encontrado (${initialOutcome.code}): $lastFailureDetail.$availableHint"
                    )
                )
            }
        }
    }

    private fun executeCompletionOnce(
        endpoint: String,
        apiKey: String,
        model: String,
        content: Any,
        maxTokens: Int
    ): AttemptOutcome {
        return try {
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

            if (apiKey.isNotBlank()) {
                requestBuilder.addHeader("Authorization", "Bearer $apiKey")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = extractApiErrorMessage(rawBody) ?: rawBody.take(160)
                    if (isModelNotFoundError(response.code, detail)) {
                        return AttemptOutcome.ModelNotFound(response.code, detail)
                    }
                    val friendlyPrefix = when (response.code) {
                        401 -> "Clave de API inválida o sin autorización (401)"
                        403 -> "Acceso denegado o permisos faltantes (403)"
                        404 -> "Endpoint o modelo no encontrado (404)"
                        429 -> "Límite de peticiones alcanzado o sin saldo (429)"
                        else -> "Error HTTP ${response.code}"
                    }
                    return AttemptOutcome.OtherError(IOException("$friendlyPrefix: $detail"))
                }

                parseCompletionResponse(rawBody)
                    ?.let { AttemptOutcome.Success(it) }
                    ?: AttemptOutcome.OtherError(
                        IOException(buildNonJsonErrorMessage(rawBody, endpoint))
                    )
            }
        } catch (t: Throwable) {
            AttemptOutcome.OtherError(t)
        }
    }

    private fun parseCompletionResponse(rawBody: String): String? {
        val trimmed = rawBody.trim()
        if (trimmed.isBlank()) return null

        val jsonObj = runCatching { JSONObject(trimmed) }.getOrNull()
        if (jsonObj != null) {
            val choices = jsonObj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val msg = choices.optJSONObject(0)?.optJSONObject("message")
                if (msg != null) {
                    val extracted = extractMessageText(msg)
                    if (!extracted.isNullOrBlank()) return extracted
                }
                val text = choices.optJSONObject(0)?.optString("text")?.takeIf { it.isNotBlank() }
                if (text != null) return text
            }
            val direct = jsonObj.optString("response").takeIf { it.isNotBlank() }
                ?: jsonObj.optString("content").takeIf { it.isNotBlank() }
            if (direct != null) return direct
        }

        // Handle SSE streaming payloads (data: {"choices":[{"delta":{"content":"..."}}]})
        if (trimmed.startsWith("data:", ignoreCase = true)) {
            val sb = StringBuilder()
            trimmed.lineSequence().forEach { line ->
                val data = line.trim()
                if (data.startsWith("data:", ignoreCase = true)) {
                    val payload = data.removePrefix("data:").trim()
                    if (payload.isNotEmpty() && payload != "[DONE]") {
                        val chunk = runCatching { JSONObject(payload) }.getOrNull()
                        val choice0 = chunk?.optJSONArray("choices")?.optJSONObject(0)
                        val deltaText = choice0?.optJSONObject("delta")?.optString("content")
                            ?: choice0?.optJSONObject("message")?.optString("content")
                        if (!deltaText.isNullOrEmpty()) sb.append(deltaText)
                    }
                }
            }
            if (sb.isNotBlank()) return sb.toString()
        }

        return null
    }

    private fun buildNonJsonErrorMessage(rawBody: String, endpoint: String): String {
        val trimmed = rawBody.trim()
        val lowerEndpoint = endpoint.lowercase()
        if (lowerEndpoint.contains("models.github.ai") || lowerEndpoint.contains("models.inference.ai.azure.com")) {
            return "GitHub Models fue retirado y devolvió '$trimmed' en vez de una respuesta de chat. Elige Pollinations (Gratis / Sin Key), Gemini, Groq, NVIDIA u OpenRouter."
        }
        val jsonError = runCatching { extractApiErrorMessage(trimmed) }.getOrNull()
        if (!jsonError.isNullOrBlank()) {
            return "El proveedor devolvió un error sin respuesta de chat: $jsonError"
        }
        val preview = trimmed.take(60).replace("\n", " ")
        return "El servidor respondió '$preview' en lugar de una respuesta JSON compatible con OpenAI. Verifica la URL o cambia a otro proveedor gratuito (ej. Pollinations, Gemini o Groq)."
    }

    private fun fetchModelsBlocking(baseUrl: String, apiKey: String): List<String> {
        val cleanBase = baseUrl.removeSuffix("/chat/completions").trimEnd('/')
        val modelsUrl = "$cleanBase/models"
        val requestBuilder = Request.Builder()
            .url(modelsUrl)
            .get()
        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        return runCatching {
            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) return@use emptyList()
                parseModelsList(response.body?.string().orEmpty(), cleanBase)
            }
        }.getOrDefault(emptyList())
    }

    private fun parseModelsList(rawBody: String, baseUrl: String): List<String> {
        val trimmed = rawBody.trim()
        if (trimmed.isBlank()) return emptyList()

        val itemsArray: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            else -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("data")
                    ?: obj.optJSONArray("models")
                    ?: return emptyList()
            }
        }

        val isGemini = baseUrl.contains("generativelanguage.googleapis.com", ignoreCase = true)
        val isOpenRouter = baseUrl.contains("openrouter.ai", ignoreCase = true)

        val collected = mutableListOf<Pair<String, Boolean>>()
        for (i in 0 until itemsArray.length()) {
            val elem = itemsArray.opt(i)
            var id = ""
            var isFree = false
            when (elem) {
                is String -> id = elem
                is JSONObject -> {
                    id = elem.optString("id").ifBlank { elem.optString("name") }.trim()
                    if (isOpenRouter) {
                        val pricing = elem.optJSONObject("pricing")
                        val promptPrice = pricing?.optString("prompt")?.toDoubleOrNull()
                        val compPrice = pricing?.optString("completion")?.toDoubleOrNull()
                        isFree = id == "openrouter/free" ||
                            id.endsWith(":free", ignoreCase = true) ||
                            (promptPrice == 0.0 && compPrice == 0.0)
                    }
                }
            }
            if (isGemini) {
                id = id.removePrefix("models/")
            }
            if (id.isBlank() || isNonChatModel(id)) continue
            collected.add(id to isFree)
        }

        val filtered = if (isOpenRouter) {
            val freeOnly = collected.filter { it.second }.map { it.first }
            if (freeOnly.isNotEmpty()) listOf("openrouter/free") + freeOnly else listOf("openrouter/free") + collected.map { it.first }
        } else {
            collected.map { it.first }
        }

        return filtered.distinct().sortedBy { modelPriorityScore(it) }
    }

    private fun isNonChatModel(id: String): Boolean {
        val lower = id.lowercase()
        val blocked = listOf(
            "whisper", "tts", "orpheus", "embed", "embedding", "moderation",
            "guard", "imagen", "veo", "dall-e", "flux", "rerank", "bge-",
            "realtime", "transcribe", "aqa", "learnlm"
        )
        return blocked.any { lower.contains(it) }
    }

    private fun modelPriorityScore(id: String): Int {
        val lower = id.lowercase()
        return when {
            lower == "openrouter/free" -> 0
            lower.endsWith(":free") -> 1
            lower == "openai" -> 2
            lower.contains("flash-lite") -> 3
            lower.contains("flash") -> 4
            lower.contains("gpt-4o-mini") -> 5
            lower.contains("llama-3.3") -> 6
            lower.contains("mistral-small") -> 7
            lower.contains("gpt-oss") -> 8
            lower.contains("gemma") -> 9
            lower.contains("qwen") -> 10
            lower.contains("llama-3.1") -> 11
            else -> 20
        }
    }

    private fun isModelNotFoundError(code: Int, detail: String): Boolean {
        if (code == 404) return true
        if (code != 400 && code != 422) return false
        val lower = detail.lowercase()
        return lower.contains("model") && (
            lower.contains("not found") ||
                lower.contains("not_found") ||
                lower.contains("does not exist") ||
                lower.contains("decommissioned") ||
                lower.contains("deprecated") ||
                lower.contains("unknown") ||
                lower.contains("not supported") ||
                lower.contains("unavailable") ||
                lower.contains("invalid")
            )
    }

    private fun fallbackModelsFor(baseUrl: String): List<String> {
        val lower = baseUrl.lowercase()
        return when {
            lower.contains("generativelanguage.googleapis.com") ->
                listOf("gemini-3.5-flash-lite", "gemini-3.6-flash", "gemini-2.5-flash", "gemini-2.5-flash-lite")
            lower.contains("api.groq.com") ->
                listOf("openai/gpt-oss-120b", "openai/gpt-oss-20b")
            lower.contains("openrouter.ai") ->
                listOf("openrouter/free")
            lower.contains("integrate.api.nvidia.com") ->
                listOf("meta/llama-3.3-70b-instruct", "meta/llama-3.1-8b-instruct", "qwen/qwen2.5-72b-instruct")
            lower.contains("pollinations.ai") ->
                listOf("openai", "gemini", "mistral")
            lower.contains("api.mistral.ai") ->
                listOf("mistral-small-latest", "pixtral-12b-2409", "open-mistral-nemo")
            lower.contains("api.cerebras.ai") ->
                listOf("gpt-oss-120b", "qwen-3.8-27b", "llama3.1-8b")
            lower.contains("api.sambanova.ai") ->
                listOf("gemma-4-31B-it", "gpt-oss-120b", "Meta-Llama-3.3-70B-Instruct")
            lower.contains("router.huggingface.co") ->
                listOf("meta-llama/Llama-3.1-8B-Instruct:fastest", "Qwen/Qwen2.5-7B-Instruct:fastest", "deepseek-ai/DeepSeek-R1:fastest")
            else -> emptyList()
        }
    }

    private fun isKeyOptionalHost(baseUrl: String): Boolean {
        val lower = baseUrl.lowercase()
        return lower.contains("localhost") ||
            lower.contains("127.0.0.1") ||
            lower.contains("10.0.2.2") ||
            lower.contains("192.168.") ||
            lower.contains("pollinations.ai")
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
