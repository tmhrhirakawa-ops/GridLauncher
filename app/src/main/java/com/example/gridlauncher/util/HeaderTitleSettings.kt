package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * ヘッダー中央（横画面・縦画面（大））に表示する3段の文字。カスタマイズ画面で書き換えられる。
 *
 * @property top 上段の小さい文字（アクセントカラー）。
 * @property main 中段の大きい文字。
 * @property bottom 下段の小さい文字（薄い文字）。
 */
data class HeaderTitle(val top: String, val main: String, val bottom: String) {
    companion object {
        /** 初期値（「元に戻す」で戻す値）。 */
        val Default = HeaderTitle(top = "ACCESS GRANTED", main = "THE GRID OS", bottom = "USER@GRIDLAUNCHER")
    }
}

private const val KEY_TOP = "header_title_top"
private const val KEY_MAIN = "header_title_main"
private const val KEY_BOTTOM = "header_title_bottom"

/** 保存したヘッダー中央の文字を読み込む（未設定の段は初期値）。 */
fun loadHeaderTitle(prefs: SharedPreferences): HeaderTitle = HeaderTitle(
    top = prefs.getString(KEY_TOP, null) ?: HeaderTitle.Default.top,
    main = prefs.getString(KEY_MAIN, null) ?: HeaderTitle.Default.main,
    bottom = prefs.getString(KEY_BOTTOM, null) ?: HeaderTitle.Default.bottom
)

/** ヘッダー中央の文字を保存する。 */
fun saveHeaderTitle(prefs: SharedPreferences, title: HeaderTitle) {
    prefs.edit {
        putString(KEY_TOP, title.top)
        putString(KEY_MAIN, title.main)
        putString(KEY_BOTTOM, title.bottom)
    }
}
