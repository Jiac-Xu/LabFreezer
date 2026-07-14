package com.labfreezer.export

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导入包类型。
 * - DATABASE_BACKUP: 数据库备份包（含 labfreezer.db 或 manifest.json 中 type=database）
 * - MARKDOWN_EXPORT: Markdown 导出包（含 .md 文件或 manifest.json 中 type=markdown）
 * - INVALID: 压缩包损坏或无法读取
 * - UNSUPPORTED: 压缩包正常，但不是冰盒导出的数据
 */
enum class ZipType {
    DATABASE_BACKUP,
    MARKDOWN_EXPORT,
    INVALID,
    UNSUPPORTED
}

/**
 * 导入包分析结果。
 * @param type 包类型
 * @param sampleCount 包中包含的样本数
 * @param version 导出格式版本号（来自 manifest.json，未来兼容用）
 * @param exportTime 导出时间戳（来自 manifest.json，未来展示用）
 */
data class ZipAnalysis(
    val type: ZipType,
    val sampleCount: Int,
    val version: Int? = null,
    val exportTime: Long? = null
)

@Singleton
class ZipInspector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 分析导入包，识别类型并统计样本数。
     * 优先读取 manifest.json，不存在则回退到启发式检测。
     */
    fun inspectImportPackage(uri: Uri): ZipAnalysis {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    val entries = mutableMapOf<String, ByteArray>()
                    var entry = zis.nextEntry
                    while (entry != null) {
                        if (entry.name == "manifest.json") {
                            entries[entry.name] = zis.readBytes()
                        } else if (entry.name == "labfreezer.db") {
                            entries[entry.name] = zis.readBytes()
                        } else if (entry.name.endsWith(".md") && !entries.containsKey("manifest.json")) {
                            // 只记录第一个 .md 文件（不含 manifest.json 时回退）
                            entries.putIfAbsent("md_content", zis.readBytes())
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                    parseAnalysis(entries)
                }
            } ?: ZipAnalysis(ZipType.INVALID, 0)
        } catch (e: Exception) {
            ZipAnalysis(ZipType.INVALID, 0)
        }
    }

    private fun parseAnalysis(entries: Map<String, ByteArray>): ZipAnalysis {
        // 优先使用 manifest.json
        val manifest = entries["manifest.json"]
        if (manifest != null) {
            return try {
                val json = JSONObject(String(manifest, Charsets.UTF_8))
                val type = when (json.optString("type")) {
                    "database" -> ZipType.DATABASE_BACKUP
                    "markdown" -> ZipType.MARKDOWN_EXPORT
                    else -> return ZipAnalysis(ZipType.UNSUPPORTED, 0)
                }
                ZipAnalysis(
                    type = type,
                    sampleCount = json.optInt("sampleCount", 0),
                    version = json.optInt("version"),
                    exportTime = json.optLong("exportTime")
                )
            } catch (_: Exception) {
                ZipAnalysis(ZipType.INVALID, 0)
            }
        }

        // 无 manifest.json，回退到启发式检测
        val dbBytes = entries["labfreezer.db"]
        if (dbBytes != null) {
            return analyzeDatabaseBackup(dbBytes)
        }

        val mdBytes = entries["md_content"]
        if (mdBytes != null) {
            return analyzeMarkdownExport(mdBytes)
        }

        return ZipAnalysis(ZipType.UNSUPPORTED, 0)
    }

    private fun analyzeDatabaseBackup(dbBytes: ByteArray): ZipAnalysis {
        return try {
            val tempDir = File(context.cacheDir, "zip_inspect")
            tempDir.mkdirs()
            val tmpFile = File(tempDir, "inspect.db")
            FileOutputStream(tmpFile).use { it.write(dbBytes) }
            val count = try {
                val db = SQLiteDatabase.openDatabase(tmpFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val cursor = db.rawQuery("SELECT COUNT(*) FROM sample_position", null)
                cursor.moveToFirst()
                cursor.getInt(0).also { cursor.close(); db.close() }
            } catch (_: Exception) { 0 }
            tmpFile.delete(); tempDir.delete()
            ZipAnalysis(ZipType.DATABASE_BACKUP, count)
        } catch (_: Exception) {
            ZipAnalysis(ZipType.INVALID, 0)
        }
    }

    private fun analyzeMarkdownExport(mdBytes: ByteArray): ZipAnalysis {
        val content = String(mdBytes, Charsets.UTF_8)
        val rows = content.lines().filter {
            it.startsWith("|") && !it.startsWith("| ---") && !it.startsWith("| 样本")
        }
        return ZipAnalysis(ZipType.MARKDOWN_EXPORT, rows.size)
    }
}