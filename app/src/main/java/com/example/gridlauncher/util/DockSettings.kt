package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit

/** DOCKの1ページに並べられるアイコン数の範囲。 */
const val DOCK_MIN_SLOTS_PER_PAGE = 1
const val DOCK_MAX_SLOTS_PER_PAGE = 10

/**
 * DOCKの並び（1ページのアイコン数とページ数）。縦画面・横画面で別々に保存する。
 * アプリの割り当ては先頭から順に各ページへ詰めて表示する（縦横で同じ並びにするかは別に設定する）。
 */
data class DockLayout(val slotsPerPage: Int, val pageCount: Int)

/** 未設定のときの並び（従来の固定表示と同じ：縦画面は4個×2ページ、横画面は8個×1ページ）。 */
private fun defaultDockLayout(isPortrait: Boolean) = if (isPortrait) DockLayout(4, 2) else DockLayout(8, 1)

private fun orientationSuffix(isPortrait: Boolean) = if (isPortrait) "portrait" else "landscape"

/** 画面の向きごとに保存したDOCKの並びを読み込む。 */
fun loadDockLayout(prefs: SharedPreferences, isPortrait: Boolean): DockLayout {
    val default = defaultDockLayout(isPortrait)
    val suffix = orientationSuffix(isPortrait)
    return DockLayout(
        slotsPerPage = prefs.getInt("dock_slots_per_page_$suffix", default.slotsPerPage)
            .coerceIn(DOCK_MIN_SLOTS_PER_PAGE, DOCK_MAX_SLOTS_PER_PAGE),
        pageCount = prefs.getInt("dock_pages_$suffix", default.pageCount).coerceIn(1, SLOT_GRID_MAX_PAGES)
    )
}

/** 画面の向きごとのDOCKの並びを保存する。 */
fun saveDockLayout(prefs: SharedPreferences, isPortrait: Boolean, layout: DockLayout) {
    val suffix = orientationSuffix(isPortrait)
    prefs.edit {
        putInt("dock_slots_per_page_$suffix", layout.slotsPerPage.coerceIn(DOCK_MIN_SLOTS_PER_PAGE, DOCK_MAX_SLOTS_PER_PAGE))
        putInt("dock_pages_$suffix", layout.pageCount.coerceIn(1, SLOT_GRID_MAX_PAGES))
    }
}

/** DOCKのアプリの並び（パッケージ名のカンマ区切り）の保存キー。 */
const val KEY_DOCK_APPS = "dock_apps"

/** 縦画面と横画面で別々に並べる場合の、横画面用のDOCKの並びの保存キー。 */
const val KEY_DOCK_APPS_LANDSCAPE = "dock_apps_landscape"

private const val KEY_SHARE_DOCK_ACROSS_ORIENTATIONS = "dock_share_orientations"

/** 縦画面と横画面でDOCKに同じ並びを使うかどうか（未設定なら同じ並びを使う）。 */
fun loadShareDockAcrossOrientations(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(KEY_SHARE_DOCK_ACROSS_ORIENTATIONS, true)

/** 今の向きで使うDOCKの並びの保存キー。 */
fun dockAppsKeyFor(shareAcrossOrientations: Boolean, isPortrait: Boolean): String =
    if (shareAcrossOrientations || isPortrait) KEY_DOCK_APPS else KEY_DOCK_APPS_LANDSCAPE

/**
 * 縦画面と横画面でDOCKに同じ並びを使うかどうかを切り替えて保存する。
 *
 * - 別々にする場合: 今の並びを横画面用にコピーして始める。
 * - 同じにする場合: [keepLandscape]がtrueなら横画面の並び、falseなら縦画面の並びにそろえる。
 */
fun saveShareDockAcrossOrientations(prefs: SharedPreferences, share: Boolean, keepLandscape: Boolean) {
    if (loadShareDockAcrossOrientations(prefs) == share) return
    val portraitApps = prefs.getString(KEY_DOCK_APPS, "") ?: ""
    prefs.edit {
        if (!share) {
            putString(KEY_DOCK_APPS_LANDSCAPE, portraitApps)
        } else {
            if (keepLandscape) putString(KEY_DOCK_APPS, prefs.getString(KEY_DOCK_APPS_LANDSCAPE, "") ?: "")
            remove(KEY_DOCK_APPS_LANDSCAPE)
        }
        putBoolean(KEY_SHARE_DOCK_ACROSS_ORIENTATIONS, share)
    }
}
