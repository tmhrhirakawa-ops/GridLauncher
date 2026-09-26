package com.example.gridlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * グリッド内でフォルダ（アイコン＋名前）を表示するためのコンポーザブル。
 * 見た目は[AppCard]に合わせつつ、実アイコンの代わりに汎用のフォルダアイコンを表示する。
 *
 * @param name フォルダの名前。
 * @param modifier レイアウトに適用するModifier。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onClick カードがクリックされたときのコールバック（通常時はフォルダの中身を開く）。
 * @param onLongClick カードが長押しされたときのコールバック。
 */
@Composable
fun FolderCard(
    name: String,
    modifier: Modifier = Modifier,
    isWallpaperMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isWallpaperMode) LocalCyberColors.current.panel.copy(alpha = 0.55f) else LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = name,
                    tint = LocalCyberColors.current.accent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    fontFamily = CyberFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCyberColors.current.text,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}
