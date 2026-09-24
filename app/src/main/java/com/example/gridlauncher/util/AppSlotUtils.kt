package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit

private const val AppSlotPackagePrefix = "app_slot_package_"
private const val AppSlotNextIdKey = "app_slot_next_id"

/**
 * 配置済みのAPP SLOTウィジェット（[com.example.gridlauncher.model.WidgetPanel.APP_SLOT_ICON_ONLY]/
 * [com.example.gridlauncher.model.WidgetPanel.APP_SLOT_NAMED]）が、それぞれどのアプリを
 * 割り当てられているかをSharedPreferencesから読み込む。
 *
 * この割り当ては[com.example.gridlauncher.model.PlacedWidget.instanceId]をキーにした、
 * 画面モード（縦画面/横画面）をまたいで共有される永続データで、配置位置・サイズ自体
 * （`widget_layout_v2_*`キー）とは別に保持する。
 *
 * @return インスタンスID→パッケージ名のマップ（未割り当てのインスタンスは含まれない）。
 */
fun loadAppSlotAssignments(prefs: SharedPreferences): Map<Int, String> =
    prefs.all.keys
        .filter { it.startsWith(AppSlotPackagePrefix) }
        .mapNotNull { key ->
            val instanceId = key.removePrefix(AppSlotPackagePrefix).toIntOrNull() ?: return@mapNotNull null
            val packageName = prefs.getString(key, null)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            instanceId to packageName
        }
        .toMap()

/** 指定したAPP SLOTインスタンスに、選択されたアプリを割り当てて保存する。 */
fun saveAppSlotAssignment(prefs: SharedPreferences, instanceId: Int, packageName: String) {
    prefs.edit { putString("$AppSlotPackagePrefix$instanceId", packageName) }
}

/**
 * 指定したAPP SLOTインスタンスの割り当てを消す（「スロットから削除」、またはウィジェット
 * 自体を削除したときの後始末に使う）。
 */
fun clearAppSlotAssignment(prefs: SharedPreferences, instanceId: Int) {
    prefs.edit { remove("$AppSlotPackagePrefix$instanceId") }
}

/**
 * 新しいAPP SLOTウィジェットを追加するときに呼ぶ、未使用のインスタンスIDの発行。
 * 一度発行したIDは（該当ウィジェットを削除しても）再利用しない。
 */
fun allocateNextAppSlotInstanceId(prefs: SharedPreferences): Int {
    val next = prefs.getInt(AppSlotNextIdKey, 0) + 1
    prefs.edit { putInt(AppSlotNextIdKey, next) }
    return next
}
