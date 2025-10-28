package com.rst.recipeappopsc6312

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudinary.Transformation
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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
    private val foodEmojis = listOf("🥞", "🍕", "🍔", "🍣", "🌮", "🥗", "🍜", "🍰", "🍩", "🥐", "🍝", "カレー", "🍲")
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

    suspend fun getFirebaseRecipeCount(): Int {
        val TAG = "FirebaseRecipeCount" // Define a tag for your logs for easy filtering
        return try {
            Log.d(TAG, "Attempting to fetch recipe count from Firestore...")
            val documents = firestore.collection("recipes").get().await()
            val count = documents.size()
            Log.d(TAG, "Successfully fetched recipe count: $count") // Log the count
            count // Return the count
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching recipe count from Firestore", e) // Log the error and the exception
            0 // Return 0 in case of error
        }
    }

    suspend fun seedFirebaseDatabase() {
        // First, check if we even need to seed. If you have enough recipes, we can stop.

        val currentRecipeCount = getFirebaseRecipeCount()
        if (currentRecipeCount >= 200) {
            Log.d("API_SEED", "Firestore has $currentRecipeCount recipes. No seeding needed.")
            return
        }

        Log.d("API_SEED", "Starting database seed with multiple API keys...")

        // 1. List all your API keys securely
        val spoonacularApiKeys  = listOf(
            BuildConfig.SPOONACULAR_API_KEY,
            BuildConfig.SPOONACULAR_API_KEY_1, // Assuming you've named them like this
            BuildConfig.SPOONACULAR_API_KEY_2,
            BuildConfig.SPOONACULAR_API_KEY_3,
            BuildConfig.SPOONACULAR_API_KEY_4,
            BuildConfig.SPOONACULAR_API_KEY_5,
            BuildConfig.SPOONACULAR_API_KEY_6,
            BuildConfig.TASTY_API_KEY,
            BuildConfig.TASTY_API_KEY1
        )

        // 2. Define the types of recipes you want
        val spoonacularTags = listOf(
            // Meals
            "african", "breakfast", "brunch", "lunch", "dinner", "snack", "dessert", "supper",

            // Courses
            "appetizer", "starter", "main course", "side dish", "soup", "salad",
            "bread", "sandwich", "wraps", "pizza", "pasta", "casserole",

            // Diet & lifestyle
            "vegetarian", "vegan", "keto", "paleo", "low-carb", "gluten free",
            "dairy free", "high protein", "low fat",

            // Cuisines
            "italian", "mexican", "indian", "thai", "chinese", "japanese",
            "mediterranean", "french", "greek", "spanish", "american",
            "african", "middle eastern", "caribbean",

            // Occasion & seasonal
            "holiday", "christmas", "thanksgiving", "easter", "halloween",
            "summer", "winter", "spring", "fall",

            // Proteins
            "chicken", "beef", "pork", "lamb", "seafood", "fish", "shrimp",
            "tofu", "eggs",

            // Others
            "comfort food", "healthy", "quick", "slow cooker", "instant pot",
            "bbq", "grilling", "stir fry", "baking"
        )


        // 3. Loop through each key and each tag to get a wide variety
        for (key in spoonacularApiKeys  ) {
            for (tag in spoonacularTags ) {
                try {
                    val response = apiService.getRandomRecipes(key, 10 , tag)
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

        val tastyQueries = listOf(
            // Proteins
            "african" ,"chicken", "beef", "pork", "lamb", "fish", "shrimp", "tofu", "egg",

            // Meals
            "breakfast", "brunch", "lunch", "dinner", "snack", "dessert",

            // Popular dishes
            "pasta", "pizza", "burger", "sandwich", "tacos", "salad", "soup",
            "curry", "stir fry", "fried rice",

            // Quick & easy
            "30 minute meals", "quick dessert", "meal prep", "one pot", "easy dinner",

            // Cuisines
            "italian", "mexican", "indian", "thai", "chinese", "japanese",
            "greek", "french", "american", "mediterranean",

            // Diet & lifestyle
            "vegetarian", "vegan", "keto", "gluten free", "healthy", "high protein",

            // Occasions
            "holiday", "christmas", "thanksgiving", "party food", "bbq", "summer"
        )

        try {
            for (query in tastyQueries) {
                val response = tastyApiService.listRecipes(from = 0, size = 20, query = query)
                val newRecipes = response.results.map { it.toAppRecipe() }
                newRecipes.forEach { firestore.collection("recipes").document(it.id).set(it) }
                Log.d("API_SEED", "Tasty: Fetched ${newRecipes.size} for query '$query'")
            }
        } catch (e: Exception) {
            Log.e("API_SEED", "Tasty API failed during seeding. Error: ${e.message}")
        }

        Log.d("API_SEED", "Database seeding process complete.")
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

            // Fallback to fetch from API if cache is empty
            val fetchedRecipes = fetchNewRecipesFromApis()
            val newRecipes = upscaleAndCacheRecipes(fetchedRecipes)
            return (newRecipes + cachedRecipes).distinctBy { it.id }.shuffled()

        } catch (e: FirebaseFirestoreException) {
            // ++ THIS IS THE FIX for offline crashes ++
            Log.w("Firestore", "Failed to get public recipes (likely offline): ${e.message}")
            // If we're offline, return an empty list. The UI will handle showing the offline message.
            return emptyList()
        }
    }

    private suspend fun fetchNewRecipesFromApis(): List<Recipe> {
        Log.d("API_FETCH", "Cache is low or refresh forced. Fetching from both APIs.")
        var spoonacularRecipes = emptyList<Recipe>()
        var tastyRecipes = emptyList<Recipe>()

        // 1. Gather user preferences to guide the API calls
        val userId = FirebaseManager.auth.currentUser?.uid
        var userCuisines = emptyList<String>()
        if (userId != null) {
            val userProfileDoc = firestore.collection("users").document(userId).get().await()
            userCuisines = userProfileDoc.get("selected_cuisines") as? List<String> ?: emptyList()
        }

        // 2. Define the full list of possible tags/queries
        val tastyQueries = listOf("chicken", "beef", "pork", "lamb", "fish", "shrimp", "tofu", "egg", "breakfast", "brunch", "lunch", "dinner", "snack", "dessert", "pasta", "pizza", "burger", "sandwich", "tacos", "salad", "soup", "curry", "stir fry", "fried rice", "30 minute meals", "quick dessert", "meal prep", "one pot", "easy dinner", "italian", "mexican", "indian", "thai", "chinese", "japanese", "greek", "french", "american", "mediterranean", "vegetarian", "vegan", "keto", "gluten free", "healthy", "high protein", "holiday", "christmas", "thanksgiving", "party food", "bbq", "summer")
        val spoonacularTags = listOf("breakfast", "brunch", "lunch", "dinner", "snack", "dessert", "supper", "appetizer", "starter", "main course", "side dish", "soup", "salad", "bread", "sandwich", "wraps", "pizza", "pasta", "casserole", "vegetarian", "vegan", "keto", "paleo", "low-carb", "gluten free", "dairy free", "high protein", "low fat", "italian", "mexican", "indian", "thai", "chinese", "japanese", "mediterranean", "french", "greek", "spanish", "american", "middle eastern", "caribbean", "holiday", "christmas", "thanksgiving", "easter", "halloween", "summer", "winter", "spring", "fall", "chicken", "beef", "pork", "lamb", "seafood", "fish", "shrimp", "tofu", "eggs", "comfort food", "healthy", "quick", "slow cooker", "instant pot", "bbq", "grilling", "stir fry", "baking")

        // 3. Build the final, prioritized list of search terms
        val finalSpoonacularTags = (listOf("african") + userCuisines + spoonacularTags.shuffled().take(5)).distinct()
        val finalTastyQueries = (listOf("african") + userCuisines + tastyQueries.shuffled().take(5)).distinct()

        // 4. Call both APIs in parallel for maximum efficiency
        coroutineScope {
            launch {
                try {
                    val apiKeys = listOf(
                        BuildConfig.SPOONACULAR_API_KEY_1,
                        BuildConfig.SPOONACULAR_API_KEY_2,
                        BuildConfig.SPOONACULAR_API_KEY_3,
                        BuildConfig.SPOONACULAR_API_KEY_4,
                        BuildConfig.SPOONACULAR_API_KEY_5,
                        BuildConfig.SPOONACULAR_API_KEY_6,
                        BuildConfig.TASTY_API_KEY,
                        BuildConfig.TASTY_API_KEY1
                    )
                    val response = apiService.getRandomRecipes(apiKeys.random(), 25, finalSpoonacularTags.joinToString(","))
                    spoonacularRecipes = response.recipes.map { it.toAppRecipe() }
                } catch (e: Exception) { Log.e("API_FETCH", "Spoonacular refresh failed: ${e.message}") }
            }
            launch {
                try {
                    val response = tastyApiService.listRecipes(0, 25, finalTastyQueries.joinToString(" "))
                    tastyRecipes = response.results.map { it.toAppRecipe() }
                } catch (e: Exception) { Log.e("API_FETCH", "Tasty refresh failed: ${e.message}") }
            }
        }

        val combinedNewRecipes = (spoonacularRecipes + tastyRecipes).distinctBy { it.id }
        return upscaleAndCacheRecipes(combinedNewRecipes)
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

        val today = Calendar.getInstance()
        val lastLogin = Calendar.getInstance()
        if (progress.lastLogin != null) {
            lastLogin.time = progress.lastLogin!!
        } else {
            // If they've never logged in, this is their first day
            lastLogin.add(Calendar.DAY_OF_YEAR, -1)
        }

        val isConsecutiveDay = today.get(Calendar.DAY_OF_YEAR) == lastLogin.get(Calendar.DAY_OF_YEAR) + 1 &&
                today.get(Calendar.YEAR) == lastLogin.get(Calendar.YEAR)

        val isSameDay = today.get(Calendar.DAY_OF_YEAR) == lastLogin.get(Calendar.DAY_OF_YEAR) &&
                today.get(Calendar.YEAR) == lastLogin.get(Calendar.YEAR)

        val newStreak = if (isSameDay) {
            progress.loginStreak // Don't increment if they already logged in today
        } else if (isConsecutiveDay) {
            progress.loginStreak + 1
        } else {
            1 // Reset streak
        }

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

    private suspend fun upscaleAndCacheRecipes(recipes: List<Recipe>): List<Recipe> {
        if (recipes.isEmpty()) return emptyList()
        Log.d("Cloudinary", "Upscaling ${recipes.size} new images...")
        return coroutineScope {
            recipes.map { recipe ->
                async {
                    val upscaledUrl = upscaleImageWithCloudinary(recipe.imageUrl, recipe.id)
                    val finalRecipe = recipe.copy(imageUrl = upscaledUrl ?: recipe.imageUrl)
                    firestore.collection("recipes").document(finalRecipe.id).set(finalRecipe).await()
                    finalRecipe
                }
            }.map { it.await() }
        }
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


        // === Step 2: Build an efficient Firestore query with HARD filters (Diet) ===
        var query: com.google.firebase.firestore.Query = firestore.collection("recipes")
            .whereEqualTo("public", true)

        if (category != null && category != "All") {
            query = query.whereEqualTo("category", category)
        }

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
            return getPopularRecipes(category) // Fallback on error
        }

        // === Step 3: Apply our new, smarter SOFT filters in Kotlin ===
        val filteredRecipes = candidateRecipes.filter { recipe ->
            val ingredientsLower = recipe.ingredients.map { it.name.lowercase() }.toSet()
            val titleLower = recipe.title.lowercase()

            // Helper to check if any keyword from a given list is present.
            fun matchesAny(text: String, keywords: Set<String>) = keywords.any { text.contains(it) }
            fun ingredientsMatchAny(keywords: Set<String>) = ingredientsLower.any { matchesAny(it, keywords) }
            fun titleMatchesAny(keywords: Set<String>) = matchesAny(titleLower, keywords)

            // A recipe is valid if it passes ALL the user's selected diet filters.
            userDiets.all { diet ->
                when (diet) {
                    "Vegan" -> {
                        val blocklist = DietaryKeywords.meatKeywords + DietaryKeywords.seafoodKeywords + DietaryKeywords.dairyKeywords + DietaryKeywords.eggKeywords
                        !ingredientsMatchAny(blocklist) && !titleMatchesAny(blocklist)
                    }
                    "Vegetarian" -> {
                        val blocklist = DietaryKeywords.meatKeywords + DietaryKeywords.seafoodKeywords
                        !ingredientsMatchAny(blocklist) && !titleMatchesAny(blocklist)
                    }
                    "Pescetarian" -> !ingredientsMatchAny(DietaryKeywords.meatKeywords)
                    "Nut Allergy" -> !ingredientsMatchAny(DietaryKeywords.nutKeywords) && !titleMatchesAny(DietaryKeywords.nutKeywords)
                    "Halal" -> !ingredientsMatchAny(DietaryKeywords.haramKeywords) && !titleMatchesAny(DietaryKeywords.haramKeywords)
                    "Kosher" -> {
                        val hasNonKosher = ingredientsMatchAny(DietaryKeywords.nonKosherKeywords)
                        val hasMeat = ingredientsMatchAny(DietaryKeywords.meatKeywords)
                        val hasDairy = ingredientsMatchAny(DietaryKeywords.dairyKeywords)
                        !hasNonKosher && !(hasMeat && hasDairy)
                    }
                    "Low-Carb" -> !ingredientsMatchAny(DietaryKeywords.highCarbKeywords)
                    // Other simple diets are already handled by the Firestore query, so they pass automatically.
                    else -> true
                }
            }
        }

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
                .whereEqualTo("isPopular", true)

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

    private fun applySoftDietFilters(recipes: List<Recipe>, userDiets: List<String>): List<Recipe> {
        if (userDiets.isEmpty()) return recipes

        return recipes.filter { recipe ->
            userDiets.all { diet ->
                val ingredientsLower = recipe.ingredients.map { it.name.lowercase() }.toSet()
                val titleLower = recipe.title.lowercase()

                fun ingredientsMatchAny(keywords: Set<String>) = ingredientsLower.any { ing -> keywords.any { kw -> ing.contains(kw) } }
                fun titleMatchesAny(keywords: Set<String>) = keywords.any { titleLower.contains(it) }

                when (diet) {
                    "Pescetarian" -> !ingredientsMatchAny(DietaryKeywords.meatKeywords)
                    "Nut Allergy" -> !ingredientsMatchAny(DietaryKeywords.nutKeywords) && !titleMatchesAny(DietaryKeywords.nutKeywords)
                    "Halal" -> !ingredientsMatchAny(DietaryKeywords.haramKeywords) && !titleMatchesAny(DietaryKeywords.haramKeywords)
                    "Kosher" -> {
                        val hasNonKosher = ingredientsMatchAny(DietaryKeywords.nonKosherKeywords)
                        val hasMeat = ingredientsMatchAny(DietaryKeywords.meatKeywords)
                        val hasDairy = ingredientsMatchAny(DietaryKeywords.dairyKeywords)
                        !hasNonKosher && !(hasMeat && hasDairy)
                    }
                    else -> true // Already handled by the hard filter
                }
            }
        }
    }

    suspend fun searchRecipes(queryText: String, userDiets: List<String>): List<Recipe> {
        // 1. Fetch all public recipes from the cache
        val allCachedRecipes = getPublicRecipes(forceRefresh = false)

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

    suspend fun searchRecipesFromApis(query: String): List<Recipe> {
        Log.d("API_SEARCH", "No results in cache for '$query'. Searching APIs...")
        var spoonacularRecipes = emptyList<Recipe>()
        var tastyRecipes = emptyList<Recipe>()

        coroutineScope {
            launch {
                try {
                    val apiKeys = listOf(
                        BuildConfig.SPOONACULAR_API_KEY_1, BuildConfig.SPOONACULAR_API_KEY_2,
                        BuildConfig.SPOONACULAR_API_KEY_3, BuildConfig.SPOONACULAR_API_KEY_4,
                        BuildConfig.SPOONACULAR_API_KEY_5, BuildConfig.SPOONACULAR_API_KEY_6,
                        BuildConfig.TASTY_API_KEY, BuildConfig.TASTY_API_KEY1).random()
                    val response = apiService.searchRecipes(apiKeys, query, 10)
                    spoonacularRecipes = response.results.map { it.toAppRecipe() }
                } catch (e: Exception) { Log.e("API_SEARCH", "Spoonacular search failed.") }
            }
            launch {
                try {
                    val response = tastyApiService.listRecipes(0, 10, query)
                    tastyRecipes = response.results.map { it.toAppRecipe() }
                } catch (e: Exception) { Log.e("API_SEARCH", "Tasty search failed.") }
            }
        }

        val newRecipes = (spoonacularRecipes + tastyRecipes).distinctBy { it.id }
        return upscaleAndCacheRecipes(newRecipes)
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

    suspend fun saveNewRecipe(recipe: Recipe, imageUri: Uri?) {
        var recipeToSave = recipe
        recipeDao.insertRecipe(recipeToSave)
        if (imageUri != null) {
            val userId = FirebaseManager.auth.currentUser?.uid ?: "unknown_user"
            val storageRef = storage.reference.child("recipe_images/$userId/${recipe.id}.jpg")
            val downloadUrl = storageRef.putFile(imageUri).await().storage.downloadUrl.await()
            recipeToSave = recipe.copy(imageUrl = downloadUrl.toString())
            recipeDao.insertRecipe(recipeToSave)
        }
        firestore.collection("recipes").document(recipeToSave.id).set(recipeToSave).await()

        // ++ TRIGGER for creating a recipe ++
        logRecipeCreated(recipeToSave.userId)
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
                .whereEqualTo("isPopular", true)
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
                batch.update(doc.reference, "isRead", true)
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

    suspend fun getDiscoverPage(sortOption: String, pageSize: Int, userDiets: List<String>): List<Recipe> {
        if (sortOption == "Recommended") return getRecommendedForYou()

        return try {
            var query: com.google.firebase.firestore.Query = firestore.collection("recipes").whereEqualTo("public", true)

            // Step 1: Apply HARD filters from user's diet preferences
            if (userDiets.isNotEmpty()) {
                query = query.whereArrayContainsAny("dietTags", userDiets)
            }

            // Step 2: Apply Sorting
            when (sortOption) {
                "Popular" -> query = query.orderBy("isPopular", com.google.firebase.firestore.Query.Direction.DESCENDING)
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
            "selected_country" to "South Africa",
            "selected_cuisines" to cuisineNames, // Use the pre-converted list
            "selected_diets" to emptyList<String>()
        )
        firestore.collection("users").document(user.uid).set(userProfile).await()
    }

    private suspend fun upscaleImageWithCloudinary(sourceUrl: String, publicId: String): String? {
        if (sourceUrl.isBlank()) return null

        return try {
            // 1. Download the image data from the API URL in the background
            val imageBytes = withContext(Dispatchers.IO) {
                URL(sourceUrl).readBytes()
            }

            val deferred = CompletableDeferred<String?>()
            MediaManager.get().upload(imageBytes)
                .option("public_id", publicId)
                .option("overwrite", true)
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>?) {
                        // Use the simple string method to create the transformation
                        val upscaleTransformation =
                            Transformation<Transformation<*>>() // Use documented Kotlin signature
                                .effect("e_upscale")
                        val newUrl = MediaManager.get().url()
                            .transformation(upscaleTransformation)
                            .generate(publicId)
                        deferred.complete(newUrl)
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("Cloudinary", "Upload error: ${error.description}")
                        deferred.complete(null)
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                }).dispatch()

            deferred.await()

        } catch (e: Exception) {
            Log.e("Cloudinary", "Failed to download image from URL: $sourceUrl", e)
            null // Return null if the download fails
        }
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