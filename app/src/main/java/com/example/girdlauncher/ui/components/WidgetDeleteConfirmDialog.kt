package com.example.girdlauncher.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

private val DangerColor = Color(0xFFFF3B4E)

/**
 * ウィジェットを「ここにドラッグして削除」ゾーンへドロップしたときに表示する削除確認ダイアログ。
 *
 * @param widgetLabel 対象ウィジェットの表示名（タイトルに表示）。
 * @param onConfirm 「削除」が選択されたときのコールバック。
 * @param onDismiss ダイアログが閉じられるとき（キャンセル含む）のコールバック。
 */
@Composable
fun WidgetDeleteConfirmDialog(
    widgetLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = {
            Text(widgetLabel, fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
        },
        text = {
            Text(
                "このウィジェットを削除しますか？",
                fontFamily = CyberFont,
                fontSize = 12.sp,
                color = colors.text.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("削除", fontFamily = CyberFont, fontSize = 12.sp, color = DangerColor)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}
