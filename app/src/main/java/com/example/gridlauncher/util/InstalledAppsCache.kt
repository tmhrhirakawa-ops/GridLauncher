package com.example.gridlauncher.util

import android.content.pm.PackageManager
import com.example.gridlauncher.model.AppInfo
import java.util.Locale

/**
 * インストール済みアプリの一覧（アイコンの加工を含む）の、アプリのプロセスが生きている間のキャッシュ。
 *
 * ホーム画面は、画面の回転や折りたたみの開閉などのたびに作り直されるため、そのたびに全アプリの
 * 情報取得とアイコンの加工をメインスレッドでやり直すと、切り替えがもたつき、電池も余計に使う。
 * そこで一覧を使い回し、前回から変わったアプリ（インストール・更新・削除）だけを
 * [PackageManager.getChangedPackages]で調べて取り直す。端末の言語が変わった場合は、アプリ名が
 * 変わるため全部取り直す。
 */
object InstalledAppsCache {
    private var cachedApps: List<AppInfo>? = null
    private var cachedLocale: Locale? = null

    // 最後に変更を確かめた時点の、端末のパッケージ変更の通し番号
    private var sequenceNumber = 0

    /** インストール済みアプリの一覧を返す（キャッシュがあれば、変わったアプリだけ取り直して使う）。 */
    fun get(packageManager: PackageManager): List<AppInfo> {
        val locale = Locale.getDefault()
        val cached = cachedApps
        if (cached == null || locale != cachedLocale) return reload(packageManager, locale)

        val changed = packageManager.getChangedPackages(sequenceNumber) ?: return cached
        val changedPackages = changed.packageNames.toSet()
        val apps = cached.filterNot { it.packageName in changedPackages }.toMutableList()
        changedPackages.forEach { packageName ->
            resolveInstalledApp(packageManager, packageName)?.let { apps.add(it) }
        }
        sequenceNumber = changed.sequenceNumber
        return apps.sortedByInstallOrder().also { cachedApps = it }
    }

    /** 画面側でアプリの追加・削除を反映した一覧を、キャッシュにも反映する。 */
    fun update(apps: List<AppInfo>) {
        cachedApps = apps
    }

    private fun reload(packageManager: PackageManager, locale: Locale): List<AppInfo> {
        val apps = getInstalledApps(packageManager)
        cachedApps = apps
        cachedLocale = locale
        // ここまでの変更はすべて反映済みなので、今の通し番号を覚えておく
        sequenceNumber = packageManager.getChangedPackages(0)?.sequenceNumber ?: 0
        return apps
    }
}
