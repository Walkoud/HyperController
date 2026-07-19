package com.walkoud.hypercontroller.service

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.walkoud.hypercontroller.HyperControllerApp
import com.walkoud.hypercontroller.core.db.DbExecutor
import com.walkoud.hypercontroller.core.db.PowerKeeperDb
import com.walkoud.hypercontroller.core.model.BuiltinTemplates
import com.walkoud.hypercontroller.core.model.RestrictionState
import com.walkoud.hypercontroller.core.model.Template
import com.walkoud.hypercontroller.core.model.TemplateAction
import com.walkoud.hypercontroller.core.model.TemplateTarget
import com.walkoud.hypercontroller.core.root.RootShell
import com.walkoud.hypercontroller.core.safety.BackupManager
import com.walkoud.hypercontroller.core.safety.SystemAppGuard
import org.json.JSONArray
import org.json.JSONObject

class AutoConfigService : Service() {

    private val sqlitePath: String by lazy {
        (application as HyperControllerApp).sqlite3Path
    }
    private val executor by lazy { DbExecutor(sqlitePath) }
    private val backupManager by lazy { BackupManager() }
    private val powerKeeperDb by lazy { PowerKeeperDb(executor, backupManager, sqlitePath) }
    private val notificationManager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val appInstalledReceiver = AppInstalledReceiver()
    private var isRunning = false

