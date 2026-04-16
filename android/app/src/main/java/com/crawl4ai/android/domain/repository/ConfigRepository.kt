package com.crawl4ai.android.domain.repository

import com.crawl4ai.android.data.local.db.entity.CrawlConfigEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing saved crawl configurations.
 */
interface ConfigRepository {
    fun getAllConfigs(): Flow<List<CrawlConfigEntity>>
    suspend fun getConfig(id: Long): CrawlConfigEntity?
    suspend fun getDefaultConfig(): CrawlConfigEntity?
    suspend fun saveConfig(entity: CrawlConfigEntity): Long
    suspend fun deleteConfig(id: Long)
    suspend fun setDefault(id: Long)
}
