package com.labfreezer.data.model

/**
 * 样本位置（行列）统一编码工具。
 *
 * 约定：行 = Excel 风格字母列（0-based → A/B/.../Z/AA/AB...），列 = 1-based 数字。
 *   (0, 0) -> A1， (25, 0) -> Z1， (26, 0) -> AA1 ...
 *
 * 统一编码避免原先 `'A' + row` 在行数超过 26 时产生乱字符（'['、'\' 等），
 * 也保证导出/导入两端对位置的处理完全一致。
 */
object Position {

    /** 行号（0-based）→ 字母标签：0->A，25->Z，26->AA */
    fun toRowLabel(row: Int): String {
        var v = row.coerceAtLeast(0) + 1
        val out = StringBuilder()
        while (v > 0) {
            var d = v % 26
            if (d == 0) d = 26
            out.append('A' + d - 1)
            v = (v - d) / 26
        }
        return out.reverse().toString()
    }

    /** 完整位置标签：(0, 0) -> A1 */
    fun toLabel(row: Int, col: Int): String = toRowLabel(row) + (col + 1)

    /**
     * 解析位置标签（A1 / B26 / AA1 等）。
     * @return (row, col)，均为 0-based；解析失败返回 null。
     */
    fun parse(label: String?): Pair<Int, Int>? {
        if (label.isNullOrBlank()) return null
        val t = label.trim()
        val rowPart = t.takeWhile { it.isLetter() }
        val colPart = t.drop(rowPart.length)
        if (rowPart.isEmpty() || colPart.isEmpty()) return null

        var n = 0
        for (ch in rowPart.uppercase()) {
            val d = ch - 'A' + 1
            n = n * 26 + d
        }
        val row = n - 1
        val col = colPart.toIntOrNull()?.minus(1) ?: return null
        if (row < 0 || col < 0) return null
        return row to col
    }
}