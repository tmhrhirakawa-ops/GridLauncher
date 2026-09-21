package com.example.girdlauncher.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.CyberNotificationListener

/**
 * 現在再生中のメディア（音楽・動画など）を表示し、再生操作を行うウィジェット。
 *
 * @param info 表示する再生中メディアの情報。
 * @param modifier レイアウトに適用するModifier。
 * @param compact trueの場合、縦画面向けの省スペースな表示にする。
 */
@Composable
fun NowPlayingWidget(
    info: CyberNotificationListener.NowPlayingInfo,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    // 再生中はイコライザー風のバーをそれぞれ異なる周期でパルスさせる
    val infiniteTransition = rememberInfiniteTransition(label = "nowPlayingAnim")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (info.isPlaying) LocalCyberColors.current.accent.copy(alpha = 0.6f) else LocalCyberColors.current.border
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // イコライザー風の再生アニメーション
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.height(14.dp)
            ) {
                val barHeights = if (info.isPlaying) listOf(bar1, bar2, bar3) else listOf(0.3f, 0.3f, 0.3f)
                barHeights.forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .fillMaxHeight(h)
                            .background(LocalCyberColors.current.accent, RoundedCornerShape(1.dp))
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .widthIn(max = if (compact) 80.dp else 140.dp)
            ) {
                Text(
                    text = info.title ?: "NOW PLAYING",
                    fontFamily = CyberFont,
                    fontSize = if (compact) 9.sp else 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCyberColors.current.text,
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
                if (!info.artist.isNullOrBlank()) {
                    Text(
                        text = info.artist,
                        fontFamily = CyberFont,
                        fontSize = 8.sp,
                        color = LocalCyberColors.current.text.copy(alpha = 0.5f),
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))

            NowPlayingIconButton(
                icon = Icons.Outlined.SkipPrevious,
                contentDescription = "Previous",
                onClick = { CyberNotificationListener.skipToPrevious() }
            )
            Spacer(modifier = Modifier.width(2.dp))
            NowPlayingPlayButton(
                isPlaying = info.isPlaying,
                onClick = {
                    if (info.isPlaying) CyberNotificationListener.pause() else CyberNotificationListener.play()
                }
            )
            Spacer(modifier = Modifier.width(2.dp))
            NowPlayingIconButton(
                icon = Icons.Outlined.SkipNext,
                contentDescription = "Next",
                onClick = { CyberNotificationListener.skipToNext() }
            )
            NowPlayingIconButton(
                icon = Icons.Outlined.Close,
                contentDescription = "Stop and dismiss",
                dim = true,
                onClick = { CyberNotificationListener.stopAndDismiss() }
            )
        }
    }
}

@Composable
private fun NowPlayingIconButton(
    icon: ImageVector,
    contentDescription: String,
    dim: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalCyberColors.current.text.copy(alpha = if (dim) 0.5f else 0.85f),
            modifier = Modifier.size(15.dp)
        )
    }
}

/**
 * 再生/一時停止ボタン。他の操作ボタンより目立つように、円形のアクセントカラー背景で強調する。
 */
@Composable
private fun NowPlayingPlayButton(isPlaying: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(LocalCyberColors.current.accent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = if (isPlaying) "Pause" else "Play" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
    }
}
