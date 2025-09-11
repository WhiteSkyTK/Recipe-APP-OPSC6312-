package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow // For reactive updates

@Dao
interface RecipeDao {

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipes WHERE id IN (:recipeIds)")
    fun getRecipesByIds(recipeIds: List<String>): LiveData<List<Recipe>>


    @Query("SELECT * FROM recipes ORDER BY title ASC")
    fun getAllRecipes(): Flow<List<Recipe>> // Observe all recipes

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteRecipeById(recipeId: String)

    @Query("DELETE FROM recipes WHERE userId = :userId")
    suspend fun deleteAllUserRecipes(userId: String)

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()


    @Delete
    suspend fun delete(recipe: Recipe) // Or delete by ID

    @Query("DELETE FROM recipes WHERE id = :recipeId")
    suspend fun deleteById(recipeId: String)

    @Query("SELECT * FROM recipes") // To get all favorites
    fun getAllFavoriteRecipes(): LiveData<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE mealType = :mealType AND isPublic = 1")
    suspend fun getRecipesByMealType(mealType: String): List<Recipe>


    @Query("SELECT COUNT(id) FROM recipes")
    suspend fun getRecipeCount(): Int

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    // ++ THIS is for a quick, one-time check
    @Query("SELECT isFavorite FROM recipes WHERE id = :recipeId")
    suspend fun isFavoriteNow(recipeId: String): Boolean?

    // --- Core Recipe Functions ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecipes(recipes: List<Recipe>)

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): Recipe?

    @Query("SELECT * FROM recipes WHERE userId = :userId")
    fun getUserRecipes(userId: String): Flow<List<Recipe>>

    // --- Favorite Functions ---

    // Gets all recipes that are marked as a favorite
    @Query("SELECT * FROM recipes WHERE isFavorite = 1")
    fun getAllFavorites(): LiveData<List<Recipe>>

    // Observes the favorite status of a single recipe
    @Query("SELECT isFavorite FROM recipes WHERE id = :recipeId")
    fun isFavorite(recipeId: String): LiveData<Boolean>

    // Updates the favorite status for a specific recipe
    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :recipeId")
    suspend fun updateFavoriteStatus(recipeId: String, isFavorite: Boolean)

    @Query("SELECT id FROM recipes WHERE isFavorite = 1")
    suspend fun getFavoriteRecipeIds(): List<String>
}

