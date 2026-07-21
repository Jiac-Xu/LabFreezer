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
import com.labfreezer.data.search.SearchNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchResultItem {
    data class Device(val entity: StorageDeviceEntity) : SearchResultItem()
    data class Layer(val entity: StorageLayerEntity, val deviceName: String) : SearchResultItem()
    data class Box(val entity: StorageBoxEntity, val deviceName: String, val layerName: String) : SearchResultItem()
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

            // Device/Layer/Box 搜索：使用原始关键词（层级名称格式较统一，无需模糊匹配）
            val deviceResults = deviceRepository.searchByName(trimmed).map { SearchResultItem.Device(it) }

            val layerResults = layerRepository.searchByName(trimmed).map { layer ->
                val device = deviceRepository.getById(layer.deviceId)
                SearchResultItem.Layer(layer, device?.name ?: "")
            }

            val boxResults = boxRepository.searchByName(trimmed).map { box ->
                val layer = layerRepository.getById(box.layerId)
                val device = layer?.let { deviceRepository.getById(it.deviceId) }
                SearchResultItem.Box(box, device?.name ?: "", layer?.name ?: "")
            }

            // Sample 搜索：使用 SearchNormalizer 生成多关键词变体，并行查询后去重
            val nameVariants = SearchNormalizer.generateNameVariants(trimmed)
            val dateVariants = SearchNormalizer.generateDateVariants(trimmed)
            val allVariants = (nameVariants + dateVariants).distinct()

            val sampleResults = if (allVariants.isNotEmpty()) {
                allVariants
                    .map { variant ->
                        async {
                            if (tagIds.isEmpty()) {
                                sampleRepository.searchWithPath(variant)
                            } else {
                                sampleRepository.searchWithPathByTags(variant, tagIds)
                            }
                        }
                    }
                    .awaitAll()
                    .flatten()
                    .distinctBy { it.sampleId }
                    .map { SearchResultItem.Sample(it) }
            } else {
                emptyList()
            }

            _results.value = deviceResults + layerResults + boxResults + sampleResults
            _isSearching.value = false
        }
    }
}
