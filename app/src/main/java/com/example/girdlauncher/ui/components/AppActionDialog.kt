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
 * アプリに対する操作（スロットから削除／アンインストール）を選択させるダイアログ。
 *
 * @param appName 対象アプリの名前（タイトルに表示）。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 * @param onRemoveFromSlot 「スロットから削除」が選択されたときのコールバック。nullの場合はこの選択肢自体を表示しない
 *   （アプリドロワーのようにスロットの概念がない場所から呼ぶ場合に使用）。
 * @param onUninstall 「アンインストール」が選択されたときのコールバック。
 */
@Composable
fun AppActionDialog(
    appName: String,
    onDismiss: () -> Unit,
    onRemoveFromSlot: (() -> Unit)? = null,
    onUninstall: () -> Unit
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = {
            Text(appName, fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
        },
        text = {
            Text(
                "どの操作を行いますか？",
                fontFamily = CyberFont,
                fontSize = 12.sp,
                color = colors.text.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            TextButton(onClick = onUninstall) {
                Text("アンインストール", fontFamily = CyberFont, fontSize = 12.sp, color = DangerColor)
            }
        },
        dismissButton = {
            if (onRemoveFromSlot != null) {
                TextButton(onClick = onRemoveFromSlot) {
                    Text("スロットから削除", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("キャンセル", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
                }
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}
