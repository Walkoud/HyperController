package com.walkoud.hypercontroller.core.db

import com.walkoud.hypercontroller.core.model.AppRestriction
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.core.root.RootShell
import com.walkoud.hypercontroller.core.safety.SafetyValidator
import com.walkoud.hypercontroller.core.safety.SystemAppGuard
import com.walkoud.hypercontroller.core.safety.BackupManager

class PowerKeeperDb(
    private val executor: DbExecutor,
    private val backupManager: BackupManager,
    sqliteBinary: String
) {

    companion object {
        const val DB_BASE = "/data/data/com.miui.powerkeeper/databases"
        const val USER_DB = "$DB_BASE/user_configure.db"
        const val CLOUD_DB = "$DB_BASE/cloud_configure.db"
        const val THERMAL_DB = "$DB_BASE/thermal.db"
    }

    /** Récupère la restriction d'une app depuis userTable */
    fun getAppRestriction(pkgName: String): AppRestriction? {
        val sql = "SELECT bgControl, bgDelayMin, lastConfigured FROM userTable WHERE pkgName='$pkgName' AND userId=0"
        val rows = executor.query(USER_DB, sql)
        if (rows.isEmpty()) return null
        val row = rows.first()
        return AppRestriction(
            bgControl = row["bgControl"] ?: "miuiAuto",
            bgDelayMin = (row["bgDelayMin"] ?: "-1").toIntOrNull() ?: -1,
            lastConfigured = (row["lastConfigured"] ?: "0").toLongOrNull() ?: 0
        )
    }

    /** Applique une restriction à une app (userTable) */
    fun setAppRestriction(pkgName: String, state: RestrictionState, delayMin: Int) {
        SystemAppGuard.guardCheckOrThrow(pkgName)
        require(SafetyValidator.validateBgControl(state.bgControl)) { "Invalid bgControl: ${state.bgControl}" }
        require(SafetyValidator.validateDelay(delayMin)) { "Invalid delay: $delayMin" }

        backupManager.backup(USER_DB, "user_configure")

        val now = System.currentTimeMillis()
        val sql = """
            INSERT OR REPLACE INTO userTable (userId, pkgName, bgControl, bgDelayMin, lastConfigured)
            VALUES (0, '$pkgName', '${state.bgControl}', $delayMin, $now)
        """.trimIndent()

        require(SafetyValidator.validateSqlInjection(sql)) { "SQL injection detected" }
        executor.execSQL(USER_DB, sql)
    }

    /** Applique une restriction à plusieurs apps en batch */
    fun setBatchRestriction(packages: Set<String>, state: RestrictionState, delayMin: Int) {
        val filtered = SystemAppGuard.filterCritical(packages)
        if (filtered.isEmpty()) {
            android.util.Log.d("HyperCtrl", "setBatchRestriction: aucun package après filtrage")
            return
        }

        android.util.Log.d("HyperCtrl", "setBatchRestriction: ${filtered.size} packages, state=${state.bgControl}, delay=$delayMin")

        require(SafetyValidator.validateBgControl(state.bgControl))
        require(SafetyValidator.validateDelay(delayMin))

        backupManager.backup(USER_DB, "user_configure")

        val now = System.currentTimeMillis()
        val sqlList = filtered.map { pkg ->
            """
            INSERT OR REPLACE INTO userTable (userId, pkgName, bgControl, bgDelayMin, lastConfigured)
            VALUES (0, '$pkg', '${state.bgControl}', $delayMin, $now)
            """.trimIndent()
        }

        android.util.Log.d("HyperCtrl", "setBatchRestriction: exécution de ${sqlList.size} requêtes SQL")
        executor.execSQLBatch(USER_DB, sqlList)
        android.util.Log.d("HyperCtrl", "setBatchRestriction: terminé")
    }

    /** Récupère toutes les restrictions de toutes les apps */
    fun getAllRestrictions(): Map<String, AppRestriction> {
        if (!RootShell.dbExists(USER_DB)) return emptyMap()
        return try {
            val sql = "SELECT pkgName, bgControl, bgDelayMin, lastConfigured FROM userTable WHERE userId=0"
            val rows = executor.query(USER_DB, sql)
            rows.mapNotNull { row ->
                val pkg = row["pkgName"] ?: return@mapNotNull null
                pkg to AppRestriction(
                    bgControl = row["bgControl"] ?: "miuiAuto",
                    bgDelayMin = (row["bgDelayMin"] ?: "-1").toIntOrNull() ?: -1,
                    lastConfigured = (row["lastConfigured"] ?: "0").toLongOrNull() ?: 0
                )
            }.toMap()
        } catch (e: DbException) {
            emptyMap()
        }
    }

    /** Récupère la config cloud d'une app (cloudAppTable) */
    fun getCloudAppConfig(pkgName: String): Map<String, String> {
        val sql = "SELECT * FROM cloudAppTable WHERE pkgName='$pkgName'"
        val rows = executor.query(CLOUD_DB, sql)
        return rows.firstOrNull() ?: emptyMap()
    }

    /** Récupère un paramètre global (GlobalFeatureTable) */
    fun getGlobalFeature(key: String): String? {
        val sql = "SELECT configureParam FROM GlobalFeatureTable WHERE configureName='$key'"
        val rows = executor.query(CLOUD_DB, sql)
        return rows.firstOrNull()?.get("configureParam")
    }

    /** Modifie un paramètre global (GlobalFeatureTable) */
    fun setGlobalFeature(key: String, value: String) {
        require(SafetyValidator.validateGlobalKey(key)) { "Invalid global key: $key" }
        require(SafetyValidator.validateSqlInjection(value))

        backupManager.backup(CLOUD_DB, "cloud_configure")

        val sql = """
            INSERT OR REPLACE INTO GlobalFeatureTable (userId, configureName, configureParam)
            VALUES (0, '$key', '$value')
        """.trimIndent()
        executor.execSQL(CLOUD_DB, sql)
    }

    /** Récupère tous les paramètres globaux */
    fun getAllGlobalFeatures(): Map<String, String> {
        val sql = "SELECT configureName, configureParam FROM GlobalFeatureTable"
        val rows = executor.query(CLOUD_DB, sql)
        return rows.associate { row ->
            (row["configureName"] ?: "") to (row["configureParam"] ?: "")
        }
    }

    /** Vérifie si une app est dans levelUtimateSpecialApps */
    fun isInUltimateSpecialApps(pkgName: String): Boolean {
        val list = getGlobalFeature("levelUtimateSpecialApps") ?: return false
        return list.contains(pkgName)
    }

    /** Ajoute une app aux listes d'exemption */
    fun addToWhitelist(listName: String, pkgName: String) {
        val current = getGlobalFeature(listName) ?: ""
        if (current.contains(pkgName)) return
        val updated = if (current.isBlank()) pkgName else "$current:$pkgName"
        setGlobalFeature(listName, updated)
    }
}
