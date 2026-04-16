package com.crawl4ai.android.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crawl4ai.android.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCrawlClick: () -> Unit,
    onDeepCrawlClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onResultClick: (Long) -> Unit,
    onSettingsClick: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val history by viewModel.recentHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Crawl4AI") },
            actions = {
                IconButton(onClick = onSettingsClick) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Engine status card ─────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                        } else {
                            Icon(
                                if (state.engineReady) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (state.engineReady)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error,
                            )
                        }
                        Column {
                            Text(
                                if (state.engineReady) "Engine Ready" else "Initialising…",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            if (state.crawl4aiVersion.isNotEmpty()) {
                                Text(
                                    "crawl4ai v${state.crawl4aiVersion}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }

            // ── Stats row ─────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total crawls",
                        value = state.totalCrawls.toString(),
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Successful",
                        value = state.successfulCrawls.toString(),
                    )
                }
            }

            // ── Quick action buttons ───────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ElevatedButton(
                        onClick = onCrawlClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null)
                        Text(" Crawl URL")
                    }
                    ElevatedButton(
                        onClick = onDeepCrawlClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Text(" Deep Crawl")
                    }
                }
            }

            // ── Recent crawls ─────────────────────────────────────────────
            item {
                Text(
                    "Recent crawls",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            if (history.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.placeholder_no_history),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            items(history.take(10)) { item ->
                ListItem(
                    headlineContent = { Text(item.url, maxLines = 1) },
                    supportingContent = {
                        Text(if (item.success) "Success" else "Failed — ${item.errorMessage}")
                    },
                    leadingContent = {
                        Icon(
                            if (item.success) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (item.success)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error,
                        )
                    },
                    modifier = androidx.compose.foundation.clickable(
                        enabled = item.resultId != null,
                        onClick = { item.resultId?.let(onResultClick) },
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

// Needed inside item block
@Composable
private fun remember(calculation: () -> androidx.compose.foundation.interaction.MutableInteractionSource) =
    androidx.compose.runtime.remember(calculation)
