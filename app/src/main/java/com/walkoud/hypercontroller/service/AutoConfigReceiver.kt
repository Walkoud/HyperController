package com.walkoud.hypercontroller.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.walkoud.hypercontroller.core.safety.SystemAppGuard

class AutoConfigReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pkgName = intent.data?.schemeSpecificPart ?: return
        if (!AutoConfigService.isEnabled(context)) return
        if (SystemAppGuard.isCritical(pkgName)) return

        val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
        if (isReplacing) return

        val serviceIntent = Intent(context, AutoConfigService::class.java).apply {
            putExtra("pkg_name", pkgName)
            putExtra("template_id", AutoConfigService.getActiveTemplateId(context))
        }
        context.startService(serviceIntent)
    }
}
