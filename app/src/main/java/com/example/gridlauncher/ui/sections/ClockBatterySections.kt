package com.example.gridlauncher.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.rememberBatteryLevel
import com.example.gridlauncher.util.rememberCurrentTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** ウィジェットとして置く時計・バッテリーの共通の枠（他のウィジェットと同じパネルの見た目）。 */
@Composable
private fun WidgetPanelSurface(
    modifier: Modifier,
    showBorder: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (showBorder) LocalCyberColors.current.panel.copy(alpha = 0.5f) else Color.Transparent,
        border = if (showBorder) BorderStroke(1.dp, LocalCyberColors.current.border) else null,
        modifier = modifier.fillMaxSize(),
        content = content
    )
}

/**
 * ヘッダーと同じ時刻・日付を表示するウィジェット。文字の大きさはウィジェットの大きさに合わせて変わる。
 * 時刻はヘッダーと同じく、システムの毎分の時刻通知で更新する（ホーム画面が見えている間だけ）。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param showBorder 枠線を表示するかどうか。
 */
@Composable
fun ClockWidgetSection(modifier: Modifier = Modifier, showBorder: Boolean = true) {
    val colors = LocalCyberColors.current
    val currentTime = rememberCurrentTimeMillis()
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd // EEE", Locale.ENGLISH) } // 例: SEP 13 // SUN
    val timeString = timeFormat.format(Date(currentTime))
    val dateString = dateFormat.format(Date(currentTime)).uppercase()

    WidgetPanelSurface(modifier = modifier, showBorder = showBorder) {
        BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // 時刻（5文字）が幅に収まり、日付の行も入る高さになるよう、小さい方に合わせる
            val timeSize = minOf(maxWidth / 3.2f, maxHeight * 0.55f).coerceAtLeast(12.dp)
            val dateSize = (timeSize * 0.25f).coerceAtLeast(8.dp)
            val density = LocalDensity.current
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    timeString,
                    fontFamily = CyberFont,
                    fontSize = with(density) { timeSize.toSp() },
                    fontWeight = FontWeight.Bold,
                    color = colors.text,
                    letterSpacing = 2.sp,
                    maxLines = 1,
                    softWrap = false
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(dateSize / 2).background(colors.accent))
                    Spacer(modifier = Modifier.width(dateSize / 2))
                    Text(
                        dateString,
                        fontFamily = CyberFont,
                        fontSize = with(density) { dateSize.toSp() },
                        fontWeight = FontWeight.Bold,
                        color = colors.text.copy(alpha = 0.7f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * ヘッダーと同じバッテリー残量（円形のゲージと％）を表示するウィジェット。中央の歯車をタップすると
 * カスタマイズ画面を開く。円の大きさはウィジェットの大きさに合わせて変わる。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param showBorder 枠線を表示するかどうか。
 * @param onCoreClick 中央の歯車がタップされたときのコールバック（カスタマイズ画面を開く）。
 */
@Composable
fun BatteryWidgetSection(modifier: Modifier = Modifier, showBorder: Boolean = true, onCoreClick: () -> Unit = {}) {
    val colors = LocalCyberColors.current
    val batteryLevel = rememberBatteryLevel()

    WidgetPanelSurface(modifier = modifier, showBorder = showBorder) {
        BoxWithConstraints(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // 下の「BATTERY xx%」の行を入れる高さを残して、円をできるだけ大きくする
            val labelHeight: Dp = 16.dp
            val showLabel = maxHeight >= 64.dp
            val circleSize = minOf(maxWidth, if (showLabel) maxHeight - labelHeight - 4.dp else maxHeight)
                .coerceAtLeast(24.dp)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(circleSize)) {
                    CircularProgressIndicator(
                        progress = { batteryLevel / 100f },
                        color = colors.accent,
                        trackColor = colors.border,
                        strokeWidth = (circleSize * 0.11f).coerceIn(3.dp, 10.dp),
                        modifier = Modifier.fillMaxSize()
                    )
                    // 真ん中の青い歯車（タップするとカスタマイズ画面がボトムシートで開く）
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Menu",
                        tint = colors.core,
                        modifier = Modifier
                            .size(circleSize * 0.55f)
                            .clip(CircleShape)
                            .clickable(onClick = onCoreClick)
                    )
                }
                if (showLabel) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("BATTERY", fontFamily = CyberFont, fontSize = 10.sp, color = colors.core, fontWeight = FontWeight.Bold, maxLines = 1)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("$batteryLevel%", fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.text, maxLines = 1)
                    }
                }
            }
        }
    }
}
