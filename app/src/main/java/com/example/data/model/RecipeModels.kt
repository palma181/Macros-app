package com.example.data.model

import com.example.data.local.SavedRecipeEntity

enum class RecipeDifficulty(val id: String, val label: String) {
    BASIC("basico", "Básico"),
    INTERMEDIATE("intermedio", "Intermedio"),
    ADVANCED("avanzado", "Avanzado");

    companion object {
        fun fromId(id: String): RecipeDifficulty =
            entries.firstOrNull {
                it.id.equals(id, ignoreCase = true) || it.label.equals(id, ignoreCase = true)
            } ?: BASIC
    }
}

data class IngredientGroup(
    val title: String,
    val items: List<String>
)

data class RecipeOption(
    val id: String,
    val title: String,
    val difficulty: RecipeDifficulty,
    val prepMinutes: Int,
    val matchTags: List<String>,
    val ingredients: List<String>,
    val steps: List<String>,
    val tips: String,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val isAiGenerated: Boolean = false
) {
    fun toSavedEntity(): SavedRecipeEntity = SavedRecipeEntity(
        id = id,
        title = title,
        difficulty = difficulty.id,
        prepMinutes = prepMinutes,
        ingredientsJoined = ingredients.joinToString("\n"),
        stepsJoined = steps.joinToString("\n"),
        tips = tips,
        kcal = kcal,
        protein = protein,
        carbs = carbs,
        fat = fat
    )

    companion object {
        fun fromSavedEntity(entity: SavedRecipeEntity): RecipeOption = RecipeOption(
            id = entity.id,
            title = entity.title,
            difficulty = RecipeDifficulty.fromId(entity.difficulty),
            prepMinutes = entity.prepMinutes,
            matchTags = emptyList(),
            ingredients = entity.ingredientsJoined.split("\n").filter { it.isNotBlank() },
            steps = entity.stepsJoined.split("\n").filter { it.isNotBlank() },
            tips = entity.tips,
            kcal = entity.kcal,
            protein = entity.protein,
            carbs = entity.carbs,
            fat = entity.fat
        )
    }
}

data class RecipeMatchResult(
    val recipe: RecipeOption,
    val matchedIngredients: List<String>
)
