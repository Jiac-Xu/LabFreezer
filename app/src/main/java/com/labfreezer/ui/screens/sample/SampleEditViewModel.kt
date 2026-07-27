package com.labfreezer.ui.screens.sample
import com.labfreezer.R

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.SamplePositionEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.data.file.PhotoManager
import com.labfreezer.data.ocr.OcrEngine
import com.labfreezer.data.ocr.OcrPreferences
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class SampleEditState(
    val sample: SamplePositionEntity? = null,
    val name: String = "",
    val date: String = "",
    val note: String = "",
    val photoPath: String? = null,
    val photoVersion: Int = 0,
    val deleted: Boolean = false,
    val assignedTagIds: Set<Long> = emptySet(),
    val deviceName: String = "",
    val layerName: String = "",
    val boxName: String = "",
    val ocrEnabled: Boolean = true,
    /** 当前在浏览上下文中的位置索引（0-based），-1 表示无上下文 */
    val currentIndex: Int = -1,
    /** 浏览上下文中的总样本数，0 表示无上下文 */
    val totalCount: Int = 0
)

/** 导航事件：携带下一个样本 ID 和浏览上下文 key */
data class SampleEditNavigation(
    val sampleId: Long,
    val browseCtxKey: String?
)

@HiltViewModel
class SampleEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val sampleRepository: SamplePositionRepository,
    private val tagRepository: TagRepository,
    private val photoManager: PhotoManager,
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val ocrEngine: OcrEngine,
    private val ocrPreferences: OcrPreferences
) : ViewModel() {

    private val sampleId: Long = savedStateHandle["sampleId"] ?: -1L

    /** 浏览上下文 key，从导航参数获取 */
    private val browseCtxKey: String? = savedStateHandle.get<String>("browseCtx")?.takeIf { it.isNotBlank() }

    /** 浏览上下文，从 store 读取 */
    val browseContext: SampleBrowseContext? = browseCtxKey?.let { BrowseContextStore.get(it) }

    private val _state = MutableStateFlow(SampleEditState())
    val state: StateFlow<SampleEditState> = _state

    val allTags: StateFlow<List<TagEntity>> = tagRepository.getAllFlow()
        .let { flow ->
            val ms = MutableStateFlow<List<TagEntity>>(emptyList())
            viewModelScope.launch { flow.collect { ms.value = it } }
            ms
        }

    private val _allDevices = MutableStateFlow<List<StorageDeviceEntity>>(emptyList())
    val allDevices: StateFlow<List<StorageDeviceEntity>> = _allDevices

    private val _layersByDevice = MutableStateFlow<Map<Long, List<StorageLayerEntity>>>(emptyMap())
    val layersByDevice: StateFlow<Map<Long, List<StorageLayerEntity>>> = _layersByDevice

    private val _boxesByLayer = MutableStateFlow<Map<Long, List<StorageBoxEntity>>>(emptyMap())
    val boxesByLayer: StateFlow<Map<Long, List<StorageBoxEntity>>> = _boxesByLayer

    private val _occupiedPositions = MutableStateFlow<Map<Long, Set<Pair<Int, Int>>>>(emptyMap())
    val occupiedPositions: StateFlow<Map<Long, Set<Pair<Int, Int>>>> = _occupiedPositions

    private var currentPhotoUri: Uri? = null
    private var currentSample: SamplePositionEntity? = null

    private var pendingBoxId: Long? = null
    private var pendingRow: Int? = null
    private var pendingCol: Int? = null

    init { loadSample() }

    private fun loadSample() {
        viewModelScope.launch {
            val sample = sampleRepository.getById(sampleId) ?: return@launch
            currentSample = sample
            val tags = tagRepository.getTagsBySampleId(sampleId)
            val box = boxRepository.getById(sample.boxId)
            val layer = if (box != null) layerRepository.getById(box.layerId) else null
            val device = if (layer != null) deviceRepository.getById(layer.deviceId) else null

            // 计算浏览上下文中的位置
            val ctx = browseContext
            val idx = ctx?.sampleIds?.indexOf(sampleId) ?: -1
            val total = ctx?.sampleIds?.size ?: 0

            _state.value = SampleEditState(
                sample = sample,
                name = sample.name ?: "",
                date = sample.date ?: "",
                note = sample.note ?: "",
                photoPath = sample.photoPath,
                ocrEnabled = ocrPreferences.isEnabled(),
                assignedTagIds = tags.map { it.id }.toSet(),
                deviceName = device?.name ?: "",
                layerName = layer?.name ?: "",
                boxName = box?.name ?: "",
                currentIndex = idx,
                totalCount = total
            )
            _allDevices.value = deviceRepository.getAll()
        }
    }

    private val _navigationEvent = MutableSharedFlow<SampleEditNavigation>()
    val navigationEvent: SharedFlow<SampleEditNavigation> = _navigationEvent.asSharedFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    /**
     * 导航到相邻样本。
     * 优先使用浏览上下文（browseContext）的 sampleIds 列表，
     * 回退到按盒子查询数据库的旧逻辑。
     */
    fun navigateAdjacent(isNext: Boolean) {
        val ctx = browseContext
        if (ctx != null) {
            // 使用浏览上下文导航
            val currentIndex = ctx.sampleIds.indexOf(sampleId)
            if (currentIndex == -1) return
            val targetIndex = if (isNext) currentIndex + 1 else currentIndex - 1
            if (targetIndex in ctx.sampleIds.indices) {
                viewModelScope.launch {
                    _navigationEvent.emit(SampleEditNavigation(ctx.sampleIds[targetIndex], browseCtxKey))
                }
            } else {
                val msg = if (isNext) context.getString(R.string.sample_edit_last_sample)
                          else context.getString(R.string.sample_edit_first_sample)
                viewModelScope.launch { _toastEvent.emit(msg) }
            }
        } else {
            // 回退到旧逻辑：按盒子查询数据库
            viewModelScope.launch(Dispatchers.IO) {
                val current = currentSample ?: return@launch
                val allSamples = sampleRepository.getByBoxId(current.boxId)
                    .sortedWith(compareBy({ it.row }, { it.col }))
                val currentIndex = allSamples.indexOfFirst { it.id == current.id }
                if (currentIndex != -1) {
                    val targetIndex = if (isNext) currentIndex + 1 else currentIndex - 1
                    if (targetIndex in allSamples.indices) {
                        _navigationEvent.emit(SampleEditNavigation(allSamples[targetIndex].id, null))
                    } else {
                        val msg = if (isNext) context.getString(R.string.sample_edit_last_sample)
                                  else context.getString(R.string.sample_edit_first_sample)
                        _toastEvent.emit(msg)
                    }
                }
            }
        }
    }

    fun loadLayers(deviceId: Long) {
        viewModelScope.launch {
            val layers = layerRepository.getByDeviceId(deviceId)
            _layersByDevice.value = _layersByDevice.value + (deviceId to layers)
        }
    }

    fun loadBoxes(layerId: Long) {
        viewModelScope.launch {
            val boxes = boxRepository.getByLayerId(layerId)
            _boxesByLayer.value = _boxesByLayer.value + (layerId to boxes)
        }
    }

    fun loadOccupied(boxId: Long) {
        viewModelScope.launch {
            val samples = sampleRepository.getByBoxId(boxId)
            _occupiedPositions.value = _occupiedPositions.value + (boxId to samples.map { it.row to it.col }.toSet())
        }
    }

    fun setLocation(boxId: Long, row: Int, col: Int) {
        pendingBoxId = boxId
        pendingRow = row
        pendingCol = col
        viewModelScope.launch {
            val box = boxRepository.getById(boxId) ?: return@launch
            val layer = layerRepository.getById(box.layerId) ?: return@launch
            val device = deviceRepository.getById(layer.deviceId) ?: return@launch
            _state.update {
                it.copy(
                    deviceName = device.name,
                    layerName = layer.name,
                    boxName = box.name
                )
            }
        }
    }

    fun updateName(name: String) { _state.update { it.copy(name = name) } }
    fun updateDate(date: String) { _state.update { it.copy(date = date) } }
    fun updateNote(note: String) { _state.update { it.copy(note = note) } }

    fun toggleTag(tagId: Long) {
        _state.update { st ->
            val newIds = if (tagId in st.assignedTagIds) st.assignedTagIds - tagId else st.assignedTagIds + tagId
            st.copy(assignedTagIds = newIds)
        }
    }

    fun save() {
        val sample = _state.value.sample ?: return
        viewModelScope.launch {
            val oldPhotoPath = sample.photoPath
            val newPhotoPath = _state.value.photoPath
            if (oldPhotoPath != null && oldPhotoPath != newPhotoPath) {
                photoManager.deletePhoto(oldPhotoPath)
            }
            val updatedSample = sample.copy(
                name = _state.value.name.ifBlank { null },
                date = _state.value.date.ifBlank { null },
                note = _state.value.note.ifBlank { null },
                photoPath = _state.value.photoPath,
                boxId = pendingBoxId ?: sample.boxId,
                row = pendingRow ?: sample.row,
                col = pendingCol ?: sample.col,
                updatedAt = System.currentTimeMillis()
            )
            sampleRepository.update(updatedSample)
            tagRepository.setSampleTags(sampleId, _state.value.assignedTagIds.toList())
            _toastEvent.emit(context.getString(R.string.sample_edit_saved))
        }
    }

    fun delete() {
        val sample = _state.value.sample ?: return
        viewModelScope.launch {
            photoManager.deletePhoto(sample.photoPath)
            sampleRepository.delete(sample)
            _state.update { it.copy(deleted = true) }
        }
    }

    fun onCameraResult(success: Boolean) {
        if (success) {
            val sample = currentSample
            if (sample != null && currentPhotoUri != null) {
                viewModelScope.launch {
                    val compressedPath = photoManager.compressAndSave(currentPhotoUri!!, sample.boxId, sample.row, sample.col)
                    currentPhotoUri = null
                    _state.update { it.copy(photoPath = compressedPath) }
                    // Run auto-OCR if enabled
                    if (ocrPreferences.isAutoOcrEnabled()) {
                        runOcrAndUpdate(sample.copy(photoPath = compressedPath))
                    }
                    // Apply auto-date after taking photo
                    if (ocrPreferences.isAutoDateEnabled()) {
                        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                        _state.update { it.copy(date = today) }
                    }
                }
            } else {
                currentPhotoUri = null
            }
        } else {
            currentPhotoUri = null
        }
    }

    private suspend fun runOcrAndUpdate(sample: SamplePositionEntity) {
        if (!ocrPreferences.isEnabled()) return
        val photoPath = sample.photoPath ?: return
        try {
            val bitmap = withContext(Dispatchers.IO) {
                val file = File(android.net.Uri.parse(photoPath).path ?: return@withContext null)
                if (file.exists()) android.graphics.BitmapFactory.decodeFile(file.absolutePath) else null
            } ?: return
            val result = ocrEngine.recognize(bitmap) ?: return
            val parsed = ocrEngine.parseResult(result.simpleText)
            val name = parsed.name.ifBlank { null }
            val date = parsed.date.ifBlank { null }
            val ocrNote = if (result.simpleText.isNotBlank()) "【OCR】${result.simpleText}" else null
            val note = listOfNotNull(_state.value.note.takeIf { it.isNotBlank() }, ocrNote).ifEmpty { null }?.joinToString("\n")
            if (name != null || date != null || ocrNote != null) {
                _state.update { it.copy(
                    name = name ?: it.name,
                    date = date ?: it.date,
                    note = note ?: it.note
                ) }
            }
        } catch (_: Exception) { }
    }

    fun runOcrNow() {
        val sample = _state.value.sample ?: return
        if (sample.photoPath == null) return
        viewModelScope.launch {
            runOcrAndUpdate(sample)
        }
    }

    fun createPhotoUri(): Uri {
        val sample = _state.value.sample ?: throw IllegalStateException("No sample loaded")
        val uri = photoManager.createPhotoUri(sample.boxId, sample.row, sample.col)
        currentPhotoUri = uri
        return uri
    }

    fun rotatePhoto() {
        val path = _state.value.photoPath ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = photoManager.rotatePhoto(path)
            if (result != null) {
                _state.update { it.copy(photoVersion = it.photoVersion + 1) }
                _toastEvent.emit(context.getString(R.string.sample_edit_photo_rotated))
            }
        }
    }

    companion object {
        fun positionToLabel(row: Int, col: Int): String = "${'A' + row}${col + 1}"
    }
}