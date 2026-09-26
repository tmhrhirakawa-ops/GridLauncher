package com.example.gridlauncher.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.components.AccentColorPickerDialog
import com.example.gridlauncher.ui.components.DefaultAccentColor
import com.example.gridlauncher.ui.components.DefaultAccentColor2
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * バッテリーコア（歯車アイコン）のタップで開く、ランチャーの見た目をカスタマイズするボトムシート。
 * 壁紙・壁紙透過・テーマ・アクセントカラー・アプリアイコンの配色・ウィジェットの枠線（グリッド線）を
 * まとめて設定できる。下部には従来のコアメニューにあった端末設定・電源メニューへの導線も残す。
 *
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onWallpaperModeChange 壁紙透過スイッチが切り替えられたときのコールバック。
 * @param isDarkTheme ダークテーマかどうか。
 * @param onDarkThemeChange テーマが選択されたときのコールバック（true=ダーク）。
 * @param accentColor 現在のアクセントカラー1。
 * @param onAccentColorChange カラーパレットでアクセントカラー1が選択されたときのコールバック。
 * @param accentColor2 現在のアクセントカラー2。
 * @param onAccentColor2Change カラーパレットでアクセントカラー2が選択されたときのコールバック。
 * @param accent2Panels アクセントカラー2を使うウィジェットの集合（それ以外は1を使う）。
 * @param onPanelAccentChange ウィジェットごとのアクセントカラーが選択されたときのコールバック
 *   （trueならアクセントカラー2を使う）。
 * @param useOriginalIconColors アプリアイコンをオリジナルカラーのまま表示しているかどうか。
 * @param onUseOriginalIconColorsChange アイコン配色のスイッチが切り替えられたときのコールバック。
 * @param onOpenAppListSettings 「APP LISTの詳細設定」がタップされたときのコールバック（APP LISTの設定画面を開く）。
 * @param onOpenQuickAccessSettings 「QUICK ACCESSの詳細設定」がタップされたときのコールバック（QUICK ACCESSの設定画面を開く）。
 * @param showAddWidgetTile ホーム画面の空き領域に「+ ADD WIDGET」タイルを表示しているかどうか。
 * @param onShowAddWidgetTileChange 「+ ADD WIDGET」の表示スイッチが切り替えられたときのコールバック。
 * @param hiddenPanels 枠線を非表示にしているウィジェットの集合。
 * @param onSetAllBorders 全ウィジェットの枠線を一括で表示/非表示にするときのコールバック（true=表示）。
 * @param onTogglePanelBorder 個別のウィジェットの枠線が切り替えられたときのコールバック。
 * @param onOpenPowerMenu 電源メニューボタンがタップされたときのコールバック。
 * @param onDismiss シートが閉じられるときのコールバック。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(
    isWallpaperMode: Boolean,
    onWallpaperModeChange: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    accentColor: Color,
    onAccentColorChange: (Color) -> Unit,
    accentColor2: Color,
    onAccentColor2Change: (Color) -> Unit,
    accent2Panels: Set<WidgetPanel>,
    onPanelAccentChange: (WidgetPanel, Boolean) -> Unit,
    useOriginalIconColors: Boolean,
    onUseOriginalIconColorsChange: (Boolean) -> Unit,
    onOpenAppListSettings: () -> Unit,
    onOpenQuickAccessSettings: () -> Unit,
    showAddWidgetTile: Boolean,
    onShowAddWidgetTileChange: (Boolean) -> Unit,
    hiddenPanels: Set<WidgetPanel>,
    onSetAllBorders: (Boolean) -> Unit,
    onTogglePanelBorder: (WidgetPanel) -> Unit,
    onOpenPowerMenu: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current
    var colorPickerTarget by remember { mutableStateOf<Int?>(null) } // パレットで編集中のアクセントカラー（1 or 2）
    var showPanelAccents by remember { mutableStateOf(false) }

    colorPickerTarget?.let { target ->
        val isAccent2 = target == 2
        AccentColorPickerDialog(
            currentColor = if (isAccent2) accentColor2 else accentColor,
            useOriginalIconColors = useOriginalIconColors,
            onColorSelected = if (isAccent2) onAccentColor2Change else onAccentColorChange,
            onUseOriginalIconColorsChange = onUseOriginalIconColorsChange,
            title = "ACCENT COLOR $target",
            defaultColor = if (isAccent2) DefaultAccentColor2 else DefaultAccentColor,
            onDismiss = { colorPickerTarget = null }
        )
    }

    // シートが画面の上端（ステータスバーの裏）まで届くと、ModalBottomSheetはシートの位置に
    // 応じてステータスバー分の上余白を増減させるため、ドラッグするたびにシートの高さ＝アンカーが
    // 変わって元の位置へ引き戻され、スワイプで閉じられなくなる（Galaxyのカバー画面など、項目が
    // 画面に収まりきらない小さい画面で発生）。中身の高さに上限を設けてシートが上端に届かない
    // ようにし、収まらない分は中身をスクロールさせる
    val windowInfo = LocalWindowInfo.current
    val maxContentHeight = with(LocalDensity.current) { windowInfo.containerSize.height.toDp() } * 0.75f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxContentHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("CUSTOMIZE // SYSTEM CONFIG", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            Spacer(modifier = Modifier.height(2.dp))

            // 壁紙の変更（端末標準の壁紙選択画面を開く）
            CustomizeRow(
                icon = Icons.Outlined.Wallpaper,
                title = "壁紙の変更",
                description = "端末の壁紙選択画面を開きます",
                onClick = {
                    val intent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                }
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.text.copy(alpha = 0.5f))
            }

            // 壁紙の透過
            CustomizeRow(
                icon = Icons.Outlined.Opacity,
                title = "壁紙の透過",
                description = "ホーム画面の背景を透過して壁紙を表示します",
                onClick = { onWallpaperModeChange(!isWallpaperMode) }
            ) {
                CyberSwitch(checked = isWallpaperMode, onCheckedChange = onWallpaperModeChange)
            }

            // テーマ切り替え
            CustomizeRow(
                icon = if (isDarkTheme) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                title = "テーマ",
                description = if (isDarkTheme) "ダーク" else "ライト"
            ) {
                ThemeSegmentedToggle(isDarkTheme = isDarkTheme, onDarkThemeChange = onDarkThemeChange)
            }

            // アクセントカラー1・2の変更と、ウィジェットごとにどちらを使うかの選択
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.Palette,
                    title = "アクセントカラー",
                    description = "1・2をタップしてパレットで色を変えます"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        AccentSwatchButton(label = "1", color = accentColor, onClick = { colorPickerTarget = 1 })
                        AccentSwatchButton(label = "2", color = accentColor2, onClick = { colorPickerTarget = 2 })
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                ExpandHeader(
                    label = "ウィジェットごとに設定",
                    expanded = showPanelAccents,
                    onToggle = { showPanelAccents = !showPanelAccents }
                )
                AnimatedVisibility(visible = showPanelAccents, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 外部ウィジェット（APP WIDGET）は他アプリが描画するため、アクセントカラーの対象外
                        WidgetPanel.entries.filter { it != WidgetPanel.APPWIDGET }.forEach { panel ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(panel.label, fontFamily = CyberFont, fontSize = 12.sp, color = colors.text, modifier = Modifier.weight(1f))
                                AccentSlotToggle(
                                    useAccent2 = panel in accent2Panels,
                                    accentColor = accentColor,
                                    accentColor2 = accentColor2,
                                    onChange = { useAccent2 -> onPanelAccentChange(panel, useAccent2) }
                                )
                            }
                        }
                    }
                }
            }

            // アプリアイコンをオリジナルに戻す（アクセントカラーのデュオトーン加工をしない）
            CustomizeRow(
                icon = Icons.Outlined.Image,
                title = "アプリアイコンをオリジナルに戻す",
                description = "アクセントカラーで加工せず、本来の色で表示します",
                onClick = { onUseOriginalIconColorsChange(!useOriginalIconColors) }
            ) {
                CyberSwitch(checked = useOriginalIconColors, onCheckedChange = onUseOriginalIconColorsChange)
            }

            // APP LISTの詳細設定（ICON ONLY・縦横で同じ並びにするか・アイコンの並び・ページ数）。
            // APP LISTのヘッダーの歯車ボタンと同じ設定画面を開く
            CustomizeRow(
                icon = Icons.Outlined.GridView,
                title = "APP LIST の詳細設定",
                description = "アイコンの並び・ページ数・ICON ONLY などを設定します",
                onClick = onOpenAppListSettings
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.text.copy(alpha = 0.5f))
            }

            // QUICK ACCESSの詳細設定（縦横で同じ並びにするか・ボタンの並び・ページ数）。
            // QUICK ACCESSのヘッダーの歯車ボタンと同じ設定画面を開く
            CustomizeRow(
                icon = Icons.Outlined.Dashboard,
                title = "QUICK ACCESS の詳細設定",
                description = "ボタンの並び・ページ数などを設定します",
                onClick = onOpenQuickAccessSettings
            ) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.text.copy(alpha = 0.5f))
            }

            // 「+ ADD WIDGET」タイルの表示切り替え（非表示でも、何もないところの長押しメニューから追加できる）
            CustomizeRow(
                icon = Icons.Outlined.AddBox,
                title = "ADD WIDGET の表示",
                description = "非表示でも、何もないところの長押しで追加できます",
                onClick = { onShowAddWidgetTileChange(!showAddWidgetTile) }
            ) {
                CyberSwitch(checked = showAddWidgetTile, onCheckedChange = onShowAddWidgetTileChange)
            }

            // グリッド線（ウィジェットの枠線）の編集
            GridLinesCard(
                hiddenPanels = hiddenPanels,
                onSetAllBorders = onSetAllBorders,
                onTogglePanelBorder = onTogglePanelBorder
            )

            // 従来のコアメニューにあった、端末設定・電源メニューへの導線
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                FooterButton(
                    icon = Icons.Outlined.Settings,
                    label = "端末設定",
                    onClick = {
                        onDismiss()
                        context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    },
                    modifier = Modifier.weight(1f)
                )
                FooterButton(
                    icon = Icons.Outlined.PowerSettingsNew,
                    label = "電源メニュー",
                    onClick = {
                        onDismiss()
                        onOpenPowerMenu()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/** カスタマイズ項目1つ分の枠（パネル色＋枠線の角丸カード）。 */
@Composable
internal fun CustomizeCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalCyberColors.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = colors.panel.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

/**
 * アイコン・タイトル・説明文と、右端のコントロール（スイッチ等）を並べたカスタマイズ項目。
 * [onClick]を指定すると行全体がタップ可能になる。
 */
@Composable
internal fun CustomizeRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    CustomizeCard {
        CustomizeRowContent(
            icon = icon,
            title = title,
            description = description,
            modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            trailing = trailing
        )
    }
}

@Composable
internal fun CustomizeRowContent(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit
) {
    val colors = LocalCyberColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = CyberFont, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.text)
            Text(description, fontFamily = CyberFont, fontSize = 10.sp, color = colors.text.copy(alpha = 0.6f))
        }
        Spacer(modifier = Modifier.width(12.dp))
        trailing()
    }
}

