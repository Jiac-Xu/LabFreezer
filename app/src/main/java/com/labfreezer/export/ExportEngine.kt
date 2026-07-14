package com.labfreezer.export

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.labfreezer.data.db.dao.SamplePositionDao
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.entity.SamplePositionEntity
import com.labfreezer.data.db.entity.StorageBoxEntity
import com.labfreezer.data.db.entity.StorageDeviceEntity
import com.labfreezer.data.db.entity.StorageLayerEntity
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import com.labfreezer.data.repository.TagRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sampleRepository: SamplePositionRepository,
    private val deviceRepository: StorageDeviceRepository,
    private val layerRepository: StorageLayerRepository,
    private val boxRepository: StorageBoxRepository,
    private val tagRepository: TagRepository
) {
    init {
        cleanupOldExports()
    }

    private fun cleanupOldExports() {
        val dir = File(context.filesDir, "exports")
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
        }
    }

    enum class ZipType { DATABASE_BACKUP, MARKDOWN_EXPORT, UNKNOWN }

    data class ZipAnalysis(val type: ZipType, val importSampleCount: Int, val label: String)

    fun analyzeZipFile(uri: Uri): ZipAnalysis {
        return try {
            var type = ZipType.UNKNOWN
            var sampleCount = 0
            var mdContent: String? = null
            var tempDbFile: File? = null

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            name == "labfreezer.db" -> {
                                type = ZipType.DATABASE_BACKUP
                                val tempDir = File(context.cacheDir, "zip_analysis")
                                tempDir.mkdirs()
                                val tmpFile = File(tempDir, "analysis.db")
                                FileOutputStream(tmpFile).use { zis.copyTo(it) }
                                tempDbFile = tmpFile
                            }
                            name.endsWith(".md") && type != ZipType.DATABASE_BACKUP -> {
                                type = ZipType.MARKDOWN_EXPORT
                                mdContent = zis.readBytes().toString(Charsets.UTF_8)
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            }

            when (type) {
                ZipType.DATABASE_BACKUP -> {
                    tempDbFile?.let { dbFile ->
                        try {
                            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                            sampleCount = try {
                                val cursor = db.rawQuery("SELECT COUNT(*) FROM sample_position", null)
                                cursor.moveToFirst()
                                cursor.getInt(0).also { cursor.close() }
                            } catch (_: Exception) { 0 }
                            db.close()
                        } catch (_: Exception) {}
                        dbFile.delete()
                        dbFile.parentFile?.delete()
                    }
                    ZipAnalysis(type, sampleCount, "数据库备份包")
                }
                ZipType.MARKDOWN_EXPORT -> {
                    mdContent?.let { content ->
                        val rows = content.lines().filter {
                            it.startsWith("|") && !it.startsWith("| ---") && !it.startsWith("| 样本")
                        }
                        sampleCount = rows.size
                    }
                    ZipAnalysis(type, sampleCount, "Markdown 导出包")
                }
                ZipType.UNKNOWN -> ZipAnalysis(type, 0, "")
            }
        } catch (_: Exception) {
            ZipAnalysis(ZipType.UNKNOWN, 0, "")
        }
    }

    fun exportCsv(samples: List<SampleWithPath>, name: String, tagsMap: Map<Long, String>): File {
        cleanupOldExports()
        val dir = File(context.filesDir, "exports")
        dir.mkdirs()
        val file = File(dir, "$name.csv")

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { writer ->
            writer.write("\uFEFF")
            val headers = listOf(
                "\u6837\u672c\u540d\u79f0", "\u8bbe\u5907", "\u5c42",
                "\u76d2\u5b50", "\u4f4d\u7f6e", "\u65e5\u671f", "\u5907\u6ce8", "\u6807\u7b7e"
            )
            writer.write(headers.joinToString(","))
            writer.write("\n")

            samples.forEach { s ->
                val tags = tagsMap[s.sampleId] ?: ""
                val row = listOf(
                    s.name ?: "",
                    s.deviceName,
                    s.layerName,
                    s.boxName,
                    "${'A' + s.row}${s.col + 1}",
                    s.date ?: "",
                    s.note ?: "",
                    tags
                )
                writer.write(row.joinToString(","))
                writer.write("\n")
            }
        }
        return file
    }

    fun exportMarkdown(samples: List<SampleWithPath>, name: String, tagsMap: Map<Long, String>): File {
        cleanupOldExports()
        val dir = File(context.filesDir, "exports")
        dir.mkdirs()
        val zipFile = File(dir, "$name.zip")
        val tempDir = File(dir, "${name}_temp")
        tempDir.mkdirs()
        val imagesDir = File(tempDir, "images")
        imagesDir.mkdirs()

        val mdFile = File(tempDir, "$name.md")

        val sb = StringBuilder()
        sb.appendLine("# \u6837\u672c\u6570\u636e\u5bfc\u51fa")
        sb.appendLine()
        sb.appendLine("| \u6837\u672c\u540d\u79f0 | \u8bbe\u5907 | \u5c42 | \u76d2\u5b50 | \u4f4d\u7f6e | \u65e5\u671f | \u5907\u6ce8 | \u6807\u7b7e | \u56fe\u7247 |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|")

        samples.forEach { s ->
            var imageRef = "\u65e0"
            if (s.photoPath != null) {
                try {
                    val srcFile = try {
                        val path = s.photoPath!!
                        val uri = Uri.parse(path)
                        when {
                            uri.scheme == "file" -> File(uri.path!!)
                            uri.scheme == "content" -> {
                                val tmp = File(context.cacheDir, "export_${s.sampleId}.jpg")
                                context.contentResolver.openInputStream(uri)?.use { ins ->
                                    tmp.outputStream().use { ins.copyTo(it) }
                                }
                                tmp.takeIf { it.exists() }
                            }
                            path.startsWith("/") -> File(path)
                            else -> null
                        }
                    } catch (_: Exception) { null }
                    if (srcFile != null && srcFile.exists()) {
                        val imgName = "${s.boxId}_${s.row}_${s.col}.jpg"
                        srcFile.copyTo(File(imagesDir, imgName), overwrite = true)
                        imageRef = "![${s.name ?: "\u6837\u672c"}](images/$imgName)"
                    }
                } catch (_: Exception) {}
            }
            val note = (s.note ?: "").replace("\n", " <br> ")
            val tags = tagsMap[s.sampleId] ?: ""
            sb.appendLine("| ${s.name ?: ""} | ${s.deviceName} | ${s.layerName} | ${s.boxName} | ${'A' + s.row}${s.col + 1} | ${s.date ?: ""} | $note | $tags | $imageRef |")
        }

        mdFile.writeText(sb.toString())

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("$name.md"))
            FileInputStream(mdFile).use { it.copyTo(zos) }
            zos.closeEntry()

            imagesDir.listFiles()?.forEach { img ->
                zos.putNextEntry(ZipEntry("images/${img.name}"))
                FileInputStream(img).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        tempDir.deleteRecursively()
        return zipFile
    }

    fun exportDatabase(): File {
        cleanupOldExports()
        val dir = File(context.filesDir, "exports")
        dir.mkdirs()
        val file = File(dir, "labfreezer_backup.zip")
        val dbFile = context.getDatabasePath("labfreezer.db")

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            if (dbFile.exists()) {
                zos.putNextEntry(ZipEntry("labfreezer.db"))
                FileInputStream(dbFile).use { it.copyTo(zos) }
                zos.closeEntry()
            }
            val shmFile = File(dbFile.path + "-shm")
            val walFile = File(dbFile.path + "-wal")
            if (shmFile.exists()) {
                zos.putNextEntry(ZipEntry("labfreezer.db-shm"))
                FileInputStream(shmFile).use { it.copyTo(zos) }
                zos.closeEntry()
            }
            if (walFile.exists()) {
                zos.putNextEntry(ZipEntry("labfreezer.db-wal"))
                FileInputStream(walFile).use { it.copyTo(zos) }
                zos.closeEntry()
            }

            val photosDir = File(context.filesDir, "photos")
            if (photosDir.exists()) {
                photosDir.listFiles()?.forEach { photo ->
                    zos.putNextEntry(ZipEntry("photos/${photo.name}"))
                    FileInputStream(photo).use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
        }
        return file
    }

    fun importDatabase(uri: Uri): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when {
                            name == "labfreezer.db" -> {
                                val dbFile = context.getDatabasePath("labfreezer.db")
                                dbFile.parentFile?.mkdirs()
                                FileOutputStream(dbFile).use { zis.copyTo(it) }
                            }
                            name == "labfreezer.db-shm" -> {
                                val f = File(context.getDatabasePath("labfreezer.db").path + "-shm")
                                FileOutputStream(f).use { zis.copyTo(it) }
                            }
                            name == "labfreezer.db-wal" -> {
                                val f = File(context.getDatabasePath("labfreezer.db").path + "-wal")
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
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun importMarkdown(uri: Uri): Boolean {
        return try {
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

                if (mdContent == null) { tempDir.deleteRecursively(); return false }

                val lines = mdContent!!.lines().filter { it.startsWith("|") && !it.startsWith("| ---") && !it.startsWith("| 样本名称") }

                data class Row(
                    val deviceName: String, val layerName: String, val boxName: String,
                    val posLabel: String, val sampleName: String?, val date: String?, val note: String?,
                    val imageFile: String?, val tags: String
                )

                val rows = mutableListOf<Row>()
                for (line in lines) {
                    val cols = line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
                    if (cols.size < 8) continue
                    val imgCol = cols.getOrElse(8) { "" }
                    val imgFile = if (imgCol.contains("](")) {
                        imgCol.substringAfter("](").substringBefore(")").removePrefix("images/")
                    } else null
                    rows.add(Row(
                        deviceName = cols[1], layerName = cols[2], boxName = cols[3],
                        posLabel = cols[4],
                        sampleName = cols.getOrElse(0) { "" }.ifBlank { null },
                        date = cols.getOrElse(5) { "" }.ifBlank { null },
                        note = cols.getOrElse(6) { "" }.replace(" <br> ", "\n").ifBlank { null },
                        imageFile = imgFile,
                        tags = cols.getOrElse(7) { "" }.ifBlank { "" }
                    ))
                }

                data class BoxKey(val device: String, val layer: String, val box: String)
                val boxPositions = mutableMapOf<BoxKey, MutableSet<Pair<Int, Int>>>()
                for (r in rows) {
                    val pos = parsePosition(r.posLabel) ?: continue
                    boxPositions.getOrPut(BoxKey(r.deviceName, r.layerName, r.boxName)) { mutableSetOf() }.add(pos)
                }

                for (r in rows) {
                    val pos = parsePosition(r.posLabel) ?: continue

                    val allDevices = deviceRepository.getAll()
                    var device = allDevices.find { it.name == r.deviceName }
                    if (device == null) {
                        val newId = deviceRepository.insert(StorageDeviceEntity(name = r.deviceName))
                        device = deviceRepository.getById(newId) ?: continue
                    }

                    val allLayers = layerRepository.getByDeviceId(device.id)
                    var layer = allLayers.find { it.name == r.layerName }
                    if (layer == null) {
                        val newId = layerRepository.insert(StorageLayerEntity(deviceId = device.id, name = r.layerName))
                        layer = layerRepository.getById(newId) ?: continue
                    }

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

                    var photoPath: String? = null
                    if (r.imageFile != null) {
                        val srcImg = File(imagesDir, r.imageFile)
                        if (srcImg.exists()) {
                            val photosDir = File(context.filesDir, "photos")
                            photosDir.mkdirs()
                            val destName = "${box.id}_${pos.first}_${pos.second}.jpg"
                            val destFile = File(photosDir, destName)
                            srcImg.copyTo(destFile, overwrite = true)
                            photoPath = destFile.toURI().toString()
                        }
                    }

                    val existing = sampleRepository.getByPosition(box.id, pos.first, pos.second)
                    val sampleId = if (existing != null) {
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
                                val newTagId = tagRepository.insert(com.labfreezer.data.db.entity.TagEntity(name = tagName, color = "#007AFF"))
                                tag = tagRepository.getById(newTagId)
                            }
                            tag!!.id
                        }
                        tagRepository.setSampleTags(sampleId, tagIds)
                    }
                }

                tempDir.deleteRecursively()
            }
            true
        } catch (e: Exception) {
            false
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
}
