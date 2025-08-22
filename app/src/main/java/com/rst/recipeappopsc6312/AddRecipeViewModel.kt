package com.rst.recipeappopsc6312

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

// This ViewModel takes the repository to handle data operations
class AddRecipeViewModel(val repository: ShoppingRepository) : ViewModel() {

    // LiveData to notify the Fragment when saving is complete
    private val _saveStatus = MutableLiveData<SaveResult>()
    val saveStatus: LiveData<SaveResult> = _saveStatus

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories.map { allCats ->
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
                _saveStatus.postValue(SaveResult(success = false, error = e.message))
            }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _categories.postValue(repository.getAllCategories())
        }
    }
}

// Also, update the SaveResult to track if it was a deletion
data class SaveResult(val success: Boolean, val error: String? = null, val isDelete: Boolean = false)