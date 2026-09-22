package com.example.girdlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.girdlauncher.model.QuickActionId

/**
 * QUICK ACCESSに配置できるスロット数の上限。設定できる機能（[QuickActionId]）の総数と同じにする
 * （機能が増えれば自動的にこの上限も増える）。実際にウィジェット上に表示される列数・行数は
 * ウィジェットのサイズに応じてこれ以下の範囲で自動調整される。
 */
val QUICK_ACTION_CAPACITY: Int = QuickActionId.entries.size

private const val KEY_QUICK_ACTIONS = "quick_actions"

/** 初回起動時のデフォルトのボタン構成。残りのスロットは空きになる。 */
private val DEFAULT_QUICK_ACTIONS: List<QuickActionId?> = listOf(
    QuickActionId.WIFI,
    QuickActionId.DISPLAY,
    QuickActionId.BLUETOOTH,
    QuickActionId.DEVELOP,
    QuickActionId.COLOR,
    QuickActionId.THEME
)

/**
 * QUICK ACCESSのスロット構成をSharedPreferencesから読み込みます。
 * 保存されていない場合は [DEFAULT_QUICK_ACTIONS] を初期値として使い、
 * 常に [QUICK_ACTION_CAPACITY] 個ぴったりのリスト（空きスロットはnull）を返します。
 */
fun loadQuickActionSlots(prefs: SharedPreferences): List<QuickActionId?> {
    val stored = prefs.getString(KEY_QUICK_ACTIONS, null)
    val ids: List<QuickActionId?> = if (stored == null) {
        DEFAULT_QUICK_ACTIONS
    } else {
        stored.split(",").map { token ->
            token.takeIf { it.isNotEmpty() }?.let { name -> runCatching { QuickActionId.valueOf(name) }.getOrNull() }
        }
    }
    val slots = ids.toMutableList()
    while (slots.size < QUICK_ACTION_CAPACITY) slots.add(null)
    return slots.take(QUICK_ACTION_CAPACITY)
}

/** QUICK ACCESSのスロット構成をSharedPreferencesに保存します。 */
fun saveQuickActionSlots(prefs: SharedPreferences, slots: List<QuickActionId?>) {
    prefs.edit { putString(KEY_QUICK_ACTIONS, slots.joinToString(",") { it?.name ?: "" }) }
}
