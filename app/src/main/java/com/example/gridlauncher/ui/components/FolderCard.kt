package com.example.gridlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
        // 名前の表示位置は[AppCard]と同じ基準で決める（狭いスロットではアイコンの下に小さめに表示）
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val folderIcon: @Composable (Dp) -> Unit = { size ->
                Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = name,
                    tint = LocalCyberColors.current.accent,
                    modifier = Modifier.size(size)
                )
            }
            when (cardLabelPlacement(maxWidth, maxHeight, isCompact = false)) {
                CardLabelPlacement.BESIDE -> Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    folderIcon(28.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    CardLabel(text = name, fontSize = 12)
                }
                CardLabelPlacement.BELOW -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .align(Alignment.Center)
                ) {
                    folderIcon(24.dp)
                    Spacer(modifier = Modifier.height(2.dp))
                    CardLabel(text = name, fontSize = 9)
                }
                CardLabelPlacement.NONE -> Box(modifier = Modifier.align(Alignment.Center)) {
                    folderIcon(32.dp)
                }
            }
        }
    }
}
