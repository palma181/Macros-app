package com.example.data.model

enum class MealType(val label: String) {
    BREAKFAST("Desayuno"),
    LUNCH("Comida"),
    DINNER("Cena"),
    SNACK("Snack");

    companion object {
        fun fromLabel(label: String): MealType =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) } ?: LUNCH
    }
}

enum class AiPreset(
    val id: String,
    val displayName: String,
    val defaultUrl: String,
    val defaultModel: String,
    val supportsVisionHint: String
) {
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        supportsVisionHint = "Requiere clave de API de platform.openai.com (usa saldo API, separado de ChatGPT Plus)"
    ),
    GEMINI(
        id = "gemini",
        displayName = "Gemini (Gratis)",
        defaultUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-2.0-flash",
        supportsVisionHint = "Nivel gratuito sin pagar en Google AI Studio · soporta fotos y texto"
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultUrl = "https://openrouter.ai/api/v1",
        defaultModel = "meta-llama/llama-3.2-11b-vision-instruct:free",
        supportsVisionHint = "Modelos terminados en :free son gratis sin saldo (o elige gpt-4o-mini / claude)"
    ),
    GROQ(
        id = "groq",
        displayName = "Groq (Gratis)",
        defaultUrl = "https://api.groq.com/openai/v1",
        defaultModel = "meta-llama/llama-4-scout-17b-16e-instruct",
        supportsVisionHint = "Nivel gratuito rápido en console.groq.com · llama-4-scout soporta fotos"
    ),
    LOCAL(
        id = "local",
        displayName = "Ollama / Local",
        defaultUrl = "http://localhost:11434/v1",
        defaultModel = "llava",
        supportsVisionHint = "100% gratis sin API key en tu PC/red local (llava, qwen2.5vl o gemma3)"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Otro",
        defaultUrl = "",
        defaultModel = "",
        supportsVisionHint = "Compatible con cualquier API /chat/completions"
    );

    companion object {
        fun fromId(id: String): AiPreset =
            entries.firstOrNull { it.id == id } ?: OPENAI
    }
}

data class MacroGoals(
    val kcal: Int = 2000,
    val protein: Int = 150,
    val carbs: Int = 200,
    val fat: Int = 60
)

data class MacroTotals(
    val kcal: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
)

data class AiConfig(
    val preset: AiPreset = AiPreset.OPENAI,
    val baseUrl: String = AiPreset.OPENAI.defaultUrl,
    val apiKey: String = "",
    val model: String = AiPreset.OPENAI.defaultModel
)

data class MacroEstimation(
    val name: String?,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int
)

data class DayCalories(
    val date: String,
    val dayInitial: String,
    val kcal: Int,
    val isToday: Boolean
)
