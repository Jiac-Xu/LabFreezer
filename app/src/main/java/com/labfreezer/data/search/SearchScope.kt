package com.labfreezer.data.search

/**
 * 搜索范围类型。
 *
 * ALL:    全局搜索，搜索全部样本
 * DEVICE: 设备范围，搜索当前设备内样本
 * LEVEL:  层级范围，搜索当前层级及子节点内样本
 * BOX:    盒子范围，搜索当前盒子内样本
 */
enum class ScopeType {
    ALL,
    DEVICE,
    LEVEL,
    BOX
}

/**
 * 搜索范围参数。
 *
 * @param type 范围类型
 * @param id   设备/层级/盒子的 ID（ALL 时为 null）
 * @param name 名称，用于显示动态 placeholder
 */
data class SearchScope(
    val type: ScopeType = ScopeType.ALL,
    val id: Long? = null,
    val name: String? = null
)