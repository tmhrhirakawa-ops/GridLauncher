package com.example.girdlauncher.model

import android.graphics.drawable.Drawable

/**
 * インストールされているアプリケーションを表すデータクラス。
 *
 * @property label アプリケーションの名前。
 * @property packageName アプリケーションのパッケージ名。
 * @property icon アプリケーションのアイコンDrawable。
 */
data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)
