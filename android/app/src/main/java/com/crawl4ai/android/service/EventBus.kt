package com.crawl4ai.android.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Application-wide event bus built on [SharedFlow].
 *
 * All SDLC runtime events are published here and consumed by:
 * - ViewModels (UI state updates)
 * - CrawlForegroundService (notification updates)
 * - WorkManager workers (job lifecycle)
 *
 * Events are enumerated below.  Publishers call [emit]; collectors call
 * [events] from a coroutine scope.
 */
@Singleton
class EventBus @Inject constructor() {

    private val _events = MutableSharedFlow<CrawlEvent>(
        replay = 0,
        extraBufferCapacity = 64,
    )

    /** Collect all emitted events. */
    val events: SharedFlow<CrawlEvent> = _events.asSharedFlow()

    /** Emit an event to all active collectors. Non-suspending (uses tryEmit). */
    fun emit(event: CrawlEvent) {
        _events.tryEmit(event)
    }
}

// ── Event hierarchy ────────────────────────────────────────────────────────

sealed interface CrawlEvent {

    // ── Crawl lifecycle ─────────────────────────────────────────────────────

    data class CrawlStarted(
        val jobId: String,
        val url: String,
    ) : CrawlEvent

    data class CrawlProgress(
        val jobId: String,
        val percent: Int,
        val currentStep: String = "",
    ) : CrawlEvent

    data class CrawlCompleted(
        val jobId: String,
        val url: String,
        val resultId: Long,
        val success: Boolean,
        val durationMs: Long,
    ) : CrawlEvent

    data class CrawlFailed(
        val jobId: String,
        val url: String,
        val error: String,
    ) : CrawlEvent

    data class CrawlCancelled(
        val jobId: String,
    ) : CrawlEvent

    // ── Deep crawl ─────────────────────────────────────────────────────────

    data class DeepCrawlPageDiscovered(
        val sessionId: String,
        val url: String,
        val depth: Int,
    ) : CrawlEvent

    data class DeepCrawlPageCrawled(
        val sessionId: String,
        val url: String,
        val depth: Int,
        val pagesVisited: Int,
        val pagesRemaining: Int,
    ) : CrawlEvent

    data class DeepCrawlCompleted(
        val sessionId: String,
        val totalPages: Int,
        val durationMs: Long,
    ) : CrawlEvent

    data class DeepCrawlPaused(val sessionId: String) : CrawlEvent
    data class DeepCrawlResumed(val sessionId: String) : CrawlEvent

    // ── Cache ──────────────────────────────────────────────────────────────

    data class CacheHit(val url: String, val ageMs: Long) : CrawlEvent
    data class CacheMiss(val url: String) : CrawlEvent
    data class CacheCleared(val entriesRemoved: Int) : CrawlEvent

    // ── Extraction ────────────────────────────────────────────────────────

    data class ExtractionCompleted(
        val resultId: Long,
        val strategy: String,
        val itemCount: Int,
    ) : CrawlEvent

    // ── System / device ───────────────────────────────────────────────────

    data class NetworkStateChanged(val isConnected: Boolean, val isWifi: Boolean) : CrawlEvent
    data class MemoryWarning(val usagePercent: Int) : CrawlEvent
    data class BatteryLow(val levelPercent: Int) : CrawlEvent

    // ── Configuration ─────────────────────────────────────────────────────

    data class ConfigChanged(val configId: Long, val field: String = "") : CrawlEvent
    data class DefaultConfigChanged(val configId: Long) : CrawlEvent

    // ── Python engine ─────────────────────────────────────────────────────

    object PythonEngineReady : CrawlEvent
    data class PythonEngineError(val error: String) : CrawlEvent
}
