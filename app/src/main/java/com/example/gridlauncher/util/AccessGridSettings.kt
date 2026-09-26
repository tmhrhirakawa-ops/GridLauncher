package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit

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
