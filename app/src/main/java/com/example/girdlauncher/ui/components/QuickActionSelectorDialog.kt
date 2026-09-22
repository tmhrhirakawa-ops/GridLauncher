package com.example.girdlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.QuickActionId
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * QUICK ACCESSの空きスロットに追加するボタンの種類を選択させるダイアログ。
 *
 * @param availableActions まだ配置されていない（追加候補となる）ボタンの種類。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 * @param onSelect ボタンの種類が選択されたときのコールバック。
 */
@Composable
fun QuickActionSelectorDialog(
    availableActions: List<QuickActionId>,
    onDismiss: () -> Unit,
    onSelect: (QuickActionId) -> Unit
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = {
            Text("ボタンを追加", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
        },
        text = {
            if (availableActions.isEmpty()) {
                Text(
                    "追加できるボタンがありません",
                    fontFamily = CyberFont,
                    fontSize = 12.sp,
                    color = colors.text.copy(alpha = 0.6f)
                )
            } else {
                Column {
                    availableActions.forEach { action ->
                        Text(
                            text = action.label,
                            fontFamily = CyberFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(action) }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
            }
        },
        modifier = Modifier.padding(8.dp)
    )
}