/** アクセントカラーに合わせたスイッチ。 */
@Composable
internal fun CyberSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = LocalCyberColors.current
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onAccent,
            checkedTrackColor = colors.accent,
            checkedBorderColor = colors.accent,
            uncheckedThumbColor = colors.text.copy(alpha = 0.6f),
            uncheckedTrackColor = colors.bg,
            uncheckedBorderColor = colors.border
        )
    )
}

/** カード内の「ウィジェットごとに設定」など、タップで下の項目を開閉する見出し行。 */
@Composable
private fun ExpandHeader(label: String, expanded: Boolean, onToggle: () -> Unit) {
    val colors = LocalCyberColors.current
    val expandRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "expandRotation")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(label, fontFamily = CyberFont, fontSize = 11.sp, color = colors.text.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = null,
            tint = colors.text.copy(alpha = 0.5f),
            modifier = Modifier.rotate(expandRotation)
        )
    }
}

/** 番号（1/2）を添えたアクセントカラーの見本。タップでその色のパレットを開く。 */
@Composable
private fun AccentSwatchButton(label: String, color: Color, onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, colors.border, CircleShape)
        )
        Text(label, fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.text.copy(alpha = 0.7f))
    }
}

/** ウィジェットがアクセントカラー1と2のどちらを使うかを選ぶ2択のセグメントボタン。 */
@Composable
private fun AccentSlotToggle(useAccent2: Boolean, accentColor: Color, accentColor2: Color, onChange: (Boolean) -> Unit) {
    val colors = LocalCyberColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
    ) {
        listOf(false to accentColor, true to accentColor2).forEach { (isAccent2, slotColor) ->
            val selected = useAccent2 == isAccent2
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(if (selected) slotColor.copy(alpha = 0.2f) else Color.Transparent)
                    .clickable { onChange(isAccent2) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(slotColor))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isAccent2) "2" else "1",
                    fontFamily = CyberFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.text else colors.text.copy(alpha = 0.4f)
                )
            }
        }
    }
}

