package com.crawl4ai.android.ui.screens.deepcrawl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crawl4ai.android.R
import com.crawl4ai.android.domain.model.CrawlJobStatus
import com.crawl4ai.android.domain.model.DeepCrawlStrategy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeepCrawlScreen(
    onPageResult: (Long) -> Unit,
    viewModel: DeepCrawlViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_deep_crawl)) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── URL ───────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrlChanged,
                label = { Text("Start URL") },
                placeholder = { Text(stringResource(R.string.placeholder_url)) },
                singleLine = true,
                isError = state.url.isNotEmpty() && !state.isValidUrl,
                modifier = Modifier.fillMaxWidth(),
                enabled = state.status != CrawlJobStatus.RUNNING,
            )

            // ── Strategy ─────────────────────────────────────────────────
            Text("Strategy", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DeepCrawlStrategy.entries.forEach { s ->
                    FilterChip(
                        selected = state.strategy == s,
                        onClick = { viewModel.onStrategyChanged(s) },
                        label = { Text(s.displayName) },
                        enabled = state.status != CrawlJobStatus.RUNNING,
                    )
                }
            }

            // ── Max depth ─────────────────────────────────────────────────
            Column {
                Text(
                    "Max depth: ${state.maxDepth}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.maxDepth.toFloat(),
                    onValueChange = { viewModel.onMaxDepthChanged(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                    enabled = state.status != CrawlJobStatus.RUNNING,
                )
            }

            // ── Max pages ─────────────────────────────────────────────────
            Column {
                Text(
                    "Max pages: ${state.maxPages}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.maxPages.toFloat(),
                    onValueChange = { viewModel.onMaxPagesChanged(it.toInt()) },
                    valueRange = 5f..500f,
                    steps = 98,
                    enabled = state.status != CrawlJobStatus.RUNNING,
                )
            }

            // ── Start / Cancel ─────────────────────────────────────────────
            if (state.status == CrawlJobStatus.RUNNING) {
                OutlinedButton(
                    onClick = viewModel::cancelCrawl,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Stop, contentDescription = null)
                    Text("  Cancel")
                }
            } else {
                Button(
                    onClick = viewModel::startDeepCrawl,
                    enabled = state.isValidUrl,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Tune, contentDescription = null)
                    Text("  Start Deep Crawl")
                }
            }

            // ── Progress ──────────────────────────────────────────────────
            if (state.status == CrawlJobStatus.RUNNING || state.pagesVisited > 0) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Pages: ${state.pagesVisited} / ${state.maxPages}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (state.status == CrawlJobStatus.RUNNING) {
                            LinearProgressIndicator(
                                progress = { state.pagesVisited.toFloat() / state.maxPages },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                            )
                        }
                        if (state.currentUrl.isNotEmpty()) {
                            Text(
                                "Current: ${state.currentUrl.take(60)}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        Text(
                            "Depth: ${state.currentDepth} / ${state.maxDepth}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // ── Completed results list ─────────────────────────────────────
            if (state.results.isNotEmpty()) {
                Text(
                    "Crawled pages (${state.results.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
                state.results.forEach { result ->
                    Text(
                        "• ${result.url}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            // ── Error ─────────────────────────────────────────────────────
            if (state.error.isNotEmpty()) {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
