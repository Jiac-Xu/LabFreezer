package com.labfreezer.ui.screens.settings

import android.content.Context
import com.labfreezer.R

data class StartPageSetting(
    val label: String,
    val route: String,
    val id: Long = -1L
)

object StartPagePreference {
    private const val PREFS = "start_page_prefs"
    private const val KEY_LABEL = "start_label"
    private const val KEY_ROUTE = "start_route"
    private const val KEY_ID = "start_id"

    fun get(context: Context): StartPageSetting {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val defaultLabel = context.getString(R.string.start_page_default_label)
        return StartPageSetting(
            label = prefs.getString(KEY_LABEL, defaultLabel) ?: defaultLabel,
            route = prefs.getString(KEY_ROUTE, "device_list") ?: "device_list",
            id = prefs.getLong(KEY_ID, -1L)
        )
    }

    fun set(context: Context, setting: StartPageSetting) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LABEL, setting.label)
            .putString(KEY_ROUTE, setting.route)
            .putLong(KEY_ID, setting.id)
            .apply()
    }
}
