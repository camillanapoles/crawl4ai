package com.crawl4ai.android.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawl4ai.android.data.local.db.dao.CrawlHistoryDao
import com.crawl4ai.android.data.local.db.entity.CrawlHistoryEntity
import com.crawl4ai.android.domain.repository.CrawlRepository
import com.crawl4ai.android.service.EventBus
import com.crawl4ai.android.service.CrawlEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val crawl4aiVersion: String = "",
    val engineReady: Boolean = false,
    val totalCrawls: Int = 0,
    val successfulCrawls: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val crawlRepository: CrawlRepository,
    private val historyDao: CrawlHistoryDao,
    private val eventBus: EventBus,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentHistory: StateFlow<List<CrawlHistoryEntity>> =
        historyDao.getAllFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        initEngine()
        collectEvents()
    }

    private fun initEngine() {
        viewModelScope.launch {
            crawlRepository.getVersion().onSuccess { version ->
                _uiState.value = _uiState.value.copy(
                    crawl4aiVersion = version,
                    isLoading = false,
                )
            }
            crawlRepository.initEngine().onSuccess { ready ->
                _uiState.value = _uiState.value.copy(engineReady = ready)
                if (ready) eventBus.emit(CrawlEvent.PythonEngineReady)
            }.onFailure { err ->
                eventBus.emit(CrawlEvent.PythonEngineError(err.message ?: ""))
            }

            val total = historyDao.count()
            val success = historyDao.successCount()
            _uiState.value = _uiState.value.copy(
                totalCrawls = total,
                successfulCrawls = success,
                isLoading = false,
            )
        }
    }

    private fun collectEvents() {
        viewModelScope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is CrawlEvent.CrawlCompleted -> {
                        val total = historyDao.count()
                        val success = historyDao.successCount()
                        _uiState.value = _uiState.value.copy(
                            totalCrawls = total,
                            successfulCrawls = success,
                        )
                    }
                    is CrawlEvent.PythonEngineReady -> {
                        _uiState.value = _uiState.value.copy(engineReady = true)
                    }
                    else -> Unit
                }
            }
        }
    }
}
