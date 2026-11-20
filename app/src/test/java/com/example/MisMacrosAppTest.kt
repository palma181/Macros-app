package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.MealEntity
import com.example.data.model.MacroGoals
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
        // 2026-11-20 is Friday -> Monday is 2026-11-16
        assertEquals("2026-11-16", DateUtils.mondayOfWeek("2026-11-20"))
        assertEquals(30, DateUtils.daysInMonth("2026-11-20"))
        assertEquals(29, DateUtils.daysInMonth("2024-02-10")) // Leap year
        assertEquals(28, DateUtils.daysInMonth("2025-02-10"))
        assertEquals("2026-11-21", DateUtils.addDays("2026-11-20", 1))
    }

    @Test
    fun repository_exportsAndImportsPwaCompatibleBackupJson() = runTest {
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

            val json = repo.exportBackupJson()
            assertTrue(json.contains("\"Avena con proteína\""))
            assertTrue(json.contains("\"kcal\": 2200"))

            // Import a fresh PWA-style JSON payload
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

            val meals = repo.observeMealsInRange("2026-11-01", "2026-11-30").first()
            assertEquals(1, meals.size)
            assertEquals("Tacos de pollo", meals.first().name)

            val weights = repo.allWeightsFlow.first()
            assertEquals(1, weights.size)
            assertEquals(73.8, weights.first().kg, 0.01)

            val goals = repo.goalsFlow.first()
            assertEquals(1900, goals.kcal)
            assertEquals(140, goals.protein)
        } finally {
            db.close()
        }
    }

    @Test
    fun userJourney_addMealAndNavigateAllTabsWithoutErrors() {
        composeRule.waitForIdle()

        // 1. Open Add Meal sheet on Today tab and log a meal
        composeRule.onNodeWithTag("fab_add_meal").performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("meal_name_input").performTextInput("Bowl de salmón")
        composeRule.onNodeWithTag("meal_kcal_input").performTextInput("610")
        composeRule.onNodeWithTag("meal_protein_input").performTextInput("42")
        composeRule.onNodeWithTag("meal_carbs_input").performTextInput("54")
        composeRule.onNodeWithTag("meal_fat_input").performTextInput("22")
        composeRule.onNodeWithTag("meal_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("today_lazy_column")
            .performScrollToNode(hasText("Bowl de salmón"))
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Bowl de salmón").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Bowl de salmón").assertIsDisplayed()

        // 2. Navigate to Progress and toggle Semana / Mes
        composeRule.onNodeWithTag("nav_progress").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("segment_month").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("segment_week").performClick()
        composeRule.waitForIdle()

        // 3. Navigate to Weight and save a weight entry
        composeRule.onNodeWithTag("nav_weight").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("weight_kg_input").performTextInput("71.5")
        composeRule.onNodeWithTag("save_weight_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("weight_lazy_column")
            .performScrollToNode(hasText("71.5 kg"))
        composeRule.onNodeWithText("71.5 kg").assertIsDisplayed()

        // 4. Navigate to AI tab, switch presets, and save
        composeRule.onNodeWithTag("nav_ai").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_preset_gemini").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ai_save_button").performScrollTo().performClick()
        composeRule.waitForIdle()

        // 5. Navigate to Goals tab, update kcal goal, and save
        composeRule.onNodeWithTag("nav_goals").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("goal_kcal_input").performTextClearance()
        composeRule.onNodeWithTag("goal_kcal_input").performTextInput("2100")
        composeRule.onNodeWithTag("save_goals_button").performClick()
        composeRule.waitForIdle()
    }
}