/** DARK / LIGHT を切り替える2択のセグメントボタン。 */
@Composable
private fun ThemeSegmentedToggle(isDarkTheme: Boolean, onDarkThemeChange: (Boolean) -> Unit) {
    val colors = LocalCyberColors.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
    ) {
        listOf(true to "DARK", false to "LIGHT").forEach { (dark, label) ->
            val selected = isDarkTheme == dark
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .background(if (selected) colors.accent else Color.Transparent)
                    .clickable { onDarkThemeChange(dark) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    label,
                    fontFamily = CyberFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.onAccent else colors.text.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/** シート下部の、端末設定・電源メニューを開く小さなボタン。 */
@Composable
private fun FooterButton(icon: ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalCyberColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.border),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.text.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, fontFamily = CyberFont, fontSize = 11.sp, color = colors.text.copy(alpha = 0.8f))
        }
    }
}

/**
 * グリッド線（ウィジェットの枠線）の設定カード。全ウィジェットの一括表示/非表示のスイッチと、
 * 開閉できるウィジェットごとのチェックボックスを持つ。カスタマイズ画面と、QUICK ACCESSの
 * GRIDボタンで開くグリッド線の設定画面（[GridLinesSheet]）で共通に使う。
 *
 * @param hiddenPanels 枠線を非表示にしているウィジェットの集合。
 * @param onSetAllBorders 全ウィジェットの枠線を一括で表示/非表示にするときのコールバック（true=表示）。
 * @param onTogglePanelBorder 個別のウィジェットの枠線が切り替えられたときのコールバック。
 * @param initiallyExpanded 「ウィジェットごとに設定」を最初から開いておくかどうか。
 */
