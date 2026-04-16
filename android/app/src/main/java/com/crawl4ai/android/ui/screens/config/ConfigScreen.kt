package com.crawl4ai.android.ui.screens.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.crawl4ai.android.R
import com.crawl4ai.android.data.local.db.entity.CrawlConfigEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: ConfigViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val configs by viewModel.configs.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        if (uiState.snackbarMessage.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.snackbarMessage)
            viewModel.onSnackbarShown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.nav_config)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onCreateClick) {
                Icon(Icons.Default.Add, contentDescription = "New config")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (configs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No saved configurations",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "Tap + to create one",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(configs, key = { it.id }) { config ->
                    ConfigItem(
                        config = config,
                        onSetDefault = { viewModel.onSetDefault(config.id) },
                        onDelete = { viewModel.onDeleteRequest(config.id) },
                    )
                }
            }
        }

        // Create dialog
        if (uiState.showCreateDialog) {
            AlertDialog(
                onDismissRequest = viewModel::onCreateDismissed,
                title = { Text("New Configuration") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = uiState.newConfigName,
                            onValueChange = viewModel::onNewNameChanged,
                            label = { Text("Name *") },
                            isError = uiState.nameError.isNotEmpty(),
                            supportingText = {
                                if (uiState.nameError.isNotEmpty())
                                    Text(uiState.nameError)
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = uiState.newConfigDesc,
                            onValueChange = viewModel::onNewDescChanged,
                            label = { Text("Description") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = viewModel::onCreateConfirmed) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onCreateDismissed) { Text("Cancel") }
                },
            )
        }

        // Delete confirmation
        if (uiState.deleteConfirmId != null) {
            AlertDialog(
                onDismissRequest = viewModel::onDeleteDismissed,
                title = { Text("Delete config?") },
                text = { Text("This configuration will be permanently removed.") },
                confirmButton = {
                    TextButton(onClick = viewModel::onDeleteConfirmed) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::onDeleteDismissed) { Text("Cancel") }
                },
            )
        }
    }
}

@Composable
private fun ConfigItem(
    config: CrawlConfigEntity,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (config.isDefault)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (config.isDefault) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Default",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = config.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (config.description.isNotBlank()) {
                    Text(
                        text = config.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Text(
                    text = "Created ${dateFormat.format(Date(config.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            if (!config.isDefault) {
                IconButton(onClick = onSetDefault) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Set as default",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

