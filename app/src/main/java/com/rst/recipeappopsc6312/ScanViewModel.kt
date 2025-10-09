package com.rst.recipeappopsc6312

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.launch

class ScanViewModel(private val repository: ShoppingRepository) : ViewModel() {

    private val _ingredientsList = MutableLiveData<MutableList<String>>(mutableListOf())
    val ingredientsList: LiveData<MutableList<String>> = _ingredientsList

    private val _recipeMatches = MutableLiveData<List<RecipeMatch>>()
    val recipeMatches: LiveData<List<RecipeMatch>> = _recipeMatches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // This holds the history for the history tab
    val scanHistory: LiveData<List<ScanHistoryItem>> = repository.getScanHistory()

    fun findRecipesByIngredients(userIngredients: List<String>) {
        if (userIngredients.isEmpty()) return

        _isLoading.value = true
        viewModelScope.launch {
            val results = repository.findRecipesByIngredients(userIngredients)
            _recipeMatches.postValue(results)

            // After finding recipes, save this scan to the history
            val topImage = results.firstOrNull()?.recipe?.imageUrl
            saveScanToHistory(userIngredients, topImage)

            _isLoading.postValue(false)
        }
    }

    // --- Functions for the Manual Input Tab ---
    fun addIngredient(ingredient: String?) {
        if (ingredient.isNullOrBlank()) return
        val currentList = _ingredientsList.value ?: mutableListOf()
        val formattedIngredient = ingredient.trim().replaceFirstChar { it.uppercase() }
        if (!currentList.contains(formattedIngredient)) {
            currentList.add(formattedIngredient)
            _ingredientsList.value = currentList
        }
    }

    fun removeIngredient(ingredient: String) {
        val currentList = _ingredientsList.value ?: mutableListOf()
        currentList.remove(ingredient)
        _ingredientsList.value = currentList
    }

    // --- Private Helper Functions ---
    private suspend fun saveScanToHistory(ingredients: List<String>, topRecipeImage: String?) {
        val title = ingredients.take(3).joinToString(", ") + if (ingredients.size > 3) "..." else ""
        val historyItem = ScanHistoryItem(
            title = title,
            imageUrl = topRecipeImage,
            ingredients = ingredients
        )
        repository.saveScanToHistory(historyItem)
    }

    fun findRecipesForIngredients(ingredients: List<String>) {
        if (ingredients.isEmpty()) return
        _isLoading.value = true
        viewModelScope.launch {
            val results = repository.findRecipesByIngredients(ingredients)
            _recipeMatches.postValue(results)

            // Save the scan to history after finding recipes
            val topImage = results.firstOrNull()?.recipe?.imageUrl
            saveScanToHistory(ingredients, topImage)

            _isLoading.postValue(false)
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun processImageFromCamera(
        image: InputImage,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit,
        onComplete: () -> Unit
    ) {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val objectDetector = ObjectDetection.getClient(options)

        objectDetector.process(image)
            .addOnSuccessListener { detectedObjects ->
                val labels = detectedObjects.mapNotNull { obj ->
                    obj.labels.firstOrNull()?.text?.replaceFirstChar { it.uppercase() }
                }.distinct() // Get unique labels
                onSuccess(labels)
            }
            .addOnFailureListener(onFailure)
            .addOnCompleteListener { onComplete() }
    }
}

