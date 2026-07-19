package com.walkoud.hypercontroller.core.model

import java.util.UUID

data class Template(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val isBuiltin: Boolean = false,
    val target: TemplateTarget = TemplateTarget.SELECTED_PACKAGES,
    val actions: List<TemplateAction> = emptyList()
)

sealed class TemplateAction {
    data class SetRestriction(
        val state: RestrictionState,
        val delayMin: Int = -1
    ) : TemplateAction()

    data class SetGlobalFeature(
        val key: String,
        val value: String
    ) : TemplateAction()

    data class AddToWhitelist(
        val listName: String
    ) : TemplateAction()

    data class KillProcess(
        val processName: String = "com.miui.powerkeeper"
    ) : TemplateAction()
}

enum class TemplateTarget(val label: String) {
    ALL_USER_APPS("All user apps"),
    COMMUNICATIONS("Messaging apps"),
    GAMES("Games"),
    SYSTEM_APPS("System apps"),
    SELECTED_PACKAGES("Selected packages")
}

object BuiltinTemplates {
    val UNCHAINED = Template(
        id = "builtin_unchained",
        name = "Unchained",
        description = "Frees selected apps from all MIUI restrictions",
        isBuiltin = true,
        target = TemplateTarget.SELECTED_PACKAGES,
        actions = listOf(
            TemplateAction.SetRestriction(RestrictionState.NO_RESTRICT, -1),
            TemplateAction.AddToWhitelist("levelUtimateSpecialApps"),
            TemplateAction.AddToWhitelist("dozeWhiteListApps")
        )
    )

    val ANTI_KILL_GLOBAL = Template(
        id = "builtin_antikill",
        name = "Anti-Kill Global",
        description = "Disables the aggressiveness of the MIUI task killer",
        isBuiltin = true,
        target = TemplateTarget.SELECTED_PACKAGES,
        actions = listOf(
            TemplateAction.SetGlobalFeature("featureStatus", "false"),
            TemplateAction.SetGlobalFeature("k_policy", "0"),
            TemplateAction.SetGlobalFeature("miui_idle", "false"),
            TemplateAction.SetGlobalFeature("miui_standby", "false"),
            TemplateAction.KillProcess()
        )
    )

    val THERMAL_UNLOCK = Template(
        id = "builtin_thermal",
        name = "Thermal Unlock",
        description = "Pushes back thermal limits for performance",
        isBuiltin = true,
        target = TemplateTarget.SELECTED_PACKAGES,
        actions = listOf(
            TemplateAction.SetGlobalFeature("IECKillBatteryTemp", "50"),
            TemplateAction.SetGlobalFeature("allowedIECBatteryTemp", "350"),
            TemplateAction.SetGlobalFeature("IECScreenOffTimePeriod", "0"),
            TemplateAction.KillProcess()
        )
    )

    val ALL = listOf(UNCHAINED, ANTI_KILL_GLOBAL, THERMAL_UNLOCK)
}
