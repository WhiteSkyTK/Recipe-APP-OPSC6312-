package com.rst.recipeappopsc6312

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ShoppingViewModelFactory(private val repository: ShoppingRepository) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // This is the new, smarter logic.
        // It checks which type of ViewModel is being requested and creates the correct one.
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingListViewModel(repository) as T
        }
        // Add other ViewModels that need the repository here...
        // if (modelClass.isAssignableFrom(AnotherViewModel::class.java)) { ... }

        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
