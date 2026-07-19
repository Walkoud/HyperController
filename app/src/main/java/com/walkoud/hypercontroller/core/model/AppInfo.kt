package com.walkoud.hypercontroller.core.model

import android.graphics.drawable.Drawable

data class AppInfo(
    val pkgName: String,
    val appName: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val category: AppCategory = AppCategory.OTHER,
    val currentState: RestrictionState = RestrictionState.MIUI_AUTO,
    val bgDelayMin: Int = -1,
    val powerStateId: Int = -1,
    val kPolicy: Int = -1,
    val hasNotificationPermission: Boolean = false
) {
    val isRestricted: Boolean
        get() = currentState == RestrictionState.RESTRICT_BG || currentState == RestrictionState.NO_BG

    val hasConflict: Boolean
        get() = isRestricted && hasNotificationPermission
}

enum class AppCategory(val label: String) {
    COMMUNICATION("Messaging"),
    SOCIAL("Social"),
    GAME("Games"),
    MAPS_NAV("Navigation"),
    MUSIC("Music"),
    VIDEO("Video"),
    TOOLS("Tools"),
    BENCHMARK("Benchmark"),
    OTHER("Other")
}
