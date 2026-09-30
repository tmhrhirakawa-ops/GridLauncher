package com.example.gridlauncher.model

/**
 * ホーム画面のウィジェットキャンバス上に配置された1つのウィジェット。
 *
 * @property type ウィジェットの種類。
 * @property appWidgetId [type]が[WidgetPanel.APPWIDGET]の場合にのみ意味を持つ、
 *   [android.appwidget.AppWidgetHost]が発行したインスタンス固有のID。同じ外部ウィジェットの
 *   プロバイダを複数配置しても区別できるようにするためのもの。それ以外の種類では-1のまま。
 * @property instanceId [type]が[WidgetPanel.APP_SLOT_ICON_ONLY]/[WidgetPanel.APP_SLOT_NAMED]の
 *   場合にのみ意味を持つ、アプリ自身が発行するインスタンス固有のID（[util.allocateNextAppSlotInstanceId]
 *   参照）。同じ種類のAPP SLOTを複数配置しても、どのアプリが割り当てられているかを区別できる
 *   ようにするためのもの。それ以外の種類では-1のまま。
 * @property col 配置されている列（0始まり）。左端・上端のコーナーハンドルでリサイズすると
 *   小数値を取り得る。
 * @property row 配置されている行（0始まり）。[col]と同様に小数値を取り得る。
 * @property colSpan 横方向に占めるセル数。ウィジェットの種類によっては、アイコンサイズ単位での
 *   細かいリサイズに対応するため小数値を取り得る。
 * @property rowSpan 縦方向に占めるセル数。[colSpan]と同様に小数値を取り得る。
 * @property stackId このウィジェットが属するスタック（同じ領域に重ねて、横スワイプで切り替える
 *   ウィジェットのまとまり）のID。同じ値のウィジェット同士は同じ位置・サイズを共有し、一覧での
 *   並び順がスタック内のページ順になる。スタックに属していない場合は-1。
 */
data class PlacedWidget(
    val type: WidgetPanel,
    val appWidgetId: Int = -1,
    val instanceId: Int = -1,
    val col: Float,
    val row: Float,
    val colSpan: Float,
    val rowSpan: Float,
    val stackId: Int = -1
) {
    /**
     * キャンバス上で1つの枠として扱うまとまりのキー。スタックに属していれば、そのスタックの
     * 全員で同じキーになる。属していなければ[instanceKey]。
     */
    val groupKey: String
        get() = if (stackId >= 0) "STACK:$stackId" else instanceKey

    /**
     * この配置済みウィジェットを一意に識別するキー。複数インスタンスを同時配置できる種類
     * （[WidgetPanel.APPWIDGET]、[WidgetPanel.APP_SLOT_ICON_ONLY]、[WidgetPanel.APP_SLOT_NAMED]）は
     * [type]単体ではなく、それぞれのインスタンスIDも含めて区別する。
     */
    val instanceKey: String
        get() = when (type) {
            WidgetPanel.APPWIDGET -> "APPWIDGET:$appWidgetId"
            WidgetPanel.APP_SLOT_ICON_ONLY, WidgetPanel.APP_SLOT_NAMED -> "APP_SLOT:$instanceId"
            else -> type.name
        }

    /** [other]と矩形が重なっているかどうか。 */
    fun overlaps(other: PlacedWidget): Boolean {
        val aRight = col + colSpan
        val aBottom = row + rowSpan
        val bRight = other.col + other.colSpan
        val bBottom = other.row + other.rowSpan
        return col < bRight && aRight > other.col && row < bBottom && aBottom > other.row
    }
}
