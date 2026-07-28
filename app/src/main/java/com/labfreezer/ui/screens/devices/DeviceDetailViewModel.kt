package com.labfreezer.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.model.NodeType
import com.labfreezer.data.model.VisibleTreeNode
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TreeTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val treeTransformer: TreeTransformer
) : ViewModel() {

    private val _device = MutableStateFlow<StorageDeviceEntity?>(null)
    val device: StateFlow<StorageDeviceEntity?> = _device

    private val _visibleChildren = MutableStateFlow<List<VisibleTreeNode>>(emptyList())
    val visibleChildren: StateFlow<List<VisibleTreeNode>> = _visibleChildren

    /**
     * 当前节点是否允许创建层级。
     * 对于非 hidden 的设备（FREEZER）可以创建层级。
     */
    private val _canCreateLevel = MutableStateFlow(false)
    val canCreateLevel: StateFlow<Boolean> = _canCreateLevel

    private val _allDevices = MutableStateFlow<List<StorageDeviceEntity>>(emptyList())
    val allDevices: StateFlow<List<StorageDeviceEntity>> = _allDevices

    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    /** true = 创建层级对话框, false = 创建盒子对话框 */
    private val _addDialogMode = MutableStateFlow(AddDialogMode.NONE)
    val addDialogMode: StateFlow<AddDialogMode> = _addDialogMode

    private val _editingLayer = MutableStateFlow<StorageLayerEntity?>(null)
    val editingLayer: StateFlow<StorageLayerEntity?> = _editingLayer

    private val _editingBox = MutableStateFlow<StorageBoxEntity?>(null)
    val editingBox: StateFlow<StorageBoxEntity?> = _editingBox

    private val _deletingLayer = MutableStateFlow<StorageLayerEntity?>(null)
    val deletingLayer: StateFlow<StorageLayerEntity?> = _deletingLayer

    private val _deletingBox = MutableStateFlow<StorageBoxEntity?>(null)
    val deletingBox: StateFlow<StorageBoxEntity?> = _deletingBox

    private val _showMoveDialog = MutableStateFlow(false)
    val showMoveDialog: StateFlow<Boolean> = _showMoveDialog

    fun loadDevice(deviceId: Long) {
        viewModelScope.launch {
            val dev = deviceRepository.getById(deviceId)
            _device.value = dev
            _allDevices.value = deviceRepository.getAll()
            _canCreateLevel.value = dev != null

            // 加载可见子节点（混合类型）
            _visibleChildren.value = treeTransformer.getVisibleChildren(deviceId)
        }
    }

    fun toggleSelection(id: Long) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedIds.value = current
        if (current.isEmpty()) _isSelecting.value = false
    }

    fun startSelection(id: Long) {
        _isSelecting.value = true
        _selectedIds.value = setOf(id)
    }

    fun selectAll() {
        val allIds = _visibleChildren.value.map { it.id }.toSet()
        if (_selectedIds.value == allIds) {
            _selectedIds.value = emptySet()
            _isSelecting.value = false
        } else {
            _selectedIds.value = allIds
        }
    }

    fun exitSelection() {
        _selectedIds.value = emptySet()
        _isSelecting.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                val node = _visibleChildren.value.find { it.id == id } ?: return@forEach
                when (node.type) {
                    NodeType.LEVEL -> layerRepository.deleteById(id)
                    NodeType.BOX -> boxRepository.deleteById(id)
                    else -> {}
                }
            }
            exitSelection()
            _device.value?.let { loadDevice(it.id) }
        }
    }

    fun showMoveDialog() { _showMoveDialog.value = true }
    fun hideMoveDialog() { _showMoveDialog.value = false }

    fun moveSelected(targetDeviceId: Long) {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                val node = _visibleChildren.value.find { it.id == id } ?: return@forEach
                when (node.type) {
                    NodeType.LEVEL -> {
                        val layer = layerRepository.getById(id) ?: return@forEach
                        layerRepository.update(layer.copy(deviceId = targetDeviceId))
                    }
                    else -> {}
                }
            }
            exitSelection()
            _showMoveDialog.value = false
            _device.value?.let { loadDevice(it.id) }
        }
    }

    // ==================== Speed Dial 对话框控制 ====================

    /** 显示创建盒子对话框 */
    fun showCreateBoxDialog() {
        _addDialogMode.value = AddDialogMode.BOX
        _showAddDialog.value = true
    }

    /** 显示创建层级对话框 */
    fun showCreateLevelDialog() {
        _addDialogMode.value = AddDialogMode.LEVEL
        _showAddDialog.value = true
    }

    fun hideAddDialog() {
        _showAddDialog.value = false
        _addDialogMode.value = AddDialogMode.NONE
    }

    fun showEditDialog(layer: StorageLayerEntity) { _editingLayer.value = layer }
    fun hideEditDialog() { _editingLayer.value = null }

    fun showDeleteConfirm(layer: StorageLayerEntity) { _deletingLayer.value = layer }
    fun hideDeleteConfirm() { _deletingLayer.value = null }

    fun showEditBoxDialog(boxId: Long) {
        viewModelScope.launch {
            val box = boxRepository.getById(boxId) ?: return@launch
            _editingBox.value = box
        }
    }
    fun hideEditBoxDialog() { _editingBox.value = null }

    fun showDeleteBoxConfirm(boxId: Long) {
        viewModelScope.launch {
            val box = boxRepository.getById(boxId) ?: return@launch
            _deletingBox.value = box
        }
    }
    fun hideDeleteBoxConfirm() { _deletingBox.value = null }

    fun updateBox(id: Long, name: String, layerId: Long, rows: Int, cols: Int, note: String?) {
        viewModelScope.launch {
            val existing = boxRepository.getById(id) ?: return@launch
            boxRepository.update(existing.copy(name = name, layerId = layerId, rows = rows, cols = cols, note = note))
            _editingBox.value = null
            _device.value?.let { loadDevice(it.id) }
        }
    }

    fun deleteBox(box: StorageBoxEntity) {
        viewModelScope.launch {
            boxRepository.delete(box)
            _deletingBox.value = null
            _device.value?.let { loadDevice(it.id) }
        }
    }

    /**
     * 创建盒子（自动填充 hidden 层级）。
     */
    fun addBox(name: String, rows: Int, cols: Int, note: String?) {
        val deviceId = _device.value?.id ?: return
        viewModelScope.launch {
            treeTransformer.createBoxWithHiddenFill(
                name = name,
                rows = rows,
                cols = cols,
                note = note,
                parentDeviceId = deviceId,
                parentLayerId = null
            )
            _showAddDialog.value = false
            _addDialogMode.value = AddDialogMode.NONE
            _visibleChildren.value = treeTransformer.getVisibleChildren(deviceId)
        }
    }

    /**
     * 创建层级。
     */
    fun addLayer(name: String, note: String?) {
        val deviceId = _device.value?.id ?: return
        viewModelScope.launch {
            treeTransformer.createLevel(deviceId = deviceId, name = name, note = note)
            _showAddDialog.value = false
            _addDialogMode.value = AddDialogMode.NONE
            _visibleChildren.value = treeTransformer.getVisibleChildren(deviceId)
        }
    }

    fun updateLayer(id: Long, name: String, deviceId: Long, note: String?) {
        viewModelScope.launch {
            val existing = layerRepository.getById(id) ?: return@launch
            layerRepository.update(existing.copy(name = name, deviceId = deviceId, note = note))
            _editingLayer.value = null
            loadDevice(deviceId)
        }
    }

    fun deleteLayer(layer: StorageLayerEntity) {
        viewModelScope.launch {
            layerRepository.delete(layer)
            _deletingLayer.value = null
            _device.value?.let { loadDevice(it.id) }
        }
    }
}

/** 添加对话框模式 */
enum class AddDialogMode {
    NONE,       // 对话框关闭
    BOX,        // 创建盒子
    LEVEL       // 创建层级
}
