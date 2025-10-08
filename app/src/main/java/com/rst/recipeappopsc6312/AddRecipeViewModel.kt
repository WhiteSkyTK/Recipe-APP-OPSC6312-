package com.rst.recipeappopsc6312

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// This is the data class to hold the result of a save or delete operation.
data class SaveResult(val success: Boolean, val error: String? = null, val isDelete: Boolean = false)

class AddRecipeViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    private val _saveStatus = MutableLiveData<SaveResult>()
    val saveStatus: LiveData<SaveResult> = _saveStatus

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories.map { allCats ->
        // This correctly filters out the "All" category so users can't assign it to a new recipe.
        allCats.filter { !it.name.equals("All", ignoreCase = true) }
    }

    init {
        loadCategories()
    }

    fun saveRecipe(recipe: Recipe, imageUri: Uri?) {
        viewModelScope.launch {
            try {
                repository.saveNewRecipe(recipe, imageUri)
                _saveStatus.postValue(SaveResult(success = true))
            } catch (e: Exception) {
                _saveStatus.postValue(SaveResult(success = false, error = e.message))
            }
        }
    }

    fun deleteRecipe(recipe: Recipe) {
        viewModelScope.launch {
            try {
                repository.deleteRecipe(recipe)
                _saveStatus.postValue(SaveResult(success = true, isDelete = true))
            } catch (e: Exception) {
                // Correctly includes the error message on failure
                _saveStatus.postValue(SaveResult(success = false, error = e.message, isDelete = true))
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.postValue(repository.getAllCategories())
        }
    }
}

