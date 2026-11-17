package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "toc_nodes",
    foreignKeys = [
        ForeignKey(
            entity = TocNodeEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["documentHash", "cloudProvider", "cloudFileId"]),
        Index(value = ["parentId"]),
    ],
)
data class TocNodeEntity(
    @PrimaryKey
    val id: String,
    val documentHash: String,
    val cloudProvider: String,
    val cloudFileId: String,
    val parentId: String? = null,
    val title: String,
    val pageIndex: Int,
    val sortOrder: Int,
    val createdAt: Long = System.currentTimeMillis(),
)
