package com.chatspar.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ChatSparGreen,
    onPrimary = ChatSparSurface,
    primaryContainer = ChatSparGreenLight,
    onPrimaryContainer = ChatSparGreen,
    background = ChatSparBackground,
    onBackground = ChatSparText,
    surface = ChatSparSurface,
    onSurface = ChatSparText,
    surfaceVariant = ChatSparGreenLight,
    onSurfaceVariant = ChatSparMuted,
)

@Composable
fun ChatSparTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ChatSparBackground.toArgb()
            window.navigationBarColor = ChatSparSurface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content,
    )
}
