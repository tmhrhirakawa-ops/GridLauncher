package com.example.gridlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * アプリをホーム画面のスロット以外の場所へドロップしたときに、APP SLOT（ウィジェット）として置く形
 * （アイコンのみ・アイコン＋名前）を選ぶダイアログ。それぞれの見本を並べて見せる。
 *
 * @param app ドロップしたアプリ。
 * @param useOriginalIconColors trueの場合、アイコンをアクセントカラーで加工せず本来の色で表示する。
 * @param onSelect 形が選ばれたときのコールバック（[WidgetPanel.APP_SLOT_ICON_ONLY]か[WidgetPanel.APP_SLOT_NAMED]）。
 * @param onDismiss キャンセルされたときのコールバック（何も置かない）。
 */
@Composable
fun AppSlotStyleDialog(
    app: AppInfo,
    useOriginalIconColors: Boolean,
    onSelect: (WidgetPanel) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    val bitmap = rememberAppIconBitmap(app.packageName, app.icon, app.iconIsMonochrome, useOriginalIconColors)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = { Text("ウィジェットとして置く", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "「${app.label}」をこの場所に置きます。表示のしかたを選んでください。",
                    fontFamily = CyberFont,
                    fontSize = 11.sp,
                    color = colors.text.copy(alpha = 0.7f)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    // アイコンのみ（正方形）
                    StyleChoice(label = "アイコンのみ", onClick = { onSelect(WidgetPanel.APP_SLOT_ICON_ONLY) }, modifier = Modifier.weight(1f)) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(32.dp))
                        }
                    }
                    // アイコン＋名前
                    StyleChoice(label = "アイコン＋名前", onClick = { onSelect(WidgetPanel.APP_SLOT_NAMED) }, modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(48.dp).padding(horizontal = 6.dp)) {
                            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                app.label,
                                fontFamily = CyberFont,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
            }
        }
    )
}

/** 置く形の選択肢1つ分（見本と名前）。 */
@Composable
private fun StyleChoice(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, preview: @Composable () -> Unit) {
    val colors = LocalCyberColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = colors.bg,
        border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f)),
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 10.dp)) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = colors.panel,
                border = BorderStroke(1.dp, colors.border)
            ) {
                preview()
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, fontFamily = CyberFont, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
        }
    }
}
