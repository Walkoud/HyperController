package com.walkoud.hypercontroller.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkoud.hypercontroller.core.safety.SafetyLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val safetyLogs by viewModel.safetyLogs.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkRoot()
        viewModel.refreshSafetyLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Paramètres") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Root", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (settings.rootAvailable) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (settings.rootAvailable) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error
                            )
                            Text(
                                if (settings.rootAvailable) "Root disponible"
                                else if (settings.rootChecked) "Root non disponible"
                                else "Vérification..."
                            )
                        }

                        settings.miuiVersion.let { v ->
                            if (v.isNotBlank()) {
                                Text("MIUI/HyperOS: $v",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text(
                            if (settings.dbAccessible) "Bases de données accessibles"
                            else "Bases de données inaccessibles",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.dbAccessible) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                        )

                        Spacer(Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { viewModel.checkRoot() },
                            enabled = !settings.isLoading
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Tester à nouveau")
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Configuration", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)

                        Spacer(Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto-config pour nouvelles apps")
                            Switch(
                                checked = settings.autoConfigEnabled,
                                onCheckedChange = { viewModel.setAutoConfig(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Kill after apply")
                                Text(
                                    "Redémarre powerkeeper après chaque modification",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.killAfterApply,
                                onCheckedChange = { viewModel.setKillAfterApply(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Thème sombre")
                            Switch(
                                checked = settings.darkTheme,
                                onCheckedChange = { viewModel.setDarkTheme(it) }
                            )
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Journal de sécurité",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { viewModel.refreshSafetyLogs() }) {
                                Text("Rafraîchir")
                            }
                        }

                        if (safetyLogs.isEmpty()) {
                            Text("Aucun événement",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            safetyLogs.take(20).forEach { event ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        event.type.name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = when (event.type) {
                                            SafetyLogger.SafetyEvent.EventType.GUARD_BLOCK,
                                            SafetyLogger.SafetyEvent.EventType.SQL_INJECTION_BLOCK,
                                            SafetyLogger.SafetyEvent.EventType.DB_ACCESS_ERROR
                                                -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Text(
                                        event.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.widthIn(max = 200.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("À propos", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("HyperController v1.0",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Contrôle avancé des restrictions MIUI/HyperOS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
