package com.crawl4ai.android.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.crawl4ai.android.data.local.prefs.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "system",
    val cacheEnabled: Boolean = true,
    val autoClearCache: Boolean = false,
    val showNotifications: Boolean = true,
    val historyLimit: Int = 200,
    val defaultTimeoutSec: Int = 30,
    val defaultWordThreshold: Int = 10,
    val useRemoteDocker: Boolean = false,
    val dockerHost: String = "",
    val dockerPort: Int = 11235,
    val dockerToken: String = "",
    // Transient dialog state
    val dockerPortInput: String = "11235",
    val snackbarMessage: String = "",
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _ui = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _ui.asStateFlow()

    // Collect all preferences into one state (array-based combine for >5 flows)
    val settings: StateFlow<SettingsUiState> = combine(
        listOf(
            prefs.theme,
            prefs.cacheEnabled,
            prefs.autoClearCache,
            prefs.showNotifications,
            prefs.historyLimit,
            prefs.defaultTimeoutMs,
            prefs.defaultWordThreshold,
            prefs.useRemoteDocker,
            prefs.dockerHost,
            prefs.dockerPort,
            prefs.dockerToken,
        )
    ) { values ->
        SettingsUiState(
            theme = values[0] as String,
            cacheEnabled = values[1] as Boolean,
            autoClearCache = values[2] as Boolean,
            showNotifications = values[3] as Boolean,
            historyLimit = values[4] as Int,
            defaultTimeoutSec = (values[5] as Int) / 1000,
            defaultWordThreshold = values[6] as Int,
            useRemoteDocker = values[7] as Boolean,
            dockerHost = values[8] as String,
            dockerPort = values[9] as Int,
            dockerToken = values[10] as String,
            dockerPortInput = (values[9] as Int).toString(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    // ── Setters ────────────────────────────────────────────────────────────

    fun setTheme(value: String) = save { prefs.setTheme(value) }
    fun setCacheEnabled(value: Boolean) = save { prefs.setCacheEnabled(value) }
    fun setAutoClearCache(value: Boolean) = save { prefs.setAutoClearCache(value) }
    fun setShowNotifications(value: Boolean) = save { prefs.setShowNotifications(value) }
    fun setHistoryLimit(value: Int) = save { prefs.setHistoryLimit(value) }
    fun setDefaultTimeoutSec(value: Int) = save { prefs.setDefaultTimeoutMs(value * 1000) }
    fun setDefaultWordThreshold(value: Int) = save { prefs.setDefaultWordThreshold(value) }
    fun setUseRemoteDocker(value: Boolean) = save { prefs.setUseRemoteDocker(value) }
    fun setDockerHost(value: String) = save { prefs.setDockerHost(value) }
    fun setDockerPort(value: Int) = save { prefs.setDockerPort(value) }
    fun setDockerToken(value: String) = save { prefs.setDockerToken(value) }

    fun onSnackbarShown() {
        _ui.value = _ui.value.copy(snackbarMessage = "")
    }

    private fun save(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
                .onFailure { _ui.value = _ui.value.copy(snackbarMessage = "Failed to save setting") }
        }
    }
}
