package com.labfreezer.ui.screens.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.db.entity.SamplePositionEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TagRepository
import com.labfreezer.export.ExportEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ExportFormat { CSV, MARKDOWN }

data class ExportResult(val uri: Uri, val mimeType: String)

@HiltViewModel
class ExportViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sampleRepository: SamplePositionRepository,
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val tagRepository: TagRepository,
    private val exportEngine: ExportEngine
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _result = MutableStateFlow<ExportResult?>(null)
    val result: StateFlow<ExportResult?> = _result

    fun exportSample(format: ExportFormat) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val allSamples = sampleRepository.getAllWithPath()
                val tagsMap = allSamples.associate { sample ->
                    val tagNames = tagRepository.getTagsBySampleId(sample.sampleId).joinToString(";") { it.name }
                    sample.sampleId to tagNames
                }
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "labfreezer_$ts"
                val file = when (format) {
                    ExportFormat.CSV -> exportEngine.exportCsv(allSamples, fileName, tagsMap)
                    ExportFormat.MARKDOWN -> exportEngine.exportMarkdown(allSamples, fileName, tagsMap)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val mime = when(format) {
                    ExportFormat.CSV -> "text/csv"
                    ExportFormat.MARKDOWN -> "application/zip"
                }
                _result.value = ExportResult(uri, mime)
            } catch (e: Exception) {
                Toast.makeText(context, "\u5bfc\u51fa\u5931\u8d25: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportDatabaseZip() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val file = exportEngine.exportDatabase()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                _result.value = ExportResult(uri, "application/zip")
            } catch (e: Exception) {
                Toast.makeText(context, "\u5bfc\u51fa\u5931\u8d25: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importDatabase(uri: Uri) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val success = exportEngine.importDatabase(uri)
                if (success) {
                    Toast.makeText(context, "\u5bfc\u5165\u6210\u529f\uff0c\u8bf7\u91cd\u542f\u5e94\u7528", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "\u5bfc\u5165\u5931\u8d25", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "\u5bfc\u5165\u5931\u8d25: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importMarkdown(uri: Uri) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    exportEngine.importMarkdown(uri)
                }
                Toast.makeText(context, "\u5bfc\u5165\u6210\u529f", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "\u5bfc\u5165\u5931\u8d25: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun importCsv(uri: Uri) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    importCsvInternal(uri)
                }
                Toast.makeText(context, "\u5bfc\u5165\u6210\u529f", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "\u5bfc\u5165\u5931\u8d25: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun importCsvInternal(uri: Uri) {
        data class Row(
            val deviceName: String, val layerName: String, val boxName: String,
            val posLabel: String, val sampleName: String?, val date: String?, val note: String?,
            val tags: String
        )

        val rows = mutableListOf<Row>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                val header = reader.readLine() ?: return
                var line = reader.readLine()
                while (line != null) {
                    val cols = line.split(",", limit = 8)
                    if (cols.size >= 5) {
                        rows.add(Row(
                            deviceName = cols.getOrElse(1) { "" }.trim(),
                            layerName = cols.getOrElse(2) { "" }.trim(),
                            boxName = cols.getOrElse(3) { "" }.trim(),
                            posLabel = cols.getOrElse(4) { "" }.trim(),
                            sampleName = cols.getOrElse(0) { "" }.trim().ifBlank { null },
                            date = cols.getOrElse(5) { "" }.trim().ifBlank { null },
                            note = cols.getOrElse(6) { "" }.trim().ifBlank { null },
                            tags = cols.getOrElse(7) { "" }.trim()
                        ))
                    }
                    line = reader.readLine()
                }
            }
        }

        // First pass: determine box dimensions from position data
        data class BoxKey(val device: String, val layer: String, val box: String)
        val boxPositions = mutableMapOf<BoxKey, MutableSet<Pair<Int, Int>>>()
        for (r in rows) {
            val pos = parsePosition(r.posLabel) ?: continue
            val key = BoxKey(r.deviceName, r.layerName, r.boxName)
            boxPositions.getOrPut(key) { mutableSetOf() }.add(pos)
        }

        // Second pass: find or create hierarchy, insert samples
        for (r in rows) {
            val pos = parsePosition(r.posLabel) ?: continue

            // Find or create device
            val allDevices = deviceRepository.getAll()
            var device = allDevices.find { it.name == r.deviceName }
            if (device == null) {
                val newId = deviceRepository.insert(StorageDeviceEntity(name = r.deviceName))
                device = deviceRepository.getById(newId) ?: continue
            }

            // Find or create layer
            val allLayers = layerRepository.getByDeviceId(device.id)
            var layer = allLayers.find { it.name == r.layerName }
            if (layer == null) {
                val newId = layerRepository.insert(StorageLayerEntity(deviceId = device.id, name = r.layerName))
                layer = layerRepository.getById(newId) ?: continue
            }

            // Find or create box with proper dimensions
            val allBoxes = boxRepository.getByLayerId(layer.id)
            var box = allBoxes.find { it.name == r.boxName }
            if (box == null) {
                val key = BoxKey(r.deviceName, r.layerName, r.boxName)
                val positions = boxPositions[key] ?: emptySet()
                val maxRow = (positions.maxOfOrNull { it.first } ?: pos.first) + 1
                val maxCol = (positions.maxOfOrNull { it.second } ?: pos.second) + 1
                val newId = boxRepository.insert(StorageBoxEntity(
                    layerId = layer.id, name = r.boxName,
                    rows = maxRow.coerceAtLeast(1), cols = maxCol.coerceAtLeast(1)
                ))
                box = boxRepository.getById(newId) ?: continue
            }

            // Check if sample already exists at this position (merge: skip if exists)
            val existing = sampleRepository.getByPosition(box.id, pos.first, pos.second)
            if (existing != null) continue

            // Insert new sample
            val sampleId = sampleRepository.insert(SamplePositionEntity(
                boxId = box.id, row = pos.first, col = pos.second,
                name = r.sampleName, date = r.date, note = r.note
            ))

            if (r.tags.isNotBlank()) {
                val tagNames = r.tags.split(";").map { it.trim() }.filter { it.isNotBlank() }
                val tagIds = tagNames.map { tagName ->
                    var tag = tagRepository.getByName(tagName)
                    if (tag == null) {
                        val newTagId = tagRepository.insert(com.labfreezer.data.db.entity.TagEntity(name = tagName, color = "#007AFF"))
                        tag = tagRepository.getById(newTagId)
                    }
                    tag!!.id
                }
                tagRepository.setSampleTags(sampleId, tagIds)
            }
        }
    }

    private fun parsePosition(label: String): Pair<Int, Int>? {
        if (label.length < 2) return null
        val rowChar = label[0]
        val colStr = label.substring(1)
        val row = rowChar.uppercaseChar() - 'A'
        val col = colStr.toIntOrNull()?.minus(1) ?: return null
        if (row < 0 || col < 0) return null
        return row to col
    }

    fun clearResult() { _result.value = null }
}
