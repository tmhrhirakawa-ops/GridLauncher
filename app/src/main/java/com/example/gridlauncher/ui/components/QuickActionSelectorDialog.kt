package com.example.gridlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * QUICK ACCESSの空きスロットに追加するボタンの種類を選択させるダイアログ。
 * すべての種類を表示し、すでに別のスロットに設定されているものには「設定済み」と表示する
 * （選んだ場合に元のスロットから移動するかどうかの確認は、呼び出し側で行う）。
 *
 * @param assignedActions すでにいずれかのスロットに設定されているボタンの種類。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 * @param onSelect ボタンの種類が選択されたときのコールバック。
 */
@Composable
fun QuickActionSelectorDialog(
    assignedActions: Set<QuickActionId>,
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
            // 種類が多く小さい画面では入り切らないため、スクロールできるようにする
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                QuickActionId.entries.forEach { action ->
                    val isAssigned = action in assignedActions
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(action) }
                            .padding(vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = null,
                                tint = if (isAssigned) colors.accent.copy(alpha = 0.5f) else colors.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = action.label,
                                fontFamily = CyberFont,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAssigned) colors.accent.copy(alpha = 0.5f) else colors.accent
                            )
                            if (isAssigned) {
                                Text(
                                    text = "設定済み",
                                    fontFamily = CyberFont,
                                    fontSize = 9.sp,
                                    color = colors.text.copy(alpha = 0.5f)
                                )
                            }
                        }
                        Text(
                            text = action.description,
                            fontFamily = CyberFont,
                            fontSize = 10.sp,
                            color = colors.text.copy(alpha = if (isAssigned) 0.4f else 0.6f)
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
