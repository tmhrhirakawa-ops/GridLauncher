package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.model.QuickButtonStyle

/** QUICK ACCESSの並び（ボタンの種類のカンマ区切り。空きスロットは空文字）の保存キー。 */
private const val KEY_QUICK_ACTIONS = "quick_actions"

/** 縦画面と横画面で別々に並べる場合の、横画面用の並びの保存キー。 */
private const val KEY_QUICK_ACTIONS_LANDSCAPE = "quick_actions_landscape"

private const val KEY_SHARE_QUICK_ACTIONS_ACROSS_ORIENTATIONS = "quick_access_share_orientations"

/** 初回起動時のデフォルトのボタン構成。残りのスロットは空きになる。 */
private val DEFAULT_QUICK_ACTIONS: List<QuickActionId?> = listOf(
    QuickActionId.WIFI,
    QuickActionId.DISPLAY,
    QuickActionId.BLUETOOTH,
    QuickActionId.DEVELOP,
    QuickActionId.COLOR,
    QuickActionId.THEME
)

/** 縦画面と横画面でQUICK ACCESSに同じ並びを使うかどうか（未設定なら同じ並びを使う）。 */
fun loadShareQuickActionsAcrossOrientations(prefs: SharedPreferences): Boolean =
    prefs.getBoolean(KEY_SHARE_QUICK_ACTIONS_ACROSS_ORIENTATIONS, true)

/** 今の向きで使うQUICK ACCESSの並びの保存キー。 */
fun quickActionsKeyFor(shareAcrossOrientations: Boolean, isPortrait: Boolean): String =
    if (shareAcrossOrientations || isPortrait) KEY_QUICK_ACTIONS else KEY_QUICK_ACTIONS_LANDSCAPE

/**
 * QUICK ACCESSのスロット構成をSharedPreferencesから読み込みます（null=空きスロット）。
 * 縦画面用の並び（[KEY_QUICK_ACTIONS]）が保存されていない場合は [DEFAULT_QUICK_ACTIONS] を初期値として使う。
 * スロットの数はウィジェットの大きさ・ページ数で変わるため、ここでは長さを揃えない。
 */
fun loadQuickActionSlots(prefs: SharedPreferences, key: String = KEY_QUICK_ACTIONS): List<QuickActionId?> {
    val stored = prefs.getString(key, null) ?: prefs.getString(KEY_QUICK_ACTIONS, null)
    return if (stored == null) {
        DEFAULT_QUICK_ACTIONS
    } else {
        stored.split(",").map { token ->
            token.takeIf { it.isNotEmpty() }?.let { name -> runCatching { QuickActionId.valueOf(name) }.getOrNull() }
        }
    }
}

/** QUICK ACCESSのスロット構成をSharedPreferencesに保存します。 */
fun saveQuickActionSlots(prefs: SharedPreferences, key: String, slots: List<QuickActionId?>) {
    prefs.edit { putString(key, slots.joinToString(",") { it?.name ?: "" }) }
}

/**
 * 縦画面と横画面でQUICK ACCESSに同じ並びを使うかどうかを切り替えて保存する。
 *
 * - 別々にする場合: 今の並びを横画面用にコピーして始める。
 * - 同じにする場合: [keepLandscape]がtrueなら横画面の並び、falseなら縦画面の並びにそろえる。
 */
fun saveShareQuickActionsAcrossOrientations(prefs: SharedPreferences, share: Boolean, keepLandscape: Boolean) {
    if (loadShareQuickActionsAcrossOrientations(prefs) == share) return
    val portraitSlots = loadQuickActionSlots(prefs, KEY_QUICK_ACTIONS)
    if (!share) {
        saveQuickActionSlots(prefs, KEY_QUICK_ACTIONS_LANDSCAPE, portraitSlots)
        prefs.edit { putBoolean(KEY_SHARE_QUICK_ACTIONS_ACROSS_ORIENTATIONS, false) }
    } else {
        if (keepLandscape) {
            saveQuickActionSlots(prefs, KEY_QUICK_ACTIONS, loadQuickActionSlots(prefs, KEY_QUICK_ACTIONS_LANDSCAPE))
        }
        prefs.edit {
            remove(KEY_QUICK_ACTIONS_LANDSCAPE)
            putBoolean(KEY_SHARE_QUICK_ACTIONS_ACROSS_ORIENTATIONS, true)
        }
    }
}

private const val KEY_QUICK_BUTTON_STYLE = "quick_access_button_style"

/** QUICK ACCESSのボタンの表示スタイルを読み込む（未設定なら従来通りの名前のみ）。 */
fun loadQuickButtonStyle(prefs: SharedPreferences): QuickButtonStyle =
    prefs.getString(KEY_QUICK_BUTTON_STYLE, null)
        ?.let { name -> runCatching { QuickButtonStyle.valueOf(name) }.getOrNull() }
        ?: QuickButtonStyle.NAME_ONLY

/** QUICK ACCESSのボタンの表示スタイルを保存する。 */
fun saveQuickButtonStyle(prefs: SharedPreferences, style: QuickButtonStyle) {
    prefs.edit { putString(KEY_QUICK_BUTTON_STYLE, style.name) }
}
