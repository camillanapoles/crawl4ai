package com.crawl4ai.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A lightweight log entry for every crawl attempt (success or failure).
 * Linked to the full result via [resultId].
 */
@Entity(
    tableName = "crawl_history",
    indices = [Index(value = ["url"]), Index(value = ["createdAt"])],
)
data class CrawlHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val url: String,
    val configJson: String = "{}",
    val resultId: Long? = null,
    val durationMs: Long = 0L,
    val success: Boolean = false,
    val errorMessage: String? = null,
    val dataSizeBytes: Long = 0L,

    /** "single" | "multi" | "deep" */
    val crawlType: String = "single",

    val createdAt: Long = System.currentTimeMillis(),
)
