package com.example.gridlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * 親レイアウト内の[pressOffset]（長押しした位置）を左上としてポップアップを配置し、
 * 画面からはみ出す場合は画面内に収まるよう押し戻す。
 */
private class PressPointPositionProvider(private val pressOffset: Offset) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left + pressOffset.x.toInt()
        val y = anchorBounds.top + pressOffset.y.toInt()
        return IntOffset(
            x = x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y = y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        )
    }
}

/**
 * ホーム画面の何もないところを長押ししたときに、その位置へ「にゅいっ」と弾んで表示される
 * メニュー。ウィジェットの追加とカスタマイズ画面の表示を選べる。
 *
 * @param pressOffset 長押しした位置（このメニューを置いた親レイアウト内の座標）。
 * @param onAddWidget 「ウィジェットを追加」がタップされたときのコールバック。
 * @param onOpenCustomize 「カスタマイズ」がタップされたときのコールバック。
 * @param onDismiss メニューが閉じられるときのコールバック。
 */
@Composable
fun HomeLongPressMenu(
    pressOffset: Offset,
    onAddWidget: () -> Unit,
    onOpenCustomize: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Popup(
        popupPositionProvider = remember(pressOffset) { PressPointPositionProvider(pressOffset) },
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = scaleOut(targetScale = 0.3f) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.bg,
                border = BorderStroke(1.dp, colors.border)
            ) {
                // 項目の幅を一番長いものにそろえ、行全体をタップできるようにする
                Column(modifier = Modifier.width(IntrinsicSize.Max).padding(vertical = 6.dp)) {
                    HomeMenuItem(icon = Icons.Outlined.AddBox, label = "ウィジェットを追加", onClick = onAddWidget)
                    HomeMenuItem(icon = Icons.Outlined.Tune, label = "カスタマイズ", onClick = onOpenCustomize)
                }
            }
        }
    }
}

@Composable
private fun HomeMenuItem(icon: ImageVector, label: String, onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
    }
}
