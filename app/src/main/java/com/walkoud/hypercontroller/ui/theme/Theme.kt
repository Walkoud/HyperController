package com.walkoud.hypercontroller.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF90CAF9),
    secondary = Color(0xFF80CBC4),
    tertiary = Color(0xFFFFCC80),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    error = Color(0xFFEF5350),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0),
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1565C0),
    secondary = Color(0xFF00897B),
    tertiary = Color(0xFFE65100),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    error = Color(0xFFD32F2F),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    onError = Color.White
)

object AppColors {
    val noRestrict = Color(0xFF4CAF50)
    val miuiAuto = Color(0xFFFF9800)
    val restrictBg = Color(0xFFFF5722)
    val noBg = Color(0xFFF44336)
    val conflict = Color(0xFFE040FB)
}

@Composable
fun HyperControllerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
