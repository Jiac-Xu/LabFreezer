package com.labfreezer.ui.screens.boxgrid

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InputModePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("input_mode_prefs", Context.MODE_PRIVATE)

    fun getInputMode(): InputMode {
        val saved = prefs.getString("input_mode", InputMode.CAMERA.name)
        return runCatching {
            InputMode.valueOf(saved ?: InputMode.CAMERA.name)
        }.getOrDefault(InputMode.CAMERA)
    }

    fun setInputMode(mode: InputMode) {
        prefs.edit().putString("input_mode", mode.name).apply()
    }
}