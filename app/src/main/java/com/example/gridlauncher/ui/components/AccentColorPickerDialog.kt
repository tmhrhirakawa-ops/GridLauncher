package com.example.gridlauncher.ui.components

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LightAccentColor
import com.example.gridlauncher.ui.theme.LocalCyberColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * メインテーマのデフォルトのアクセントカラー（従来からのオレンジ）。
 */
val DefaultAccentColor = LightAccentColor

private const val KEY_RECENT_ACCENT_COLORS = "recent_accent_colors"
private const val MAX_RECENT_ACCENT_COLORS = 10

/** 直近で設定したアクセントカラーの履歴（新しい順）をSharedPreferencesから読み込む。 */
private fun loadRecentAccentColors(prefs: SharedPreferences): List<Color> =
    prefs.getString(KEY_RECENT_ACCENT_COLORS, null)
        ?.split(",")
        ?.mapNotNull { token -> token.toIntOrNull()?.let { Color(it) } }
        ?: emptyList()

/**
 * [color]を履歴の先頭に追加して保存する（既に含まれていれば重複を除いて先頭に移動し、
 * 最大[MAX_RECENT_ACCENT_COLORS]件まで保持する）。更新後の履歴を返す。
 */
private fun addRecentAccentColor(prefs: SharedPreferences, current: List<Color>, color: Color): List<Color> {
    val updated = (listOf(color) + current.filter { it.toArgb() != color.toArgb() }).take(MAX_RECENT_ACCENT_COLORS)
    prefs.edit { putString(KEY_RECENT_ACCENT_COLORS, updated.joinToString(",") { it.toArgb().toString() }) }
    return updated
}

/**
 * アクセントカラーを選ぶためのパレット。「にゅいっ」と弾むように表示されるポップアップで、
 * カラーサークル上をタップ・ドラッグすると、メインテーマのアクセントカラー
 * （現在オレンジになっている部分）がリアルタイムに変わる。
 * 閉じるときに、その時点の色を「最近使った色」の履歴に追加する。
 *
 * @param currentColor 現在のアクセントカラー。
 * @param useOriginalIconColors アプリアイコンをオリジナルカラーのまま表示しているかどうか。
 * @param onColorSelected 色が選択されたときのコールバック。
 * @param onUseOriginalIconColorsChange 「アプリアイコンはオリジナルカラーを使用」チェックボックスが
 *   切り替えられたときのコールバック。
 * @param onDismiss パレットが閉じられるときのコールバック。
 */
