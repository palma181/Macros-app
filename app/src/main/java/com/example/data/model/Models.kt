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
    val isFreeTier: Boolean = false,
    val requiresApiKey: Boolean = true
) {
    POLLINATIONS(
        id = "pollinations",
        displayName = "Pollinations (Gratis / Sin Key)",
        defaultUrl = "https://gen.pollinations.ai/v1",
        defaultModel = "openai",
        supportsVisionHint = "Gratis sin tarjeta e incluso sin API key (o saca una gratis en enter.pollinations.ai). Texto y fotos.",
        signupUrl = "https://enter.pollinations.ai",
        isFreeTier = true,
        requiresApiKey = false
    ),
    GEMINI(
        id = "gemini",
        displayName = "Gemini (Gratis)",
        defaultUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
        defaultModel = "gemini-3.5-flash-lite",
        supportsVisionHint = "Gratis sin tarjeta en Google AI Studio · gemini-3.5-flash-lite o gemini-3.6-flash (fotos y texto)",
        signupUrl = "https://aistudio.google.com/apikey",
        isFreeTier = true
    ),
    GROQ(
        id = "groq",
        displayName = "Groq (Gratis)",
        defaultUrl = "https://api.groq.com/openai/v1",
        defaultModel = "openai/gpt-oss-120b",
        supportsVisionHint = "Gratis y ultrarrápido en console.groq.com (openai/gpt-oss-120b o openai/gpt-oss-20b)",
        signupUrl = "https://console.groq.com/keys",
        isFreeTier = true
    ),
    NVIDIA(
        id = "nvidia",
        displayName = "NVIDIA (Gratis)",
        defaultUrl = "https://integrate.api.nvidia.com/v1",
        defaultModel = "meta/llama-3.3-70b-instruct",
        supportsVisionHint = "Gratis sin tarjeta en build.nvidia.com · catálogo amplio de modelos rápidos",
        signupUrl = "https://build.nvidia.com",
        isFreeTier = true
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter (Gratis)",
        defaultUrl = "https://openrouter.ai/api/v1",
        defaultModel = "openrouter/free",
        supportsVisionHint = "openrouter/free elige automáticamente un modelo $0 disponible (texto y fotos sin tarjeta)",
        signupUrl = "https://openrouter.ai/keys",
        isFreeTier = true
    ),
    MISTRAL(
        id = "mistral",
        displayName = "Mistral (Gratis)",
        defaultUrl = "https://api.mistral.ai/v1",
        defaultModel = "mistral-small-latest",
        supportsVisionHint = "Plan Experiment gratis sin tarjeta en console.mistral.ai · soporta fotos y texto",
        signupUrl = "https://console.mistral.ai/api-keys",
        isFreeTier = true
    ),
    CEREBRAS(
        id = "cerebras",
        displayName = "Cerebras (Gratis)",
        defaultUrl = "https://api.cerebras.ai/v1",
        defaultModel = "gpt-oss-120b",
        supportsVisionHint = "Nivel gratuito veloz en cloud.cerebras.ai (gpt-oss-120b o qwen-3.8-27b)",
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
        defaultModel = "meta-llama/Llama-3.1-8B-Instruct:fastest",
        supportsVisionHint = "Créditos mensuales gratis con token de Hugging Face · usa 'Detectar modelos' si cambian",
        signupUrl = "https://huggingface.co/settings/tokens",
        isFreeTier = true
    ),
    LOCAL(
        id = "local",
        displayName = "Ollama / Local",
        defaultUrl = "http://localhost:11434/v1",
        defaultModel = "llava",
        supportsVisionHint = "100% gratis sin API key en tu PC/red local (llava, qwen2.5vl o gemma3)",
        isFreeTier = true,
        requiresApiKey = false
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
            entries.firstOrNull { it.id == id } ?: POLLINATIONS
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
