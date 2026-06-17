package com.labfreezer.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchResultItem {
    data class Device(val entity: StorageDeviceEntity) : SearchResultItem()
    data class Layer(val entity: StorageLayerEntity) : SearchResultItem()
    data class Box(val entity: StorageBoxEntity) : SearchResultItem()
    data class Sample(val sample: SampleWithPath) : SearchResultItem()
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sampleRepository: SamplePositionRepository,
    private val tagRepository: TagRepository,
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val results: StateFlow<List<SearchResultItem>> = _results

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _allTags = MutableStateFlow<List<TagEntity>>(emptyList())
    val allTags: StateFlow<List<TagEntity>> = _allTags

    private val _selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedTagIds: StateFlow<Set<Long>> = _selectedTagIds

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _allTags.value = tagRepository.getAll()
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        triggerSearch()
    }

    fun toggleTag(tagId: Long) {
        val current = _selectedTagIds.value.toMutableSet()
        if (current.contains(tagId)) current.remove(tagId) else current.add(tagId)
        _selectedTagIds.value = current
        if (_query.value.isNotBlank()) triggerSearch()
    }

    private fun triggerSearch() {
        searchJob?.cancel()
        val q = _query.value
        if (q.isBlank()) {
            _results.value = emptyList()
            _isSearching.value = false
            return
        }
        _isSearching.value = true
        searchJob = viewModelScope.launch {
            delay(300)
            val trimmed = q.trim()
            val tagIds = _selectedTagIds.value.toList()

            val deviceResults = deviceRepository.searchByName(trimmed).map { SearchResultItem.Device(it) }
            val layerResults = layerRepository.searchByName(trimmed).map { SearchResultItem.Layer(it) }
            val boxResults = boxRepository.searchByName(trimmed).map { SearchResultItem.Box(it) }

            val sampleResults = if (tagIds.isEmpty()) {
                sampleRepository.searchWithPath(trimmed)
            } else {
                sampleRepository.searchWithPathByTags(trimmed, tagIds)
            }.map { SearchResultItem.Sample(it) }

            _results.value = deviceResults + layerResults + boxResults + sampleResults
            _isSearching.value = false
        }
    }
}
