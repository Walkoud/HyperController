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
                title = { Text(if (template == null) "New template" else "Edit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (name.isNotBlank()) {
                                val t = Template(
                                    id = template?.id ?: UUID.randomUUID().toString(),
                                    name = name,
                                    description = description,
                                    isBuiltin = false,
                                    target = target,
                                    actions = actions
                                )
                                onSave(t)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Save")
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
                label = { Text("Template name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Text("Target", style = MaterialTheme.typography.titleSmall)
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
                    onUpdate = { updated ->
                        actions = actions.toMutableList().also { it[index] = updated }
                    },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCard(
    action: TemplateAction,
    onUpdate: (TemplateAction) -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    when (action) {
                        is TemplateAction.SetRestriction -> {
                            Text("Restriction", style = MaterialTheme.typography.bodyMedium)
                        }
                        is TemplateAction.SetGlobalFeature -> {
                            Text("Global setting", style = MaterialTheme.typography.bodyMedium)
                        }
                        is TemplateAction.AddToWhitelist -> {
                            Text("Whitelist", style = MaterialTheme.typography.bodyMedium)
                        }
                        is TemplateAction.KillProcess -> {
                            Text("Kill process", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                }
            }

            Spacer(Modifier.height(8.dp))

            when (action) {
                is TemplateAction.SetRestriction -> {
                    var expanded by remember { mutableStateOf(false) }
                    var delayText by remember { mutableStateOf(action.delayMin.toString()) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = action.state.label,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mode") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            RestrictionState.entries.forEach { state ->
                                DropdownMenuItem(
                                    text = { Text(state.label) },
                                    onClick = {
                                        onUpdate(action.copy(state = state))
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = delayText,
                        onValueChange = { v ->
                            delayText = v
                            val delay = v.toIntOrNull() ?: -1
                            onUpdate(action.copy(delayMin = delay))
                        },
                        label = { Text("Delay (min, -1 = immediate)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is TemplateAction.SetGlobalFeature -> {
                    var key by remember { mutableStateOf(action.key) }
                    var value by remember { mutableStateOf(action.value) }

                    OutlinedTextField(
                        value = key,
                        onValueChange = { k ->
                            key = k
                            onUpdate(action.copy(key = k))
                        },
                        label = { Text("Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = value,
                        onValueChange = { v ->
                            value = v
                            onUpdate(action.copy(value = v))
                        },
                        label = { Text("Value") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is TemplateAction.AddToWhitelist -> {
                    var listName by remember { mutableStateOf(action.listName) }

                    OutlinedTextField(
                        value = listName,
                        onValueChange = { v ->
                            listName = v
                            onUpdate(action.copy(listName = v))
                        },
                        label = { Text("List name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                is TemplateAction.KillProcess -> {
                    var processName by remember { mutableStateOf(action.processName) }

                    OutlinedTextField(
                        value = processName,
                        onValueChange = { v ->
                            processName = v
                            onUpdate(action.copy(processName = v))
                        },
                        label = { Text("Process name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
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
            Text("Add an action")
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Restriction") },
                onClick = {
                    onAdd(TemplateAction.SetRestriction(RestrictionState.NO_RESTRICT))
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Global setting") },
                onClick = {
                    onAdd(TemplateAction.SetGlobalFeature("", ""))
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Add to whitelist") },
                onClick = {
                    onAdd(TemplateAction.AddToWhitelist(""))
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Kill process") },
                onClick = {
                    onAdd(TemplateAction.KillProcess())
                    expanded = false
                }
            )
        }
    }
}
