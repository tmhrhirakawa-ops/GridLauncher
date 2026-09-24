package com.example.girdlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.WidgetPanel
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * ウィジェットキャンバスの空き領域に追加するウィジェットの種類を選択させるダイアログ。
 *
 * @param availableWidgets まだ配置されていない（追加候補となる）GirdLauncher内蔵ウィジェットの種類。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 * @param onSelect 内蔵ウィジェットの種類が選択されたときのコールバック。
 * @param onSelectExternal 「外部ウィジェットを追加」が選択されたときのコールバック。他アプリが
 *   提供するAppWidgetの選択（システム標準のウィジェット選択画面）を開始する想定。
 */
@Composable
fun WidgetTypeSelectorDialog(
    availableWidgets: List<WidgetPanel>,
    onDismiss: () -> Unit,
    onSelect: (WidgetPanel) -> Unit,
    onSelectExternal: () -> Unit = {}
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = {
            Text("ウィジェットを追加", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
        },
        text = {
            Column {
                availableWidgets.forEach { widget ->
                    Text(
                        text = widget.label,
                        fontFamily = CyberFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(widget) }
                            .padding(vertical = 10.dp)
                    )
                }
                if (availableWidgets.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(colors.border)
                    )
                }
                Text(
                    text = "＋ 外部ウィジェットを追加...",
                    fontFamily = CyberFont,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text.copy(alpha = 0.7f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectExternal() }
                        .padding(vertical = 10.dp)
                )
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
