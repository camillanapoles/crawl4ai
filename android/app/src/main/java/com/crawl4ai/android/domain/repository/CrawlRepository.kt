package com.crawl4ai.android.domain.repository

import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.model.DeepCrawlConfig
import com.crawl4ai.android.domain.model.DeepCrawlProgress
import com.crawl4ai.android.domain.model.ExtractionConfig
import com.crawl4ai.android.domain.model.ExtractionResult
import com.crawl4ai.android.domain.model.MarkdownResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for all crawling operations.
 * Implemented by [CrawlRepositoryImpl] which delegates to [PythonBridge].
 */
interface CrawlRepository {

    /** Crawl a single URL and cache the result. */
    suspend fun crawlUrl(url: String, config: CrawlConfig): Result<CrawlResponse>

    /** Crawl multiple URLs concurrently. */
    suspend fun crawlUrls(urls: List<String>, config: CrawlConfig): Result<List<CrawlResponse>>

    /**
     * Execute a deep crawl starting from [startUrl].
     * Emits one [DeepCrawlProgress] per page discovered/crawled.
     */
    fun deepCrawl(
        startUrl: String,
        deepCrawlConfig: DeepCrawlConfig,
        crawlConfig: CrawlConfig,
    ): Flow<DeepCrawlProgress>

    /** Process HTML already fetched by Android WebView. */
    suspend fun processHtml(html: String, url: String, config: CrawlConfig): Result<CrawlResponse>

    /** Run an extraction strategy on HTML. */
    suspend fun extractContent(html: String, strategy: ExtractionConfig): Result<ExtractionResult>

    /** Convert HTML to Markdown. */
    suspend fun generateMarkdown(html: String, config: CrawlConfig): Result<MarkdownResult>

    /** Split text into chunks. */
    suspend fun chunkText(
        text: String,
        strategyType: String = "regex",
        chunkSize: Int = 500,
    ): Result<List<String>>

    /** Discover URLs from a seed URL. */
    suspend fun seedUrls(startUrl: String, maxDepth: Int, maxUrls: Int): Result<List<String>>

    /** Pre-warm the Python engine. */
    suspend fun initEngine(): Result<Boolean>

    /** Get crawl4ai version string. */
    suspend fun getVersion(): Result<String>
}
