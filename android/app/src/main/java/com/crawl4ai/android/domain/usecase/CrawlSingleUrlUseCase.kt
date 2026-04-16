package com.crawl4ai.android.domain.usecase

import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.repository.CrawlRepository
import javax.inject.Inject

/** Crawl a single URL with the given configuration. */
class CrawlSingleUrlUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    suspend operator fun invoke(url: String, config: CrawlConfig): Result<CrawlResponse> =
        repository.crawlUrl(url, config)
}
