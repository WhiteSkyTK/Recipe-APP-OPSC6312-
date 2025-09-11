package com.rst.recipeappopsc6312

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        Recipe::class,
        ShoppingList::class,
        ShoppingItem::class,
        ScanHistoryItem::class
    ],
    version = 9,
    exportSchema = false
) // Increment version on schema changes
@TypeConverters(
    IngredientListConverter::class,
    MethodStepListConverter::class,
    NutritionFactListConverter::class,
    StringListConverter::class,
    DateConverter::class )
abstract class AppDatabase : RoomDatabase() {

    abstract fun recipeDao(): RecipeDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "recipe_app_database"
                )
                    // Wipes and rebuilds instead of migrating if no Migration object.
                    .fallbackToDestructiveMigration() // Use migrations for production apps
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}