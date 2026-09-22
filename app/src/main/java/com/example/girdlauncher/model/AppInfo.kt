package com.example.girdlauncher.model

import android.graphics.drawable.Drawable

/**
 * インストールされているアプリケーションを表すデータクラス。
 *
 * @property label アプリケーションの名前。
 * @property packageName アプリケーションのパッケージ名。
 * @property icon アプリケーションのアイコンDrawable。デュオトーン加工に使う対象レイヤーのみを
 *   抽出し、表示に必要な解像度までダウンサンプリング済みのもの（[com.example.girdlauncher.util.getInstalledApps]参照）。
 * @property iconIsMonochrome [icon]がAndroid 13+のモノクロレイヤー由来かどうか。加工方法の選択に使う。
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val iconIsMonochrome: Boolean = false
)
