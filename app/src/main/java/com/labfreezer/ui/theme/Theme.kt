package com.labfreezer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val HyperLightColorScheme = lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EAFF),
    onPrimaryContainer = Color(0xFF001C3A),
    secondary = Color(0xFF545F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8E3F8),
    onSecondaryContainer = Color(0xFF111C2B),
    tertiary = Color(0xFF6E5676),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF8D8FF),
    onTertiaryContainer = Color(0xFF271430),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF8F9FA),
    onBackground = Color(0xFF1A1C1E),
    surface = Color(0xFFF8F9FA),
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFEDF0F5),
    onSurfaceVariant = Color(0xFF43474E),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    inverseSurface = Color(0xFF2F3033),
    inverseOnSurface = Color(0xFFF1F0F4),
    inversePrimary = Color(0xFFA1CAFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF2F3F5),
    surfaceContainerHigh = Color(0xFFECEDF0),
)

private val HyperDarkColorScheme = darkColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color(0xFF00315C),
    primaryContainer = Color(0xFF004982),
    onPrimaryContainer = Color(0xFFD6EAFF),
    secondary = Color(0xFFBCC7DB),
    onSecondary = Color(0xFF263140),
    secondaryContainer = Color(0xFF3C4758),
    onSecondaryContainer = Color(0xFFD8E3F8),
    tertiary = Color(0xFFDBBCE2),
    onTertiary = Color(0xFF3D2947),
    tertiaryContainer = Color(0xFF553F5F),
    onTertiaryContainer = Color(0xFFF8D8FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF43474E),
    onSurfaceVariant = Color(0xFFC3C6CF),
    outline = Color(0xFF8D9199),
    outlineVariant = Color(0xFF43474E),
    inverseSurface = Color(0xFFE2E2E6),
    inverseOnSurface = Color(0xFF2F3033),
    inversePrimary = Color(0xFF0061A4),
    surfaceContainerLow = Color(0xFF1A1C21),
    surfaceContainer = Color(0xFF1F2126),
    surfaceContainerHigh = Color(0xFF292C31),
)

private val HyperShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun LabFreezerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val resolvedDarkTheme = when (LocalThemeMode.current) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }
    val colorScheme = if (resolvedDarkTheme) HyperDarkColorScheme else HyperLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.navigationBarColor = android.graphics.Color.argb(
                (colorScheme.background.alpha * 255).toInt(),
                (colorScheme.background.red * 255).toInt(),
                (colorScheme.background.green * 255).toInt(),
                (colorScheme.background.blue * 255).toInt()
            )
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !resolvedDarkTheme
            controller.isAppearanceLightNavigationBars = !resolvedDarkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = HyperShapes,
        content = content
    )
}
