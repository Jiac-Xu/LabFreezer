package com.labfreezer.data.db

import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.entity.StorageBoxEntity

/**
 * 隐藏层级标记常量。
 *
 * 用于支持「动态储存层级」特性。
 * 当用户跳过某一层级时，在数据库中以 name = HIDDEN_MARKER 创建占位实体，
 * UI 层通过扩展函数过滤这些占位实体。
 *
 * 规则：
 * - SQL DAO 层：唯一允许直接使用 '__hidden__' 字符串字面量的地方。
 * - Kotlin 业务层：必须通过扩展函数 (isHidden() / isHiddenMarker()) 判断，
 *   禁止直接与 HIDDEN_MARKER 比较。
 */
const val HIDDEN_MARKER = "__hidden__"

// ==================== 实体扩展函数（主要方式） ====================

/** 判断设备是否为隐藏占位节点 */
fun StorageDeviceEntity.isHidden(): Boolean = name == HIDDEN_MARKER

/** 判断层级是否为隐藏占位节点 */
fun StorageLayerEntity.isHidden(): Boolean = name == HIDDEN_MARKER

/**
 * 判断盒子是否位于隐藏层级之下。
 * 用于确定盒子是否为「跳过层级直接挂在设备下」。
 */
fun StorageBoxEntity?.isUnderHiddenLayer(allLayers: List<StorageLayerEntity>): Boolean {
    if (this == null) return false
    return allLayers.find { it.id == layerId }?.isHidden() == true
}

// ==================== 字符串辅助（次要方式） ====================

/**
 * 判断字符串是否为隐藏标记。
 * 用于 SampleWithPath 等从 SQL JOIN 查询返回的字符串字段
 * （deviceName / layerName 是普通 String，无法调用实体扩展函数）。
 */
fun String.isHiddenMarker(): Boolean = this == HIDDEN_MARKER
