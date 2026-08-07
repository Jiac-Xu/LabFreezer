package com.labfreezer.ui.screens.export

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.R
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TagRepository
import com.labfreezer.export.ExportEngine
import com.labfreezer.export.ImportEngine
import com.labfreezer.export.ImportSampleRow
import com.labfreezer.export.ZipAnalysis
import com.labfreezer.export.ZipInspector
import com.labfreezer.export.ZipType
import com.labfreezer.util.Csv
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
    private val exportEngine: ExportEngine,
    private val importEngine: ImportEngine,
    private val zipInspector: ZipInspector
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
                val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
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
                Toast.makeText(context, context.getString(R.string.export_failed, e.message), Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportDatabaseZip() {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { exportEngine.exportDatabase() }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                _result.value = ExportResult(uri, "application/zip")
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.export_failed, e.message), Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun exportDatabaseToUri(targetUri: Uri) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val file = withContext(Dispatchers.IO) { exportEngine.exportDatabase() }
                context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    file.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Toast.makeText(context, context.getString(R.string.export_success), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.export_failed, e.message), Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 用备份包整体替换数据库。
     * @return true 表示替换成功；失败返回 false（可由 UI 展示错误）。
     */
    suspend fun importDatabase(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching { importEngine.importDatabase(uri) }.getOrDefault(false)
    }

    fun inspectImportPackage(uri: Uri, onResult: (ZipAnalysis, Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val analysis = zipInspector.inspectImportPackage(uri)
            val currentCount = sampleRepository.countAll()
            withContext(Dispatchers.Main) {
                onResult(analysis, currentCount)
            }
        }
    }

    /**
     * 合并导入 Markdown 导出包。
     * @return true 表示导入流程完成（无论合并了几条数据）；失败返回 false。
     */
    suspend fun importMarkdown(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching { importEngine.importMarkdown(uri) }.getOrDefault(false)
    }

    fun importCsv(uri: Uri) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    importCsvInternal(uri)
                }
                Toast.makeText(context, context.getString(R.string.import_success), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, context.getString(R.string.import_operation_failed), Toast.LENGTH_LONG).show()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun importCsvInternal(uri: Uri) {
        val rows = mutableListOf<ImportSampleRow>()

        context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                reader.readLine() // header
                var line = reader.readLine()
                while (line != null) {
                    // 使用标准 CSV 解码（支持引号包裹/转义），替换原先的朴素 split(",")
                    val cols = Csv.decodeLine(line)
                    if (cols.size >= 5) {
                        rows.add(ImportSampleRow(
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

        // 解析完成后交给共享的 importSamples：find-or-create 层级、跳过已存在样本（合并模式）
        importEngine.importSamples(rows, overwriteExisting = false)
    }

    fun clearResult() { _result.value = null }
}
