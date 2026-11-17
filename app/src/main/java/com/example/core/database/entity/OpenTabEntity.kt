package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "open_tabs")
data class OpenTabEntity(
    @PrimaryKey
    val tabId: String,
    val sortOrder: Int,
    val isSplit: Boolean = false,
    val activePaneIndex: Int = 0,
    val splitRatio: Float = 0.5f,
    // Primary (Left in LTR / First) Pane
    val primaryTitle: String,
    val primaryProvider: String,
    val primaryCloudFileId: String,
    val primaryDocumentHash: String,
    val primaryLocalCachePath: String,
    val primaryPageIndex: Int = 0,
    val primaryScrollOffset: Float = 0f,
    val primaryZoomScale: Float = 1.0f,
    val primaryLayoutMode: String = "SINGLE_CONTINUOUS",
    val primaryReadingFilter: String = "NORMAL",
    // Secondary (Right in LTR / Second) Pane for Split-Tab
    val secondaryTitle: String? = null,
    val secondaryProvider: String? = null,
    val secondaryCloudFileId: String? = null,
    val secondaryDocumentHash: String? = null,
    val secondaryLocalCachePath: String? = null,
    val secondaryPageIndex: Int = 0,
    val secondaryScrollOffset: Float = 0f,
    val secondaryZoomScale: Float = 1.0f,
    val secondaryLayoutMode: String = "SINGLE_CONTINUOUS",
    val secondaryReadingFilter: String = "NORMAL",
    val updatedAt: Long = System.currentTimeMillis(),
)
