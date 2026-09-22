package com.example.girdlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.WidgetPanel
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * ウィジェットごとの枠線表示/非表示をチェックマークで個別に設定するダイアログ。
 *
 * @param hiddenPanels 現在枠線を非表示にしているウィジェットの集合。
 * @param onTogglePanel ウィジェットの行がタップされたときのコールバック。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 */
@Composable
fun WidgetBorderSettingsDialog(
    hiddenPanels: Set<WidgetPanel>,
    onTogglePanel: (WidgetPanel) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = {
            Text("枠線の表示設定", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
        },
        text = {
            Column {
                WidgetPanel.entries.forEach { panel ->
                    val isVisible = panel !in hiddenPanels
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePanel(panel) }
                            .padding(vertical = 6.dp)
                    ) {
                        Checkbox(
                            checked = isVisible,
                            onCheckedChange = { onTogglePanel(panel) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.accent,
                                uncheckedColor = colors.border
                            )
                        )
                        Text(
                            panel.label,
                            fontFamily = CyberFont,
                            fontSize = 12.sp,
                            color = colors.text
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
            }
        }
    )
}
