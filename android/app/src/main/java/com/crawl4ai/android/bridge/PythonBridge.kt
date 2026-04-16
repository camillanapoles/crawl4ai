package com.crawl4ai.android.bridge

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.PyException
import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.model.DeepCrawlConfig
import com.crawl4ai.android.domain.model.ExtractionConfig
import com.crawl4ai.android.domain.model.ExtractionResult
import com.crawl4ai.android.domain.model.MarkdownResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PythonBridge
 * ─────────────
 * Kotlin gateway to the crawl4ai Python runtime via Chaquopy.
 *
 * Every call:
 * 1. Serialises parameters to JSON strings (Chaquopy handles str ↔ String).
 * 2. Invokes the corresponding Python function in crawl4ai_bridge.py.
 * 3. Deserialises the JSON string result back to Kotlin domain models.
 * 4. All Python calls are dispatched on [Dispatchers.IO] to avoid
 *    blocking the main thread (Python GIL can block for seconds).
 */
@Singleton
class PythonBridge @Inject constructor(
    private val context: Context,
) {
    private val TAG = "PythonBridge"
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    // ── Module references (lazy — initialised on first use) ────────────────
    private val bridge by lazy {
        Python.getInstance().getModule("crawl4ai_bridge")
    }

    // ── Engine lifecycle ───────────────────────────────────────────────────

    /**
     * Pre-warm the Python engine (NLTK data download, import chains).
     * Should be called from a background coroutine early in app startup.
     */
    suspend fun initEngine(): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val result = bridge.callAttr("init_engine").toString()
            val obj = json.parseToJsonElement(result).jsonObject
            obj["ready"]?.jsonPrimitive?.boolean ?: false
        }.onFailure { Log.e(TAG, "initEngine failed", it) }
    }

    /**
     * Return the installed crawl4ai version string.
     */
    suspend fun getVersion(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val result = bridge.callAttr("get_version").toString()
            val obj = json.parseToJsonElement(result).jsonObject
            obj["version"]?.jsonPrimitive?.content ?: "unknown"
        }.onFailure { Log.e(TAG, "getVersion failed", it) }
    }

    // ── Crawling ───────────────────────────────────────────────────────────

    /**
     * Crawl a single URL and return the result.
     */
    suspend fun crawlUrl(url: String, config: CrawlConfig): Result<CrawlResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val configJson = ResultMapper.crawlConfigToJson(config)
                val resultJson = bridge.callAttr("crawl_url", url, configJson).toString()
                ResultMapper.jsonToCrawlResponse(resultJson)
            }.onFailure { Log.e(TAG, "crawlUrl failed for $url", it) }
        }

    /**
     * Crawl multiple URLs concurrently (managed by Python asyncio).
     */
    suspend fun crawlUrls(urls: List<String>, config: CrawlConfig): Result<List<CrawlResponse>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val urlsJson = kotlinx.serialization.json.Json.encodeToString(
                    kotlinx.serialization.builtins.ListSerializer(
                        kotlinx.serialization.builtins.serializer()
                    ),
                    urls
                )
                val configJson = ResultMapper.crawlConfigToJson(config)
                val resultJson = bridge.callAttr("crawl_urls", urlsJson, configJson).toString()
                ResultMapper.jsonToCrawlResponseList(resultJson)
            }.onFailure { Log.e(TAG, "crawlUrls failed", it) }
        }

    /**
     * Execute a deep crawl; emits one [CrawlResponse] per page as a [Flow].
     */
    fun deepCrawlFlow(
        startUrl: String,
        deepCrawlConfig: DeepCrawlConfig,
        crawlConfig: CrawlConfig,
    ): Flow<CrawlResponse> = flow {
        val strategyJson = ResultMapper.deepCrawlConfigToJson(deepCrawlConfig)
        val configJson = ResultMapper.crawlConfigToJson(crawlConfig)
        val resultJson = bridge.callAttr("deep_crawl", startUrl, strategyJson, configJson)
            .toString()
        ResultMapper.jsonToCrawlResponseList(resultJson).forEach { emit(it) }
    }.flowOn(Dispatchers.IO)

    // ── Post-processing ────────────────────────────────────────────────────

    /**
     * Run the full crawl4ai pipeline on HTML that was fetched by Android WebView.
     * Use this for JS-rendered pages where the browser already loaded the content.
     */
    suspend fun processHtml(
        html: String,
        url: String,
        config: CrawlConfig,
    ): Result<CrawlResponse> = withContext(Dispatchers.IO) {
        runCatching {
            val configJson = ResultMapper.crawlConfigToJson(config)
            val resultJson = bridge.callAttr("process_html", html, url, configJson).toString()
            ResultMapper.jsonToCrawlResponse(resultJson)
        }.onFailure { Log.e(TAG, "processHtml failed", it) }
    }

    /**
     * Run a content extraction strategy against raw HTML.
     */
    suspend fun extractContent(
        html: String,
        strategy: ExtractionConfig,
    ): Result<ExtractionResult> = withContext(Dispatchers.IO) {
        runCatching {
            val strategyJson = ResultMapper.extractionConfigToJson(strategy)
            val resultJson = bridge.callAttr("extract_content", html, strategyJson).toString()
            ResultMapper.jsonToExtractionResult(resultJson)
        }.onFailure { Log.e(TAG, "extractContent failed", it) }
    }

    /**
     * Convert HTML to Markdown.
     */
    suspend fun generateMarkdown(html: String, config: CrawlConfig): Result<MarkdownResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val configJson = ResultMapper.crawlConfigToJson(config)
                val resultJson = bridge.callAttr("generate_markdown", html, configJson).toString()
                ResultMapper.jsonToMarkdownResult(resultJson)
            }.onFailure { Log.e(TAG, "generateMarkdown failed", it) }
        }

    /**
     * Split text into chunks using the given strategy.
     */
    suspend fun chunkText(
        text: String,
        strategyType: String = "regex",
        chunkSize: Int = 500,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val strategyJson = """{"type":"$strategyType","chunk_size":$chunkSize}"""
            val resultJson = bridge.callAttr("chunk_text", text, strategyJson).toString()
            ResultMapper.jsonToStringList(resultJson, "chunks")
        }.onFailure { Log.e(TAG, "chunkText failed", it) }
    }

    /**
     * Discover URLs reachable from a seed URL.
     */
    suspend fun seedUrls(
        startUrl: String,
        maxDepth: Int = 2,
        maxUrls: Int = 100,
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val configJson = """{"max_depth":$maxDepth,"max_urls":$maxUrls}"""
            val resultJson = bridge.callAttr("seed_urls", startUrl, configJson).toString()
            ResultMapper.jsonToStringList(resultJson, "urls")
        }.onFailure { Log.e(TAG, "seedUrls failed", it) }
    }

    // ── Cache management ───────────────────────────────────────────────────

    /**
     * Return cache statistics from the Python SQLite cache.
     */
    suspend fun getCacheStats(): Result<CacheStats> = withContext(Dispatchers.IO) {
        runCatching {
            val resultJson = bridge.callAttr("get_cache_stats").toString()
            ResultMapper.jsonToCacheStats(resultJson)
        }.onFailure { Log.e(TAG, "getCacheStats failed", it) }
    }

    /**
     * Clear the cache. Pass a URL to clear only that entry, or null to clear all.
     */
    suspend fun clearCache(url: String? = null): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val resultJson = bridge.callAttr("clear_cache", url ?: "").toString()
            val obj = json.parseToJsonElement(resultJson).jsonObject
            obj["cleared"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        }.onFailure { Log.e(TAG, "clearCache failed", it) }
    }
}

/**
 * Simple data class for cache statistics returned from Python.
 */
data class CacheStats(
    val totalEntries: Int = 0,
    val sizeBytes: Long = 0L,
    val sizeMb: Float = 0f,
)
