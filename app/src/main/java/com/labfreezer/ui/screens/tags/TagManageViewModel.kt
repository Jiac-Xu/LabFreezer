package com.labfreezer.ui.screens.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.DeviceTypeEntity
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.data.repository.DeviceTypeRepository
import com.labfreezer.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagManageViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val deviceTypeRepository: DeviceTypeRepository
) : ViewModel() {

    val tags: StateFlow<List<TagEntity>> = tagRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deviceTypes: StateFlow<List<DeviceTypeEntity>> = deviceTypeRepository.getAllFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddDialog = MutableStateFlow(false)
    val showAddDialog: StateFlow<Boolean> = _showAddDialog

    private val _editingTag = MutableStateFlow<TagEntity?>(null)
    val editingTag: StateFlow<TagEntity?> = _editingTag

    private val _deletingTag = MutableStateFlow<TagEntity?>(null)
    val deletingTag: StateFlow<TagEntity?> = _deletingTag

    private val _showAddTypeDialog = MutableStateFlow(false)
    val showAddTypeDialog: StateFlow<Boolean> = _showAddTypeDialog

    private val _deletingType = MutableStateFlow<DeviceTypeEntity?>(null)
    val deletingType: StateFlow<DeviceTypeEntity?> = _deletingType

    fun showAddDialog() { _showAddDialog.value = true }
    fun hideAddDialog() { _showAddDialog.value = false }
    fun showEditDialog(tag: TagEntity) { _editingTag.value = tag }
    fun hideEditDialog() { _editingTag.value = null }
    fun showDeleteConfirm(tag: TagEntity) { _deletingTag.value = tag }
    fun hideDeleteConfirm() { _deletingTag.value = null }

    fun addTag(name: String, color: String) {
        viewModelScope.launch {
            tagRepository.insert(TagEntity(name = name, color = color))
            _showAddDialog.value = false
        }
    }

    fun updateTag(id: Long, name: String, color: String) {
        viewModelScope.launch {
            val existing = tagRepository.getById(id) ?: return@launch
            tagRepository.update(existing.copy(name = name, color = color, updatedAt = System.currentTimeMillis()))
            _editingTag.value = null
        }
    }

    fun deleteTag(tag: TagEntity) {
        viewModelScope.launch {
            tagRepository.delete(tag)
            _deletingTag.value = null
        }
    }

    fun showAddTypeDialog() { _showAddTypeDialog.value = true }
    fun hideAddTypeDialog() { _showAddTypeDialog.value = false }
    fun showDeleteTypeConfirm(type: DeviceTypeEntity) { _deletingType.value = type }
    fun hideDeleteTypeConfirm() { _deletingType.value = null }

    fun addDeviceType(name: String) {
        viewModelScope.launch {
            val types = deviceTypeRepository.getAll()
            deviceTypeRepository.insert(DeviceTypeEntity(name = name, sortOrder = types.size))
            _showAddTypeDialog.value = false
        }
    }

    fun deleteDeviceType(type: DeviceTypeEntity) {
        viewModelScope.launch {
            deviceTypeRepository.delete(type)
            _deletingType.value = null
        }
    }
}
