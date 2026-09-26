package com.example.gridlauncher.ui.sections

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.ui.components.AccentColorPickerDialog
import com.example.gridlauncher.ui.components.BrightnessControlDialog
import com.example.gridlauncher.ui.components.DefaultAccentColor
import com.example.gridlauncher.ui.components.QuickActionSelectorDialog
import com.example.gridlauncher.ui.components.QuickButton
import com.example.gridlauncher.ui.components.VolumeControlDialog
import com.example.gridlauncher.ui.drag.AppDragItem
import com.example.gridlauncher.ui.drag.AppDragPayload
import com.example.gridlauncher.ui.drag.AppDragSource
import com.example.gridlauncher.ui.drag.AppDropTarget
import com.example.gridlauncher.ui.drag.LocalAppDragState
import com.example.gridlauncher.ui.drag.appDragSource
import com.example.gridlauncher.ui.drag.appDropTarget
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.QUICK_ACTION_CAPACITY
import com.example.gridlauncher.util.adaptiveSlotCount

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

/**
 * デバイスの様々な設定にアクセスするためのクイックアクセスボタンを提供するセクション。
 * ボタングリッドはアプリのグリッドと同様に、空きスロットへの追加とドラッグでの並べ替え・削除ができる。
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
 * ボタンは長押し→ドラッグで並べ替え・削除する（[com.example.gridlauncher.ui.drag]参照）。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param slots QUICK ACCESSに配置するボタンのスロット（null=空きスロット）。
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
 * @param showBorder 枠線を表示するかどうか。
 */
@Composable
fun QuickAccessSection(
    modifier: Modifier = Modifier,
    slots: List<QuickActionId?> = emptyList(),
    isWallpaperMode: Boolean = false,
    isResizing: Boolean = false,
    accentColor: Color = DefaultAccentColor,
    useOriginalIconColors: Boolean = false,
    showBorder: Boolean = true,
    onThemeToggle: () -> Unit = {},
    onAccentColorChange: (Color) -> Unit = {},
    onUseOriginalIconColorsChange: (Boolean) -> Unit = {},
    onSlotsChanged: (List<QuickActionId?>) -> Unit = {},
    onAddClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val dragState = LocalAppDragState.current
    var showColorPicker by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var showBrightnessControl by remember { mutableStateOf(false) }

    fun handleActionClick(action: QuickActionId) {
        when (action) {
            QuickActionId.SETTINGS -> {
                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
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
                                // どのスロットもドロップ先にする。持ち上げ中のスロットは薄く表示して「抜けた」ことを示す
                                val slotModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .appDropTarget(AppDropTarget.QuickSlot(index))
                                    .graphicsLayer {
                                        alpha = if (dragState?.payload?.source == AppDragSource.QuickAccess(index)) 0.3f else 1f
                                    }
                                if (action != null) {
                                    Box(modifier = slotModifier) {
                                        QuickButton(
                                            text = action.label,
                                            modifier = Modifier.fillMaxSize().appDragSource {
                                                AppDragPayload(AppDragSource.QuickAccess(index), AppDragItem.QuickAction(action))
                                            },
                                            isWallpaperMode = isWallpaperMode,
                                            onClick = { handleActionClick(action) }
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
                                        onClick = { onAddClick(index) },
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.Transparent,
                                        border = BorderStroke(1.dp, LocalCyberColors.current.border),
                                        modifier = slotModifier
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
            useOriginalIconColors = useOriginalIconColors,
            onColorSelected = onAccentColorChange,
            onUseOriginalIconColorsChange = onUseOriginalIconColorsChange,
            onDismiss = { showColorPicker = false }
        )
    }
}
