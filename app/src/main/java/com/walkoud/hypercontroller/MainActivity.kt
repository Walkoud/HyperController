package com.walkoud.hypercontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.walkoud.hypercontroller.core.root.Sqlite3Manager
import com.walkoud.hypercontroller.ui.navigation.AppNavigation
import com.walkoud.hypercontroller.ui.theme.HyperControllerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as HyperControllerApp
        if (!app.sqlite3Manager.isDeployed()) {
            app.sqlite3Manager.deploy()
        }

        setContent {
            HyperControllerTheme {
                AppNavigation()
            }
        }
    }
}
