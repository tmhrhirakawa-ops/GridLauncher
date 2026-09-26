package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gridlauncher.model.WidgetPanel

private const val KEY_HIDDEN_WIDGET_PANELS = "hidden_widget_panels"

/**
 * 枠線を非表示にしているウィジェットパネルの集合をSharedPreferencesから読み込む。
 * 未設定の場合はすべて表示（空集合）を返す。
 */
fun loadHiddenWidgetPanels(prefs: SharedPreferences): Set<WidgetPanel> =
    prefs.getString(KEY_HIDDEN_WIDGET_PANELS, null)
        ?.split(",")
        ?.mapNotNull { token -> token.takeIf { it.isNotEmpty() }?.let { name -> runCatching { WidgetPanel.valueOf(name) }.getOrNull() } }
        ?.toSet()
        ?: emptySet()

/** 枠線を非表示にしているウィジェットパネルの集合をSharedPreferencesに保存する。 */
fun saveHiddenWidgetPanels(prefs: SharedPreferences, hiddenPanels: Set<WidgetPanel>) {
    prefs.edit { putString(KEY_HIDDEN_WIDGET_PANELS, hiddenPanels.joinToString(",") { it.name }) }
}

private const val KEY_ACCENT2_WIDGET_PANELS = "accent2_widget_panels"

/**
 * アクセントカラー2を使うウィジェットパネルの集合をSharedPreferencesから読み込む。
 * 未設定の場合はすべてアクセントカラー1（空集合）を返す。
 */
fun loadAccent2WidgetPanels(prefs: SharedPreferences): Set<WidgetPanel> =
    prefs.getString(KEY_ACCENT2_WIDGET_PANELS, null)
        ?.split(",")
        ?.mapNotNull { token -> token.takeIf { it.isNotEmpty() }?.let { name -> runCatching { WidgetPanel.valueOf(name) }.getOrNull() } }
        ?.toSet()
        ?: emptySet()

/** アクセントカラー2を使うウィジェットパネルの集合をSharedPreferencesに保存する。 */
fun saveAccent2WidgetPanels(prefs: SharedPreferences, panels: Set<WidgetPanel>) {
    prefs.edit { putString(KEY_ACCENT2_WIDGET_PANELS, panels.joinToString(",") { it.name }) }
}
