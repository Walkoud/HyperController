package com.walkoud.hypercontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.walkoud.hypercontroller.ui.navigation.AppNavigation
import com.walkoud.hypercontroller.ui.theme.HyperControllerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HyperControllerTheme {
                AppNavigation()
            }
        }
    }
}
