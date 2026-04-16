package com.crawl4ai.android.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persists metadata for a deep-crawl session.
 * Individual page results are stored in [CrawlResultEntity].
 */
@Entity(
    tableName = "deep_crawl_sessions",
    indices = [Index(value = ["sessionId"], unique = true), Index(value = ["createdAt"])],
)
data class DeepCrawlSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    val sessionId: String,        // UUID generated at session start
    val startUrl: String,

    /** "bfs" | "dfs" | "bff" */
    val strategy: String = "bfs",
    val maxDepth: Int = 3,
    val maxPages: Int = 50,
    val pagesVisited: Int = 0,

    /** "running" | "completed" | "failed" | "cancelled" */
    val status: String = "running",

    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val durationMs: Long = 0L,
)
