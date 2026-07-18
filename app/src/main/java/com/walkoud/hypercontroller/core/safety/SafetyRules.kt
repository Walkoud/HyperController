package com.walkoud.hypercontroller.core.safety

import android.util.Log

object SafetyLogger {
    private const val TAG = "HyperController-Safety"

    data class SafetyEvent(
        val type: EventType,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class EventType {
            GUARD_BLOCK,        // SystemAppGuard a bloqué une modification
            SQL_INJECTION_BLOCK, // SafetyValidator a détecté une injection
            INVALID_VALUE,      // Valeur invalide détectée
            BACKUP_CREATED,     // Backup créé avant modification
            BACKUP_RESTORED,    // Backup restauré
            PERMISSION_DENIED,  // Permission root refusée
            DB_ACCESS_ERROR     // Erreur d'accès à la base de données
        }
    }

    private val events = mutableListOf<SafetyEvent>()

    fun log(type: SafetyEvent.EventType, message: String) {
        val event = SafetyEvent(type, message)
        events.add(event)
        when (type) {
            SafetyEvent.EventType.GUARD_BLOCK,
            SafetyEvent.EventType.SQL_INJECTION_BLOCK -> Log.w(TAG, "[${type.name}] $message")
            SafetyEvent.EventType.DB_ACCESS_ERROR -> Log.e(TAG, "[${type.name}] $message")
            else -> Log.i(TAG, "[${type.name}] $message")
        }
    }

    fun getRecentEvents(limit: Int = 50): List<SafetyEvent> {
        return events.takeLast(limit)
    }

    fun clear() {
        events.clear()
    }
}
