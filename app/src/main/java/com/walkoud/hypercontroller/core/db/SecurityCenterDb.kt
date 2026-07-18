package com.walkoud.hypercontroller.core.db

import com.walkoud.hypercontroller.core.model.AutoTask

class SecurityCenterDb(private val executor: DbExecutor) {

    companion object {
        const val DB_BASE = "/data/data/com.miui.securitycenter/databases"
        const val AUTO_TASK_DB = "$DB_BASE/auto_task.db"
        const val NO_KILL_DB = "$DB_BASE/cloud_no_kill_pkg.db"
    }

    fun getAutoTasks(): List<AutoTask> {
        val sql = "SELECT id, task_uuid, task_enable, task_title FROM auto_task_table"
        val rows = executor.query(AUTO_TASK_DB, sql)
        return rows.map { row ->
            AutoTask(
                id = (row["id"] ?: "0").toIntOrNull() ?: 0,
                uuid = row["task_uuid"] ?: "",
                enabled = (row["task_enable"] ?: "0") == "1",
                title = row["task_title"] ?: ""
            )
        }
    }

    fun getNoKillPackages(): List<String> {
        val sql = "SELECT pkg_name FROM no_kill_pkg"
        val rows = executor.query(NO_KILL_DB, sql)
        return rows.mapNotNull { it["pkg_name"] }
    }

    fun addNoKillPackage(pkgName: String) {
        val sql = "INSERT OR IGNORE INTO no_kill_pkg (pkg_name) VALUES ('$pkgName')"
        executor.execSQL(NO_KILL_DB, sql)
    }

    fun removeNoKillPackage(pkgName: String) {
        val sql = "DELETE FROM no_kill_pkg WHERE pkg_name='$pkgName'"
        executor.execSQL(NO_KILL_DB, sql)
    }
}
