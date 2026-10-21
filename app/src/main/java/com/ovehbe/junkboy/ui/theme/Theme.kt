package com.ovehbe.junkboy.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = darkPrimary,
    secondary = darkSecondary,
    background = darkBackground,
    surface = darkSurface,
    onPrimary = darkOnPrimary,
    onSecondary = darkOnSecondary,
    onBackground = darkOnBackground,
    onSurface = darkOnSurface,
    surfaceVariant = darkSurface,
    onSurfaceVariant = darkOnSurface
)

private val LightColorScheme = lightColorScheme(
    primary = lightPrimary,
    secondary = lightSecondary,
    background = lightBackground,
    surface = lightSurface,
    onPrimary = lightOnPrimary,
    onSecondary = lightOnSecondary,
    onBackground = lightOnBackground,
    onSurface = lightOnSurface,
    surfaceVariant = lightSurface,
    onSurfaceVariant = lightOnSurface
)

@Composable
fun JunkboyTheme(
    // Screens use the fixed light-mode DesignColors, so the Material scheme must stay light too.
    darkTheme: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disabled to use design system colors
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
} 