package com.example.gridlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * クイックアクセスアクション用の標準ボタン。
 *
 * @param text ボタンに表示するテキスト。
 * @param modifier レイアウトに適用するModifier。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onClick ボタンがクリックされたときに呼び出されるコールバック。
 * @param onLongClick ボタンが長押しされたときに呼び出されるコールバック。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickButton(
    text: String,
    modifier: Modifier = Modifier,
    isWallpaperMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isWallpaperMode) LocalCyberColors.current.panel.copy(alpha = 0.55f) else LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = text,
                fontFamily = CyberFont,
                fontSize = 10.sp,
                color = LocalCyberColors.current.accent,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
