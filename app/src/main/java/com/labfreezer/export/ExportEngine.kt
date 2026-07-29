package com.labfreezer.export

import android.content.Context
import android.net.Uri
import com.labfreezer.data.db.dao.SampleWithPath
import com.labfreezer.data.db.isHiddenMarker
import com.labfreezer.data.repository.SamplePositionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sampleRepository: SamplePositionRepository
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
                val deviceLabel = s.deviceName
                val layerLabel = s.layerName
                val row = listOf(
                    s.name ?: "",
                    deviceLabel,
                    layerLabel,
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
            val note = (s.note ?: "").replace("\n", " <br> ")
            val tags = tagsMap[s.sampleId] ?: ""
            val mdDevice = s.deviceName
            val mdLayer = s.layerName
            sb.appendLine("| ${s.name ?: ""} | $mdDevice | $mdLayer | ${s.boxName} | ${'A' + s.row}${s.col + 1} | ${s.date ?: ""} | $note | $tags | $imageRef |")
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
}