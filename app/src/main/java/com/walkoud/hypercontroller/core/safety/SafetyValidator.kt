package com.walkoud.hypercontroller.core.safety

object SafetyValidator {

    val VALID_BG_CONTROL = setOf("miuiAuto", "noRestrict", "restrictBg", "noBg")

    const val BG_DELAY_MIN = -2
    const val BG_DELAY_MAX = 1440

    val ALLOWED_USER_TABLE_COLUMNS = setOf(
        "bgControl", "bgLocation", "bgDelayMin", "lastConfigured"
    )

    val ALLOWED_CLOUD_APP_COLUMNS = setOf(
        "bgData", "bgLocation", "k_delay", "s_delay",
        "k_policy", "power_state_id", "i_delay"
    )

    val VALID_K_POLICY = setOf(-1, 0, 896)

    // Keys connues pour GlobalFeatureTable
    val VALID_GLOBAL_KEYS = setOf(
        "featureStatus", "k_policy", "k_delay", "s_delay", "i_delay",
        "miui_idle", "miui_standby", "bgData", "bgLocation",
        "SensorControlStatus", "bleScanBlock", "lightIdleStatus",
        "appIdleStatus", "levelUtimateSpecialApps", "dozeWhiteListApps",
        "launchRestrict", "FrozenNewWhiteList"
    )

    private val SQL_INJECTION_PATTERNS = listOf(
        "';", "--", "/*", "*/",
        "DROP ", "ALTER ", "CREATE ", "DELETE ",
        " PRAGMA ", "ATTACH ", "EXEC ", " UNION "
    )

    fun validateBgControl(value: String): Boolean {
        return VALID_BG_CONTROL.contains(value)
    }

    fun validateDelay(value: Int): Boolean {
        return value in BG_DELAY_MIN..BG_DELAY_MAX
    }

    fun validateColumn(dbType: String, column: String): Boolean {
        return when (dbType) {
            "user" -> ALLOWED_USER_TABLE_COLUMNS.contains(column)
            "cloud" -> ALLOWED_CLOUD_APP_COLUMNS.contains(column)
            else -> false
        }
    }

    fun validateGlobalKey(key: String): Boolean {
        return VALID_GLOBAL_KEYS.contains(key)
    }

    fun validateSqlInjection(input: String): Boolean {
        val upper = input.uppercase()
        return !SQL_INJECTION_PATTERNS.any { upper.contains(it) }
    }

    fun sanitizeValue(value: String): String {
        return value
            .replace("'", "")
            .replace(";", "")
            .replace("--", "")
    }
}
