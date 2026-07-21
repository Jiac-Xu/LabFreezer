package com.labfreezer.data.search

/**
 * 解析后的搜索查询，将用户输入拆分为名称关键词和日期关键词。
 *
 * 例如：
 * - "R100 2026"      → nameKeyword="R100",    dateKeywords=["2026"]
 * - "HL60 WT 2026"   → nameKeyword="HL60 WT", dateKeywords=["2026"]
 * - "20260711"        → nameKeyword="",        dateKeywords=["20260711"]
 * - "R100"            → nameKeyword="R100",    dateKeywords=[]
 */
data class ParsedQuery(
    val nameKeyword: String,
    val dateKeywords: List<String>
)

/**
 * 搜索查询解析器。
 *
 * 将用户输入按空格分词，识别日期类 token（纯数字且长度为 4/6/8 位），
 * 剩余 token 作为样本名称关键词（保留原始空格连接）。
 *
 * 注意：不会拆分名称中可能包含的空格（如 "HL60 WT" 是一个整体名称）。
 * 只有独立的日期 token 会被分离出去。
 */
object SearchQueryParser {

    private val DATE_TOKEN_LENGTHS = setOf(4, 6, 8)

    /**
     * 解析用户输入的搜索关键词。
     *
     * @param input 用户原始输入
     * @return 解析后的 ParsedQuery
     */
    fun parse(input: String): ParsedQuery {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return ParsedQuery("", emptyList())

        val tokens = trimmed.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val nameParts = mutableListOf<String>()
        val dateTokens = mutableListOf<String>()

        for (token in tokens) {
            if (isDateToken(token)) {
                dateTokens.add(token)
            } else {
                nameParts.add(token)
            }
        }

        return ParsedQuery(
            nameKeyword = nameParts.joinToString(" "),
            dateKeywords = dateTokens
        )
    }

    /**
     * 判断一个 token 是否是日期关键词。
     *
     * 规则：token 全部由数字组成，且长度为 4、6 或 8。
     * 例如："2026"、"2607"、"0411"、"202607"、"260711"、"20260711"
     */
    private fun isDateToken(token: String): Boolean {
        val digitCount = token.count { it.isDigit() }
        return digitCount == token.length && digitCount in DATE_TOKEN_LENGTHS
    }
}