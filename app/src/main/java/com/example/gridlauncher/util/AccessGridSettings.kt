package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit

/** APP LISTに手動で指定できる列数・行数の範囲。 */
const val ACCESS_GRID_MIN_SPAN = 1
const val ACCESS_GRID_MAX_SPAN = 8

/** APP LISTに設定できるページ数の上限。 */
const val ACCESS_GRID_MAX_PAGES = 10

/**
 * APP LISTに手動で指定したアイコンの並び（列数×行数）。
 */
data class AccessGridSize(val columns: Int, val rows: Int)

private fun gridSizeKey(mode: WidgetLayoutMode) = "access_grid_size_${mode.name}"
private fun pageCountKey(mode: WidgetLayoutMode) = "access_grid_pages_${mode.name}"

/**
 * 画面モードごとに保存したAPP LISTのアイコンの並びを読み込む。
 * 未設定、またはAUTO（ウィジェットの大きさから自動で決める）の場合はnullを返す。
 */
fun loadAccessGridSize(prefs: SharedPreferences, mode: WidgetLayoutMode): AccessGridSize? {
    val parts = prefs.getString(gridSizeKey(mode), null)?.split(",") ?: return null
    val columns = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val rows = parts.getOrNull(1)?.toIntOrNull() ?: return null
    return AccessGridSize(
        columns.coerceIn(ACCESS_GRID_MIN_SPAN, ACCESS_GRID_MAX_SPAN),
        rows.coerceIn(ACCESS_GRID_MIN_SPAN, ACCESS_GRID_MAX_SPAN)
    )
}

/** 画面モードごとのAPP LISTのアイコンの並びを保存する（nullはAUTO）。 */
fun saveAccessGridSize(prefs: SharedPreferences, mode: WidgetLayoutMode, size: AccessGridSize?) {
    prefs.edit {
        if (size == null) remove(gridSizeKey(mode)) else putString(gridSizeKey(mode), "${size.columns},${size.rows}")
    }
}

/**
 * 画面モードごとに保存したAPP LISTのページ数を読み込む（未設定なら1ページ）。
 * 実際に表示するページ数は、アプリが入っているページ数を下回らないよう呼び出し側で調整する。
 */
fun loadAccessGridPageCount(prefs: SharedPreferences, mode: WidgetLayoutMode): Int =
    prefs.getInt(pageCountKey(mode), 1).coerceIn(1, ACCESS_GRID_MAX_PAGES)

/** 画面モードごとのAPP LISTのページ数を保存する。 */
fun saveAccessGridPageCount(prefs: SharedPreferences, mode: WidgetLayoutMode, pageCount: Int) {
    prefs.edit { putInt(pageCountKey(mode), pageCount.coerceIn(1, ACCESS_GRID_MAX_PAGES)) }
}

/**
 * スロットの中身（空きはnullや空文字）の並び[items]を1ページ[pageSize]個で区切ったとき、
 * 中身が入っている最後のスロットを表示するのに必要なページ数（最低1ページ）。
 */
fun <T> requiredAccessGridPages(items: List<T>, pageSize: Int, isEmpty: (T) -> Boolean): Int {
    if (pageSize <= 0) return 1
    val lastUsedIndex = items.indexOfLast { !isEmpty(it) }
    return if (lastUsedIndex < 0) 1 else lastUsedIndex / pageSize + 1
}

/** APP LISTの並び（パッケージ名・フォルダのスロット値のカンマ区切り）の保存キー。 */
const val KEY_GRID_APPS = "grid_apps"

/** 縦画面と横画面で別々に並べる場合の、横画面用の並びの保存キー。 */
const val KEY_GRID_APPS_LANDSCAPE = "grid_apps_landscape"

private const val KEY_SHARE_GRID_ACROSS_ORIENTATIONS = "access_grid_share_orientations"

/** 縦画面と横画面でAPP LISTに同じ並びを使うかどうか（未設定なら同じ並びを使う）。 */
fun loadShareGridAcrossOrientations(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(KEY_SHARE_GRID_ACROSS_ORIENTATIONS, true)

/** 今の向きで使うAPP LISTの並びの保存キー。 */
fun gridAppsKeyFor(shareAcrossOrientations: Boolean, isPortrait: Boolean): String =
    if (shareAcrossOrientations || isPortrait) KEY_GRID_APPS else KEY_GRID_APPS_LANDSCAPE

/**
 * 縦画面と横画面でAPP LISTに同じ並びを使うかどうかを切り替えて保存する。
 *
 * - 別々にする場合: 今の並びを横画面用にコピーして始める。中のフォルダは横画面用に複製し、
 *   片方でフォルダの中身を変えたり削除したりしても、もう片方に影響しないようにする。
 * - 同じにする場合: [keepLandscape]がtrueなら横画面の並び、falseなら縦画面の並びにそろえ、
 *   どちらの並びからも使われなくなったフォルダは削除する。
 */
fun saveShareGridAcrossOrientations(prefs: SharedPreferences, share: Boolean, keepLandscape: Boolean) {
    if (loadShareGridAcrossOrientations(prefs) == share) return
    val portraitSlots = prefs.getString(KEY_GRID_APPS, "") ?: ""
    if (!share) {
        val folders = loadFolders(prefs)
        val landscapeSlots = portraitSlots.split(",").joinToString(",") { value ->
            val folder = folderIdFromSlotValue(value)?.let { folders[it] } ?: return@joinToString value
            val copied = createFolder(prefs, folder.name).copy(packageNames = folder.packageNames)
            saveFolder(prefs, copied)
            folderSlotValue(copied.id)
        }
        prefs.edit {
            putString(KEY_GRID_APPS_LANDSCAPE, landscapeSlots)
            putBoolean(KEY_SHARE_GRID_ACROSS_ORIENTATIONS, false)
        }
    } else {
        val landscapeSlots = prefs.getString(KEY_GRID_APPS_LANDSCAPE, "") ?: ""
        val keptSlots = if (keepLandscape) landscapeSlots else portraitSlots
        val discardedSlots = if (keepLandscape) portraitSlots else landscapeSlots
        val keptFolderIds = keptSlots.split(",").mapNotNull { folderIdFromSlotValue(it) }.toSet()
        discardedSlots.split(",")
            .mapNotNull { folderIdFromSlotValue(it) }
            .filter { it !in keptFolderIds }
            .forEach { deleteFolder(prefs, it) }
        prefs.edit {
            putString(KEY_GRID_APPS, keptSlots)
            remove(KEY_GRID_APPS_LANDSCAPE)
            putBoolean(KEY_SHARE_GRID_ACROSS_ORIENTATIONS, true)
        }
    }
}
