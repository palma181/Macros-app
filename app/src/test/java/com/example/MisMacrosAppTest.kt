package com.example

import android.content.Context
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MealEntity
import com.example.data.model.MacroGoals
import com.example.data.model.RecipeCatalog
import com.example.data.model.RecipeDifficulty
import com.example.data.model.RecipeSection
import com.example.data.preferences.AppPreferencesRepository
import com.example.data.repository.MacroRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MisMacrosAppTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun dateUtils_computesMondayAndDaysInMonthCorrectly() {
        assertEquals("2026-11-16", DateUtils.mondayOfWeek("2026-11-20"))
        assertEquals(30, DateUtils.daysInMonth("2026-11-20"))
        assertEquals(29, DateUtils.daysInMonth("2024-02-10"))
        assertEquals(28, DateUtils.daysInMonth("2025-02-10"))
        assertEquals("2026-11-21", DateUtils.addDays("2026-11-20", 1))
    }

    @Test
    fun recipeCatalog_matchesIngredientsAcrossAllDifficultyLevels() {
        val matches = RecipeCatalog.findMatchingRecipes(
            selectedIngredients = listOf("Pollo", "Arroz"),
            difficultyFilter = null
        )
        assertTrue(matches.isNotEmpty())
        assertTrue(matches.any { it.recipe.difficulty == RecipeDifficulty.BASIC })
        assertTrue(matches.any { it.recipe.difficulty == RecipeDifficulty.INTERMEDIATE })
        assertTrue(matches.any { it.recipe.difficulty == RecipeDifficulty.ADVANCED })

        val pastaMatches = RecipeCatalog.findMatchingRecipes(
            selectedIngredients = listOf("Fettuccini"),
            difficultyFilter = RecipeDifficulty.INTERMEDIATE
        )
        assertTrue(pastaMatches.any { it.recipe.title.contains("Fettuccini", ignoreCase = true) })
    }

    @Test
    fun recipeCatalog_matchesSnacksFruitsAndSupplementsAcrossAllLevels() {
        val snackMatches = RecipeCatalog.findMatchingRecipes(
            selectedIngredients = listOf("Plátano", "Proteína Whey", "Creatina"),
            difficultyFilter = null,
            section = RecipeSection.SNACKS_DRINKS
        )
        assertTrue(snackMatches.isNotEmpty())
        assertEquals(RecipeSection.SNACKS_DRINKS, snackMatches.first().recipe.section)
        assertTrue(snackMatches.any { it.recipe.difficulty == RecipeDifficulty.BASIC })
        assertTrue(snackMatches.any { it.recipe.difficulty == RecipeDifficulty.INTERMEDIATE })
        assertTrue(snackMatches.any { it.recipe.difficulty == RecipeDifficulty.ADVANCED })

        val suppMatches = RecipeCatalog.findMatchingRecipes(
            selectedIngredients = listOf("Caseína", "Colágeno", "BCAA / EAA", "Electrolitos"),
            difficultyFilter = null,
            section = RecipeSection.SNACKS_DRINKS
        )
        assertTrue(suppMatches.size >= 4)
    }

    @Test
    fun repository_exportsImportsAndSavesRecipes() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val prefs = AppPreferencesRepository(context)
            val repo = MacroRepository(context, db.macroDao(), prefs)

            repo.saveGoals(MacroGoals(kcal = 2200, protein = 170, carbs = 230, fat = 65))
            repo.insertMeal(
                MealEntity(
                    date = "2026-11-20",
                    mealType = "Desayuno",
                    name = "Avena con proteína",
                    kcal = 420,
                    protein = 35,
                    carbs = 50,
                    fat = 8
                )
            )
            repo.saveWeight("2026-11-20", 74.2)

            val recipe = RecipeCatalog.recipes.first()
            repo.saveRecipe(recipe)
            assertEquals(1, repo.savedRecipesFlow.first().size)
            repo.deleteSavedRecipe(recipe.id)
            assertEquals(0, repo.savedRecipesFlow.first().size)

            val json = repo.exportBackupJson()
            assertTrue(json.contains("\"Avena con proteína\""))

            val pwaJson = """
                {
                  "goals": {"kcal": 1900, "p": 140, "c": 180, "f": 55},
                  "meals": [
                    {"id": 123, "date": "2026-11-19", "type": "Comida", "name": "Tacos de pollo", "kcal": 520, "p": 38, "c": 48, "f": 16}
                  ],
                  "weights": [
                    {"date": "2026-11-19", "kg": 73.8}
                  ]
                }
            """.trimIndent()

            val importRes = repo.importBackupJson(pwaJson)
            assertTrue(importRes.isSuccess)
            assertEquals(1 to 1, importRes.getOrThrow())
        } finally {
            db.close()
        }
    }

    @Test
    fun userJourney_swipeAndNavigateAllSevenTabsHistoryAndRecipeManual() {
        composeRule.waitForIdle()

        // 1. Add a meal on Hoy
        composeRule.onNodeWithTag("fab_add_meal").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("meal_name_input").performTextInput("Bowl de salmón")
        composeRule.onNodeWithTag("meal_kcal_input").performTextInput("610")
        composeRule.onNodeWithTag("meal_protein_input").performTextInput("42")
        composeRule.onNodeWithTag("meal_carbs_input").performTextInput("54")
        composeRule.onNodeWithTag("meal_fat_input").performTextInput("22")
        composeRule.onNodeWithTag("meal_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        waitUntilScrollToNode("today_lazy_column", hasText("Bowl de salmón"))
        composeRule.onNode(
            hasText("Bowl de salmón") and hasAnyAncestor(hasTestTag("today_lazy_column"))
        ).assertIsDisplayed()

        // 2. Swipe smoothly to Comidas (Historial) and verify daily meal + kcal appear
        composeRule.onNodeWithTag("main_horizontal_pager").performTouchInput { swipeLeft() }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithTag("meals_history_list").fetchSemanticsNodes().isNotEmpty()
        }
        waitUntilScrollToNode("meals_history_list", hasText("Bowl de salmón"))
        composeRule.onNode(
            hasText("Bowl de salmón") and hasAnyAncestor(hasTestTag("meals_history_list"))
        ).assertIsDisplayed()

        // 3. Navigate to Cocina (Recetas), select ingredients, open manual, and log as meal
        composeRule.onNodeWithTag("bottom_pill_bar").performScrollToNode(hasTestTag("nav_recipes"))
        composeRule.onNodeWithTag("nav_recipes").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ing_chip_pollo").performClick()
        composeRule.onNodeWithTag("recipes_lazy_column")
            .performScrollToNode(hasTestTag("diff_basico"))
        composeRule.onNodeWithTag("diff_basico").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("recipes_lazy_column")
            .performScrollToNode(hasTestTag("recipe_card_basic_pollo_arroz"))
        composeRule.onNodeWithTag("recipe_card_basic_pollo_arroz").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Manual de preparación").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("log_recipe_as_meal_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 3b. Switch to Snacks y Bebidas apartado inside Cocina, pick fruit + supplement, open manual and log
        composeRule.onNodeWithTag("recipes_lazy_column")
            .performScrollToNode(hasTestTag("section_snacks_bebidas"))
        composeRule.onNodeWithTag("section_snacks_bebidas").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("ing_chip_platano").performClick()
        composeRule.onNodeWithTag("ing_chip_proteina whey").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("recipes_lazy_column")
            .performScrollToNode(hasTestTag("recipe_card_snack_basic_batido_whey_platano_creatina"))
        composeRule.onNodeWithTag("recipe_card_snack_basic_batido_whey_platano_creatina").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Manual de preparación").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("log_recipe_as_meal_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 4. Navigate to Progreso
        composeRule.onNodeWithTag("bottom_pill_bar").performScrollToNode(hasTestTag("nav_progress"))
        composeRule.onNodeWithTag("nav_progress").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("segment_month").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("segment_week").performClick()
        composeRule.waitForIdle()

        // 5. Navigate to Peso and save a weight entry
        composeRule.onNodeWithTag("bottom_pill_bar").performScrollToNode(hasTestTag("nav_weight"))
        composeRule.onNodeWithTag("nav_weight").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithTag("weight_kg_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("weight_kg_input").performTextInput("71.5")
        composeRule.onNodeWithTag("save_weight_button").performClick()
        composeRule.waitForIdle()

        val todayWeightTag = "weight_row_${DateUtils.todayIso()}"
        waitUntilScrollToNode("weight_lazy_column", hasTestTag(todayWeightTag))
        composeRule.onNodeWithTag(todayWeightTag).assertIsDisplayed()

        // 6. Navigate to IA
        composeRule.onNodeWithTag("bottom_pill_bar").performScrollToNode(hasTestTag("nav_ai"))
        composeRule.onNodeWithTag("nav_ai").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithTag("ai_preset_gemini").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("ai_preset_gemini").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 7. Navigate to Metas
        composeRule.onNodeWithTag("bottom_pill_bar").performScrollToNode(hasTestTag("nav_goals"))
        composeRule.onNodeWithTag("nav_goals").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithTag("goal_kcal_input").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("goal_kcal_input").performTextClearance()
        composeRule.onNodeWithTag("goal_kcal_input").performTextInput("2100")
        composeRule.onNodeWithTag("save_goals_button").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun externalAiBridge_clipboardPastePopulatesMealAndRecipesWithoutApiKey() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeRule.waitForIdle()

        // 1. Copy ChatGPT/Claude macro JSON to clipboard and paste inside MealEditorSheet
        com.example.util.ExternalAiBridge.copyToClipboard(
            context,
            """{"name":"Sándwich de pavo","kcal":390,"p":29,"c":41,"f":11}"""
        )
        composeRule.onNodeWithTag("fab_add_meal").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("meal_share_external_ai_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("meal_paste_external_ai_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("meal_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()
        waitUntilScrollToNode("today_lazy_column", hasText("Sándwich de pavo"))
        composeRule.onNode(
            hasText("Sándwich de pavo") and hasAnyAncestor(hasTestTag("today_lazy_column"))
        ).assertIsDisplayed()

        // 2. Paste ChatGPT/Claude recipe JSON inside Cocina
        composeRule.onNodeWithTag("bottom_pill_bar").performScrollToNode(hasTestTag("nav_recipes"))
        composeRule.onNodeWithTag("nav_recipes").performClick()
        composeRule.waitForIdle()

        com.example.util.ExternalAiBridge.copyToClipboard(
            context,
            """{"recipes":[{"title":"Pollo al ajillo expres","difficulty":"basico","prepMinutes":12,"ingredients":["160g pollo"],"steps":["Dora el pollo con ajo"],"tips":"Añade limón","kcal":310,"p":36,"c":4,"f":14}]}"""
        )
        waitUntilScrollToNode("recipes_lazy_column", hasTestTag("recipes_paste_external_ai_button"))
        composeRule.onNodeWithTag("recipes_paste_external_ai_button").performClick()
        composeRule.waitForIdle()

        waitUntilScrollToNode("recipes_lazy_column", hasText("Pollo al ajillo expres"))
        composeRule.onNode(
            hasText("Pollo al ajillo expres") and hasAnyAncestor(hasTestTag("recipes_lazy_column"))
        ).assertIsDisplayed()

        // 3. Verify free API presets + Pollinations + Detect Models button + ChatGPT/Claude bridge in IA screen
        composeRule.onNodeWithTag("bottom_pill_bar").performScrollToNode(hasTestTag("nav_ai"))
        composeRule.onNodeWithTag("nav_ai").performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithTag("ai_preset_pollinations").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("ai_preset_pollinations").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_preset_mistral").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_preset_cerebras").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_preset_sambanova").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_preset_huggingface").assertIsDisplayed()
        composeRule.onNodeWithTag("ai_preset_nvidia").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_get_key_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("ai_detect_models_button").performScrollTo().assertIsDisplayed()
        waitUntilScrollToNode("ai_lazy_column", hasTestTag("external_ai_week_button"))
        composeRule.onNodeWithTag("external_ai_week_button").assertIsDisplayed()
        composeRule.onNodeWithTag("external_ai_question_button").assertIsDisplayed()
    }

    private fun waitUntilScrollToNode(listTag: String, matcher: SemanticsMatcher) {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            runCatching {
                composeRule.onNodeWithTag(listTag).performScrollToNode(matcher)
            }.isSuccess
        }
    }
}
