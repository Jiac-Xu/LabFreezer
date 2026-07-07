package com.labfreezer.ui.screens.settings
import com.labfreezer.R

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.ui.graphics.vector.ImageVector

enum class BottomTab(
    val id: String,
    val icon: ImageVector
) {
    DEVICE_LIST("device", Icons.Default.Home),
    TAG_MANAGE("tag", Icons.Default.Tag),
    SEARCH("search", Icons.Filled.Search),
    SETTINGS("settings", Icons.Default.Settings);

    fun getLabel(context: Context): String = when (this) {
        DEVICE_LIST -> context.getString(R.string.tab_home)
        TAG_MANAGE -> context.getString(R.string.tab_tags)
        SEARCH -> context.getString(R.string.tab_search)
        SETTINGS -> context.getString(R.string.tab_settings)
    }

    companion object {
        fun fromId(id: String): BottomTab? = entries.find { it.id == id }
    }
}

object BottomTabPreference {
    private const val PREFS = "bottom_tab_prefs"
    private const val KEY_ORDER = "tab_order"
    private const val DEFAULT_ORDER = "device,tag,settings"

    fun getDefault(): List<BottomTab> =
        DEFAULT_ORDER.split(",").mapNotNull { BottomTab.fromId(it.trim()) }

    fun get(context: Context): List<BottomTab> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val order = prefs.getString(KEY_ORDER, DEFAULT_ORDER) ?: DEFAULT_ORDER
        return order.split(",").mapNotNull { BottomTab.fromId(it.trim()) }
    }

    fun set(context: Context, tabs: List<BottomTab>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_ORDER, tabs.joinToString(",") { it.id })
            .apply()
    }
}
