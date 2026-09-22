package com.example.girdlauncher.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * 空きスロットがタップされたときに、アプリを追加するかフォルダを作成するかを選択させるダイアログ。
 *
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 * @param onAddApp 「アプリを追加」が選択されたときのコールバック。
 * @param onCreateFolder 「フォルダを作成」が選択されたときのコールバック。
 */
@Composable
fun AddSlotChoiceDialog(
    onDismiss: () -> Unit,
    onAddApp: () -> Unit,
    onCreateFolder: () -> Unit
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = {
            Text("スロットに追加", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
        },
        text = {
            Text(
                "どちらを追加しますか？",
                fontFamily = CyberFont,
                fontSize = 12.sp,
                color = colors.text.copy(alpha = 0.7f)
            )
        },
        confirmButton = {
            TextButton(onClick = onAddApp) {
                Text("アプリを追加", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onCreateFolder) {
                Text("フォルダを作成", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}
