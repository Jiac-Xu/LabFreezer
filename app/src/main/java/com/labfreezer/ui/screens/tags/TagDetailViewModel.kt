package com.labfreezer.ui.screens.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.data.repository.TagRepository
import com.labfreezer.data.repository.TreeTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tagRepository: TagRepository,
    private val treeTransformer: TreeTransformer
) : ViewModel() {

    private val tagId: Long = savedStateHandle["tagId"] ?: -1L

    private val _tag = MutableStateFlow<TagEntity?>(null)
    val tag: StateFlow<TagEntity?> = _tag

    private val _samples = MutableStateFlow<List<SampleWithPath>>(emptyList())
    val samples: StateFlow<List<SampleWithPath>> = _samples

    init {
        viewModelScope.launch {
            _tag.value = tagRepository.getById(tagId)
            val raw = tagRepository.getSamplesWithPathByTagId(tagId)
            // 过滤 hidden 名称（统一走 TreeTransformer）
            _samples.value = raw.map { sample ->
                sample.copy(
                    deviceName = treeTransformer.visibleDeviceName(sample.deviceName),
                    layerName = treeTransformer.visibleLayerName(sample.layerName)
                )
            }
        }
    }
}