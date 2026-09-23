package com.example.girdlauncher.util

import android.appwidget.AppWidgetHost
import android.content.Context

/**
 * 他アプリのAppWidgetをホストするための[AppWidgetHost]をアプリ全体で1つだけ保持する。
 *
 * [AppWidgetHost]はプロセス内で同じホストIDに対して1つのインスタンスだけを使うべきものなので、
 * ここでシングルトンとして保持し、[ensureInitialized]で初回だけ生成する。
 * `startListening()`/`stopListening()`の呼び出しはライフサイクルに応じて呼び出し側
 * （[com.example.girdlauncher.ui.screens.CyberLauncherScreen]）が行う。
 */
object AppWidgetHostManager {
    // このランチャーアプリを識別する、アプリ固有のホストID（他の値と衝突しなければ何でもよい）
    private const val HOST_ID = 1024

    lateinit var host: AppWidgetHost
        private set

    fun ensureInitialized(context: Context) {
        if (!::host.isInitialized) {
            host = AppWidgetHost(context.applicationContext, HOST_ID)
        }
    }
}
