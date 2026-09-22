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
enum class WidgetLayoutMode(val columns: Int, val rows: Int, val defaultWidgets: List<PlacedWidget>) {
    SMALL_PORTRAIT(
        columns = 2,
        rows = 6,
        defaultWidgets = listOf(
            PlacedWidget(WidgetPanel.ACCESS_GRID, col = 0, row = 0, colSpan = 2f, rowSpan = 3f),
            PlacedWidget(WidgetPanel.DEVICE_STATUS, col = 0, row = 3, colSpan = 1f, rowSpan = 3f),
            PlacedWidget(WidgetPanel.QUICK_ACCESS, col = 1, row = 3, colSpan = 1f, rowSpan = 3f)
        )
    ),
    LARGE_PORTRAIT(
        columns = 4,
        rows = 7,
        defaultWidgets = listOf(
            PlacedWidget(WidgetPanel.ACCESS_GRID, col = 0, row = 0, colSpan = 4f, rowSpan = 3f),
            PlacedWidget(WidgetPanel.CALENDAR, col = 0, row = 3, colSpan = 2f, rowSpan = 4f),
            PlacedWidget(WidgetPanel.DEVICE_STATUS, col = 2, row = 3, colSpan = 2f, rowSpan = 2f),
            PlacedWidget(WidgetPanel.QUICK_ACCESS, col = 2, row = 5, colSpan = 2f, rowSpan = 2f)
        )
    ),
    LANDSCAPE(
        columns = 4,
        rows = 4,
        defaultWidgets = listOf(
            PlacedWidget(WidgetPanel.ACCESS_GRID, col = 0, row = 0, colSpan = 2f, rowSpan = 4f),
            PlacedWidget(WidgetPanel.CALENDAR, col = 2, row = 0, colSpan = 2f, rowSpan = 2f),
            PlacedWidget(WidgetPanel.DEVICE_STATUS, col = 2, row = 2, colSpan = 1f, rowSpan = 2f),
            PlacedWidget(WidgetPanel.QUICK_ACCESS, col = 3, row = 2, colSpan = 1f, rowSpan = 2f)
        )
    )
}

private fun widgetLayoutKey(mode: WidgetLayoutMode) = "widget_layout_${mode.name}"

/**
 * 指定した画面モードのウィジェット配置をSharedPreferencesから読み込む。
 * 未保存の場合は[WidgetLayoutMode.defaultWidgets]を返す。
 */
fun loadPlacedWidgets(prefs: SharedPreferences, mode: WidgetLayoutMode): List<PlacedWidget> {
    val stored = prefs.getString(widgetLayoutKey(mode), null) ?: return mode.defaultWidgets
    if (stored.isEmpty()) return emptyList()
    return stored.split(";").mapNotNull { entry ->
        val parts = entry.split(":")
        if (parts.size != 5) return@mapNotNull null
        val type = runCatching { WidgetPanel.valueOf(parts[0]) }.getOrNull() ?: return@mapNotNull null
        val col = parts[1].toIntOrNull() ?: return@mapNotNull null
        val row = parts[2].toIntOrNull() ?: return@mapNotNull null
        val colSpan = parts[3].toFloatOrNull() ?: return@mapNotNull null
        val rowSpan = parts[4].toFloatOrNull() ?: return@mapNotNull null
        PlacedWidget(type, col, row, colSpan, rowSpan)
    }
}

/** 指定した画面モードのウィジェット配置をSharedPreferencesに保存する。 */
fun savePlacedWidgets(prefs: SharedPreferences, mode: WidgetLayoutMode, widgets: List<PlacedWidget>) {
    val serialized = widgets.joinToString(";") { "${it.type.name}:${it.col}:${it.row}:${it.colSpan}:${it.rowSpan}" }
    prefs.edit { putString(widgetLayoutKey(mode), serialized) }
}

/**
 * まだ何も配置されていない領域から、2×2（入らなければ1×1）が収まる最初のセルを
 * 行優先で探す。見つからなければnull。
 *
 * @return 見つかった場合 (col, row, colSpan, rowSpan) の組。
 */
fun findFreeGridSlot(placed: List<PlacedWidget>, columns: Int, rows: Int): IntArray? {
    for (span in intArrayOf(2, 1)) {
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
