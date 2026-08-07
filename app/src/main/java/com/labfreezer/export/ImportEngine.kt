package com.labfreezer.export

import android.content.Context
import android.net.Uri
import com.labfreezer.data.db.AppDatabase
import com.labfreezer.data.db.HIDDEN_MARKER
import com.labfreezer.data.db.entity.SamplePositionEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.db.entity.TagEntity
import com.labfreezer.data.model.Position
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TagRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/** 导入中的一行样本数据（Markdown / CSV 共用）。 */
data class ImportSampleRow(
    val deviceName: String,
    val layerName: String,
    val boxName: String,
    val posLabel: String,
    val sampleName: String?,
    val date: String?,
    val note: String?,
    val tags: String,
    val imageFile: String? = null
)

@Singleton
class ImportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val sampleRepository: SamplePositionRepository,
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val tagRepository: TagRepository
) {
    /**
     * 用备份包整体替换数据库。
     *
     * 安全措施：
     * 1. 覆盖前先 [AppDatabase.close] 关闭活跃连接，避免向正在使用的 SQLite 文件写入造成损坏；
     * 2. 备份包里没有 -wal/-shm 时，删除残留的旧 WAL，防止未 checkpoint 的旧状态污染新库。
     */
    fun importDatabase(uri: Uri): Boolean {
        return try {
            // 先关闭数据库连接（close 时 Room 会 checkpoint 并移除现有 WAL）
            appDatabase.close()

            var sawWal = false
            var sawShm = false
            val input = context.contentResolver.openInputStream(uri) ?: return false
            input.use { stream ->
                ZipInputStream(stream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            name == "labfreezer.db" -> {
                                val dbFile = context.getDatabasePath("labfreezer.db")
                                dbFile.parentFile?.mkdirs()
                                FileOutputStream(dbFile).use { zis.copyTo(it) }
                            }
                            name == "labfreezer.db-wal" -> {
                                sawWal = true
                                val f = File(context.getDatabasePath("labfreezer.db").path + "-wal")
                                FileOutputStream(f).use { zis.copyTo(it) }
                            }
                            name == "labfreezer.db-shm" -> {
                                sawShm = true
                                val f = File(context.getDatabasePath("labfreezer.db").path + "-shm")
                                FileOutputStream(f).use { zis.copyTo(it) }
                            }
                            name.startsWith("photos/") -> {
                                val photo = File(context.filesDir, name)
                                photo.parentFile?.mkdirs()
                                FileOutputStream(photo).use { zis.copyTo(it) }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            // 备份包里若没有 WAL/SHM，则删除可能残留的旧文件，防止旧状态污染新库
            val dbFile = context.getDatabasePath("labfreezer.db")
            if (!sawWal) File(dbFile.path + "-wal").delete()
            if (!sawShm) File(dbFile.path + "-shm").delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 批量导入样本（Markdown / CSV 共用）。
     *
     * 按「设备/层/盒」名称查找已有实体，不存在则创建（盒子行列按数据推算）。
     *
     * @param overwriteExisting true 时同名位置样本会被更新（MD 导入）；
     *                          false 时跳过已存在位置（CSV 合并）。
     * @param photoResolver     可选：导入照片并返回路径的钩子（MD 导入使用）。
     * @return 实际写入的样本条数。
     */
    suspend fun importSamples(
        rows: List<ImportSampleRow>,
        overwriteExisting: Boolean,
        photoResolver: (suspend (row: ImportSampleRow, box: StorageBoxEntity, pos: Pair<Int, Int>) -> String?)? = null
    ): Int {
        data class BoxKey(val device: String, val layer: String, val box: String)
        val boxPositions = mutableMapOf<BoxKey, MutableSet<Pair<Int, Int>>>()
        for (r in rows) {
            val pos = Position.parse(r.posLabel) ?: continue
            boxPositions.getOrPut(BoxKey(r.deviceName, r.layerName, r.boxName)) { mutableSetOf() }.add(pos)
        }

        var imported = 0
        for (r in rows) {
            val pos = Position.parse(r.posLabel) ?: continue

            // 设备名为空 → 使用 hidden 占位
            val deviceName = r.deviceName.ifBlank { HIDDEN_MARKER }
            var device = if (deviceName == HIDDEN_MARKER) {
                deviceRepository.getOrCreateHiddenDevice()
            } else {
                val allDevices = deviceRepository.getAll() + deviceRepository.getAllHidden()
                var dev = allDevices.find { it.name == deviceName }
                if (dev == null) {
                    val newId = deviceRepository.insert(StorageDeviceEntity(name = deviceName))
                    dev = deviceRepository.getById(newId)
                }
                dev
            } ?: continue

            // 层名为创建按使用 hidden 占位
            val layerName = r.layerName.ifBlank { HIDDEN_MARKER }
            var layer = if (layerName == HIDDEN_MARKER) {
                layerRepository.getOrCreateHiddenLayer(device.id)
            } else {
                val allLayers = layerRepository.getByDeviceIdAll(device.id)
                var lay = allLayers.find { it.name == layerName }
                if (lay == null) {
                    val newId = layerRepository.insert(StorageLayerEntity(deviceId = device.id, name = layerName))
                    lay = layerRepository.getById(newId)
                }
                lay
            } ?: continue

            var box = boxRepository.getByLayerId(layer.id).find { it.name == r.boxName }
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

            val photoPath = photoResolver?.invoke(r, box, pos)
            val existing = sampleRepository.getByPosition(box.id, pos.first, pos.second)

            val sampleId = if (existing != null) {
                if (!overwriteExisting) continue
                sampleRepository.update(existing.copy(
                    name = r.sampleName ?: existing.name,
                    date = r.date ?: existing.date,
                    note = r.note ?: existing.note,
                    photoPath = photoPath ?: existing.photoPath
                ))
                existing.id
            } else {
                sampleRepository.insert(SamplePositionEntity(
                    boxId = box.id, row = pos.first, col = pos.second,
                    name = r.sampleName, date = r.date, note = r.note,
                    photoPath = photoPath
                ))
            }

            if (r.tags.isNotBlank()) {
                val tagNames = r.tags.split(";").map { it.trim() }.filter { it.isNotBlank() }
                val tagIds = tagNames.map { tagName ->
                    var tag = tagRepository.getByName(tagName)
                    if (tag == null) {
                        val newTagId = tagRepository.insert(TagEntity(name = tagName, color = "#007AFF"))
                        tag = tagRepository.getById(newTagId)
                    }
                    tag!!.id
                }
                tagRepository.setSampleTags(sampleId, tagIds)
            }
            imported++
        }
        return imported
    }

    /**
     * 合并导入 Markdown 导出包。
     * @return true 表示至少导入了一行有效数据；zip 无效或无可用行时返回 false。
     */
    suspend fun importMarkdown(uri: Uri): Boolean {
        return try {
            var imported = 0
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val tempDir = File(context.cacheDir, "md_import_${System.currentTimeMillis()}")
                tempDir.mkdirs()
                val imagesDir = File(tempDir, "images")
                imagesDir.mkdirs()
                var mdContent: String? = null

                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            name.endsWith(".md") -> {
                                mdContent = zis.readBytes().toString(Charsets.UTF_8)
                            }
                            name.startsWith("images/") -> {
                                val imgFile = File(imagesDir, name.removePrefix("images/"))
                                FileOutputStream(imgFile).use { zis.copyTo(it) }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }

                if (mdContent != null) {
                    val lines = mdContent!!.lines()
                        .filter { it.startsWith("|") && !it.startsWith("| ---") && !it.startsWith("| 样本") && !it.startsWith("| 样品") && !it.startsWith("| Sample") }

                    val rows = mutableListOf<ImportSampleRow>()
                    for (line in lines) {
                        // 先还原导出端的 \| 转义，再按 | 切分，保证含管道符的备注/名称往返一致
                        val cols = line.trim().removePrefix("|").removeSuffix("|")
                            .replace("\\|", "\u0000")
                            .split("|")
                            .map { it.trim().replace("\u0000", "|") }
                        if (cols.size < 8) continue
                        val imgCol = cols.getOrElse(8) { "" }
                        val imgFile = if (imgCol.contains("](")) {
                            imgCol.substringAfter("](").substringBefore(")").removePrefix("images/")
                        } else null
                        rows.add(ImportSampleRow(
                            deviceName = cols[1], layerName = cols[2], boxName = cols[3],
                            posLabel = cols[4],
                            sampleName = cols.getOrElse(0) { "" }.ifBlank { null },
                            date = cols.getOrElse(5) { "" }.ifBlank { null },
                            note = cols.getOrElse(6) { "" }.replace(" <br> ", "\n").ifBlank { null },
                            tags = cols.getOrElse(7) { "" }.ifBlank { "" },
                            imageFile = imgFile
                        ))
                    }
                    if (rows.isNotEmpty()) {
                        imported = importSamples(rows, overwriteExisting = true) { row, box, pos ->
                            copyPhotoInto(row.imageFile, imagesDir, box, pos)
                        }
                    }
                }
                tempDir.deleteRecursively()
            }
            imported > 0
        } catch (e: Exception) {
            false
        }
    }

    /** 把导出包中的图片复制到应用照片目录，并返回其 file:// 路径（用于写入 photo_path）。 */
    private fun copyPhotoInto(imageFile: String?, imagesDir: File, box: StorageBoxEntity, pos: Pair<Int, Int>): String? {
        if (imageFile == null) return null
        val srcImg = File(imagesDir, imageFile)
        if (!srcImg.exists()) return null
        val photosDir = File(context.filesDir, "photos")
        photosDir.mkdirs()
        val destName = "${box.id}_${pos.first}_${pos.second}.jpg"
        val destFile = File(photosDir, destName)
        srcImg.copyTo(destFile, overwrite = true)
        return destFile.toURI().toString()
    }
}