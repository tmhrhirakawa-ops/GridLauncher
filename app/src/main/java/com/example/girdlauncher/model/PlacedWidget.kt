package com.example.girdlauncher.model

/**
 * ホーム画面のウィジェットキャンバス上に配置された1つのウィジェット。
 *
 * @property type ウィジェットの種類。
 * @property col 配置されている列（0始まり）。
 * @property row 配置されている行（0始まり）。
 * @property colSpan 横方向に占めるセル数。
 * @property rowSpan 縦方向に占めるセル数。
 */
data class PlacedWidget(
    val type: WidgetPanel,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int
) {
    /** [other]と矩形が重なっているかどうか。 */
    fun overlaps(other: PlacedWidget): Boolean {
        val aRight = col + colSpan
        val aBottom = row + rowSpan
        val bRight = other.col + other.colSpan
        val bBottom = other.row + other.rowSpan
        return col < bRight && aRight > other.col && row < bBottom && aBottom > other.row
    }
}
