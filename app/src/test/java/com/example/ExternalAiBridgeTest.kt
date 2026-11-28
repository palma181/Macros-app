package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MealEntity
import com.example.data.model.MacroGoals
import com.example.data.model.MacroTotals
import com.example.data.model.RecipeDifficulty
import com.example.data.model.RecipeSection
import com.example.util.ExternalAiBridge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExternalAiBridgeTest {

    @Test
    fun `parseMacroResponse parses markdown wrapped JSON from ChatGPT or Claude`() {
        val reply = """
            Aquí tienes el cálculo aproximado:
            ```json
            {"name":"Tacos de bistec","kcal":520,"p":36,"c":44,"f":20}
            ```
        """.trimIndent()

        val result = ExternalAiBridge.parseMacroResponse(reply)
        assertTrue(result.isSuccess)
        val est = result.getOrThrow()
        assertEquals("Tacos de bistec", est.name)
        assertEquals(520, est.kcal)
        assertEquals(36, est.protein)
        assertEquals(44, est.carbs)
        assertEquals(20, est.fat)
    }

    @Test
    fun `parseMacroResponse falls back to Spanish natural text`() {
        val reply = """
            Estimación total de tu plato:
            - Calorías: 615 kcal
            - Proteínas: 42 g
            - Carbohidratos: 55 g
            - Grasas: 24 g
        """.trimIndent()

        val result = ExternalAiBridge.parseMacroResponse(reply)
        assertTrue(result.isSuccess)
        val est = result.getOrThrow()
        assertEquals(615, est.kcal)
        assertEquals(42, est.protein)
        assertEquals(55, est.carbs)
        assertEquals(24, est.fat)
    }

    @Test
    fun `parseMacroResponse fails clearly on empty or non numeric text`() {
        assertTrue(ExternalAiBridge.parseMacroResponse("   ").isFailure)
        assertTrue(ExternalAiBridge.parseMacroResponse("Hola, ¿en qué puedo ayudarte hoy?").isFailure)
    }

    @Test
    fun `parseRecipesResponse parses recipes JSON for snacks and supplements`() {
        val reply = """
            Claro, aquí tienes tus recetas:
            ```json
            {
              "recipes": [
                {
                  "title": "Licuado proteico de mango y creatina",
                  "difficulty": "basico",
                  "prepMinutes": 4,
                  "ingredients": ["150g mango", "1 scoop whey", "5g creatina"],
                  "steps": ["Agrega agua fría", "Licúa por 30 segundos"],
                  "tips": "Sirve con hielo",
                  "kcal": 265,
                  "p": 28,
                  "c": 31,
                  "f": 3
                }
              ]
            }
            ```
        """.trimIndent()

        val result = ExternalAiBridge.parseRecipesResponse(
            rawText = reply,
            ingredients = listOf("Mango", "Proteína Whey", "Creatina"),
            section = RecipeSection.SNACKS_DRINKS
        )
        assertTrue(result.isSuccess)
        val recipes = result.getOrThrow()
        assertEquals(1, recipes.size)
        val first = recipes.first()
        assertEquals("Licuado proteico de mango y creatina", first.title)
        assertEquals(RecipeDifficulty.BASIC, first.difficulty)
        assertEquals(RecipeSection.SNACKS_DRINKS, first.section)
        assertEquals(265, first.kcal)
        assertEquals(2, first.steps.size)
    }

    @Test
    fun `clipboard copy read and prompt builders work cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mealPrompt = ExternalAiBridge.buildMealPrompt("2 huevos con aguacate", hasPhotoHint = false)
        assertTrue(mealPrompt.contains("2 huevos con aguacate"))
        assertTrue(mealPrompt.contains("\"kcal\""))

        val coachPrompt = ExternalAiBridge.buildCoachPeriodPrompt(
            periodLabel = "semana",
            daysCount = 7,
            goals = MacroGoals(kcal = 2100, protein = 160, carbs = 210, fat = 65),
            meals = listOf(
                MealEntity(
                    date = "2026-11-27",
                    mealType = "Comida",
                    name = "Pollo con arroz",
                    kcal = 550,
                    protein = 45,
                    carbs = 60,
                    fat = 12
                )
            )
        )
        assertTrue(coachPrompt.contains("2100 kcal"))
        assertTrue(coachPrompt.contains("Pollo con arroz"))

        val qPrompt = ExternalAiBridge.buildCoachQuestionPrompt(
            question = "¿Qué ceno?",
            goals = MacroGoals(kcal = 2000, protein = 150, carbs = 200, fat = 60),
            todayTotals = MacroTotals(kcal = 1400, protein = 100, carbs = 140, fat = 40)
        )
        assertTrue(qPrompt.contains("¿Qué ceno?"))
        assertTrue(qPrompt.contains("1400 kcal"))

        ExternalAiBridge.copyToClipboard(context, qPrompt)
        assertEquals(qPrompt, ExternalAiBridge.readClipboardText(context))
    }
}
