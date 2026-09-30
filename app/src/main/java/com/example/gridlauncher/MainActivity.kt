package com.example.gridlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.gridlauncher.billing.ProManager
import com.example.gridlauncher.ui.LocalHomePressedSignal
import com.example.gridlauncher.ui.screens.CyberLauncherScreen
import com.example.gridlauncher.ui.theme.GridLauncherTheme
import com.example.gridlauncher.ui.theme.applyCyberFont
import com.example.gridlauncher.ui.theme.loadCyberFontOption
import com.example.gridlauncher.util.AppWidgetConfigureResultBridge

class MainActivity : ComponentActivity() {
    // ホームボタンが押された回数（LocalHomePressedSignal参照）
    private var homePressedSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Google Play に接続し、PRO（有料機能の解放）の購入状態と価格を取得する
        ProManager.init(this)
        // 起動直後の最初の表示から、選んでいるフォント（PROの機能）で描く
        if (ProManager.isPro.value) {
            applyCyberFont(loadCyberFontOption(getSharedPreferences("cyber_launcher", MODE_PRIVATE)))
        }
        enableEdgeToEdge()
        setContent {
            GridLauncherTheme {
                CompositionLocalProvider(LocalHomePressedSignal provides homePressedSignal) {
                    CyberLauncherScreen()
                }
            }
        }
    }

    // 他の端末での購入や払い戻しを反映するため、ホーム画面に戻るたびに購入状態を取り直す
    override fun onResume() {
        super.onResume()
        ProManager.refreshPurchases()
    }

    // ランチャー（launchMode="singleTask"）を表示中、または他のアプリを使用中にホームボタンが
    // 押されると、HOMEカテゴリのIntentがここに届く。開いているポップアップ・ボトムシート・
    // 編集モードなどを閉じてホーム画面に戻るよう、画面側に知らせる
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) {
            homePressedSignal++
        }
    }

    // AppWidgetHost.startAppWidgetConfigureActivityForResult()（ウィジェットの設定画面起動）は
    // 昔ながらのrequestCode方式のため、ここで受け取ってCompose側へ橋渡しする
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppWidgetConfigureResultBridge.REQUEST_CODE) {
            val callback = AppWidgetConfigureResultBridge.onResult
            AppWidgetConfigureResultBridge.onResult = null
            callback?.invoke(resultCode)
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, device = "spec:width=1280dp,height=800dp,dpi=240,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun LauncherPreview() {
    GridLauncherTheme {
        CyberLauncherScreen()
    }
}
