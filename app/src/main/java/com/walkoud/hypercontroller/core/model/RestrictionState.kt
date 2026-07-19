package com.walkoud.hypercontroller.core.model

enum class RestrictionState(val bgControl: String, val label: String, val description: String) {
    NO_RESTRICT("noRestrict", "Full freedom", "No restriction — the app can do anything"),
    MIUI_AUTO("miuiAuto", "Default (MIUI)", "Managed automatically by MIUI"),
    RESTRICT_BG("restrictBg", "Soft saver", "Cuts data/location in background, keeps the app in memory"),
    NO_BG("noBg", "Strict kill", "Kills or freezes the app in the background immediately");

    companion object {
        fun fromBgControl(value: String): RestrictionState {
            return entries.find { it.bgControl.equals(value, ignoreCase = true) } ?: MIUI_AUTO
        }
    }
}
