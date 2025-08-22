package com.rst.recipeappopsc6312

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class AddRecipeViewModelFactory(private val repository: ShoppingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddRecipeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddRecipeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}