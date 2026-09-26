package com.example.gridlauncher.util

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * 権限/設定ステップ1件分の定義。オンボーディング画面と、未設定項目を知らせる
 * ボトムシートの両方から共有される。
 *
 * @property title ステップのタイトル。
 * @property description 何のためにこの設定が必要かの説明文。
 * @property isSatisfied 現在この設定が既に済んでいるかどうかを判定する関数。
 * @property settingsIntent 「設定を開く」がタップされたときに起動するIntent。
 */
data class OnboardingStepInfo(
    val title: String,
    val description: String,
    val isSatisfied: (Context) -> Boolean,
    val settingsIntent: (Context) -> Intent
)

val OnboardingSteps = listOf(
    OnboardingStepInfo(
        title = "デフォルトのホームアプリに設定",
        description = "GridLauncherをホーム画面として使うには、デフォルトのホームアプリに設定してください。",
        isSatisfied = { context ->
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            context.packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName == context.packageName
        },
        settingsIntent = { context -> homeRoleRequestIntent(context) }
    ),
    OnboardingStepInfo(
        title = "通知へのアクセスを許可",
        description = "通知バッジや再生中メディアの表示、QUICK ACCESSのミュート操作を使うには、通知へのアクセスを許可してください。",
        isSatisfied = { context -> isNotificationListenerEnabled(context) },
        settingsIntent = { Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) }
    ),
    OnboardingStepInfo(
        title = "バッテリー最適化の対象から除外",
        description = "通知や再生中メディアの監視を安定して続けるため、バッテリー最適化の対象からGridLauncherを除外することをおすすめします。",
        isSatisfied = { context ->
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            powerManager.isIgnoringBatteryOptimizations(context.packageName)
        },
        settingsIntent = { context ->
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        }
    ),
    OnboardingStepInfo(
        title = "使用状況へのアクセスを許可（任意）",
        description = "アプリドロワーに「よく使うアプリ」を表示するために使います。スキップしてもその他の機能には影響ありません。",
        isSatisfied = { context -> hasUsageStatsPermission(context) },
        settingsIntent = { Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS) }
    )
)

/**
 * デフォルトのホームアプリ設定を、可能であれば設定画面への遷移ではなく
 * [RoleManager]のシステムポップアップ（「ホームアプリに設定しますか？」の確認ダイアログ）で
 * 完結させる。[RoleManager.ROLE_HOME]はAPI 29以降でのみ利用できるため、それより前の端末では
 * 従来通り設定一覧画面（[Settings.ACTION_HOME_SETTINGS]）にフォールバックする。
 */
private fun homeRoleRequestIntent(context: Context): Intent {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        requestHomeRoleIntentOrNull(context)?.let { return it }
    }
    return Intent(Settings.ACTION_HOME_SETTINGS)
}

@RequiresApi(Build.VERSION_CODES.Q)
private fun requestHomeRoleIntentOrNull(context: Context): Intent? {
    val roleManager = context.getSystemService(RoleManager::class.java) ?: return null
    if (!roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) return null
    return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
}
