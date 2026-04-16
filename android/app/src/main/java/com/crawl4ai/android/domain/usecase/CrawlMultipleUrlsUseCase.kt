package com.crawl4ai.android.domain.usecase

import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.repository.CrawlRepository
import javax.inject.Inject

/** Crawl multiple URLs concurrently. */
class CrawlMultipleUrlsUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    suspend operator fun invoke(
        urls: List<String>,
        config: CrawlConfig,
    ): Result<List<CrawlResponse>> = repository.crawlUrls(urls, config)
}
