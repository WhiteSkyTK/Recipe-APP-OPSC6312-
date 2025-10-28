package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ShoppingListViewModel(private val repository: ShoppingRepository) : ViewModel() {

    companion object {
        const val ALL_ITEMS_ID = "ALL_ITEMS_ID"
        const val MY_LIST_ID = "MY_LIST_ID"
    }

    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val userId = MutableLiveData<String>()

    private val databaseShoppingLists: LiveData<List<ShoppingList>> = userId.switchMap { id ->
        repository.getAllShoppingListsForUser(id)
    }

    val allShoppingLists: LiveData<List<ShoppingList>> = databaseShoppingLists
    private val _selectedListId = MutableLiveData<String>()

    init {
        currentUser?.let { user ->
            userId.value = user.uid
            viewModelScope.launch {
                // Ensure the default "My List" exists locally first
                repository.ensureListExists(MY_LIST_ID, "My List", user.uid)
                // ** ADD THIS LINE TO TRIGGER THE SYNC **
                repository.syncShoppingDataFromFirebase(user.uid)
            }
        }
        // If currentUser is null initially, you might need an observer
        // on the auth state to trigger the sync when the user logs in.
    }

    val currentShoppingItems: LiveData<List<ShoppingItem>> = _selectedListId.switchMap { listId ->
        when (listId) {
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

    fun onItemClicked(item: ShoppingItem) {
        if (_isInSelectionMode.value == true) {
            toggleItemSelection(item)
        } else {
            onItemCheckedChanged(item, !item.isChecked)
        }
    }

    fun onItemLongClicked(item: ShoppingItem) {
        _isInSelectionMode.value = true
        toggleItemSelection(item)
    }

    fun onItemCheckedChanged(item: ShoppingItem, isChecked: Boolean) {
        val updatedItem = item.copy(isChecked = isChecked)
        viewModelScope.launch {
            val currentUserId = userId.value ?: return@launch
            repository.updateItem(updatedItem, currentUserId)
        }
    }

    private fun toggleItemSelection(item: ShoppingItem) {
        val selected = _selectedItems.value ?: mutableSetOf()
        if (selected.any { it.itemId == item.itemId }) {
            selected.removeIf { it.itemId == item.itemId }
        } else {
            selected.add(item)
        }
        _selectedItems.value = selected

        if (selected.isEmpty()) {
            _isInSelectionMode.value = false
        }
    }

    fun onListSelected(position: Int) {
        when (position) {
            0 -> _selectedListId.value = ALL_ITEMS_ID
            1 -> _selectedListId.value = MY_LIST_ID
            else -> {
                val recipeListIndex = position - 2
                val selectedList = databaseShoppingLists.value?.get(recipeListIndex)
                _selectedListId.value = selectedList?.listId
            }
        }
    }

    fun addItems(itemNames: List<String>) {
        viewModelScope.launch {
            var targetListId = _selectedListId.value ?: MY_LIST_ID // Default to My List if null
            // If "All Items" is selected, force add to "My List" instead.
            if (targetListId == ALL_ITEMS_ID) {
                targetListId = MY_LIST_ID
            }
            val currentUserId = userId.value ?: return@launch
            val newItems = itemNames.map { name -> ShoppingItem(ownerListId = targetListId, name = name) }
            repository.addItems(newItems, targetListId, currentUserId)
        }
    }

    fun deleteSelectedItems() {
        val itemsToDelete = _selectedItems.value?.toList() ?: return
        if (itemsToDelete.isNotEmpty()) {
            viewModelScope.launch {
                val currentUserId = userId.value ?: return@launch
                repository.deleteItems(itemsToDelete, currentUserId)
            }
        }
        clearSelection()
    }

    fun clearSelection() {
        _selectedItems.value?.clear()
        _isInSelectionMode.value = false
    }

    fun deleteItem(item: ShoppingItem) {
        viewModelScope.launch {
            val currentUserId = userId.value ?: return@launch
            repository.deleteItems(listOf(item), currentUserId)
        }
    }

    fun addItem(item: ShoppingItem) {
        viewModelScope.launch {
            val currentUserId = userId.value ?: return@launch
            repository.insertItem(item, currentUserId)
        }
    }

    fun deleteCurrentList() {
        val listId = _selectedListId.value
        if (listId == null || listId == ALL_ITEMS_ID || listId == MY_LIST_ID) return

        viewModelScope.launch {
            val listToDelete = databaseShoppingLists.value?.find { it.listId == listId }
            val currentUserId = userId.value
            if (listToDelete != null && currentUserId != null) {
                repository.deleteList(listToDelete, currentUserId)
                _selectedListId.postValue(MY_LIST_ID)
            }
        }
    }
}

