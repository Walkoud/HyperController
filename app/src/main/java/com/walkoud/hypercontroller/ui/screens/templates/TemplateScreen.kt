package com.walkoud.hypercontroller.ui.screens.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.walkoud.hypercontroller.core.model.BuiltinTemplates
import com.walkoud.hypercontroller.core.model.Template

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateScreen(
    onEditTemplate: (Template) -> Unit,
    onCreateTemplate: () -> Unit,
    viewModel: TemplateViewModel = viewModel()
) {
    val templates by viewModel.templates.collectAsState()
    val applyResult by viewModel.applyResult.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showApplyDialog by remember { mutableStateOf<Template?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Templates") },
                actions = {
                    IconButton(onClick = onCreateTemplate) {
                        Icon(Icons.Default.Add, contentDescription = "New template")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            applyResult?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(12.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearResult() }) { Text("OK") }
                    }
                ) { Text(msg) }
            }

            if (templates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No template", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Create a template to apply configurations in one tap",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(templates, key = { it.id }) { template ->
                        TemplateCard(
                            template = template,
                            onApply = { showApplyDialog = template },
                            onEdit = { if (!template.isBuiltin) onEditTemplate(template) },
                            onDelete = {
                                if (!template.isBuiltin) viewModel.deleteUserTemplate(template.id)
                            }
                        )
                    }
                }
            }
        }
    }

    showApplyDialog?.let { template ->
        AlertDialog(
            onDismissRequest = { showApplyDialog = null },
            title = { Text("Apply '${template.name}'") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(template.description)
                    Text(
                        "${template.actions.size} action(s) will be executed",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isLoading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.applyTemplate(template)
                        showApplyDialog = null
                    },
                    enabled = !isLoading
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showApplyDialog = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun TemplateCard(
    template: Template,
    onApply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        template.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (template.isBuiltin) {
                        Text(
                            "Built-in",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onApply) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Apply")
                    }
                    if (!template.isBuiltin) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }

            if (template.description.isNotBlank()) {
                Text(
                    template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Text(
                "Target: ${template.target.label} • ${template.actions.size} action(s)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
