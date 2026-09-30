package com.example.gridlauncher.model

import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable

/**
 * インストールされているアプリケーションを表すデータクラス。
 *
 * @property label アプリケーションの名前。
 * @property packageName アプリケーションのパッケージ名。
 * @property icon アプリケーションのアイコンDrawable。デュオトーン加工に使う対象レイヤーのみを
 *   抽出し、表示に必要な解像度までダウンサンプリング済みのもの（[com.example.gridlauncher.util.getInstalledApps]参照）。
 * @property iconIsMonochrome [icon]がAndroid 13+のモノクロレイヤー由来かどうか。加工方法の選択に使う。
 * @property firstInstallTime アプリが最初にインストールされた日時（エポックミリ秒）。
 *   ALL APPS・SELECT APPをインストールした順に並べるのに使う。取得できない場合は0。
 * @property category アプリ自身が申告しているカテゴリ（[ApplicationInfo.category]。ゲーム・音楽など）。
 *   ALL APPS・SELECT APPをカテゴリ順に並べるのに使う。申告していなければ[ApplicationInfo.CATEGORY_UNDEFINED]。
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val iconIsMonochrome: Boolean = false,
    val firstInstallTime: Long = 0L,
    val category: Int = ApplicationInfo.CATEGORY_UNDEFINED
)
