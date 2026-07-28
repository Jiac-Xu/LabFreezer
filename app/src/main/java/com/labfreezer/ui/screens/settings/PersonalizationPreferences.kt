package com.labfreezer.ui.screens.settings

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonalizationPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("personalization_prefs", Context.MODE_PRIVATE)

    fun getInputMode(): String = prefs.getString("input_mode", "CAMERA") ?: "CAMERA"
    fun setInputMode(mode: String) {
        prefs.edit().putString("input_mode", mode).apply()
    }

    fun isTempModeAllowed(): Boolean = prefs.getBoolean("temp_mode_allowed", false)
    fun setTempModeAllowed(allowed: Boolean) {
        prefs.edit().putBoolean("temp_mode_allowed", allowed).apply()
    }

    fun isAutoSaveEnabled(): Boolean = prefs.getBoolean("auto_save_enabled", false)
    fun setAutoSaveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_save_enabled", enabled).apply()
    }

    fun isZoomSliderEnabled(): Boolean = prefs.getBoolean("zoom_slider_enabled", true)
    fun setZoomSliderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("zoom_slider_enabled", enabled).apply()
    }

    fun isSearchHistoryEnabled(): Boolean = prefs.getBoolean("search_history_enabled", true)
    fun setSearchHistoryEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("search_history_enabled", enabled).apply()
    }
}