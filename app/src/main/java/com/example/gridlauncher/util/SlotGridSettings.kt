package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit

/** スロットの並びに手動で指定できる列数・行数の範囲。 */
const val SLOT_GRID_MIN_SPAN = 1
const val SLOT_GRID_MAX_SPAN = 8

/** 設定できるページ数の上限。 */
const val SLOT_GRID_MAX_PAGES = 10

/**
 * スロットの並び・ページ数を設定できるウィジェット。
 *
 * @property keyPrefix 設定の保存キーの接頭辞。
 */
enum class SlotGridSection(val keyPrefix: String) {
    ACCESS_GRID("access_grid"),
    QUICK_ACCESS("quick_access")
}

/**
 * 手動で指定したスロットの並び（列数×行数）。
 */
data class SlotGridSize(val columns: Int, val rows: Int)

private fun gridSizeKey(section: SlotGridSection, mode: WidgetLayoutMode) = "${section.keyPrefix}_size_${mode.name}"
private fun pageCountKey(section: SlotGridSection, mode: WidgetLayoutMode) = "${section.keyPrefix}_pages_${mode.name}"

/**
 * 画面モードごとに保存したスロットの並びを読み込む。
 * 未設定、またはAUTO（ウィジェットの大きさから自動で決める）の場合はnullを返す。
 */
fun loadSlotGridSize(prefs: SharedPreferences, section: SlotGridSection, mode: WidgetLayoutMode): SlotGridSize? {
    val parts = prefs.getString(gridSizeKey(section, mode), null)?.split(",") ?: return null
    val columns = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val rows = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return SlotGridSize(
        columns.coerceIn(SLOT_GRID_MIN_SPAN, SLOT_GRID_MAX_SPAN),
        rows.coerceIn(SLOT_GRID_MIN_SPAN, SLOT_GRID_MAX_SPAN)
    )
}

/** 画面モードごとのスロットの並びを保存する（nullはAUTO）。 */
fun saveSlotGridSize(prefs: SharedPreferences, section: SlotGridSection, mode: WidgetLayoutMode, size: SlotGridSize?) {
    prefs.edit {
        val key = gridSizeKey(section, mode)
        if (size == null) remove(key) else putString(key, "${size.columns},${size.rows}")
    }
}

/**
 * 画面モードごとに保存したページ数を読み込む（未設定なら1ページ）。
 * 実際に表示するページ数は、中身が入っているページ数を下回らないよう呼び出し側で調整する。
 */
fun loadSlotGridPageCount(prefs: SharedPreferences, section: SlotGridSection, mode: WidgetLayoutMode): Int =
    prefs.getInt(pageCountKey(section, mode), 1).coerceIn(1, SLOT_GRID_MAX_PAGES)

/** 画面モードごとのページ数を保存する。 */
fun saveSlotGridPageCount(prefs: SharedPreferences, section: SlotGridSection, mode: WidgetLayoutMode, pageCount: Int) {
    prefs.edit { putInt(pageCountKey(section, mode), pageCount.coerceIn(1, SLOT_GRID_MAX_PAGES)) }
}

/**
 * スロットの中身（空きはnullや空文字）の並び[items]を1ページ[pageSize]個で区切ったとき、
 * 中身が入っている最後のスロットを表示するのに必要なページ数（最低1ページ）。
 */
fun <T> requiredSlotGridPages(items: List<T>, pageSize: Int, isEmpty: (T) -> Boolean): Int {
    if (pageSize <= 0) return 1
    val lastUsedIndex = items.indexOfLast { !isEmpty(it) }
    return if (lastUsedIndex < 0) 1 else lastUsedIndex / pageSize + 1
}
