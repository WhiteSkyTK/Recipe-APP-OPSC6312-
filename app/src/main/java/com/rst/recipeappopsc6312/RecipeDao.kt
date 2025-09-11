package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    // --- Core Insert & Update Functions ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecipes(recipes: List<Recipe>)

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    // --- Core Get/Read Functions ---

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): Recipe?

    @Query("SELECT * FROM recipes WHERE id IN (:recipeIds)")
    fun getRecipesByIds(recipeIds: List<String>): LiveData<List<Recipe>>

    @Query("SELECT * FROM recipes ORDER BY title ASC")
    fun getAllRecipes(): Flow<List<Recipe>> // For reactive updates with Flow

    // NOTE: Renamed from 'getAllFavoriteRecipes' for clarity, as it gets ALL recipes.
    @Query("SELECT * FROM recipes")
    fun getAllFavoriteRecipes(): LiveData<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE userId = :userId")
    fun getUserRecipes(userId: String): Flow<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE mealType = :mealType AND isPublic = 1")
    suspend fun getRecipesByMealType(mealType: String): List<Recipe>

    // --- Favorite Functions ---

    @Query("SELECT * FROM recipes WHERE isFavorite = 1")
    fun getAllFavorites(): LiveData<List<Recipe>>

    @Query("SELECT isFavorite FROM recipes WHERE id = :recipeId")
    fun isFavorite(recipeId: String): LiveData<Boolean>

    @Query("SELECT isFavorite FROM recipes WHERE id = :recipeId")
    suspend fun isFavoriteNow(recipeId: String): Boolean?

    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :recipeId")
    suspend fun updateFavoriteStatus(recipeId: String, isFavorite: Boolean)

    @Query("SELECT id FROM recipes WHERE isFavorite = 1")
    suspend fun getFavoriteRecipeIds(): List<String>

    // --- Delete Functions ---

    @Delete
    suspend fun deleteRecipe(recipe: Recipe) // Merged 'delete' and 'deleteRecipe' as they were identical

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String) // Merged 'deleteById' and 'deleteRecipeById'

    @Query("DELETE FROM recipes WHERE userId = :userId")
    suspend fun deleteAllUserRecipes(userId: String)

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()

    // --- Utility Functions ---

    @Query("SELECT COUNT(id) FROM recipes")
    suspend fun getRecipeCount(): Int
}