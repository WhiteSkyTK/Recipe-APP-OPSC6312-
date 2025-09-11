package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class ScanViewModel(private val repository: ShoppingRepository) : ViewModel() {

    // This will hold the final list of matched recipes, sorted and with missing ingredient info
    private val _recipeMatches = MutableLiveData<List<RecipeMatch>>()
    val recipeMatches: LiveData<List<RecipeMatch>> = _recipeMatches

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    val scanHistory: LiveData<List<ScanHistoryItem>> = repository.getScanHistory()


    fun findRecipes(userIngredients: List<String>) {
        _isLoading.value = true
        viewModelScope.launch {
            val results = repository.findRecipesByIngredients(userIngredients)
            _recipeMatches.postValue(results)
            _isLoading.postValue(false)
        }
    }

    fun saveScanToHistory(ingredients: List<String>, topRecipeImage: String?) {
        viewModelScope.launch {
            val title = ingredients.take(3).joinToString(", ") + if (ingredients.size > 3) "..." else ""
            val historyItem = ScanHistoryItem(
                title = title,
                imageUrl = topRecipeImage,
                ingredients = ingredients
            )
            repository.saveScanToHistory(historyItem)
        }
    }
}