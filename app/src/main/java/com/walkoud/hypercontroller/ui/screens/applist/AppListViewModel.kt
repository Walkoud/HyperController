package com.walkoud.hypercontroller.ui.screens.applist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.walkoud.hypercontroller.core.db.DbExecutor
import com.walkoud.hypercontroller.core.db.PowerKeeperDb
import com.walkoud.hypercontroller.core.db.SecurityCenterDb
import com.walkoud.hypercontroller.core.model.AppCategory
import com.walkoud.hypercontroller.core.model.AppInfo
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.core.safety.BackupManager
import com.walkoud.hypercontroller.core.safety.SystemAppGuard
import com.walkoud.hypercontroller.core.util.PackageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class FilterOptions(
    val searchQuery: String = "",
    val category: AppCategory? = null,
    val restriction: RestrictionState? = null,
    val showSystemApps: Boolean = true,
    val showUserApps: Boolean = true,
    val sortBy: SortMode = SortMode.NAME_ASC
)

enum class SortMode { NAME_ASC, NAME_DESC, STATE, CATEGORY }

class AppListViewModel(application: Application) : AndroidViewModel(application) {

    private val packageUtils = PackageUtils(application)
    private val executor = DbExecutor("sqlite3")
    private val backupManager = BackupManager()
    private val powerKeeperDb = PowerKeeperDb(executor, backupManager, "sqlite3")
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
                val allRestrictions = powerKeeperDb.getAllRestrictions()
                val allInstalled = packageUtils.getInstalledApps()

                val apps = allInstalled.map { appInfo ->
                    val restriction = allRestrictions[appInfo.packageName]
                    packageUtils.buildAppInfo(
                        pkgName = appInfo.packageName,
                        restriction = restriction?.let {
                            RestrictionState.fromBgControl(it.bgControl)
                        } ?: RestrictionState.MIUI_AUTO,
                        bgDelayMin = restriction?.bgDelayMin ?: -1
                    )
                }
                _allApps.value = apps.sortedBy { it.appName.lowercase() }
            } catch (e: Exception) {
                _error.value = "Erreur: ${e.message}"
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
                if (packages.isEmpty()) {
                    onResult(false, "Aucune app sélectionnée")
                    return@launch
                }
                val critical = packages.filter { SystemAppGuard.isCritical(it) }
                if (critical.isNotEmpty()) {
                    onResult(false, "Apps système protégées: ${critical.joinToString(", ")}")
                    return@launch
                }
                powerKeeperDb.setBatchRestriction(packages, state, delayMin)
                refresh()
                clearSelection()
                onResult(true, "${packages.size} app(s) modifiée(s)")
            } catch (e: Exception) {
                onResult(false, "Erreur: ${e.message}")
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
