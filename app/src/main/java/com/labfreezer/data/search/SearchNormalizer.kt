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

    private val SEPARATORS = setOf('-', '_', ' ', '.')
    private const val MAX_VARIANTS = 12
    private val YEAR_RANGE = 1900..2100

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

        // 3. 在字母/数字边界统一插入各分隔符
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