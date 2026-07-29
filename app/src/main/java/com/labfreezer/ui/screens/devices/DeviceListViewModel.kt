package com.labfreezer.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.DeviceTypeEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.repository.DeviceTypeRepository
import com.labfreezer.data.repository.RecentlyViewedRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.TreeTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class DeviceListViewModel @Inject constructor(
    private val repository: StorageDeviceRepository,
    private val deviceTypeRepository: DeviceTypeRepository,
    private val recentBoxRepo: RecentlyViewedRepository,
    private val boxRepository: StorageBoxRepository,
    private val treeTransformer: TreeTransformer
) : ViewModel() {

    private val _recentBoxes = MutableStateFlow(recentBoxRepo.getRecentBoxes())
    val recentBoxes: StateFlow<List<com.labfreezer.data.repository.RecentBox>> = _recentBoxes

    fun refreshRecentBoxes() {
        _recentBoxes.value = recentBoxRepo.getRecentBoxes()
    }

    private val _isSelectingRecent = MutableStateFlow(false)
    val isSelectingRecent: StateFlow<Boolean> = _isSelectingRecent

    private val _selectedRecentIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedRecentIds: StateFlow<Set<Long>> = _selectedRecentIds

    fun toggleRecentSelection(id: Long) {
        val current = _selectedRecentIds.value.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        _selectedRecentIds.value = current
        if (current.isEmpty()) _isSelectingRecent.value = false
    }

    fun startRecentSelecting(id: Long) {
        _isSelectingRecent.value = true
        _selectedRecentIds.value = setOf(id)
    }

    fun exitRecentSelecting() {
        _selectedRecentIds.value = emptySet()
        _isSelectingRecent.value = false
    }

    fun selectAllRecent() {
        val allIds = _recentBoxes.value.map { it.id }.toSet()
        if (_selectedRecentIds.value == allIds) {
            _selectedRecentIds.value = emptySet()
            _isSelectingRecent.value = false
        } else {
            _selectedRecentIds.value = allIds
        }
    }

    fun deleteSelectedRecent() {
        viewModelScope.launch {
            _selectedRecentIds.value.forEach { id -> recentBoxRepo.deleteById(id) }
            exitRecentSelecting()
            refreshRecentBoxes()
        }
    }

    val devices: StateFlow<List<StorageDeviceEntity>> = repository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceTypeNames: StateFlow<List<String>> = deviceTypeRepository.getAllFlow()
        .map { types -> types.map { it.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _editingDevice = MutableStateFlow<StorageDeviceEntity?>(null)
    val editingDevice: StateFlow<StorageDeviceEntity?> = _editingDevice

    private val _deletingDevice = MutableStateFlow<StorageDeviceEntity?>(null)
    val deletingDevice: StateFlow<StorageDeviceEntity?> = _deletingDevice

    private val _showDeleteConfirm = MutableStateFlow(false)
    val showDeleteConfirm: StateFlow<Boolean> = _showDeleteConfirm

    /** 挂在 hidden device 下的独立盒子 */
    private val _standaloneBoxes = MutableStateFlow<List<StorageBoxEntity>>(emptyList())
    val standaloneBoxes: StateFlow<List<StorageBoxEntity>> = _standaloneBoxes

    /** 独立盒子多选 */
    private val _isSelectingBoxes = MutableStateFlow(false)
    val isSelectingBoxes: StateFlow<Boolean> = _isSelectingBoxes

    private val _selectedBoxIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedBoxIds: StateFlow<Set<Long>> = _selectedBoxIds

    private val _editingBox = MutableStateFlow<StorageBoxEntity?>(null)
    val editingBox: StateFlow<StorageBoxEntity?> = _editingBox

    private val _deletingBox = MutableStateFlow<StorageBoxEntity?>(null)
    val deletingBox: StateFlow<StorageBoxEntity?> = _deletingBox

    init {
        viewModelScope.launch {
            if (deviceTypeRepository.getAll().isEmpty()) {
                val defaults = listOf("4\u2103\u51b0\u7bb1", "-20\u2103\u51b0\u7bb1", "-80\u2103\u51b0\u7bb1", "\u5e38\u6e29", "\u6db2\u6c2e")
                defaults.forEachIndexed { i, name ->
                    deviceTypeRepository.insert(DeviceTypeEntity(name = name, sortOrder = i))
                }
            }
            refreshStandaloneBoxes()
        }
    }

    fun refreshStandaloneBoxes() {
        viewModelScope.launch {
            val hiddenDevices = repository.getAllHidden()
            val result = mutableListOf<StorageBoxEntity>()
            for (device in hiddenDevices) {
                val boxes = boxRepository.getBoxesByDeviceDirect(device.id)
                result.addAll(boxes)
            }
            _standaloneBoxes.value = result
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
        val allIds = devices.value.map { it.id }.toSet()
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
            _selectedIds.value.forEach { id -> repository.deleteById(id) }
            exitSelection()
        }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun showEditDialog(device: StorageDeviceEntity) { _editingDevice.value = device }
    fun hideEditDialog() { _editingDevice.value = null }

    fun showDeleteConfirm(device: StorageDeviceEntity) { _deletingDevice.value = device }
    fun hideDeleteConfirm() { _deletingDevice.value = null }

    fun addDevice(name: String, type: String, note: String?) {
        viewModelScope.launch {
            repository.insert(StorageDeviceEntity(name = name, type = type, note = note))
            _showAddDialog.value = false
        }
    }

    fun updateDevice(id: Long, name: String, type: String, note: String?) {
        viewModelScope.launch {
            val existing = repository.getById(id) ?: return@launch
            repository.update(existing.copy(name = name, type = type, note = note))
            _editingDevice.value = null
        }
    }

    fun deleteDevice(device: StorageDeviceEntity) {
        viewModelScope.launch {
            repository.delete(device)
            _deletingDevice.value = null
        }
    }

    // ==================== 独立盒子选择/编辑/删除 ====================

    fun toggleBoxSelection(boxId: Long) {
        val current = _selectedBoxIds.value.toMutableSet()
        if (current.contains(boxId)) current.remove(boxId) else current.add(boxId)
        _selectedBoxIds.value = current
        if (current.isEmpty()) _isSelectingBoxes.value = false
    }

    fun startBoxSelection(boxId: Long) {
        _isSelectingBoxes.value = true
        _selectedBoxIds.value = setOf(boxId)
    }

    fun exitBoxSelection() {
        _selectedBoxIds.value = emptySet()
        _isSelectingBoxes.value = false
    }

    fun selectAllBoxes() {
        val allIds = _standaloneBoxes.value.map { it.id }.toSet()
        if (_selectedBoxIds.value == allIds) {
            _selectedBoxIds.value = emptySet()
            _isSelectingBoxes.value = false
        } else {
            _selectedBoxIds.value = allIds
        }
    }

    fun deleteSelectedBoxes() {
        viewModelScope.launch {
            _selectedBoxIds.value.forEach { id -> boxRepository.deleteById(id) }
            exitBoxSelection()
            refreshStandaloneBoxes()
        }
    }

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

    fun updateBox(id: Long, name: String, rows: Int, cols: Int, note: String?) {
        viewModelScope.launch {
            val existing = boxRepository.getById(id) ?: return@launch
            boxRepository.update(existing.copy(name = name, rows = rows, cols = cols, note = note))
            _editingBox.value = null
            refreshStandaloneBoxes()
        }
    }

    fun deleteBox(box: StorageBoxEntity) {
        viewModelScope.launch {
            boxRepository.delete(box)
            _deletingBox.value = null
            refreshStandaloneBoxes()
        }
    }

    fun createBox(name: String, rows: Int, cols: Int, note: String?) {
        // 在设备列表页创建的盒子始终为独立盒子（不依赖父设备）
        viewModelScope.launch {
            treeTransformer.createBoxWithHiddenFill(
                name = name,
                rows = rows,
                cols = cols,
                note = note,
                parentDeviceId = null,
                parentLayerId = null
            )
            refreshStandaloneBoxes()
        }
    }
}
