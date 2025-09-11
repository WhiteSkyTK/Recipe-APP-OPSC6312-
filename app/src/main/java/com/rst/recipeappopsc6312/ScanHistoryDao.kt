package com.rst.recipeappopsc6312

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScanHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(historyItem: ScanHistoryItem)

    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getScanHistory(): LiveData<List<ScanHistoryItem>>
}