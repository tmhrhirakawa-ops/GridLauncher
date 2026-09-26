package com.example.gridlauncher.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.components.AnimatedEqualizerBars
import com.example.gridlauncher.ui.components.EqualizerBars
import com.example.gridlauncher.ui.components.NowPlayingIconButton
import com.example.gridlauncher.ui.components.NowPlayingPlayButton
import com.example.gridlauncher.ui.components.NowPlayingProgressBar
import com.example.gridlauncher.ui.components.PausedBarHeight
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.CyberNotificationListener
import com.example.gridlauncher.util.rememberLastPlayedMedia
import com.example.gridlauncher.util.resumeLastPlayedMedia

/**
 * 再生中のメディアを表示・操作するウィジェット（ヘッダーのNOW PLAYINGをウィジェットにしたもの）。
 * 操作ボタン以外の部分をタップすると再生中のアプリを開く。
 * 再生中のメディアがないときは、最後に再生したメディアを「LAST PLAYED」として表示し、再生ボタンで
 * そのアプリの再生を再開できる（一度も再生していなければ待機中の表示にする）。
 * ウィジェットの高さが低いときは、曲名と操作ボタンを1行にまとめた表示にする。
 *
 * このウィジェットがホーム画面にあるときは、ヘッダーのNOW PLAYINGは表示しない（呼び出し側で制御）。
 *
 * @param info 再生中のメディアの情報。nullの場合は最後に再生したメディア（なければ待機中）を表示する。
 * @param modifier レイアウトに適用するModifier。
 * @param showBorder 枠線を表示するかどうか。
 */
@Composable
fun NowPlayingSection(
    info: CyberNotificationListener.NowPlayingInfo?,
    modifier: Modifier = Modifier,
    showBorder: Boolean = true
) {
    val colors = LocalCyberColors.current
    val context = LocalContext.current
    // 再生中のメディアがないときに表示する、最後に再生したメディア
    val lastPlayed = rememberLastPlayedMedia().takeIf { info == null }
    // タップで開くアプリ（再生中のアプリ、なければ最後に再生したアプリ）
    val targetPackage = info?.packageName ?: lastPlayed?.packageName

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (showBorder) colors.panel.copy(alpha = 0.5f) else Color.Transparent,
        border = if (showBorder) BorderStroke(1.dp, colors.border) else null,
        modifier = modifier.fillMaxSize()
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .clickable(enabled = targetPackage != null) {
                    targetPackage ?: return@clickable
                    context.packageManager.getLaunchIntentForPackage(targetPackage)?.let { context.startActivity(it) }
                }
                .padding(10.dp)
        ) {
            // 高さが足りないときは、見出しを省いて曲名と操作ボタンを1行にまとめる
            val isCompact = maxHeight < 96.dp
            Column(modifier = Modifier.fillMaxSize()) {
                if (!isCompact) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(colors.accent))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (lastPlayed != null) "LAST PLAYED" else "NOW PLAYING",
                            fontFamily = CyberFont,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth().weight(1f)
                ) {
                    when {
                        info != null -> TrackLayout(
                            title = info.title,
                            artist = info.artist,
                            isPlaying = info.isPlaying,
                            isCompact = isCompact
                        ) { PlaybackControls(info) }
                        lastPlayed != null -> TrackLayout(
                            title = lastPlayed.title,
                            artist = lastPlayed.artist,
                            isPlaying = false,
                            isCompact = isCompact,
                            dim = true
                        ) {
                            // 再生セッションがないので、最後に再生していたアプリへ「再生」を送って再開させる
                            NowPlayingPlayButton(isPlaying = false, onClick = { resumeLastPlayedMedia(context) })
                        }
                        else -> StandbyContent()
                    }
                }
                if (info != null && info.duration > 0) {
                    NowPlayingProgressBar(info = info, compact = true)
                }
            }
        }
    }
}

/**
 * 曲の情報と操作ボタンの配置。[isCompact]のときは1行に並べ、そうでなければ曲の情報の下に
 * 操作ボタンを中央寄せで置く。
 */
@Composable
private fun TrackLayout(
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    isCompact: Boolean,
    dim: Boolean = false,
    controls: @Composable () -> Unit
) {
    if (isCompact) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrackInfo(title, artist, isPlaying, dim = dim, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(4.dp))
            controls()
        }
    } else {
        TrackInfo(title, artist, isPlaying, dim = dim, large = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            controls()
        }
    }
}

/**
 * イコライザーと曲名・アーティスト名。長い名前は横に流して表示する。
 * [dim]のとき（最後に再生したメディアの表示）は、再生中と区別できるよう文字を薄くする。
 */
@Composable
private fun TrackInfo(
    title: String?,
    artist: String?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    dim: Boolean = false
) {
    val colors = LocalCyberColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        if (isPlaying) AnimatedEqualizerBars() else EqualizerBars(heights = { PausedBarHeight })
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title ?: "NOW PLAYING",
                fontFamily = CyberFont,
                fontSize = if (large) 13.sp else 10.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text.copy(alpha = if (dim) 0.6f else 1f),
                maxLines = 1,
                modifier = Modifier.basicMarquee()
            )
            if (!artist.isNullOrBlank()) {
                Text(
                    text = artist,
                    fontFamily = CyberFont,
                    fontSize = if (large) 10.sp else 8.sp,
                    color = colors.text.copy(alpha = if (dim) 0.35f else 0.5f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }
    }
}

/**
 * 前へ・再生/一時停止・次への操作ボタン。ウィジェットは常に置いておくものなので、ヘッダーにある
 * 「停止して非表示」のボタンは付けない。
 */
@Composable
private fun PlaybackControls(info: CyberNotificationListener.NowPlayingInfo) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        NowPlayingIconButton(
            icon = Icons.Outlined.SkipPrevious,
            contentDescription = "Previous",
            onClick = { CyberNotificationListener.skipToPrevious() }
        )
        NowPlayingPlayButton(
            isPlaying = info.isPlaying,
            onClick = { if (info.isPlaying) CyberNotificationListener.pause() else CyberNotificationListener.play() }
        )
        NowPlayingIconButton(
            icon = Icons.Outlined.SkipNext,
            contentDescription = "Next",
            onClick = { CyberNotificationListener.skipToNext() }
        )
    }
}

/** 再生中のメディアがないときの待機中の表示。 */
@Composable
private fun StandbyContent() {
    val colors = LocalCyberColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        EqualizerBars(heights = { PausedBarHeight })
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "NO MEDIA // STANDBY",
            fontFamily = CyberFont,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text.copy(alpha = 0.4f),
            maxLines = 1
        )
    }
}
