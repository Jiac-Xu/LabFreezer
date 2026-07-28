package com.labfreezer.ui.screens.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.search.ParsedQuery
import com.labfreezer.data.search.ScopeType
import com.labfreezer.data.search.SearchHistoryManager
import com.labfreezer.data.search.SearchHistoryItem
import com.labfreezer.data.search.SearchNormalizer
import com.labfreezer.data.search.SearchQueryParser
import com.labfreezer.data.search.SearchScope
import com.labfreezer.ui.screens.sample.FilterLogic
import com.labfreezer.ui.screens.sample.FilterType
import com.labfreezer.ui.screens.sample.SearchCondition
import com.labfreezer.ui.screens.sample.SearchFilterContext
import com.labfreezer.ui.screens.settings.PersonalizationPreferences
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

// ==================== Faceted Search 数据结构 ====================

enum class FacetType { LOCATION, DATE, TAG }

data class FacetGroup(
    val type: FacetType,
    val label: String,
    val options: List<FacetOption>
)

data class FacetOption(
    val id: String,
    val label: String,
    val count: Int,
    val isSelected: Boolean = false,
    val locationInfo: LocationInfo? = null
)

data class LocationInfo(
    val deviceId: Long,
    val layerId: Long,
    val boxId: Long
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sampleRepository: SamplePositionRepository,
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val searchHistoryManager: SearchHistoryManager,
    private val personalizationPreferences: PersonalizationPreferences
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _results = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val results: StateFlow<List<SearchResultItem>> = _results

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchHistory = MutableStateFlow<List<SearchHistoryItem>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryItem>> = _searchHistory

    private val _scope = MutableStateFlow(SearchScope())
    val scope: StateFlow<SearchScope> = _scope

    // Faceted Search 状态
    private val _facetGroups = MutableStateFlow<List<FacetGroup>>(emptyList())
    val facetGroups: StateFlow<List<FacetGroup>> = _facetGroups

    // 未经过 Facet 筛选的原始结果（用于重新计算筛选）
    private var unfilteredSampleResults: List<SearchResultItem.Sample> = emptyList()

    // 当前选中的 Facet Option ID 集合
    private val _activeFacetIds = MutableStateFlow<Set<String>>(emptySet())
    val activeFacetIds: StateFlow<Set<String>> = _activeFacetIds

    private var searchJob: Job? = null

    init {
        _searchHistory.value = if (personalizationPreferences.isSearchHistoryEnabled()) {
            searchHistoryManager.getHistory()
        } else {
            emptyList()
        }
    }

    /**
     * 设置搜索范围。
     * 只在初始设置时调用，搜索过程中不改变。
     */
    fun setScope(newScope: SearchScope) {
        if (_scope.value != newScope) {
            _scope.value = newScope
            if (_query.value.isNotBlank()) triggerSearch()
        }
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        triggerSearch()
    }

    // ==================== Faceted Search 方法 ====================

    /**
     * 切换某个 Facet 选项的选中状态。
     * 组内 OR，组间 AND。
     */
    fun toggleFacet(facetId: String) {
        val current = _activeFacetIds.value.toMutableSet()
        if (current.contains(facetId)) {
            current.remove(facetId)
        } else {
            current.add(facetId)
        }
        _activeFacetIds.value = current
        applyFacetFilters()
        // 更新 FacetGroup 中的 isSelected 状态
        updateFacetSelectionState()
    }

    /**
     * 清除所有 Facet 筛选条件。
     */
    fun clearFacets() {
        _activeFacetIds.value = emptySet()
        applyFacetFilters()
        updateFacetSelectionState()
    }

    /**
     * 是否有任何 Facet 筛选条件被激活。
     */
    fun hasActiveFacets(): Boolean = _activeFacetIds.value.isNotEmpty()

    // ==================== 搜索方法 ====================

    private fun triggerSearch() {
        searchJob?.cancel()
        val q = _query.value
        if (q.isBlank()) {
            _results.value = emptyList()
            _facetGroups.value = emptyList()
            _activeFacetIds.value = emptySet()
            unfilteredSampleResults = emptyList()
            _isSearching.value = false
            return
        }

        _isSearching.value = true
        searchJob = viewModelScope.launch {
            delay(300)
            val trimmed = q.trim()
            val currentScope = _scope.value

            // 1. 解析输入
            val parsed = SearchQueryParser.parse(trimmed)

            // 2. 根据 scope 搜索
            val (deviceResults, layerResults, boxResults, sampleResults) = when (currentScope.type) {
                ScopeType.ALL -> searchAll(trimmed, parsed)
                ScopeType.DEVICE -> searchByDevice(trimmed, parsed, currentScope.id!!)
                ScopeType.LEVEL -> searchByLevel(trimmed, parsed, currentScope.id!!)
                ScopeType.BOX -> searchByBox(trimmed, parsed, currentScope.id!!)
            }

            // 3. 拼接结果
            val allResults = deviceResults + layerResults + boxResults + sampleResults
            _results.value = allResults

            // 4. 提取样本结果用于 Facet 计算
            unfilteredSampleResults = sampleResults

            // 5. 计算 Facet Group
            _facetGroups.value = computeFacets(sampleResults, currentScope.type)

            // 6. 如果之前有激活的筛选条件，重新应用
            if (_activeFacetIds.value.isNotEmpty()) {
                applyFacetFilters()
            }

            _isSearching.value = false
        }
    }

    /**
     * 全局搜索：搜索所有设备、层级、盒子和样本。
     */
    private suspend fun searchAll(
        trimmed: String,
        parsed: ParsedQuery
    ): SearchResults {
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
        val sampleResults = searchSamples(parsed, emptyList()) { query, tagIds ->
            if (tagIds.isEmpty()) sampleRepository.searchWithPath(query)
            else sampleRepository.searchWithPathByTags(query, tagIds)
        }
        return SearchResults(deviceResults, layerResults, boxResults, sampleResults)
    }

    /**
     * 设备范围搜索：搜索当前设备内的盒子和样本。
     */
    private suspend fun searchByDevice(
        trimmed: String,
        parsed: ParsedQuery,
        deviceId: Long
    ): SearchResults {
        // 不搜索设备实体（已在设备内）
        // 不搜索层级实体（已在设备内）
        val boxResults = boxRepository.searchByName(trimmed).filter { box ->
            val layer = layerRepository.getById(box.layerId)
            layer?.deviceId == deviceId
        }.map { box ->
            val layer = layerRepository.getById(box.layerId)
            val device = deviceRepository.getById(deviceId)
            SearchResultItem.Box(box, device?.name ?: "", layer?.name ?: "")
        }

        val sampleResults = searchSamples(parsed, emptyList()) { query, tagIds ->
            if (tagIds.isEmpty()) sampleRepository.searchWithPathByDevice(query, deviceId)
            else sampleRepository.searchWithPathByDeviceAndTags(query, deviceId, tagIds)
        }
        return SearchResults(emptyList(), emptyList(), boxResults, sampleResults)
    }

    /**
     * 层级范围搜索：搜索当前层级内的盒子和样本。
     */
    private suspend fun searchByLevel(
        trimmed: String,
        parsed: ParsedQuery,
        layerId: Long
    ): SearchResults {
        val boxResults = boxRepository.searchByName(trimmed).filter { it.layerId == layerId }
            .map { box ->
                val layer = layerRepository.getById(layerId)
                val device = layer?.let { deviceRepository.getById(it.deviceId) }
                SearchResultItem.Box(box, device?.name ?: "", layer?.name ?: "")
            }

        val sampleResults = searchSamples(parsed, emptyList()) { query, tagIds ->
            if (tagIds.isEmpty()) sampleRepository.searchWithPathByLayer(query, layerId)
            else sampleRepository.searchWithPathByLayerAndTags(query, layerId, tagIds)
        }
        return SearchResults(emptyList(), emptyList(), boxResults, sampleResults)
    }

    /**
     * 盒子范围搜索：只搜索当前盒子内的样本。
     */
    private suspend fun searchByBox(
        trimmed: String,
        parsed: ParsedQuery,
        boxId: Long
    ): SearchResults {
        // 不搜索盒子实体（已在盒子内）
        val sampleResults = searchSamples(parsed, emptyList()) { query, tagIds ->
            if (tagIds.isEmpty()) sampleRepository.searchWithPathByBox(query, boxId)
            else sampleRepository.searchWithPathByBoxAndTags(query, boxId, tagIds)
        }
        return SearchResults(emptyList(), emptyList(), emptyList(), sampleResults)
    }

    // ==================== 样本搜索核心逻辑 ====================

    /**
     * 样本搜索的抽象：通过 scopeQuery 回调注入不同范围的数据访问。
     */
    private suspend fun searchSamples(
        parsed: ParsedQuery,
        tagIds: List<Long>,
        scopeQuery: suspend (String, List<Long>) -> List<SampleWithPath>
    ): List<SearchResultItem.Sample> {
        val normalizedKeyword = SearchNormalizer.normalizeNameForCompare(parsed.nameKeyword)

        val nameMatches = if (parsed.nameKeyword.isNotBlank()) {
            searchByName(parsed.nameKeyword, normalizedKeyword, tagIds, scopeQuery)
        } else {
            null
        }

        val dateMatches = if (parsed.dateKeywords.isNotEmpty()) {
            searchByDate(parsed.dateKeywords, tagIds, scopeQuery)
        } else {
            null
        }

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

    private suspend fun searchByName(
        nameKeyword: String,
        normalizedKeyword: String,
        tagIds: List<Long>,
        scopeQuery: suspend (String, List<Long>) -> List<SampleWithPath>
    ): List<SampleWithPath> {
        val variants = SearchNormalizer.generateNameVariants(nameKeyword)
        val variantResults = coroutineScope {
            variants.map { variant ->
                async { scopeQuery(variant, tagIds) }
            }.awaitAll().flatten()
        }

        var filtered = normalizeFilter(variantResults, normalizedKeyword)

        if (filtered.isEmpty() && nameKeyword.length >= 4) {
            val broadPrefix = extractBroadPrefix(nameKeyword)
            if (broadPrefix != null) {
                val broadResults = scopeQuery(broadPrefix, tagIds)
                filtered = normalizeFilter(broadResults, normalizedKeyword)
            }
        }

        return filtered
    }

    private suspend fun searchByDate(
        dateKeywords: List<String>,
        tagIds: List<Long>,
        scopeQuery: suspend (String, List<Long>) -> List<SampleWithPath>
    ): List<SampleWithPath> {
        return coroutineScope {
            dateKeywords.flatMap { dateKw ->
                val dateVariants = SearchNormalizer.generateDateVariants(dateKw)
                dateVariants.map { variant ->
                    async { scopeQuery(variant, tagIds) }
                }.awaitAll().flatten()
            }
        }
    }

    /**
     * 归一化后置过滤。
     */
    private fun normalizeFilter(
        results: List<SampleWithPath>,
        normalizedKeyword: String
    ): List<SampleWithPath> {
        if (normalizedKeyword.isEmpty()) return results
        return results.filter { sample ->
            val storedName = SearchNormalizer.normalizeNameForCompare(sample.name)
            val storedNote = SearchNormalizer.normalizeNameForCompare(sample.note)
            storedName == normalizedKeyword ||
                storedName.startsWith(normalizedKeyword) ||
                storedName.contains(normalizedKeyword) ||
                storedNote.contains(normalizedKeyword)
        }
    }

    private fun extractBroadPrefix(keyword: String): String? {
        val stripped = keyword.filter { it !in SearchNormalizer.SEPARATORS }
        val prefix = stripped.takeWhile { it.isLetter() }
        return if (prefix.length >= 2) prefix else null
    }

    // ==================== Faceted Search 计算 ====================

    /**
     * 根据搜索结果计算 Facet 分组。
     * 规则：如果某个维度只有一个选项，则不显示该维度。
     */
    private suspend fun computeFacets(
        sampleResults: List<SearchResultItem.Sample>,
        scopeType: ScopeType
    ): List<FacetGroup> {
        val groups = mutableListOf<FacetGroup>()

        // 预填充标签缓存，供 applyFacetFilters 非挂起使用
        sampleTagCache.clear()
        if (sampleResults.isNotEmpty()) {
            val sampleIds = sampleResults.map { it.sample.sampleId }
            val tagInfos = sampleRepository.getTagsBySampleIds(sampleIds)
            for (info in tagInfos) {
                sampleTagCache.getOrPut(info.sampleId) { mutableSetOf() }.add(info.tagId)
            }
        }

        // 1. 位置 Facet（按 boxName 分组）
        val locationGroup = computeLocationFacet(sampleResults, scopeType)
        if (locationGroup != null) groups.add(locationGroup)

        // 2. 日期 Facet（按 yyyy-MM 分组）
        val dateGroup = computeDateFacet(sampleResults)
        if (dateGroup != null) groups.add(dateGroup)

        // 3. 标签 Facet（按 tag 分组）
        val tagGroup = computeTagFacet(sampleResults)
        if (tagGroup != null) groups.add(tagGroup)

        return groups
    }

    private fun computeLocationFacet(
        sampleResults: List<SearchResultItem.Sample>,
        scopeType: ScopeType
    ): FacetGroup? {
        // 按 boxId 分组
        val locationMap = LinkedHashMap<Long, MutableList<SampleWithPath>>()
        for (item in sampleResults) {
            locationMap.getOrPut(item.sample.boxId) { mutableListOf() }.add(item.sample)
        }

        // 如果只有一个或零个位置，不显示
        if (locationMap.size <= 1) return null

        val options = locationMap.map { (boxId, samples) ->
            val sample = samples.first()
            // 根据 scope 类型决定显示什么
            // ALL 范围：显示 boxName（足够区分）
            // DEVICE/LEVEL 范围：显示 boxName（同设备/层级内盒子名唯一）
            // BOX 范围：不可能有多个位置
            FacetOption(
                id = "loc_$boxId",
                label = sample.boxName,
                count = samples.size,
                isSelected = _activeFacetIds.value.contains("loc_$boxId"),
                locationInfo = LocationInfo(
                    deviceId = 0, // 不需要，筛选时只用 boxId
                    layerId = 0,
                    boxId = boxId
                )
            )
        }

        return FacetGroup(
            type = FacetType.LOCATION,
            label = "位置",
            options = options
        )
    }

    private fun computeDateFacet(
        sampleResults: List<SearchResultItem.Sample>
    ): FacetGroup? {
        // 按 yyyy-MM 分组
        val dateMap = LinkedHashMap<String, MutableList<SampleWithPath>>()
        for (item in sampleResults) {
            val ym = SearchNormalizer.parseDateToYearMonth(item.sample.date)
            if (ym != null) {
                dateMap.getOrPut(ym) { mutableListOf() }.add(item.sample)
            }
        }

        // 如果只有一个或零个日期，不显示
        if (dateMap.size <= 1) return null

        val options = dateMap.map { (ym, samples) ->
            FacetOption(
                id = "date_$ym",
                label = ym,
                count = samples.size,
                isSelected = _activeFacetIds.value.contains("date_$ym")
            )
        }

        return FacetGroup(
            type = FacetType.DATE,
            label = "日期",
            options = options
        )
    }

    private suspend fun computeTagFacet(
        sampleResults: List<SearchResultItem.Sample>
    ): FacetGroup? {
        if (sampleTagCache.isEmpty()) return null

        // 使用已预填充的缓存反向统计：按 tagId 统计样本数
        val tagSampleCount = mutableMapOf<Long, Pair<String, Int>>()
        val sampleTagCounts = mutableMapOf<Long, Int>() // tagId -> count
        val tagNameMap = mutableMapOf<Long, String>() // tagId -> name

        // 通过缓存获取所有 tagId -> name 映射
        val allTagIds = sampleTagCache.values.flatten().toSet()
        if (allTagIds.isEmpty()) return null

        // 从缓存中统计每个 tag 关联的样本数
        for ((sampleId, tagIds) in sampleTagCache) {
            for (tagId in tagIds) {
                sampleTagCounts[tagId] = (sampleTagCounts[tagId] ?: 0) + 1
            }
        }

        // 需要获取标签名称（从数据库或缓存）
        // 从数据库获取标签名称
        val tagInfos = sampleRepository.getTagsBySampleIds(sampleTagCache.keys.toList())
        for (info in tagInfos) {
            tagNameMap[info.tagId] = info.tagName
        }

        // 如果只有一个标签，不显示
        if (sampleTagCounts.size <= 1) return null

        val options = sampleTagCounts.map { (tagId, count) ->
            FacetOption(
                id = "tag_$tagId",
                label = tagNameMap[tagId] ?: "#$tagId",
                count = count,
                isSelected = _activeFacetIds.value.contains("tag_$tagId")
            )
        }

        return FacetGroup(
            type = FacetType.TAG,
            label = "标签",
            options = options
        )
    }

    /**
     * 应用 Facet 筛选条件，更新 _results。
     * 组内 OR，组间 AND。
     */
    private fun applyFacetFilters() {
        val activeIds = _activeFacetIds.value
        if (activeIds.isEmpty() || unfilteredSampleResults.isEmpty()) {
            // 没有筛选条件，恢复原始结果
            _results.value = getNonSampleResults() + unfilteredSampleResults
            return
        }

        // 按 FacetType 分组激活的 ID
        val activeByType = activeIds.groupBy { id ->
            when {
                id.startsWith("loc_") -> FacetType.LOCATION
                id.startsWith("date_") -> FacetType.DATE
                id.startsWith("tag_") -> FacetType.TAG
                else -> null
            }
        }

        val filteredSamples = unfilteredSampleResults.filter { sampleItem ->
            val sample = sampleItem.sample

            // 位置条件：组内 OR
            val locationIds = activeByType[FacetType.LOCATION] ?: emptyList()
            val locationMatch = locationIds.isEmpty() ||
                locationIds.any { id ->
                    val boxId = id.removePrefix("loc_").toLongOrNull()
                    boxId != null && sample.boxId == boxId
                }

            // 日期条件：组内 OR
            val dateIds = activeByType[FacetType.DATE] ?: emptyList()
            val dateMatch = dateIds.isEmpty() ||
                dateIds.any { id ->
                    val targetYm = id.removePrefix("date_")
                    val sampleYm = SearchNormalizer.parseDateToYearMonth(sample.date)
                    sampleYm != null && sampleYm == targetYm
                }

            // 标签条件：组内 OR（需要查数据库）
            val tagIds = activeByType[FacetType.TAG] ?: emptyList()
            val tagMatch = tagIds.isEmpty() ||
                tagIds.any { id ->
                    val tagId = id.removePrefix("tag_").toLongOrNull() ?: return@any false
                    // 这里需要检查样本是否有该标签
                    // 使用缓存或同步查询
                    sampleHasTagCached(sample.sampleId, tagId)
                }

            // 组间 AND
            locationMatch && dateMatch && tagMatch
        }

        _results.value = if (activeIds.isNotEmpty()) {
            // 有筛选条件时，只显示样本结果，隐藏设备/层/盒子
            filteredSamples
        } else {
            getNonSampleResults() + filteredSamples
        }
    }

    /**
     * 缓存样本标签关系，避免重复查询数据库。
     * 在 computeFacets 时预填充，applyFacetFilters 时只读。
     */
    private val sampleTagCache = mutableMapOf<Long, MutableSet<Long>>()

    private fun sampleHasTagCached(sampleId: Long, tagId: Long): Boolean {
        val cached = sampleTagCache[sampleId]
        return cached != null && tagId in cached
    }

    /**
     * 更新 FacetGroup 中的选中状态。
     */
    private fun updateFacetSelectionState() {
        val activeIds = _activeFacetIds.value
        _facetGroups.value = _facetGroups.value.map { group ->
            group.copy(
                options = group.options.map { option ->
                    option.copy(isSelected = option.id in activeIds)
                }
            )
        }
    }

    /**
     * 从 _results 中提取非样本结果（Device, Layer, Box）。
     */
    private fun getNonSampleResults(): List<SearchResultItem> {
        return _results.value.filter { it !is SearchResultItem.Sample }
    }

    /**
     * 从当前 Facet 状态构建 SearchFilterContext。
     * 用于传递到 SampleEditPage 展示筛选条件摘要。
     */
    fun buildFilterContext(): SearchFilterContext {
        val activeIds = _activeFacetIds.value
        if (activeIds.isEmpty()) return SearchFilterContext(emptyList(), FilterLogic.AND)

        val conditions = mutableListOf<SearchCondition>()

        // 盒子筛选
        val boxLabels = activeIds.filter { it.startsWith("loc_") }.mapNotNull { id ->
            val boxId = id.removePrefix("loc_").toLongOrNull()
            if (boxId != null) {
                // 从 facetGroups 中查找对应的 label
                _facetGroups.value.find { it.type == FacetType.LOCATION }
                    ?.options?.find { it.id == id }?.label
            } else null
        }
        if (boxLabels.isNotEmpty()) {
            conditions.add(SearchCondition(FilterType.BOX, boxLabels))
        }

        // 日期筛选
        val dateLabels = activeIds.filter { it.startsWith("date_") }.mapNotNull { id ->
            _facetGroups.value.find { it.type == FacetType.DATE }
                ?.options?.find { it.id == id }?.label
        }
        if (dateLabels.isNotEmpty()) {
            conditions.add(SearchCondition(FilterType.DATE, dateLabels))
        }

        // 标签筛选
        val tagLabels = activeIds.filter { it.startsWith("tag_") }.mapNotNull { id ->
            _facetGroups.value.find { it.type == FacetType.TAG }
                ?.options?.find { it.id == id }?.label
        }
        if (tagLabels.isNotEmpty()) {
            conditions.add(SearchCondition(FilterType.TAG, tagLabels))
        }

        return SearchFilterContext(conditions, FilterLogic.AND)
    }

    // ==================== 搜索历史 ====================

    fun saveCurrentQuery() {
        val q = _query.value.trim()
        if (q.isNotBlank() && personalizationPreferences.isSearchHistoryEnabled()) {
            searchHistoryManager.addKeyword(q)
            _searchHistory.value = searchHistoryManager.getHistory()
        }
    }

    fun onHistoryItemClick(keyword: String) {
        _query.value = keyword
        triggerSearch()
        saveCurrentQuery()
    }

    fun clearSearchHistory() {
        searchHistoryManager.clearAll()
        _searchHistory.value = emptyList()
    }

    // ==================== 内部数据结构 ====================

    private data class SearchResults(
        val devices: List<SearchResultItem.Device> = emptyList(),
        val layers: List<SearchResultItem.Layer> = emptyList(),
        val boxes: List<SearchResultItem.Box> = emptyList(),
        val samples: List<SearchResultItem.Sample> = emptyList()
    )
}