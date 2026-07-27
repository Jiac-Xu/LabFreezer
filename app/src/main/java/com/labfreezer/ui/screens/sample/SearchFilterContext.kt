package com.labfreezer.ui.screens.sample

/**
 * 搜索筛选条件上下文。
 *
 * 保存结构化筛选条件，用于 UI 展示摘要和国际化。
 * 不保存原始搜索关键词（由 SampleBrowseContext.Search.query 保存）。
 */
data class SearchFilterContext(
    /** 筛选条件列表，各条件之间逻辑关系由 logic 决定 */
    val conditions: List<SearchCondition> = emptyList(),
    /** 条件之间的逻辑关系（AND / OR） */
    val logic: FilterLogic = FilterLogic.AND
)

/**
 * 单个筛选条件。
 *
 * @param type 筛选类型
 * @param values 筛选值列表（显示标签，如盒子名、年月、标签名），组内 OR
 */
data class SearchCondition(
    val type: FilterType,
    val values: List<String>
)

/** 筛选类型 */
enum class FilterType {
    BOX,
    DATE,
    TAG
}

/** 条件间逻辑关系 */
enum class FilterLogic {
    AND,
    OR
}