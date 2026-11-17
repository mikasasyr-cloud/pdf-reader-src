package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pinned_items")
data class PinnedItemEntity(
    @PrimaryKey
    val itemKey: String,
    val provider: String,
    val cloudFileId: String,
    val title: String,
    val isFolder: Boolean,
    val parentFolderId: String? = null,
    val mimeType: String? = null,
    val pinnedAt: Long = System.currentTimeMillis(),
) {
    companion object {
        fun buildKey(provider: String, cloudFileId: String): String = "$provider::$cloudFileId"
    }
}
