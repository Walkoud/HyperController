package com.walkoud.hypercontroller.ui.screens.applist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walkoud.hypercontroller.HyperControllerApp
import com.walkoud.hypercontroller.core.db.DbExecutor
import com.walkoud.hypercontroller.core.db.PowerKeeperDb
import com.walkoud.hypercontroller.core.db.SecurityCenterDb
import com.walkoud.hypercontroller.core.model.AppCategory
import com.walkoud.hypercontroller.core.model.AppInfo
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.core.safety.BackupManager
import com.walkoud.hypercontroller.core.safety.SystemAppGuard
import com.walkoud.hypercontroller.core.util.PackageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FilterOptions(
    val searchQuery: String = "",
    val category: AppCategory? = null,
    val restriction: RestrictionState? = null,
    val showSystemApps: Boolean = false,
    val showUserApps: Boolean = true,
    val sortBy: SortMode = SortMode.NAME_ASC
)

enum class SortMode {
    NAME_ASC, NAME_DESC, STATE, CATEGORY;

    val label: String
        get() = when (this) {
            NAME_ASC -> "Name (A→Z)"
            NAME_DESC -> "Name (Z→A)"
            STATE -> "Restriction state"
            CATEGORY -> "Category"
        }
}

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as HyperControllerApp
    private val sqlitePath = app.sqlite3Path
    private val packageUtils = PackageUtils(application)
    private val executor = DbExecutor(sqlitePath)
    private val backupManager = BackupManager()
    private val powerKeeperDb = PowerKeeperDb(executor, backupManager, sqlitePath)
    private val securityCenterDb = SecurityCenterDb(executor)

    private val _allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val _filter = MutableStateFlow(FilterOptions())
    val filter: StateFlow<FilterOptions> = _filter.asStateFlow()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps: StateFlow<List<AppInfo>> = _filteredApps.asStateFlow()

    private val _selectedPackages = MutableStateFlow<Set<String>>(emptySet())
    val selectedPackages: StateFlow<Set<String>> = _selectedPackages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            combine(_allApps, _filter) { apps, filter -> filterApps(apps, filter) }
                .collect { _filteredApps.value = it }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val apps = withContext(Dispatchers.IO) {
                    val allRestrictions = powerKeeperDb.getAllRestrictions()
                    val allInstalled = packageUtils.getInstalledApps()

                    allInstalled.map { appInfo ->
                        val restriction = allRestrictions[appInfo.packageName]
                        packageUtils.buildAppInfo(
                            pkgName = appInfo.packageName,
                            restriction = restriction?.let {
                                RestrictionState.fromBgControl(it.bgControl)
                            } ?: RestrictionState.MIUI_AUTO,
                            bgDelayMin = restriction?.bgDelayMin ?: -1
                        )
                    }
                }
                _allApps.value = apps.sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                _error.value = "Error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateFilter(transform: (FilterOptions) -> FilterOptions) {
        _filter.value = transform(_filter.value)
    }

    fun toggleSelection(pkgName: String) {
        _selectedPackages.value = _selectedPackages.value.let { current ->
            if (pkgName in current) current - pkgName else current + pkgName
        }
    }

    fun selectAll() {
        _selectedPackages.value = _filteredApps.value.map { it.pkgName }.toSet()
    }

    fun clearSelection() {
        _selectedPackages.value = emptySet()
    }

    fun applyBatchRestriction(state: RestrictionState, delayMin: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val packages = _selectedPackages.value
                android.util.Log.d("HyperCtrl", "applyBatchRestriction appelé: ${packages.size} apps sélectionnées")
                if (packages.isEmpty()) {
                    android.util.Log.d("HyperCtrl", "No app selected")
                    onResult(false, "No app selected")
                    return@launch
                }
                val nonCritical = packages.filterNot { SystemAppGuard.isCritical(it) }
                android.util.Log.d("HyperCtrl", "Non-critiques: ${nonCritical.size}, Critiques: ${packages.size - nonCritical.size}")
                if (nonCritical.isEmpty()) {
                    onResult(false, "Only protected system apps are selected")
                    return@launch
                }
                powerKeeperDb.setBatchRestriction(nonCritical.toSet(), state, delayMin)
                android.util.Log.d("HyperCtrl", "setBatchRestriction terminé")
                refresh()
                clearSelection()
                val count = nonCritical.size
                val criticalCount = packages.size - count
                val message = if (criticalCount > 0) {
                    "$count app(s) modified ($criticalCount protected system app(s) skipped)"
                } else {
                    "$count app(s) modified"
                }
                onResult(true, message)
            } catch (e: Exception) {
                android.util.Log.e("HyperCtrl", "applyBatchRestriction error", e)
                onResult(false, "Error: ${e.message}")
            }
        }
    }

    private fun filterApps(apps: List<AppInfo>, filter: FilterOptions): List<AppInfo> {
        return apps.asSequence()
            .filter { app ->
                when {
                    app.isSystemApp && !filter.showSystemApps -> false
                    !app.isSystemApp && !filter.showUserApps -> false
                    filter.category != null && app.category != filter.category -> false
                    filter.restriction != null && app.currentState != filter.restriction -> false
                    filter.searchQuery.isNotBlank() -> {
                        val q = filter.searchQuery.lowercase()
                        app.appName.lowercase().contains(q) || app.pkgName.lowercase().contains(q)
                    }
                    else -> true
                }
            }
            .sortedWith(
                when (filter.sortBy) {
                    SortMode.NAME_ASC -> compareBy { it.appName.lowercase() }
                    SortMode.NAME_DESC -> compareByDescending { it.appName.lowercase() }
                    SortMode.STATE -> compareBy<AppInfo> { it.currentState.ordinal }.thenBy { it.appName }
                    SortMode.CATEGORY -> compareBy<AppInfo> { it.category.label }.thenBy { it.appName }
                }
            )
            .toList()
    }
}
