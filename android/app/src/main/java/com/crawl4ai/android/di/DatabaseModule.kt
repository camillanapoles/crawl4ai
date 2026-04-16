package com.crawl4ai.android.di

import android.content.Context
import androidx.room.Room
import com.crawl4ai.android.data.local.db.Crawl4AIDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing the Room database and all DAO instances.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): Crawl4AIDatabase = Room.databaseBuilder(
        context,
        Crawl4AIDatabase::class.java,
        Crawl4AIDatabase.DATABASE_NAME,
    )
        // Enable Write-Ahead Logging for better concurrent read/write performance
        .enableMultiInstanceInvalidation()
        .build()

    @Provides
    fun provideCrawlResultDao(db: Crawl4AIDatabase) = db.crawlResultDao()

    @Provides
    fun provideCrawlHistoryDao(db: Crawl4AIDatabase) = db.crawlHistoryDao()

    @Provides
    fun provideCrawlConfigDao(db: Crawl4AIDatabase) = db.crawlConfigDao()

    @Provides
    fun provideDeepCrawlSessionDao(db: Crawl4AIDatabase) = db.deepCrawlSessionDao()

    @Provides
    fun provideExtractionTemplateDao(db: Crawl4AIDatabase) = db.extractionTemplateDao()
}
