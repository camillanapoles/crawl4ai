package com.crawl4ai.android.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawl4ai.android.data.local.db.dao.CrawlHistoryDao
import com.crawl4ai.android.data.local.db.entity.CrawlHistoryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val deleteConfirmId: Long? = null,
    val showClearAllConfirm: Boolean = false,
    val snackbarMessage: String = "",
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: CrawlHistoryDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")

    /**
     * History list, live-updating from Room.
     * Switches between filtered search results and full history based on query.
     */
    val history: StateFlow<List<CrawlHistoryEntity>> = _query
        .debounce(300)
        .flatMapLatest { q ->
            if (q.isBlank()) dao.getAllFlow() else dao.searchFlow(q)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChanged(query: String) {
        _query.value = query
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun onDeleteRequest(id: Long) {
        _uiState.value = _uiState.value.copy(deleteConfirmId = id)
    }

    fun onDeleteConfirmed() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            dao.deleteById(id)
            _uiState.value = _uiState.value.copy(
                deleteConfirmId = null,
                snackbarMessage = "Entry deleted",
            )
        }
    }

    fun onDeleteDismissed() {
        _uiState.value = _uiState.value.copy(deleteConfirmId = null)
    }

    fun onClearAllRequest() {
        _uiState.value = _uiState.value.copy(showClearAllConfirm = true)
    }

    fun onClearAllConfirmed() {
        viewModelScope.launch {
            dao.deleteAll()
            _uiState.value = _uiState.value.copy(
                showClearAllConfirm = false,
                snackbarMessage = "History cleared",
            )
        }
    }

    fun onClearAllDismissed() {
        _uiState.value = _uiState.value.copy(showClearAllConfirm = false)
    }

    fun onSnackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = "")
    }
}
