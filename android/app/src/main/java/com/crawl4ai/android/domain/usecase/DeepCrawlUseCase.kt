package com.crawl4ai.android.domain.usecase

import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.DeepCrawlConfig
import com.crawl4ai.android.domain.model.DeepCrawlProgress
import com.crawl4ai.android.domain.repository.CrawlRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Execute a deep (multi-level) crawl emitting progress per page. */
class DeepCrawlUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    operator fun invoke(
        startUrl: String,
        deepCrawlConfig: DeepCrawlConfig,
        crawlConfig: CrawlConfig,
    ): Flow<DeepCrawlProgress> =
        repository.deepCrawl(startUrl, deepCrawlConfig, crawlConfig)
}
