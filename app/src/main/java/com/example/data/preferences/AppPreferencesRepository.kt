package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiConfig
import com.example.data.model.AiPreset
import com.example.data.model.MacroGoals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mis_macros_prefs")

class AppPreferencesRepository(private val context: Context) {

    private object Keys {
        val GOAL_KCAL = intPreferencesKey("goal_kcal")
        val GOAL_PROTEIN = intPreferencesKey("goal_protein")
        val GOAL_CARBS = intPreferencesKey("goal_carbs")
        val GOAL_FAT = intPreferencesKey("goal_fat")

        val AI_PRESET = stringPreferencesKey("ai_preset")
        val AI_URL = stringPreferencesKey("ai_url")
        val AI_KEY = stringPreferencesKey("ai_key")
        val AI_MODEL = stringPreferencesKey("ai_model")
    }

    val goalsFlow: Flow<MacroGoals> = context.dataStore.data.map { prefs ->
        MacroGoals(
            kcal = prefs[Keys.GOAL_KCAL] ?: 2000,
            protein = prefs[Keys.GOAL_PROTEIN] ?: 150,
            carbs = prefs[Keys.GOAL_CARBS] ?: 200,
            fat = prefs[Keys.GOAL_FAT] ?: 60
        )
    }

    val aiConfigFlow: Flow<AiConfig> = context.dataStore.data.map { prefs ->
        val rawPresetId = prefs[Keys.AI_PRESET] ?: AiPreset.POLLINATIONS.id
        val rawUrl = prefs[Keys.AI_URL].orEmpty()
        val isRetiredGithub = rawPresetId == "github" ||
            rawUrl.contains("models.github.ai", ignoreCase = true) ||
            rawUrl.contains("models.inference.ai.azure.com", ignoreCase = true)

        if (isRetiredGithub) {
            AiConfig(
                preset = AiPreset.POLLINATIONS,
                baseUrl = AiPreset.POLLINATIONS.defaultUrl,
                apiKey = "",
                model = AiPreset.POLLINATIONS.defaultModel
            )
        } else {
            val preset = AiPreset.fromId(rawPresetId)
            val storedModel = prefs[Keys.AI_MODEL] ?: preset.defaultModel
            val migratedModel = when {
                storedModel == "gemini-2.0-flash" -> "gemini-3.5-flash-lite"
                rawUrl.contains("api.groq.com", ignoreCase = true) &&
                    (storedModel == "llama-3.3-70b-versatile" || storedModel == "llama-3.1-8b-instant") ->
                    "openai/gpt-oss-120b"
                else -> storedModel
            }
            AiConfig(
                preset = preset,
                baseUrl = rawUrl.ifBlank { preset.defaultUrl },
                apiKey = prefs[Keys.AI_KEY] ?: "",
                model = migratedModel
            )
        }
    }

    suspend fun saveGoals(goals: MacroGoals) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOAL_KCAL] = goals.kcal.coerceAtLeast(0)
            prefs[Keys.GOAL_PROTEIN] = goals.protein.coerceAtLeast(0)
            prefs[Keys.GOAL_CARBS] = goals.carbs.coerceAtLeast(0)
            prefs[Keys.GOAL_FAT] = goals.fat.coerceAtLeast(0)
        }
    }

    suspend fun saveAiConfig(config: AiConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AI_PRESET] = config.preset.id
            prefs[Keys.AI_URL] = config.baseUrl.trim()
            prefs[Keys.AI_KEY] = config.apiKey.trim()
            prefs[Keys.AI_MODEL] = config.model.trim()
        }
    }
}
