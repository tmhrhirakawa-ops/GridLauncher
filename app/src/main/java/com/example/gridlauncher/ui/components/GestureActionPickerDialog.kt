package com.example.gridlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.GestureAction
import com.example.gridlauncher.util.HomeGesture

/**
 * ホーム画面のジェスチャーに割り当てるアクションを選ぶダイアログ。
 *
 * @param gesture 割り当てを変えるジェスチャー。
 * @param current 今割り当てているアクション。
 * @param isAccessibilityEnabled アクセシビリティサービスが使える状態かどうか（使えない場合は、
 *   それが必要なアクションに注意書きを出す）。
 * @param onSelect アクションが選ばれたときのコールバック（「アプリを起動」の場合、アプリは呼び出し側で選ばせる）。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 */
@Composable
fun GestureActionPickerDialog(
    gesture: HomeGesture,
    current: GestureAction,
    isAccessibilityEnabled: Boolean,
    onSelect: (GestureAction) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = { Text("${gesture.label}の動作", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                GestureAction.availableEntries().forEach { action ->
                    val selected = action == current
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(action) }
                            .padding(vertical = 10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selected) colors.accent else Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                action.label,
                                fontFamily = CyberFont,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) colors.accent else colors.text
                            )
                            // 端末全体の操作は、アクセシビリティサービスがまだ使えない場合だけ、有効化が必要なことを知らせる
                            if (action.globalAction != null && !isAccessibilityEnabled) {
                                Text(
                                    "アクセシビリティサービスの有効化が必要です",
                                    fontFamily = CyberFont,
                                    fontSize = 9.sp,
                                    color = colors.text.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("閉じる", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
            }
        }
    )
}
