package com.labfreezer.data.model

/**
 * 节点类型枚举，用于 UI 层标识可见树中节点的业务含义。
 *
 * 取代旧有的「通过 Level 编号判断节点类型」的模式。
 * 数据库仍保持固定四级结构，业务层通过 TreeTransformer 转换为 VisibleTree，
 * UI 基于此枚举渲染。
 */
enum class NodeType {
    /** 冷冻设备/冰箱（StorageDeviceEntity，且非 hidden） */
    FREEZER,

    /** 储存层级（StorageLayerEntity，且非 hidden） */
    LEVEL,

    /** 盒子（StorageBoxEntity，永不 hidden） */
    BOX
}
