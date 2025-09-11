package com.rst.recipeappopsc6312

import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import com.cloudinary.Transformation
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Calendar

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
    private val tastyApiService = RetrofitClient.tastyInstance
    private val lastDocumentSnapshots = mutableMapOf<String, com.google.firebase.firestore.DocumentSnapshot?>()

    // Get lists directly from Room. Room is the source of truth for the UI.
    fun getAllShoppingListsForUser(userId: String) = shoppingDao.getAllShoppingListsForUser(userId)
    fun getItemsForList(listId: String) = shoppingDao.getItemsForList(listId)
    fun getAllItemsForUser(userId: String) = shoppingDao.getAllItemsForUser(userId)

    private var cachedFeatured: List<Recipe>? = null
    private var cachedRecommended: List<Recipe>? = null
    private var cachedCategories: List<Category>? = null
    private var cachedBreakfast: List<Recipe>? = null
    private var cachedLunch: List<Recipe>? = null
    private var cachedSnack: List<Recipe>? = null
    private var cachedDinner: List<Recipe>? = null

    suspend fun preloadHomeScreenData(forceRefresh: Boolean = false) = coroutineScope {
        // We use async to run all these network calls at the same time
        val featuredJob = async { getFeaturedRecipes(forceRefresh) }
        val recommendedJob = async { getRecommendedForYou(forceRefresh) }
        val categoriesJob = async { getAllCategories(forceRefresh) }
        val breakfastJob = async { getBreakfastRecipes(forceRefresh) }
        val lunchJob = async { getLunchRecipes(forceRefresh) }
        val snackJob = async { getSnackRecipes(forceRefresh) }
        val dinnerJob = async { getDinnerRecipes(forceRefresh) }

        // Wait for all of them to complete
        featuredJob.await()
        recommendedJob.await()
        categoriesJob.await()
        breakfastJob.await()
        lunchJob.await()
        snackJob.await()
        dinnerJob.await()
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
            BuildConfig.TASTY_API_KEY
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


    suspend fun getPublicRecipes(forceRefresh: Boolean = false, limit: Long = 20): List<Recipe> {
        val cachedRecipesQuery = firestore.collection("recipes")
            .whereEqualTo("public", true)
            .limit(limit) // Use the new limit parameter here
            .get().await()
        val cachedRecipes = cachedRecipesQuery.toObjects(Recipe::class.java)
        Log.d("DataFlow", "Step 1 (Repo/Public): Firestore query for public recipes returned ${cachedRecipes.size} documents.")


        // The rest of this function can be simplified.
        // We will now handle fetching new recipes in the ViewModel.
        if (!forceRefresh && cachedRecipes.isNotEmpty()) {
            Log.d("API_FETCH", "Found ${cachedRecipes.size} recipes in cache. Using them.")
            return cachedRecipes
        }

        // Fallback to fetch from API if cache is empty
        val fetchedRecipes = fetchNewRecipesFromApis()
        val newRecipes = upscaleAndCacheRecipes(fetchedRecipes)
        return (newRecipes + cachedRecipes).distinctBy { it.id }.shuffled()
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
                        BuildConfig.SPOONACULAR_API_KEY_1, BuildConfig.SPOONACULAR_API_KEY_2,
                        BuildConfig.SPOONACULAR_API_KEY_3, BuildConfig.SPOONACULAR_API_KEY_4,
                        BuildConfig.SPOONACULAR_API_KEY_5, BuildConfig.TASTY_API_KEY
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

    suspend fun getRecommendedForYou(forceRefresh: Boolean = false): List<Recipe> {
        if (cachedRecommended != null && !forceRefresh) return cachedRecommended!!
        val userId = FirebaseManager.auth.currentUser?.uid
        if (userId == null) return getPublicRecipes() // Fallback for logged-out users

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
        val recommended = scoredRecipes.toList()
            .sortedByDescending { (_, score) -> score }
            .map { (recipe, _) -> recipe }
            .take(20)

        val result = if (recommended.isNotEmpty()) recommended else getPopularRecipes()

        // ++ ADD THIS LINE AT THE VERY END ++
        cachedRecommended = result // Save the final result to the cache
        return result
    }

    suspend fun getPopularRecipes(): List<Recipe> {
        val popularRecipes = try {
            firestore.collection("recipes")
                .whereEqualTo("public", true)
                .whereEqualTo("isPopular", true)
                .limit(20).get().await().toObjects(Recipe::class.java)
        } catch (e: Exception) { emptyList() }

        // If there are no "popular" recipes, return a random selection from the cache.
        return if (popularRecipes.isNotEmpty()) popularRecipes else getPublicRecipes().shuffled().take(20)
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
                        BuildConfig.SPOONACULAR_API_KEY_5, BuildConfig.TASTY_API_KEY).random()
                    // IMPORTANT: You'll need to add a 'searchRecipes' endpoint to your ApiService interface
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
            Log.e("DataFlow", "Could not find recipe $recipeId in cache.", e)
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

            val publicRecipes = getPublicRecipes(forceRefresh)

            // ++ ADD THIS LOG ++
            Log.d("DataFlow", "Step 2 (Repo/Featured): Received ${publicRecipes.size} public recipes to generate featured list.")

            val result = publicRecipes.shuffled().take(5)

            Log.d("DataFlow", "Step 3 (Repo/Featured): Returning ${result.size} featured recipes.")

            if (popularRecipes.isNotEmpty()) {
                // SUCCESS: We found popular recipes.
                Log.d("FeaturedLogic", "Successfully fetched ${popularRecipes.size} popular recipes.")
                finalFeaturedList = popularRecipes.shuffled()
            } else {
                // STEP 2: ROBUST BACKUP PLAN
                Log.w("FeaturedLogic", "No popular recipes found. Using smart random fallback.")

                // First, get a large pool of ANY public recipes.
                val randomPool = getPublicRecipes(forceRefresh, limit = 50)

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
                    // If we found enough time-based ones, use them.
                    timeBasedList.shuffled().take(5)
                } else {
                    // Otherwise, just use 5 from the original random pool.
                    randomPool.shuffled().take(5)
                }
            }
        } catch (e: Exception) {
            // EMERGENCY BACKUP: The entire request failed.
            Log.e("FeaturedLogic", "Error fetching featured recipes. Using generic random fallback.", e)
            finalFeaturedList = getPublicRecipes(forceRefresh, limit = 5).shuffled()
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
        cachedLunch = result
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

    suspend fun getAllCategories(forceRefresh: Boolean = false): List<Category> {
        if (cachedCategories != null && !forceRefresh) return cachedCategories!!
        val publicRecipes = getPublicRecipes(forceRefresh)
        val categories = publicRecipes.map { Category(it.category) }
            .distinctBy { it.name }.sortedBy { it.name }.toMutableList()
        categories.add(0, Category("All", isSelected = true))
        cachedCategories = categories
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
        val userId = FirebaseManager.auth.currentUser?.uid ?: return // Can't favorite if not logged in
        val favoriteRef = firestore.collection("users").document(userId)
            .collection("favorites").document(recipe.id)

        // First, check the local Room DB for the current status
        val isCurrentlyFavorite = isFavoriteNow(recipe.id)
        val newFavoriteState = !isCurrentlyFavorite

        if (newFavoriteState) {
            // --- ADDING to favorites ---
            // Save the full recipe to the user's favorites in Firebase
            favoriteRef.set(recipe).await()

            // Save the full recipe to Room to ensure it's available offline
            recipeDao.insertRecipe(recipe.copy(isFavorite = true))

            // Log this action for the recommendation engine
            logActivity("FAVORITE_RECIPE", recipe.id)

        } else {
            // --- REMOVING from favorites ---
            // Delete the recipe from the user's favorites in Firebase
            favoriteRef.delete().await()

            // Log this action
            logActivity("UNFAVORITE_RECIPE", recipe.id)
        }

        // Finally, update the status in the local Room database for the UI to react
        recipeDao.updateFavoriteStatus(recipe.id, newFavoriteState)
    }

    private fun logActivity(type: String, value: String) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        val activity = UserActivity(type = type, value = value)
        firestore.collection("users").document(userId)
            .collection("activity_log").add(activity)
    }


    fun logSearchQuery(query: String) {
        val userId = FirebaseManager.auth.currentUser?.uid ?: return
        val activity = UserActivity(type = "SEARCH_QUERY", value = query)
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

    suspend fun getSortedAndFilteredRecipes(sortOption: String, category: String?): List<Recipe> {
        return try {
            var query: com.google.firebase.firestore.Query = firestore.collection("recipes")
                .whereEqualTo("public", true)

            // Apply category filter if one is selected
            if (category != null && category != "All") {
                query = query.whereEqualTo("category", category)
            }

            // Apply sorting
            when (sortOption) {
                "A-Z" -> query = query.orderBy("title", com.google.firebase.firestore.Query.Direction.ASCENDING)
                "Z-A" -> query = query.orderBy("title", com.google.firebase.firestore.Query.Direction.DESCENDING)
                // "Recommended" will be handled separately, so no sorting here.
            }

            // For now, we fetch a simple list. Pagination can be added back later if needed.
            val documents = query.limit(50).get().await()
            documents.toObjects(Recipe::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting sorted/filtered recipes", e)
            emptyList()
        }
    }

    suspend fun getDiscoverPage(sortOption: String, pageSize: Int): List<Recipe> {
        if (sortOption == "Recommended") return getRecommendedForYou()

        return try {
            var query: com.google.firebase.firestore.Query = firestore.collection("recipes").whereEqualTo("public", true)

            when (sortOption) {
                "Popular" -> query = query.orderBy("isPopular", com.google.firebase.firestore.Query.Direction.DESCENDING)
                "Cook Time" -> query = query.orderBy("timeInMins", com.google.firebase.firestore.Query.Direction.ASCENDING)
                "A-Z" -> query = query.orderBy("title", com.google.firebase.firestore.Query.Direction.ASCENDING)
                "Z-A" -> query = query.orderBy("title", com.google.firebase.firestore.Query.Direction.DESCENDING)
            }

            val lastDoc = lastDocumentSnapshots[sortOption]
            if (lastDoc != null) {
                query = query.startAfter(lastDoc)
            }

            val documents = query.limit(pageSize.toLong()).get().await()
            if (documents.size() > 0) {
                lastDocumentSnapshots[sortOption] = documents.documents[documents.size() - 1]
            }
            documents.toObjects(Recipe::class.java)
        } catch (e: Exception) {
            Log.e("Firestore", "Error getting discover page for sort: $sortOption", e)
            emptyList()
        }
    }

    // ++ ADD THIS POWERFUL UPSCALING FUNCTION ++
    suspend fun upscaleAllRecipeImages() {
        Log.d("Cloudinary", "Starting batch image upscaling process...")
        val allRecipes = getPublicRecipes(forceRefresh = false) // Get from cache
        for (recipe in allRecipes) {
            if (recipe.imageUrl.contains("cloudinary")) {
                Log.d("Cloudinary", "Skipping already upscaled image for: ${recipe.title}")
                continue
            }
            val upscaledUrl = upscaleImageWithCloudinary(recipe.imageUrl, recipe.id)
            if (upscaledUrl != null) {
                firestore.collection("recipes").document(recipe.id)
                    .update("imageUrl", upscaledUrl)
                    .await()
                Log.d("Cloudinary", "Successfully updated URL for: ${recipe.title}")
            } else {
                Log.e("Cloudinary", "Failed to upscale image for: ${recipe.title}")
            }
        }
        Log.d("Cloudinary", "Batch image upscaling process finished.")
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
            // This will make them available offline and update the UI.
            if (recipesToSave.isNotEmpty()) {
                recipeDao.insertAllRecipes(recipesToSave) // Assuming you have a bulk insert method.
                Log.d("Sync", "Successfully synced ${recipesToSave.size} favorites from Firebase.")
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error syncing favorites from Firebase", e)
        }
    }
}