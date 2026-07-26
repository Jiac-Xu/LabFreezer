package com.labfreezer.data.search

import java.util.Calendar

/**
 * 搜索关键词标准化工具。
 *
 * 在搜索阶段对用户输入的关键词进行扩展，生成多个候选变体，
 * 用于匹配数据库中格式不统一的样本名称和日期字段。
 *
 * 不修改数据库结构、不新增字段、不做 Room Migration。
 */
object SearchNormalizer {

    internal val SEPARATORS = setOf('-', '_', ' ', '.')
    private const val MAX_VARIANTS = 12
    private val YEAR_RANGE = 1900..2100

    /**
     * 已知实验缩写后缀，用于样本名称的模糊搜索扩展。
     *
     * 这些缩写在生物/医学实验命名中具有特定含义：
     * - WT:  Wild Type（野生型）
     * - NC:  Negative Control（阴性对照）
     * - OE:  Over Expression（过表达）
     * - SH:  shRNA（短发夹RNA）
     * - KO:  Knock Out（基因敲除）
     * - KD:  Knock Down（基因敲低）
     * - MUT: Mutant（突变体）
     * - CTRL: Control（对照）
     *
     * 按长度降序排列，确保最长匹配优先。
     */
    internal val knownExperimentSuffixes = listOf("CTRL", "MUT", "WT", "NC", "OE", "SH", "KO", "KD")

    // ==================== 名称模糊搜索 ====================

    /**
     * 生成样本名称的模糊搜索关键词变体。
     *
     * 策略：
     * 1. 保留原始输入
     * 2. 去除分隔符（-、_、空格、.）后的纯文本形式
     * 3. 在字母/数字边界插入各常见分隔符
     *
     * 例如 "R100" → ["R100", "R-100", "R_100", "R 100", "R.100"]
     * 例如 "MV411R100" → ["MV411R100", "MV-411-R-100", "MV_411_R_100", ...]
     *
     * 注意：只在字母/数字类型变化的边界插入分隔符，不会在任意位置插入。
     * 因此 "R100" 不会匹配 "R10-0"（因为 "R10-0" 中不含 "R-100" 或 "R100" 子串）。
     */
    fun generateNameVariants(input: String): List<String> {
        if (input.isBlank()) return emptyList()

        val trimmed = input.trim()
        val variants = linkedSetOf<String>()

        // 1. 保留原始输入
        variants.add(trimmed)

        // 2. 去除分隔符的纯文本形式
        val stripped = trimmed.filter { it !in SEPARATORS }
        if (stripped != trimmed) {
            variants.add(stripped)
        }

        // 3. 实验缩写后缀识别：当名称以已知实验缩写结尾时，生成带各分隔符的变体
        //    仅对样本名称有效，不应用于 note/OCR 文本搜索
        //    例如 "BTKSH" → ["BTK SH", "BTK-SH", "BTK_SH", "BTK.SH"]
        //    例如 "HL60WT" → ["HL60 WT", "HL60-WT", "HL60_WT", "HL60.WT"]
        addExperimentSuffixVariants(stripped, variants)

        // 4. 在字母/数字边界统一插入各分隔符
        val boundaries = findLetterDigitBoundaries(stripped)
        if (boundaries.isNotEmpty()) {
            for (sep in SEPARATORS) {
                val variant = buildString {
                    var lastIdx = 0
                    for (b in boundaries.sorted()) {
                        append(stripped.substring(lastIdx, b))
                        append(sep)
                        lastIdx = b
                    }
                    append(stripped.substring(lastIdx))
                }
                variants.add(variant)
            }
        }

        return variants.toList()
    }

    /**
     * 将样本名称归一化为可供比较的规范形式。
     *
     * 规则：
     * - lowercase()
     * - 仅保留字母和数字，去除所有分隔符（-、_、空格、.）
     *
     * 用于 SQL 查询结果的后置过滤，确保名称匹配不受格式差异影响。
     *
     * 例如：
     * - "MV4-11" → "mv411"
     * - "MV411"  → "mv411"
     * - "HL60 WT" → "hl60wt"
     * - "R-100"  → "r100"
     */
    fun normalizeNameForCompare(name: String?): String {
        return name?.lowercase()?.filter { it.isLetterOrDigit() } ?: ""
    }

    // ==================== 日期智能搜索 ====================

    /**
     * 生成日期的智能搜索关键词变体。
     *
     * 根据输入数字位数智能解析日期格式，生成常见格式变体：
     * - 8位 → yyyyMMdd
     * - 6位 → 前4位在合理年份范围内则为 yyyyMM，否则 yyMMdd
     * - 4位 → 同时尝试 YYMM 和 MMdd，生成多种格式
     *
     * 不在搜索阶段修改原始数据，只生成候选关键词用于匹配。
     */
    fun generateDateVariants(input: String): List<String> {
        val digits = input.filter { it.isDigit() }
        if (digits.length != input.length || digits.length < 4 || digits.length > 8) {
            return emptyList()
        }

        val variants = linkedSetOf<String>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        when (digits.length) {
            8 -> addYyyyMmDd(digits, variants)
            6 -> handle6Digits(digits, variants, currentYear)
            4 -> handle4Digits(digits, variants, currentYear)
        }

        return variants.take(MAX_VARIANTS).toList()
    }

