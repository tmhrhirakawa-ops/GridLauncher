package com.example.girdlauncher.model

/**
 * ホーム画面のウィジェットキャンバス上に配置された1つのウィジェット。
 *
 * @property type ウィジェットの種類。
 * @property appWidgetId [type]が[WidgetPanel.APPWIDGET]の場合にのみ意味を持つ、
 *   [android.appwidget.AppWidgetHost]が発行したインスタンス固有のID。同じ外部ウィジェットの
 *   プロバイダを複数配置しても区別できるようにするためのもの。それ以外の種類では-1のまま。
 * @property col 配置されている列（0始まり）。左端・上端のコーナーハンドルでリサイズすると
 *   小数値を取り得る。
 * @property row 配置されている行（0始まり）。[col]と同様に小数値を取り得る。
 * @property colSpan 横方向に占めるセル数。ウィジェットの種類によっては、アイコンサイズ単位での
 *   細かいリサイズに対応するため小数値を取り得る。
 * @property rowSpan 縦方向に占めるセル数。[colSpan]と同様に小数値を取り得る。
 */
data class PlacedWidget(
    val type: WidgetPanel,
    val appWidgetId: Int = -1,
    val col: Float,
    val row: Float,
    val colSpan: Float,
    val rowSpan: Float
) {
    /**
     * この配置済みウィジェットを一意に識別するキー。[WidgetPanel]は[WidgetPanel.APPWIDGET]だけ
     * 複数インスタンスを同時配置できるため、[type]単体ではなく[appWidgetId]も含めて区別する。
     */
    val instanceKey: String
        get() = if (type == WidgetPanel.APPWIDGET) "APPWIDGET:$appWidgetId" else type.name

    /** [other]と矩形が重なっているかどうか。 */
    fun overlaps(other: PlacedWidget): Boolean {
        val aRight = col + colSpan
        val aBottom = row + rowSpan
        val bRight = other.col + other.colSpan
        val bBottom = other.row + other.rowSpan
        return col < bRight && aRight > other.col && row < bBottom && aBottom > other.row
    }
}
