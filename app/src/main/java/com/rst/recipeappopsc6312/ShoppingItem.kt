package com.rst.recipeappopsc6312

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

// This class represents a single shopping list (e.g., "Groceries", "Hardware Store")
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey val listId: String = UUID.randomUUID().toString(),
    val name: String,
    val userId: String,
    val emoji: String
) {
    // Add a no-argument constructor for Firebase deserialization
    constructor() : this("", "", "", "🛒")
}

// This class represents one item within a shopping list
@Entity(tableName = "shopping_items",
    foreignKeys = [ForeignKey(
        entity = ShoppingList::class,
        parentColumns = ["listId"],
        childColumns = ["ownerListId"],
        onDelete = ForeignKey.CASCADE // If a list is deleted, its items are too
    )]
)
data class ShoppingItem(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val ownerListId: String, // Links this item to a ShoppingList
    val name: String,
    var isChecked: Boolean = false
) {
    // Add a no-argument constructor for Firebase deserialization
    constructor() : this("", "", "", false)
    @Ignore
    var isSelected: Boolean = false
}