    /**
     * 将数据库中任意格式的日期字符串标准化为 yyyy-MM 格式，用于日期聚合统计。
     *
     * 支持格式：
     * - yyyy-MM-dd / yyyy-M-d
     * - yyyy.MM.dd / yyyy.M.d
     * - yyyy/MM/dd / yyyy/M/d
     * - M-d-yyyy / MM-dd-yyyy（月日年）
     * - yyyy年M月d日
     * - yyyyMMdd（8位紧凑）
     * - yyyyMM（6位，年份前4位在合理范围）
     * - yyMMdd（6位，年份前2位，自动补20xx）
     * - YYMM（4位，前2位为年份后2位为月份）
     *
     * @param date 数据库中的原始日期字符串
     * @return yyyy-MM 格式字符串，解析失败返回 null
     */
    fun parseDateToYearMonth(date: String?): String? {
        if (date.isNullOrBlank()) return null
        val trimmed = date.trim()

        // 1. 尝试 yyyy-MM-dd / yyyy-M-d（最常用，自动日期格式）
        //    yyyy.MM.dd / yyyy.M.d
        //    yyyy/MM/dd / yyyy/M/d
        YYYY_MM_DD_REGEX.find(trimmed)?.let { m ->
            val y = m.groupValues[1]
            val mo = m.groupValues[2].padStart(2, '0')
            return "$y-$mo"
        }

        // 2. 尝试 M-d-yyyy / MM-dd-yyyy（OCR 月日年格式）
        M_D_YYYY_REGEX.find(trimmed)?.let { m ->
            val y = m.groupValues[3]
            val mo = m.groupValues[1].padStart(2, '0')
            return "$y-$mo"
        }

        // 3. 尝试 yyyy年M月d日
        CHINESE_DATE_REGEX.find(trimmed)?.let { m ->
            val y = m.groupValues[1]
            val mo = m.groupValues[2].padStart(2, '0')
            return "$y-$mo"
        }

        // 4. 纯数字格式
        val digits = trimmed.filter { it.isDigit() }
        if (digits.length == trimmed.length && digits.isNotEmpty()) {
            return when (digits.length) {
                8 -> {
                    // yyyyMMdd
                    "${digits.substring(0, 4)}-${digits.substring(4, 6)}"
                }
                6 -> {
                    val first4 = digits.substring(0, 4).toIntOrNull() ?: 0
                    if (first4 in YEAR_RANGE) {
                        // yyyyMM
                        "${digits.substring(0, 4)}-${digits.substring(4, 6)}"
                    } else {
                        // yyMMdd → 20xx
                        val year = "20${digits.substring(0, 2)}"
                        "$year-${digits.substring(2, 4)}"
                    }
                }
                4 -> {
                    val yy = digits.substring(0, 2).toIntOrNull() ?: 0
                    val mm = digits.substring(2, 4).toIntOrNull() ?: 0
                    if (mm in 1..12) {
                        // YYMM
                        "20${digits.substring(0, 2)}-${digits.substring(2, 4)}"
                    } else {
                        null // 无法解析为年月
                    }
                }
                else -> null
            }
        }

        // 5. 尝试提取年份和月份（通用 fallback：查找 4 位年份 + 1-2 位月份）
        //    例如 "2026年7月" 但前面没匹配到，或者 "2026-07" 这类
        FALLBACK_DATE_REGEX.find(trimmed)?.let { m ->
            val y = m.groupValues[1]
            val mo = m.groupValues[2].padStart(2, '0')
            return "$y-$mo"
        }

        return null
    }

    // ==================== 日期解析正则表达式 ====================

    private val YYYY_MM_DD_REGEX = Regex("""(\d{4})[-/.](\d{1,2})[-/.](\d{1,2})""")
    private val M_D_YYYY_REGEX = Regex("""(\d{1,2})[-/.](\d{1,2})[-/.](\d{4})""")
    private val CHINESE_DATE_REGEX = Regex("""(\d{4})年(\d{1,2})月(\d{1,2})日""")
    private val FALLBACK_DATE_REGEX = Regex("""(\d{4})\D(\d{1,2})""")

    // ==================== 内部辅助方法 ====================

    /**
     * 找出字符串中字母/数字类型变化的边界索引。
     * 例如 "R100" → [1]（R->1 处），"MV411R100" → [2, 5, 6]
     */
    private fun findLetterDigitBoundaries(s: String): List<Int> {
        val boundaries = mutableListOf<Int>()
        for (i in 1 until s.length) {
            val prevIsLetter = s[i - 1].isLetter()
            val currIsLetter = s[i].isLetter()
            if (prevIsLetter != currIsLetter) {
                boundaries.add(i)
            }
        }
        return boundaries
    }

