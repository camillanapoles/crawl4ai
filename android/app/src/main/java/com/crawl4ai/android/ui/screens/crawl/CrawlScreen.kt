package com.crawl4ai.android.ui.screens.crawl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crawl4ai.android.R
import com.crawl4ai.android.domain.model.CacheMode
import com.crawl4ai.android.domain.model.CrawlJobStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrawlScreen(
    onResultReady: (Long) -> Unit,
    viewModel: CrawlViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Navigate to results once crawl completes with a stored result
    LaunchedEffect(state.resultId) {
        state.resultId?.let { onResultReady(it) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_crawl)) })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── URL input ─────────────────────────────────────────────────
            OutlinedTextField(
                value = state.url,
                onValueChange = viewModel::onUrlChanged,
                label = { Text("URL") },
                placeholder = { Text(stringResource(R.string.placeholder_url)) },
                singleLine = true,
                isError = state.url.isNotEmpty() && !state.isValidUrl,
                supportingText = {
                    if (state.url.isNotEmpty() && !state.isValidUrl)
                        Text(stringResource(R.string.error_invalid_url))
                    else if (state.error.isNotEmpty())
                        Text(state.error, color = MaterialTheme.colorScheme.error)
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { viewModel.startCrawl() }),
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Cache mode chips ──────────────────────────────────────────
            Text("Cache mode", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CacheMode.entries.take(3).forEach { mode ->
                    FilterChip(
                        selected = state.cacheMode == mode,
                        onClick = { viewModel.onCacheModeChanged(mode) },
                        label = { Text(mode.displayName) },
                    )
                }
            }

            // ── Word count threshold slider ────────────────────────────────
            Column {
                Text(
                    "Word threshold: ${state.wordCountThreshold}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.wordCountThreshold.toFloat(),
                    onValueChange = { viewModel.onWordThresholdChanged(it.toInt()) },
                    valueRange = 0f..200f,
                    steps = 19,
                )
            }

            // ── Timeout slider ────────────────────────────────────────────
            Column {
                Text(
                    "Timeout: ${state.timeoutSeconds}s",
                    style = MaterialTheme.typography.labelLarge,
                )
                Slider(
                    value = state.timeoutSeconds.toFloat(),
                    onValueChange = { viewModel.onTimeoutChanged(it.toInt()) },
                    valueRange = 5f..120f,
                    steps = 22,
                )
            }

            // ── Toggle: Screenshot ─────────────────────────────────────────
            ElevatedCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Save screenshot")
                    Switch(
                        checked = state.screenshot,
                        onCheckedChange = viewModel::onScreenshotChanged,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Start button ──────────────────────────────────────────────
            Button(
                onClick = viewModel::startCrawl,
                enabled = state.isValidUrl && !state.isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
                Text("  ${stringResource(R.string.action_crawl)}")
            }

            // ── Status display ────────────────────────────────────────────
            if (state.status == CrawlJobStatus.COMPLETED && state.result != null) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "✅ Completed",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text("Status: ${state.result!!.statusCode}")
                        Text("Words: ~${state.result!!.markdown.rawMarkdown.split(" ").size}")
                        Text("Links: ${
                            (state.result!!.links["internal"]?.size ?: 0) +
                            (state.result!!.links["external"]?.size ?: 0)
                        } found")
                        if (state.result!!.media["images"]?.isNotEmpty() == true) {
                            Text("Images: ${state.result!!.media["images"]!!.size}")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { state.resultId?.let { onResultReady(it) } },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("View Full Results →")
                        }
                    }
                }
            }
        }
    }
}
