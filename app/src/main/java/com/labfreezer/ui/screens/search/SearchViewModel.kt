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
import com.labfreezer.data.search.ParsedQuery
import com.labfreezer.data.search.SearchHistoryManager
import com.labfreezer.data.search.SearchHistoryItem
import com.labfreezer.data.search.SearchNormalizer
import com.labfreezer.data.search.SearchQueryParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    private val boxRepository: StorageBoxRepository,
    private val searchHistoryManager: SearchHistoryManager
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

    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _allTags.value = tagRepository.getAll()
        }
        _searchHistory.value = searchHistoryManager.getHistory()
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

        // 保存搜索历史（非空关键词）
        searchHistoryManager.addKeyword(q)
        _searchHistory.value = searchHistoryManager.getHistory()
        _isSearching.value = true
        searchJob = viewModelScope.launch {
            delay(300)
            val trimmed = q.trim()
            val tagIds = _selectedTagIds.value.toList()

            // 1. 解析输入：分离名称关键词和日期关键词
            val parsed = SearchQueryParser.parse(trimmed)

            // 2. Device/Layer/Box 搜索（使用原始关键词，层级名称格式较统一）
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

            // 3. Sample 搜索：名称模糊搜索 + 日期智能搜索
            val sampleResults = searchSamples(parsed, tagIds)

            _results.value = deviceResults + layerResults + boxResults + sampleResults
            _isSearching.value = false
        }
    }

    /**
     * 执行样本搜索：
     * - 名称搜索：变体查询 → 若为空则 fallback 宽泛前缀 → 归一化后置过滤
     * - 日期搜索：日期变体查询
     * - 多条件时取 AND（交集）
     */
    private suspend fun searchSamples(
        parsed: ParsedQuery,
        tagIds: List<Long>
    ): List<SearchResultItem.Sample> {
        val normalizedKeyword = SearchNormalizer.normalizeNameForCompare(parsed.nameKeyword)

        // 名称搜索
        val nameMatches = if (parsed.nameKeyword.isNotBlank()) {
            searchByName(parsed.nameKeyword, normalizedKeyword, tagIds)
        } else {
            null
        }

        // 日期搜索
        val dateMatches = if (parsed.dateKeywords.isNotEmpty()) {
            searchByDate(parsed.dateKeywords, tagIds)
        } else {
            null
        }

        // 结果组合：name AND date（交集）
        val combined = when {
            nameMatches != null && dateMatches != null -> {
                val nameIds = nameMatches.map { it.sampleId }.toSet()
                dateMatches.filter { it.sampleId in nameIds }
            }
            nameMatches != null -> nameMatches
            dateMatches != null -> dateMatches
            else -> emptyList()
        }

        return combined
            .distinctBy { it.sampleId }
            .map { SearchResultItem.Sample(it) }
    }

    /**
     * 按名称搜索样本。
     *
     * 1. 生成名称变体，并行 SQL 查询
     * 2. 归一化后置过滤
     * 3. 若结果为空，fallback 到宽泛前缀查询 + 归一化过滤
     */
    private suspend fun searchByName(
        nameKeyword: String,
        normalizedKeyword: String,
        tagIds: List<Long>
    ): List<SampleWithPath> {
        // Step 1: 变体查询
        val variants = SearchNormalizer.generateNameVariants(nameKeyword)
        val variantResults = coroutineScope {
            variants.map { variant ->
                async {
                    if (tagIds.isEmpty()) {
                        sampleRepository.searchWithPath(variant)
                    } else {
                        sampleRepository.searchWithPathByTags(variant, tagIds)
                    }
                }
            }.awaitAll().flatten()
        }

        var filtered = normalizeFilter(variantResults, normalizedKeyword)

        // Step 2: Fallback — 变体结果为空时，尝试宽泛前缀查询
        if (filtered.isEmpty() && nameKeyword.length >= 4) {
            val broadPrefix = extractBroadPrefix(nameKeyword)
            if (broadPrefix != null) {
                val broadResults = if (tagIds.isEmpty()) {
                    sampleRepository.searchWithPath(broadPrefix)
                } else {
                    sampleRepository.searchWithPathByTags(broadPrefix, tagIds)
                }
                filtered = normalizeFilter(broadResults, normalizedKeyword)
            }
        }

        return filtered
    }

    /**
     * 按日期搜索样本。
     *
     * 对每个日期关键词生成日期变体，并行 SQL 查询后合并。
     */
    private suspend fun searchByDate(
        dateKeywords: List<String>,
        tagIds: List<Long>
    ): List<SampleWithPath> {
        return coroutineScope {
            dateKeywords.flatMap { dateKw ->
                val dateVariants = SearchNormalizer.generateDateVariants(dateKw)
                dateVariants.map { variant ->
                    async {
                        if (tagIds.isEmpty()) {
                            sampleRepository.searchWithPath(variant)
                        } else {
                            sampleRepository.searchWithPathByTags(variant, tagIds)
                        }
                    }
                }.awaitAll().flatten()
            }
        }
    }

    /**
     * 归一化后置过滤。
     *
     * 同时检查 name 和 note 字段：
     * - name 匹配优先级：== > startsWith > contains
     * - note 使用 contains 匹配（自由文本内容）
     *
     * 避免 "R10" 匹配到 "R100xxx" 等不相关结果。
     */
    private fun normalizeFilter(
        results: List<SampleWithPath>,
        normalizedKeyword: String
    ): List<SampleWithPath> {
        if (normalizedKeyword.isEmpty()) return results

        return results.filter { sample ->
            val storedName = SearchNormalizer.normalizeNameForCompare(sample.name)
            val storedNote = SearchNormalizer.normalizeNameForCompare(sample.note)
            // name 匹配（格式容错）
            storedName == normalizedKeyword ||
                storedName.startsWith(normalizedKeyword) ||
                storedName.contains(normalizedKeyword) ||
                // note 匹配（自由文本，使用 contains）
                storedNote.contains(normalizedKeyword)
        }
    }

    /**
     * 从关键词中提取首字母段作为宽泛前缀。
     *
     * 例如：
     * - "MV411" → "MV"（LIKE '%MV%' 可匹配 "MV4-11"）
     * - "HL60WT" → "HL"（LIKE '%HL%' 可匹配 "HL60 WT"）
     * - "R100" → null（单字母过于宽泛）
     * - "123ABC" → null（不以字母开头）
     */
    private fun extractBroadPrefix(keyword: String): String? {
        val stripped = keyword.filter { it !in SearchNormalizer.SEPARATORS }
        val prefix = stripped.takeWhile { it.isLetter() }
        return if (prefix.length >= 2) prefix else null
    }

    // ========== 搜索历史 ==========

    /**
     * 点击历史搜索关键词，直接执行搜索。
     */
    fun onHistoryItemClick(keyword: String) {
        _query.value = keyword
        triggerSearch()
    }

    /**
     * 清空所有搜索历史。
     */
    fun clearSearchHistory() {
        searchHistoryManager.clearAll()
        _searchHistory.value = emptyList()
    }
}
