package com.example.gridlauncher.ui.sections

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.model.QuickButtonStyle
import com.example.gridlauncher.ui.LocalHomePressedSignal
import com.example.gridlauncher.ui.components.AccentColorPickerDialog
import com.example.gridlauncher.ui.components.BrightnessControlDialog
import com.example.gridlauncher.ui.components.DefaultAccentColor
import com.example.gridlauncher.ui.components.QuickButton
import com.example.gridlauncher.ui.components.VolumeControlDialog
import com.example.gridlauncher.ui.components.icon
import com.example.gridlauncher.ui.drag.AppDragItem
import com.example.gridlauncher.ui.drag.AppDragPayload
import com.example.gridlauncher.ui.drag.AppDragSource
import com.example.gridlauncher.ui.drag.AppDropTarget
import com.example.gridlauncher.ui.drag.LocalAppDragState
import com.example.gridlauncher.ui.drag.appDragSource
import com.example.gridlauncher.ui.drag.appDropTarget
import com.example.gridlauncher.ui.drag.dragEdgeAutoScroll
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.SlotGridSize
import com.example.gridlauncher.util.adaptiveSlotCount
import com.example.gridlauncher.util.requiredSlotGridPages

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
 * ボタンの基準行数（ウィジェットのサイズがちょうどよいときに使う行数）。ボタンの種類を増やしても
 * AUTOの並びが変わらないよう、種類の数からは求めず固定値にする。
 */
private const val BaseRows = 6

/**
 * デバイスの様々な設定にアクセスするためのクイックアクセスボタンを提供するセクション。
 * ボタングリッドはアプリのグリッドと同様に、空きスロットへの追加とドラッグでの並べ替え・削除ができる。
 *
 * 列数・行数はAUTO（デフォルト）の場合、[BaseColumns]・[BaseRows]を軸に、
 * ウィジェットの実際の描画サイズに応じて自動的に決まる。ボタンが[MinButtonWidth]・[MinButtonHeight]を
 * 下回りそうなほど狭くなったときは列数・行数を減らし、逆に[MaxButtonWidth]・[MaxButtonHeight]を超えて
 * 間延びしそうなほど広くなったときは列数・行数を増やす。QUICK ACCESSの設定画面（ヘッダーの歯車
 * ボタン）で列数×行数を指定した場合はそちらを使う。
 *
 * 1ページに入り切らないスロットは横スワイプのページに並べる。ページ数は設定画面で指定した数で、
 * ボタンが入っているページより少なくはしない。ウィジェットを縮めてもボタンは消えず、後ろの
 * ページへ送られるだけになる。
 *
 * ボタンは長押し→ドラッグで並べ替え・削除する（[com.example.gridlauncher.ui.drag]参照）。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param slots QUICK ACCESSに配置するボタンのスロット（null=空きスロット）。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param gridSize 設定画面で指定したスロットの並び（列数×行数）。nullの場合はAUTO。
 * @param pageCount 設定画面で指定したページ数。
 * @param accentColor 現在のメインテーマ（アクセント）カラー。
 * @param useOriginalIconColors trueの場合、アプリアイコンをアクセントカラーのデュオトーン
 *   加工をせず、本来の色のまま表示する（カラーパレット下部のチェックボックスで切り替える）。
 * @param buttonStyle ボタンの表示スタイル（アイコンのみ・アイコン＋名前・名前のみ）。
 * @param onThemeToggle テーマ切り替えボタンがクリックされたときのコールバック。
 * @param onWallpaperModeToggle 壁紙透過（CLEAR）ボタンがクリックされたときのコールバック。
 * @param onGridLinesClick グリッド線（GRID）ボタンがクリックされたときのコールバック（グリッド線の設定画面を開く）。
 * @param onAccentColorChange カラーパレットで色が選択されたときのコールバック。
 * @param onUseOriginalIconColorsChange カラーパレット下部の「アプリアイコンはオリジナルカラーを
 *   使用」チェックボックスが切り替えられたときのコールバック。
 * @param onSettingsClick ヘッダーの歯車ボタン（QUICK ACCESSの設定）がクリックされたときのコールバック。
 * @param onLayoutMeasured AUTOの場合の列数・行数と、実際の1ページのスロット数が決まるたびに呼ばれる
 *   コールバック（設定画面で現在の並びや、必要な最小ページ数を表示するために使う）。
 * @param onAddClick 空きスロットがクリックされたときのコールバック。
 * @param showBorder 枠線を表示するかどうか。
 */