@Composable
fun AccentColorPickerDialog(
    currentColor: Color,
    useOriginalIconColors: Boolean,
    onColorSelected: (Color) -> Unit,
    onUseOriginalIconColorsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE) }
    val recentColors = remember { loadRecentAccentColors(prefs) }
    val dismissAndRemember = {
        addRecentAccentColor(prefs, recentColors, currentColor)
        onDismiss()
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(onDismissRequest = dismissAndRemember) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = scaleOut(targetScale = 0.5f) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.bg,
                border = BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("ACCENT COLOR", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            "デフォルトに戻す",
                            fontFamily = CyberFont,
                            fontSize = 10.sp,
                            color = colors.accent,
                            modifier = Modifier.clickable { onColorSelected(DefaultAccentColor) }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ColorWheel(
                        color = currentColor,
                        onColorChange = onColorSelected,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    val hsv = remember(currentColor) {
                        val arr = FloatArray(3)
                        android.graphics.Color.colorToHSV(currentColor.toArgb(), arr)
                        arr
                    }
                    val hue = hsv[0]
                    val saturation = hsv[1]
                    val brightness = hsv[2]

                    // 明度インジケータ（このHue・彩度における V=0[黒]〜V=1[最大輝度]）
                    Text("明度", fontFamily = CyberFont, fontSize = 9.sp, color = colors.text.copy(alpha = 0.6f), modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(4.dp))
                    ColorSlider(
                        value = brightness,
                        trackColors = listOf(
                            Color.Black,
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                        ),
                        onValueChange = { newVal ->
                            onColorSelected(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, newVal))))
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (recentColors.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            "最近使った色",
                            fontFamily = CyberFont,
                            fontSize = 9.sp,
                            color = colors.text.copy(alpha = 0.6f),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val swatchSize = 24.dp
                        val swatchSpacing = 10.dp
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            // 横幅に入るだけ1行に並べ、入り切らない分だけ次の行へ折り返す
                            val maxPerRow = ((maxWidth + swatchSpacing) / (swatchSize + swatchSpacing)).toInt().coerceAtLeast(1)
                            val columns = min(maxPerRow, recentColors.size)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentColors.chunked(columns).forEach { rowColors ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(swatchSpacing)) {
                                        rowColors.forEach { swatch ->
                                            val isSelected = swatch.toArgb() == currentColor.toArgb()
                                            Box(
                                                modifier = Modifier
                                                    .size(swatchSize)
                                                    .clip(CircleShape)
                                                    .background(swatch)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 0.dp,
                                                        color = if (isSelected) colors.text else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { onColorSelected(swatch) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUseOriginalIconColorsChange(!useOriginalIconColors) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .then(
                                    if (useOriginalIconColors) {
                                        Modifier.background(colors.accent, RoundedCornerShape(3.dp))
                                    } else {
                                        Modifier.border(1.dp, colors.text.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "アプリアイコンはオリジナルカラーを使用",
                            fontFamily = CyberFont,
                            fontSize = 10.sp,
                            color = colors.text.copy(alpha = 0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            "閉じる",
                            fontFamily = CyberFont,
                            fontSize = 12.sp,
                            color = colors.text.copy(alpha = 0.7f),
                            modifier = Modifier.clickable(onClick = dismissAndRemember)
                        )
                    }
                }
            }
        }
    }
}

/**
 * HSVのカラーサークル（色相=角度、彩度=中心からの距離）。見やすさのため輪の描画自体は
 * 常に明度1で描くが、実際に選択される色には現在の明度（下の明度インジケータの値）を反映する。
 * タップ・ドラッグした位置から即座に色を計算してコールバックする。
 */
@Composable
private fun ColorWheel(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentValue = remember(color) {
        val arr = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), arr)
        arr[2]
    }
    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .pointerInput(currentValue) {
                detectTapGestures { offset -> onColorChange(colorAtOffset(offset, size, currentValue)) }
            }
            .pointerInput(currentValue) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onColorChange(colorAtOffset(change.position, size, currentValue))
                }
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // 色相の輪（彩度=1、明度=現在の明度インジケータの値の純色を角度で並べる）
        val hueColors = (0..12).map { i -> Color(android.graphics.Color.HSVToColor(floatArrayOf(i * 30f, 1f, currentValue))) }
        drawCircle(brush = Brush.sweepGradient(hueColors, center = center), radius = radius, center = center)
        // 中心を「現在の明度のグレー」でブレンドし、彩度=距離になるようにする
        val grayAtValue = Color(currentValue, currentValue, currentValue)
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to grayAtValue, 1f to grayAtValue.copy(alpha = 0f)),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )

        // 現在選択中の色の位置に選択インジケータを描画
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
        val hueRad = Math.toRadians(hsv[0].toDouble())
        val saturation = hsv[1]
        val indicatorCenter = Offset(
            x = center.x + (radius * saturation * cos(hueRad)).toFloat(),
            y = center.y + (radius * saturation * sin(hueRad)).toFloat()
        )
        drawCircle(color = Color.White, radius = 9.dp.toPx(), center = indicatorCenter, style = Stroke(width = 2.dp.toPx()))
        drawCircle(color = Color.Black.copy(alpha = 0.5f), radius = 9.dp.toPx(), center = indicatorCenter, style = Stroke(width = 1.dp.toPx()))
    }
}

private fun colorAtOffset(offset: Offset, size: IntSize, value: Float): Color {
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = min(size.width, size.height) / 2f
    if (radius <= 0f) return Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 0f, value)))
    val dx = offset.x - center.x
    val dy = offset.y - center.y
    val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)
    val angle = ((Math.toDegrees(atan2(dy, dx).toDouble()) + 360.0) % 360.0).toFloat()
    val saturation = (distance / radius).coerceIn(0f, 1f)
    return Color(android.graphics.Color.HSVToColor(floatArrayOf(angle, saturation, value)))
}

/**
 * 彩度・明度インジケータ用の横長スライダー。トラックの色はグラデーションで、
 * つまみの位置は現在値（0f〜1f）から算出する。
 */
@Composable
private fun ColorSlider(
    value: Float,
    trackColors: List<Color>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbSize = 18.dp
    BoxWithConstraints(
        modifier = modifier
            .height(20.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.horizontalGradient(trackColors))
            .pointerInput(Unit) {
                detectTapGestures { offset -> onValueChange((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val trackWidth = maxWidth
        val offsetX = (trackWidth - thumbSize) * value.coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = offsetX)
                .size(thumbSize)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape)
        )
    }
}
