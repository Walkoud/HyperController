package com.walkoud.hypercontroller.core.model

enum class RestrictionState(val bgControl: String, val label: String, val description: String) {
    NO_RESTRICT("noRestrict", "Liberté totale", "Aucune restriction — l'app peut tout faire"),
    MIUI_AUTO("miuiAuto", "Par défaut (MIUI)", "Géré automatiquement par MIUI"),
    RESTRICT_BG("restrictBg", "Économie soft", "Coupe data/location en arrière-plan, garde l'app en mémoire"),
    NO_BG("noBg", "Kill strict", "Tue ou gèle l'app en arrière-plan immédiatement");

    companion object {
        fun fromBgControl(value: String): RestrictionState {
            return entries.find { it.bgControl.equals(value, ignoreCase = true) } ?: MIUI_AUTO
        }
    }
}
