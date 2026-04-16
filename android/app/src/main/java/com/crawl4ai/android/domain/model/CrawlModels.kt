package com.crawl4ai.android.domain.model

import kotlinx.serialization.Serializable

// ── Core result types ─────────────────────────────────────────────────────

@Serializable
data class CrawlResponse(
    val success: Boolean = false,
    val url: String = "",
    val html: String = "",
    val cleanedHtml: String = "",
    val markdown: MarkdownResult = MarkdownResult(),
    val extractedContent: String = "",
    val media: Map<String, List<MediaItem>> = emptyMap(),
    val links: Map<String, List<LinkItem>> = emptyMap(),
    val tables: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap(),
    val statusCode: Int = 0,
    val responseHeaders: Map<String, String> = emptyMap(),
    val errorMessage: String = "",
    val cacheStatus: String = "miss",
    val headFingerprint: String = "",
    /** Timestamp when this result was fetched (epoch ms, set by Kotlin layer) */
    val fetchedAt: Long = System.currentTimeMillis(),
)

@Serializable
data class MarkdownResult(
    val rawMarkdown: String = "",
    val markdownWithCitations: String = "",
    val referencesMarkdown: String = "",
    val fitMarkdown: String = "",
    val fitHtml: String = "",
)

@Serializable
data class MediaItem(
    val url: String,
    val alt: String = "",
    val title: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val type: String = "image",
)

@Serializable
data class LinkItem(
    val url: String,
    val text: String = "",
    val title: String = "",
)

@Serializable
data class ExtractionResult(
    val success: Boolean = false,
    val data: String = "",       // JSON string of extracted data
    val strategy: String = "",
    val error: String = "",
)

// ── Configuration models ──────────────────────────────────────────────────

/**
 * Maps to CrawlerRunConfig on the Python side.
 */
@Serializable
data class CrawlConfig(
    val cacheMode: CacheMode = CacheMode.ENABLED,
    val wordCountThreshold: Int = 10,
    val timeoutMs: Int = 30_000,
    val screenshot: Boolean = false,
    val pdf: Boolean = false,
    val excludeExternalLinks: Boolean = false,
    val headers: Map<String, String> = emptyMap(),
    val cookies: List<Map<String, String>> = emptyList(),
    val contentFilter: ContentFilterConfig? = null,
    val extractionStrategy: ExtractionConfig? = null,
    val markdownOptions: MarkdownOptions = MarkdownOptions(),
)

@Serializable
data class ContentFilterConfig(
    val type: String = "pruning",   // "pruning" | "bm25"
    val threshold: Float = 0.48f,
    val query: String? = null,      // required for BM25
)

@Serializable
data class MarkdownOptions(
    val generateCitations: Boolean = true,
    val generateFitMarkdown: Boolean = false,
)

@Serializable
data class ExtractionConfig(
    val type: ExtractionStrategyType = ExtractionStrategyType.CSS,
    val schema: String? = null,        // JSON schema string
    val pattern: String? = null,       // Regex pattern (for REGEX type)
    val provider: String? = null,      // LLM provider (for LLM type)
    val apiKey: String? = null,
    val instruction: String? = null,
    val inputFormat: String = "html",  // "html" | "markdown" | "fit_markdown"
)

@Serializable
data class DeepCrawlConfig(
    val strategy: DeepCrawlStrategy = DeepCrawlStrategy.BFS,
    val maxDepth: Int = 3,
    val maxPages: Int = 50,
    val urlPatterns: List<String> = emptyList(),
    val allowedDomains: List<String> = emptyList(),
    val blockedDomains: List<String> = emptyList(),
    val scorerType: String? = null,
    val scorerKeywords: List<String> = emptyList(),
)

// ── Enumerations ──────────────────────────────────────────────────────────

enum class CacheMode(val pythonKey: String, val displayName: String) {
    ENABLED("enabled", "Enabled"),
    DISABLED("disabled", "Disabled"),
    BYPASS("bypass", "Bypass"),
    WRITE_ONLY("write_only", "Write Only"),
    READ_ONLY("read_only", "Read Only"),
}

enum class ExtractionStrategyType(val pythonKey: String, val displayName: String) {
    CSS("css", "CSS Selectors"),
    XPATH("xpath", "XPath"),
    REGEX("regex", "Regular Expression"),
    LLM("llm", "LLM (AI)"),
    NONE("none", "None"),
}

enum class DeepCrawlStrategy(val pythonKey: String, val displayName: String) {
    BFS("bfs", "Breadth-First (BFS)"),
    DFS("dfs", "Depth-First (DFS)"),
    BEST_FIRST("bff", "Best-First (Score-based)"),
}

// ── Session / status models ───────────────────────────────────────────────

/**
 * Represents a crawl job that is currently running or queued.
 */
data class CrawlJob(
    val id: String,
    val url: String,
    val config: CrawlConfig,
    val status: CrawlJobStatus = CrawlJobStatus.QUEUED,
    val progressPercent: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val resultId: Long? = null,
    val errorMessage: String = "",
)

enum class CrawlJobStatus {
    QUEUED, RUNNING, PAUSED, COMPLETED, FAILED, CANCELLED
}

/**
 * Real-time progress update emitted during a deep crawl.
 */
data class DeepCrawlProgress(
    val sessionId: String,
    val strategy: DeepCrawlStrategy,
    val currentUrl: String = "",
    val pagesVisited: Int = 0,
    val pagesRemaining: Int = 0,
    val maxPages: Int = 0,
    val currentDepth: Int = 0,
    val maxDepth: Int = 0,
    val latestResult: CrawlResponse? = null,
)
