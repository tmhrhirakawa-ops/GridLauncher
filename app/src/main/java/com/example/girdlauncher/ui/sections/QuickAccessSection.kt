package com.example.girdlauncher.ui.sections

import android.content.Context
import android.content.Intent
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
import com.example.girdlauncher.model.QuickActionId
import com.example.girdlauncher.ui.components.BrightnessControlDialog
import com.example.girdlauncher.ui.components.QuickActionSelectorDialog
import com.example.girdlauncher.ui.components.QuickButton
import com.example.girdlauncher.ui.components.VolumeControlDialog
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.QUICK_ACTION_CAPACITY
import com.example.girdlauncher.util.adaptiveSlotCount
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * メインテーマのデフォルトのアクセントカラー（従来からのオレンジ）。
 */
private val DefaultAccentColor = Color(0xFFFF5722)

/** ボタンがこれより狭く・低くなると窮屈になるとみなす、1ボタンの最小幅・最小高さ。 */
private val MinButtonWidth = 64.dp
private val MinButtonHeight = 32.dp

/** ボタンがこれより広く・高くなると間延びして見えるとみなす、1ボタンの最大幅・最大高さ。 */
private val MaxButtonWidth = 160.dp
private val MaxButtonHeight = 56.dp

/** ボタン間の余白。列数・行数の算出にもこの値を使う。 */
private val ButtonSpacing = 8.dp

/** ボタンの基準列数（ウィジェットのサイズがちょうどよいときに使う列数）。 */
private const val BaseColumns = 2

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
 * デバイスの様々な設定にアクセスするためのクイックアクセスボタンを提供するセクション。
 * ボタングリッドはアプリのグリッドと同様に追加・削除でき、編集モードはメイングリッドと共通。
 *
 * 列数・行数は[BaseColumns]・スロット総数（[QUICK_ACTION_CAPACITY]、設定できる機能の総数）から
 * 求めた基準行数を軸に、ウィジェットの実際の描画サイズに応じて自動的に決まる。ボタンが
 * [MinButtonWidth]・[MinButtonHeight]を下回りそうなほど狭くなったときは列数・行数を減らし、
 * 逆に[MaxButtonWidth]・[MaxButtonHeight]を超えて間延びしそうなほど広くなったときは列数・行数を
 * 増やす。ただし列数×行数は[QUICK_ACTION_CAPACITY]を超えない（それ以上は必ず空きスロットにしか
 * ならないため、代わりにボタン自体を大きくする）。
 *
 * ウィジェットが縮小されて、それまで表示されていたスロットの一部が入りきらなくなった場合、
 * ドラッグ中はそれらを一時的に非表示にするだけに留め、実際にリサイズハンドルのドラッグを
 * 終えた（[isResizing]がtrue→falseへ遷移した）タイミングで初めて、あふれたスロットを実際に
 * 空きスロットとして確定させる（そこに設定されていた機能は、別の空きスロットに設定し直せる
 * ようになる）。あふれて消えるスロットは、スロット番号の大きいものから（＝表示上は下・右側から）
 * 優先的に選ばれる。
 *
 * この確定処理は、あくまで実際のリサイズドラッグの完了だけをトリガーにしている（初回表示時や、
 * 縦画面・横画面の切り替えなど、ユーザーがドラッグしたわけではない理由でウィジェットのサイズが
 * 変わっただけのときは確定しない）。そのため、例えば縦画面では収まりきらず一時的に隠れている
 * スロットがあっても、横画面に切り替えただけでその設定が失われることはない。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param slots QUICK ACCESSに配置するボタンのスロット（null=空きスロット）。
 * @param isEditMode UIが編集モードかどうか（メイングリッドと共通の状態）。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param isResizing ウィジェットが現在リサイズドラッグ中かどうか。falseになったタイミングで、
 *   あふれたスロットを空きスロットとして確定する。
 * @param accentColor 現在のメインテーマ（アクセント）カラー。
 * @param useOriginalIconColors trueの場合、アプリアイコンをアクセントカラーのデュオトーン
 *   加工をせず、本来の色のまま表示する（カラーパレット下部のチェックボックスで切り替える）。
 * @param onThemeToggle テーマ切り替えボタンがクリックされたときのコールバック。
 * @param onAccentColorChange カラーパレットで色が選択されたときのコールバック。
 * @param onUseOriginalIconColorsChange カラーパレット下部の「アプリアイコンはオリジナルカラーを
 *   使用」チェックボックスが切り替えられたときのコールバック。
 * @param onSlotsChanged ウィジェットのサイズが確定し、あふれたスロットを空きスロットとして
 *   実際に確定するときのコールバック（更新後の全スロットを渡す）。呼び出し側はこれを使って
 *   保存する想定。
 * @param onAddClick 空きスロットがクリックされたときのコールバック。
 * @param onLongClick ボタンが長押しされたときのコールバック。
 * @param onRemoveClick 編集モードで削除バッジがクリックされたときのコールバック。
 * @param onExitEditMode 編集モード中に削除バッジ以外の部分がタップされたときのコールバック。
 * @param showBorder 枠線を表示するかどうか。
 */
