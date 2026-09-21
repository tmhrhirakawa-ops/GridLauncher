package com.example.girdlauncher.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Process
import com.example.girdlauncher.UninstallResultReceiver
import com.example.girdlauncher.model.AppInfo
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.roundToInt

/**
 * アプリの実アイコン（[Drawable]）を、背景色を含まない単色（デュオトーン）加工した
 * [ImageBitmap] に変換します。加工元として以下を優先順位順に使います。
 *
 * 1. モノクロレイヤー（Android 13+の「テーマアイコン」用レイヤー）
 *    背景を含まない単色シルエット専用に作られているため、そのままアクセントカラーで
 *    塗りつぶすだけで綺麗に仕上がる。
 * 2. 前景レイヤー（Adaptive Icon、Android 8.0+）
 *    背景レイヤーとロゴ（前景レイヤー）が分離されているため、前景だけを使うことで
 *    背景色を含まないロゴのみの表示にできる。
 * 3. 通常のアイコン全体（上記が使えない古い形式のアプリ向けのフォールバック）
 *    背景込みの単一画像しか無いため、背景色は残ったまま加工する。
 *
 * @param drawable 加工対象のアプリアイコン。
 * @param accent マッピング先のアクセントカラー。
 */
fun toDuotoneImageBitmap(drawable: Drawable, accent: Color): ImageBitmap {
    val monochrome = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && drawable is AdaptiveIconDrawable) {
        drawable.monochrome
    } else null

    if (monochrome != null) {
        // モノクロレイヤーは既に単色シルエット用に作られているため、明るさ変換はせず
        // 元のアルファ形状をそのままアクセントカラーで塗りつぶす
        return toFlatTintedImageBitmap(monochrome, accent)
    }

    val foreground = (drawable as? AdaptiveIconDrawable)?.foreground
    return toLightnessDuotoneImageBitmap(foreground ?: drawable, accent)
}

/**
 * [drawable] の形状（アルファチャンネル）をそのまま残し、色だけを[accent]一色に塗りつぶします。
 * モノクロレイヤーのような「背景を含まない単色シルエット」向け。
 */
