package com.walkoud.hypercontroller.core.db

import com.walkoud.hypercontroller.core.root.RootResult
import com.walkoud.hypercontroller.core.root.RootShell

class DbExecutor(private val sqliteBinary: String) {

    private val forbiddenPrefixes = listOf("DROP", "ALTER", "CREATE", "DELETE", "INSERT INTO sqlite_")

    fun query(dbPath: String, sql: String): List<Map<String, String>> {
        validateSql(sql)
        val result = RootShell.execSQLiteQuery(sqliteBinary.trim(), dbPath.trim(), sql)
        if (!result.success) {
            throw DbException("Query failed [db=${dbPath.trim()}]: ${result.stderr.ifBlank { result.stdout }}")
        }
        return parseResults(result.stdout)
    }

    fun execSQL(dbPath: String, sql: String) {
        validateSql(sql)
        val upper = sql.trim().uppercase()
        if (upper.startsWith("DROP") || upper.startsWith("ALTER") ||
            upper.startsWith("CREATE") || upper.startsWith("DELETE")
        ) {
            throw DbException("Forbidden SQL operation: only UPDATE and INSERT are allowed")
        }
        val result = RootShell.execSQLite(sqliteBinary, dbPath, sql)
        if (!result.success) {
            throw DbException("Exec failed: ${result.stderr}")
        }
    }

    fun execSQLBatch(dbPath: String, sqlList: List<String>) {
        val batch = sqlList.joinToString(";\n")
        validateSql(batch)

        val containsForbidden = forbiddenPrefixes.any { prefix ->
            batch.uppercase().contains(prefix)
        }
        if (containsForbidden) {
            throw DbException("Forbidden SQL operation in batch")
        }

        val fullResult = RootShell.execSQLite(sqliteBinary, dbPath, batch)
        if (!fullResult.success) {
            throw DbException("Batch exec failed: ${fullResult.stderr}")
        }
    }

    fun rawExec(command: String): RootResult {
        return RootShell.exec("$sqliteBinary $command")
    }

    private fun validateSql(sql: String) {
        if (sql.contains(";") && !sql.trim().endsWith(";")) {
            val parts = sql.split(";")
            if (parts.size > 2) {
                throw DbException("Multiple SQL statements not allowed in single exec")
            }
        }
        if (FORBIDDEN_PATTERNS.any { sql.uppercase().contains(it) }) {
            throw DbException("SQL contains forbidden patterns (DROP, ALTER, CREATE, DELETE)")
        }
    }

    private fun parseResults(output: String): List<Map<String, String>> {
        if (output.isBlank()) return emptyList()

        val lines = output.lines().filter { it.isNotBlank() }
        if (lines.size < 2) return emptyList()

        val headers = lines[0].split("PIPE")
        val rows = lines.drop(1)

        return rows.map { row ->
            val values = row.split("PIPE")
            headers.mapIndexed { index, header ->
                header.trim() to (values.getOrNull(index)?.trim() ?: "")
            }.toMap()
        }
    }

    companion object {
        val FORBIDDEN_PATTERNS = listOf(
            "DROP ", "ALTER ", "CREATE ", "DELETE ",
            " PRAGMA ", "ATTACH ", "DETACH ", "REINDEX "
        )
    }
}

class DbException(message: String) : Exception(message)
