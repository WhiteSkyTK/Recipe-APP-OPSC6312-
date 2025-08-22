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
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: Recipe)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllRecipes(recipes: List<Recipe>)

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Query("SELECT * FROM recipes WHERE id IN (:recipeIds)")
    fun getRecipesByIds(recipeIds: List<String>): LiveData<List<Recipe>>

    @Query("SELECT * FROM recipes WHERE id = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: String): Recipe?

    @Query("SELECT * FROM recipes WHERE userId = :userId ORDER BY title ASC")
    fun getUserRecipes(userId: String): Flow<List<Recipe>> // Observe user's recipes

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToFavorites(favorite: FavoriteRecipe)

    @Query("DELETE FROM favorite_recipes WHERE id = :recipeId")
    suspend fun removeFromFavorites(recipeId: String)

    @Query("SELECT * FROM favorite_recipes")
    fun getAllFavoriteIds(): LiveData<List<FavoriteRecipe>>



    @Query("SELECT COUNT(id) FROM recipes")
    suspend fun getRecipeCount(): Int

    @Delete
    suspend fun deleteRecipe(recipe: Recipe)

    // ++ THIS IS THE NEW QUERY for the FavoritesFragment ++
    @Query("SELECT * FROM recipes WHERE isFavorite = 1")
    fun getAllFavorites(): LiveData<List<Recipe>>

    // ++ THIS IS THE NEW QUERY for the RecipeDetailActivity heart icon ++
    @Query("SELECT isFavorite FROM recipes WHERE id = :recipeId")
    fun isFavorite(recipeId: String): LiveData<Boolean>

    // ++ THIS is for a quick, one-time check
    @Query("SELECT isFavorite FROM recipes WHERE id = :recipeId")
    suspend fun isFavoriteNow(recipeId: String): Boolean?

    // ++ ADD THIS function to update the favorite status
    @Query("UPDATE recipes SET isFavorite = :isFavorite WHERE id = :recipeId")
    suspend fun updateFavoriteStatus(recipeId: String, isFavorite: Boolean)

    @Query("SELECT * FROM favorite_recipes")
    suspend fun getAllFavoriteIdsNow(): List<FavoriteRecipe>

}

