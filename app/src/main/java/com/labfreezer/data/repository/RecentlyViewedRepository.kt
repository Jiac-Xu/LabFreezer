package com.labfreezer.data.repository

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class RecentBox(val id: Long, val name: String, val deviceName: String? = null, val layerName: String? = null)

@Singleton
class RecentlyViewedRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getRecentBoxes(limit: Int = 10): List<RecentBox> {
        val json = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        val arr = JSONArray(json)
        val result = mutableListOf<RecentBox>()
        for (i in 0 until minOf(arr.length(), limit)) {
            val obj = arr.getJSONObject(i)
            result.add(RecentBox(obj.getLong("id"), obj.getString("name"),
                deviceName = obj.optString("deviceName", null),
                layerName = obj.optString("layerName", null)))
        }
        return result
    }

    fun deleteById(id: Long) {
        val json = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        val arr = JSONArray(json)
        val newArr = JSONArray()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            if (obj.getLong("id") != id) {
                newArr.put(obj)
            }
        }
        prefs.edit().putString(KEY_RECENT, newArr.toString()).apply()
    }

    fun addBox(id: Long, name: String, deviceName: String? = null, layerName: String? = null) {
        val json = prefs.getString(KEY_RECENT, "[]") ?: "[]"
        val arr = JSONArray(json)
        val newArr = JSONArray()
        newArr.put(JSONObject().apply {
            put("id", id)
            put("name", name)
            deviceName?.let { put("deviceName", it) }
            layerName?.let { put("layerName", it) }
        })
        var count = 0
        for (i in 0 until arr.length()) {
            if (count >= MAX_ITEMS - 1) break
            val obj = arr.getJSONObject(i)
            if (obj.getLong("id") != id) {
                newArr.put(obj)
                count++
            }
        }
        prefs.edit().putString(KEY_RECENT, newArr.toString()).apply()
    }

    companion object {
        private const val PREF_NAME = "recently_viewed"
        private const val KEY_RECENT = "boxes"
        private const val MAX_ITEMS = 20
    }
}
