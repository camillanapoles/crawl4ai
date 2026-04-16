package com.crawl4ai.android.ui.screens.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawl4ai.android.data.local.db.dao.CrawlResultDao
import com.crawl4ai.android.data.local.db.entity.CrawlResultEntity
import com.crawl4ai.android.domain.model.CrawlResponse
import com.crawl4ai.android.domain.model.MarkdownResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultsUiState(
    val isLoading: Boolean = true,
    val result: CrawlResultEntity? = null,
    val error: String = "",
    val selectedTab: ResultTab = ResultTab.MARKDOWN,
)

enum class ResultTab(val label: String) {
    MARKDOWN("Markdown"),
    HTML("HTML"),
    EXTRACTED("Extracted"),
    MEDIA("Media"),
    LINKS("Links"),
    RAW("Raw"),
}

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val dao: CrawlResultDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    fun load(resultId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val entity = dao.getById(resultId)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                result = entity,
                error = if (entity == null) "Result not found" else "",
            )
        }
    }

    fun selectTab(tab: ResultTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }
}
