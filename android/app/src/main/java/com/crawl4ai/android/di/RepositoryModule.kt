package com.crawl4ai.android.di

import com.crawl4ai.android.data.repository.CrawlRepositoryImpl
import com.crawl4ai.android.data.repository.ConfigRepositoryImpl
import com.crawl4ai.android.data.repository.HistoryRepositoryImpl
import com.crawl4ai.android.domain.repository.CrawlRepository
import com.crawl4ai.android.domain.repository.ConfigRepository
import com.crawl4ai.android.domain.repository.HistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCrawlRepository(impl: CrawlRepositoryImpl): CrawlRepository

    @Binds
    @Singleton
    abstract fun bindConfigRepository(impl: ConfigRepositoryImpl): ConfigRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository
}
