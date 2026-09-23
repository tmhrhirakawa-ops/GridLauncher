package com.example.girdlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.girdlauncher.model.PlacedWidget
import com.example.girdlauncher.model.WidgetPanel

/**
 * ウィジェットキャンバスの画面モード。モードごとにグリッドの寸法とデフォルト配置を持つ。
 *
 * @property columns グリッドの列数。
 * @property rows グリッドの行数。
 * @property defaultWidgets 初回（未保存時）のデフォルト配置。
 */
// キャンバスグリッドの細かさの倍率。値を大きくするほど、移動・リサイズのスナップ単位が細かくなり
// 配置の自由度が上がる（以前は等倍で、デフォルト配置がグリッドを隙間なく埋め尽くしてしまい、
// 少し動かす・広げるだけで必ず他のウィジェットと重なってしまっていたため導入した）
private const val GRID_SCALE = 3

enum class WidgetLayoutMode(val columns: Int, val rows: Int, val defaultWidgets: List<PlacedWidget>) {
    SMALL_PORTRAIT(
        columns = 2 * GRID_SCALE,
        rows = 6 * GRID_SCALE,
        defaultWidgets = listOf(
            PlacedWidget(WidgetPanel.ACCESS_GRID, col = 0f, row = 0f, colSpan = 2f * GRID_SCALE, rowSpan = 3f * GRID_SCALE),
            PlacedWidget(WidgetPanel.DEVICE_STATUS, col = 0f, row = 3f * GRID_SCALE, colSpan = 1f * GRID_SCALE, rowSpan = 3f * GRID_SCALE),
            PlacedWidget(WidgetPanel.QUICK_ACCESS, col = 1f * GRID_SCALE, row = 3f * GRID_SCALE, colSpan = 1f * GRID_SCALE, rowSpan = 3f * GRID_SCALE)
        )
    ),
    LARGE_PORTRAIT(
        columns = 4 * GRID_SCALE,
        rows = 7 * GRID_SCALE,
        defaultWidgets = listOf(
            PlacedWidget(WidgetPanel.ACCESS_GRID, col = 0f, row = 0f, colSpan = 4f * GRID_SCALE, rowSpan = 3f * GRID_SCALE),
            PlacedWidget(WidgetPanel.CALENDAR, col = 0f, row = 3f * GRID_SCALE, colSpan = 2f * GRID_SCALE, rowSpan = 4f * GRID_SCALE),
            PlacedWidget(WidgetPanel.DEVICE_STATUS, col = 2f * GRID_SCALE, row = 3f * GRID_SCALE, colSpan = 2f * GRID_SCALE, rowSpan = 2f * GRID_SCALE),
            PlacedWidget(WidgetPanel.QUICK_ACCESS, col = 2f * GRID_SCALE, row = 5f * GRID_SCALE, colSpan = 2f * GRID_SCALE, rowSpan = 2f * GRID_SCALE)
        )
    ),
    LANDSCAPE(
        columns = 4 * GRID_SCALE,
        rows = 4 * GRID_SCALE,
        defaultWidgets = listOf(
            PlacedWidget(WidgetPanel.ACCESS_GRID, col = 0f, row = 0f, colSpan = 2f * GRID_SCALE, rowSpan = 4f * GRID_SCALE),
            PlacedWidget(WidgetPanel.CALENDAR, col = 2f * GRID_SCALE, row = 0f, colSpan = 2f * GRID_SCALE, rowSpan = 2f * GRID_SCALE),
            PlacedWidget(WidgetPanel.DEVICE_STATUS, col = 2f * GRID_SCALE, row = 2f * GRID_SCALE, colSpan = 1f * GRID_SCALE, rowSpan = 2f * GRID_SCALE),
            PlacedWidget(WidgetPanel.QUICK_ACCESS, col = 3f * GRID_SCALE, row = 2f * GRID_SCALE, colSpan = 1f * GRID_SCALE, rowSpan = 2f * GRID_SCALE)
        )
    )
}

// v2: グリッドの細かさ（GRID_SCALE）を導入した際に座標の意味が変わったため、キーを分けて
// 旧スケールで保存された配置を誤って読み込まないようにしている（該当端末は一度だけ
// デフォルト配置に戻る）
private fun widgetLayoutKey(mode: WidgetLayoutMode) = "widget_layout_v2_${mode.name}"

/**
 * 指定した画面モードのウィジェット配置をSharedPreferencesから読み込む。
 * 未保存の場合は[WidgetLayoutMode.defaultWidgets]を返す。
 */
fun loadPlacedWidgets(prefs: SharedPreferences, mode: WidgetLayoutMode): List<PlacedWidget> {
    val stored = prefs.getString(widgetLayoutKey(mode), null) ?: return mode.defaultWidgets
    if (stored.isEmpty()) return emptyList()
    return stored.split(";").mapNotNull { entry ->
        val parts = entry.split(":")
        // 6フィールド目（appWidgetId）はAPPWIDGET対応で後から追加したもの。
        // 5フィールドの旧形式もそのまま読めるようにし、既存の配置がリセットされないようにする
        if (parts.size != 5 && parts.size != 6) return@mapNotNull null
        val type = runCatching { WidgetPanel.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val col = parts[1].toFloatOrNull() ?: return@mapNotNull null
        val row = parts[2].toFloatOrNull() ?: return@mapNotNull null
        val colSpan = parts[3].toFloatOrNull() ?: return@mapNotNull null
        val rowSpan = parts[4].toFloatOrNull() ?: return@mapNotNull null
        val appWidgetId = if (parts.size == 6) (parts[5].toIntOrNull() ?: -1) else -1
        PlacedWidget(type = type, appWidgetId = appWidgetId, col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
    }
}

/** 指定した画面モードのウィジェット配置をSharedPreferencesに保存する。 */
fun savePlacedWidgets(prefs: SharedPreferences, mode: WidgetLayoutMode, widgets: List<PlacedWidget>) {
    val serialized = widgets.joinToString(";") { "${it.type.name}:${it.col}:${it.row}:${it.colSpan}:${it.rowSpan}:${it.appWidgetId}" }
    prefs.edit { putString(widgetLayoutKey(mode), serialized) }
}

/**
 * まだ何も配置されていない領域から、6×6（入らなければ3×3、それも入らなければ1×1）が
 * 収まる最初のセルを行優先で探す。見つからなければnull。
 *
 * @return 見つかった場合 (col, row, colSpan, rowSpan) の組。
 */
fun findFreeGridSlot(placed: List<PlacedWidget>, columns: Int, rows: Int): IntArray? {
    for (span in intArrayOf(2 * GRID_SCALE, GRID_SCALE, 1)) {
        if (span > columns || span > rows) continue
        for (row in 0..rows - span) {
            for (col in 0..columns - span) {
                val fits = placed.none { existing ->
                    col < existing.col + existing.colSpan &&
                        col + span > existing.col &&
                        row < existing.row + existing.rowSpan &&
                        row + span > existing.row
                }
                if (fits) return intArrayOf(col, row, span, span)
            }
        }
    }
    return null
}