@Composable
fun QuickAccessSection(
    modifier: Modifier = Modifier,
    slots: List<QuickActionId?> = emptyList(),
    isWallpaperMode: Boolean = false,
    gridSize: SlotGridSize? = null,
    pageCount: Int = 1,
    accentColor: Color = DefaultAccentColor,
    useOriginalIconColors: Boolean = false,
    showBorder: Boolean = true,
    buttonStyle: QuickButtonStyle = QuickButtonStyle.NAME_ONLY,
    onThemeToggle: () -> Unit = {},
    onWallpaperModeToggle: () -> Unit = {},
    onGridLinesClick: () -> Unit = {},
    onAccentColorChange: (Color) -> Unit = {},
    onUseOriginalIconColorsChange: (Boolean) -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onLayoutMeasured: (autoColumns: Int, autoRows: Int, pageSize: Int) -> Unit = { _, _, _ -> },
    onAddClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val dragState = LocalAppDragState.current
    var showColorPicker by remember { mutableStateOf(false) }
    var showVolumeControl by remember { mutableStateOf(false) }
    var showBrightnessControl by remember { mutableStateOf(false) }
    // ホームボタンが押されたら、開いているポップアップ（カラーパレット・音量・明るさ）を閉じる
    val homePressedSignal = LocalHomePressedSignal.current
    LaunchedEffect(homePressedSignal) {
        if (homePressedSignal == 0) return@LaunchedEffect
        showColorPicker = false
        showVolumeControl = false
        showBrightnessControl = false
    }

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
            QuickActionId.WALLPAPER_TRANSPARENT -> onWallpaperModeToggle()
            QuickActionId.GRID_LINES -> onGridLinesClick()
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
                Spacer(modifier = Modifier.weight(1f))
                // QUICK ACCESSの設定（縦横で同じ並びにするか・スロットの並び・ページ数）を開く歯車ボタン
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "QUICK ACCESS Settings",
                    tint = LocalCyberColors.current.accent,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(onClick = onSettingsClick)
                        .padding(2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // AUTO（ウィジェットの大きさから自動で決める）の場合の列数・行数
                val autoColumns = adaptiveSlotCount(maxWidth, BaseColumns, MinButtonWidth, MaxButtonWidth, ButtonSpacing)
                val autoRows = adaptiveSlotCount(maxHeight, BaseRows, MinButtonHeight, MaxButtonHeight, ButtonSpacing)
                // 設定画面で並びを指定している場合はそちらを使う
                val columns = gridSize?.columns ?: autoColumns
                val rows = gridSize?.rows ?: autoRows
                val pageSize = columns * rows
                LaunchedEffect(autoColumns, autoRows, pageSize) {
                    onLayoutMeasured(autoColumns, autoRows, pageSize)
                }
                // ページ数は設定画面で指定した数。ボタンが入っているページが隠れないよう、それより少なくはしない
                val displayedPageCount = maxOf(pageCount, requiredSlotGridPages(slots, pageSize) { it == null })
                val pagerState = rememberPagerState(pageCount = { displayedPageCount })

                HorizontalPager(
                    state = pagerState,
                    // ドラッグ中に端でページ送りしても、ドラッグ元のスロットが破棄されないよう全ページを保持する
                    beyondViewportPageCount = displayedPageCount - 1,
                    modifier = Modifier.fillMaxSize().dragEdgeAutoScroll(pagerState)
                ) { page ->
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
                                    val index = page * pageSize + rowIndex * columns + col
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
                                                icon = action.icon,
                                                style = buttonStyle,
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
