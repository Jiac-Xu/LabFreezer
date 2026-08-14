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

    fun isAutoKeyboardFromBottomBar(): Boolean =
        prefs.getBoolean(KEY_AUTO_KEYBOARD_BOTTOM_BAR, false)
    fun setAutoKeyboardFromBottomBar(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_KEYBOARD_BOTTOM_BAR, enabled).apply()
    }

    companion object {
        private const val PREFS = "personalization_prefs"
        private const val KEY_AUTO_KEYBOARD_BOTTOM_BAR = "auto_keyboard_bottom_bar"

        fun getAutoKeyboardFromBottomBar(context: Context): Boolean =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_AUTO_KEYBOARD_BOTTOM_BAR, false)
    }
}