private fun toFlatTintedImageBitmap(drawable: Drawable, accent: Color): ImageBitmap {
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 108
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 108
    val source = drawable.toBitmap(width = width, height = height, config = Bitmap.Config.ARGB_8888)

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val accentColorRgb = ((accent.red * 255f).roundToInt() shl 16) or
        ((accent.green * 255f).roundToInt() shl 8) or
        (accent.blue * 255f).roundToInt()

    for (i in pixels.indices) {
        val alpha = (pixels[i] ushr 24) and 0xFF
        pixels[i] = if (alpha == 0) 0 else (alpha shl 24) or accentColorRgb
    }

    val (cropped, cropWidth, cropHeight) = cropToContent(pixels, width, height)
    return Bitmap.createBitmap(cropped, cropWidth, cropHeight, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * [drawable] の明るさを[accent]の濃淡にマッピングするデュオトーン加工をします。
 *
 * 明るさの指標には輝度（luma = 0.2126R + 0.7152G + 0.0722B）ではなく、
 * HSLの明度（Lightness = (max(R,G,B) + min(R,G,B)) / 2）を使っています。
 * lumaは赤の寄与率が低いため、Netflixのような「黒背景+赤ロゴ」を変換すると
 * 黒く潰れてしまう問題がありました。一方でHSVの明度（Value = max(R,G,B)）は
 * 逆に彩度の高い背景色まで明るくなりすぎ、白いロゴとのコントラストが失われて
 * ただの塗りつぶし丸に見えてしまう問題がありました。Lightnessはその中間の
 * 挙動になるため、両方のケースでロゴの視認性を保ちやすくなります。
 */
private fun toLightnessDuotoneImageBitmap(drawable: Drawable, accent: Color): ImageBitmap {
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 108
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 108
    val source = drawable.toBitmap(width = width, height = height, config = Bitmap.Config.ARGB_8888)

    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    val accentR = (accent.red * 255f).roundToInt()
    val accentG = (accent.green * 255f).roundToInt()
    val accentB = (accent.blue * 255f).roundToInt()

    // 背景を取り除いた前景/モノクロ以外のレイヤーでは、黒に近いロゴがダークパネルの背景に
    // 溶け込んで見えなくなることがあるため、明るさの下限（フロア）を設けて完全な黒にはしない
    val minLightness = 0.55f

    for (i in pixels.indices) {
        val pixel = pixels[i]
        val alpha = (pixel ushr 24) and 0xFF
        if (alpha == 0) continue

        val r = (pixel ushr 16) and 0xFF
        val g = (pixel ushr 8) and 0xFF
        val b = pixel and 0xFF
        val rawLightness = (maxOf(r, g, b) + minOf(r, g, b)) / (2f * 255f)
        val lightness = minLightness + (1f - minLightness) * rawLightness

        val outR = (accentR * lightness).roundToInt().coerceIn(0, 255)
        val outG = (accentG * lightness).roundToInt().coerceIn(0, 255)
        val outB = (accentB * lightness).roundToInt().coerceIn(0, 255)

        pixels[i] = (alpha shl 24) or (outR shl 16) or (outG shl 8) or outB
    }

    val (cropped, cropWidth, cropHeight) = cropToContent(pixels, width, height)
    return Bitmap.createBitmap(cropped, cropWidth, cropHeight, Bitmap.Config.ARGB_8888).asImageBitmap()
}

/**
 * ピクセル配列のうち、実際に描画されている範囲（アルファが閾値を超える範囲）だけを
 * 切り出します。アプリごとにアイコン内の余白量がまちまちなため、これを揃えることで
 * グリッド上での見た目の大きさを統一します。切り出した範囲の外周には、詰まりすぎて
 * 見えないよう内容物サイズに応じた余白を残します。
 *
 * @return 切り出したピクセル配列と、その幅・高さの組。描画内容が無い場合は元の配列をそのまま返す。
 */
private fun cropToContent(pixels: IntArray, width: Int, height: Int): Triple<IntArray, Int, Int> {
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1

    for (y in 0 until height) {
        val rowOffset = y * width
        for (x in 0 until width) {
            val alpha = (pixels[rowOffset + x] ushr 24) and 0xFF
            if (alpha > 10) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }

    if (maxX < minX || maxY < minY) {
        // 完全に透明な場合はそのまま返す
        return Triple(pixels, width, height)
    }

    val contentWidth = maxX - minX + 1
    val contentHeight = maxY - minY + 1
    val padding = (maxOf(contentWidth, contentHeight) * 0.12f).roundToInt()

    val cropMinX = (minX - padding).coerceAtLeast(0)
    val cropMinY = (minY - padding).coerceAtLeast(0)
    val cropMaxX = (maxX + padding).coerceAtMost(width - 1)
    val cropMaxY = (maxY + padding).coerceAtMost(height - 1)

    val cropWidth = cropMaxX - cropMinX + 1
    val cropHeight = cropMaxY - cropMinY + 1
    val cropped = IntArray(cropWidth * cropHeight)
    for (y in 0 until cropHeight) {
        System.arraycopy(pixels, (cropMinY + y) * width + cropMinX, cropped, y * cropWidth, cropWidth)
    }

    return Triple(cropped, cropWidth, cropHeight)
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
 * 指定したパッケージのアンインストール確認画面（システム標準ダイアログ）を起動します。
 * 実際のアンインストール処理はシステム側で行われるため、ここでは要求を投げるのみです。
 *
 * [android.content.pm.PackageInstaller.uninstall] 経由で要求する。ホーム（ランチャー）の
 * アクティビティから直接 `Intent.ACTION_DELETE` でアクティビティを起動する方式では、
 * 一部端末で確認画面が開いた直後に自ら閉じてしまう問題があったため、
 * こちらのAPI経由に変更した。
 *
 * アンインストールが実際に完了したかどうかは [UninstallResultReceiver] が結果を
 * 受け取って判断する（ユーザーがキャンセルした場合はスロットのアプリを残すため）。
 *
 * @param context インテントの発行に使用する [Context]。
 * @param packageName アンインストール対象アプリのパッケージ名。
 */
fun requestUninstall(context: Context, packageName: String) {
    val packageInstaller = context.packageManager.packageInstaller
    val statusIntent = Intent(context, UninstallResultReceiver::class.java).apply {
        putExtra(UninstallResultReceiver.EXTRA_PACKAGE_NAME, packageName)
    }
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        packageName.hashCode(),
        statusIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    )
    packageInstaller.uninstall(packageName, pendingIntent.intentSender)
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
