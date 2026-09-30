package com.example.gridlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * ウィジェットキャンバスの空き領域に追加するウィジェットの種類を選択させるダイアログ。
 *
 * @param availableWidgets まだ配置されていない（追加候補となる）GridLauncher内蔵ウィジェットの種類。
 * @param lockedWidgets PROを購入していないと追加できない種類（PROのマークを付ける）。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 * @param onSelect 内蔵ウィジェットの種類が選択されたときのコールバック。
 * @param onSelectExternal 「外部ウィジェットを追加」が選択されたときのコールバック。他アプリが
 *   提供するAppWidgetの選択（システム標準のウィジェット選択画面）を開始する想定。
 */
@Composable
fun WidgetTypeSelectorDialog(
    availableWidgets: List<WidgetPanel>,
    lockedWidgets: Set<WidgetPanel> = emptySet(),
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
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                availableWidgets.forEach { widget ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(widget) }
                            .padding(vertical = 10.dp)
                    ) {
                        Text(
                            text = widget.label,
                            fontFamily = CyberFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        // PROでないと追加できない種類には、PROのマークを付ける（選ぶとPRO解放の案内を出す）
                        if (widget in lockedWidgets) {
                            Spacer(modifier = Modifier.width(8.dp))
                            ProBadge()
                        }
                    }
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
