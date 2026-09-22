package com.example.girdlauncher.ui.sections

import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper as WallpaperFilled
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper as WallpaperOutlined
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
import com.example.girdlauncher.ui.components.QuickButton
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * メインテーマのデフォルトのアクセントカラー（従来からのオレンジ）。
 */
private val DefaultAccentColor = Color(0xFFFF5722)

/**
 * デバイスの様々な設定にアクセスするためのクイックアクセスボタンを提供するセクション。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param accentColor 現在のメインテーマ（アクセント）カラー。
 * @param onWallpaperToggle 壁紙透過切り替えボタンがクリックされたときのコールバック。
 * @param onThemeToggle テーマ切り替えボタンがクリックされたときのコールバック。
 * @param onAccentColorChange カラーパレットで色が選択されたときのコールバック。
 */
@Composable
fun QuickAccessSection(
    modifier: Modifier = Modifier,
    isWallpaperMode: Boolean = false,
    accentColor: Color = DefaultAccentColor,
    onWallpaperToggle: () -> Unit = {},
    onThemeToggle: () -> Unit = {},
    onAccentColorChange: (Color) -> Unit = {}
) {
    val context = LocalContext.current
    var showColorPicker by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.fillMaxWidth()
    ) {
         Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier
                        .size(6.dp)
                        .background(LocalCyberColors.current.accent))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("QUICK", fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                    Text(" // ACCESS", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 設定（歯車）ボタン
                    Surface(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        border = BorderStroke(1.dp, LocalCyberColors.current.border),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = "Settings",
                                tint = LocalCyberColors.current.text.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    // 壁紙切り替えボタン
                    Surface(
                        onClick = onWallpaperToggle,
                        shape = RoundedCornerShape(4.dp),
                        color = if (isWallpaperMode) LocalCyberColors.current.accent.copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent,
                        border = BorderStroke(1.dp, if (isWallpaperMode) LocalCyberColors.current.accent else LocalCyberColors.current.border),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWallpaperMode) Icons.Filled.WallpaperFilled else Icons.Outlined.WallpaperOutlined,
                                contentDescription = "Toggle Wallpaper",
                                tint = if (isWallpaperMode) LocalCyberColors.current.accent else LocalCyberColors.current.text.copy(alpha = 0.5f),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1段目
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = "WI-FI",
                        modifier = Modifier.weight(1f),
                        isWallpaperMode = isWallpaperMode,
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                    QuickButton(
                        text = "DISPLAY",
                        modifier = Modifier.weight(1f),
                        isWallpaperMode = isWallpaperMode,
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                // 2段目
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = "BLUETOOTH",
                        modifier = Modifier.weight(1f),
                        isWallpaperMode = isWallpaperMode,
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                    QuickButton(
                        text = "DEVELOP",
                        modifier = Modifier.weight(1f),
                        isWallpaperMode = isWallpaperMode,
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val fallbackIntent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(fallbackIntent)
                            }
                        }
                    )
                }
                // 3段目
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = "COLOR",
                        modifier = Modifier.weight(1f),
                        isWallpaperMode = isWallpaperMode,
                        onClick = { showColorPicker = true }
                    )
                    QuickButton(
                        text = "THEME",
                        modifier = Modifier.weight(1f),
                        isWallpaperMode = isWallpaperMode,
                        onClick = onThemeToggle
                    )
                }
            }
         }
    }

    if (showColorPicker) {
        AccentColorPickerDialog(
            currentColor = accentColor,
            onColorSelected = onAccentColorChange,
            onDismiss = { showColorPicker = false }
        )
    }
}

/**
 * アクセントカラーを選ぶためのパレット。「にゅいっ」と弾むように表示されるポップアップで、
 * カラーサークル上をタップ・ドラッグすると、メインテーマのアクセントカラー
 * （現在オレンジになっている部分）がリアルタイムに変わる。
 */
@Composable
private fun AccentColorPickerDialog(
    currentColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(onDismissRequest = onDismiss) {
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

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            "閉じる",
                            fontFamily = CyberFont,
                            fontSize = 12.sp,
                            color = colors.text.copy(alpha = 0.7f),
                            modifier = Modifier.clickable(onClick = onDismiss)
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
