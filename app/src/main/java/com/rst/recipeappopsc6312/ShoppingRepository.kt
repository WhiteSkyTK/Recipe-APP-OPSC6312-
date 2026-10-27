package com.rst.recipeappopsc6312

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar

class ShoppingRepository(
    private val shoppingDao: ShoppingDao,
    private val recipeDao: RecipeDao,
    private val scanHistoryDao: ScanHistoryDao,
    val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {
    private val foodEmojis = listOf("🥞", "🍕", "🍔", "🍣", "🌮", "🥗", "🍜", "🍰", "🍩", "🥐", "🍝", "🍛", "🍲")
    private val apiService = RetrofitClient.instance
    private val tastyApiService = RetrofitClient.tastyInstance

    private val gamificationEngine = GamificationEngine(firestore, this)
    private val lastDocumentSnapshots = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot?>()

    // Get lists directly from Room. Room is the source of truth for the UI.
    fun getAllShoppingListsForUser(userId: String) = shoppingDao.getAllShoppingListsForUser(userId)
    fun getItemsForList(listId: String) = shoppingDao.getItemsForList(listId)
    fun getAllItemsForUser(userId: String) = shoppingDao.getAllItemsForUser(userId)
    fun getAllBadges(): List<Badge> {
        return gamificationEngine.allBadges
    }

    private var cachedFeatured: List<Recipe>? = null
    private var cachedRecommended: List<Recipe>? = null
    private var cachedCategories: List<Category>? = null
    private var cachedBreakfast: List<Recipe>? = null
    private var cachedLunch: List<Recipe>? = null
    private var cachedSnack: List<Recipe>? = null
    private var cachedDinner: List<Recipe>? = null

    suspend fun preloadHomeScreenData(forceRefresh: Boolean = false) = coroutineScope {
        if (forceRefresh) {
            cachedFeatured = null
            cachedRecommended = null
            cachedCategories = null
            cachedBreakfast = null
            cachedLunch = null
            cachedSnack = null
            cachedDinner = null
        }

        // We use async to run all these network calls at the same time
        val featuredJob = async { getFeaturedRecipes(forceRefresh) }
        val recommendedJob = async { getRecommendedForYou(forceRefresh) }
        val categoriesJob = async { getAllCategories(forceRefresh) }
        val breakfastJob = async { getBreakfastRecipes(forceRefresh) }
        val lunchJob = async { getLunchRecipes(forceRefresh) }
        val snackJob = async { getSnackRecipes(forceRefresh) }
        val dinnerJob = async { getDinnerRecipes(forceRefresh) }

        // Await all jobs to ensure data is ready
        listOf(featuredJob, recommendedJob, categoriesJob, breakfastJob, lunchJob, snackJob, dinnerJob).forEach { it.await() }
    }

    // Database seeding used to run on every app launch: it downloaded the whole recipes
    // collection just to count it and could then make 630 Spoonacular calls (9 keys x 70 tags).
    // It has been removed; the APIs are now only used when a search finds nothing in Firestore.

    // Blank or missing keys arrive from BuildConfig as "" or "null".
    private val spoonacularApiKeys = listOf(
        BuildConfig.SPOONACULAR_API_KEY,
        BuildConfig.SPOONACULAR_API_KEY_1,
        BuildConfig.SPOONACULAR_API_KEY_2,
        BuildConfig.SPOONACULAR_API_KEY_3,
        BuildConfig.SPOONACULAR_API_KEY_4,
        BuildConfig.SPOONACULAR_API_KEY_5,
        BuildConfig.SPOONACULAR_API_KEY_6
    ).filter { it.isNotBlank() && it != "null" }.distinct()

    /** Tries up to three Spoonacular keys, moving on when one is rejected or out of quota. */
    private suspend fun <T> withSpoonacularKey(call: suspend (String) -> T): T? {
        for (key in spoonacularApiKeys.shuffled().take(3)) {
            try {
                return call(key)
            } catch (e: Exception) {
                Log.w("API_FETCH", "Spoonacular call failed with key ending ${key.takeLast(4)}: ${e.message}")
            }
        }
        return null
    }

    fun clearCaches() {
        cachedFeatured = null
        cachedRecommended = null
        cachedCategories = null
        cachedBreakfast = null
        cachedLunch = null
        cachedSnack = null
        cachedDinner = null
        resetPagination()
    }

    suspend fun getPublicRecipes(forceRefresh: Boolean = false, limit: Long = 100): List<Recipe> {
        try {
            val cachedRecipesQuery = firestore.collection("recipes")
                .whereEqualTo("public", true)
                .limit(limit) // Use the new limit parameter here
                .get().await()
            val cachedRecipes = cachedRecipesQuery.toObjects(Recipe::class.java)
            Log.d("DataFlow", "Step 1 (Repo/Public): Firestore query for public recipes returned ${cachedRecipes.size} documents.")

            if (!forceRefresh && cachedRecipes.isNotEmpty()) {
                Log.d("API_FETCH", "Found ${cachedRecipes.size} recipes in cache. Using them.")
                return cachedRecipes
            }

            // Only when the collection is empty (e.g. a brand-new Firebase project).
            // fetchNewRecipesFromApis() already stores what it fetches; the old code then
            // uploaded every image to Cloudinary a second time.
            val newRecipes = fetchNewRecipesFromApis()
            return (newRecipes + cachedRecipes).distinctBy { it.id }.shuffled()

        } catch (e: FirebaseFirestoreException) {
            // ++ THIS IS THE FIX for offline crashes ++
            Log.w("Firestore", "Failed to get public recipes (likely offline): ${e.message}")
            // If we're offline, return an empty list. The UI will handle showing the offline message.
            return emptyList()
        }
    }

    /**
     * Fetches a starter set from Spoonacular and Tasty and stores it in Firestore.
     * One tag per request: Spoonacular's include-tags requires *all* listed tags, so the old
     * comma-joined list of seven tags usually returned nothing (the same for Tasty's search).
     */
    private suspend fun fetchNewRecipesFromApis(): List<Recipe> {
        val userId = FirebaseManager.auth.currentUser?.uid
        val userCuisines = if (userId == null) emptyList() else try {
            @Suppress("UNCHECKED_CAST")
            firestore.collection("users").document(userId).get().await().get("selected_cuisines") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        val tags = (listOf("african") + userCuisines.map { it.lowercase() } + listOf("dinner", "breakfast")).distinct().take(3)

        val fetched = coroutineScope {
            tags.map { tag ->
                async {
                    val spoonacular = withSpoonacularKey { key ->
                        apiService.getRandomRecipes(key, 10, tag).recipes.mapNotNull { runCatching { it.toAppRecipe() }.getOrNull() }
                    } ?: emptyList()
                    val tasty = try {
                        tastyApiService.listRecipes(0, 10, tag).results.mapNotNull { runCatching { it.toAppRecipe() }.getOrNull() }
                    } catch (e: Exception) {
                        Log.e("API_FETCH", "Tasty refresh failed: ${e.message}")
                        emptyList()
                    }
                    spoonacular + tasty
                }
            }.awaitAll().flatten()
        }
        // Tasty "compilations" have no ingredients or steps; do not store them as recipes.
        val unique = fetched.distinctBy { it.id }.filter { it.ingredients.isNotEmpty() || it.method.isNotEmpty() }
        cacheRecipes(unique)
        return unique
    }

    /** Saves API results to Firestore in one batch so other users find them without an API call. */
    private suspend fun cacheRecipes(recipes: List<Recipe>) {
        if (recipes.isEmpty()) return
        try {
            val batch = firestore.batch()
            recipes.take(450).forEach { batch.set(firestore.collection("recipes").document(it.id), it) }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("API_FETCH", "Caching API recipes failed: ${e.message}")
        }
    }

    // --- Gamification Triggers ---
    suspend fun getUserProgress(userId: String): UserProgress? {
        return try {
            firestore.collection("users").document(userId)
                .collection("progress").document("main")
                .get().await().toObject(UserProgress::class.java)
        } catch (e: Exception) {
            Log.e("Gamification", "Failed to get user progress for $userId", e)
            null // Return null if the document doesn't exist or an error occurs
        }
    }

    suspend fun logUserLogin(userId: String) {
        val progressRef = firestore.collection("users").document(userId).collection("progress").document("main")
        val progressDoc = progressRef.get().await()
        val progress = progressDoc.toObject(UserProgress::class.java) ?: UserProgress(userId = userId)

        // Counted in calendar days; comparing day-of-year numbers reset the streak every 1 January.
        val newStreak = LoginStreak.next(progress.loginStreak, progress.lastLogin)

        val updatedProgress = mapOf(
            "loginStreak" to newStreak,
            "lastLogin" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        // Use .set with merge to create the document if it doesn't exist
        progressRef.set(updatedProgress, com.google.firebase.firestore.SetOptions.merge()).await()

        // After updating progress, check for new badges
        gamificationEngine.checkAndAwardBadges(userId)
    }

    suspend fun logRecipeCreated(userId: String) {
        val progressRef = firestore.collection("users").document(userId).collection("progress").document("main")
        progressRef.set(mapOf("recipesCreated" to com.google.firebase.firestore.FieldValue.increment(1)), com.google.firebase.firestore.SetOptions.merge()).await()
        gamificationEngine.checkAndAwardBadges(userId)
    }

    suspend fun logFavoriteToggled(userId: String, isFavoriting: Boolean) {
        val progressRef = firestore.collection("users").document(userId).collection("progress").document("main")
        val increment = if (isFavoriting) 1L else -1L
        progressRef.set(mapOf("recipesFavorited" to com.google.firebase.firestore.FieldValue.increment(increment)), com.google.firebase.firestore.SetOptions.merge()).await()
        gamificationEngine.checkAndAwardBadges(userId)
    }

    fun resetPagination() {
        lastDocumentSnapshots.clear()
    }

    suspend fun getRecommendedForYou(forceRefresh: Boolean = false, category: String? = null): List<Recipe> {
        if (category == null && cachedRecommended != null && !forceRefresh) return cachedRecommended!!
        val userId = FirebaseManager.auth.currentUser?.uid
        if (userId == null) return getPopularRecipes(category)

        // === Step 1: Gather all user preference and activity data ===
        val userProfileDoc = firestore.collection("users").document(userId).get().await()
        val userDiets = userProfileDoc.get("selected_diets") as? List<String> ?: emptyList()
        val userCuisines = userProfileDoc.get("selected_cuisines") as? List<String> ?: emptyList()

        val favoritesSnapshot = firestore.collection("users").document(userId)
            .collection("favorites").get().await()
        val favoriteIds = favoritesSnapshot.documents.map { it.id }.toSet()

        val recentActivity = firestore.collection("users").document(userId)
            .collection("activity_log").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50).get().await().toObjects(UserActivity::class.java)

        val viewedRecipes = recentActivity.filter { it.type == "VIEW_RECIPE" }
        val searchQueries = recentActivity.filter { it.type == "SEARCH_QUERY" }.map { it.value.lowercase() }


        // === Step 2: Gather candidates ===
        // Recipes written by users have no diet flags, so a flag-only query would hide all of
        // them from anyone with a diet. Take general public recipes plus flag-matched ones and
        // let DietFilter decide.
        var baseQuery: com.google.firebase.firestore.Query = firestore.collection("recipes")
            .whereEqualTo("public", true)
        if (category != null && category != "All") {
            baseQuery = baseQuery.whereEqualTo("category", category)
        }
        val flagFields = mapOf(
            "Vegan" to "vegan", "Vegetarian" to "vegetarian", "Gluten-Free" to "glutenFree",
            "Dairy-Free" to "dairyFree", "Keto" to "keto", "Paleo" to "paleo", "Low-FODMAP" to "lowFodmap"
        )
        val candidateRecipes = try {
            val general = baseQuery.limit(150).get().await().toObjects(Recipe::class.java)
            val fields = userDiets.mapNotNull { flagFields[it] }
            val flagged = if (fields.isEmpty()) emptyList() else {
                var flagQuery = baseQuery
                fields.forEach { flagQuery = flagQuery.whereEqualTo(it, true) }
                flagQuery.limit(100).get().await().toObjects(Recipe::class.java)
            }
            (general + flagged).distinctBy { it.id }
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting recommended candidates", e)
            return getPopularRecipes(category) // Fallback on error
        }

        // === Step 3: Keep only recipes that fit every selected diet ===
        val filteredRecipes = DietFilter.filter(candidateRecipes, userDiets)

        // === Step 4: Score the remaining recipes based on SOFT preferences ===
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

        // === Step 5: Sort the recipes by score and return the top results ===
        val recommended = scoredRecipes.toList()
            .sortedByDescending { (_, score) -> score }
            .map { (recipe, _) -> recipe }
            .take(20)

        val result = if (recommended.isNotEmpty()) recommended else getPopularRecipes(category)

        if (category == null) {
            cachedRecommended = result // Only cache the main "All" recommendations
        }
        return result
    }

    suspend fun getPopularRecipes(category: String? = null): List<Recipe> {
        return try {
            var query: com.google.firebase.firestore.Query = firestore.collection("recipes")
                .whereEqualTo("public", true)
                .whereEqualTo("popular", true) // Kotlin's isPopular is stored as "popular"

            // Apply the category filter if one is provided
            if (category != null && category != "All") {
                query = query.whereEqualTo("category", category)
            }

            val documents = query.limit(20).get().await()
            val popularRecipes = documents.toObjects(Recipe::class.java)

            // If we still get no results (e.g., no popular recipes in that category), fall back to random.
            if (popularRecipes.isNotEmpty()) {
                popularRecipes
            } else {
                getPublicRecipes(forceRefresh = false).shuffled().take(20)
            }
        } catch (e: Exception) {
            getPublicRecipes(forceRefresh = false).shuffled().take(20)
        }
    }

    private fun applySoftDietFilters(recipes: List<Recipe>, userDiets: List<String>): List<Recipe> =
        DietFilter.filter(recipes, userDiets)

    suspend fun searchRecipes(queryText: String, userDiets: List<String>): List<Recipe> {
        logActivity("SEARCH_QUERY", queryText.trim().lowercase())
        // 1. Fetch public recipes from the cache
        val allCachedRecipes = getPublicRecipes(forceRefresh = false, limit = 300)

        // 2. Filter the cached recipes by the search query
        var localResults = allCachedRecipes.filter {
            it.title.contains(queryText, ignoreCase = true) || it.ingredients.any { i -> i.name.contains(queryText, ignoreCase = true)}
        }

        // 3. Apply dietary filters to the local results
        localResults = applySoftDietFilters(localResults, userDiets)

        // 4. If the cache search finds results, return them. Otherwise, call the APIs.
        return if (localResults.isNotEmpty()) {
            localResults
        } else {
            searchRecipesFromApis(queryText)
        }
    }

    /**
     * Asks Spoonacular and Tasty when nothing in Firestore matches, then stores the results.
     * complexSearch only returns IDs and titles, so full recipes come from informationBulk.
     * (Decoding complexSearch results as full recipes made every Spoonacular search fail.)
     */
    suspend fun searchRecipesFromApis(query: String): List<Recipe> {
        Log.d("API_SEARCH", "No results in cache for '$query'. Searching APIs...")
        val results = coroutineScope {
            val spoonacular = async {
                withSpoonacularKey { key ->
                    val ids = apiService.searchRecipes(key, query, 10).results.map { it.id }
                    if (ids.isEmpty()) emptyList()
                    else apiService.getRecipeInformationBulk(key, ids.joinToString(","))
                        .mapNotNull { runCatching { it.toAppRecipe() }.getOrNull() }
                } ?: emptyList()
            }
            val tasty = async {
                try {
                    tastyApiService.listRecipes(0, 10, query).results.mapNotNull { runCatching { it.toAppRecipe() }.getOrNull() }
                } catch (e: Exception) {
                    Log.e("API_SEARCH", "Tasty search failed: ${e.message}")
                    emptyList()
                }
            }
            spoonacular.await() + tasty.await()
        }
        val newRecipes = results.distinctBy { it.id }.filter { it.ingredients.isNotEmpty() || it.method.isNotEmpty() }
        cacheRecipes(newRecipes)
        return newRecipes
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

    suspend fun ensureListExists(listId: String, name: String, userId: String) {
        if (shoppingDao.getListById(listId) == null) {
            // Use a fixed emoji for "My List" for consistency
            val defaultList = ShoppingList(listId, name, userId, "📝")
            shoppingDao.insertShoppingList(defaultList)
        }
        // Write the list document as well; only its items used to be stored in Firestore, so
        // syncShoppingDataFromFirebase never found "My List" on a new device.
        try {
            firestore.collection("users").document(userId)
                .collection("shopping_lists").document(listId)
                .set(ShoppingList(listId, name, userId, "📝"), com.google.firebase.firestore.SetOptions.merge())
        } catch (e: Exception) {
            Log.w("ShoppingRepo", "Could not save $name to Firestore: ${e.message}")
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
            // ++ START: DEBUGGING STEP ++
            // Verify the parent list exists before inserting items
            val parentListExists = shoppingDao.getListById(listId) != null
            if (!parentListExists) {
                // Log an error if the parent list doesn't exist in the database
                Log.e("ShoppingRepo", "CRITICAL ERROR: Parent ShoppingList with ID $listId NOT FOUND in Room DB before inserting items! Aborting item insert.")
                // Stop this function here to prevent the crash
                return
            }
            // Log success if the parent list is found
            Log.d("ShoppingRepo", "Parent list $listId confirmed to exist in Room DB. Proceeding with item insert.")
            // ++ END: DEBUGGING STEP ++
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
            Log.e("DataFlow", "Could not find recipe $recipeId in cache.", e)
            null
        }
    }

    suspend fun saveNewRecipe(recipe: Recipe, imageUri: Uri?, isNew: Boolean = true) {
        var recipeToSave = recipe
        recipeDao.insertRecipe(recipeToSave)
        // Only local images (content:// or file://) are uploaded. An existing https:// photo is
        // kept as is; passing it to putFile made every edit of a recipe with a photo fail.
        if (imageUri != null && imageUri.scheme != "http" && imageUri.scheme != "https") {
            val userId = FirebaseManager.auth.currentUser?.uid ?: "unknown_user"
            val storageRef = storage.reference.child("recipe_images/$userId/${recipe.id}.jpg")
            val downloadUrl = storageRef.putFile(imageUri).await().storage.downloadUrl.await()
            recipeToSave = recipe.copy(imageUrl = downloadUrl.toString())
            recipeDao.insertRecipe(recipeToSave)
        }
        firestore.collection("recipes").document(recipeToSave.id).set(recipeToSave).await()

        // Only new recipes count towards badges (edits used to count as new recipes).
        if (isNew) logRecipeCreated(recipeToSave.userId)
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

    suspend fun getFeaturedRecipes(forceRefresh: Boolean = false): List<Recipe> {
        if (cachedFeatured != null && !forceRefresh) return cachedFeatured!!

        var finalFeaturedList: List<Recipe>

        try {
            // STEP 1: PRIMARY PLAN - Try to fetch popular recipes.
            val popularDocs = firestore.collection("recipes")
                .whereEqualTo("public", true)
                .whereEqualTo("popular", true) // Kotlin's isPopular is stored as "popular"
                .limit(10).get().await()
            val popularRecipes = popularDocs.toObjects(Recipe::class.java)

            if (popularRecipes.isNotEmpty()) {
                // SUCCESS: We found popular recipes.
                Log.d("FeaturedLogic", "Successfully fetched ${popularRecipes.size} popular recipes.")
                finalFeaturedList = popularRecipes.shuffled()
            } else {
                // STEP 2: ROBUST BACKUP PLAN
                Log.w("FeaturedLogic", "No popular recipes found. Using smart random fallback.")
                // First, get a large pool of ANY public recipes.
                val randomPool = getPublicRecipes(forceRefresh = false, limit = 50)
                // Second, determine the current meal type.
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val desiredMealType = when (hour) {
                    in 5..10 -> "Breakfast"
                    in 11..13 -> "Lunch"
                    in 14..17 -> "Snack"
                    in 18..21 -> "Dinner"
                    else -> "Snack"
                }

                // Third, TRY to filter the random pool for time-appropriate recipes.
                val timeBasedList = randomPool.filter { it.mealType.equals(desiredMealType, ignoreCase = true) }
                // FINALLY, decide what to show.
                finalFeaturedList = if (timeBasedList.size >= 5) {
                    timeBasedList.shuffled().take(5)
                } else {
                    randomPool.shuffled().take(5)
                }
            }
        } catch (e: Exception) {
            // EMERGENCY BACKUP: The entire request failed.
            Log.e("FeaturedLogic", "Error fetching featured recipes. Using generic random fallback.", e)
            finalFeaturedList = getPublicRecipes(forceRefresh = false, limit = 5).shuffled()
        }

        cachedFeatured = finalFeaturedList
        return finalFeaturedList
    }

    suspend fun getBreakfastRecipes(forceRefresh: Boolean = false): List<Recipe> {
        if (cachedBreakfast != null && !forceRefresh) return cachedBreakfast!!
        val result = getPublicRecipesByMealType("Breakfast")
        cachedBreakfast = result
        return result
    }
    suspend fun getLunchRecipes(forceRefresh: Boolean = false): List<Recipe> {
        if (cachedLunch != null && !forceRefresh) return cachedLunch!!
        val result = getPublicRecipesByMealType("Lunch")
        cachedLunch = result
        return result
    }

    suspend fun getDinnerRecipes(forceRefresh: Boolean = false): List<Recipe> {
        if (cachedDinner != null && !forceRefresh) return cachedDinner!!
        val result = getPublicRecipesByMealType("Dinner")
        cachedDinner = result
        return result
    }

    suspend fun getSnackRecipes(forceRefresh: Boolean = false): List<Recipe> {
        if (cachedSnack != null && !forceRefresh) return cachedSnack!!
        val snacks = getPublicRecipesByMealType("Snack")
        val desserts = getPublicRecipesByMealType("Dessert")
        val result = (snacks + desserts).shuffled()
        cachedSnack = result
        return result
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

    suspend fun getAllCategories(forceRefresh: Boolean = false): List<Category> {
        if (cachedCategories != null && !forceRefresh) return cachedCategories!!

        return try {
            // ++ THIS IS THE FIX: We fetch the documents, then manually extract only the 'category' field. ++
            val documents = firestore.collection("recipes")
                .whereEqualTo("public", true)
                .limit(300) // Used to download every public recipe just to list categories.
                .get().await()

            val categories = documents.documents // Iterate through the document snapshots
                .mapNotNull { it.getString("category") } // Safely get the category string from each document
                .filter { it.isNotBlank() } // Safely ignore any recipes with no category
                .distinct()
                .sorted()
                .map { Category(it) }
                .toMutableList()

            categories.add(0, Category("All", isSelected = true))
            cachedCategories = categories
            categories
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting all categories (likely offline)", e)
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
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        val favoriteRef = firestore.collection("users").document(userId).collection("favorites").document(recipe.id)
        val isCurrentlyFavorite = isFavoriteNow(recipe.id)
        val newFavoriteState = !isCurrentlyFavorite

        if (newFavoriteState) {
            favoriteRef.set(recipe).await()
            recipeDao.insertRecipe(recipe.copy(isFavorite = true))
            logActivity("FAVORITE_RECIPE", recipe.id)
            // ++ TRIGGER for favoriting a recipe ++
            logFavoriteToggled(userId, true)
        } else {
            favoriteRef.delete().await()
            logActivity("UNFAVORITE_RECIPE", recipe.id)
            // ++ TRIGGER for un-favoriting a recipe ++
            logFavoriteToggled(userId, false)
        }
        recipeDao.updateFavoriteStatus(recipe.id, newFavoriteState)
    }

    /**
     * Marks all of a user's unread notifications as read in Firestore.
     */
    suspend fun markNotificationsAsRead(userId: String, notificationIds: List<String>) {
        try {
            val notificationsRef = firestore.collection("users").document(userId).collection("notifications")
            val unreadNotifications = notificationsRef.whereEqualTo("isRead", false).get().await()

            if (unreadNotifications.isEmpty) {
                Log.d("Notifications", "No unread notifications to mark as read for user $userId.")
                return
            }

            val batch = firestore.batch()
            for (doc in unreadNotifications.documents) {
                batch.update(doc.reference, mapOf("isRead" to true, "read" to true))
            }
            batch.commit().await()
            Log.d("Notifications", "Marked ${unreadNotifications.size()} notifications as read for user $userId.")
        } catch (e: Exception) {
            Log.e("Notifications", "Error marking notifications as read for user $userId", e)
        }
    }

    // --- Notification Subscription ---
    fun updateNotificationSubscription(isEnabled: Boolean) {
        if (isEnabled) {
            FirebaseMessaging.getInstance().subscribeToTopic("new_recipes")
                .addOnSuccessListener { Log.d("FCM", "Subscribed to new_recipes topic") }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("new_recipes")
                .addOnSuccessListener { Log.d("FCM", "Unsubscribed from new_recipes topic") }
        }
    }


    private fun logActivity(type: String, value: String) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        val activity = UserActivity(type = type, value = value)
        firestore.collection("users").document(userId)
            .collection("activity_log").add(activity)
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

    /**
     * Recipes the user can (nearly) make. Matching is by word, so "chicken" covers
     * "chicken breast"; recipes the user has none of the ingredients for are left out.
     */
    suspend fun findRecipesByIngredients(userIngredients: List<String>): List<RecipeMatch> =
        IngredientMatcher.matches(userIngredients, getPublicRecipes(limit = 300))

    // ++ ADD THESE SCAN HISTORY FUNCTIONS ++
    fun getScanHistory(): LiveData<List<ScanHistoryItem>> = scanHistoryDao.getScanHistory()
    suspend fun saveScanToHistory(historyItem: ScanHistoryItem) = scanHistoryDao.insert(historyItem)

    suspend fun getDiscoverPage(sortOption: String, pageSize: Int, userDiets: List<String>): List<Recipe> {
        if (sortOption == "Recommended") return getRecommendedForYou()

        return try {
            var query: com.google.firebase.firestore.Query = firestore.collection("recipes").whereEqualTo("public", true)

            // Diets are applied after loading by DietFilter (step 4). The old
            // whereArrayContainsAny("dietTags") matched recipes with *any* of the user's diets and
            // returned nothing for Halal, Kosher, Nut Allergy, Pescetarian or Low-Carb.

            // Step 2: Apply Sorting
            when (sortOption) {
                "Popular" -> query = query.orderBy("popular", com.google.firebase.firestore.Query.Direction.DESCENDING)
                "Cook Time" -> query = query.orderBy("timeInMins", com.google.firebase.firestore.Query.Direction.ASCENDING)
                "A-Z" -> query = query.orderBy("title", com.google.firebase.firestore.Query.Direction.ASCENDING)
                "Z-A" -> query = query.orderBy("title", com.google.firebase.firestore.Query.Direction.DESCENDING)
            }

            // Step 3: Handle Pagination
            val lastDoc = lastDocumentSnapshots[sortOption]
            if (lastDoc != null) {
                query = query.startAfter(lastDoc)
            }
            val documents = query.limit(pageSize.toLong()).get().await()
            if (documents.size() > 0) {
                lastDocumentSnapshots[sortOption] = documents.documents[documents.size() - 1]
            }

            val candidateRecipes = documents.toObjects(Recipe::class.java)

            // Step 4: Apply SOFT keyword-based filters
            return applySoftDietFilters(candidateRecipes, userDiets)

        } catch (e: Exception) {
            Log.e("Firestore", "Error getting discover page for sort: $sortOption", e)
            emptyList()
        }
    }

    suspend fun getUserDiets(userId: String): List<String> {
        return try {
            val userProfileDoc = firestore.collection("users").document(userId).get().await()
            userProfileDoc.get("selected_diets") as? List<String> ?: emptyList()
        } catch (e: Exception) {
            Log.e("Firestore", "Error fetching user diets for $userId", e)
            emptyList()
        }
    }

    // ++ ADD THIS FUNCTION to create the default profile for SSO users ++
    suspend fun createDefaultUserProfile(user: com.google.firebase.auth.FirebaseUser, cuisineNames: List<String>) { // Pass the names
        val userProfile = mapOf(
            "full_name" to user.displayName,
            "email" to user.email,
            "username" to (user.email?.split("@")?.get(0) ?: "user_${user.uid.take(6)}"),
            "country" to "South Africa",
            "selected_cuisines" to cuisineNames, // Use the pre-converted list
            "selected_diets" to emptyList<String>()
        )
        firestore.collection("users").document(user.uid).set(userProfile).await()
    }

    /**
     * Creates a new notification and saves it to a specific user's private sub-collection.
     */
    suspend fun createNotification(userId: String, title: String, message: String, iconName: String) {
        try {
            val notification = mapOf(
                "title" to title,
                "message" to message,
                "iconName" to iconName,
                "isRead" to false, // Always starts as unread
                "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            firestore.collection("users").document(userId).collection("notifications").add(notification).await()
            Log.d("Notifications", "Successfully created notification for user $userId: $title")
        } catch (e: Exception) {
            Log.e("Notifications", "Error creating notification for user $userId", e)
        }
    }

    /**
     * Sets up a real-time listener for a user's notifications.
     * This will automatically provide updates whenever a notification is added or changed.
     * It returns a Flow that the ViewModel can collect.
     */
    fun getNotificationsForUser(userId: String): Flow<List<Notification>> = callbackFlow {
        Log.d("Notifications", "Setting up real-time listener for user $userId...")
        val listenerRegistration = firestore.collection("users").document(userId).collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("Notifications", "Listener failed.", error)
                    close(error) // Close the flow on error
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val notifications = snapshot.toObjects(Notification::class.java)
                    val unreadCount = notifications.count { !it.isRead }
                    Log.d("Notifications", "Listener fired! Found ${notifications.size} notifications, $unreadCount are unread.")
                    trySend(notifications) // Send the latest list to the flow collector
                }
            }
        // This is called when the coroutine scope is cancelled, cleaning up the listener
        awaitClose {
            Log.d("Notifications", "Closing real-time listener.")
            listenerRegistration.remove()
        }
    }

    suspend fun syncFavoritesFromFirebase(userId: String) {
        try {
            // Step 1: Get all favorite recipe documents from the user's collection in Firebase.
            val favoritesSnapshot = firestore.collection("users").document(userId)
                .collection("favorites").get().await()

            // Step 2: Convert the documents into Recipe objects.
            val favoriteRecipes = favoritesSnapshot.toObjects(Recipe::class.java)

            // Step 3: Make sure the isFavorite flag is set to true before saving.
            val recipesToSave = favoriteRecipes.map { it.copy(isFavorite = true) }

            // Step 4: Save the list of favorited recipes into the local Room database.
            if (recipesToSave.isNotEmpty()) {
                recipeDao.insertAllRecipes(recipesToSave)
                Log.d("Sync", "Successfully synced ${recipesToSave.size} favorites from Firebase.")
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error syncing favorites from Firebase", e)
        }
    }

    suspend fun syncShoppingDataFromFirebase(userId: String) {
        Log.d("Sync", "Starting shopping data sync from Firebase for user $userId.")
        if (userId.isBlank()){
            Log.w("Sync", "User ID is blank, cannot sync shopping data.")
            return
        }
        try {
            // 1. Fetch Lists from Firestore
            val firestoreListsSnapshot = firestore.collection("users").document(userId)
                .collection("shopping_lists").get().await()
            val firestoreLists = firestoreListsSnapshot.toObjects(ShoppingList::class.java)
            Log.d("Sync", "Fetched ${firestoreLists.size} lists from Firestore.")

            // 2. Insert/Update Lists into Room
            if (firestoreLists.isNotEmpty()) {
                // Using insertShoppingLists which should handle conflicts (replace)
                shoppingDao.insertShoppingLists(firestoreLists)
                Log.d("Sync", "Upserted ${firestoreLists.size} lists into Room.")
            }

            // 3. Fetch Items for EACH list from Firestore
            var totalItemsSynced = 0
            firestoreLists.forEach { list ->
                try { // Add inner try-catch for item fetching per list
                    val firestoreItemsSnapshot = firestore.collection("users").document(userId)
                        .collection("shopping_lists").document(list.listId)
                        .collection("items").get().await()
                    val firestoreItems = firestoreItemsSnapshot.toObjects(ShoppingItem::class.java)

                    if (firestoreItems.isNotEmpty()) {
                        // 4. Insert/Update Items into Room
                        // Ensure ownerListId is correct before inserting (should be, but safe)
                        val validItems = firestoreItems.filter { it.ownerListId == list.listId }
                        if (validItems.isNotEmpty()) {
                            shoppingDao.insertItems(validItems) // Assumes insertItems handles conflicts
                            totalItemsSynced += validItems.size
                            //Log.d("Sync", "Upserted ${validItems.size} items for list ${list.listId} into Room.")
                        }
                        if (validItems.size != firestoreItems.size) {
                            Log.w("Sync", "Found ${firestoreItems.size - validItems.size} items with mismatched ownerListId for list ${list.listId}.")
                        }
                    }
                } catch (itemError: Exception) {
                    Log.e("Sync", "Error syncing items for list ${list.listId}", itemError)
                }
            }
            Log.d("Sync", "Finished syncing shopping data. Total items synced: $totalItemsSynced")

        } catch (e: Exception) {
            Log.e("Sync", "Error syncing shopping data from Firebase for user $userId", e)
            // Handle error appropriately - maybe notify the user?
        }
    }
}