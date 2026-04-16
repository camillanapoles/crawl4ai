package com.crawl4ai.android.data.repository

import com.crawl4ai.android.bridge.PythonBridge
import com.crawl4ai.android.data.local.db.dao.CrawlHistoryDao
import com.crawl4ai.android.data.local.db.dao.CrawlResultDao
import com.crawl4ai.android.data.local.db.entity.CrawlHistoryEntity
import com.crawl4ai.android.data.local.db.entity.CrawlResultEntity
import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.model.DeepCrawlConfig
import com.crawl4ai.android.domain.model.DeepCrawlProgress
import com.crawl4ai.android.domain.model.ExtractionConfig
import com.crawl4ai.android.domain.model.ExtractionResult
import com.crawl4ai.android.domain.model.MarkdownResult
import com.crawl4ai.android.domain.repository.CrawlRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * [CrawlRepository] implementation.
 *
 * - Delegates HTTP crawling to [PythonBridge] (crawl4ai Python engine).
 * - Persists results to [CrawlResultDao] and [CrawlHistoryDao] (Room).
 */
class CrawlRepositoryImpl @Inject constructor(
    private val bridge: PythonBridge,
    private val crawlResultDao: CrawlResultDao,
    private val crawlHistoryDao: CrawlHistoryDao,
) : CrawlRepository {

    override suspend fun crawlUrl(url: String, config: CrawlConfig): Result<CrawlResponse> {
        val startTime = System.currentTimeMillis()
        return bridge.crawlUrl(url, config).also { result ->
            val duration = System.currentTimeMillis() - startTime
            result.onSuccess { response ->
                val resultId = crawlResultDao.insert(response.toEntity())
                crawlHistoryDao.insert(
                    CrawlHistoryEntity(
                        url = url,
                        configJson = "{}",
                        resultId = resultId,
                        durationMs = duration,
                        success = response.success,
                        errorMessage = response.errorMessage,
                        dataSizeBytes = (response.html.length + response.markdown.rawMarkdown.length).toLong(),
                    )
                )
            }
            result.onFailure {
                crawlHistoryDao.insert(
                    CrawlHistoryEntity(
                        url = url,
                        durationMs = duration,
                        success = false,
                        errorMessage = it.message ?: "Unknown error",
                    )
                )
            }
        }
    }

    override suspend fun crawlUrls(
        urls: List<String>,
        config: CrawlConfig,
    ): Result<List<CrawlResponse>> = bridge.crawlUrls(urls, config).also { result ->
        result.onSuccess { responses ->
            responses.forEach { response ->
                val resultId = crawlResultDao.insert(response.toEntity())
                crawlHistoryDao.insert(
                    CrawlHistoryEntity(
                        url = response.url,
                        resultId = resultId,
                        success = response.success,
                        errorMessage = response.errorMessage,
                        crawlType = "multi",
                    )
                )
            }
        }
    }

    override fun deepCrawl(
        startUrl: String,
        deepCrawlConfig: DeepCrawlConfig,
        crawlConfig: CrawlConfig,
    ): Flow<DeepCrawlProgress> {
        var pagesVisited = 0
        return bridge.deepCrawlFlow(startUrl, deepCrawlConfig, crawlConfig).map { response ->
            pagesVisited++
            // Persist each page result
            crawlResultDao.insert(response.toEntity())
            DeepCrawlProgress(
                sessionId = startUrl,
                strategy = deepCrawlConfig.strategy,
                currentUrl = response.url,
                pagesVisited = pagesVisited,
                maxPages = deepCrawlConfig.maxPages,
                maxDepth = deepCrawlConfig.maxDepth,
                latestResult = response,
            )
        }
    }

    override suspend fun processHtml(
        html: String,
        url: String,
        config: CrawlConfig,
    ): Result<CrawlResponse> = bridge.processHtml(html, url, config)

    override suspend fun extractContent(
        html: String,
        strategy: ExtractionConfig,
    ): Result<ExtractionResult> = bridge.extractContent(html, strategy)

    override suspend fun generateMarkdown(
        html: String,
        config: CrawlConfig,
    ): Result<MarkdownResult> = bridge.generateMarkdown(html, config)

    override suspend fun chunkText(
        text: String,
        strategyType: String,
        chunkSize: Int,
    ): Result<List<String>> = bridge.chunkText(text, strategyType, chunkSize)

    override suspend fun seedUrls(
        startUrl: String,
        maxDepth: Int,
        maxUrls: Int,
    ): Result<List<String>> = bridge.seedUrls(startUrl, maxDepth, maxUrls)

    override suspend fun initEngine(): Result<Boolean> = bridge.initEngine()

    override suspend fun getVersion(): Result<String> = bridge.getVersion()
}

// ── Extension: CrawlResponse → CrawlResultEntity ─────────────────────────

private fun CrawlResponse.toEntity() = CrawlResultEntity(
    url = url,
    statusCode = statusCode,
    success = success,
    html = html,
    cleanedHtml = cleanedHtml,
    rawMarkdown = markdown.rawMarkdown,
    markdownWithCitations = markdown.markdownWithCitations,
    referencesMarkdown = markdown.referencesMarkdown,
    fitMarkdown = markdown.fitMarkdown,
    extractedContent = extractedContent,
    errorMessage = errorMessage,
    cacheStatus = cacheStatus,
    headFingerprint = headFingerprint,
)
