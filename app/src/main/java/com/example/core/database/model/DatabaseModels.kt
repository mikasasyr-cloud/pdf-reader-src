package com.example.core.database.model

import com.example.core.database.entity.TocNodeEntity

data class DocumentIdentity(
    val documentHash: String,
    val cloudProvider: String,
    val cloudFileId: String,
)

enum class HighlightColorKey {
    YELLOW,
    MINT,
    SKY,
    PINK;

    companion object {
        fun fromString(raw: String?): HighlightColorKey =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: YELLOW
    }
}

enum class ReadingLayoutMode {
    SINGLE_CONTINUOUS,
    SINGLE_SWIPE,
    TWO_PAGE_SPREAD;

    companion object {
        fun fromString(raw: String?): ReadingLayoutMode =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: SINGLE_CONTINUOUS
    }
}

data class TocTreeChapter(
    val chapter: TocNodeEntity,
    val subHeadings: List<TocNodeEntity>,
)
