package com.crawl4ai.android.ui.screens.deepcrawl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlJobStatus
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.model.DeepCrawlConfig
import com.crawl4ai.android.domain.model.DeepCrawlProgress
import com.crawl4ai.android.domain.model.DeepCrawlStrategy
import com.crawl4ai.android.domain.usecase.DeepCrawlUseCase
import com.crawl4ai.android.service.CrawlEvent
import com.crawl4ai.android.service.EventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class DeepCrawlUiState(
    val url: String = "",
    val isValidUrl: Boolean = false,
    val strategy: DeepCrawlStrategy = DeepCrawlStrategy.BFS,
    val maxDepth: Int = 3,
    val maxPages: Int = 50,
    val status: CrawlJobStatus = CrawlJobStatus.QUEUED,
    val pagesVisited: Int = 0,
    val currentUrl: String = "",
    val currentDepth: Int = 0,
    val results: List<CrawlResponse> = emptyList(),
    val error: String = "",
)

@HiltViewModel
class DeepCrawlViewModel @Inject constructor(
    private val deepCrawlUseCase: DeepCrawlUseCase,
    private val eventBus: EventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeepCrawlUiState())
    val uiState: StateFlow<DeepCrawlUiState> = _uiState.asStateFlow()

    private var crawlJob: Job? = null

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(
            url = url,
            isValidUrl = url.matches(Regex("https?://.*")),
        )
    }

    fun onStrategyChanged(strategy: DeepCrawlStrategy) {
        _uiState.value = _uiState.value.copy(strategy = strategy)
    }

    fun onMaxDepthChanged(depth: Int) {
        _uiState.value = _uiState.value.copy(maxDepth = depth)
    }

    fun onMaxPagesChanged(pages: Int) {
        _uiState.value = _uiState.value.copy(maxPages = pages)
    }

    fun startDeepCrawl() {
        val state = _uiState.value
        if (!state.isValidUrl || state.status == CrawlJobStatus.RUNNING) return

        val sessionId = UUID.randomUUID().toString()
        _uiState.value = state.copy(
            status = CrawlJobStatus.RUNNING,
            results = emptyList(),
            pagesVisited = 0,
            error = "",
        )

        crawlJob = viewModelScope.launch {
            eventBus.emit(CrawlEvent.CrawlStarted(sessionId, state.url))

            val deepConfig = DeepCrawlConfig(
                strategy = state.strategy,
                maxDepth = state.maxDepth,
                maxPages = state.maxPages,
            )

            try {
                deepCrawlUseCase(state.url, deepConfig, CrawlConfig())
                    .collect { progress ->
                        _uiState.value = _uiState.value.copy(
                            pagesVisited = progress.pagesVisited,
                            currentUrl = progress.currentUrl,
                            currentDepth = progress.currentDepth,
                            results = if (progress.latestResult != null)
                                _uiState.value.results + progress.latestResult
                            else
                                _uiState.value.results,
                        )
                        eventBus.emit(
                            CrawlEvent.DeepCrawlPageCrawled(
                                sessionId = sessionId,
                                url = progress.currentUrl,
                                depth = progress.currentDepth,
                                pagesVisited = progress.pagesVisited,
                                pagesRemaining = progress.pagesRemaining,
                            )
                        )
                    }

                _uiState.value = _uiState.value.copy(status = CrawlJobStatus.COMPLETED)
                eventBus.emit(
                    CrawlEvent.DeepCrawlCompleted(
                        sessionId = sessionId,
                        totalPages = _uiState.value.pagesVisited,
                        durationMs = 0L,
                    )
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = CrawlJobStatus.FAILED,
                    error = e.message ?: "Deep crawl failed",
                )
            }
        }
    }

    fun cancelCrawl() {
        crawlJob?.cancel()
        _uiState.value = _uiState.value.copy(status = CrawlJobStatus.CANCELLED)
    }
}
