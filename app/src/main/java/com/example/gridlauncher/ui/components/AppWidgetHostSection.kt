package com.example.gridlauncher.ui.components

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.AppWidgetHostManager

/**
 * 他アプリが提供するAppWidget（[appWidgetId]で指定されたインスタンス）を、
 * [com.example.gridlauncher.util.AppWidgetHostManager]が保持する共有の
 * [android.appwidget.AppWidgetHost]経由でホスト・表示するセクション。
 *
 * ウィジェットのプロバイダが取得できない場合（アプリがアンインストールされた等）は、
 * その旨のプレースホルダーを表示する。削除は他のウィジェットと同様、ウィジェット編集モードで
 * 削除ゾーンへドラッグする既存のジェスチャーで行う（ここでは何もしない）。
 *
 * @param appWidgetId ホスト対象のAppWidgetインスタンスID。
 * @param modifier レイアウトに適用するModifier。
 * @param showBorder 枠線を表示するかどうか。
 * @param isResizing 現在リサイズドラッグ中かどうか。ドラッグ中の連続したライブプレビュー値を
 *   使って毎フレーム[AppWidgetManager.updateAppWidgetOptions]を呼んでしまわないよう、
 *   ドラッグ確定後（falseになった瞬間）にだけ実際のサイズをプロバイダへ伝えるために使う。
 */
@Composable
fun AppWidgetHostSection(
    appWidgetId: Int,
    modifier: Modifier = Modifier,
    showBorder: Boolean = true,
    isResizing: Boolean = false
) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current
    val providerInfo = remember(appWidgetId) {
        AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (showBorder) colors.panel.copy(alpha = 0.5f) else Color.Transparent,
        border = if (showBorder) BorderStroke(1.dp, colors.border) else null,
        modifier = modifier.fillMaxSize()
    ) {
        if (providerInfo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "ウィジェットを表示できません\n（アプリがアンインストールされた可能性があります）",
                    fontFamily = CyberFont,
                    fontSize = 10.sp,
                    color = colors.text.copy(alpha = 0.5f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthDp = maxWidth
                val heightDp = maxHeight
                // プロバイダに実際のサイズを伝え、そのサイズ向けのレイアウトで再描画してもらう。
                // これをしないと、ウィジェット編集モードでリサイズしたときにプロバイダ側は
                // 元のサイズのまま描画し続け、その画像をViewが単純に引き伸ばして表示するだけに
                // なるため、大きくリサイズするほど画質が粗く（ぼやけて）見える不具合になる。
                // ドラッグ中の連続したライブプレビュー値ごとに呼ぶと呼び出し過多になるため、
                // ドラッグ確定後（isResizingがfalseになったとき。初回配置時も含む）にだけ呼ぶ
                LaunchedEffect(isResizing, widthDp, heightDp) {
                    if (!isResizing) {
                        val widthValue = widthDp.value.toInt()
                        val heightValue = heightDp.value.toInt()
                        val options = Bundle().apply {
                            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthValue)
                            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthValue)
                            putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightValue)
                            putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightValue)
                        }
                        AppWidgetManager.getInstance(context).updateAppWidgetOptions(appWidgetId, options)
                    }
                }
                AndroidView(
                    factory = { viewContext ->
                        AppWidgetHostManager.host.createView(viewContext, appWidgetId, providerInfo)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
