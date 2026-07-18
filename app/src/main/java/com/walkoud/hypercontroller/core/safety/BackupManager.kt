package com.walkoud.hypercontroller.core.safety

import com.walkoud.hypercontroller.core.root.RootShell

data class BackupInfo(
    val dbName: String,
    val timestamp: Long,
    val filePath: String,
    val fileSize: String = ""
)

class BackupManager {

    companion object {
        const val BACKUP_DIR = "/data/local/tmp/hypercontroller_backups"
        const val MAX_BACKUPS_PER_DB = 5
    }

    private val backups = mutableListOf<BackupInfo>()

    init {
        ensureBackupDir()
    }

    private fun ensureBackupDir() {
        RootShell.exec("mkdir -p $BACKUP_DIR")
    }

    fun backup(dbPath: String, dbName: String): BackupInfo {
        val timestamp = System.currentTimeMillis()
        val backupPath = "$BACKUP_DIR/${dbName}_backup_$timestamp.db"

        val result = RootShell.copyFile(dbPath, backupPath)
        if (!result) {
            throw BackupException("Failed to backup $dbPath to $backupPath")
        }

        val info = BackupInfo(
            dbName = dbName,
            timestamp = timestamp,
            filePath = backupPath
        )
        backups.add(info)
        rotateBackups(dbName)
        return info
    }

    fun restore(dbName: String, timestamp: Long): Boolean {
        val backup = backups.find { it.dbName == dbName && it.timestamp == timestamp }
            ?: return false
        return restoreInternal(dbName, backup.filePath)
    }

    fun restoreLatest(dbName: String): Boolean {
        val dbBackups = backups
            .filter { it.dbName == dbName }
            .sortedByDescending { it.timestamp }
        val latest = dbBackups.firstOrNull() ?: return false
        return restoreInternal(dbName, latest.filePath)
    }

    fun listBackups(dbName: String): List<BackupInfo> {
        val result = RootShell.exec("ls -lh $BACKUP_DIR/${dbName}_backup_*.db 2>/dev/null")
        if (!result.success || result.stdout.isBlank()) return emptyList()

        return result.stdout.lines().mapNotNull { line ->
            val parts = line.split("\\s+".toRegex())
            if (parts.size < 9) return@mapNotNull null
            val filePath = parts[8]
            val fileName = filePath.substringAfterLast("/")
            val ts = fileName
                .removePrefix("${dbName}_backup_")
                .removeSuffix(".db")
                .toLongOrNull() ?: return@mapNotNull null
            BackupInfo(
                dbName = dbName,
                timestamp = ts,
                filePath = filePath,
                fileSize = parts[4]
            )
        }.sortedByDescending { it.timestamp }
    }

    fun restoreAll(): Map<String, Boolean> {
        val dbNames = backups.map { it.dbName }.distinct()
        return dbNames.associateWith { restoreLatest(it) }
    }

    private fun restoreInternal(dbName: String, backupPath: String): Boolean {
        val dbPath = when (dbName) {
            "user_configure" -> "/data/data/com.miui.powerkeeper/databases/$dbName.db"
            "cloud_configure" -> "/data/data/com.miui.powerkeeper/databases/$dbName.db"
            "thermal" -> "/data/data/com.miui.powerkeeper/databases/$dbName.db"
            else -> return false
        }

        val result = RootShell.exec("cp \"$backupPath\" \"$dbPath\"")
        if (result.success) {
            RootShell.pkill("com.miui.powerkeeper")
        }
        return result.success
    }

    private fun rotateBackups(dbName: String) {
        val result = RootShell.exec(
            "ls -1t $BACKUP_DIR/${dbName}_backup_*.db 2>/dev/null | tail -n +$((MAX_BACKUPS_PER_DB + 1))"
        )
        if (result.success && result.stdout.isNotBlank()) {
            result.stdout.lines().forEach { line ->
                if (line.isNotBlank()) {
                    RootShell.exec("rm \"$line\"")
                }
            }
        }
    }
}

class BackupException(message: String) : Exception(message)
