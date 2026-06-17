package com.labfreezer.ui.screens.boxgrid

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.SamplePositionEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.file.PhotoManager
import com.labfreezer.data.ocr.OcrEngine
import com.labfreezer.data.ocr.OcrPreferences
import com.labfreezer.data.repository.RecentlyViewedRepository
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class GridCell(
    val row: Int,
    val col: Int,
    val label: String,
    val status: GridCellStatus = GridCellStatus.EMPTY,
    val sampleId: Long? = null,
    val photoPath: String? = null,
    val sampleName: String? = null
)

@Immutable
data class GridCellsState(val list: List<GridCell> = emptyList())

enum class GridCellStatus {
    EMPTY,
    PHOTO_ONLY,
    COMPLETE
}

@HiltViewModel
class BoxGridViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val boxRepository: StorageBoxRepository,
    private val sampleRepository: SamplePositionRepository,
    private val photoManager: PhotoManager,
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val ocrEngine: OcrEngine,
    private val ocrPreferences: OcrPreferences,
    private val recentBoxRepo: RecentlyViewedRepository,
) : ViewModel() {

    private val _box = MutableStateFlow<StorageBoxEntity?>(null)
    val box: StateFlow<StorageBoxEntity?> = _box

    private val _cells = MutableStateFlow(GridCellsState())
    val cells: StateFlow<GridCellsState> = _cells

    private val _pendingSampleId = MutableStateFlow<Long?>(null)
    val pendingSampleId: StateFlow<Long?> = _pendingSampleId

    private val _isSelecting = MutableStateFlow(false)
    val isSelecting: StateFlow<Boolean> = _isSelecting

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds

    private val _allDevices = MutableStateFlow<List<StorageDeviceEntity>>(emptyList())
    val allDevices: StateFlow<List<StorageDeviceEntity>> = _allDevices

    private val _layersByDevice = MutableStateFlow<Map<Long, List<StorageLayerEntity>>>(emptyMap())
    val layersByDevice: StateFlow<Map<Long, List<StorageLayerEntity>>> = _layersByDevice

    private val _boxesByLayer = MutableStateFlow<Map<Long, List<StorageBoxEntity>>>(emptyMap())
    val boxesByLayer: StateFlow<Map<Long, List<StorageBoxEntity>>> = _boxesByLayer

    private var pendingRow = 0
    private var pendingCol = 0
    private var currentPhotoUri: Uri? = null

    fun loadBox(boxId: Long) {
        viewModelScope.launch {
            val b = withContext(Dispatchers.IO) { boxRepository.getById(boxId) }
            _box.value = b
            if (b != null) {
                withContext(Dispatchers.IO) {
                    val layer = layerRepository.getById(b.layerId)
                    val device = if (layer != null) deviceRepository.getById(layer.deviceId) else null
                    recentBoxRepo.addBox(b.id, b.name, device?.name, layer?.name)
                }
                refreshGrid(b)

                launch(Dispatchers.IO) {
                    kotlinx.coroutines.delay(600)
                    preloadHierarchyData()
                }
            }
        }
    }

    private suspend fun preloadHierarchyData() {
        val devices = deviceRepository.getAll()
        _allDevices.value = devices

        val allLayers = mutableMapOf<Long, MutableList<StorageLayerEntity>>()
        val allBoxes = mutableMapOf<Long, MutableList<StorageBoxEntity>>()

        for (device in devices) {
            val layers = layerRepository.getByDeviceId(device.id)
            allLayers[device.id] = layers.toMutableList()

            for (layer in layers) {
                allBoxes[layer.id] = boxRepository.getByLayerId(layer.id).toMutableList()
            }
        }

        _layersByDevice.value = allLayers
        _boxesByLayer.value = allBoxes
    }

    private suspend fun refreshGrid(box: StorageBoxEntity) {
        val grid = withContext(Dispatchers.IO) {
            val samples = sampleRepository.getByBoxId(box.id)
            val sampleMap = samples.associateBy { it.row * 1000 + it.col }
            val tempGrid = mutableListOf<GridCell>()

            for (r in 0 until box.rows) {
                for (c in 0 until box.cols) {
                    val label = positionToLabel(r, c)
                    val sample = sampleMap[r * 1000 + c]
                    tempGrid.add(
                        GridCell(
                            row = r, col = c, label = label,
                            status = when {
                                sample == null -> GridCellStatus.EMPTY
                                sample.name.isNullOrBlank() -> GridCellStatus.PHOTO_ONLY
                                else -> GridCellStatus.COMPLETE
                            },
                            sampleId = sample?.id, photoPath = sample?.photoPath, sampleName = sample?.name
                        )
                    )
                }
            }
            tempGrid
        }

        _cells.value = GridCellsState(grid)
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
        val sampleIds = _cells.value.list.filter { it.sampleId != null }.mapNotNull { it.sampleId }.toSet()
        if (_selectedIds.value == sampleIds) {
            _selectedIds.value = emptySet()
            _isSelecting.value = false
        } else {
            _selectedIds.value = sampleIds
        }
    }

    fun exitSelection() {
        _selectedIds.value = emptySet()
        _isSelecting.value = false
    }

    fun deleteSelected() {
        viewModelScope.launch {
            _selectedIds.value.forEach { id ->
                val sample = sampleRepository.getById(id)
                sample?.let { photoManager.deletePhoto(it.photoPath) }
                sampleRepository.deleteById(id)
            }
            exitSelection()
            _box.value?.let { refreshGrid(it) }
        }
    }

    fun moveSelected(targetBoxId: Long) {
        viewModelScope.launch {
            val targetBox = boxRepository.getById(targetBoxId) ?: return@launch
            val existing = sampleRepository.getByBoxId(targetBoxId)
            val currentBoxId = _box.value?.id
            val occupied = existing
                .filter { !(targetBoxId == currentBoxId && it.id in _selectedIds.value) }
                .map { it.row to it.col }.toMutableSet()
            val emptyCells = mutableListOf<Pair<Int, Int>>()
            for (r in 0 until targetBox.rows) {
                for (c in 0 until targetBox.cols) {
                    if ((r to c) !in occupied) emptyCells.add(r to c)
                }
            }
            var cellIndex = 0
            _selectedIds.value.forEach { id ->
                val sample = sampleRepository.getById(id) ?: return@forEach
                if (cellIndex < emptyCells.size) {
                    val (newRow, newCol) = emptyCells[cellIndex]
                    sampleRepository.update(sample.copy(boxId = targetBoxId, row = newRow, col = newCol))
                    occupied.add(newRow to newCol)
                    cellIndex++
                }
            }
            exitSelection()
            _box.value?.let { refreshGrid(it) }
        }
    }

    fun onCellClick(cell: GridCell) {
        val box = _box.value ?: return
        viewModelScope.launch {
            if (cell.status == GridCellStatus.EMPTY) {
                pendingRow = cell.row
                pendingCol = cell.col
                val id = sampleRepository.insert(
                    SamplePositionEntity(boxId = box.id, row = cell.row, col = cell.col)
                )
                _pendingSampleId.value = id
            }
        }
    }

    fun onCameraResult(success: Boolean) {
        val sampleId = _pendingSampleId.value ?: return
        val box = _box.value
        _pendingSampleId.value = null

        if (success && box != null && currentPhotoUri != null) {
            viewModelScope.launch {
                val compressedPath = photoManager.compressAndSave(currentPhotoUri!!, box.id, pendingRow, pendingCol)
                currentPhotoUri = null
                val sample = sampleRepository.getById(sampleId) ?: return@launch
                var updatedSample = sample.copy(photoPath = compressedPath)
                sampleRepository.update(updatedSample)
                refreshGrid(box)
                runOcrAndUpdate(updatedSample, compressedPath)
            }
        } else {
            currentPhotoUri = null
            viewModelScope.launch {
                sampleRepository.deleteById(sampleId)
            }
        }
    }

    private suspend fun runOcrAndUpdate(sample: SamplePositionEntity, photoPath: String?) {
        if (!ocrPreferences.isEnabled() || photoPath == null) return
        try {
            val bitmap = withContext(Dispatchers.IO) {
                val file = File(Uri.parse(photoPath).path ?: return@withContext null)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            } ?: return
            val result = ocrEngine.recognize(bitmap) ?: return
            val parsed = ocrEngine.parseResult(result.simpleText)
            val name = parsed.name.ifBlank { null }
            val date = parsed.date.ifBlank { null }
            val ocrNote = if (result.simpleText.isNotBlank()) "\u3010OCR\u3011${result.simpleText}" else null
            val note = listOfNotNull(sample.note?.takeIf { it.isNotBlank() }, ocrNote).ifEmpty { null }?.joinToString("\n")
            if (name != null || date != null || ocrNote != null) {
                sampleRepository.update(sample.copy(
                    name = name ?: sample.name,
                    date = date ?: sample.date,
                    note = note
                ))
                _box.value?.let { refreshGrid(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OCR failed: ${e.message}")
        }
    }

    fun createPhotoUri(): Uri {
        val box = _box.value ?: throw IllegalStateException("No box loaded")
        val uri = photoManager.createPhotoUri(box.id, pendingRow, pendingCol)
        currentPhotoUri = uri
        return uri
    }

    companion object {
        private const val TAG = "BoxGridViewModel"
        fun positionToLabel(row: Int, col: Int): String = "${'A' + row}${col + 1}"
    }
}
