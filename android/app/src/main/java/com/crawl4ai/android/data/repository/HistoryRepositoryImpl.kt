package com.crawl4ai.android.data.repository

import com.crawl4ai.android.data.local.db.dao.CrawlHistoryDao
import com.crawl4ai.android.data.local.db.entity.CrawlHistoryEntity
import com.crawl4ai.android.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HistoryRepositoryImpl @Inject constructor(
    private val dao: CrawlHistoryDao,
) : HistoryRepository {

    override fun getAllHistory(): Flow<List<CrawlHistoryEntity>> = dao.getAllFlow()

    override suspend fun getHistoryById(id: Long): CrawlHistoryEntity? = dao.getById(id)

    override suspend fun saveHistory(entity: CrawlHistoryEntity): Long = dao.insert(entity)

    override suspend fun deleteHistory(id: Long) = dao.deleteById(id)

    override suspend fun clearAllHistory() = dao.deleteAll()
}