    companion object {
        const val CHANNEL_ID_FOREGROUND = "hypercontroller_foreground"
        const val CHANNEL_ID_EVENTS = "hypercontroller_events"
        const val NOTIFICATION_ID_FOREGROUND = 1001
        const val NOTIFICATION_ID_EVENT = 1002
        const val ACTION_START = "com.walkoud.hypercontroller.ACTION_START_AUTOCONFIG"
        const val ACTION_STOP = "com.walkoud.hypercontroller.ACTION_STOP_AUTOCONFIG"

        fun isEnabled(context: Context): Boolean {
            val prefs = context.getSharedPreferences("settings", 0)
            return prefs.getBoolean("auto_config", false)
        }

        fun getActiveTemplateId(context: Context): String? {
            val prefs = context.getSharedPreferences("settings", 0)
            return prefs.getString("auto_config_template_id", null)
        }

        fun startIfEnabled(context: Context) {
            if (isEnabled(context)) {
                // L'app a le root : on s'octroie les app-ops nécessaires pour que le
                // foreground service (et sa notification persistante) ne soit pas bloqué
                // par la ROM (MIUI/HyperOS/Thanox), même après reboot.
                RootShell.grantBackgroundAppOps(context.packageName)

                val intent = Intent(context, AutoConfigService::class.java).apply {
                    action = ACTION_START
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                Log.d("AutoConfigService", "Service démarré")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AutoConfigService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
            Log.d("AutoConfigService", "Service arrêté")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            when (intent?.action) {
                ACTION_START -> {
                    if (!isRunning) {
                        startForegroundService()
                    }
                }
                ACTION_STOP -> {
                    stopForegroundService()
                }
                Intent.ACTION_PACKAGE_ADDED -> {
                    val pkgName = intent.data?.schemeSpecificPart
                    if (pkgName != null && intent.getBooleanExtra(Intent.EXTRA_REPLACING, false) == false) {
                        applyAutoConfig(pkgName)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AutoConfigService", "Erreur dans onStartCommand", e)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        try {
            createNotificationChannels()
            val notification = createForegroundNotification()
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID_FOREGROUND,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0
            )
            Log.d("AutoConfigService", "startForeground OK")

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }
            registerReceiver(appInstalledReceiver, filter)

            isRunning = true
            Log.d("AutoConfigService", "Foreground service démarré")
        } catch (e: Exception) {
            Log.e("AutoConfigService", "Échec du démarrage foreground", e)
        }
    }

    private fun stopForegroundService() {
        try {
            unregisterReceiver(appInstalledReceiver)
        } catch (e: IllegalArgumentException) {
            // Déjà unregistered
        }
        stopForeground(true)
        stopSelf()
        isRunning = false
        Log.d("AutoConfigService", "Foreground service arrêté")
    }

    private fun applyAutoConfig(pkgName: String) {
        if (SystemAppGuard.isCritical(pkgName)) {
            Log.d("AutoConfigService", "App critique ignorée: $pkgName")
            return
        }

        val templateId = getActiveTemplateId(this) ?: run {
            Log.d("AutoConfigService", "Aucun template sélectionné")
            return
        }

        val template = loadTemplate(templateId) ?: run {
            Log.d("AutoConfigService", "Template non trouvé: $templateId")
            return
        }

        Log.d("AutoConfigService", "Application du template '${template.name}' à $pkgName")
        var appliedCount = 0

        for (action in template.actions) {
            try {
                when (action) {
                    is TemplateAction.SetRestriction -> {
                        powerKeeperDb.setAppRestriction(pkgName, action.state, action.delayMin)
                        appliedCount++
                    }
                    is TemplateAction.AddToWhitelist -> {
                        powerKeeperDb.addToWhitelist(action.listName, pkgName)
                        appliedCount++
                    }
                    is TemplateAction.SetGlobalFeature -> {
                        powerKeeperDb.setGlobalFeature(action.key, action.value)
                        appliedCount++
                    }
                    is TemplateAction.KillProcess -> {
                        // Ignorer le kill process pour les nouvelles apps
                    }
                }
            } catch (e: Exception) {
                Log.e("AutoConfigService", "Erreur lors de l'application de l'action", e)
            }
        }

        // Envoyer une notification
        showEventNotification("Auto-config applied", "Template '${template.name}' was applied to $pkgName ($appliedCount action(s))")
        Log.d("AutoConfigService", "Auto-config terminé pour $pkgName ($appliedCount action(s) appliquée(s))")
    }

    private fun loadTemplate(templateId: String): Template? {
        // Chercher d'abord dans les templates intégrés
        BuiltinTemplates.ALL.find { it.id == templateId }?.let { return it }

        // Chercher dans les templates utilisateur
        val prefs = getSharedPreferences("templates", 0)
        val json = prefs.getString("user_templates", null) ?: return null

        return try {
            val arr = JSONArray(json)
            (0 until arr.length())
                .map { i -> jsonToTemplate(arr.getJSONObject(i)) }
                .find { it.id == templateId }
        } catch (e: Exception) {
            Log.e("AutoConfigService", "Erreur de chargement des templates", e)
            null
        }
    }

    private fun jsonToTemplate(json: JSONObject): Template {
        val actionsArr = json.getJSONArray("actions")
        val actions = (0 until actionsArr.length()).map { i ->
            val act = actionsArr.getJSONObject(i)
            when (act.getString("type")) {
                "SET_RESTRICTION" -> TemplateAction.SetRestriction(
                    state = com.walkoud.hypercontroller.core.model.RestrictionState.fromBgControl(act.getString("state")),
                    delayMin = act.optInt("delayMin", -1)
                )
                "SET_GLOBAL" -> TemplateAction.SetGlobalFeature(
                    key = act.getString("key"),
                    value = act.getString("value")
                )
                "ADD_TO_WHITELIST" -> TemplateAction.AddToWhitelist(
                    listName = act.getString("list")
                )
                "KILL_PROCESS" -> TemplateAction.KillProcess(
                    processName = act.optString("process", "com.miui.powerkeeper")
                )
                else -> throw IllegalArgumentException("Unknown action type")
            }
        }
        return Template(
            id = json.getString("id"),
            name = json.getString("name"),
            description = json.optString("description", ""),
            isBuiltin = false,
            target = com.walkoud.hypercontroller.core.model.TemplateTarget.valueOf(json.getString("target")),
            actions = actions
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal pour le service foreground
            val foregroundChannel = NotificationChannel(
                CHANNEL_ID_FOREGROUND,
                "HyperController Service",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Watches for newly installed apps"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(foregroundChannel)

            // Canal pour les événements
            val eventsChannel = NotificationChannel(
                CHANNEL_ID_EVENTS,
                "HyperController Events",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when templates are applied"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(eventsChannel)
        }
    }

    private fun createForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
            .setContentTitle("HyperController")
            .setContentText("Auto-config active — watching for new apps")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showEventNotification(title: String, message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID_EVENTS)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID_EVENT, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(appInstalledReceiver)
        } catch (e: IllegalArgumentException) {
            // Ignorer
        }
        Log.d("AutoConfigService", "Service détruit")
    }

    private inner class AppInstalledReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    val pkgName = intent.data?.schemeSpecificPart
                    if (pkgName != null && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                        applyAutoConfig(pkgName)
                    }
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    // Ignorer les mises à jour
                }
            }
        }
    }
}
