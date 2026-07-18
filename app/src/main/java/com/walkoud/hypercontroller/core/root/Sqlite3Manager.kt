package com.walkoud.hypercontroller.core.root

import android.content.Context

class Sqlite3Manager(private val context: Context) {

    companion object {
        private const val BINARY_NAME = "sqlite3"
        private const val BINARY_MODE = "755"
    }

    private val targetPath: String
        get() = "${context.filesDir}/$BINARY_NAME"

    fun deploy(): DeployResult {
        return try {
            val rawId = context.resources.getIdentifier(BINARY_NAME, "raw", context.packageName)
            if (rawId == 0) {
                return DeployResult(false, "sqlite3 binary not found in res/raw")
            }

            val inputStream = context.resources.openRawResource(rawId)
            val bytes = inputStream.readBytes()
            inputStream.close()

            val tempPath = "${context.cacheDir}/$BINARY_NAME"
            java.io.File(tempPath).writeBytes(bytes)

            val copyResult = RootShell.copyFile(tempPath, targetPath)
            if (!copyResult) {
                return DeployResult(false, "Failed to copy sqlite3 binary to $targetPath")
            }

            val chmodResult = RootShell.chmod(targetPath, BINARY_MODE)
            if (!chmodResult) {
                return DeployResult(false, "Failed to chmod sqlite3 binary")
            }

            val verifyResult = RootShell.verifyBinary(targetPath)
            if (!verifyResult) {
                return DeployResult(false, "sqlite3 binary verification failed")
            }

            java.io.File(tempPath).delete()
            DeployResult(true, targetPath)
        } catch (e: Exception) {
            DeployResult(false, e.message ?: "Unknown error during sqlite3 deploy")
        }
    }

    fun getPath(): String = targetPath

    fun isDeployed(): Boolean {
        return RootShell.exec("ls $targetPath").success
    }

    data class DeployResult(
        val success: Boolean,
        val pathOrError: String
    )
}
