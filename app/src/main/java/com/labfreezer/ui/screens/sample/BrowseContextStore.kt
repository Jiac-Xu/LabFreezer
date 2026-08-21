package com.labfreezer.ui.screens.sample

import android.util.LruCache

/**
 * 浏览上下文存储容器。
 *
 * 由于 Navigation Compose 不支持传递复杂对象，使用此单例在页面间传递 SampleBrowseContext。
 * 通过短 key 在导航路由中引用。
 *
 * 采用 LruCache 限制最多保留 20 个上下文，防止无界增长引发内存泄漏。
 */
object BrowseContextStore {

    private const val MAX_ENTRIES = 20

    private val cache = LruCache<String, SampleBrowseContext>(MAX_ENTRIES)

    private var counter = 0L

    /**
     * 存入上下文，返回唯一 key。
     * 如果 key 已存在则覆盖。
     */
    @Synchronized
    fun put(context: SampleBrowseContext, key: String? = null): String {
        val k = key ?: "browse_${++counter}"
        cache.put(k, context)
        return k
    }

    /** 根据 key 获取上下文 */
    @Synchronized
    fun get(key: String): SampleBrowseContext? = cache.get(key)

    /** 移除指定 key 的上下文 */
    @Synchronized
    fun remove(key: String) {
        cache.remove(key)
    }

    /** 清除所有上下文 */
    @Synchronized
    fun clear() {
        cache.evictAll()
    }
}