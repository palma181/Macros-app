package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MacroDao {

    @Query("SELECT * FROM meals WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, createdAt DESC")
    fun observeMealsInRange(startDate: String, endDate: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY date DESC, createdAt DESC")
    fun observeAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, createdAt ASC")
    suspend fun getMealsInRangeOnce(startDate: String, endDate: String): List<MealEntity>

    @Query("SELECT * FROM meals ORDER BY date ASC, createdAt ASC")
    suspend fun getAllMealsOnce(): List<MealEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Long)

    @Query("DELETE FROM meals")
    suspend fun clearAllMeals()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>)

    @Query("SELECT * FROM weights ORDER BY date ASC")
    fun observeAllWeights(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weights ORDER BY date ASC")
    suspend fun getAllWeightsOnce(): List<WeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeight(weight: WeightEntity)

    @Query("DELETE FROM weights WHERE date = :date")
    suspend fun deleteWeightByDate(date: String)

    @Query("DELETE FROM weights")
    suspend fun clearAllWeights()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeights(weights: List<WeightEntity>)

    @Query("SELECT * FROM saved_recipes ORDER BY createdAt DESC")
    fun observeSavedRecipes(): Flow<List<SavedRecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedRecipe(recipe: SavedRecipeEntity)

    @Query("DELETE FROM saved_recipes WHERE id = :id")
    suspend fun deleteSavedRecipeById(id: String)

    @Transaction
    suspend fun replaceBackupData(meals: List<MealEntity>, weights: List<WeightEntity>) {
        clearAllMeals()
        clearAllWeights()
        if (meals.isNotEmpty()) insertMeals(meals)
        if (weights.isNotEmpty()) insertWeights(weights)
    }
}
