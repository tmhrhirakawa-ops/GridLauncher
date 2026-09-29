package com.example.gridlauncher.util

import android.accessibilityservice.AccessibilityService
import android.content.SharedPreferences
import android.os.Build
import androidx.core.content.edit

/**
 * ホーム画面の何もないところで行うジェスチャー（長押しはメニュー表示のため、割り当ての対象外）。
 *
 * @property label 表示する名前。
 */
enum class HomeGesture(val label: String) {
    SWIPE_UP("上スワイプ"),
    SWIPE_DOWN("下スワイプ"),
    DOUBLE_TAP("ダブルタップ")
}

/**
 * ジェスチャーに割り当てるアクション。
 *
 * @property label 表示する名前。
 * @property globalAction ユーザー補助（アクセシビリティ）サービスで実行する操作（[AccessibilityService]の
 *   `GLOBAL_ACTION_*`）。nullならアプリ内で完結する操作。
 */
enum class GestureAction(val label: String, val globalAction: Int? = null) {
    NONE("なし"),
    ALL_APPS("ALL APPS を開く"),
    NOTIFICATIONS("通知パネルを開く", AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS),
    QUICK_SETTINGS("クイック設定を開く", AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS),
    LOCK_SCREEN("画面をオフにする", AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN),
    RECENTS("最近使ったアプリ", AccessibilityService.GLOBAL_ACTION_RECENTS),
    POWER_MENU("電源メニューを開く", AccessibilityService.GLOBAL_ACTION_POWER_DIALOG),
    CUSTOMIZE("カスタマイズ画面を開く"),
    LAUNCH_APP("アプリを起動");

    companion object {
        /** この端末で選べるアクション（画面オフは Android 9 以降のみ）。 */
        fun availableEntries(): List<GestureAction> =
            entries.filter { it != LOCK_SCREEN || Build.VERSION.SDK_INT >= Build.VERSION_CODES.P }
    }
}

/**
 * ジェスチャーへのアクションの割り当て。
 *
 * @property action 実行するアクション。
 * @property packageName [GestureAction.LAUNCH_APP]のときに起動するアプリ。
 */
data class GestureBinding(val action: GestureAction, val packageName: String? = null)

/** 初期の割り当て（上スワイプで ALL APPS。PROでない場合もこの割り当てで動く）。 */
val DefaultGestureBindings: Map<HomeGesture, GestureBinding> = mapOf(
    HomeGesture.SWIPE_UP to GestureBinding(GestureAction.ALL_APPS),
    HomeGesture.SWIPE_DOWN to GestureBinding(GestureAction.NONE),
    HomeGesture.DOUBLE_TAP to GestureBinding(GestureAction.NONE)
)

private fun gestureKey(gesture: HomeGesture) = "gesture_${gesture.name}"

/** 保存したジェスチャーの割り当てを読み込む（未設定のジェスチャーは初期の割り当て）。 */
fun loadGestureBindings(prefs: SharedPreferences): Map<HomeGesture, GestureBinding> =
    HomeGesture.entries.associateWith { gesture ->
        val stored = prefs.getString(gestureKey(gesture), null)
        val action = stored?.substringBefore(":")?.let { name -> GestureAction.entries.firstOrNull { it.name == name } }
        if (action == null) {
            DefaultGestureBindings.getValue(gesture)
        } else {
            GestureBinding(action, stored.substringAfter(":", "").takeIf { it.isNotEmpty() })
        }
    }

/** ジェスチャーの割り当てを保存する。 */
fun saveGestureBinding(prefs: SharedPreferences, gesture: HomeGesture, binding: GestureBinding) {
    prefs.edit { putString(gestureKey(gesture), "${binding.action.name}:${binding.packageName.orEmpty()}") }
}
