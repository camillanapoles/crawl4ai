package com.crawl4ai.android.ui.screens.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawl4ai.android.data.local.db.entity.CrawlConfigEntity
import com.crawl4ai.android.domain.repository.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfigUiState(
    val showCreateDialog: Boolean = false,
    val newConfigName: String = "",
    val newConfigDesc: String = "",
    val nameError: String = "",
    val deleteConfirmId: Long? = null,
    val snackbarMessage: String = "",
)

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConfigUiState())
    val uiState: StateFlow<ConfigUiState> = _uiState.asStateFlow()

    val configs: StateFlow<List<CrawlConfigEntity>> = configRepository
        .getAllConfigs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Create dialog ──────────────────────────────────────────────────────

    fun onCreateClick() {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = true,
            newConfigName = "",
            newConfigDesc = "",
            nameError = "",
        )
    }

    fun onNewNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(newConfigName = name, nameError = "")
    }

    fun onNewDescChanged(desc: String) {
        _uiState.value = _uiState.value.copy(newConfigDesc = desc)
    }

    fun onCreateConfirmed() {
        val name = _uiState.value.newConfigName.trim()
        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(nameError = "Name is required")
            return
        }
        viewModelScope.launch {
            configRepository.saveConfig(
                CrawlConfigEntity(
                    name = name,
                    description = _uiState.value.newConfigDesc.trim(),
                )
            )
            _uiState.value = _uiState.value.copy(
                showCreateDialog = false,
                snackbarMessage = "Config \"$name\" saved",
            )
        }
    }

    fun onCreateDismissed() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    // ── Set default ────────────────────────────────────────────────────────

    fun onSetDefault(id: Long) {
        viewModelScope.launch {
            configRepository.setDefault(id)
            _uiState.value = _uiState.value.copy(snackbarMessage = "Default config updated")
        }
    }

    // ── Delete ─────────────────────────────────────────────────────────────

    fun onDeleteRequest(id: Long) {
        _uiState.value = _uiState.value.copy(deleteConfirmId = id)
    }

    fun onDeleteConfirmed() {
        val id = _uiState.value.deleteConfirmId ?: return
        viewModelScope.launch {
            configRepository.deleteConfig(id)
            _uiState.value = _uiState.value.copy(
                deleteConfirmId = null,
                snackbarMessage = "Config deleted",
            )
        }
    }

    fun onDeleteDismissed() {
        _uiState.value = _uiState.value.copy(deleteConfirmId = null)
    }

    fun onSnackbarShown() {
        _uiState.value = _uiState.value.copy(snackbarMessage = "")
    }
}
