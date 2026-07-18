package com.walkoud.hypercontroller.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.walkoud.hypercontroller.HyperControllerApp
import com.walkoud.hypercontroller.core.db.DbExecutor
import com.walkoud.hypercontroller.core.db.PowerKeeperDb
import com.walkoud.hypercontroller.core.model.BuiltinTemplates
import com.walkoud.hypercontroller.core.model.TemplateAction
import com.walkoud.hypercontroller.core.safety.BackupManager
import com.walkoud.hypercontroller.core.safety.SystemAppGuard

class AutoConfigService : Service() {

    private val sqlitePath: String by lazy {
        (application as HyperControllerApp).sqlite3Path
    }
    private val executor by lazy { DbExecutor(sqlitePath) }
    private val backupManager by lazy { BackupManager() }
    private val powerKeeperDb by lazy { PowerKeeperDb(executor, backupManager, sqlitePath) }

    companion object {
        const val CHANNEL_ID = "hypercontroller_auto_config"
        const val NOTIFICATION_ID = 1001
        const val PREF_NAME = "settings"
        const val PREF_AUTO_CONFIG = "auto_config"
        const val PREF_TEMPLATE_ID = "auto_config_template"

        fun isEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREF_NAME, 0)
            return prefs.getBoolean(PREF_AUTO_CONFIG, false)
        }

        fun getActiveTemplateId(context: Context): String? {
            val prefs = context.getSharedPreferences(PREF_NAME, 0)
            return prefs.getString(PREF_TEMPLATE_ID, null)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pkgName = intent?.getStringExtra("pkg_name") ?: run {
            startForeground(NOTIFICATION_ID, createNotification())
            return START_STICKY
        }

        applyAutoConfig(pkgName)
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun applyAutoConfig(pkgName: String) {
        if (SystemAppGuard.isCritical(pkgName)) return

        val templateId = getActiveTemplateId(this) ?: return
        val template = BuiltinTemplates.ALL.find { it.id == templateId } ?: return

        for (action in template.actions) {
            when (action) {
                is TemplateAction.SetRestriction -> {
                    try {
                        powerKeeperDb.setAppRestriction(pkgName, action.state, action.delayMin)
                    } catch (_: Exception) {}
                }
                is TemplateAction.AddToWhitelist -> {
                    try {
                        powerKeeperDb.addToWhitelist(action.listName, pkgName)
                    } catch (_: Exception) {}
                }
                else -> {}
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Auto-Config HyperController",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Surveille les nouvelles apps pour appliquer les templates"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("HyperController")
            .setContentText("Auto-config actif — surveillance des nouvelles apps")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .build()
    }
}
