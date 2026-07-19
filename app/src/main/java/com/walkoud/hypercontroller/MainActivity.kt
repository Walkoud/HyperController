package com.walkoud.hypercontroller

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.walkoud.hypercontroller.service.AutoConfigService
import com.walkoud.hypercontroller.ui.navigation.AppNavigation
import com.walkoud.hypercontroller.ui.theme.HyperControllerTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission accordée - démarrer le service si config auto activée
            AutoConfigService.startIfEnabled(this)
        }
        // Si refusée, le service ne pourra pas afficher de notification
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Demander la permission POST_NOTIFICATIONS sur Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            HyperControllerTheme {
                AppNavigation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Démarrer le service auto-config en contexte foreground (visible) pour éviter
        // le blocage par les frameworks tiers (ex: Thanox) sur les starts en background
        AutoConfigService.startIfEnabled(this)
    }
}
