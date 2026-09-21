package com.example.girdlauncher.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import com.example.girdlauncher.model.AppInfo

/**
 * 起動可能なすべてのインストール済みアプリのリストを取得します。
 *
 * @param packageManager 照会する [PackageManager] のインスタンス。
 * @return ラベルのアルファベット順でソートされた [AppInfo] のリスト。
 */
fun getInstalledApps(packageManager: PackageManager): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    val resolvedInfos = packageManager.queryIntentActivities(intent, 0)
    
    return resolvedInfos.map { resolveInfo ->
        AppInfo(
            label = resolveInfo.loadLabel(packageManager).toString(),
            packageName = resolveInfo.activityInfo.packageName,
            icon = resolveInfo.loadIcon(packageManager)
        )
    }.sortedBy { it.label }
}

/**
 * 過去1週間で最も頻繁に使用されたアプリの上位8個を取得します。
 *
 * @param context 使用状況サービスにアクセスするための [Context]。
 * @param allApps インストールされているすべてのアプリのリスト。
 * @return よく使われる [AppInfo] アイテムのリスト。
 */
@SuppressLint("MissingPermission")
fun getFrequentApps(context: Context, allApps: List<AppInfo>): List<AppInfo> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = java.util.Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
    val startTime = calendar.timeInMillis

    val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime)
    
    val usageMap = usageStatsList.associateBy({ it.packageName }, { it.totalTimeInForeground })
    
    return allApps
        .map { app -> Pair(app, usageMap[app.packageName] ?: 0L) }
        .filter { it.second > 0L }
        .sortedByDescending { it.second }
        .take(8)
        .map { it.first }
}

/**
 * アプリに使用状況へのアクセス権限が付与されているかどうかを確認します。
 *
 * @param context 権限の確認に使用する [Context]。
 * @return 権限が付与されている場合は true、そうでない場合は false。
 */
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * 通知へのアクセス権限が付与されているかどうかを確認します。
 *
 * @param context 権限の確認に使用する [Context]。
 * @return 権限が付与されている場合は true、そうでない場合は false。
 */
fun hasNotificationAccess(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!android.text.TextUtils.isEmpty(flat)) {
        val names = flat.split(":")
        for (name in names) {
            val cn = android.content.ComponentName.unflattenFromString(name)
            if (cn != null && android.text.TextUtils.equals(pkgName, cn.packageName)) {
                return true
            }
        }
    }
    return false
}
