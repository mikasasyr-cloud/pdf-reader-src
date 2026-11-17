package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.OpenTabEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OpenTabDao {
    @Query("SELECT * FROM open_tabs ORDER BY sortOrder ASC")
    fun observeAllTabs(): Flow<List<OpenTabEntity>>

    @Query("SELECT * FROM open_tabs ORDER BY sortOrder ASC")
    suspend fun getAllTabsOnce(): List<OpenTabEntity>

    @Query("SELECT * FROM open_tabs WHERE tabId = :tabId LIMIT 1")
    suspend fun getTabById(tabId: String): OpenTabEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTab(tab: OpenTabEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTabs(tabs: List<OpenTabEntity>)

    @Query("DELETE FROM open_tabs WHERE tabId = :tabId")
    suspend fun deleteTabById(tabId: String)

    @Query("DELETE FROM open_tabs")
    suspend fun clearAllTabs()
}
