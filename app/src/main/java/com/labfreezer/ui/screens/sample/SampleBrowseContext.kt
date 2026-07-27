package com.labfreezer.ui.screens.sample

/**
 * 样本浏览上下文。
 *
 * 决定 SampleEditPage 左右滑动时的浏览范围。
 * 不包含 currentIndex（由 ViewModel 动态计算）和 totalCount（由 sampleIds.size 获取）。
 */
sealed class SampleBrowseContext {

    /** 浏览列表中的所有样本 ID，顺序与进入页面前的列表一致 */
    abstract val sampleIds: List<Long>

    /**
     * 从冰盒详情页进入。
     *
     * @param boxId 当前盒子 ID
     * @param boxName 盒子名称，用于 UI 显示
     * @param sampleIds 盒子内所有样本 ID，按 (row, col) 排序
     */
    data class Box(
        val boxId: Long,
        val boxName: String,
        override val sampleIds: List<Long>
    ) : SampleBrowseContext()

    /**
     * 从智能搜索进入。
     *
     * @param query 用户原始搜索关键词（非标准化后的）
     * @param sampleIds 搜索结果的样本 ID 列表，按搜索排序
     * @param filterContext 筛选条件上下文
     */
    data class Search(
        val query: String,
        override val sampleIds: List<Long>,
        val filterContext: SearchFilterContext
    ) : SampleBrowseContext()

    /**
     * 从标签样本列表进入。
     *
     * @param tagId 标签 ID
     * @param tagName 标签名称，用于 UI 显示
     * @param sampleIds 标签下所有样本 ID，按加载顺序排列
     */
    data class Tag(
        val tagId: Long,
        val tagName: String,
        override val sampleIds: List<Long>
    ) : SampleBrowseContext()
}