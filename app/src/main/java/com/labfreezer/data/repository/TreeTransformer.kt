package com.labfreezer.data.repository

import com.labfreezer.data.db.HIDDEN_MARKER
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.isHidden
import com.labfreezer.data.model.NodeType
import com.labfreezer.data.model.VisiblePath
import com.labfreezer.data.model.VisibleTreeNode
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TreeTransformer — 动态储存层级核心转换层。
 *
 * 职责：在保持 DB 固定四级结构不变的前提下，提供「可见树」的查询和操作。
 * - 查询时：将 DB 实体转换为 VisibleTreeNode，跳过 __hidden__ 占位实体
 * - 创建时：根据用户当前所在位置自动填充必要的 hidden 层级
 */
@Singleton
class TreeTransformer @Inject constructor(
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository
) {

    // ==================== 可见父节点查询 ====================

    /**
     * 获取一个设备的直接可见子节点列表（混合类型：LEVEL + BOX）。
     *
     * 例如数据库结构为：
     *   冰箱A → __hidden__ → 盒子B
     *   冰箱A → 层1 → 盒子C
     *
     * 返回：
     *   [LEVEL(层1), BOX(盒子B)]
     */
    suspend fun getVisibleChildren(deviceId: Long): List<VisibleTreeNode> {
        val layers = layerRepository.getByDeviceId(deviceId)     // 已过滤 hidden
        val directBoxes = boxRepository.getBoxesByDeviceDirect(deviceId)  // 通过 hidden layer 的盒子

        return buildList {
            // 1. 非 hidden 的 Layer
            layers.forEach { layer ->
                add(
                    VisibleTreeNode(
                        id = layer.id,
                        name = layer.name,
                        type = NodeType.LEVEL,
                        parentId = deviceId
                    )
                )
            }
            // 2. 直接挂在设备下的 Box（通过 hidden layer）
            directBoxes.forEach { box ->
                add(
                    VisibleTreeNode(
                        id = box.id,
                        name = box.name,
                        type = NodeType.BOX,
                        parentId = deviceId
                    )
                )
            }
        }
    }

    /**
     * 获取一个非 hidden Layer 的直接可见子节点列表。
     * 目前 Layer 下只有 Box。
     */
    suspend fun getVisibleChildrenOfLayer(layerId: Long): List<VisibleTreeNode> {
        val layer = layerRepository.getById(layerId) ?: return emptyList()
        if (layer.isHidden()) return emptyList()   // hidden layer 不应该有可见子节点

        val boxes = boxRepository.getByLayerId(layerId)

        return boxes.map { box ->
            VisibleTreeNode(
                id = box.id,
                name = box.name,
                type = NodeType.BOX,
                parentId = layerId
            )
        }
    }

    // ==================== 可见路径构建 ====================

    /**
     * 构建从根（设备）到指定盒子的可见路径。
     * 跳过所有 hidden 中间层。
     *
     * 例如数据库路径为：冰箱A → __hidden__ → 盒子B
     * 返回的 segments：[FREEZER(冰箱A), BOX(盒子B)]
     */
    suspend fun buildVisiblePath(boxId: Long): VisiblePath {
        val box = boxRepository.getById(boxId) ?: return VisiblePath.EMPTY
        val segments = mutableListOf<VisibleTreeNode>()

        // 1. Box
        segments.add(
            VisibleTreeNode(id = box.id, name = box.name, type = NodeType.BOX)
        )

        // 2. 向上遍历：Layer → Device（跳过 hidden）
        val layer = layerRepository.getById(box.layerId)
        if (layer != null && !layer.isHidden()) {
            segments.add(0,
                VisibleTreeNode(id = layer.id, name = layer.name, type = NodeType.LEVEL)
            )
        }

        // 3. Device（如果非 hidden）
        val device = layer?.let { deviceRepository.getById(it.deviceId) }
        if (device != null && !device.isHidden()) {
            segments.add(0,
                VisibleTreeNode(id = device.id, name = device.name, type = NodeType.FREEZER)
            )
        }

        return VisiblePath(segments)
    }

    /**
     * 构建从根到指定设备的可见路径。
     */
    suspend fun buildVisiblePath(deviceId: Long, deviceName: String): VisiblePath {
        // 设备本身就是根
        return VisiblePath(
            listOf(
                VisibleTreeNode(id = deviceId, name = deviceName, type = NodeType.FREEZER)
            )
        )
    }

    // ==================== 可见设备列表 ====================

    /**
     * 获取所有非 hidden 的设备（DAO 已过滤）。
     */
    suspend fun getAllVisibleDevices(): List<StorageDeviceEntity> {
        return deviceRepository.getAll()
    }

    // ==================== 创建盒子（自动填充 hidden） ====================

    /**
     * 在当前用户所在位置创建盒子。
     * 根据提供的父节点信息，自动填充必要的 hidden 层级。
     *
     * @param name 盒子名称
     * @param rows 行数
     * @param cols 列数
     * @param note 备注（可选）
     * @param parentDeviceId 当前所在设备 ID（可选）
     * @param parentLayerId 当前所在层级 ID（可选）
     * @return 创建的盒子实体
     */
    suspend fun createBoxWithHiddenFill(
        name: String,
        rows: Int,
        cols: Int,
        note: String?,
        parentDeviceId: Long?,
        parentLayerId: Long?
    ): StorageBoxEntity {
        // 场景 A：有明确父 Layer → 正常创建盒子
        if (parentLayerId != null) {
            val existingLayer = layerRepository.getById(parentLayerId)
            if (existingLayer != null && !existingLayer.isHidden()) {
                val boxId = boxRepository.insert(
                    StorageBoxEntity(layerId = parentLayerId, name = name, rows = rows, cols = cols, note = note)
                )
                return boxRepository.getById(boxId)!!
            }
        }

        // 场景 B：有父 Device，但无父 Layer → 创建 hidden Layer → 创建盒子
        if (parentDeviceId != null) {
            val hiddenLayerId = layerRepository.insert(
                StorageLayerEntity(deviceId = parentDeviceId, name = HIDDEN_MARKER)
            )
            val boxId = boxRepository.insert(
                StorageBoxEntity(layerId = hiddenLayerId, name = name, rows = rows, cols = cols, note = note)
            )
            return boxRepository.getById(boxId)!!
        }

        // 场景 C：无父 Device → 创建 hidden Device → 创建 hidden Layer → 创建盒子（独立盒子）
        val hiddenDeviceId = deviceRepository.insert(
            StorageDeviceEntity(name = HIDDEN_MARKER)
        )
        val hiddenLayerId = layerRepository.insert(
            StorageLayerEntity(deviceId = hiddenDeviceId, name = HIDDEN_MARKER)
        )
        val boxId = boxRepository.insert(
            StorageBoxEntity(layerId = hiddenLayerId, name = name, rows = rows, cols = cols, note = note)
        )
        return boxRepository.getById(boxId)!!
    }

    // ==================== 创建层级 ====================

    /**
     * 在指定设备下创建一个普通（非 hidden）层级。
     *
     * @param deviceId 父设备 ID
     * @param name 层级名称
     * @param note 备注（可选）
     * @return 创建的层级实体
     */
    suspend fun createLevel(deviceId: Long, name: String, note: String? = null): StorageLayerEntity {
        val layerId = layerRepository.insert(
            StorageLayerEntity(deviceId = deviceId, name = name, note = note)
        )
        return layerRepository.getById(layerId)!!
    }

    // ==================== 移动目标创建（自动填充 hidden） ====================

    /**
     * 将盒子移动到指定父节点下。
     * 如果目标父节点是 Device，自动创建 hidden Layer 作为中间层。
     *
     * @param boxId 要移动的盒子 ID
     * @param targetDeviceId 目标设备 ID（可选）
     * @param targetLayerId 目标层级 ID（可选）
     */
    suspend fun moveBoxToContainer(
        boxId: Long,
        targetDeviceId: Long?,
        targetLayerId: Long?
    ) {
        val box = boxRepository.getById(boxId) ?: return

        val newLayerId = if (targetLayerId != null) {
            // 直接挂到指定层级下
            targetLayerId
        } else if (targetDeviceId != null) {
            // 挂到设备下 → 创建 hidden layer
            layerRepository.insert(
                StorageLayerEntity(deviceId = targetDeviceId, name = HIDDEN_MARKER)
            )
        } else {
            return
        }

        boxRepository.update(box.copy(layerId = newLayerId))
    }

    // ==================== 批量查询 ====================

    /**
     * 一次加载多段路径（用于搜索结果批量显示）。
     */
    suspend fun buildVisiblePaths(boxIds: List<Long>): Map<Long, VisiblePath> = coroutineScope {
        val deferreds = boxIds.map { boxId ->
            async { boxId to buildVisiblePath(boxId) }
        }
        deferreds.map { it.await() }.toMap()
    }
}
