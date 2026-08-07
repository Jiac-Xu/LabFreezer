package com.labfreezer.ui.screens.move

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.model.Position
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TreeTransformer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private fun positionToLabel(row: Int, col: Int): String = Position.toLabel(row, col)

@HiltViewModel
class MoveBrowserViewModel @Inject constructor(
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val sampleRepository: SamplePositionRepository,
    private val treeTransformer: TreeTransformer
) : ViewModel() {

    private val _breadcrumb = MutableStateFlow<List<BreadcrumbItem>>(emptyList())
    val breadcrumb: StateFlow<List<BreadcrumbItem>> = _breadcrumb

    private val _currentLevel = MutableStateFlow(MoveLevel.DEVICE)
    val currentLevel: StateFlow<MoveLevel> = _currentLevel

    private val _selectedDeviceId = MutableStateFlow<Long?>(null)
    val selectedDeviceId: StateFlow<Long?> = _selectedDeviceId
    private val _selectedLayerId = MutableStateFlow<Long?>(null)
    private val _selectedBoxId = MutableStateFlow<Long?>(null)

    private val _devices = MutableStateFlow<List<StorageDeviceEntity>>(emptyList())
    val devices: StateFlow<List<StorageDeviceEntity>> = _devices

    private val _layers = MutableStateFlow<List<StorageLayerEntity>>(emptyList())
    val layers: StateFlow<List<StorageLayerEntity>> = _layers

    private val _boxes = MutableStateFlow<List<StorageBoxEntity>>(emptyList())
    val boxes: StateFlow<List<StorageBoxEntity>> = _boxes

    private val _gridCells = MutableStateFlow<List<GridCellInfo>>(emptyList())
    val gridCells: StateFlow<List<GridCellInfo>> = _gridCells

    private val _selectedPositions = MutableStateFlow<Set<Pair<Int, Int>>>(emptySet())
    val selectedPositions: StateFlow<Set<Pair<Int, Int>>> = _selectedPositions

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<SearchResult>>(emptyList())
    val searchResults: StateFlow<List<SearchResult>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _moveCompleted = MutableStateFlow(false)
    val moveCompleted: StateFlow<Boolean> = _moveCompleted

    private var allDevices: List<StorageDeviceEntity> = emptyList()
    private var allLayers: List<StorageLayerEntity> = emptyList()
    private var allBoxes: List<StorageBoxEntity> = emptyList()

    data class SearchResult(
        val label: String,
        val level: MoveLevel,
        val id: Long,
        val parentLabel: String = ""
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            allDevices = deviceRepository.getAll()
            allLayers = mutableListOf<StorageLayerEntity>().also { list ->
                for (d in allDevices) {
                    list.addAll(layerRepository.getByDeviceId(d.id))
                }
            }
            allBoxes = mutableListOf<StorageBoxEntity>().also { list ->
                for (l in allLayers) {
                    list.addAll(boxRepository.getByLayerId(l.id))
                }
            }
            _devices.value = allDevices
        }
    }

    fun navigateToDevice(deviceId: Long) {
        viewModelScope.launch {
            _selectedDeviceId.value = deviceId
            _selectedLayerId.value = null
            _selectedBoxId.value = null
            _selectedPositions.value = emptySet()

            val device = allDevices.find { it.id == deviceId }
            _breadcrumb.value = listOf(
                BreadcrumbItem(device?.name ?: "", MoveLevel.DEVICE, deviceId)
            )

            _layers.value = allLayers.filter { it.deviceId == deviceId }
            _currentLevel.value = MoveLevel.LAYER
        }
    }

    fun navigateToLayer(layerId: Long) {
        viewModelScope.launch {
            _selectedLayerId.value = layerId
            _selectedBoxId.value = null
            _selectedPositions.value = emptySet()

            val layer = allLayers.find { it.id == layerId } ?: return@launch
            val device = allDevices.find { it.id == layer.deviceId } ?: return@launch

            _breadcrumb.value = listOf(
                BreadcrumbItem(device.name, MoveLevel.DEVICE, device.id),
                BreadcrumbItem(layer.name, MoveLevel.LAYER, layerId)
            )

            _boxes.value = allBoxes.filter { it.layerId == layerId }
            _currentLevel.value = MoveLevel.BOX
        }
    }

    fun navigateToBox(boxId: Long) {
        viewModelScope.launch {
            _selectedBoxId.value = boxId
            _selectedPositions.value = emptySet()
            _currentLevel.value = MoveLevel.GRID

            val box = allBoxes.find { it.id == boxId }
            val layer = box?.let { allLayers.find { l -> l.id == it.layerId } }
            val device = layer?.let { allDevices.find { d -> d.id == it.deviceId } }

            _breadcrumb.value = listOfNotNull(
                device?.let { BreadcrumbItem(it.name, MoveLevel.DEVICE, it.id) },
                layer?.let { BreadcrumbItem(it.name, MoveLevel.LAYER, it.id) },
                box?.let { BreadcrumbItem(it.name, MoveLevel.BOX, boxId) }
            )

            loadGridCells(boxId)
        }
    }

    private suspend fun loadGridCells(boxId: Long) {
        val box = boxRepository.getById(boxId) ?: return
        val samples = sampleRepository.getByBoxId(boxId)
        val currentBoxId = MoveState.sourceBoxId
        val sampleMap = samples.associateBy { it.row * 1000 + it.col }

        val grid = mutableListOf<GridCellInfo>()
        for (r in 0 until box.rows) {
            for (c in 0 until box.cols) {
                val label = positionToLabel(r, c)
                val sample = sampleMap[r * 1000 + c]
                val isSelectedSample = boxId == currentBoxId && sample?.id in MoveState.selectedItemIds
                grid.add(
                    GridCellInfo(
                        row = r, col = c, label = label,
                        occupied = sample != null && !isSelectedSample,
                        occupiedBySampleId = if (isSelectedSample) null else sample?.id
                    )
                )
            }
        }
        _gridCells.value = grid
    }

    fun togglePosition(row: Int, col: Int) {
        val current = _selectedPositions.value.toMutableSet()
        val key = row to col
        if (key in current) {
            current.remove(key)
        } else {
            if (current.size < MoveState.selectedItemIds.size) {
                current.add(key)
            }
        }
        _selectedPositions.value = current
    }

    fun navigateToBreadcrumb(level: MoveLevel) {
        when (level) {
            MoveLevel.DEVICE -> {
                _selectedDeviceId.value = null
                _selectedLayerId.value = null
                _selectedBoxId.value = null
                _selectedPositions.value = emptySet()
                _breadcrumb.value = emptyList()
                _currentLevel.value = MoveLevel.DEVICE
                _devices.value = allDevices
            }
            MoveLevel.LAYER -> {
                _selectedDeviceId.value?.let { navigateToDevice(it) }
            }
            MoveLevel.BOX -> {
                _selectedLayerId.value?.let { navigateToLayer(it) }
            }
            MoveLevel.GRID -> {
                _selectedBoxId.value?.let { navigateToBox(it) }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }
        val q = query.lowercase()
        val results = mutableListOf<SearchResult>()

        results.addAll(
            allDevices.filter { it.name.lowercase().contains(q) }
                .map { SearchResult(it.name, MoveLevel.DEVICE, it.id) }
        )

        for (layer in allLayers.filter { it.name.lowercase().contains(q) }) {
            val parent = allDevices.find { it.id == layer.deviceId }
            results.add(
                SearchResult(layer.name, MoveLevel.LAYER, layer.id, parent?.name ?: "")
            )
        }

        for (box in allBoxes.filter { it.name.lowercase().contains(q) }) {
            val layer = allLayers.find { it.id == box.layerId }
            val device = layer?.let { allDevices.find { d -> d.id == it.deviceId } }
            val parentPath = listOfNotNull(device?.name, layer?.name).joinToString(" / ")
            results.add(
                SearchResult(box.name, MoveLevel.BOX, box.id, parentPath)
            )
        }

        _searchResults.value = results
        _isSearching.value = results.isNotEmpty()
    }

    fun navigateToSearchResult(result: SearchResult) {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _isSearching.value = false

        when (result.level) {
            MoveLevel.DEVICE -> navigateToDevice(result.id)
            MoveLevel.LAYER -> {
                val layer = allLayers.find { it.id == result.id } ?: return
                navigateToDevice(layer.deviceId)
                navigateToLayer(result.id)
            }
            MoveLevel.BOX -> {
                val box = allBoxes.find { it.id == result.id } ?: return
                navigateToDevice(allLayers.find { it.id == box.layerId }?.deviceId ?: return)
                navigateToLayer(box.layerId)
                navigateToBox(result.id)
            }
            else -> {}
        }
    }

    fun confirmMove(onCompleted: () -> Unit) {
        viewModelScope.launch {
            if (MoveState.selectMode) {
                when (MoveState.moveTarget) {
                    MoveTarget.DEVICE -> MoveState.resultDeviceId = _selectedDeviceId.value
                    MoveTarget.LAYER -> MoveState.resultLayerId = _selectedLayerId.value
                    MoveTarget.BOX -> {
                        MoveState.resultBoxId = _selectedBoxId.value
                        MoveState.selectedItemIds = _selectedPositions.value.map { it.first.toLong() }.toSet()
                        _selectedPositions.value.firstOrNull()?.let { (r, c) ->
                            MoveState.resultGridRow = r
                            MoveState.resultGridCol = c
                        }
                    }
                    MoveTarget.CONTAINER -> {
                        // CONTAINER 模式在 selectMode 下不应触发
                    }
                }
                MoveState.selectMode = false
                onCompleted()
                return@launch
            }
            val target = MoveState.moveTarget
            when (target) {
                MoveTarget.DEVICE -> {
                    val targetDeviceId = _selectedDeviceId.value ?: return@launch
                    for (id in MoveState.selectedItemIds) {
                        val layer = layerRepository.getById(id) ?: continue
                        layerRepository.update(layer.copy(deviceId = targetDeviceId))
                    }
                }
                MoveTarget.LAYER, MoveTarget.CONTAINER -> {
                    if (_selectedLayerId.value != null) {
                        // 移动到指定层级
                        val targetLayerId = _selectedLayerId.value!!
                        for (id in MoveState.selectedItemIds) {
                            val box = boxRepository.getById(id) ?: continue
                            boxRepository.update(box.copy(layerId = targetLayerId))
                        }
                    } else {
                        // 移动到指定设备（_selectedDeviceId != null）或第一层独立盒子（_selectedDeviceId == null）
                        val targetDeviceId = _selectedDeviceId.value
                        for (id in MoveState.selectedItemIds) {
                            treeTransformer.moveBoxToContainer(
                                boxId = id,
                                targetDeviceId = targetDeviceId,
                                targetLayerId = null
                            )
                        }
                    }
                }
                MoveTarget.BOX -> {
                    val targetBoxId = _selectedBoxId.value ?: return@launch
                    val positions = _selectedPositions.value.toList()
                    if (positions.size != MoveState.selectedItemIds.size) return@launch
                    val sampleIds = MoveState.selectedItemIds.toList()
                    for (i in positions.indices) {
                        if (i < sampleIds.size) {
                            val (row, col) = positions[i]
                            val sample = sampleRepository.getById(sampleIds[i]) ?: continue
                            sampleRepository.update(sample.copy(boxId = targetBoxId, row = row, col = col))
                        }
                    }
                }
            }
            _moveCompleted.value = true
            MoveState.clear()
            onCompleted()
        }
    }

    fun canConfirm(): Boolean {
        return when (MoveState.moveTarget) {
            MoveTarget.DEVICE -> _selectedDeviceId.value != null
            MoveTarget.LAYER, MoveTarget.CONTAINER -> true
            MoveTarget.BOX -> {
                if (MoveState.selectMode) _selectedBoxId.value != null
                else _selectedBoxId.value != null &&
                    _selectedPositions.value.size == MoveState.selectedItemIds.size &&
                    MoveState.selectedItemIds.isNotEmpty()
            }
        }
    }
}
