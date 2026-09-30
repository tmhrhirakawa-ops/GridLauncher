package com.example.gridlauncher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.sections.HeaderSectionLandscape
import com.example.gridlauncher.ui.sections.HeaderSectionPortrait
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.CyberNotificationListener
import com.example.gridlauncher.util.HeaderTitle
import com.example.gridlauncher.util.WidgetLayoutMode

// ホーム画面（CyberLauncherScreen）を組み立てる、小さな部品。

/**
 * 画面モードに合わせたヘッダー。
 * 縦画面（小）はスマホサイズのカバー画面など、縦画面（大）はタブレットや展開状態の大画面、
 * 横画面はメイン画面のレイアウトを使う。
 */
@Composable
internal fun LauncherHeader(
    mode: WidgetLayoutMode,
    nowPlaying: CyberNotificationListener.NowPlayingInfo?,
    title: HeaderTitle,
    onCoreClick: () -> Unit
) {
    when (mode) {
        WidgetLayoutMode.SMALL_PORTRAIT -> HeaderSectionPortrait(nowPlaying = nowPlaying, title = title, onCoreClick = onCoreClick)
        WidgetLayoutMode.LARGE_PORTRAIT -> HeaderSectionPortrait(nowPlaying = nowPlaying, title = title, isLarge = true, onCoreClick = onCoreClick)
        WidgetLayoutMode.LANDSCAPE -> HeaderSectionLandscape(nowPlaying = nowPlaying, title = title, onCoreClick = onCoreClick)
    }
}

/**
 * ヘッダー下部・DOCKの上に引く区切り線。全モード（横画面・縦画面（小）・縦画面（大））共通で使う。
 */
@Composable
internal fun HeaderDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalCyberColors.current.border)
    )
}

/**
 * ウィジェットの移動ドラッグ中にDock付近へ表示する、「ここにドラッグして削除」ゾーン。
 * ドラッグ中の指がこの範囲に入っている間は[isActive]がtrueになり、危険色で強調表示する。
 */
@Composable
internal fun DeleteWidgetDropZone(isActive: Boolean) {
    val colors = LocalCyberColors.current
    val dangerColor = Color(0xFFFF3B4E)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) dangerColor.copy(alpha = 0.3f) else colors.bg.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, dangerColor.copy(alpha = if (isActive) 1f else 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = dangerColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "指を離すと削除します" else "ここにドラッグして削除",
                fontFamily = CyberFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = dangerColor
            )
        }
    }
}

/**
 * ホーム画面の確認・お知らせ用の、テーマの配色に合わせたダイアログ。
 *
 * @param confirmLabel 確定ボタンの文字。押すと[onConfirm]を呼ぶ（閉じるのは呼び出し側で行う）。
 * @param dismissLabel キャンセルボタンの文字。nullならキャンセルボタンを出さない（お知らせ用）。
 */
@Composable
internal fun LauncherAlertDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dismissLabel: String? = null
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = { Text(title, fontFamily = CyberFont, fontSize = 14.sp, color = colors.text) },
        text = {
            Text(message, fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.8f))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
            }
        },
        dismissButton = dismissLabel?.let { label ->
            {
                TextButton(onClick = onDismiss) {
                    Text(label, fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
                }
            }
        }
    )
}
