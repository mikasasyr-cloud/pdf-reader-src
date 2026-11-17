package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "annotations",
    indices = [
        Index(value = ["documentHash", "cloudProvider", "cloudFileId"]),
    ],
)
data class AnnotationEntity(
    @PrimaryKey
    val id: String,
    val documentHash: String,
    val cloudProvider: String,
    val cloudFileId: String,
    val pageIndex: Int,
    val scrollOffset: Float,
    val selectedText: String,
    val noteText: String = "",
    val highlightColorKey: String,
    val rectLeftNorm: Float = 0f,
    val rectTopNorm: Float = 0f,
    val rectRightNorm: Float = 0f,
    val rectBottomNorm: Float = 0f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
