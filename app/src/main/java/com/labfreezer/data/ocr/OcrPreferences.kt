package com.labfreezer.data.ocr

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("ocr_prefs", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean("ocr_enabled", true)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("ocr_enabled", enabled).apply()
    }
}
