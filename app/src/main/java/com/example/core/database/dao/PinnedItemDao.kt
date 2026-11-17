package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.PinnedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PinnedItemDao {
    @Query("SELECT * FROM pinned_items ORDER BY isFolder DESC, pinnedAt DESC")
    fun observeAllPinned(): Flow<List<PinnedItemEntity>>

    @Query("SELECT itemKey FROM pinned_items")
    fun observePinnedKeys(): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM pinned_items WHERE itemKey = :itemKey)")
    suspend fun isPinned(itemKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: PinnedItemEntity)

    @Query("DELETE FROM pinned_items WHERE itemKey = :itemKey")
    suspend fun deleteByKey(itemKey: String)
}