@Composable
fun QuickAccessSection(
    modifier: Modifier = Modifier,
    slots: List<QuickActionId?> = emptyList(),
    isEditMode: Boolean = false,
    isWallpaperMode: Boolean = false,
    isResizing: Boolean = false,
    accentColor: Color = DefaultAccentColor,
    useOriginalIconColors: Boolean = false,
    showBorder: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onAccentColorChange: (Color) -> Unit = {},
    onUseOriginalIconColorsChange: (Boolean) -> Unit = {},
    onSlotsChanged: (List<QuickActionId?>) -> Unit = {},
    onAddClick: (Int) -> Unit = {},
    onLongClick: () -> Unit = {},
    onRemoveClick: (Int) -> Unit = {},
    onExitEditMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var showBrightnessControl by remember { mutableStateOf(false) }
    var recentAccentColors by remember { mutableStateOf(loadRecentAccentColors(prefs)) }

    fun handleActionClick(action: QuickActionId) {
        when (action) {
            QuickActionId.WIFI -> {
                val intent = Intent(android.    provider.Settings.ACTION_WIFI_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            QuickActionId.DISPLAY -> {
                val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            QuickActionId.BLUETOOTH -> {
                val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            QuickActionId.DEVELOP -> {
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
            QuickActionId.COLOR -> showColorPicker = true
            QuickActionId.THEME -> onThemeToggle()
            QuickActionId.VOLUME -> showVolumeControl = true
            QuickActionId.BRIGHTNESS -> showBrightnessControl = true
            QuickActionId.APP -> {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
            QuickActionId.WALLPAPER -> {
                val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (showBorder) LocalCyberColors.current.panel.copy(alpha = 0.5f) else Color.Transparent,
        border = if (showBorder) BorderStroke(1.dp, LocalCyberColors.current.border) else null,
        modifier = modifier.fillMaxSize()
    ) {
         Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(6.dp)
                    .background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(8.dp))
                Text("QUICK ACCESS", fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
            }
            Spacer(modifier = Modifier.height(6.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val baseRows = (QUICK_ACTION_CAPACITY + BaseColumns - 1) / BaseColumns
                val columns = adaptiveSlotCount(maxWidth, BaseColumns, MinButtonWidth, MaxButtonWidth, ButtonSpacing)
                // 列数×行数がQUICK_ACTION_CAPACITY（設定できる機能の総数）を超えないよう、
                // 行数の上限をここで決めておく。それ以上増やしても必ず空きスロットにしかならない
                val maxRowsForCapacity = (QUICK_ACTION_CAPACITY / columns).coerceAtLeast(1)
                val rows = adaptiveSlotCount(maxHeight, baseRows, MinButtonHeight, MaxButtonHeight, ButtonSpacing)
                    .coerceAtMost(maxRowsForCapacity)
                val effectiveCapacity = columns * rows

                // ウィジェットのサイズが確定した（リサイズドラッグ中でなくなった）タイミングでだけ、
                // 現在表示しきれないスロット（番号の大きいもの＝下・右側）を空きスロットとして確定する。
                // ただし「isResizingがfalseになった」だけを条件にすると、縦画面・横画面の切り替えなど
                // ユーザーがドラッグでリサイズしたわけではない理由でウィジェットのサイズが変わった
                // 直後の初回測定でも（isResizingは変化前からfalseのままなので通常は再実行されないが、
                // 念のため）誤ってスロットが消えてしまわないよう、「実際にリサイズドラッグが
                // true→falseへ遷移した」ときだけ確定する。初回コンポーズ時のfalseは無視する
                var hasHandledInitialResizingState by remember { mutableStateOf(false) }
                LaunchedEffect(isResizing) {
                    if (!hasHandledInitialResizingState) {
                        hasHandledInitialResizingState = true
                        return@LaunchedEffect
                    }
                    if (!isResizing) {
                        val hasOverflow = slots.withIndex().any { (index, action) -> action != null && index >= effectiveCapacity }
                        if (hasOverflow) {
                            onSlotsChanged(slots.mapIndexed { index, action -> if (index < effectiveCapacity) action else null })
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(ButtonSpacing)
                ) {
                    for (rowIndex in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(ButtonSpacing)
                        ) {
                            for (col in 0 until columns) {
                                val index = rowIndex * columns + col
                                val action = slots.getOrNull(index)
                                if (action != null) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        QuickButton(
                                            text = action.label,
                                            modifier = Modifier.fillMaxSize(),
                                            isWallpaperMode = isWallpaperMode,
                                            isEditMode = isEditMode,
                                            onClick = {
                                                if (isEditMode) onExitEditMode() else handleActionClick(action)
                                            },
                                            onLongClick = onLongClick,
                                            onRemoveClick = { onRemoveClick(index) }
                                        )
                                        // ボタンの近くに縦スライダーのポップアップを表示する
                                        if (action == QuickActionId.VOLUME && showVolumeControl) {
                                            VolumeControlDialog(onDismiss = { showVolumeControl = false })
                                        }
                                        if (action == QuickActionId.BRIGHTNESS && showBrightnessControl) {
                                            BrightnessControlDialog(onDismiss = { showBrightnessControl = false })
                                        }
                                    }
                                } else {
                                    Surface(
                                        onClick = { if (isEditMode) onExitEditMode() else onAddClick(index) },
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Transparent,
                                        border = BorderStroke(1.dp, LocalCyberColors.current.border),
                                        modifier = Modifier.weight(1f).fillMaxHeight()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("EMPTY", fontFamily = CyberFont, fontSize = 9.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
         }
    }

    if (showColorPicker) {
        AccentColorPickerDialog(
            currentColor = accentColor,
            recentColors = recentAccentColors,
            useOriginalIconColors = useOriginalIconColors,
            onColorSelected = onAccentColorChange,
            onUseOriginalIconColorsChange = onUseOriginalIconColorsChange,
            onDismiss = {
                recentAccentColors = addRecentAccentColor(prefs, recentAccentColors, accentColor)
                showColorPicker = false
            }
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
    recentColors: List<Color>,
    useOriginalIconColors: Boolean,
    onColorSelected: (Color) -> Unit,
    onUseOriginalIconColorsChange: (Boolean) -> Unit,
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
