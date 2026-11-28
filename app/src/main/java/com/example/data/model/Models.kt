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
    val supportsVisionHint: String,
    val signupUrl: String = "",
    val isFreeTier: Boolean = false
) {
    GEMINI(
        id = "gemini",
        displayName = "Gemini (Gratis)",
        defaultUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-2.0-flash",
        supportsVisionHint = "Gratis sin tarjeta en Google AI Studio · soporta fotos y texto",
        signupUrl = "https://aistudio.google.com/apikey",
        isFreeTier = true
    ),
    GROQ(
        id = "groq",
        displayName = "Groq (Gratis)",
        defaultUrl = "https://api.groq.com/openai/v1",
        defaultModel = "meta-llama/llama-4-scout-17b-16e-instruct",
        supportsVisionHint = "Gratis y ultrarrápido en console.groq.com · soporta fotos y texto",
        signupUrl = "https://console.groq.com/keys",
        isFreeTier = true
    ),
    GITHUB_MODELS(
        id = "github",
        displayName = "GitHub (Gratis)",
        defaultUrl = "https://models.inference.ai.azure.com",
        defaultModel = "gpt-4o-mini",
        supportsVisionHint = "Gratis con tu token de GitHub (github.com/settings/tokens) · gpt-4o-mini soporta fotos",
        signupUrl = "https://github.com/settings/tokens",
        isFreeTier = true
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter (Gratis)",
        defaultUrl = "https://openrouter.ai/api/v1",
        defaultModel = "meta-llama/llama-3.2-11b-vision-instruct:free",
        supportsVisionHint = "Modelos con :free cuestan $0 sin tarjeta (ej. llama-3.2-11b-vision-instruct:free)",
        signupUrl = "https://openrouter.ai/keys",
        isFreeTier = true
    ),
    MISTRAL(
        id = "mistral",
        displayName = "Mistral (Gratis)",
        defaultUrl = "https://api.mistral.ai/v1",
        defaultModel = "mistral-small-latest",
        supportsVisionHint = "Plan Experiment gratis en console.mistral.ai · soporta fotos (pixtral-12b-2409 / mistral-small-latest)",
        signupUrl = "https://console.mistral.ai/api-keys",
        isFreeTier = true
    ),
    CEREBRAS(
        id = "cerebras",
        displayName = "Cerebras (Gratis)",
        defaultUrl = "https://api.cerebras.ai/v1",
        defaultModel = "gpt-oss-120b",
        supportsVisionHint = "Nivel gratuito veloz en cloud.cerebras.ai (solo texto: gpt-oss-120b o qwen-3.8-27b)",
        signupUrl = "https://cloud.cerebras.ai",
        isFreeTier = true
    ),
    SAMBANOVA(
        id = "sambanova",
        displayName = "SambaNova (Gratis)",
        defaultUrl = "https://api.sambanova.ai/v1",
        defaultModel = "gemma-4-31B-it",
        supportsVisionHint = "Gratis sin tarjeta en cloud.sambanova.ai · gemma-4-31B-it soporta fotos y texto",
        signupUrl = "https://cloud.sambanova.ai",
        isFreeTier = true
    ),
    HUGGINGFACE(
        id = "huggingface",
        displayName = "HuggingFace (Gratis)",
        defaultUrl = "https://router.huggingface.co/v1",
        defaultModel = "Qwen/Qwen2.5-VL-7B-Instruct",
        supportsVisionHint = "Créditos mensuales gratis con token de Hugging Face · Qwen2.5-VL soporta fotos",
        signupUrl = "https://huggingface.co/settings/tokens",
        isFreeTier = true
    ),
    LOCAL(
        id = "local",
        displayName = "Ollama / Local",
        defaultUrl = "http://localhost:11434/v1",
        defaultModel = "llava",
        supportsVisionHint = "100% gratis sin API key en tu PC/red local (llava, qwen2.5vl o gemma3)",
        isFreeTier = true
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI (Pago)",
        defaultUrl = "https://api.openai.com/v1",
        defaultModel = "gpt-4o-mini",
        supportsVisionHint = "Requiere clave con saldo en platform.openai.com (separado de ChatGPT Plus)",
        signupUrl = "https://platform.openai.com/api-keys",
        isFreeTier = false
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
