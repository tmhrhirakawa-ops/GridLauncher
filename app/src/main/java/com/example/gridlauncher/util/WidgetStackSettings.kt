package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit

/** ウィジェットスタックの自動切り替えで選べる間隔（秒）。 */
val StackAutoRotateIntervalOptions = listOf(5, 10, 30, 60, 300)

/** 自動切り替えの間隔の初期値（秒）。 */
private const val DEFAULT_STACK_AUTO_ROTATE_INTERVAL = 10

private const val KEY_STACK_AUTO_ROTATE = "stack_auto_rotate"
private const val KEY_STACK_AUTO_ROTATE_INTERVAL = "stack_auto_rotate_interval_sec"

/**
 * ウィジェットスタックの自動切り替えの設定。
 *
 * @property enabled 自動で次のウィジェットに切り替えるかどうか。
 * @property intervalSeconds 切り替えの間隔（秒）。
 */
data class StackAutoRotateSettings(val enabled: Boolean, val intervalSeconds: Int)

/** ウィジェットスタックの自動切り替えの設定を読み込む（未設定ならオフ・10秒）。 */
fun loadStackAutoRotateSettings(prefs: SharedPreferences): StackAutoRotateSettings = StackAutoRotateSettings(
    enabled = prefs.getBoolean(KEY_STACK_AUTO_ROTATE, false),
    intervalSeconds = prefs.getInt(KEY_STACK_AUTO_ROTATE_INTERVAL, DEFAULT_STACK_AUTO_ROTATE_INTERVAL)
        .takeIf { it in StackAutoRotateIntervalOptions } ?: DEFAULT_STACK_AUTO_ROTATE_INTERVAL
)

/** ウィジェットスタックの自動切り替えの設定を保存する。 */
fun saveStackAutoRotateSettings(prefs: SharedPreferences, settings: StackAutoRotateSettings) {
    prefs.edit {
        putBoolean(KEY_STACK_AUTO_ROTATE, settings.enabled)
        putInt(KEY_STACK_AUTO_ROTATE_INTERVAL, settings.intervalSeconds)
    }
}

/** 間隔（秒）の表示用の文字（例: 30秒、1分）。 */
fun stackAutoRotateIntervalLabel(seconds: Int): String =
    if (seconds >= 60 && seconds % 60 == 0) "${seconds / 60}分" else "${seconds}秒"
