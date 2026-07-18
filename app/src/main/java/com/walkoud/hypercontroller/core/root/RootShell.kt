package com.walkoud.hypercontroller.core.root

import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class RootResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val exitCode: Int
)

object RootShell {

    private const val TIMEOUT_MS = 15_000L

    fun exec(command: String): RootResult {
        return try {
            val scriptFile = writeScript(command)
            val process = ProcessBuilder("su", "--mount-master", "-c", "/system/bin/sh $scriptFile")
                .redirectErrorStream(false)
                .start()

            val stdout = readStream(process.inputStream)
            val stderr = readStream(process.errorStream)

            val exited = process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)
            val exitCode = if (exited) process.exitValue() else {
                process.destroyForcibly()
                -1
            }

            RootResult(
                success = exited && exitCode == 0,
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode
            ).also {
                if (!it.success) {
                    Log.w("HyperCtrl", "exec FAILED [$exitCode]: cmd='${command.take(80)}' stderr='${it.stderr.take(120)}'")
                }
                File(scriptFile).delete()
            }
        } catch (e: Exception) {
            RootResult(
                success = false,
                stdout = "",
                stderr = e.message ?: "Unknown error",
                exitCode = -1
            )
        }
    }

    fun execSQLite(sqliteBinary: String, dbPath: String, sql: String): RootResult {
        val sqlFile = writeTempSql(sql)
        return try {
            exec("${sqliteBinary.trim()} ${dbPath.trim()} < $sqlFile")
        } finally {
            rmTempSql(sqlFile)
        }
    }

    fun execSQLiteQuery(sqliteBinary: String, dbPath: String, sql: String): RootResult {
        val sqlFile = writeTempSql(sql)
        return try {
            exec("${sqliteBinary.trim()} -header -separator PIPE ${dbPath.trim()} < $sqlFile")
        } finally {
            rmTempSql(sqlFile)
        }
    }

    private fun writeTempSql(sql: String): String {
        val tmp = "/data/data/com.walkoud.hypercontroller/files/hyperctrl_${System.nanoTime()}.sql"
        File(tmp).writeText(sql)
        return tmp
    }

    private fun rmTempSql(path: String) {
        File(path).delete()
    }

    fun dbExists(dbPath: String): Boolean {
        val result = exec("${getSqlitePath()} ${dbPath.trim()} .tables")
        return result.success
    }

    private fun getSqlitePath(): String {
        return "/data/data/com.walkoud.hypercontroller/files/sqlite3"
    }

    fun chmod(path: String, mode: String = "755"): Boolean {
        return exec("chmod $mode \"$path\"").success
    }

    fun copyFile(src: String, dst: String): Boolean {
        return exec("cp \"$src\" \"$dst\"").success
    }

    fun verifyBinary(binaryPath: String): Boolean {
        val result = exec("${binaryPath.trim()} --version")
        return result.success && result.stdout.isNotBlank()
    }

    fun pgrep(processName: String): Boolean {
        val result = exec("pgrep -f \"$processName\"")
        return result.success
    }

    fun pkill(processName: String): Boolean {
        val result = exec("pkill -f \"$processName\"")
        return result.success || result.exitCode == 1
    }

    private fun readStream(stream: java.io.InputStream): String {
        return BufferedReader(InputStreamReader(stream)).readText().trim()
    }

    private fun writeScript(command: String): String {
        val tmp = "/data/data/com.walkoud.hypercontroller/files/hyperctrl_exec_${System.nanoTime()}.sh"
        val f = File(tmp)
        f.writeText(command)
        f.setReadable(true, false)
        f.setExecutable(true, false)
        return tmp
    }
}
