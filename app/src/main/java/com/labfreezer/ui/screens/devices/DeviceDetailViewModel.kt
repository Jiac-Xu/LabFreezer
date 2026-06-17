package com.labfreezer.ui.screens.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeviceDetailViewModel @Inject constructor(
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository
) : ViewModel() {

    private val _device = MutableStateFlow<StorageDeviceEntity?>(null)
    val device: StateFlow<StorageDeviceEntity?> = _device

    private val _layers = MutableStateFlow<List<StorageLayerEntity>>(emptyList())
    val layers: StateFlow<List<StorageLayerEntity>> = _layers

    private val _allDevices = MutableStateFlow<List<StorageDeviceEntity>>(emptyList())
    val allDevices: StateFlow<List<StorageDeviceEntity>> = _allDevices

    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _editingLayer = MutableStateFlow<StorageLayerEntity?>(null)
    val editingLayer: StateFlow<StorageLayerEntity?> = _editingLayer

    private val _deletingLayer = MutableStateFlow<StorageLayerEntity?>(null)
    val deletingLayer: StateFlow<StorageLayerEntity?> = _deletingLayer

    private val _showMoveDialog = MutableStateFlow(false)
    val showMoveDialog: StateFlow<Boolean> = _showMoveDialog

    fun loadDevice(deviceId: Long) {
        viewModelScope.launch {
            _device.value = deviceRepository.getById(deviceId)
            _allDevices.value = deviceRepository.getAll()
        }
        layerRepository.getByDeviceIdFlow(deviceId).stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        ).also { flow ->
            viewModelScope.launch {
                flow.collect { _layers.value = it }
            }
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
        val allIds = _layers.value.map { it.id }.toSet()
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
            _selectedIds.value.forEach { id -> layerRepository.deleteById(id) }
            exitSelection()
        }
    }

    fun showMoveDialog() { _showMoveDialog.value = true }
    fun hideMoveDialog() { _showMoveDialog.value = false }

    fun moveSelected(targetDeviceId: Long) {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                val layer = layerRepository.getById(id) ?: return@forEach
                layerRepository.update(layer.copy(deviceId = targetDeviceId))
            }
            exitSelection()
            _showMoveDialog.value = false
        }
    }

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }

    fun showEditDialog(layer: StorageLayerEntity) { _editingLayer.value = layer }
    fun hideEditDialog() { _editingLayer.value = null }

    fun showDeleteConfirm(layer: StorageLayerEntity) { _deletingLayer.value = layer }
    fun hideDeleteConfirm() { _deletingLayer.value = null }

    fun addLayer(name: String, note: String?) {
        val deviceId = _device.value?.id ?: return
        viewModelScope.launch {
            layerRepository.insert(StorageLayerEntity(deviceId = deviceId, name = name, note = note))
            _showAddDialog.value = false
        }
    }

    fun updateLayer(id: Long, name: String, deviceId: Long, note: String?) {
        viewModelScope.launch {
            val existing = layerRepository.getById(id) ?: return@launch
            layerRepository.update(existing.copy(name = name, deviceId = deviceId, note = note))
            _editingLayer.value = null
        }
    }

    fun deleteLayer(layer: StorageLayerEntity) {
        viewModelScope.launch {
            layerRepository.delete(layer)
            _deletingLayer.value = null
        }
    }
}
