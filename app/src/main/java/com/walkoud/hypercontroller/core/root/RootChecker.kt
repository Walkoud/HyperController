package com.walkoud.hypercontroller.core.root

data class RootCheckResult(
    val hasRoot: Boolean,
    val suAvailable: Boolean,
    val sqliteAvailable: Boolean,
    val sqlitePath: String = "",
    val powerKeeperDbAccessible: Boolean = false,
    val securityCenterDbAccessible: Boolean = false,
    val miuiVersion: String = "",
    val errors: List<String> = emptyList()
)

object RootChecker {

    private val SU_PATHS = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/su/bin/su",
        "/data/adb/magisk/su"
    )

    val POWERKEEPER_DB_BASE = "/data/data/com.miui.powerkeeper/databases"
    val SECURITYCENTER_DB_BASE = "/data/data/com.miui.securitycenter/databases"

    fun check(suggestedSqlitePath: String = ""): RootCheckResult {
        val errors = mutableListOf<String>()

        val hasRoot = testRoot()
        if (!hasRoot) {
            return RootCheckResult(
                hasRoot = false,
                suAvailable = false,
                sqliteAvailable = false,
                errors = listOf("No root access: 'su' command failed")
            )
        }

        val suPath = findSu()
        if (suPath == null) {
            return RootCheckResult(
                hasRoot = true,
                suAvailable = false,
                sqliteAvailable = false,
                errors = listOf("su binary not found at known paths")
            )
        }

        val (sqliteOk, sqlitePath) = findSqlite(suggestedSqlitePath)

        val pkDbOk = if (hasRoot) RootShell.dbExists("$POWERKEEPER_DB_BASE/user_configure.db") else false
        val scDbOk = if (hasRoot) RootShell.dbExists("$SECURITYCENTER_DB_BASE/auto_task.db") else false

        val miuiVer = if (hasRoot) detectMiuiVersion() else ""

        return RootCheckResult(
            hasRoot = hasRoot,
            suAvailable = true,
            sqliteAvailable = sqliteOk,
            sqlitePath = sqlitePath,
            powerKeeperDbAccessible = pkDbOk,
            securityCenterDbAccessible = scDbOk,
            miuiVersion = miuiVer,
            errors = errors
        )
    }

    private fun testRoot(): Boolean {
        val result = RootShell.exec("id")
        return result.success && result.stdout.contains("uid=0")
    }

    private fun findSu(): String? {
        for (path in SU_PATHS) {
            val result = RootShell.exec("[ -x \"$path\" ] && echo 'OK'")
            if (result.success && result.stdout.trim() == "OK") return path
        }
        return null
    }

    private fun findSqlite(suggestedPath: String): Pair<Boolean, String> {
        if (suggestedPath.isNotBlank() && RootShell.verifyBinary(suggestedPath)) {
            return true to suggestedPath
        }
        val paths = listOf(
            "/data/data/com.walkoud.hypercontroller/files/sqlite3",
            "/system/bin/sqlite3",
            "/system/xbin/sqlite3",
            "/data/adb/modules/hypercontroller/sqlite3"
        )
        for (path in paths) {
            if (RootShell.verifyBinary(path)) {
                return true to path
            }
        }
        return false to ""
    }

    private fun detectMiuiVersion(): String {
        val result = RootShell.exec("getprop ro.miui.ui.version.name")
        if (result.success && result.stdout.isNotBlank()) {
            val ver = result.stdout.trim()
            val build = RootShell.exec("getprop ro.build.version.incremental")
            return if (build.success && build.stdout.isNotBlank()) {
                "$ver (${build.stdout.trim()})"
            } else ver
        }
        val osVer = RootShell.exec("getprop ro.build.version.release")
        return if (osVer.success && osVer.stdout.isNotBlank()) "Android ${osVer.stdout.trim()}" else ""
    }
}
