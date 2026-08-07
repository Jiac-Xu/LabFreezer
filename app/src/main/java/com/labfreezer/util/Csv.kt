package com.labfreezer.util

/**
 * CSV 编解码工具（RFC 4180 常用子集）。
 *
 * - [encodeLine]：字段含逗号/引号/换行符时用双引号包裹，内部引号双写转义；
 *   同时为首字符为 = / + / - / @ 的字段加单引号前缀，防止电子表格软件将其当作公式执行（CSV 注入）。
 * - [decodeLine]：支持引号包裹字段与 "" 转义，并按行剥离 BOM。
 */
object Csv {

    private const val BOM = '\uFEFF'
    private val FORMULA_PREFIXES = charArrayOf('=', '+', '-', '@')

    /** 编码单个字段；返回已按需求包裹/转义后的字符串。 */
    fun encodeField(raw: String): String {
        val guarded = if (raw.firstOrNull() in FORMULA_PREFIXES.toList()) "'$raw" else raw
        val needsQuote = guarded.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuote) "\"${guarded.replace("\"", "\"\"")}\"" else guarded
    }

    /**
     * 解码整行 CSV（不含换行符）。
     * @return 解析出的字段列表。空行返回空列表。
     */
    fun decodeLine(line: String): List<String> {
        val s = if (line.startsWith(BOM)) line.substring(1) else line
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < s.length) {
            val c = s[i]
            when {
                c == '"' && inQuotes && i + 1 < s.length && s[i + 1] == '"' -> {
                    sb.append('"')
                    i += 2
                }
                c == '"' -> {
                    inQuotes = !inQuotes
                    i++
                }
                c == ',' && !inQuotes -> {
                    result.add(sb.toString())
                    sb.setLength(0)
                    i++
                }
                else -> {
                    sb.append(c)
                    i++
                }
            }
        }
        // 补上行尾：最后字段（含空字段，如 "a,"）
        if (result.isNotEmpty() || sb.isNotEmpty() || inQuotes) {
            result.add(sb.toString())
        }
        return result
    }

    /** 将字段列表编码为一行 CSV。 */
    fun encodeLine(fields: Iterable<String>): String =
        fields.joinToString(",") { encodeField(it) }
}