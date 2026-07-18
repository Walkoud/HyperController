package com.walkoud.hypercontroller.ui.screens.templates

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.core.model.Template
import com.walkoud.hypercontroller.core.model.TemplateAction
import com.walkoud.hypercontroller.core.model.TemplateTarget
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditor(
    template: Template?,
    onSave: (Template) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf(template?.name ?: "") }
    var description by remember { mutableStateOf(template?.description ?: "") }
    var target by remember { mutableStateOf(template?.target ?: TemplateTarget.SELECTED_PACKAGES) }
    var actions by remember { mutableStateOf(template?.actions ?: emptyList<TemplateAction>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (template == null) "Nouveau template" else "Éditer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val t = Template(
                            id = template?.id ?: UUID.randomUUID().toString(),
                            name = name,
                            description = description,
                            isBuiltin = false,
                            target = target,
                            actions = actions
                        )
                        onSave(t)
                    }) {
                        Text("Sauvegarder")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nom du template") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Text("Cible", style = MaterialTheme.typography.titleSmall)
            TemplateTarget.entries.forEach { t ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(selected = target == t, onClick = { target = t })
                    Text(t.label, modifier = Modifier.padding(start = 8.dp))
                }
            }

            Text("Actions (${actions.size})", style = MaterialTheme.typography.titleSmall)

            actions.forEachIndexed { index, action ->
                ActionCard(
                    action = action,
                    onDelete = {
                        actions = actions.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            AddActionButton { newAction ->
                actions = actions + newAction
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActionCard(
    action: TemplateAction,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val title = when (action) {
                    is TemplateAction.SetRestriction -> "Restriction: ${action.state.label}"
                    is TemplateAction.SetGlobalFeature -> "Global: ${action.key}=${action.value}"
                    is TemplateAction.AddToWhitelist -> "Whitelist: ${action.listName}"
                    is TemplateAction.KillProcess -> "Kill: ${action.processName}"
                }
                Text(title, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
            }
        }
    }
}

@Composable
private fun AddActionButton(onAdd: (TemplateAction) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        TextButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("Ajouter une action")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("SetRestriction — Appliquer un état") },
                onClick = {
                    onAdd(TemplateAction.SetRestriction(RestrictionState.NO_RESTRICT))
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("SetGlobalFeature — Modifier un paramètre global") },
                onClick = {
                    onAdd(TemplateAction.SetGlobalFeature("featureStatus", "false"))
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("AddToWhitelist — Ajouter à une liste d'exemption") },
                onClick = {
                    onAdd(TemplateAction.AddToWhitelist("levelUtimateSpecialApps"))
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("KillProcess — Tuer un processus") },
                onClick = {
                    onAdd(TemplateAction.KillProcess())
                    expanded = false
                }
            )
        }
    }
}
