package com.labfreezer.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.file.PhotoManager
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.TreeTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PhotoGroup(
    val deviceName: String,
    val layerName: String,
    val boxName: String,
    val samples: List<SampleWithPath>
)

@HiltViewModel
class ImageCleanupViewModel @Inject constructor(
    private val sampleRepo: SamplePositionRepository,
    private val photoManager: PhotoManager,
    private val treeTransformer: TreeTransformer
) : ViewModel() {

    private val _groups = MutableStateFlow<List<PhotoGroup>>(emptyList())
    val groups: StateFlow<List<PhotoGroup>> = _groups.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            val all = sampleRepo.getAllWithPhoto()
            // 统一走 TreeTransformer 过滤 hidden 名称
            val grouped = all.groupBy { Triple(
                treeTransformer.visibleDeviceName(it.deviceName),
                treeTransformer.visibleLayerName(it.layerName),
                it.boxName
            ) }.map { (key, samples) ->
                PhotoGroup(deviceName = key.first, layerName = key.second, boxName = key.third, samples = samples)
            }
            _groups.value = grouped
        }
    }

    fun toggleSelection(sampleId: Long) {
        _selectedIds.value = _selectedIds.value.let { current ->
            if (sampleId in current) current - sampleId else current + sampleId
        }
    }

    fun deleteSelected(onDone: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            val ids = _selectedIds.value.toList()
            for (id in ids) {
                val sample = sampleRepo.getById(id)
                if (sample != null) {
                    photoManager.deletePhoto(sample.photoPath)
                    sampleRepo.update(sample.copy(photoPath = null))
                }
            }
            _selectedIds.value = emptySet()
            load()
            _isDeleting.value = false
            onDone()
        }
    }

    fun deleteAllNamedOrNoted(onDone: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            val items = sampleRepo.getWithPhotoAndNameOrNote()
            for (s in items) {
                val sample = sampleRepo.getById(s.sampleId)
                if (sample != null) {
                    photoManager.deletePhoto(sample.photoPath)
                    sampleRepo.update(sample.copy(photoPath = null))
                }
            }
            load()
            _isDeleting.value = false
            onDone()
        }
    }
}