package com.example.gridlauncher.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.IconPackInfo

/**
 * 使うアイコンパックを選ぶダイアログ。インストールされているアイコンパックと「なし」を並べる。
 * アイコンパックが1つもない場合は、Play ストアで探すボタンを出す。
 *
 * @param iconPacks インストールされているアイコンパック。
 * @param selectedPackage 今使っているアイコンパックのパッケージ名（使っていなければnull）。
 * @param onSelect アイコンパックが選ばれたときのコールバック（nullは「なし」）。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 */
@Composable
fun IconPackPickerDialog(
    iconPacks: List<IconPackInfo>,
    selectedPackage: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.panel,
        title = { Text("アイコンパック", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                IconPackRow(label = "なし（標準のアイコン）", selected = selectedPackage == null, onClick = { onSelect(null) }) {
                    Spacer(modifier = Modifier.size(28.dp))
                }
                iconPacks.forEach { pack ->
                    IconPackRow(label = pack.label, selected = pack.packageName == selectedPackage, onClick = { onSelect(pack.packageName) }) {
                        val icon = remember(pack.packageName) {
                            runCatching {
                                context.packageManager.getApplicationIcon(pack.packageName).toBitmap(width = 96, height = 96).asImageBitmap()
                            }.getOrNull()
                        }
                        if (icon != null) {
                            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(28.dp))
                        } else {
                            Spacer(modifier = Modifier.size(28.dp))
                        }
                    }
                }
                if (iconPacks.isEmpty()) {
                    Text(
                        "アイコンパックがインストールされていません。Nova Launcher などに対応したアイコンパックを入れると、ここに表示されます。",
                        fontFamily = CyberFont,
                        fontSize = 11.sp,
                        color = colors.text.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        "Play ストアで探す",
                        fontFamily = CyberFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier
                            .clickable {
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=icon%20pack&c=apps"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                }
                            }
                            .padding(vertical = 8.dp)
                    )
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

/** アイコンパックの選択肢1行分（選ばれているものは印とアクセントカラーで示す）。 */
@Composable
private fun IconPackRow(label: String, selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit) {
    val colors = LocalCyberColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        icon()
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            fontFamily = CyberFont,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.accent else colors.text,
            maxLines = 1
        )
    }
}
