package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.data.local.MealEntity
import com.example.data.model.MacroEstimation
import com.example.data.model.MacroGoals
import com.example.data.model.MacroTotals
import com.example.data.model.RecipeDifficulty
import com.example.data.model.RecipeOption
import com.example.data.model.RecipeSection
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

object ExternalAiBridge {

    fun buildMealPrompt(description: String, hasPhotoHint: Boolean = false): String = buildString {
        if (hasPhotoHint) {
            append("Analiza la foto que te voy a adjuntar")
            if (description.isNotBlank()) {
                append(" (contexto: ").append(description.trim()).append(")")
            }
            append(" y estima sus calorías y macronutrientes totales. ")
        } else {
            append("Estima las calorías y macronutrientes totales de esta comida (usa porción estándar si faltan gramos): ")
            append(description.trim()).append(". ")
        }
        append("""Responde con este JSON exacto para pegarlo en mi app: {"name":"nombre corto","kcal":0,"p":0,"c":0,"f":0} """)
        append("(p=proteína en g, c=carbohidratos en g, f=grasa en g).")
    }

    fun buildCoachPeriodPrompt(
        periodLabel: String,
        daysCount: Int,
        goals: MacroGoals,
        meals: List<MealEntity>
    ): String {
        val summaryLines = meals.take(40).joinToString("; ") { m ->
            "${m.date} ${m.mealType}: ${m.name} (${m.kcal} kcal, P${m.protein}/C${m.carbs}/G${m.fat})"
        }
        return buildString {
            append("Actúa como mi coach de nutrición práctico en español. ")
            append("Analiza mi $periodLabel ($daysCount días) frente a mis metas diarias ")
            append("(${goals.kcal} kcal, ${goals.protein}g proteína, ${goals.carbs}g carbs, ${goals.fat}g grasa) ")
            append("y dame 3 recomendaciones concretas. ")
            if (summaryLines.isBlank()) {
                append("Aún no tengo comidas registradas en este periodo.")
            } else {
                append("Comidas: $summaryLines")
            }
        }
    }

    fun buildCoachQuestionPrompt(
        question: String,
        goals: MacroGoals,
        todayTotals: MacroTotals
    ): String = buildString {
        append("Eres mi coach de nutrición práctico en español. ")
        append("Metas diarias: ${goals.kcal} kcal, ${goals.protein}g P, ${goals.carbs}g C, ${goals.fat}g G. ")
        append("Hoy llevo: ${todayTotals.kcal} kcal, ${todayTotals.protein}g P, ${todayTotals.carbs}g C, ${todayTotals.fat}g G. ")
        append("Pregunta: ${question.trim()}")
    }

    fun buildRecipesPrompt(
        ingredients: List<String>,
        difficulty: RecipeDifficulty?,
        section: RecipeSection
    ): String {
        val kind = if (section == RecipeSection.SNACKS_DRINKS) {
            "snacks, bowls de fruta, batidos o bebidas funcionales (aprovechando los suplementos)"
        } else {
            "platillos de cocina"
        }
        val level = difficulty?.label ?: "Básico, Intermedio y Avanzado (1 de cada uno)"
        return buildString {
            append("Crea 3 opciones de $kind en nivel $level usando estos ingredientes: ")
            append(ingredients.joinToString(", ")).append(". ")
            append("""Responde ÚNICAMENTE con un JSON válido: {"recipes":[{"title":"...","difficulty":"basico|intermedio|avanzado","prepMinutes":15,"ingredients":["..."],"steps":["Paso 1...","Paso 2..."],"tips":"...","kcal":350,"p":28,"c":35,"f":10}]}""")
        }
    }

