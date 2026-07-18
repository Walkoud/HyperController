package com.walkoud.hypercontroller.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.ui.screens.applist.StateBadge
import com.walkoud.hypercontroller.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    pkgName: String,
    onBack: () -> Unit,
    viewModel: AppDetailViewModel = viewModel()
) {
    LaunchedEffect(pkgName) {
        viewModel.loadPackage(pkgName)
    }

    val appInfo by viewModel.appInfo.collectAsState()
    val cloudConfig by viewModel.cloudConfig.collectAsState()
    val isCritical by viewModel.isCritical.collectAsState()
    val isSensitive by viewModel.isSensitive.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val operationResult by viewModel.operationResult.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appInfo?.appName ?: pkgName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isCritical) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                        Text(
                            "App système critique — modification bloquée",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            } else if (isSensitive) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AppColors.miuiAuto.copy(alpha = 0.15f))
                ) {
                    Text(
                        "App système sensible — soyez prudent",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            appInfo?.let { app ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Informations", style = MaterialTheme.typography.titleMedium)
                        DetailRow("Package", app.pkgName)
                        DetailRow("Type", if (app.isSystemApp) "Système" else "Utilisateur")
                        DetailRow("Catégorie", app.category.label)
                        DetailRow("État actuel", app.currentState.label)
                        if (app.bgDelayMin > 0) DetailRow("Délai", "${app.bgDelayMin} min")
                    }
                }
            }

            if (!isCritical) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Modifier la restriction", style = MaterialTheme.typography.titleMedium)

                        var selectedState by remember { mutableStateOf(appInfo?.currentState ?: RestrictionState.MIUI_AUTO) }
                        var delayText by remember { mutableStateOf((appInfo?.bgDelayMin ?: -1).toString()) }

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
                                    Text(state.label, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium)
                                    Text(state.description, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = delayText,
                            onValueChange = { delayText = it },
                            label = { Text("Délai avant kill (minutes)") },
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text(" -1 = immédiat, 0-1440 = minutes") }
                        )

                        Button(
                            onClick = {
                                val delay = delayText.toIntOrNull() ?: -1
                                viewModel.setRestriction(selectedState, delay)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Appliquer")
                        }
                    }
                }
            }

            cloudConfig?.let { config ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Configuration cloud (avancé)", style = MaterialTheme.typography.titleMedium)
                        config.bgData?.let { DetailRow("bgData", it) }
                        config.bgLocation?.let { DetailRow("bgLocation", it) }
                        config.kDelay?.let { DetailRow("k_delay", it) }
                        config.sDelay?.let { DetailRow("s_delay", it) }
                        config.kPolicy?.let { DetailRow("k_policy", it) }
                        config.powerStateId?.let { DetailRow("power_state_id", it) }
                        config.iDelay?.let { DetailRow("i_delay", it) }
                    }
                }
            }

            operationResult?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearResult() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium)
    }
}
