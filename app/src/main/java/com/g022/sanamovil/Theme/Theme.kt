package com.g022.sanamovil.Theme

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

@Composable
fun SanaAppTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF47FA4A),
            secondary = Color(0xFF22B424),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF22B424),
            secondary = Color(0xFF188F1A),
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF5F5F5)
        )
    }

    // --- CONFIGURACIÓN DE LA BARRA DE ESTADO (NOTIFICACIONES) ---
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            // 1. Pinta el fondo de la barra del mismo color que el background de tu tema
            window.statusBarColor = colorScheme.background.toArgb()

            // 2. Le dice al sistema: "Si NO estoy en modo oscuro, pon los iconos en color oscuro"
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}