package com.example.gridlauncher.ui.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import kotlin.math.roundToInt

/**
 * ヘッダー（縦画面・横画面共通）の配置。左端に[start]（時刻）、画面幅の中央に[center]
 * （MAIN TERMINALの表記。なければ省略）、右端に[end]（バッテリー）を置く。
 *
 * [nowPlaying]（再生中メディア）は[end]のすぐ左に置く。ヘッダーの高さは他の3つだけで決め、
 * 再生中メディアはその高さに収めるため、表示されてもヘッダーが広がって下のウィジェットの
 * エリアが狭まることはない。幅は、他の3つを横に並べても入り切る範囲でなるべく本来の幅を使い、
 * 中央の表記と重なる場合は中央の表記のほうを左へ寄せる。
 *
 * @param alignTop trueなら[start]・[end]を上端に揃える（縦画面）。falseなら上下中央に揃える（横画面）。
 * @param nowPlayingGap [nowPlaying]と両隣の要素との間隔。
 * @param nowPlayingProgress [nowPlaying]の出し入れのアニメーションの進み具合（1=表示、0=非表示）。
 *   中央の表記を寄せる量をこれに合わせて変え、再生中メディアの出し入れと一緒にスライドさせる。
 *   レイアウトの段階で読むため、アニメーション中も再コンポジションは起きない。
 */
@Composable
internal fun HeaderLayout(
    start: @Composable () -> Unit,
    center: (@Composable () -> Unit)?,
    end: @Composable () -> Unit,
    nowPlaying: (@Composable () -> Unit)?,
    alignTop: Boolean,
    nowPlayingGap: Dp,
    nowPlayingProgress: () -> Float = { 1f },
    modifier: Modifier = Modifier
) {
    Layout(
        contents = listOf(start, center ?: {}, end, nowPlaying ?: {}),
        modifier = modifier.fillMaxWidth()
    ) { (startMeasurables, centerMeasurables, endMeasurables, nowPlayingMeasurables), constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val startPlaceable = startMeasurables.firstOrNull()?.measure(loose)
        val centerPlaceable = centerMeasurables.firstOrNull()?.measure(loose)
        val endPlaceable = endMeasurables.firstOrNull()?.measure(loose)

        val width = constraints.maxWidth
        // ヘッダーの高さは、再生中メディアを除いた3つだけで決める
        val height = maxOf(
            startPlaceable?.height ?: 0,
            centerPlaceable?.height ?: 0,
            endPlaceable?.height ?: 0
        ).coerceIn(constraints.minHeight, constraints.maxHeight)

        fun y(placeableHeight: Int) = if (alignTop) 0 else (height - placeableHeight) / 2

        val startWidth = startPlaceable?.width ?: 0
        val endX = width - (endPlaceable?.width ?: 0)
        val endY = endPlaceable?.let { y(it.height) } ?: 0

        // 再生中メディアは、他の3つを横に並べても入り切る範囲で、なるべく本来の幅で表示する
        // （高さはヘッダーの高さまで）
        val gap = nowPlayingGap.roundToPx()
        val centerSpace = centerPlaceable?.let { it.width + gap } ?: 0
        val availableWidth = (endX - gap - (startWidth + gap) - centerSpace).coerceAtLeast(0)
        val nowPlayingPlaceable = nowPlayingMeasurables.firstOrNull()?.measure(
            Constraints(maxWidth = availableWidth, maxHeight = height)
        )
        val nowPlayingX = nowPlayingPlaceable?.let { endX - gap - it.width }

        // 中央の表記は画面幅の中央に置く。再生中メディアと重なる場合だけ、重ならない位置まで
        // 左へ寄せる（再生中メディアを縮めて折り返させるより読みやすいため）。寄せる量は
        // 再生中メディアの出し入れのアニメーションに合わせて変え、一緒にスライドさせる
        val centerX = centerPlaceable?.let { center ->
            val preferredX = (width - center.width) / 2
            val limitX = nowPlayingX?.let { it - gap - center.width } ?: preferredX
            val shiftedX = minOf(preferredX, limitX).coerceAtLeast(startWidth + gap)
            val progress = if (nowPlayingX != null) nowPlayingProgress().coerceIn(0f, 1f) else 0f
            preferredX + ((shiftedX - preferredX) * progress).roundToInt()
        }

        layout(width, height) {
            startPlaceable?.placeRelative(0, y(startPlaceable.height))
            if (centerPlaceable != null && centerX != null) {
                centerPlaceable.placeRelative(centerX, (height - centerPlaceable.height) / 2)
            }
            endPlaceable?.placeRelative(endX, endY)
            // 再生中メディアは、[end]のすぐ左に、[end]と上下中央を揃えて置く
            if (nowPlayingPlaceable != null && nowPlayingX != null) {
                val endHeight = endPlaceable?.height ?: height
                val nowPlayingY = (endY + (endHeight - nowPlayingPlaceable.height) / 2)
                    .coerceIn(0, (height - nowPlayingPlaceable.height).coerceAtLeast(0))
                nowPlayingPlaceable.placeRelative(nowPlayingX, nowPlayingY)
            }
        }
    }
}

/**
 * 画面中央の「MAIN TERMINAL」の表記（横画面・縦画面（大）共通）。幅が足りなくても折り返して
 * ヘッダーの高さが変わらないよう、各行は1行に固定する。
 */
@Composable
internal fun MainTerminalTitle() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("SYSTEM ONLINE", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
        Text("MAIN TERMINAL", fontFamily = CyberFont, fontSize = 24.sp, fontWeight = FontWeight.Black, color = LocalCyberColors.current.text, letterSpacing = 2.sp, maxLines = 1, softWrap = false)
        Text("TOKYO // MAIN TERMINAL", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f), maxLines = 1, softWrap = false)
    }
}
