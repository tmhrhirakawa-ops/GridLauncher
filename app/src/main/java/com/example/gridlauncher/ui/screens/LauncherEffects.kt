package com.example.gridlauncher.ui.screens

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.util.AppWidgetConfigureResultBridge
import com.example.gridlauncher.util.AppWidgetHostManager
import com.example.gridlauncher.util.InstalledAppsCache
import com.example.gridlauncher.util.resolveInstalledApp
import com.example.gridlauncher.util.sortedByInstallOrder

// ホーム画面が使う、端末の状態の監視や他の画面とのやりとり。
// CyberLauncherScreen から切り出したもの。

/**
 * インストール済みアプリの一覧。アプリのインストール・アンインストール・更新を検知して、その場で更新する。
 * 画面の回転などで作り直されるたびに全アプリを読み込み直さないよう、[InstalledAppsCache]を使う。
 */
@Composable
internal fun rememberInstalledApps(context: Context): State<List<AppInfo>> {
    val apps = remember { mutableStateOf(InstalledAppsCache.get(context.packageManager)) }
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                // 変更があったのは1パッケージだけなので、インストール済み全アプリを再取得・
                // 再加工するのではなく、その1件だけを差し替える（他アプリのアイコン処理を
                // 無駄に繰り返さないため）
                val packageName = intent.data?.schemeSpecificPart ?: return
                when (intent.action) {
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        // アップデートに伴う一時的なREMOVEDは無視する（続けてADDEDが届く）
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            apps.value = apps.value.filterNot { it.packageName == packageName }
                        }
                    }
                    Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                        val updated = resolveInstalledApp(context.packageManager, packageName)
                        if (updated != null) {
                            apps.value = (apps.value.filterNot { it.packageName == packageName } + updated).sortedByInstallOrder()
                        }
                    }
                }
                InstalledAppsCache.update(apps.value)
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    return apps
}

/**
 * 外部ウィジェット（他アプリのAppWidget）を追加するフローを返す。返した関数に、一覧で選ばれた
 * ウィジェットを渡すと、ID確保→バインド許可確認→（必要なら設定画面）を経て、[onBound]で
 * 配置を確定させる。途中でキャンセルされた場合は、確保したIDを破棄する。
 *
 * @param onBound ウィジェットを使える状態になったときに呼ぶ（配置に失敗したらIDを破棄すること）。
 */
@Composable
internal fun rememberAppWidgetAddFlow(activity: Activity, onBound: (appWidgetId: Int) -> Unit): (AppWidgetProviderInfo) -> Unit {
    val currentOnBound by rememberUpdatedState(onBound)
    // allocateAppWidgetId()で確保したIDを、選択→バインド許可確認→（必要なら設定画面）→配置確定、
    // の間ずっと覚えておく必要があるため、ここに保持する
    var pendingAppWidgetId by remember { mutableIntStateOf(-1) }

    // バインド許可が下りた（＝appWidgetIdが実際に使える状態になった）直後の共通処理。
    // 設定画面（configure）を持つウィジェットならそちらを起動し、なければそのまま配置を確定する。
    //
    // 設定画面の起動には、自前でIntent(ACTION_APPWIDGET_CONFIGURE)を組み立てて直接startActivityは
    // しない。多くのOEM製ウィジェット（例: Samsung Notesの「ノートのショートカット」）は設定画面が
    // exported="false"であり、直接起動するとSecurityExceptionでクラッシュする。
    // AppWidgetHost.startAppWidgetConfigureActivityForResult()はシステムが発行した
    // IntentSender経由で起動するため、exportedでない設定画面も正しく開ける
    // （結果はAppWidgetConfigureResultBridge経由でMainActivity.onActivityResultから受け取る）
    fun proceedAfterBind(appWidgetId: Int) {
        val configureComponent = AppWidgetManager.getInstance(activity).getAppWidgetInfo(appWidgetId)?.configure
        if (configureComponent != null) {
            AppWidgetConfigureResultBridge.onResult = { resultCode ->
                if (resultCode == Activity.RESULT_OK) {
                    currentOnBound(appWidgetId)
                } else {
                    AppWidgetHostManager.host.deleteAppWidgetId(appWidgetId)
                }
            }
            AppWidgetHostManager.host.startAppWidgetConfigureActivityForResult(
                activity, appWidgetId, 0, AppWidgetConfigureResultBridge.REQUEST_CODE, null
            )
        } else {
            currentOnBound(appWidgetId)
        }
    }

    // このアプリはBIND_APPWIDGET権限を持たない（サードパーティのランチャーは通常持てない）ため、
    // bindAppWidgetIdIfAllowedは基本的にfalseを返す。その場合はACTION_APPWIDGET_BINDで
    // システムのバインド確認ダイアログを挟む、というのが非特権ランチャーの標準的な実装方法
    val bindLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val id = pendingAppWidgetId
        pendingAppWidgetId = -1
        if (id == -1) return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            proceedAfterBind(id)
        } else {
            AppWidgetHostManager.host.deleteAppWidgetId(id)
        }
    }

    return { info ->
        val id = AppWidgetHostManager.host.allocateAppWidgetId()
        val alreadyBound = AppWidgetManager.getInstance(activity).bindAppWidgetIdIfAllowed(id, info.provider)
        if (alreadyBound) {
            proceedAfterBind(id)
        } else {
            pendingAppWidgetId = id
            bindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                }
            )
        }
    }
}
