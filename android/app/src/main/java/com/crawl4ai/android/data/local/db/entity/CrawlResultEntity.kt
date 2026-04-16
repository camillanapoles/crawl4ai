package com.crawl4ai.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists the full result of a single crawl operation.
 * Maps 1-to-1 with [com.crawl4ai.android.domain.model.CrawlResponse].
 */
@Entity(
    tableName = "crawl_results",
    indices = [Index(value = ["url"]), Index(value = ["createdAt"])],
)
data class CrawlResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val url: String,
    val statusCode: Int = 0,
    val success: Boolean = false,

    // Raw content
    val html: String = "",
    val cleanedHtml: String = "",

    // Markdown variants
    val rawMarkdown: String = "",
    val markdownWithCitations: String = "",
    val referencesMarkdown: String = "",
    val fitMarkdown: String = "",

    // Extraction
    val extractedContent: String = "",

    // Meta
    val errorMessage: String = "",
    val cacheStatus: String = "miss",
    val headFingerprint: String = "",

    val createdAt: Long = System.currentTimeMillis(),
)