    /**
     * 实验缩写后缀识别：当名称以已知实验缩写结尾时，生成带各分隔符的变体。
     *
     * 规则：
     * - 不区分大小写匹配
     * - 仅匹配末尾的完整缩写
     * - base 部分必须非空且以字母或数字结尾（避免空 base 或纯分隔符结尾）
     * - 对匹配到的每个分隔符生成一个变体
     *
     * 例如 "BTKSH" → ["BTK SH", "BTK-SH", "BTK_SH", "BTK.SH"]
     * 例如 "HL60WT" → ["HL60 WT", "HL60-WT", "HL60_WT", "HL60.WT"]
     * 例如 "KDM6AOE" → ["KDM6A OE", "KDM6A-OE", "KDM6A_OE", "KDM6A.OE"]
     *
     * 仅对 stripped（无分隔符）输入调用，确保不被中间分隔符干扰。
     * 不应应用于 note/OCR 文本搜索，仅用于样本名称(name)关键词。
     */
    private fun addExperimentSuffixVariants(input: String, variants: MutableSet<String>) {
        if (input.length < 3) return // 最短匹配：1 base + 2 suffix（如 "AOE"），但至少需要 3 字符
        val upper = input.uppercase()
        for (suffix in knownExperimentSuffixes) {
            if (upper.endsWith(suffix) && upper.length > suffix.length) {
                val base = input.substring(0, input.length - suffix.length)
                val suffixRaw = input.substring(input.length - suffix.length)
                // base 必须以字母或数字结尾，避免空 base 或 纯分隔符输入
                if (base.isNotEmpty() && base.last().isLetterOrDigit()) {
                    for (sep in SEPARATORS) {
                        variants.add("$base$sep$suffixRaw")
                    }
                }
                return // 只匹配最长后缀（列表已按长度降序排列）
            }
        }
    }

    /**
     * 8位数字：yyyyMMdd → 生成带常见分隔符的格式
     */
    private fun addYyyyMmDd(digits: String, variants: MutableSet<String>) {
        val year = digits.substring(0, 4)
        val month = digits.substring(4, 6)
        val day = digits.substring(6, 8)
        variants.add("$year-$month-$day")
        variants.add("$year.$month.$day")
        variants.add("$year/$month/$day")
        variants.add(digits) // yyyyMMdd
    }

    /**
     * 6位数字：判断前4位是否合理年份
     * - 是 → yyyyMM
     * - 否 → yyMMdd（假设 20xx）
     */
    private fun handle6Digits(digits: String, variants: MutableSet<String>, currentYear: Int) {
        val first4 = digits.substring(0, 4).toIntOrNull() ?: 0

        if (first4 in YEAR_RANGE) {
            // yyyyMM
            val year = digits.substring(0, 4)
            val month = digits.substring(4, 6)
            variants.add("$year-$month")
            variants.add("$year/$month")
            variants.add(digits) // yyyyMM
        } else {
            // yyMMdd → 补全为 20xx
            val year = "20${digits.substring(0, 2)}"
            val month = digits.substring(2, 4)
            val day = digits.substring(4, 6)
            variants.add("$year-$month-$day")
            variants.add("$year.$month.$day")
            variants.add("$year/$month/$day")
            variants.add("$year$month$day") // yyyyMMdd 无分隔符
            variants.add(digits) // yyMMdd
        }
    }

    /**
     * 4位数字：同时尝试 YYMM 和 MMdd 两种解释。
     * - YYMM：前2位为年份（20xx），后2位为月份
     * - MMdd：前2位为月份，后2位为日期，补全当前年份
     *
     * 存在歧义时生成多个候选，不做强行判断。
     */
    private fun handle4Digits(digits: String, variants: MutableSet<String>, currentYear: Int) {
        val first2 = digits.substring(0, 2).toIntOrNull() ?: 0
        val second2 = digits.substring(2, 4).toIntOrNull() ?: 0
        val fullValue = digits.toIntOrNull() ?: 0

        // 独立年份：1900-2100 范围内的 4 位数，如 "2026" → 匹配 date 字段中的 "2026-xx-xx"
        if (fullValue in YEAR_RANGE) {
            variants.add(digits)
        }

        // YYMM 解释：前2位为年份（20xx），后2位为月份
        if (second2 in 1..12) {
            val year = "20${digits.substring(0, 2)}"
            val month = digits.substring(2, 4)
            variants.add("$year-$month")
            variants.add("$year/$month")
            variants.add("$year$month") // yyyyMM
            variants.add(digits) // yyMM
        }

        // MMdd 解释：前2位为月份，后2位为日期
        if (first2 in 1..12 && second2 in 1..31) {
            val month = digits.substring(0, 2)
            val day = digits.substring(2, 4)
            variants.add("$month-$day")
            variants.add("$month.$day")
            variants.add("$currentYear-$month-$day")
            variants.add("$currentYear$month$day") // yyyyMMdd
            variants.add(digits) // MMdd
        }
    }
}