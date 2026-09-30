package com.example.gridlauncher.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * アプリの右上に出す、ステータスランプ（LED）風の通知の点。ホーム画面のアプリには常に表示し、
 * 通知が来ているときだけアクセントカラーで点灯させる（件数は表示しない）。
 * 消灯中は枠線と同じ色にして、背景に溶け込みすぎず目立ちすぎないようにする。
 *
 * @param isActive 通知が来ているかどうか（trueで点灯）。
 * @param modifier レイアウトに適用するModifier（配置位置など）。
 * @param size 点の直径。
 */
@Composable
fun NotificationDot(isActive: Boolean, modifier: Modifier = Modifier, size: Dp = 6.dp) {
    val colors = LocalCyberColors.current
    // 点灯中は周りに光のにじみ（グロー）を描くため、点の直径より少し広く領域を取る
    Canvas(modifier = modifier.size(size * 2)) {
        val radius = size.toPx() / 2
        if (isActive) {
            drawCircle(color = colors.accent.copy(alpha = 0.25f), radius = radius * 2)
            drawCircle(color = colors.accent.copy(alpha = 0.45f), radius = radius * 1.4f)
            drawCircle(color = colors.accent, radius = radius)
        } else {
            drawCircle(color = colors.border, radius = radius)
        }
    }
}
