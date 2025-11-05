package com.example.migration_script

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream

fun main() = runBlocking {
    // --- SETUP: This only needs to be done once ---
    val serviceAccount =
        FileInputStream("C:\\Users\\RC_Student_Lab\\Downloads\\serviceAccountKey.json")

    val options = FirebaseOptions.builder()
        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
        .build()

    if (FirebaseApp.getApps().isEmpty()) {
        FirebaseApp.initializeApp(options)
    }
    val db = FirestoreClient.getFirestore()
    // --- END SETUP ---


    // --- 1. TEST ON A SINGLE RECIPE (NOW COMMENTED OUT) ---
    /*
    val testRecipeId = "sp_1000566" // <-- ID of the recipe you tested
    println("Starting test migration for single recipe: $testRecipeId")

    val recipeRef = db.collection("recipes").document(testRecipeId)
    val doc = recipeRef.get().get()

    if (!doc.exists()) {
        println("TEST FAILED: Recipe with ID '$testRecipeId' not found.")
        return@runBlocking
    }

    val recipeData = doc.data!!
    val newDietTags = mutableListOf<String>()

    if (recipeData["vegan"] == true) newDietTags.add("Vegan")
    if (recipeData["vegetarian"] == true) newDietTags.add("Vegetarian")
    if (recipeData["glutenFree"] == true) newDietTags.add("Gluten-Free")
    if (recipeData["keto"] == true) newDietTags.add("Keto")
    if (recipeData["paleo"] == true) newDietTags.add("Paleo")
    if (recipeData["dairyFree"] == true) newDietTags.add("Dairy-Free")
    if (recipeData["lowFodmap"] == true) newDietTags.add("Low-FODMAP")

    if (newDietags.isNotEmpty()) {
        doc.reference.update("dietTags", newDietTags).get()
        println("SUCCESS: Updated recipe ${doc.id} with tags: $newDietTags")
    } else {
        println("TEST COMPLETE: The recipe had no boolean diet flags to migrate.")
    }
    */


    // --- 2. RUN FOR ALL RECIPES (NOW UNCOMMENTED) ---
    println("Starting recipe migration...")
    val recipesRef = db.collection("recipes")
    val allRecipes = recipesRef.get().get().documents

    if (allRecipes.isEmpty()) {
        println("No recipes found to migrate.")
        return@runBlocking
    }

    println("Found ${allRecipes.size} recipes to process. This may take a moment...")
    var updatedCount = 0

    for (doc in allRecipes) {
        val recipeData = doc.data
        if (recipeData.containsKey("dietTags") && (recipeData["dietTags"] as? List<*>)?.isNotEmpty() == true) {
            continue
        }

        val newDietTags = mutableListOf<String>()
        if (recipeData["vegan"] == true) newDietTags.add("Vegan")
        if (recipeData["vegetarian"] == true) newDietTags.add("Vegetarian")
        if (recipeData["glutenFree"] == true) newDietTags.add("Gluten-Free")
        if (recipeData["keto"] == true) newDietTags.add("Keto")
        if (recipeData["paleo"] == true) newDietTags.add("Paleo")
        if (recipeData["dairyFree"] == true) newDietTags.add("Dairy-Free")
        if (recipeData["lowFodmap"] == true) newDietTags.add("Low-FODMAP")

        if (newDietTags.isNotEmpty()) {
            doc.reference.update("dietTags", newDietTags).get()
            updatedCount++
            println("Updated recipe ${doc.id} with tags: $newDietTags")
        }
    }
    println("Migration complete! Successfully updated $updatedCount recipes.")
}

