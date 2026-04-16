package com.crawl4ai.android.data.repository

import com.crawl4ai.android.data.local.db.dao.CrawlConfigDao
import com.crawl4ai.android.data.local.db.entity.CrawlConfigEntity
import com.crawl4ai.android.domain.repository.ConfigRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConfigRepositoryImpl @Inject constructor(
    private val dao: CrawlConfigDao,
) : ConfigRepository {

    override fun getAllConfigs(): Flow<List<CrawlConfigEntity>> = dao.getAllFlow()

    override suspend fun getConfig(id: Long): CrawlConfigEntity? = dao.getById(id)

    override suspend fun getDefaultConfig(): CrawlConfigEntity? = dao.getDefault()

    override suspend fun saveConfig(entity: CrawlConfigEntity): Long = dao.insert(entity)

    override suspend fun deleteConfig(id: Long) = dao.deleteById(id)

    override suspend fun setDefault(id: Long) {
        dao.clearDefault()
        dao.setDefault(id)
    }
}