@Composable
internal fun GridLinesCard(
    hiddenPanels: Set<WidgetPanel>,
    onSetAllBorders: (Boolean) -> Unit,
    onTogglePanelBorder: (WidgetPanel) -> Unit,
    initiallyExpanded: Boolean = false
) {
    val colors = LocalCyberColors.current
    var showPanelBorders by remember { mutableStateOf(initiallyExpanded) }
    val allBordersVisible = hiddenPanels.isEmpty()
    CustomizeCard {
        CustomizeRowContent(
            icon = Icons.Outlined.GridOn,
            title = "グリッド線",
            description = when {
                allBordersVisible -> "すべてのウィジェットに表示中"
                hiddenPanels.size == WidgetPanel.entries.size -> "すべて非表示"
                else -> "一部のウィジェットのみ表示中"
            },
            modifier = Modifier.clickable { onSetAllBorders(!allBordersVisible) }
        ) {
            CyberSwitch(checked = allBordersVisible, onCheckedChange = onSetAllBorders)
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
        ExpandHeader(
            label = "ウィジェットごとに設定",
            expanded = showPanelBorders,
            onToggle = { showPanelBorders = !showPanelBorders }
        )
        AnimatedVisibility(visible = showPanelBorders, enter = expandVertically(), exit = shrinkVertically()) {
            Column(modifier = Modifier.padding(start = 4.dp, end = 14.dp, bottom = 6.dp)) {
                WidgetPanel.entries.forEach { panel ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePanelBorder(panel) }
                    ) {
                        Checkbox(
                            checked = panel !in hiddenPanels,
                            onCheckedChange = { onTogglePanelBorder(panel) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.accent,
                                checkmarkColor = colors.onAccent,
                                uncheckedColor = colors.border
                            )
                        )
                        Text(panel.label, fontFamily = CyberFont, fontSize = 12.sp, color = colors.text)
                    }
                }
            }
        }
    }
}
