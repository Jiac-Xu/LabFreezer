package com.labfreezer.ui.screens.sample

/**
 * 浏览上下文存储容器。
 *
 * 由于 Navigation Compose 不支持传递复杂对象，使用此单例在页面间传递 SampleBrowseContext。
 * 通过短 key 在导航路由中引用。
 *
 * 生命周期规则：
 * - 进入 SampleEdit 时存入，key 附加在路由参数中
 * - 返回列表页时，由调用方在弹出栈后清理（可选）
 * - Activity 重建后丢失，回退到按盒子浏览的旧逻辑
 */
object BrowseContextStore {

    private val store = mutableMapOf<String, SampleBrowseContext>()

    private var counter = 0L

    /**
     * 存入上下文，返回唯一 key。
     * 如果 key 已存在则覆盖。
     */
    @Synchronized
    fun put(context: SampleBrowseContext, key: String? = null): String {
        val k = key ?: "browse_${++counter}"
        store[k] = context
        return k
    }

    /** 根据 key 获取上下文 */
    @Synchronized
    fun get(key: String): SampleBrowseContext? = store[key]

    /** 移除指定 key 的上下文 */
    @Synchronized
    fun remove(key: String) {
        store.remove(key)
    }

    /** 清除所有上下文 */
    @Synchronized
    fun clear() {
        store.clear()
    }
}