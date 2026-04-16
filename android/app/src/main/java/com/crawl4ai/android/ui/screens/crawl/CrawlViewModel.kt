package com.crawl4ai.android.ui.screens.crawl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawl4ai.android.domain.model.CacheMode
import com.crawl4ai.android.domain.model.CrawlConfig
import com.crawl4ai.android.domain.model.CrawlJobStatus
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.usecase.CrawlSingleUrlUseCase
import com.crawl4ai.android.service.CrawlEvent
import com.crawl4ai.android.service.EventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CrawlUiState(
    val url: String = "",
    val isValidUrl: Boolean = false,
    val status: CrawlJobStatus = CrawlJobStatus.QUEUED,
    val isLoading: Boolean = false,
    val result: CrawlResponse? = null,
    val resultId: Long? = null,
    val error: String = "",
    // Config options
    val cacheMode: CacheMode = CacheMode.ENABLED,
    val wordCountThreshold: Int = 10,
    val timeoutSeconds: Int = 30,
    val screenshot: Boolean = false,
    val excludeExternalLinks: Boolean = false,
)

@HiltViewModel
class CrawlViewModel @Inject constructor(
    private val crawlSingleUrlUseCase: CrawlSingleUrlUseCase,
    private val eventBus: EventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrawlUiState())
    val uiState: StateFlow<CrawlUiState> = _uiState.asStateFlow()

    fun onUrlChanged(url: String) {
        _uiState.value = _uiState.value.copy(
            url = url,
            isValidUrl = url.matches(Regex("https?://.*")),
            error = "",
        )
    }

    fun onCacheModeChanged(mode: CacheMode) {
        _uiState.value = _uiState.value.copy(cacheMode = mode)
    }

    fun onWordThresholdChanged(value: Int) {
        _uiState.value = _uiState.value.copy(wordCountThreshold = value)
    }

    fun onTimeoutChanged(seconds: Int) {
        _uiState.value = _uiState.value.copy(timeoutSeconds = seconds)
    }

    fun onScreenshotChanged(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(screenshot = enabled)
    }

    fun startCrawl() {
        val state = _uiState.value
        if (!state.isValidUrl || state.isLoading) return

        val jobId = UUID.randomUUID().toString()
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            status = CrawlJobStatus.RUNNING,
            result = null,
            error = "",
        )

        val config = CrawlConfig(
            cacheMode = state.cacheMode,
            wordCountThreshold = state.wordCountThreshold,
            timeoutMs = state.timeoutSeconds * 1_000,
            screenshot = state.screenshot,
            excludeExternalLinks = state.excludeExternalLinks,
        )

        viewModelScope.launch {
            eventBus.emit(CrawlEvent.CrawlStarted(jobId, state.url))

            crawlSingleUrlUseCase(state.url, config)
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        status = CrawlJobStatus.COMPLETED,
                        result = response,
                    )
                    eventBus.emit(
                        CrawlEvent.CrawlCompleted(
                            jobId = jobId,
                            url = state.url,
                            resultId = 0L,
                            success = response.success,
                            durationMs = 0L,
                        )
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        status = CrawlJobStatus.FAILED,
                        error = error.message ?: "Unknown error",
                    )
                    eventBus.emit(CrawlEvent.CrawlFailed(jobId, state.url, error.message ?: ""))
                }
        }
    }

    fun reset() {
        _uiState.value = CrawlUiState()
    }
}
