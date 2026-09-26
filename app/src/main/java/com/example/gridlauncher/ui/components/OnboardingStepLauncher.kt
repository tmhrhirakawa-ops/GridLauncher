package com.example.gridlauncher.ui.components

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.gridlauncher.util.OnboardingStepInfo

/**
 * 権限/設定ステップの「設定を開く」を実行する関数を返す。
 *
 * 設定画面は結果を受け取る形式（startActivityForResult）で開く。デフォルトのホームアプリの
 * 確認ダイアログ（RoleManager）は、通常のstartActivityで開くと呼び出し元のアプリが分からず、
 * 何も表示せずに終了してしまうため。戻ってきてもまだ設定が済んでいなければ、
 * [OnboardingStepInfo.fallbackIntent]（ホームアプリなら端末の設定画面）を開く。
 */
@Composable
fun rememberOnboardingStepLauncher(): (OnboardingStepInfo) -> Unit {
    val context = LocalContext.current
    var pendingStep by remember { mutableStateOf<OnboardingStepInfo?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val step = pendingStep ?: return@rememberLauncherForActivityResult
        pendingStep = null
        if (!step.isSatisfied(context)) {
            step.fallbackIntent?.let { fallback ->
                try {
                    context.startActivity(fallback(context))
                } catch (_: ActivityNotFoundException) {
                }
            }
        }
    }
    return remember(launcher) {
        { step ->
            pendingStep = step
            try {
                launcher.launch(step.settingsIntent(context))
            } catch (_: ActivityNotFoundException) {
                pendingStep = null
                step.fallbackIntent?.let { fallback ->
                    try {
                        context.startActivity(fallback(context))
                    } catch (_: ActivityNotFoundException) {
                    }
                }
            }
        }
    }
}
