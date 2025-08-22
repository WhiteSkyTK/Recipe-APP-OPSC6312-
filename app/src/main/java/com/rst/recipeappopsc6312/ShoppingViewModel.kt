package com.rst.recipeappopsc6312

import androidx.lifecycle.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.util.UUID

class ShoppingViewModel(repository: ShoppingRepository) : BaseRecipeViewModel(repository) {

    companion object {
        const val ALL_ITEMS_ID = "ALL_ITEMS_ID"
        const val MY_LIST_ID = "MY_LIST_ID" // This will be the default list
    }

    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val userId = MutableLiveData<String>() // Replace with actual logged-in user ID from Firebase Auth

    private val databaseShoppingLists: LiveData<List<ShoppingList>> = userId.switchMap { id ->
        repository.getAllShoppingListsForUser(id)
    }

    // LiveData for the list of all shopping lists (for the Spinner)
    val allShoppingLists: LiveData<List<ShoppingList>> = databaseShoppingLists

    private val _selectedListId = MutableLiveData<String>()

    private val _featuredRecipes = MutableLiveData<List<Recipe>>()
    val featuredRecipes: LiveData<List<Recipe>> = _featuredRecipes

    private val _recommendedRecipes = MutableLiveData<List<Recipe>>()
    val recommendedRecipes: LiveData<List<Recipe>> = _recommendedRecipes

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    val currentShoppingItems: LiveData<List<ShoppingItem>> = _selectedListId.switchMap { listId ->
        when(listId) {
            ALL_ITEMS_ID -> repository.getAllItemsForUser(userId.value!!)
            MY_LIST_ID -> repository.getItemsForList(MY_LIST_ID)
            else -> repository.getItemsForList(listId)
        }
    }

    fun createListFromRecipe(recipeTitle: String, ingredientNames: List<String>) {
        viewModelScope.launch {
            val currentUserId = userId.value ?: return@launch
            repository.createNewListWithItems(recipeTitle, ingredientNames, currentUserId)
        }
    }

    private val _isInSelectionMode = MutableLiveData(false)
    val isInSelectionMode: LiveData<Boolean> = _isInSelectionMode

    private val _selectedItems = MutableLiveData<MutableSet<ShoppingItem>>(mutableSetOf())
    val selectedItemsCount: LiveData<Int> = _selectedItems.map { it.size }


    init {
        // When the ViewModel is created, check if a user is logged in.
        currentUser?.let { user ->
            // If logged in, set the userId and sync data from Firebase.
            userId.value = user.uid
            viewModelScope.launch {
                repository.syncFirebaseToRoom(user.uid)
            }
        }
    }

    fun onItemClicked(item: ShoppingItem) {
        if (_isInSelectionMode.value == true) {
            toggleItemSelection(item)
        } else {
            // If not in selection mode, a normal click toggles the checkbox
            onItemCheckedChanged(item, !item.isChecked)
        }
    }

    fun onItemLongClicked(item: ShoppingItem) {
        _isInSelectionMode.value = true
        toggleItemSelection(item)
    }

    fun onItemCheckedChanged(item: ShoppingItem, isChecked: Boolean) {
        // Find the item in the current list
        val currentList = currentShoppingItems.value ?: return
        val updatedItem = currentList.find { it.itemId == item.itemId } ?: return

        // Create a copy with the new checked state
        val itemToSave = updatedItem.copy(isChecked = isChecked)

        // Save the updated copy
        viewModelScope.launch {
            val currentUserId = userId.value ?: return@launch
            repository.updateItem(itemToSave, currentUserId)
        }
    }

