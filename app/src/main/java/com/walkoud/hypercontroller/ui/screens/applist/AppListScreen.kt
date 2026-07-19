package com.walkoud.hypercontroller.ui.screens.applist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
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

    var searchMode by remember { mutableStateOf(filter.searchQuery.isNotBlank()) }
    var searchText by remember { mutableStateOf(filter.searchQuery) }

    // Sync search text with viewModel filter
    LaunchedEffect(searchText) {
        if (searchText != filter.searchQuery) {
            viewModel.updateFilter { it.copy(searchQuery = searchText) }
        }
    }

    // When filter changes externally, update our local search text
    LaunchedEffect(filter.searchQuery) {
        if (searchText != filter.searchQuery) {
            searchText = filter.searchQuery
            searchMode = filter.searchQuery.isNotBlank()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (searchMode) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            placeholder = { Text("Search…") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(onClick = {
                                        searchText = ""
                                        searchMode = false
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                } else {
                                    IconButton(onClick = { searchMode = false }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Close")
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                disabledContainerColor = MaterialTheme.colorScheme.surface,
                            )
                        )
                    } else {
                        Text("HyperController")
                    }
                },
                actions = {
                    if (!searchMode) {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = {
                            searchMode = true
                            searchText = filter.searchQuery
                        }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedPackages.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showBatchDialog = true }
                ) {
                    Text("${selectedPackages.size} selected")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (error != null) {
                AlertDialog(
                    onDismissRequest = { /* dismiss handled below */ },
                    title = { Text("Error") },
                    text = { Text(error ?: "") },
                    confirmButton = {
                        TextButton(onClick = { viewModel.refresh() }) {
                            Text("Retry")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { /* dismiss error */ }) {
                            Text("Dismiss")
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
                    Text("No app found", style = MaterialTheme.typography.bodyLarge)
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
        var resultMessage by remember { mutableStateOf<String?>(null) }
        var isError by remember { mutableStateOf(false) }

        BatchActionDialog(
            onConfirm = { state, delay ->
                viewModel.applyBatchRestriction(state, delay) { success, msg ->
                    if (success) {
                        showBatchDialog = false
                    } else {
                        resultMessage = msg
                        isError = true
                    }
                }
            },
            onDismiss = { showBatchDialog = false },
            resultMessage = resultMessage,
            isError = isError
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterSheet(
    currentFilter: FilterOptions,
    onApply: (FilterOptions) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf(currentFilter.searchQuery) }

    var sortEnabled by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf(currentFilter.sortBy) }

    var restrictionEnabled by remember { mutableStateOf(currentFilter.restriction != null) }
    var restriction by remember { mutableStateOf(currentFilter.restriction) }

    var categoryEnabled by remember { mutableStateOf(currentFilter.category != null) }
    var category by remember { mutableStateOf(currentFilter.category) }

    var showSystem by remember { mutableStateOf(currentFilter.showSystemApps) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Name or package…") },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            FilterSection(title = "Sort by", checked = sortEnabled, onCheckedChange = { sortEnabled = it }) {
                SortMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = sortBy == mode,
                                onValueChange = { if (it) sortBy = mode },
                                enabled = sortEnabled
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = sortBy == mode, onClick = { sortBy = mode }, enabled = sortEnabled)
                        Text(mode.label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            FilterSection(title = "Restriction", checked = restrictionEnabled, onCheckedChange = { restrictionEnabled = it }) {
                FilterChipRow(
                    enabled = restrictionEnabled,
                    options = listOf<Pair<RestrictionState?, String>>(
                        null to "All",
                        RestrictionState.NO_RESTRICT to RestrictionState.NO_RESTRICT.label,
                        RestrictionState.MIUI_AUTO to RestrictionState.MIUI_AUTO.label,
                        RestrictionState.RESTRICT_BG to RestrictionState.RESTRICT_BG.label,
                        RestrictionState.NO_BG to RestrictionState.NO_BG.label
                    ),
                    selected = restriction,
                    onSelect = { restriction = it }
                )
            }

            FilterSection(title = "Category", checked = categoryEnabled, onCheckedChange = { categoryEnabled = it }) {
                FilterChipRow(
                    enabled = categoryEnabled,
                    options = listOf<Pair<AppCategory?, String>>(
                        null to "All",
                        *AppCategory.entries.map { it to it.label }.toTypedArray()
                    ),
                    selected = category,
                    onSelect = { category = it }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = showSystem,
                        onValueChange = { showSystem = it }
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = showSystem, onCheckedChange = { showSystem = it })
                Text("Show system apps", modifier = Modifier.padding(start = 8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        searchQuery = ""
                        sortEnabled = false
                        sortBy = SortMode.NAME_ASC
                        restrictionEnabled = false
                        restriction = null
                        categoryEnabled = false
                        category = null
                        showSystem = false
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = {
                        onApply(
                            currentFilter.copy(
                                searchQuery = searchQuery,
                                category = if (categoryEnabled) category else null,
                                restriction = if (restrictionEnabled) restriction else null,
                                showSystemApps = showSystem,
                                sortBy = if (sortEnabled) sortBy else SortMode.NAME_ASC
                            )
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilterSection(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(value = checked, onValueChange = onCheckedChange),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 8.dp))
        }
        Column(
            content = content,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun <T> FilterChipRow(
    enabled: Boolean = true,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelect(value) },
                label = { Text(label) },
                enabled = enabled
            )
        }
    }
}

@Composable
private fun BatchActionDialog(
    onConfirm: (RestrictionState, Int) -> Unit,
    onDismiss: () -> Unit,
    resultMessage: String? = null,
    isError: Boolean = false
) {
    var selectedState by remember { mutableStateOf(RestrictionState.NO_RESTRICT) }
    var delayText by remember { mutableStateOf("-1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batch action") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Choose the restriction to apply:")

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
                    label = { Text("Delay (min)") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (resultMessage != null) {
                    Text(
                        resultMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val delay = delayText.toIntOrNull() ?: -1
                onConfirm(selectedState, delay)
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
