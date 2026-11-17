package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.TocNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TocNodeDao {
    @Query(
        """
        SELECT * FROM toc_nodes
        WHERE documentHash = :documentHash
          AND cloudProvider = :cloudProvider
          AND cloudFileId = :cloudFileId
        ORDER BY sortOrder ASC, pageIndex ASC, createdAt ASC
        """,
    )
    fun observeForDocument(
        documentHash: String,
        cloudProvider: String,
        cloudFileId: String,
    ): Flow<List<TocNodeEntity>>

    @Query(
        """
        SELECT * FROM toc_nodes
        WHERE documentHash = :documentHash
          AND cloudProvider = :cloudProvider
          AND cloudFileId = :cloudFileId
          AND parentId IS NULL
        ORDER BY sortOrder ASC, pageIndex ASC
        """,
    )
    suspend fun getChaptersOnce(
        documentHash: String,
        cloudProvider: String,
        cloudFileId: String,
    ): List<TocNodeEntity>

    @Query("SELECT * FROM toc_nodes WHERE parentId = :parentId ORDER BY sortOrder ASC, pageIndex ASC")
    suspend fun getSubHeadingsOnce(parentId: String): List<TocNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: TocNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(nodes: List<TocNodeEntity>)

    @Query("UPDATE toc_nodes SET title = :title WHERE id = :id")
    suspend fun renameNode(id: String, title: String)

    @Query("UPDATE toc_nodes SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun updateSortOrder(id: String, sortOrder: Int)

    @Query("DELETE FROM toc_nodes WHERE id = :id")
    suspend fun deleteById(id: String)
}
