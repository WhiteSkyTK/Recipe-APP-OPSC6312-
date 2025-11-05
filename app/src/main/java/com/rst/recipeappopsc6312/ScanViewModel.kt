package com.rst.recipeappopsc6312

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    // --- ** START OF ML COMBINATION LOGIC ** ---

    /**
     * The main function to process an image. It runs both object and text detection in parallel
     * and merges the results.
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun processImageForIngredients(
        image: InputImage,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit,
        onComplete: () -> Unit
    ) {
        val allDetectedItems = mutableSetOf<String>()
        var objectDetectionFinished = false
        var textRecognitionFinished = false

        // Helper to check if both processes are done
        fun checkCompletion() {
            if (objectDetectionFinished && textRecognitionFinished) {
                onSuccess(allDetectedItems.toList())
                onComplete()
            }
        }

        // 1. Run Object Detection
        detectObjectsInImage(image,
            onSuccess = { objectLabels ->
                objectLabels.forEach { allDetectedItems.add(it) }
                objectDetectionFinished = true
                checkCompletion()
            },
            onFailure = {
                objectDetectionFinished = true // Mark as finished even on failure
                checkCompletion()
            }
        )

        // 2. Run Text Recognition
        detectTextInImage(image,
            onSuccess = { textWords ->
                textWords.forEach { allDetectedItems.add(it) }
                textRecognitionFinished = true
                checkCompletion()
            },
            onFailure = {
                textRecognitionFinished = true // Mark as finished even on failure
                checkCompletion()
            }
        )
    }

    /**
     * Helper function specifically for running the Object Detection model.
     */
    private fun detectObjectsInImage(
        image: InputImage,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
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
                }.distinct()
                onSuccess(labels)
            }
            .addOnFailureListener(onFailure)
    }

    /**
     * Helper function specifically for running the Text Recognition model.
     */
    private fun detectTextInImage(
        image: InputImage,
        onSuccess: (List<String>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val words = visionText.textBlocks.flatMap { block ->
                    block.lines.flatMap { line ->
                        line.elements.map { element ->
                            element.text.trim().replaceFirstChar { it.uppercase() }
                        }
                    }
                }.distinct()
                onSuccess(words)
            }
            .addOnFailureListener(onFailure)
    }

    // --- ** END OF ML COMBINATION LOGIC ** ---

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

