package com.walkoud.hypercontroller.ui.screens.settings

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walkoud.hypercontroller.core.root.RootChecker
import com.walkoud.hypercontroller.core.safety.SafetyLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val rootAvailable: Boolean = false,
    val rootChecked: Boolean = false,
    val autoConfigEnabled: Boolean = false,
    val autoConfigTemplateId: String = "",
    val killAfterApply: Boolean = true,
    val darkTheme: Boolean = true,
    val dbAccessible: Boolean = false,
    val miuiVersion: String = "",
    val isLoading: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("settings", 0)

    private val _settings = MutableStateFlow(SettingsState(
        autoConfigEnabled = prefs.getBoolean("auto_config", false),
        autoConfigTemplateId = prefs.getString("auto_config_template_id", "") ?: "",
        killAfterApply = prefs.getBoolean("kill_after_apply", true),
        darkTheme = prefs.getBoolean("dark_theme", true)
    ))
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _safetyLogs = MutableStateFlow<List<SafetyLogger.SafetyEvent>>(emptyList())
    val safetyLogs: StateFlow<List<SafetyLogger.SafetyEvent>> = _safetyLogs.asStateFlow()

    fun checkRoot() {
        viewModelScope.launch {
            _settings.value = _settings.value.copy(isLoading = true)
            val result = RootChecker.check()
            Log.d("HyperCtrl", "checkRoot result: hasRoot=${result.hasRoot} suAvail=${result.suAvailable} pkDb=${result.powerKeeperDbAccessible} scDb=${result.securityCenterDbAccessible} sqliteOk=${result.sqliteAvailable} ver=${result.miuiVersion} errors=${result.errors}")
            _settings.value = _settings.value.copy(
                rootAvailable = result.hasRoot,
                rootChecked = true,
                dbAccessible = result.powerKeeperDbAccessible,
                miuiVersion = result.miuiVersion,
                isLoading = false
            )
        }
    }

    fun setAutoConfig(enabled: Boolean) {
        _settings.value = _settings.value.copy(autoConfigEnabled = enabled)
        prefs.edit().putBoolean("auto_config", enabled).commit()
        android.util.Log.d("HyperCtrl", "Auto-config changé: $enabled")
        if (enabled) {
            android.util.Log.d("HyperCtrl", "Démarrage du service AutoConfigService...")
            com.walkoud.hypercontroller.service.AutoConfigService.startIfEnabled(getApplication())
        } else {
            android.util.Log.d("HyperCtrl", "Arrêt du service AutoConfigService...")
            com.walkoud.hypercontroller.service.AutoConfigService.stopService(getApplication())
        }
    }

    fun setAutoConfigTemplate(templateId: String) {
        _settings.value = _settings.value.copy(autoConfigTemplateId = templateId)
        prefs.edit().putString("auto_config_template_id", templateId).commit()
        // Redémarrer le service si actif pour prendre en compte le nouveau template
        if (_settings.value.autoConfigEnabled) {
            com.walkoud.hypercontroller.service.AutoConfigService.stopService(getApplication())
            com.walkoud.hypercontroller.service.AutoConfigService.startIfEnabled(getApplication())
        }
    }

    fun setKillAfterApply(enabled: Boolean) {
        _settings.value = _settings.value.copy(killAfterApply = enabled)
        prefs.edit().putBoolean("kill_after_apply", enabled).apply()
    }

    fun setDarkTheme(dark: Boolean) {
        _settings.value = _settings.value.copy(darkTheme = dark)
        prefs.edit().putBoolean("dark_theme", dark).apply()
    }

    fun refreshSafetyLogs() {
        _safetyLogs.value = SafetyLogger.getRecentEvents(100)
    }
}
