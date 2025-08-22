package com.rst.recipeappopsc6312

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// This factory takes the repository as a parameter...
class ShoppingViewModelFactory(private val repository: ShoppingRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // ...and uses it to create an instance of ShoppingViewModel.
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}