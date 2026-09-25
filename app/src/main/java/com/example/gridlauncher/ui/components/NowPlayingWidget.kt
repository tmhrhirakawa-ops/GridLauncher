package com.example.gridlauncher.ui.components

import android.os.SystemClock
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.CyberNotificationListener
import kotlinx.coroutines.delay

/**
 * 現在再生中のメディア（音楽・動画など）を表示し、再生操作を行うウィジェット。
 * 操作ボタン以外の部分をタップすると、再生中のアプリを開く。
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
    val context = LocalContext.current

    Surface(
        onClick = {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(info.packageName)
            launchIntent?.let { context.startActivity(it) }
        },
        shape = RoundedCornerShape(4.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.6f),
        border = BorderStroke(
            1.dp,
            if (info.isPlaying) LocalCyberColors.current.accent.copy(alpha = 0.6f) else LocalCyberColors.current.border
        ),
        modifier = modifier
    ) {
        // IntrinsicSize.Maxを指定しないと、下のゲージのfillMaxWidth()が親の
        // 利用可能幅いっぱいに広がろうとして、Row本来の内容幅を超えてカード全体が
        // 不必要に横長になってしまう
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
            Row(
                modifier = Modifier.padding(horizontal = if (compact) 6.dp else 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // イコライザー風の再生アニメーション
                if (info.isPlaying) {
                    AnimatedEqualizerBars()
                } else {
                    EqualizerBars(heights = { PausedBarHeight })
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

            if (info.duration > 0) {
                NowPlayingProgressBar(info = info, compact = compact)
            }
        }
    }
}

/** 一時停止中のイコライザーのバーの高さ（割合）。 */
private const val PausedBarHeight = 0.3f

/**
 * 再生中のイコライザー風のバー。3本をそれぞれ異なる周期でパルスさせる。
 * 無限アニメーションは再生中にこのComposableが表示されている間だけ動かし、アニメーション値は
 * 描画フェーズでのみ読むことで、毎フレームの再コンポジションを避ける。
 */
@Composable
private fun AnimatedEqualizerBars() {
    val infiniteTransition = rememberInfiniteTransition(label = "nowPlayingAnim")
    val bar1 = infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar1"
    )
    val bar2 = infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(560, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar2"
    )
    val bar3 = infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(340, easing = LinearEasing), RepeatMode.Reverse),
        label = "bar3"
    )
    EqualizerBars(heights = { index ->
        when (index) {
            0 -> bar1.value
            1 -> bar2.value
            else -> bar3.value
        }
    })
}

/**
 * イコライザー風の3本のバーを描画する。[heights]はバーの番号（0〜2）から高さの割合（0〜1）を返し、
 * 描画時にだけ呼ばれる。
 */
@Composable
private fun EqualizerBars(heights: (index: Int) -> Float) {
    val accent = LocalCyberColors.current.accent
    Canvas(modifier = Modifier.size(width = 13.dp, height = 14.dp)) {
        val barWidth = 3.dp.toPx()
        val spacing = 2.dp.toPx()
        val radius = CornerRadius(1.dp.toPx())
        for (index in 0 until 3) {
            val barHeight = size.height * heights(index).coerceIn(0f, 1f)
            drawRoundRect(
                color = accent,
                topLeft = Offset(index * (barWidth + spacing), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = radius
            )
        }
    }
}

/**
 * 再生位置のゲージ。再生中は経過時間を一定間隔で計算し直してゲージを滑らかに進める。
 * 更新はホーム画面が見えている間（ライフサイクルがSTARTED以上）だけ行い、他のアプリを
 * 使っている間に無駄に動かないようにする。
 */
@Composable
private fun NowPlayingProgressBar(info: CyberNotificationListener.NowPlayingInfo, compact: Boolean) {
    var nowElapsed by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(info.isPlaying, info.position, info.lastPositionUpdateTime, lifecycleOwner) {
        if (!info.isPlaying) return@LaunchedEffect
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                nowElapsed = SystemClock.elapsedRealtime()
                delay(500)
            }
        }
    }
    val livePosition = if (info.isPlaying) {
        info.position + ((nowElapsed - info.lastPositionUpdateTime) * info.playbackSpeed).toLong()
    } else {
        info.position
    }
    val progress = (livePosition.toFloat() / info.duration.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = 4.dp
            )
            .height(2.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(LocalCyberColors.current.border)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(LocalCyberColors.current.accent)
        )
    }
}

/**
 * ヘッダーに[NowPlayingWidget]を出し入れするための表示状態。
 *
 * @property displayed 表示する再生中メディア。消えるアニメーション中も直前の内容を保持する。
 * @property keepInLayout レイアウト上に場所を確保しておくかどうか（消えるアニメーションが
 *   終わるまではtrue）。
 */
class NowPlayingDisplayState internal constructor(
    val displayed: CyberNotificationListener.NowPlayingInfo?,
    val keepInLayout: Boolean,
    private val scaleState: State<Float>
) {
    /**
     * 出し入れのアニメーションの進み具合（1=表示、0=非表示）。値が毎フレーム変わるため、
     * graphicsLayerなど描画フェーズで読むこと。
     */
    val scale: Float get() = scaleState.value
}

/**
 * [nowPlaying]の有無に応じた[NowPlayingDisplayState]を返す。
 *
 * 表示/非表示は、AnimatedVisibilityのshrink/expand（レイアウト幅そのものを変える方式）ではなく、
 * graphicsLayerのscaleXで描画だけを縮める方式にする想定。幅を変える方式だと、右隣の
 * バッテリー表示に合わせてRow全体が右詰めで再配置されるため、右端が固定されたまま左端だけが
 * 動く「右への一方通行」に見えてしまう。scaleXならレイアウト上のサイズは変えず見た目だけを
 * 縮めるので、周りの表示位置を動かさずにその場（中心）から左右へ均等に縮んで消える。
 */
@Composable
fun rememberNowPlayingDisplayState(nowPlaying: CyberNotificationListener.NowPlayingInfo?): NowPlayingDisplayState {
    // 消滅アニメーション中も直前の内容を表示し続けるため、nullになった後も直前の非nullの値を
    // 保持しておく（フォルダを閉じるときのdisplayedFolderと同じパターン）
    var displayed by remember { mutableStateOf(nowPlaying) }
    LaunchedEffect(nowPlaying) {
        if (nowPlaying != null) displayed = nowPlaying
    }
    var keepInLayout by remember { mutableStateOf(nowPlaying != null) }
    LaunchedEffect(nowPlaying != null) {
        if (nowPlaying != null) keepInLayout = true
    }
    val scaleState = animateFloatAsState(
        targetValue = if (nowPlaying != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "nowPlayingScale",
        finishedListener = { value -> if (value == 0f) keepInLayout = false }
    )
    return NowPlayingDisplayState(displayed, keepInLayout, scaleState)
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
