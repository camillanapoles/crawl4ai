package com.crawl4ai.android.ui.screens.results

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    resultId: Long,
    onBack: () -> Unit,
    viewModel: ResultsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(resultId) {
        viewModel.load(resultId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(state.result?.url?.take(40) ?: "Results") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        )

        when {
            state.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            state.error.isNotEmpty() -> {
                Text(
                    state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp),
                )
            }

            state.result != null -> {
                val result = state.result!!

                // ── Tab row ───────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                ) {
                    ResultTab.entries.forEach { tab ->
                        FilterChip(
                            selected = state.selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            label = { Text(tab.label) },
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }

                // ── Content ───────────────────────────────────────────────
                val content = when (state.selectedTab) {
                    ResultTab.MARKDOWN -> result.rawMarkdown.ifEmpty {
                        "(No markdown generated)"
                    }
                    ResultTab.HTML -> result.cleanedHtml.ifEmpty { result.html }.ifEmpty {
                        "(No HTML)"
                    }
                    ResultTab.EXTRACTED -> result.extractedContent.ifEmpty {
                        "(No extracted content — configure an extraction strategy)"
                    }
                    ResultTab.MEDIA -> result.mediaJson
                    ResultTab.LINKS -> result.linksJson
                    ResultTab.RAW -> result.html.ifEmpty { "(No raw HTML)" }
                }

                Text(
                    text = content,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    style = when (state.selectedTab) {
                        ResultTab.MARKDOWN -> MaterialTheme.typography.bodyMedium
                        else -> MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                        )
                    },
                )
            }
        }
    }
}
