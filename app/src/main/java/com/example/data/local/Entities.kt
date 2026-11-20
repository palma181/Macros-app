package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // YYYY-MM-DD
    val mealType: String,
    val name: String,
    val kcal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weights")
data class WeightEntity(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val kg: Double
)
