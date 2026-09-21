package com.example.girdlauncher.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Process
import com.example.girdlauncher.model.AppInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.roundToInt

/**
 * アプリの実アイコン（[Drawable]）を、明るさをアクセントカラーへマッピングしたデュオトーン風の
 * [ImageBitmap] に変換します。
 *
 * 明るさの指標には輝度（luma = 0.2126R + 0.7152G + 0.0722B）ではなく、
 * HSLの明度（Lightness = (max(R,G,B) + min(R,G,B)) / 2）を使っています。
 * lumaは赤の寄与率が低いため、Netflixのような「黒背景+赤ロゴ」を変換すると
 * 黒く潰れてしまう問題がありました。一方でHSVの明度（Value = max(R,G,B)）は
 * 逆に彩度の高い背景色まで明るくなりすぎ、白いロゴとのコントラストが失われて
 * ただの塗りつぶし丸に見えてしまう問題がありました。Lightnessはその中間の
 * 挙動になるため、両方のケースでロゴの視認性を保ちやすくなります。
 *
 * @param drawable 加工対象のアプリアイコン。
 * @param accent マッピング先のアクセントカラー。
 */
fun toDuotoneImageBitmap(drawable: Drawable, accent: Color): ImageBitmap {
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 108
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 108
    val source = drawable.toBitmap(width = width, height = height, config = Bitmap.Config.ARGB_8888)

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val accentR = (accent.red * 255f).roundToInt()
    val accentG = (accent.green * 255f).roundToInt()
    val accentB = (accent.blue * 255f).roundToInt()

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha == 0) continue

        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        val lightness = (maxOf(r, g, b) + minOf(r, g, b)) / (2f * 255f)

        val outR = (accentR * lightness).roundToInt().coerceIn(0, 255)
        val outG = (accentG * lightness).roundToInt().coerceIn(0, 255)
        val outB = (accentB * lightness).roundToInt().coerceIn(0, 255)

        pixels[i] = (alpha shl 24) or (outR shl 16) or (outG shl 8) or outB
    }

    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888).asImageBitmap()
}

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
