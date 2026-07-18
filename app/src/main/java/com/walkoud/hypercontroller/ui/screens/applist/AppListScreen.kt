package com.walkoud.hypercontroller.ui.screens.applist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkoud.hypercontroller.core.model.AppCategory
import com.walkoud.hypercontroller.core.model.RestrictionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    onAppClick: (String) -> Unit,
    viewModel: AppListViewModel = viewModel()
) {
    val filteredApps by viewModel.filteredApps.collectAsState()
    val selectedPackages by viewModel.selectedPackages.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showFilterSheet by remember { mutableStateOf(false) }
    var showBatchDialog by remember { mutableStateOf(false) }
    var batchState by remember { mutableStateOf(RestrictionState.NO_RESTRICT) }
    var batchDelay by remember { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HyperController") },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.Search, contentDescription = "Filtrer")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedPackages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBatchDialog = true },
                    text = { Text("${selectedPackages.size} sélectionnée(s)") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (error != null) {
                AlertDialog(
                    onDismissRequest = { /* dismiss handled below */ },
                    title = { Text("Erreur") },
                    text = { Text(error ?: "") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Réessayer")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { /* dismiss error */ }) {
                            Text("Ignorer")
                        }
                    }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredApps.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Aucune app trouvée", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredApps, key = { it.pkgName }) { app ->
                        AppListItem(
                            app = app,
                            isSelected = app.pkgName in selectedPackages,
                            onToggleSelect = { viewModel.toggleSelection(app.pkgName) },
                            onClick = { onAppClick(app.pkgName) }
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            currentFilter = filter,
            onApply = { viewModel.updateFilter { it } },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showBatchDialog) {
        BatchActionDialog(
            onConfirm = { state, delay ->
                viewModel.applyBatchRestriction(state, delay) { success, msg ->
                    if (success) showBatchDialog = false
                }
            },
            onDismiss = { showBatchDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSheet(
    currentFilter: FilterOptions,
    onApply: (FilterOptions) -> Unit,
    onDismiss: () -> Unit
) {
    // Simplified - a real implementation would have proper filter UI
    onDismiss()
}

@Composable
private fun BatchActionDialog(
    onConfirm: (RestrictionState, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedState by remember { mutableStateOf(RestrictionState.NO_RESTRICT) }
    var delayText by remember { mutableStateOf("-1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Action groupée") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choisir la restriction à appliquer:")

                RestrictionState.entries.forEach { state ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedState == state,
                            onClick = { selectedState = state }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(state.label, style = MaterialTheme.typography.bodyMedium)
                            Text(state.description, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                OutlinedTextField(
                    value = delayText,
                    onValueChange = { delayText = it },
                    label = { Text("Délai (min)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val delay = delayText.toIntOrNull() ?: -1
                onConfirm(selectedState, delay)
            }) {
                Text("Appliquer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
