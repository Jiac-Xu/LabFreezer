package com.labfreezer.ui.screens.tags

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tagRepository: TagRepository
) : ViewModel() {

    private val tagId: Long = savedStateHandle["tagId"] ?: -1L

    private val _tag = MutableStateFlow<TagEntity?>(null)
    val tag: StateFlow<TagEntity?> = _tag

    private val _samples = MutableStateFlow<List<SampleWithPath>>(emptyList())
    val samples: StateFlow<List<SampleWithPath>> = _samples

    init {
        viewModelScope.launch {
            _tag.value = tagRepository.getById(tagId)
            _samples.value = tagRepository.getSamplesWithPathByTagId(tagId)
        }
    }
}
