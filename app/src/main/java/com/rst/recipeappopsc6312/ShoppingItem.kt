package com.rst.recipeappopsc6312

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// This class represents a single shopping list (e.g., "Groceries", "Weekly Shop")
@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey val listId: String = UUID.randomUUID().toString(),
    val name: String,
    val userId: String,
    val emoji: String
) {
    // A no-argument constructor is required by Firebase for deserialization
    constructor() : this("", "", "", "🛒")
}

// This class represents one item within a shopping list
@Entity(
    tableName = "shopping_items",
    foreignKeys = [ForeignKey(
        entity = ShoppingList::class,
        parentColumns = ["listId"],
        childColumns = ["ownerListId"],
        onDelete = ForeignKey.CASCADE
    )],
    // ++ THIS IS THE FIX for the performance warning ++
    indices = [Index(value = ["ownerListId"])]
)
data class ShoppingItem(
    @PrimaryKey val itemId: String = UUID.randomUUID().toString(),
    val ownerListId: String, // Links this item to a ShoppingList
    val name: String,
    var isChecked: Boolean = false
) {
    constructor() : this("", "", "", false)

    // This property is only for UI state (e.g., selection mode) and is not stored in the database.
    @Transient
    var isSelected: Boolean = false
}
