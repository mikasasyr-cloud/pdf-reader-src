package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query(
        """
        SELECT * FROM annotations
        WHERE documentHash = :documentHash
          AND cloudProvider = :cloudProvider
          AND cloudFileId = :cloudFileId
        ORDER BY pageIndex ASC, scrollOffset ASC, createdAt ASC
        """,
    )
    fun observeForDocument(
        documentHash: String,
        cloudProvider: String,
        cloudFileId: String,
    ): Flow<List<AnnotationEntity>>

    @Query(
        """
        SELECT * FROM annotations
        WHERE documentHash = :documentHash
          AND cloudProvider = :cloudProvider
          AND cloudFileId = :cloudFileId
          AND pageIndex = :pageIndex
        ORDER BY createdAt ASC
        """,
    )
    fun observeForPage(
        documentHash: String,
        cloudProvider: String,
        cloudFileId: String,
        pageIndex: Int,
    ): Flow<List<AnnotationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(annotation: AnnotationEntity)

    @Query("UPDATE annotations SET noteText = :noteText, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateNote(id: String, noteText: String, updatedAt: Long)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteById(id: String)
}
