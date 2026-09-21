package com.example.girdlauncher.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Process
import com.example.girdlauncher.model.AppInfo
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

// カスタムアイコンのマッピング
val customIconMap: Map<String, ImageVector> = mapOf(
    "com.google.android.youtube" to Icons.Outlined.PlayArrow,
    "com.android.chrome" to Icons.Outlined.Public,
    "com.google.android.gm" to Icons.Outlined.Email,
    "com.android.settings" to Icons.Outlined.Settings,
    "com.samsung.android.calendar" to Icons.Outlined.CalendarMonth, // CalendarMonthに変更
    "com.google.android.calendar" to Icons.Outlined.CalendarMonth, // CalendarMonthに変更
    "com.android.vending" to Icons.Outlined.ShoppingBag,
    "com.google.android.apps.maps" to Icons.Outlined.Place,
    "com.google.android.apps.photos" to Icons.Outlined.Photo,
    "com.twitter.android" to Icons.Outlined.Clear, // X (Twitter)
    "com.instagram.android" to Icons.Outlined.CameraAlt,
    "com.zhiliaoapp.musically" to Icons.Outlined.MusicNote, // TikTok

    // 追加リクエスト分
    "com.google.android.apps.walletnfcrel" to Icons.Outlined.CreditCard, // ウォレット
    "com.samsung.android.dialer" to Icons.Outlined.Phone, // 電話
    "com.sec.android.app.camera" to Icons.Outlined.CameraAlt, // カメラ
    "com.amazon.mShop.android.shopping" to Icons.Outlined.Store, // アマゾン
    "com.google.android.apps.authenticator2" to Icons.Outlined.Emergency, // 認証システム
    "com.ubercab.eats" to Icons.Outlined.Dining, // Uber Eats
    "com.amazon.kindle" to Icons.Outlined.AutoStories, // Kindle
    "jp.mufg.bk.applisp.app" to Icons.Outlined.AccountBalance, // 三菱UFJ
    "com.google.android.apps.bard" to Icons.Outlined.Assistant, // Gemini
    "com.anthropic.claude" to Icons.Outlined.LensBlur, // Claude
    "com.valvesoftware.android.steam.community" to Icons.Outlined.Psychology, // Steam
    "com.fitbit.FitbitMobile" to Icons.Outlined.FavoriteBorder, // Health (Google Fit等)
    "com.google.android.apps.healthdata" to Icons.Outlined.FavoriteBorder, // Health Connect
    "com.getkeepsafe.app" to Icons.Outlined.Key, // Keepsafe
    "com.google.android.apps.messaging" to @Suppress("DEPRECATION") Icons.Outlined.Message,
    "com.google.android.apps.wear.companion" to Icons.Outlined.Watch, //スマートウォッチ
    "com.google.ar.lens" to Icons.Outlined.CenterFocusStrong //レンズ
)

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
        val appInfo = resolveInfo.activityInfo.applicationInfo
        val category = appInfo.category
        
        AppInfo(
            label = resolveInfo.loadLabel(packageManager).toString(),
            packageName = resolveInfo.activityInfo.packageName,
            icon = resolveInfo.loadIcon(packageManager),
            category = category
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
        @Suppress("DEPRECATION") // 属性タグ付きの新オーバーロードは今回の用途では不要
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
