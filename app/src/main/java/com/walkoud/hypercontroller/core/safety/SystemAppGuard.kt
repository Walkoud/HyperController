package com.walkoud.hypercontroller.core.safety

object SystemAppGuard {

    val CRITICAL_SYSTEM_APPS = setOf(
        "com.android.systemui",
        "com.android.phone",
        "com.android.settings",
        "com.miui.home",
        "com.miui.securitycenter",
        "com.miui.securityadd",
        "com.miui.powerkeeper",
        "com.miui.securitycore",
        "com.lbe.security.miui",
        "com.android.providers.settings",
        "com.android.providers.telephony",
        "com.android.bluetooth",
        "com.android.nfc",
        "com.google.android.gms",
        "com.google.android.gsf",
        "com.android.vending",
        "com.xiaomi.xmsf",
        "com.android.incallui",
        "com.android.server.telecom",
    )

    val SENSITIVE_SYSTEM_APPS = setOf(
        "com.android.camera",
        "com.miui.gallery",
        "com.android.contacts",
        "com.android.mms",
        "com.android.email",
        "com.miui.weather2",
        "com.miui.player",
        "com.miui.notes",
        "com.miui.videoplayer",
        "com.xiaomi.market"
    )

    fun isCritical(pkgName: String): Boolean {
        return CRITICAL_SYSTEM_APPS.contains(pkgName)
    }

    fun isSensitive(pkgName: String): Boolean {
        return SENSITIVE_SYSTEM_APPS.contains(pkgName)
    }

    fun filterCritical(packages: Set<String>): Set<String> {
        return packages.filterNot { isCritical(it) }.toSet()
    }

    fun guardCheckOrThrow(pkgName: String) {
        if (isCritical(pkgName)) {
            throw SecurityException(
                "Cannot modify critical system app: $pkgName. " +
                        "This app is protected by HyperController's SystemAppGuard."
            )
        }
    }
}