    private fun toggleItemSelection(item: ShoppingItem) {
        val selected = _selectedItems.value ?: mutableSetOf()
        // We need to find the actual item from the current list to modify its isSelected property
        val currentList = currentShoppingItems.value
        val actualItem = currentList?.find { it.itemId == item.itemId }

        if (actualItem != null) {
            if (selected.any { it.itemId == actualItem.itemId }) {
                selected.removeIf { it.itemId == actualItem.itemId }
                actualItem.isSelected = false
            } else {
                selected.add(actualItem)
                actualItem.isSelected = true
            }
        }

        _selectedItems.value = selected

        if (selected.isEmpty()) {
            _isInSelectionMode.value = false
        }
    }

    fun setSelectedList(listId: String) {
        _selectedListId.value = listId
    }

    fun updateItem(item: ShoppingItem) = viewModelScope.launch {
        val currentUserId = userId.value ?: return@launch
        repository.updateItem(item, currentUserId)
    }

    fun deleteSelectedItems() {
        val itemsToDelete = _selectedItems.value?.toList() ?: return
        if (itemsToDelete.isNotEmpty()) {
            viewModelScope.launch {
                val currentUserId = userId.value ?: return@launch
                repository.deleteItems(itemsToDelete, currentUserId)
            }
        }
        // Clear selection and exit selection mode
        _selectedItems.value?.clear()
        _isInSelectionMode.value = false
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            val currentUserId = userId.value ?: return@launch
            repository.deleteItems(listOf(item), currentUserId)
        }
    }

    // For the "Undo" snackbar
    fun addItem(item: ShoppingItem) {
        viewModelScope.launch {
            val currentUserId = userId.value ?: return@launch
            repository.insertItem(item, currentUserId)
        }
    }

    fun onListSelected(position: Int) {
        when(position) {
            0 -> _selectedListId.value = ALL_ITEMS_ID
            1 -> _selectedListId.value = MY_LIST_ID
            else -> {
                // Get the actual recipe list, accounting for the 2 special items at the start
                val recipeListIndex = position - 2
                val selectedList = databaseShoppingLists.value?.get(recipeListIndex)
                _selectedListId.value = selectedList?.listId
            }
        }
    }

    // For adding new items from the dialog
    fun addItems(itemNames: List<String>) {
        viewModelScope.launch {
            // Hardcode this to only add to "My List"
            val listId = MY_LIST_ID
            val currentUserId = userId.value ?: return@launch
            // We need to ensure "My List" exists
            repository.ensureListExists(MY_LIST_ID, "My List", currentUserId)

            val newItems = itemNames.map { name -> ShoppingItem(ownerListId = listId, name = name) }
            repository.addItems(newItems, listId, currentUserId)
        }
    }

    fun deleteCurrentList() {
        // Ensure a list is selected and it's not one of the permanent ones
        val listId = _selectedListId.value
        if (listId == null || listId == ALL_ITEMS_ID || listId == MY_LIST_ID) {
            return // Do nothing if it's a special list
        }

        // Find the list object and delete it
        viewModelScope.launch {
            val listToDelete = databaseShoppingLists.value?.find { it.listId == listId }
            val currentUserId = userId.value
            if (listToDelete != null && currentUserId != null) {
                repository.deleteList(listToDelete, currentUserId)
                // After deleting, select "My List" as a safe default
                _selectedListId.postValue(MY_LIST_ID)
            }
        }
    }

    fun loadHomeScreenData() {
        _isLoading.value = true
        viewModelScope.launch {
            _featuredRecipes.postValue(repository.getFeaturedRecipes())
            // ++ CHANGE this line to call your new function ++
            _recommendedRecipes.postValue(repository.getRecommendedForYou())
            _categories.postValue(repository.getAllCategories())
            _isLoading.postValue(false)
        }
    }

    fun refreshHomeScreenData() {
        _isLoading.value = true
        viewModelScope.launch {
            _featuredRecipes.postValue(repository.getFeaturedRecipes(forceRefresh = true))
            // ++ ALSO CHANGE this line for the refresh action ++
            _recommendedRecipes.postValue(repository.getRecommendedForYou())
            _categories.postValue(repository.getAllCategories(forceRefresh = true))
            _isLoading.postValue(false)
        }
    }
}