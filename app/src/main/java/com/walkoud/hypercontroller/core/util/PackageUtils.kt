package com.walkoud.hypercontroller.core.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.walkoud.hypercontroller.core.model.AppInfo
import com.walkoud.hypercontroller.core.model.AppCategory
import com.walkoud.hypercontroller.core.model.RestrictionState

class PackageUtils(private val context: Context) {

    fun getInstalledApps(): List<ApplicationInfo> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
    }

    fun getAppName(pkgName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(pkgName, 0)
            pm.getApplicationLabel(appInfo)?.toString() ?: pkgName
        } catch (_: Exception) {
            pkgName
        }
    }

    fun getAppIcon(pkgName: String): Drawable? {
        return try {
            val pm = context.packageManager
            pm.getApplicationIcon(pkgName)
        } catch (_: Exception) {
            null
        }
    }

    fun buildAppInfo(
        pkgName: String,
        restriction: RestrictionState,
        bgDelayMin: Int = -1,
        powerStateId: Int = -1,
        kPolicy: Int = -1
    ): AppInfo {
        return AppInfo(
            pkgName = pkgName,
            appName = getAppName(pkgName),
            icon = getAppIcon(pkgName),
            isSystemApp = isSystemApp(pkgName),
            category = AppClassifier.classify(pkgName),
            currentState = restriction,
            bgDelayMin = bgDelayMin,
            powerStateId = powerStateId,
            kPolicy = kPolicy,
            hasNotificationPermission = false
        )
    }

    private fun isSystemApp(pkgName: String): Boolean {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(pkgName, 0)
            (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (_: Exception) {
            false
        }
    }
}
