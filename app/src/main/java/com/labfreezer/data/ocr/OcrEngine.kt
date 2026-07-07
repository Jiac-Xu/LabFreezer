package com.labfreezer.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.equationl.paddleocr4android.CpuPowerMode
import com.equationl.paddleocr4android.OCR
import com.equationl.paddleocr4android.OcrConfig
import com.equationl.paddleocr4android.bean.OcrResult
import com.equationl.paddleocr4android.callback.OcrInitCallback
import com.equationl.paddleocr4android.callback.OcrRunCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class OcrParseResult(
    val name: String = "",
    val date: String = ""
)

@Singleton
class OcrEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "OcrEngine"
        private const val MODEL_PATH = "models/ch_PP-OCRv4"
        private const val LABEL_PATH = "labels/ppocr_keys_v1.txt"
        private const val DET_MODEL = "det.nb"
        private const val REC_MODEL = "rec.nb"
        private const val CLS_MODEL = "cls.nb"

        private val DATE_PATTERNS = listOf(
            Regex("""\d{4}[-/.]\d{1,2}[-/.]\d{1,2}"""),
            Regex("""\d{1,2}[-/.]\d{1,2}[-/.]\d{4}"""),
            Regex("""\d{4}年\d{1,2}月\d{1,2}日""")
        )
    }

    private var ocr: OCR? = null
    private var initialized = false

    suspend fun ensureModelAndInit() {
        if (initialized) return
        withContext(Dispatchers.IO) {
            if (initialized) return@withContext
            val ocrInstance = OCR(context)
            val config = OcrConfig()
            config.modelPath = MODEL_PATH
            config.labelPath = LABEL_PATH
            config.detModelFilename = DET_MODEL
            config.recModelFilename = REC_MODEL
            config.clsModelFilename = CLS_MODEL
            config.isRunDet = true
            config.isRunCls = true
            config.isRunRec = true
            config.cpuPowerMode = CpuPowerMode.LITE_POWER_FULL
            config.isDrwwTextPositionBox = false

            suspendCancellableCoroutine<Unit> { cont ->
                ocrInstance.initModel(config, object : OcrInitCallback {
                    override fun onSuccess() {
                        ocr = ocrInstance
                        initialized = true
                        Log.i(TAG, "OCR initialized successfully")
                        cont.resume(Unit)
                    }
                    override fun onFail(e: Throwable) {
                        Log.e(TAG, "OCR init failed: ${e.message}")
                        cont.resume(Unit)
                    }
                })
            }
        }
    }

    suspend fun isAvailable(): Boolean {
        ensureModelAndInit()
        return initialized
    }

    suspend fun recognize(bitmap: Bitmap): OcrResult? {
        if (!isAvailable()) return null
        val ocr = ocr ?: return null
        return suspendCancellableCoroutine { cont ->
            ocr.run(bitmap, object : OcrRunCallback {
                override fun onSuccess(result: OcrResult) {
                    cont.resume(result)
                }
                override fun onFail(e: Throwable) {
                    cont.resume(null)
                }
            })
        }
    }

    fun parseResult(text: String): OcrParseResult {
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }
        var name = ""
        var date = ""
        for (line in lines) {
            var matched = false
            for (pattern in DATE_PATTERNS) {
                val match = pattern.find(line)
                if (match != null) {
                    date = match.value
                    val rest = line.replace(match.value, "").trim(' ', '-', '/', '.', ':', '|')
                    if (rest.isNotBlank() && name.isBlank()) {
                        name = rest
                    }
                    matched = true
                    break
                }
            }
            if (!matched && name.isBlank()) {
                name = line
            }
        }
        return OcrParseResult(name = name, date = date)
    }

    fun release() {
        ocr?.releaseModel()
        ocr = null
        initialized = false
    }
}
