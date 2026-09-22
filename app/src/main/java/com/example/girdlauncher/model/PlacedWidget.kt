package com.example.girdlauncher.model

/**
 * ホーム画面のウィジェットキャンバス上に配置された1つのウィジェット。
 *
 * @property type ウィジェットの種類。
 * @property col 配置されている列（0始まり）。
 * @property row 配置されている行（0始まり）。
 * @property colSpan 横方向に占めるセル数。ウィジェットの種類によっては、アイコンサイズ単位での
 *   細かいリサイズに対応するため小数値を取り得る。
 * @property rowSpan 縦方向に占めるセル数。[colSpan]と同様に小数値を取り得る。
 */
data class PlacedWidget(
    val type: WidgetPanel,
    val col: Int,
    val row: Int,
    val colSpan: Float,
    val rowSpan: Float
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
