package com.example.gridlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.QuickButtonStyle
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/** ICON+NAMEのとき、アイコンと名前を横に並べるのに必要なボタンの最小の幅。これより狭いと縦に並べる。 */
private val IconBesideNameMinWidth = 80.dp

/**
 * クイックアクセスアクション用の標準ボタン。
 *
 * @param text ボタンに表示するテキスト。
 * @param icon ボタンに表示するアイコン。
 * @param style 表示スタイル（アイコンのみ・アイコン＋名前・名前のみ）。ICON+NAMEでボタンの幅が
 *   狭い場合は、アイコンの下に名前を小さめに表示する。
 * @param modifier レイアウトに適用するModifier。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onClick ボタンがクリックされたときに呼び出されるコールバック。
 * @param onLongClick ボタンが長押しされたときに呼び出されるコールバック。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QuickButton(
    text: String,
    icon: ImageVector,
    style: QuickButtonStyle,
    modifier: Modifier = Modifier,
    isWallpaperMode: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val accent = LocalCyberColors.current.accent
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isWallpaperMode) LocalCyberColors.current.panel.copy(alpha = 0.55f) else LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            when {
                style == QuickButtonStyle.ICON_ONLY -> {
                    Icon(imageVector = icon, contentDescription = text, tint = accent, modifier = Modifier.size(20.dp))
                }
                style == QuickButtonStyle.NAME_ONLY -> QuickButtonLabel(text = text, fontSize = 10.sp)
                maxWidth >= IconBesideNameMinWidth -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        QuickButtonLabel(text = text, fontSize = 10.sp)
                    }
                }
                else -> {
                    // 幅が狭いときは、アイコンの下に名前を小さめに表示する
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(2.dp))
                        QuickButtonLabel(text = text, fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickButtonLabel(text: String, fontSize: TextUnit) {
    Text(
        text = text,
        fontFamily = CyberFont,
        fontSize = fontSize,
        color = LocalCyberColors.current.accent,
        fontWeight = FontWeight.Bold,
        maxLines = 1
    )
}
