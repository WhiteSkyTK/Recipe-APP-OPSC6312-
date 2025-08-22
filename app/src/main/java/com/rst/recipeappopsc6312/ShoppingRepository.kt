package com.rst.recipeappopsc6312

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ShoppingRepository(
    private val shoppingDao: ShoppingDao,
    private val recipeDao: RecipeDao,
    private val scanHistoryDao: ScanHistoryDao,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val foodEmojis = listOf("🥞", "🍕", "🍔", "🍣", "🌮", "🥗", "🍜", "🍰", "🍩", "🥐", "🍝", "カレー", "🍲")

    private var lastVisibleRecipeDocument: com.google.firebase.firestore.DocumentSnapshot? = null

    private val apiService = RetrofitClient.instance

    // Get lists directly from Room. Room is the source of truth for the UI.
    fun getAllShoppingListsForUser(userId: String) = shoppingDao.getAllShoppingListsForUser(userId)
    fun getItemsForList(listId: String) = shoppingDao.getItemsForList(listId)
    fun getAllItemsForUser(userId: String) = shoppingDao.getAllItemsForUser(userId)

    suspend fun seedFirebaseDatabase() {
        // First, check if we even need to seed. If you have enough recipes, we can stop.
        val currentRecipeCount = firestore.collection("recipes").limit(200).get().await().size()
        if (currentRecipeCount >= 200) {
            Log.d("API_SEED", "Firestore has $currentRecipeCount recipes. No seeding needed.")
            return
        }

        Log.d("API_SEED", "Starting database seed with multiple API keys...")

        // 1. List all your API keys securely
        val apiKeys = listOf(
            BuildConfig.SPOONACULAR_API_KEY,
            BuildConfig.SPOONACULAR_API_KEY_1, // Assuming you've named them like this
            BuildConfig.SPOONACULAR_API_KEY_2,
            BuildConfig.SPOONACULAR_API_KEY_3,
            BuildConfig.SPOONACULAR_API_KEY_4,
            BuildConfig.SPOONACULAR_API_KEY_5
        )

        // 2. Define the types of recipes you want
        val tagsToFetch = listOf("breakfast", "lunch", "dinner", "snack", "dessert", "soup", "salad", "appetizer", "main course")
        val recipesToFetchPerTag = 10 // Fetch 10 of each type per key

        // 3. Loop through each key and each tag to get a wide variety
        for (key in apiKeys) {
            for (tag in tagsToFetch) {
                try {
                    val response = apiService.getRandomRecipes(key, recipesToFetchPerTag, tag)
                    val newRecipes = response.recipes.map { it.toAppRecipe() }

                    // 4. Save each new recipe to Firestore
                    newRecipes.forEach { recipe ->
                        // This 'set' operation will add the recipe or overwrite it if it already exists,
                        // which naturally handles duplicates.
                        firestore.collection("recipes").document(recipe.id).set(recipe)
                    }
                    Log.d("API_SEED", "Successfully fetched ${newRecipes.size} recipes for tag '$tag' with key ending in ${key.takeLast(4)}")
                } catch (e: Exception) {
                    Log.e("API_SEED", "Failed to fetch for tag '$tag' with key ending in ${key.takeLast(4)}. Error: ${e.message}")
                    // Continue to the next key/tag even if one fails
                }
            }
        }
        Log.d("API_SEED", "Database seeding process complete.")
    }

    suspend fun addItems(items: List<ShoppingItem>, listId: String, userId: String) {
        val itemsToInsert = mutableListOf<ShoppingItem>()

        // ++ CHECK FOR DUPLICATES ++
        // Loop through each item and only add it if a duplicate name isn't already in the list.
        for (item in items) {
            if (shoppingDao.itemExists(listId, item.name) == 0) {
                itemsToInsert.add(item)
            }
        }

        // Only save and sync if there are actually new items.
        if (itemsToInsert.isNotEmpty()) {
            shoppingDao.insertItems(itemsToInsert)
            // Sync to Firebase
            val listRef = firestore.collection("users").document(userId)
                .collection("shopping_lists").document(listId)
            itemsToInsert.forEach { item ->
                listRef.collection("items").document(item.itemId).set(item)
            }
        }
    }
    suspend fun insertItem(item: ShoppingItem, userId: String) {
        shoppingDao.insertItem(item) // Update local DB
        firestore.collection("users").document(userId)
            .collection("shopping_lists").document(item.ownerListId)
            .collection("items").document(item.itemId)
            .set(item)
    }

    suspend fun updateItem(item: ShoppingItem, userId: String) {
        shoppingDao.updateItem(item) // Update local DB
        firestore.collection("users").document(userId)
            .collection("shopping_lists").document(item.ownerListId)
            .collection("items").document(item.itemId)
            .set(item) // .set() will update or create
    }

    suspend fun deleteItems(items: List<ShoppingItem>, userId: String) {
        shoppingDao.deleteItems(items) // Update local DB
        items.forEach { item ->
            firestore.collection("users").document(userId)
                .collection("shopping_lists").document(item.ownerListId)
                .collection("items").document(item.itemId)
                .delete()
        }
    }

    // A function to pull data from Firebase and store it in Room
    suspend fun syncFirebaseToRoom(userId: String) {
        try {
            val listsSnapshot = firestore.collection("users").document(userId)
                .collection("shopping_lists").get().await()
            for (listDoc in listsSnapshot.documents) {
                val shoppingList = listDoc.toObject(ShoppingList::class.java)
                if (shoppingList != null) {
                    shoppingDao.insertShoppingList(shoppingList) // Save list to Room
                    val itemsSnapshot = listDoc.reference.collection("items").get().await()
                    val items = itemsSnapshot.toObjects(ShoppingItem::class.java)
                    shoppingDao.insertItems(items) // Save its items to Room
                }
            }
        } catch (e: Exception) {
            // Handle exceptions (e.g., no internet)
            Log.e("FirestoreSync", "Error syncing from Firebase: ${e.message}", e)
        }
    }

    suspend fun ensureListExists(listId: String, name: String, userId: String) {
        if (shoppingDao.getListById(listId) == null) {
            // Use a fixed emoji for "My List" for consistency
            val defaultList = ShoppingList(listId, name, userId, "📝")
            shoppingDao.insertShoppingList(defaultList)
        }
    }

    suspend fun deleteList(list: ShoppingList, userId: String) {
        // Delete from Room. CASCADE will handle deleting its items.
        shoppingDao.deleteList(list)

        // Delete from Firebase (document and its sub-collection of items)
        val listRef = firestore.collection("users").document(userId)
            .collection("shopping_lists").document(list.listId)

        // You must delete sub-collections manually in Firestore
        listRef.collection("items").get().await().forEach { itemDoc ->
            itemDoc.reference.delete()
        }
        listRef.delete()
    }

    suspend fun createNewListWithItems(title: String, ingredientNames: List<String>, userId: String) {
        // Check if a list with this name already exists
        val existingList = shoppingDao.getListByName(title, userId)
        val listId: String

        if (existingList != null) {
            // If it exists, use its ID
            listId = existingList.listId
        } else {
            // If not, create a new list and save it
            val newList = ShoppingList(name = title, userId = userId, emoji = foodEmojis.random())
            listId = newList.listId
            shoppingDao.insertShoppingList(newList)
            // Sync new list to Firebase
            firestore.collection("users").document(userId)
                .collection("shopping_lists").document(listId).set(newList)
        }

        // Now, add only the ingredients that don't already exist in that list
        val itemsToInsert = mutableListOf<ShoppingItem>()
        for (name in ingredientNames) {
            if (shoppingDao.itemExists(listId, name) == 0) {
                itemsToInsert.add(ShoppingItem(ownerListId = listId, name = name))
            }
        }

        if (itemsToInsert.isNotEmpty()) {
            shoppingDao.insertItems(itemsToInsert)
            // Sync new items to Firebase
            itemsToInsert.forEach { item ->
                firestore.collection("users").document(userId)
                    .collection("shopping_lists").document(listId)
                    .collection("items").document(item.itemId).set(item)
            }
        }
    }

    suspend fun getRecipeById(recipeId: String): Recipe? {
        // First, check Room for an offline copy
        val localRecipe = recipeDao.getRecipeById(recipeId)
        if (localRecipe != null) return localRecipe

        // If not in Room, get it from the Firestore cache
        return try {
            firestore.collection("recipes").document(recipeId).get().await()
                .toObject(Recipe::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveNewRecipe(recipe: Recipe, imageUri: Uri?) {
        var recipeToSave = recipe

        // 1. Save to Room immediately (without the final image URL)
        // This makes the UI feel instant for the user.
        recipeDao.insertRecipe(recipeToSave)

        // 2. If there's an image, upload it to Firebase Storage
        if (imageUri != null) {
            val userId = FirebaseManager.auth.currentUser?.uid ?: "unknown_user"
            val storageRef = storage.reference.child("recipe_images/$userId/${recipe.id}.jpg")

            // Upload the file and get the download URL
            val downloadUrl = storageRef.putFile(imageUri).await().storage.downloadUrl.await()
            recipeToSave = recipe.copy(imageUrl = downloadUrl.toString())

            // 3. Update the recipe in Room again, this time with the image URL
            recipeDao.insertRecipe(recipeToSave)
        }

        // 4. Finally, save the complete recipe to Firestore
        firestore.collection("recipes").document(recipeToSave.id).set(recipeToSave).await()
    }


    suspend fun getNotifications(): List<Notification> {
        return try {
            val documents = firestore.collection("notifications")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get().await()
            documents.toObjects(Notification::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting notifications", e)
            emptyList()
        }
    }

    fun getAllFavoriteIds(): LiveData<List<FavoriteRecipe>> = recipeDao.getAllFavoriteIds()
    fun getRecipesByIds(ids: List<String>): LiveData<List<Recipe>> = recipeDao.getRecipesByIds(ids)

    suspend fun getFeaturedRecipes(forceRefresh: Boolean = false): List<Recipe> {
        val publicRecipes = getPublicRecipes(forceRefresh)
        return publicRecipes.shuffled().take(5)
    }

    suspend fun getPublicRecipes(forceRefresh: Boolean = false): List<Recipe> {
        var cachedRecipesList: List<Recipe> = emptyList()
        try {
            // 1. Always get the cache first to use as a fallback if all API calls fail.
            val cachedRecipesQuery = firestore.collection("recipes")
                .whereEqualTo("public", true).limit(50).get().await()
            if (!cachedRecipesQuery.isEmpty) {
                cachedRecipesList = cachedRecipesQuery.toObjects(Recipe::class.java)
            }

            // 2. If we don't need to refresh and the cache is good, return it immediately.
            if (!forceRefresh && cachedRecipesList.size >= 20) {
                Log.d("API_FETCH", "Found ${cachedRecipesList.size} recipes in Firestore cache. Using them.")
                return cachedRecipesList
            }

            // 3. If a refresh is needed, prepare to cycle through all your API keys.
            Log.d("API_FETCH", "Cache is empty or refresh is forced. Attempting to fetch from Spoonacular API...")
            val apiKeys = listOf(
                BuildConfig.SPOONACULAR_API_KEY,
                BuildConfig.SPOONACULAR_API_KEY_1,
                BuildConfig.SPOONACULAR_API_KEY_2,
                BuildConfig.SPOONACULAR_API_KEY_3,
                BuildConfig.SPOONACULAR_API_KEY_4,
                BuildConfig.SPOONACULAR_API_KEY_5
            )

            for (key in apiKeys) {
                try {
                    val response = apiService.getRandomRecipes(key, 50, "main course,dessert,breakfast,lunch,snack,appetizer,soup,salad")
                    val newRecipes = response.recipes.map { it.toAppRecipe() }

                    if (newRecipes.isNotEmpty()) {
                        // If this key works, save the data and return it immediately.
                        newRecipes.forEach { recipe ->
                            firestore.collection("recipes").document(recipe.id).set(recipe)
                        }
                        Log.d("API_FETCH", "Successfully fetched ${newRecipes.size} recipes with key ...${key.takeLast(4)}")
                        return newRecipes
                    }
                } catch (e: Exception) {
                    // If a key fails (e.g., limit reached), log it and the loop will try the next key.
                    Log.w("API_FETCH", "API key ...${key.takeLast(4)} failed. Trying next key. Error: ${e.message}")
                }
            }

            // 4. If the loop finishes and we still haven't returned, it means all keys failed.
            //    In this case, we return the old cache so the user doesn't see a blank screen.
            Log.e("API_FETCH", "All API keys failed. Returning stale cache of size ${cachedRecipesList.size}.")
            return cachedRecipesList

        } catch (e: Exception) {
            Log.e("API_FETCH", "A critical error occurred in getPublicRecipes: ${e.message}")
            return cachedRecipesList // Final fallback
        }
    }

    suspend fun getBreakfastRecipes(): List<Recipe> = getPublicRecipesByMealType("Breakfast")
    suspend fun getLunchRecipes(): List<Recipe> = getPublicRecipesByMealType("Lunch")
    suspend fun getDinnerRecipes(): List<Recipe> = getPublicRecipesByMealType("Dinner")
    suspend fun getSnackRecipes(): List<Recipe> {
        val snacks = getPublicRecipesByMealType("Snack")
        val desserts = getPublicRecipesByMealType("Dessert")
        return (snacks + desserts).shuffled()
    }

    private suspend fun getPublicRecipesByMealType(mealType: String): List<Recipe> {
        return try {
            // This now fetches directly from your Firestore cache, not RoomDB.
            val documents = firestore.collection("recipes")
                .whereEqualTo("public", true)
                .whereEqualTo("mealType", mealType)
                .limit(10)
                .get().await()
            documents.toObjects(Recipe::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting recipes for mealType: $mealType", e)
            emptyList()
        }
    }

    private suspend fun getRecipesByMealType(mealType: String): List<Recipe> {
        return try {
            val documents = firestore.collection("recipes")
                .whereEqualTo("public", true)
                .whereEqualTo("mealType", mealType)
                .get().await()
            documents.toObjects(Recipe::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting recipes by meal type", e)
            emptyList()
        }
    }

    // This function to get user-created recipes is already correct!
    fun getUserRecipes(userId: String): Flow<List<Recipe>> = recipeDao.getUserRecipes(userId)

    suspend fun getAllCategories(forceRefresh: Boolean = false): List<Category> {
        val publicRecipes = getPublicRecipes(forceRefresh)
        val categories = publicRecipes.map { Category(it.category) }
            .distinctBy { it.name }.sortedBy { it.name }.toMutableList()
        categories.add(0, Category("All", isSelected = true))
        return categories
    }

    suspend fun getDiscoverRecipes(pageSize: Int): List<Recipe> {
        return try {
            var query = firestore.collection("recipes")
                .whereEqualTo("public", true).orderBy("title")
            if (lastVisibleRecipeDocument != null) {
                query = query.startAfter(lastVisibleRecipeDocument!!)
            }
            val documents = query.limit(pageSize.toLong()).get().await()
            if (documents.size() > 0) {
                lastVisibleRecipeDocument = documents.documents[documents.size() - 1]
            }
            documents.toObjects(Recipe::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting discover recipes", e)
            emptyList()
        }
    }

    suspend fun publishRecipe(recipe: Recipe) {
        val updatedRecipe = recipe.copy(isPublic = true)
        // Update both Room and Firestore
        recipeDao.insertRecipe(updatedRecipe)
        firestore.collection("recipes").document(recipe.id)
            .set(updatedRecipe).await()
    }

    suspend fun unpublishRecipe(recipe: Recipe) {
        val updatedRecipe = recipe.copy(isPublic = false)
        // Update both Room and Firestore
        recipeDao.insertRecipe(updatedRecipe)
        firestore.collection("recipes").document(recipe.id)
            .set(updatedRecipe).await()
    }

    suspend fun deleteRecipe(recipe: Recipe) {
        // Delete from Room first
        recipeDao.deleteRecipe(recipe)
        // Then delete from Firestore
        firestore.collection("recipes").document(recipe.id).delete().await()
    }

    fun getAllFavorites(): LiveData<List<Recipe>> = recipeDao.getAllFavorites()
    fun isFavorite(recipeId: String): LiveData<Boolean> = recipeDao.isFavorite(recipeId)
    suspend fun isFavoriteNow(recipeId: String): Boolean = recipeDao.isFavoriteNow(recipeId) ?: false

    suspend fun toggleFavorite(recipe: Recipe) {
        val isCurrentlyFavorite = isFavoriteNow(recipe.id)
        val newFavoriteState = !isCurrentlyFavorite

        // 1. Update the local Room database
        recipeDao.updateFavoriteStatus(recipe.id, newFavoriteState)

        // 2. Sync the change to Firestore
        try {
            firestore.collection("recipes").document(recipe.id)
                .update("favorite", newFavoriteState).await()
        } catch (e: Exception) {
            Log.w("Firestore", "Couldn't sync favorite status for recipe: ${recipe.id}", e)
        }
    }

    // ++ ADD THESE TRACKING FUNCTIONS ++
    fun logRecipeView(recipeId: String) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        val activity = UserActivity(type = "VIEW_RECIPE", value = recipeId)
        firestore.collection("users").document(userId)
            .collection("activity_log").add(activity)
    }

    fun logSearchQuery(query: String) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        val activity = UserActivity(type = "SEARCH_QUERY", value = query)
        firestore.collection("users").document(userId)
            .collection("activity_log").add(activity)
    }

    suspend fun getRecommendedForYou(): List<Recipe> {
        val userId = FirebaseManager.auth.currentUser?.uid
        if (userId == null) return getPublicRecipes() // Fallback for logged-out users

        // === Step 1: Gather all user preference and activity data ===
        val userProfileDoc = firestore.collection("users").document(userId).get().await()
        val userDiets = userProfileDoc.get("selected_diets") as? List<String> ?: emptyList()
        val userCuisines = userProfileDoc.get("selected_cuisines") as? List<String> ?: emptyList()

        val favoriteIds = recipeDao.getAllFavoriteIdsNow().map { it.id }.toSet()

        val recentActivity = firestore.collection("users").document(userId)
            .collection("activity_log").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50).get().await().toObjects(UserActivity::class.java)

        val viewedRecipes = recentActivity.filter { it.type == "VIEW_RECIPE" }
        val searchQueries = recentActivity.filter { it.type == "SEARCH_QUERY" }.map { it.value.lowercase() }


        // === Step 2: Build an efficient Firestore query with HARD filters (Diet) ===
        var query: com.google.firebase.firestore.Query = firestore.collection("recipes")
            .whereEqualTo("public", true)

        // Apply strict dietary filters

        if (userDiets.contains("Vegan")) query = query.whereEqualTo("vegan", true)
        if (userDiets.contains("Vegetarian")) query = query.whereEqualTo("vegetarian", true)
        if (userDiets.contains("Gluten-Free")) query = query.whereEqualTo("glutenFree", true)
        if (userDiets.contains("Dairy-Free")) query = query.whereEqualTo("dairyFree", true)
        if (userDiets.contains("Keto")) query = query.whereEqualTo("keto", true)
        if (userDiets.contains("Paleo")) query = query.whereEqualTo("paleo", true)
        if (userDiets.contains("Low-FODMAP")) query = query.whereEqualTo("lowFodmap", true)

        val candidateRecipes = try {
            query.limit(100).get().await().toObjects(Recipe::class.java) // Fetch a pool of 100 candidates
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting recommended candidates", e)
            return getPopularRecipes() // Fallback on error
        }

        var filteredRecipes = candidateRecipes
        if (userDiets.contains("Pescetarian")) {
            // Pescetarian: a vegetarian who eats fish. Exclude non-seafood meat.
            val redMeat = listOf("beef", "pork", "lamb", "steak")
            filteredRecipes = filteredRecipes.filter { recipe ->
                recipe.ingredients.none { ingredient -> redMeat.contains(ingredient.name.lowercase()) }
            }
        }

        // === Step 3: Score the remaining recipes based on SOFT preferences ===
        val scoredRecipes = filteredRecipes.associateWith { recipe ->
            var score = 0.0

            // Strongest boost for favorites
            if (favoriteIds.contains(recipe.id)) {
                score += 50.0
            }

            // Boost for recently viewed recipes, especially long views
            viewedRecipes.find { it.value == recipe.id }?.let {
                score += 10.0 // Base score for any view
                if ((it.durationSeconds ?: 0) > 30) {
                    score += 15.0 // Extra score for long views
                }
            }

            // Boost for matching user's preferred cuisines
            userCuisines.forEach { cuisine ->
                if (recipe.category.equals(cuisine, ignoreCase = true)) {
                    score += 25.0
                }
            }

            // Small boost for matching recent search terms
            searchQueries.forEach { query ->
                if (recipe.title.lowercase().contains(query)) {
                    score += 5.0
                }
            }

            // Small boost for generally popular recipes
            if (recipe.isPopular) {
                score += 5.0
            }

            score
        }

        // === Step 4: Sort the recipes by score and return the top results ===
        return scoredRecipes.toList()
            .sortedByDescending { (_, score) -> score }
            .map { (recipe, _) -> recipe }
            .take(20) // Return the top 20 recommended recipes
    }

    fun logRecipeView(recipeId: String, durationSeconds: Long) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        val activity = UserActivity(
            type = "VIEW_RECIPE",
            value = recipeId,
            durationSeconds = durationSeconds
        )
        firestore.collection("users").document(userId)
            .collection("activity_log").add(activity)
    }

    suspend fun getPopularRecipes(): List<Recipe> {
        return try {
            val documents = firestore.collection("recipes")
                .whereEqualTo("public", true)
                .whereEqualTo("popular", true) // This uses the 'isPopular' flag
                .limit(20)
                .get().await()
            documents.toObjects(Recipe::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting popular recipes", e)
            emptyList()
        }
    }

    suspend fun findRecipesByIngredients(userIngredients: List<String>): List<RecipeMatch> {
        // Convert user's ingredients to a lowercase set for efficient lookups
        val userIngredientSet = userIngredients.map { it.lowercase() }.toSet()

        // 1. Fetch all public recipes from your Firestore cache
        val allPublicRecipes = getPublicRecipes()

        // 2. For each recipe, calculate the missing ingredients
        val matches = allPublicRecipes.mapNotNull { recipe ->
            val requiredIngredients = recipe.ingredients.map { it.name.lowercase() }.toSet()

            // Find which ingredients are in the recipe but not in the user's list
            val missing = requiredIngredients.filter { !userIngredientSet.contains(it) }

            // We can set a threshold, e.g., don't show recipes with more than 5 missing ingredients
            if (missing.size <= 5) {
                RecipeMatch(recipe = recipe, missingIngredients = missing)
            } else {
                null // This recipe has too many missing ingredients, so we discard it
            }
        }

        // 3. Sort the results: first by the number of missing ingredients (fewest first),
        //    then alphabetically by title.
        return matches.sortedWith(compareBy({ it.missingIngredients.size }, { it.recipe.title }))
    }

    // ++ ADD THESE SCAN HISTORY FUNCTIONS ++
    fun getScanHistory(): LiveData<List<ScanHistoryItem>> = scanHistoryDao.getScanHistory()
    suspend fun saveScanToHistory(historyItem: ScanHistoryItem) = scanHistoryDao.insert(historyItem)


    /*
    private suspend fun searchRecipesOnTheMealDB(query: String): List<Recipe> {
        return try {
            val response = mealDbApiService.searchByName(query)
            // Map the response to your app's Recipe format
            response.meals?.map { it.toAppRecipe() } ?: emptyList()
        } catch (e: Exception) {
            Log.e("API_FETCH", "Error searching TheMealDB for '$query'", e)
            emptyList()
        }
    }

     */

    /*
    suspend fun backfillCacheFromTheMealDB() {
        try {
            Log.d("API_FETCH", "Backfilling cache from TheMealDB...")
            // Fetch 10 random recipes from TheMealDB
            for (i in 1..10) {
                val response = mealDbApiService.getRandomRecipe()
                response.meals?.firstOrNull()?.let { meal ->
                    val recipe = meal.toAppRecipe()
                    // Save the mapped recipe to your Firestore cache
                    firestore.collection("recipes").document(recipe.id).set(recipe)
                }
            }
            Log.d("API_FETCH", "TheMealDB backfill complete.")
        } catch (e: Exception) {
            Log.e("API_FETCH", "Error backfilling from TheMealDB", e)
        }
    }

     */
}