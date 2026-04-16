package com.crawl4ai.android.domain.usecase

import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.ExtractionConfig
import com.crawl4ai.android.domain.model.ExtractionResult
import com.crawl4ai.android.domain.model.MarkdownResult
import com.crawl4ai.android.domain.repository.CrawlRepository
import javax.inject.Inject

/** Extract structured data from raw HTML using the given strategy. */
class ExtractContentUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    suspend operator fun invoke(html: String, strategy: ExtractionConfig): Result<ExtractionResult> =
        repository.extractContent(html, strategy)
}

/** Generate Markdown from raw HTML. */
class GenerateMarkdownUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    suspend operator fun invoke(html: String, config: CrawlConfig): Result<MarkdownResult> =
        repository.generateMarkdown(html, config)
}

/** Split a text string into chunks for LLM processing. */
class ChunkTextUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    suspend operator fun invoke(
        text: String,
        strategyType: String = "regex",
        chunkSize: Int = 500,
    ): Result<List<String>> = repository.chunkText(text, strategyType, chunkSize)
}

/** Discover URLs reachable from a seed URL. */
class SeedUrlsUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    suspend operator fun invoke(
        startUrl: String,
        maxDepth: Int = 2,
        maxUrls: Int = 100,
    ): Result<List<String>> = repository.seedUrls(startUrl, maxDepth, maxUrls)
}

/** Process HTML already fetched by Android WebView (JS-rendered pages). */
class ProcessHtmlUseCase @Inject constructor(
    private val repository: CrawlRepository,
) {
    suspend operator fun invoke(
        html: String,
        url: String,
        config: CrawlConfig,
    ) = repository.processHtml(html, url, config)
}
