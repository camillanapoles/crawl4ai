package com.crawl4ai.android.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.crawl4ai.android.data.local.db.dao.CrawlConfigDao
import com.crawl4ai.android.data.local.db.dao.CrawlHistoryDao
import com.crawl4ai.android.data.local.db.dao.CrawlResultDao
import com.crawl4ai.android.data.local.db.dao.DeepCrawlSessionDao
import com.crawl4ai.android.data.local.db.dao.ExtractionTemplateDao
import com.crawl4ai.android.data.local.db.entity.CrawlConfigEntity
import com.crawl4ai.android.data.local.db.entity.CrawlHistoryEntity
import com.crawl4ai.android.data.local.db.entity.CrawlResultEntity
import com.crawl4ai.android.data.local.db.entity.DeepCrawlSessionEntity
import com.crawl4ai.android.data.local.db.entity.ExtractionTemplateEntity

/**
 * Room database for Crawl4AI Android.
 *
 * Increment [version] and provide a [Migration] whenever the schema changes.
 * Export schema JSON to `schemas/` directory for CI validation.
 */
@Database(
    entities = [
        CrawlResultEntity::class,
        CrawlHistoryEntity::class,
        CrawlConfigEntity::class,
        DeepCrawlSessionEntity::class,
        ExtractionTemplateEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class Crawl4AIDatabase : RoomDatabase() {

    abstract fun crawlResultDao(): CrawlResultDao
    abstract fun crawlHistoryDao(): CrawlHistoryDao
    abstract fun crawlConfigDao(): CrawlConfigDao
    abstract fun deepCrawlSessionDao(): DeepCrawlSessionDao
    abstract fun extractionTemplateDao(): ExtractionTemplateDao

    companion object {
        const val DATABASE_NAME = "crawl4ai.db"
    }
}
