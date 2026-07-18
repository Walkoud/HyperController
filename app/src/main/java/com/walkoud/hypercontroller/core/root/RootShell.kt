package com.walkoud.hypercontroller.core.root

import java.io.BufferedReader
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
            val process = ProcessBuilder("su", "-c", command)
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
            )
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
        val tmp = "/data/local/tmp/hyperctrl_${System.nanoTime()}.sql"
        val result = exec("echo '${sql.replace("'", "'\\''")}' > $tmp")
        if (!result.success) throw RuntimeException("Failed to write temp SQL: ${result.stderr}")
        return tmp
    }

    private fun rmTempSql(path: String) {
        exec("rm -f $path")
    }

    fun dbExists(dbPath: String): Boolean {
        val result = exec("test -f ${dbPath.trim()} && echo EXISTS")
        return result.success && result.stdout.trim() == "EXISTS"
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
}