    fun copyToClipboard(context: Context, text: String, label: String = "Mis Macros Prompt") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun readClipboardText(context: Context): String? {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = clipboard?.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotBlank() }
    }

    fun sharePromptToExternalApp(context: Context, prompt: String, chooserTitle: String) {
        copyToClipboard(context, prompt)
        runCatching {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, prompt)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        }
    }

    fun parseMacroResponse(rawText: String): Result<MacroEstimation> = runCatching {
        val cleaned = rawText.trim()
        require(cleaned.isNotBlank()) { "El texto pegado está vacío." }

        // 1. Try JSON first
        val jsonMatch = Regex("""\{[\s\S]*\}""").find(cleaned)
        if (jsonMatch != null) {
            val obj = runCatching { JSONObject(jsonMatch.value) }.getOrNull()
            if (obj != null && (obj.has("kcal") || obj.has("calorias") || obj.has("calories"))) {
                val kcal = obj.optDouble("kcal", obj.optDouble("calorias", obj.optDouble("calories", 0.0)))
                    .roundToInt().coerceAtLeast(0)
                val p = obj.optDouble("p", obj.optDouble("protein", obj.optDouble("proteina", 0.0)))
                    .roundToInt().coerceAtLeast(0)
                val c = obj.optDouble("c", obj.optDouble("carbs", obj.optDouble("carbohidratos", 0.0)))
                    .roundToInt().coerceAtLeast(0)
                val f = obj.optDouble("f", obj.optDouble("fat", obj.optDouble("grasas", obj.optDouble("grasa", 0.0))))
                    .roundToInt().coerceAtLeast(0)
                val name = obj.optString("name", obj.optString("nombre", "")).trim().ifBlank { null }
                return@runCatching MacroEstimation(name = name, kcal = kcal, protein = p, carbs = c, fat = f)
            }
        }

        // 2. Natural language Spanish/English fallback (for non-JSON ChatGPT/Claude replies)
        val kcal = extractMetric(cleaned, listOf("kcal", "calor[ií]as?", "calories"))
        val p = extractMetric(cleaned, listOf("prote[ií]nas?", "protein", "\\bP\\b"))
        val c = extractMetric(cleaned, listOf("carbohidratos?", "hidratos", "carbs", "\\bC\\b"))
        val f = extractMetric(cleaned, listOf("grasas?", "l[ií]pidos?", "fat", "\\bG\\b"))

        if (kcal == null && p == null && c == null && f == null) {
            error("No se encontraron calorías ni macros en el texto pegado. Copia el JSON o el resumen numérico de ChatGPT/Claude.")
        }

        MacroEstimation(
            name = null,
            kcal = (kcal ?: 0).coerceAtLeast(0),
            protein = (p ?: 0).coerceAtLeast(0),
            carbs = (c ?: 0).coerceAtLeast(0),
            fat = (f ?: 0).coerceAtLeast(0)
        )
    }

    private fun extractMetric(text: String, labels: List<String>): Int? {
        val labelGroup = labels.joinToString("|")
        // Matches either "Calorías: 450" or "450 kcal"
        val afterLabel = Regex("""(?:$labelGroup)\s*[:=\-]?\s*~?\s*(\d{1,4})""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (afterLabel != null) return afterLabel

        val beforeLabel = Regex("""(\d{1,4})\s*(?:g|gr|gramos)?\s*(?:de\s+)?(?:$labelGroup)""", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return beforeLabel
    }

    fun parseRecipesResponse(
        rawText: String,
        ingredients: List<String>,
        section: RecipeSection
    ): Result<List<RecipeOption>> = runCatching {
        val match = Regex("""\{[\s\S]*\}""").find(rawText.trim())
            ?: error("Pega el bloque JSON devuelto por ChatGPT o Claude.")
        val root = JSONObject(match.value)
        val arr = root.optJSONArray("recipes") ?: JSONArray().apply {
            if (root.has("title")) put(root)
        }
        val list = mutableListOf<RecipeOption>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val title = obj.optString("title", "").trim()
            if (title.isBlank()) continue
            val ingArr = obj.optJSONArray("ingredients") ?: JSONArray()
            val ingList = (0 until ingArr.length()).map { idx -> ingArr.optString(idx) }.filter { it.isNotBlank() }
            val stepsArr = obj.optJSONArray("steps") ?: JSONArray()
            val stepList = (0 until stepsArr.length()).map { idx -> stepsArr.optString(idx) }.filter { it.isNotBlank() }
            list.add(
                RecipeOption(
                    id = "ext_${System.currentTimeMillis()}_$i",
                    title = title,
                    difficulty = RecipeDifficulty.fromId(obj.optString("difficulty", "basico")),
                    prepMinutes = obj.optInt("prepMinutes", 15).coerceIn(2, 180),
                    matchTags = ingredients,
                    ingredients = ingList.ifEmpty { ingredients },
                    steps = stepList.ifEmpty { listOf("Prepara e integra los ingredientes al gusto.") },
                    tips = obj.optString("tips", "Ajusta porciones según tus macros."),
                    kcal = obj.optDouble("kcal", 350.0).roundToInt().coerceAtLeast(0),
                    protein = obj.optDouble("p", 25.0).roundToInt().coerceAtLeast(0),
                    carbs = obj.optDouble("c", 35.0).roundToInt().coerceAtLeast(0),
                    fat = obj.optDouble("f", 10.0).roundToInt().coerceAtLeast(0),
                    section = section,
                    isAiGenerated = true
                )
            )
        }
        if (list.isEmpty()) error("No se encontraron recetas válidas en el JSON pegado.")
        list
    }
}
