package com.labfreezer.ui.screens.settings

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.labfreezer.data.ocr.OcrEngine
import com.labfreezer.data.ocr.OcrPreferences
import com.labfreezer.data.repository.SamplePositionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

data class ModelInfo(
    val name: String,
    val sizeBytes: Long
)

data class OcrSettingsState(
    val ocrEnabled: Boolean = true,
    val models: List<ModelInfo> = emptyList(),
    val isBatchRunning: Boolean = false,
    val batchProgress: Int = 0,
    val batchTotal: Int = 0,
    val batchCurrentName: String = "",
    val batchResult: String? = null
)

@HiltViewModel
class OcrSettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ocrEngine: OcrEngine,
    private val sampleRepository: SamplePositionRepository,
    private val ocrPreferences: OcrPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(OcrSettingsState(ocrEnabled = ocrPreferences.isEnabled()))
    val state: StateFlow<OcrSettingsState> = _state.asStateFlow()

    init {
        loadModelSizes()
    }

    private fun loadModelSizes() {
        val models = listOf("det.nb", "rec.nb", "cls.nb")
        val infos = models.mapNotNull { name ->
            try {
                val fd = context.assets.openFd("models/ch_PP-OCRv4/$name")
                ModelInfo(name = name, sizeBytes = fd.length)
            } catch (_: Exception) {
                null
            }
        }
        _state.value = _state.value.copy(models = infos)
    }

    fun setOcrEnabled(enabled: Boolean) {
        ocrPreferences.setEnabled(enabled)
        _state.value = _state.value.copy(ocrEnabled = enabled)
        if (!enabled) {
            ocrEngine.release()
        }
    }

    fun runBatchOcr() {
        if (_state.value.isBatchRunning) return
        _state.value = _state.value.copy(isBatchRunning = true, batchProgress = 0, batchTotal = 0, batchCurrentName = "", batchResult = null)

        viewModelScope.launch {
            try {
                val samples = withContext(Dispatchers.IO) {
                    sampleRepository.getWithPhotoButEmptyNameAndNote()
                }
                if (samples.isEmpty()) {
                    _state.value = _state.value.copy(
                        isBatchRunning = false,
                        batchResult = "没有找到需要 OCR 的样品"
                    )
                    return@launch
                }
                _state.value = _state.value.copy(batchTotal = samples.size)

                var successCount = 0
                for ((index, sample) in samples.withIndex()) {
                    if (!_state.value.ocrEnabled) break

                    _state.value = _state.value.copy(
                        batchProgress = index + 1,
                        batchCurrentName = sample.name ?: "样品 #${sample.id}"
                    )

                    val photoPath = sample.photoPath ?: continue
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            val file = File(Uri.parse(photoPath).path ?: return@withContext null)
                            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                        } ?: continue

                        val result = ocrEngine.recognize(bitmap) ?: continue
                        val parsed = ocrEngine.parseResult(result.simpleText)

                        val name = parsed.name.ifBlank { null }
                        val date = parsed.date.ifBlank { null }
                        val ocrNote = if (result.simpleText.isNotBlank()) "\u3010OCR\u3011${result.simpleText}" else null
                        val note = listOfNotNull(sample.note?.takeIf { it.isNotBlank() }, ocrNote).ifEmpty { null }?.joinToString("\n")
                        if (name != null || date != null || ocrNote != null) {
                            withContext(Dispatchers.IO) {
                                sampleRepository.update(
                                    sample.copy(
                                        name = name ?: sample.name,
                                        date = date ?: sample.date,
                                        note = note
                                    )
                                )
                            }
                            successCount++
                        }
                    } catch (_: Exception) { }
                }

                _state.value = _state.value.copy(
                    isBatchRunning = false,
                    batchResult = "完成：$successCount/${samples.size} 个样品已更新"
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(
                    isBatchRunning = false,
                    batchResult = "执行出错"
                )
            }
        }
    }

    fun clearResult() {
        _state.value = _state.value.copy(batchResult = null)
    }

    override fun onCleared() {
        super.onCleared()
        ocrEngine.release()
    }
}
