package com.example.girdlauncher.model

import android.graphics.drawable.Drawable

/**
 * インストールされているアプリケーションを表すデータクラス。
 *
 * @property label アプリケーションの名前。
 * @property packageName アプリケーションのパッケージ名。
 * @property icon アプリケーションのアイコンDrawable。
 * @property category アプリのカテゴリ情報 (ApplicationInfo.CATEGORY_XXX)。
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable,
    val category: Int = android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED
)
