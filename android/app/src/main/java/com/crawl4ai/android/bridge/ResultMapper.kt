package com.crawl4ai.android.bridge

import com.crawl4ai.android.domain.model.CacheMode
import com.crawl4ai.android.domain.model.ContentFilterConfig
import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.model.DeepCrawlConfig
import com.crawl4ai.android.domain.model.DeepCrawlStrategy
import com.crawl4ai.android.domain.model.ExtractionConfig
import com.crawl4ai.android.domain.model.ExtractionResult
import com.crawl4ai.android.domain.model.ExtractionStrategyType
import com.crawl4ai.android.domain.model.LinkItem
import com.crawl4ai.android.domain.model.MarkdownResult
import com.crawl4ai.android.domain.model.MediaItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * ResultMapper
 * ─────────────
 * Converts between:
 * • Kotlin domain models → JSON strings (for passing to Python)
 * • JSON strings from Python → Kotlin domain models
 *
 * Using kotlinx.serialization for JSON parsing; no Gson dependency.
 */
object ResultMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    // ── Kotlin → JSON (input to Python) ───────────────────────────────────

    fun crawlConfigToJson(config: CrawlConfig): String = buildJsonObject {
        put("cache_mode", config.cacheMode.pythonKey)
        put("word_count_threshold", config.wordCountThreshold)
        put("timeout", config.timeoutMs)
        put("screenshot", config.screenshot)
        put("pdf", config.pdf)
        put("exclude_external_links", config.excludeExternalLinks)
        // Content filter
        config.contentFilter?.let { filter ->
            putJsonObject("content_filter") {
                put("type", filter.type)
                put("threshold", filter.threshold)
                filter.query?.let { put("query", it) }
            }
        }
        // Extraction strategy
        config.extractionStrategy?.let { ext ->
            putJsonObject("extraction_strategy") {
                put("type", ext.type.pythonKey)
                ext.schema?.let { put("schema", it) }
                ext.pattern?.let { put("pattern", it) }
                ext.provider?.let { put("provider", it) }
                ext.instruction?.let { put("instruction", it) }
            }
        }
        // Headers
        if (config.headers.isNotEmpty()) {
            putJsonObject("headers") {
                config.headers.forEach { (k, v) -> put(k, v) }
            }
        }
    }.toString()

    fun deepCrawlConfigToJson(config: DeepCrawlConfig): String = buildJsonObject {
        put("type", config.strategy.pythonKey)
        put("max_depth", config.maxDepth)
        put("max_pages", config.maxPages)
        if (config.urlPatterns.isNotEmpty()) {
            // JSON array of url patterns
        }
        if (config.allowedDomains.isNotEmpty()) {
            // JSON array of allowed domains
        }
        config.scorerType?.let { put("scorer", buildJsonObject { put("type", it) }.toString()) }
    }.toString()

    fun extractionConfigToJson(config: ExtractionConfig): String = buildJsonObject {
        put("type", config.type.pythonKey)
        config.schema?.let { put("schema", it) }
        config.pattern?.let { put("pattern", it) }
        config.provider?.let { put("provider", it) }
        config.apiKey?.let { put("api_key", it) }
        config.instruction?.let { put("instruction", it) }
        put("input_format", config.inputFormat)
    }.toString()

    // ── JSON (from Python) → Kotlin models ────────────────────────────────

    fun jsonToCrawlResponse(jsonStr: String): CrawlResponse {
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        val mdObj = obj["markdown"]?.jsonObject

        return CrawlResponse(
            success = obj["success"]?.jsonPrimitive?.boolean ?: false,
            url = obj["url"]?.jsonPrimitive?.content ?: "",
            html = obj["html"]?.jsonPrimitive?.content ?: "",
            cleanedHtml = obj["cleaned_html"]?.jsonPrimitive?.content ?: "",
            markdown = MarkdownResult(
                rawMarkdown = mdObj?.get("raw_markdown")?.jsonPrimitive?.content ?: "",
                markdownWithCitations = mdObj?.get("markdown_with_citations")?.jsonPrimitive?.content ?: "",
                referencesMarkdown = mdObj?.get("references_markdown")?.jsonPrimitive?.content ?: "",
                fitMarkdown = mdObj?.get("fit_markdown")?.jsonPrimitive?.content ?: "",
            ),
            extractedContent = obj["extracted_content"]?.jsonPrimitive?.content ?: "",
            media = parseMedia(obj["media"]?.jsonObject),
            links = parseLinks(obj["links"]?.jsonObject),
            statusCode = obj["status_code"]?.jsonPrimitive?.int ?: 0,
            errorMessage = obj["error_message"]?.jsonPrimitive?.content ?: "",
            cacheStatus = obj["cache_status"]?.jsonPrimitive?.content ?: "miss",
        )
    }

    fun jsonToCrawlResponseList(jsonStr: String): List<CrawlResponse> {
        return try {
            val root = json.parseToJsonElement(jsonStr)
            // Handle both array and {"results": [...]} shapes
            val array = when {
                root is JsonObject && root.containsKey("results") ->
                    root["results"]?.jsonArray

                else -> root.jsonArray
            } ?: return emptyList()

            array.map { jsonToCrawlResponse(it.toString()) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun jsonToMarkdownResult(jsonStr: String): MarkdownResult {
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        return MarkdownResult(
            rawMarkdown = obj["raw_markdown"]?.jsonPrimitive?.content ?: "",
            markdownWithCitations = obj["markdown_with_citations"]?.jsonPrimitive?.content ?: "",
            referencesMarkdown = obj["references_markdown"]?.jsonPrimitive?.content ?: "",
            fitMarkdown = obj["fit_markdown"]?.jsonPrimitive?.content ?: "",
        )
    }

    fun jsonToExtractionResult(jsonStr: String): ExtractionResult {
        val obj = json.parseToJsonElement(jsonStr).jsonObject
        return ExtractionResult(
            success = obj["success"]?.jsonPrimitive?.boolean ?: false,
            data = obj["data"]?.toString() ?: "",
            strategy = obj["strategy"]?.jsonPrimitive?.content ?: "",
            error = obj["error"]?.jsonPrimitive?.content ?: "",
        )
    }

    fun jsonToStringList(jsonStr: String, key: String): List<String> {
        return try {
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            obj[key]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun jsonToCacheStats(jsonStr: String): CacheStats {
        return try {
            val obj = json.parseToJsonElement(jsonStr).jsonObject
            CacheStats(
                totalEntries = obj["total_entries"]?.jsonPrimitive?.int ?: 0,
                sizeBytes = obj["size_bytes"]?.jsonPrimitive?.long ?: 0L,
                sizeMb = obj["size_mb"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 0f,
            )
        } catch (e: Exception) {
            CacheStats()
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun parseMedia(mediaObj: JsonObject?): Map<String, List<MediaItem>> {
        if (mediaObj == null) return emptyMap()
        return buildMap {
            mediaObj.forEach { (key, value) ->
                try {
                    val items = value.jsonArray.mapNotNull { element ->
                        val item = element.jsonObject
                        MediaItem(
                            url = item["url"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            alt = item["alt"]?.jsonPrimitive?.content ?: "",
                            title = item["title"]?.jsonPrimitive?.content ?: "",
                            width = item["width"]?.jsonPrimitive?.int ?: 0,
                            height = item["height"]?.jsonPrimitive?.int ?: 0,
                        )
                    }
                    put(key, items)
                } catch (_: Exception) { /* skip malformed entries */ }
            }
        }
    }

    private fun parseLinks(linksObj: JsonObject?): Map<String, List<LinkItem>> {
        if (linksObj == null) return emptyMap()
        return buildMap {
            linksObj.forEach { (key, value) ->
                try {
                    val items = value.jsonArray.mapNotNull { element ->
                        val item = element.jsonObject
                        LinkItem(
                            url = item["href"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            text = item["text"]?.jsonPrimitive?.content ?: "",
                            title = item["title"]?.jsonPrimitive?.content ?: "",
                        )
                    }
                    put(key, items)
                } catch (_: Exception) { /* skip malformed entries */ }
            }
        }
    }
}
