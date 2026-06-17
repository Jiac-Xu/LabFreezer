package com.labfreezer.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode { LIGHT, DARK, SYSTEM }

object ThemePreferences {
    private const val PREFS = "theme_prefs"
    private const val KEY = "theme_mode"

    fun getMode(context: Context): ThemeMode {
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY, 2)
        return ThemeMode.entries.getOrElse(v) { ThemeMode.SYSTEM }
    }

    fun setMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY, mode.ordinal).apply()
    }
}

val LocalThemeMode = staticCompositionLocalOf { ThemeMode.SYSTEM }
