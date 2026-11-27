package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [MealEntity::class, WeightEntity::class, SavedRecipeEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun macroDao(): MacroDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `saved_recipes` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `prepMinutes` INTEGER NOT NULL,
                        `ingredientsJoined` TEXT NOT NULL,
                        `stepsJoined` TEXT NOT NULL,
                        `tips` TEXT NOT NULL,
                        `kcal` INTEGER NOT NULL,
                        `protein` INTEGER NOT NULL,
                        `carbs` INTEGER NOT NULL,
                        `fat` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mis_macros.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
