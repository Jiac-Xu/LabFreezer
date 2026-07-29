package com.labfreezer.data.db

import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.entity.StorageBoxEntity

/**
 * 隐藏层级标记常量。
 *
 * 用于支持「动态储存层级」特性。
 * 当用户跳过某一层级时，在数据库中以 name = HIDDEN_MARKER 创建占位实体，
 * UI 层通过 TreeTransformer 过滤这些占位实体。
 *
 * 规则：
 * - SQL DAO 层：唯一允许直接使用 '__hidden__' 字符串字面量的地方。
 * - 业务层：实体用 isHidden()，字符串用 isHiddenMarker()。
 * - UI 层：统一走 TreeTransformer，不直接使用这些扩展函数。
 */
const val HIDDEN_MARKER = "__hidden__"

// ==================== 实体扩展函数（仅供业务层使用） ====================

/** 判断设备是否为隐藏占位节点 */
fun StorageDeviceEntity.isHidden(): Boolean = name == HIDDEN_MARKER

/** 判断层级是否为隐藏占位节点 */
fun StorageLayerEntity.isHidden(): Boolean = name == HIDDEN_MARKER

/**
 * 判断盒子是否位于隐藏层级之下。
 */
fun StorageBoxEntity?.isUnderHiddenLayer(allLayers: List<StorageLayerEntity>): Boolean {
    if (this == null) return false
    return allLayers.find { it.id == layerId }?.isHidden() == true
}

// ==================== 字符串辅助（仅供业务层/导出使用） ====================

/** 判断字符串是否为隐藏标记 */
fun String.isHiddenMarker(): Boolean = this == HIDDEN_MARKER
