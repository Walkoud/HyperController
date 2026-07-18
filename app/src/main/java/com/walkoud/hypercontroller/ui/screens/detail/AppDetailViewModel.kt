package com.walkoud.hypercontroller.ui.screens.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walkoud.hypercontroller.HyperControllerApp
import com.walkoud.hypercontroller.core.db.DbExecutor
import com.walkoud.hypercontroller.core.db.PowerKeeperDb
import com.walkoud.hypercontroller.core.model.AppInfo
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.core.safety.BackupManager
import com.walkoud.hypercontroller.core.safety.SystemAppGuard
import com.walkoud.hypercontroller.core.util.PackageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CloudConfigInfo(
    val bgData: String? = null,
    val bgLocation: String? = null,
    val kDelay: String? = null,
    val sDelay: String? = null,
    val kPolicy: String? = null,
    val powerStateId: String? = null,
    val iDelay: String? = null
)

class AppDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperControllerApp
    private val sqlitePath = app.sqlite3Path
    private val packageUtils = PackageUtils(application)
    private val executor = DbExecutor(sqlitePath)
    private val backupManager = BackupManager()
    private val powerKeeperDb = PowerKeeperDb(executor, backupManager, sqlitePath)

    private val _appInfo = MutableStateFlow<AppInfo?>(null)
    val appInfo: StateFlow<AppInfo?> = _appInfo.asStateFlow()

    private val _cloudConfig = MutableStateFlow<CloudConfigInfo?>(null)
    val cloudConfig: StateFlow<CloudConfigInfo?> = _cloudConfig.asStateFlow()

    private val _isCritical = MutableStateFlow(false)
    val isCritical: StateFlow<Boolean> = _isCritical.asStateFlow()

    private val _isSensitive = MutableStateFlow(false)
    val isSensitive: StateFlow<Boolean> = _isSensitive.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _operationResult = MutableStateFlow<String?>(null)
    val operationResult: StateFlow<String?> = _operationResult.asStateFlow()

    private var currentPkgName: String = ""

    fun loadPackage(pkgName: String) {
        currentPkgName = pkgName
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val restriction = powerKeeperDb.getAppRestriction(pkgName)
                val cloudRaw = powerKeeperDb.getCloudAppConfig(pkgName)

                _appInfo.value = packageUtils.buildAppInfo(
                    pkgName = pkgName,
                    restriction = RestrictionState.fromBgControl(restriction?.bgControl ?: "miuiAuto"),
                    bgDelayMin = restriction?.bgDelayMin ?: -1,
                    powerStateId = cloudRaw["power_state_id"]?.toIntOrNull() ?: -1,
                    kPolicy = cloudRaw["k_policy"]?.toIntOrNull() ?: -1
                )
                _cloudConfig.value = CloudConfigInfo(
                    bgData = cloudRaw["bgData"],
                    bgLocation = cloudRaw["bgLocation"],
                    kDelay = cloudRaw["k_delay"],
                    sDelay = cloudRaw["s_delay"],
                    kPolicy = cloudRaw["k_policy"],
                    powerStateId = cloudRaw["power_state_id"],
                    iDelay = cloudRaw["i_delay"]
                )
                _isCritical.value = SystemAppGuard.isCritical(pkgName)
                _isSensitive.value = SystemAppGuard.isSensitive(pkgName)
            } catch (e: Exception) {
                _operationResult.value = "Erreur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setRestriction(state: RestrictionState, delayMin: Int) {
        viewModelScope.launch {
            try {
                powerKeeperDb.setAppRestriction(currentPkgName, state, delayMin)
                loadPackage(currentPkgName)
                _operationResult.value = "Restriction appliquée: ${state.label}"
            } catch (e: Exception) {
                _operationResult.value = "Erreur: ${e.message}"
            }
        }
    }

    fun clearResult() {
        _operationResult.value = null
    }
}
