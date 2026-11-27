package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.MacroDao
import com.example.data.local.MealEntity
import com.example.data.local.WeightEntity
import com.example.data.model.AiConfig
import com.example.data.model.MacroEstimation
import com.example.data.model.MacroGoals
import com.example.data.model.MacroTotals
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.remote.OpenAiClient
import com.example.util.ImageProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MacroRepository(
    private val context: Context,
    private val dao: MacroDao,
    private val prefs: AppPreferencesRepository,
    private val openAiClient: OpenAiClient = OpenAiClient()
) {
    val goalsFlow: Flow<MacroGoals> = prefs.goalsFlow
    val aiConfigFlow: Flow<AiConfig> = prefs.aiConfigFlow
    val allWeightsFlow: Flow<List<WeightEntity>> = dao.observeAllWeights()
    val allMealsFlow: Flow<List<MealEntity>> = dao.observeAllMeals()
    val savedRecipesFlow: Flow<List<com.example.data.local.SavedRecipeEntity>> = dao.observeSavedRecipes()

    fun observeMealsInRange(startDate: String, endDate: String): Flow<List<MealEntity>> =
        dao.observeMealsInRange(startDate, endDate)

    suspend fun insertMeal(meal: MealEntity): Long = dao.insertMeal(meal)

    suspend fun updateMeal(meal: MealEntity) = dao.updateMeal(meal)

    suspend fun deleteMeal(id: Long) = dao.deleteMealById(id)

    suspend fun saveRecipe(recipe: com.example.data.model.RecipeOption) {
        dao.upsertSavedRecipe(recipe.toSavedEntity())
    }

    suspend fun deleteSavedRecipe(id: String) = dao.deleteSavedRecipeById(id)

    suspend fun generateAiRecipes(
        ingredients: List<String>,
        difficulty: com.example.data.model.RecipeDifficulty?,
        section: com.example.data.model.RecipeSection = com.example.data.model.RecipeSection.MEALS
    ): Result<List<com.example.data.model.RecipeOption>> {
        val config = prefs.aiConfigFlow.first()
        return openAiClient.generateRecipesFromIngredients(config, ingredients, difficulty, section)
    }

    suspend fun saveWeight(date: String, kg: Double) {
        dao.upsertWeight(WeightEntity(date = date, kg = kg))
    }

    suspend fun deleteWeight(date: String) = dao.deleteWeightByDate(date)

    suspend fun saveGoals(goals: MacroGoals) = prefs.saveGoals(goals)

    suspend fun saveAiConfig(config: AiConfig) = prefs.saveAiConfig(config)

    suspend fun estimateFromText(description: String): Result<MacroEstimation> {
        val config = prefs.aiConfigFlow.first()
        return openAiClient.estimateFromText(config, description)
    }

    suspend fun estimateFromImageUri(uri: Uri, hint: String?): Result<MacroEstimation> {
        val config = prefs.aiConfigFlow.first()
        return runCatching {
            ImageProcessor.uriToBase64DataUrl(context, uri)
        }.fold(
            onSuccess = { dataUrl -> openAiClient.estimateFromImage(config, dataUrl, hint) },
            onFailure = { Result.failure(it) }
        )
    }

    suspend fun testAiConnection(config: AiConfig): Result<String> {
        return openAiClient.testConnection(config)
    }

    suspend fun analyzePeriodWithCoach(
        periodLabel: String,
        daysCount: Int,
        startDate: String,
        endDate: String
    ): Result<String> {
        val config = prefs.aiConfigFlow.first()
        val goals = prefs.goalsFlow.first()
        val meals = dao.getMealsInRangeOnce(startDate, endDate)
        val mealsArray = JSONArray().apply {
            meals.forEach { m ->
                put(JSONObject().apply {
                    put("date", m.date)
                    put("type", m.mealType)
                    put("name", m.name)
                    put("kcal", m.kcal)
                    put("p", m.protein)
                    put("c", m.carbs)
                    put("f", m.fat)
                })
            }
        }
        val goalsJson = JSONObject().apply {
            put("kcal", goals.kcal)
            put("p", goals.protein)
            put("c", goals.carbs)
            put("f", goals.fat)
        }
        val prompt = buildString {
            append("Analiza mi $periodLabel de alimentación frente a mis metas y dame 3 recomendaciones concretas. ")
            append("Sé claro, práctico y breve, en español. ")
            append("Metas diarias: $goalsJson. ")
            append("Días del periodo: $daysCount. ")
            append("Comidas: $mealsArray")
        }
        return openAiClient.askCoach(config, prompt)
    }

    suspend fun askCoachQuestion(question: String, todayDate: String): Result<String> {
        val config = prefs.aiConfigFlow.first()
        val goals = prefs.goalsFlow.first()
        val todayMeals = dao.getMealsInRangeOnce(todayDate, todayDate)
        val totals = todayMeals.fold(MacroTotals()) { acc, m ->
            MacroTotals(
                kcal = acc.kcal + m.kcal,
                protein = acc.protein + m.protein,
                carbs = acc.carbs + m.carbs,
                fat = acc.fat + m.fat
            )
        }
        val prompt = buildString {
            append("Eres mi coach de nutrición práctico. Responde breve y en español. ")
            append("Mis metas diarias: kcal=${goals.kcal}, p=${goals.protein}g, c=${goals.carbs}g, f=${goals.fat}g. ")
            append("Hoy llevo: kcal=${totals.kcal}, p=${totals.protein}g, c=${totals.carbs}g, f=${totals.fat}g. ")
            append("Pregunta: ${question.trim()}")
        }
        return openAiClient.askCoach(config, prompt)
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val goals = prefs.goalsFlow.first()
        val meals = dao.getAllMealsOnce()
        val weights = dao.getAllWeightsOnce()

        val root = JSONObject().apply {
            put("goals", JSONObject().apply {
                put("kcal", goals.kcal)
                put("p", goals.protein)
                put("c", goals.carbs)
                put("f", goals.fat)
            })
            put("meals", JSONArray().apply {
                meals.forEach { m ->
                    put(JSONObject().apply {
                        put("id", m.id)
                        put("date", m.date)
                        put("type", m.mealType)
                        put("name", m.name)
                        put("kcal", m.kcal)
                        put("p", m.protein)
                        put("c", m.carbs)
                        put("f", m.fat)
                    })
                }
            })
            put("weights", JSONArray().apply {
                weights.forEach { w ->
                    put(JSONObject().apply {
                        put("date", w.date)
                        put("kg", w.kg)
                    })
                }
            })
        }
        root.toString(2)
    }

    suspend fun importBackupJson(rawJson: String): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JSONObject(rawJson)
            root.optJSONObject("goals")?.let { g ->
                prefs.saveGoals(
                    MacroGoals(
                        kcal = g.optInt("kcal", 2000).coerceAtLeast(0),
                        protein = g.optInt("p", g.optInt("protein", 150)).coerceAtLeast(0),
                        carbs = g.optInt("c", g.optInt("carbs", 200)).coerceAtLeast(0),
                        fat = g.optInt("f", g.optInt("fat", 60)).coerceAtLeast(0)
                    )
                )
            }

            val mealsList = mutableListOf<MealEntity>()
            val mealsArray = root.optJSONArray("meals") ?: JSONArray()
            for (i in 0 until mealsArray.length()) {
                val obj = mealsArray.getJSONObject(i)
                val name = obj.optString("name", "").trim()
                val date = obj.optString("date", "").trim()
                if (name.isNotBlank() && date.isNotBlank()) {
                    mealsList.add(
                        MealEntity(
                            date = date,
                            mealType = obj.optString("type", obj.optString("mealType", "Comida")),
                            name = name,
                            kcal = obj.optInt("kcal", 0),
                            protein = obj.optInt("p", obj.optInt("protein", 0)),
                            carbs = obj.optInt("c", obj.optInt("carbs", 0)),
                            fat = obj.optInt("f", obj.optInt("fat", 0))
                        )
                    )
                }
            }

            val weightsList = mutableListOf<WeightEntity>()
            val weightsArray = root.optJSONArray("weights") ?: JSONArray()
            for (i in 0 until weightsArray.length()) {
                val obj = weightsArray.getJSONObject(i)
                val date = obj.optString("date", "").trim()
                val kg = obj.optDouble("kg", Double.NaN)
                if (date.isNotBlank() && !kg.isNaN() && kg > 0.0) {
                    weightsList.add(WeightEntity(date = date, kg = kg))
                }
            }

            dao.replaceBackupData(mealsList, weightsList)
            mealsList.size to weightsList.size
        }
    }
}
