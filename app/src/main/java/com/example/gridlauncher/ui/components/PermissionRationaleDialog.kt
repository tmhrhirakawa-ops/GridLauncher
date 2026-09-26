package com.example.gridlauncher.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * 特殊な権限（設定画面から手動で許可する必要があるもの）へ案内する前に表示する、
 * 用途を説明する確認ダイアログ。
 *
 * @param message 権限が必要な理由の説明文。
 * @param onConfirm 「設定を開く」が選択されたときのコールバック（権限付与画面への遷移はここで行う）。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 */
@Composable
fun PermissionRationaleDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = {
            Text("権限が必要です", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
        },
        text = {
            Text(message, fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.8f))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("設定を開く", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
            }
        }
    )
}
