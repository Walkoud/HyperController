package com.walkoud.hypercontroller.ui.screens.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkoud.hypercontroller.core.safety.SafetyLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    templateViewModel: com.walkoud.hypercontroller.ui.screens.templates.TemplateViewModel = viewModel()
) {
    val settings by viewModel.settings.collectAsState()
    val safetyLogs by viewModel.safetyLogs.collectAsState()
    val templates by templateViewModel.templates.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkRoot()
        viewModel.refreshSafetyLogs()
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
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
                                if (settings.rootAvailable) "Root available"
                                else if (settings.rootChecked) "Root not available"
                                else "Checking..."
                            )
                        }

                        settings.miuiVersion.let { v ->
                            if (v.isNotBlank()) {
                                Text("MIUI/HyperOS: $v",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        Text(
                            if (settings.dbAccessible) "Databases accessible"
                            else "Databases inaccessible",
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
                            Text("Test again")
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

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Auto-config for new apps")
                                    Text(
                                        "Automatically applies a template to newly installed apps",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = settings.autoConfigEnabled,
                                    onCheckedChange = { viewModel.setAutoConfig(it) }
                                )
                            }

                            if (settings.autoConfigEnabled) {
                                var expanded by remember { mutableStateOf(false) }
                                val selectedTemplate = templates.find { it.id == settings.autoConfigTemplateId }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = selectedTemplate?.name ?: "Choose a template...",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Template to apply") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        templates.forEach { template ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(template.name + if (template.isBuiltin) " (built-in)" else "")
                                                },
                                                onClick = {
                                                    viewModel.setAutoConfigTemplate(template.id)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Restart after change")
                                Text(
                                    "Forces a powerkeeper service restart so changes take effect immediately",
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
                            Text("Dark theme")
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
                            Text("Safety log",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { viewModel.refreshSafetyLogs() }) {
                                Text("Refresh")
                            }
                        }

                        if (safetyLogs.isEmpty()) {
                            Text("No event",
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
                        Text("About", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text("HyperController v1.0",
                            style = MaterialTheme.typography.bodyMedium)
                        Text("Advanced control of MIUI/HyperOS restrictions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)

                        Spacer(Modifier.height(8.dp))

                        val context = LocalContext.current
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Walkoud/HyperController")
                                )
                                context.startActivity(intent)
                            }
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("View on GitHub")
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}
