package com.labfreezer.export

import android.content.Context
import android.net.Uri
import com.labfreezer.data.db.AppDatabase
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.isHiddenMarker
import com.labfreezer.data.model.Position
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.util.Csv
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val sampleRepository: SamplePositionRepository
) {
    init {
        cleanupOldExports()
    }

    /**
     * 导出前执行 WAL checkpoint，把未落盘的更改刷入主库文件。
     * 这样即使 zip 里不包含 -wal/-shm，恢复出的库也是完整一致的。
     */
    private fun checkpointDatabase() {
        try {
            database.openHelper.writableDatabase
                .query("PRAGMA wal_checkpoint(TRUNCATE)")
                .use { cursor -> cursor.moveToFirst() }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "WAL checkpoint failed, exporting raw files", e)
        }
    }

    private fun cleanupOldExports() {
        val dir = File(context.filesDir, "exports")
        if (dir.exists()) {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) file.deleteRecursively() else file.delete()
            }
        }
    }

    private fun createManifestJson(type: String, sampleCount: Int): String {
        val json = JSONObject()
        json.put("app", "LabFreezer")
        json.put("type", type)
        json.put("version", 3)
        json.put("sampleCount", sampleCount)
        json.put("exportTime", System.currentTimeMillis())
        json.put("appVersion", try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" })
        return json.toString(2)
    }

    fun exportCsv(samples: List<SampleWithPath>, name: String, tagsMap: Map<Long, String>): File {
        cleanupOldExports()
        val dir = File(context.filesDir, "exports")
        dir.mkdirs()
        val file = File(dir, "$name.csv")

        OutputStreamWriter(FileOutputStream(file), Charsets.UTF_8).use { writer ->
            writer.write("﻿")
            val headers = listOf(
                "样本名称", "设备", "层",
                "盒子", "位置", "日期", "备注", "标签"
            )
            writer.write(headers.joinToString(","))
            writer.write("\n")

            samples.forEach { s ->
                val tags = tagsMap[s.sampleId] ?: ""
                // 备注里的换行统一转成空格，保证每条样本固定为一行 CSV
                val note = (s.note ?: "").replace("\n", " ").replace("\r", " ")
                val row = listOf(
                    s.name ?: "",
                    s.deviceName,
                    s.layerName,
                    s.boxName,
                    Position.toLabel(s.row, s.col),
                    s.date ?: "",
                    note,
                    tags
                )
                writer.write(Csv.encodeLine(row))
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
        sb.appendLine("# 样本数据导出")
        sb.appendLine()
        sb.appendLine("| 样本名称 | 设备 | 层 | 盒子 | 位置 | 日期 | 备注 | 标签 | 图片 |")
        sb.appendLine("|---|---|---|---|---|---|---|---|---|")

        samples.forEach { s ->
            var imageRef = "无"
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
                        imageRef = "![${s.name ?: "样本"}](images/$imgName)"
                    }
                } catch (_: Exception) {}
            }
            // 备注/名称中含 | 会破坏表格结构，统一转义为 HTML 实体
            val note = (s.note ?: "").replace("\n", " <br> ").replace("|", "\\|")
            val mdName = (s.name ?: "").replace("|", "\\|")
            val mdDevice = s.deviceName.replace("|", "\\|")
            val mdLayer = s.layerName.replace("|", "\\|")
            val mdBox = s.boxName.replace("|", "\\|")
            val tags = tagsMap[s.sampleId] ?: ""
            sb.appendLine("| $mdName | $mdDevice | $mdLayer | $mdBox | ${Position.toLabel(s.row, s.col)} | ${s.date ?: ""} | $note | $tags | $imageRef |")
        }

        mdFile.writeText(sb.toString())

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // Manifest
            val manifest = createManifestJson("markdown", samples.size)
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifest.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Markdown
            zos.putNextEntry(ZipEntry("$name.md"))
            FileInputStream(mdFile).use { it.copyTo(zos) }
            zos.closeEntry()

            // Images
            imagesDir.listFiles()?.forEach { img ->
                zos.putNextEntry(ZipEntry("images/${img.name}"))
                FileInputStream(img).use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }

        tempDir.deleteRecursively()
        return zipFile
    }

    suspend fun exportDatabase(): File {
        cleanupOldExports()
        // 先把 WAL 未落盘数据刷入主库，保证恢复时无需依赖 WAL 文件
        checkpointDatabase()
        val dir = File(context.filesDir, "exports")
        dir.mkdirs()
        val file = File(dir, "labfreezer_backup.zip")
        val dbFile = context.getDatabasePath("labfreezer.db")

        // 统计样本数用于 manifest
        val sampleCount = try {
            sampleRepository.countAll()
        } catch (_: Exception) { 0 }

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // Manifest
            val manifest = createManifestJson("database", sampleCount)
            zos.putNextEntry(ZipEntry("manifest.json"))
            zos.write(manifest.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // Database
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

            // Photos
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

    companion object {
        private const val TAG = "ExportEngine"
    }
}