package com.labfreezer.ui.screens.layers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
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
class LayerDetailViewModel @Inject constructor(
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val deviceRepository: StorageDeviceRepository,
    private val treeTransformer: TreeTransformer
) : ViewModel() {

    private val _layer = MutableStateFlow<StorageLayerEntity?>(null)
    val layer: StateFlow<StorageLayerEntity?> = _layer

    private val _visibleChildren = MutableStateFlow<List<VisibleTreeNode>>(emptyList())
    val visibleChildren: StateFlow<List<VisibleTreeNode>> = _visibleChildren

    private val _allDevices = MutableStateFlow<List<StorageDeviceEntity>>(emptyList())
    val allDevices: StateFlow<List<StorageDeviceEntity>> = _allDevices

    private val _layersByDevice = MutableStateFlow<Map<Long, List<StorageLayerEntity>>>(emptyMap())
    val layersByDevice: StateFlow<Map<Long, List<StorageLayerEntity>>> = _layersByDevice

    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _editingBox = MutableStateFlow<StorageBoxEntity?>(null)
    val editingBox: StateFlow<StorageBoxEntity?> = _editingBox

    private val _deletingBox = MutableStateFlow<StorageBoxEntity?>(null)
    val deletingBox: StateFlow<StorageBoxEntity?> = _deletingBox

    private val _showMoveDialog = MutableStateFlow(false)
    val showMoveDialog: StateFlow<Boolean> = _showMoveDialog

    fun loadLayer(layerId: Long) {
        viewModelScope.launch {
            _layer.value = layerRepository.getById(layerId)
            _allDevices.value = deviceRepository.getAll()
            val allLayers = mutableMapOf<Long, MutableList<StorageLayerEntity>>()
            for (device in _allDevices.value) {
                allLayers.getOrPut(device.id) { mutableListOf() }.addAll(layerRepository.getByDeviceId(device.id))
            }
            _layersByDevice.value = allLayers

            // 加载可见子节点（该 Layer 下的盒子）
            _visibleChildren.value = treeTransformer.getVisibleChildrenOfLayer(layerId)
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
            _selectedIds.value.forEach { id -> boxRepository.deleteById(id) }
            exitSelection()
            _layer.value?.let { loadLayer(it.id) }
        }
    }

    fun showMoveDialog() { _showMoveDialog.value = true }
    fun hideMoveDialog() { _showMoveDialog.value = false }

    fun moveSelected(targetLayerId: Long) {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                val box = boxRepository.getById(id) ?: return@forEach
                boxRepository.update(box.copy(layerId = targetLayerId))
            }
            exitSelection()
            _showMoveDialog.value = false
            _layer.value?.let { loadLayer(it.id) }
        }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun showEditDialog(box: StorageBoxEntity) { _editingBox.value = box }
    fun hideEditDialog() { _editingBox.value = null }

    fun showDeleteConfirm(box: StorageBoxEntity) { _deletingBox.value = box }
    fun hideDeleteConfirm() { _deletingBox.value = null }

    fun addBox(name: String, rows: Int, cols: Int, note: String?) {
        val layerId = _layer.value?.id ?: return
        viewModelScope.launch {
            boxRepository.insert(StorageBoxEntity(layerId = layerId, name = name, rows = rows, cols = cols, note = note))
            _showAddDialog.value = false
            _visibleChildren.value = treeTransformer.getVisibleChildrenOfLayer(layerId)
        }
    }

    fun updateBox(id: Long, name: String, layerId: Long, rows: Int, cols: Int, note: String?) {
        viewModelScope.launch {
            val existing = boxRepository.getById(id) ?: return@launch
            boxRepository.update(existing.copy(name = name, layerId = layerId, rows = rows, cols = cols, note = note))
            _editingBox.value = null
            _layer.value?.let { loadLayer(it.id) }
        }
    }

    fun deleteBox(box: StorageBoxEntity) {
        viewModelScope.launch {
            boxRepository.delete(box)
            _deletingBox.value = null
            _layer.value?.let { loadLayer(it.id) }
        }
    }
}
