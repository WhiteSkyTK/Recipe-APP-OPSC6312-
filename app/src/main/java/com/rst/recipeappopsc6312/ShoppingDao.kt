package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface ShoppingDao {

    // --- Shopping List Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(list: ShoppingList)

    @Query("SELECT * FROM shopping_lists")
    fun getAllShoppingLists(): LiveData<List<ShoppingList>>

    // --- Shopping Item Operations ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingItem)

    @Update
    suspend fun updateItem(item: ShoppingItem)

    @Delete
    suspend fun deleteItems(items: List<ShoppingItem>)

    @Delete
    suspend fun deleteList(list: ShoppingList)

    @Query("SELECT * FROM shopping_lists WHERE userId = :userId")
    fun getAllShoppingListsForUser(userId: String): LiveData<List<ShoppingList>>

    @Query("SELECT * FROM shopping_lists WHERE listId = :listId")
    suspend fun getListById(listId: String): ShoppingList?

    @Query("SELECT * FROM shopping_items WHERE ownerListId = :listId")
    fun getItemsForList(listId: String): LiveData<List<ShoppingItem>>

    @Query("SELECT COUNT(*) FROM shopping_items WHERE ownerListId = :listId AND name = :itemName")
    suspend fun itemExists(listId: String, itemName: String): Int

    @Query("SELECT * FROM shopping_lists WHERE name = :name AND userId = :userId LIMIT 1")
    suspend fun getListByName(name: String, userId: String): ShoppingList?

    @Query("""
        SELECT i.* FROM shopping_items i
        INNER JOIN shopping_lists l ON i.ownerListId = l.listId
        WHERE l.userId = :userId
    """)
    fun getAllItemsForUser(userId: String): LiveData<List<ShoppingItem>>
}