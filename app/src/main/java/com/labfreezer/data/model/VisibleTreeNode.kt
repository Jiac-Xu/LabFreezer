package com.labfreezer.data.model

/**
 * 可见树节点 — UI 层使用的统一节点表示。
 *
 * 数据库仍保持固定四级结构，业务层通过 TreeTransformer 将数据库实体
 * 转换为可见树节点，跳过所有 hidden 占位实体。
 */
data class VisibleTreeNode(
    /** 实体 ID（StorageDeviceEntity.id / StorageLayerEntity.id / StorageBoxEntity.id） */
    val id: Long,

    /** 用户可见的名称（不会是 __hidden__） */
    val name: String,

    /** 节点类型 */
    val type: NodeType,

    /** 逻辑父节点 ID（可能跨过一层 hidden 中间层） */
    val parentId: Long? = null,

    /** 样本数量（叶子节点有效） */
    val sampleCount: Int = 0,

    /** 备注（盒子/层级可选） */
    val note: String? = null
)

/**
 * 从根到指定节点的可见路径。
 * 所有 segment 均已过滤 hidden。
 */
data class VisiblePath(
    val segments: List<VisibleTreeNode>
) {
    /** 展示路径字符串，例如 "冰箱A > 层1 > 盒子B" */
    val displayPath: String
        get() = segments.joinToString(" > ") { it.name }

    companion object {
        val EMPTY = VisiblePath(emptyList())
    }
}
