package com.rst.recipeappopsc6312

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "scan_history")
@TypeConverters(StringListConverter::class) // We'll create this next
data class ScanHistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imageUrl: String?,
    val ingredients: List<String>,
    val timestamp: Date = Date()
)