package com.crawl4ai.android.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crawl4ai.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        if (uiState.snackbarMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.snackbarMessage)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_settings)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Appearance ─────────────────────────────────────────────────
            SettingsSectionHeader("Appearance")
            ThemeSelector(current = settings.theme, onChange = viewModel::setTheme)
            Divider()

            // ── Cache ──────────────────────────────────────────────────────
            SettingsSectionHeader("Cache")
            ListItem(
                headlineContent = { Text("Enable cache") },
                supportingContent = { Text("Reuse previously fetched pages") },
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                trailingContent = {
                    Switch(checked = settings.cacheEnabled, onCheckedChange = viewModel::setCacheEnabled)
                },
            )
            ListItem(
                headlineContent = { Text("Auto-clear on exit") },
                supportingContent = { Text("Remove cached pages when app closes") },
                leadingContent = { Icon(Icons.Default.Storage, contentDescription = null) },
                trailingContent = {
                    Switch(checked = settings.autoClearCache, onCheckedChange = viewModel::setAutoClearCache)
                },
            )
            Divider()

            // ── Notifications ──────────────────────────────────────────────
            SettingsSectionHeader("Notifications")
            ListItem(
                headlineContent = { Text("Show crawl notifications") },
                leadingContent = { Icon(Icons.Default.Notifications, contentDescription = null) },
                trailingContent = {
                    Switch(checked = settings.showNotifications, onCheckedChange = viewModel::setShowNotifications)
                },
            )
            Divider()

            // ── History ────────────────────────────────────────────────────
            SettingsSectionHeader("History")
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "History limit: ${settings.historyLimit} entries",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = settings.historyLimit.toFloat(),
                    onValueChange = { viewModel.setHistoryLimit(it.toInt()) },
                    valueRange = 20f..500f,
                    steps = 23,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Divider()

            // ── Crawl defaults ─────────────────────────────────────────────
            SettingsSectionHeader("Crawl Defaults")
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    "Default timeout: ${settings.defaultTimeoutSec}s",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Slider(
                    value = settings.defaultTimeoutSec.toFloat(),
                    onValueChange = { viewModel.setDefaultTimeoutSec(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "Word count threshold: ${settings.defaultWordThreshold}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Slider(
                    value = settings.defaultWordThreshold.toFloat(),
                    onValueChange = { viewModel.setDefaultWordThreshold(it.toInt()) },
                    valueRange = 0f..200f,
                    steps = 39,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Divider()

            // ── Remote Docker ──────────────────────────────────────────────
            SettingsSectionHeader("Remote Docker (crawl4ai server)")
            ListItem(
                headlineContent = { Text("Use remote Docker server") },
                supportingContent = { Text("Delegate crawls to a hosted crawl4ai instance") },
                leadingContent = { Icon(Icons.Default.Cloud, contentDescription = null) },
                trailingContent = {
                    Switch(checked = settings.useRemoteDocker, onCheckedChange = viewModel::setUseRemoteDocker)
                },
            )
            if (settings.useRemoteDocker) {
                var hostValue by remember(settings.dockerHost) { mutableStateOf(settings.dockerHost) }
                var portValue by remember(settings.dockerPort) { mutableStateOf(settings.dockerPort.toString()) }
                var tokenValue by remember(settings.dockerToken) { mutableStateOf(settings.dockerToken) }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = hostValue,
                        onValueChange = { hostValue = it; viewModel.setDockerHost(it) },
                        label = { Text("Host") },
                        placeholder = { Text("192.168.1.100") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = portValue,
                        onValueChange = { v ->
                            portValue = v
                            v.toIntOrNull()?.let { viewModel.setDockerPort(it) }
                        },
                        label = { Text("Port") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = tokenValue,
                        onValueChange = { tokenValue = it; viewModel.setDockerToken(it) },
                        label = { Text("API Token") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun ThemeSelector(current: String, onChange: (String) -> Unit) {
    val options = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Brightness4,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text("Theme:", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(end = 4.dp))
        options.forEach { (key, label) ->
            FilterChip(
                selected = current == key,
                onClick = { onChange(key) },
                label = { Text(label) },
            )
        }
    }
}
