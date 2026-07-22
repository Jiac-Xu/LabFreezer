package com.labfreezer.data.search

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SearchHistoryItem(
    val keyword: String,
    val timestamp: Long
)

/**
 * 搜索历史管理，使用 SharedPreferences 存储搜索关键词历史。
 *
 * - 最多保存 MAX_ITEMS 条记录
 * - 相同关键词重复搜索时，移动到最前，不重复记录
 * - 空关键词不保存
 * - 按时间倒序排列
 */
@Singleton
class SearchHistoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /**
     * 获取搜索历史记录列表，按时间倒序排列。
     */
    fun getHistory(limit: Int = MAX_ITEMS): List<SearchHistoryItem> {
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val arr = JSONArray(json)
        val result = mutableListOf<SearchHistoryItem>()
        for (i in 0 until minOf(arr.length(), limit)) {
            val obj = arr.getJSONObject(i)
            result.add(
                SearchHistoryItem(
                    keyword = obj.getString("keyword"),
                    timestamp = obj.getLong("timestamp")
                )
            )
        }
        return result
    }

    /**
     * 添加搜索关键词到历史记录。
     * - 如果已存在相同关键词，将其移动到最前
     * - 空关键词不保存
     */
    fun addKeyword(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isEmpty()) return

        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val arr = JSONArray(json)
        val newArr = JSONArray()

        // 将当前关键词放在最前
        newArr.put(JSONObject().apply {
            put("keyword", trimmed)
            put("timestamp", System.currentTimeMillis())
        })

        var count = 0
        for (i in 0 until arr.length()) {
            if (count >= MAX_ITEMS - 1) break
            val obj = arr.getJSONObject(i)
            // 跳过已存在的相同关键词
            if (obj.getString("keyword") != trimmed) {
                newArr.put(obj)
                count++
            }
        }

        prefs.edit().putString(KEY_HISTORY, newArr.toString()).apply()
    }

    /**
     * 清空所有搜索历史记录。
     */
    fun clearAll() {
        prefs.edit().putString(KEY_HISTORY, "[]").apply()
    }

    companion object {
        private const val PREF_NAME = "search_history"
        private const val KEY_HISTORY = "keywords"
        const val MAX_ITEMS = 20
    }
}