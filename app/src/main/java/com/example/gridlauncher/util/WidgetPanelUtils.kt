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
