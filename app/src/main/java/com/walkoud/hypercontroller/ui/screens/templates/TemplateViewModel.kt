package com.walkoud.hypercontroller.ui.screens.templates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walkoud.hypercontroller.core.db.DbExecutor
import com.walkoud.hypercontroller.core.db.PowerKeeperDb
import com.walkoud.hypercontroller.core.model.BuiltinTemplates
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.core.model.Template
import com.walkoud.hypercontroller.core.model.TemplateAction
import com.walkoud.hypercontroller.core.model.TemplateTarget
import com.walkoud.hypercontroller.core.safety.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class TemplateViewModel(application: Application) : AndroidViewModel(application) {

    private val executor = DbExecutor("sqlite3")
    private val backupManager = BackupManager()
    private val powerKeeperDb = PowerKeeperDb(executor, backupManager, "sqlite3")

    private val _templates = MutableStateFlow<List<Template>>(emptyList())
    val templates: StateFlow<List<Template>> = _templates.asStateFlow()

    private val _selectedTemplate = MutableStateFlow<Template?>(null)
    val selectedTemplate: StateFlow<Template?> = _selectedTemplate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _applyResult = MutableStateFlow<String?>(null)
    val applyResult: StateFlow<String?> = _applyResult.asStateFlow()

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        val builtins = BuiltinTemplates.ALL
        val userTemplates = loadUserTemplates()
        _templates.value = builtins + userTemplates
    }

    fun selectTemplate(template: Template) {
        _selectedTemplate.value = template
    }

    fun clearSelection() {
        _selectedTemplate.value = null
    }

    fun applyTemplate(template: Template, targetPackages: Set<String> = emptySet()) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val packages = when (template.target) {
                    TemplateTarget.SELECTED_PACKAGES -> targetPackages
                    else -> targetPackages
                }

                var appliedCount = 0
                for (action in template.actions) {
                    when (action) {
                        is TemplateAction.SetRestriction -> {
                            if (packages.isNotEmpty()) {
                                powerKeeperDb.setBatchRestriction(packages, action.state, action.delayMin)
                                appliedCount += packages.size
                            }
                        }
                        is TemplateAction.SetGlobalFeature -> {
                            powerKeeperDb.setGlobalFeature(action.key, action.value)
                            appliedCount++
                        }
                        is TemplateAction.AddToWhitelist -> {
                            packages.forEach { pkg ->
                                powerKeeperDb.addToWhitelist(action.listName, pkg)
                            }
                            appliedCount += packages.size
                        }
                        is TemplateAction.KillProcess -> {
                            com.walkoud.hypercontroller.core.root.RootShell.pkill(action.processName)
                            appliedCount++
                        }
                    }
                }
                _applyResult.value = "Template '${template.name}' appliqué ($appliedCount actions)"
            } catch (e: Exception) {
                _applyResult.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveUserTemplate(template: Template) {
        val userTemplates = loadUserTemplates().toMutableList()
        val existing = userTemplates.indexOfFirst { it.id == template.id }
        if (existing >= 0) {
            userTemplates[existing] = template
        } else {
            userTemplates.add(template)
        }
        saveUserTemplates(userTemplates)
        loadTemplates()
    }

    fun deleteUserTemplate(templateId: String) {
        val userTemplates = loadUserTemplates().filter { it.id != templateId }
        saveUserTemplates(userTemplates)
        loadTemplates()
    }

    fun clearResult() {
        _applyResult.value = null
    }

    private fun loadUserTemplates(): List<Template> {
        val prefs = getApplication<Application>().getSharedPreferences("templates", 0)
        val json = prefs.getString("user_templates", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i -> jsonToTemplate(arr.getJSONObject(i)) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveUserTemplates(templates: List<Template>) {
        val prefs = getApplication<Application>().getSharedPreferences("templates", 0)
        val arr = JSONArray()
        templates.forEach { arr.put(templateToJson(it)) }
        prefs.edit().putString("user_templates", arr.toString()).apply()
    }

    private fun templateToJson(t: Template): JSONObject {
        val actions = JSONArray()
        t.actions.forEach { action ->
            when (action) {
                is TemplateAction.SetRestriction -> {
                    actions.put(JSONObject().apply {
                        put("type", "SET_RESTRICTION")
                        put("state", action.state.bgControl)
                        put("delayMin", action.delayMin)
                    })
                }
                is TemplateAction.SetGlobalFeature -> {
                    actions.put(JSONObject().apply {
                        put("type", "SET_GLOBAL")
                        put("key", action.key)
                        put("value", action.value)
                    })
                }
                is TemplateAction.AddToWhitelist -> {
                    actions.put(JSONObject().apply {
                        put("type", "ADD_TO_WHITELIST")
                        put("list", action.listName)
                    })
                }
                is TemplateAction.KillProcess -> {
                    actions.put(JSONObject().apply {
                        put("type", "KILL_PROCESS")
                        put("process", action.processName)
                    })
                }
            }
        }
        return JSONObject().apply {
            put("id", t.id)
            put("name", t.name)
            put("description", t.description)
            put("isBuiltin", false)
            put("target", t.target.name)
            put("actions", actions)
        }
    }

    private fun jsonToTemplate(json: JSONObject): Template {
        val actionsArr = json.getJSONArray("actions")
        val actions = (0 until actionsArr.length()).map { i ->
            val act = actionsArr.getJSONObject(i)
            when (act.getString("type")) {
                "SET_RESTRICTION" -> TemplateAction.SetRestriction(
                    state = RestrictionState.fromBgControl(act.getString("state")),
                    delayMin = act.optInt("delayMin", -1)
                )
                "SET_GLOBAL" -> TemplateAction.SetGlobalFeature(
                    key = act.getString("key"),
                    value = act.getString("value")
                )
                "ADD_TO_WHITELIST" -> TemplateAction.AddToWhitelist(
                    listName = act.getString("list")
                )
                "KILL_PROCESS" -> TemplateAction.KillProcess(
                    processName = act.optString("process", "com.miui.powerkeeper")
                )
                else -> throw IllegalArgumentException("Unknown action type")
            }
        }
        return Template(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description", ""),
            isBuiltin = false,
            target = TemplateTarget.valueOf(json.getString("target")),
            actions = actions
        )
    }
}
