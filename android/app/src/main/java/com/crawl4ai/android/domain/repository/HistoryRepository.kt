package com.crawl4ai.android.domain.repository

import com.crawl4ai.android.data.local.db.entity.CrawlHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for crawl history.
 */
interface HistoryRepository {
    fun getAllHistory(): Flow<List<CrawlHistoryEntity>>
    suspend fun getHistoryById(id: Long): CrawlHistoryEntity?
    suspend fun saveHistory(entity: CrawlHistoryEntity): Long
    suspend fun deleteHistory(id: Long)
    suspend fun clearAllHistory()
}
