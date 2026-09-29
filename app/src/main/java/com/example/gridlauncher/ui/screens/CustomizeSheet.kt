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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.outlined.SettingsBackupRestore
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.billing.ProManager
import com.example.gridlauncher.ui.components.ProBadge
import com.example.gridlauncher.ui.components.consumeUpwardSheetFling
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.components.AccentColorPickerDialog
import com.example.gridlauncher.ui.components.DefaultAccentColor
import com.example.gridlauncher.ui.components.DefaultAccentColor2
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.CyberColors
import com.example.gridlauncher.ui.theme.CyberFontOption
import com.example.gridlauncher.ui.theme.DarkBgColor
import com.example.gridlauncher.ui.theme.DarkBorderColor
import com.example.gridlauncher.ui.theme.DarkCoreColor
import com.example.gridlauncher.ui.theme.DarkPanelColor
import com.example.gridlauncher.ui.theme.DarkTextColor
import com.example.gridlauncher.ui.theme.LightBgColor
import com.example.gridlauncher.ui.theme.LightBorderColor
import com.example.gridlauncher.ui.theme.LightCoreColor
import com.example.gridlauncher.ui.theme.LightPanelColor
import com.example.gridlauncher.ui.theme.LightTextColor
import com.example.gridlauncher.ui.theme.ThemePreset
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.DOCK_MAX_SLOTS_PER_PAGE
import com.example.gridlauncher.util.HeaderTitle
import com.example.gridlauncher.util.DOCK_MIN_SLOTS_PER_PAGE
import com.example.gridlauncher.util.SLOT_GRID_MAX_PAGES
import com.example.gridlauncher.util.StackAutoRotateIntervalOptions
import com.example.gridlauncher.util.StackAutoRotateSettings
import com.example.gridlauncher.util.stackAutoRotateIntervalLabel

/**
 * バッテリーコア（歯車アイコン）のタップで開く、ランチャーの見た目をカスタマイズするボトムシート。
 * 壁紙・壁紙透過・テーマ・アクセントカラー・アプリアイコンの配色・ウィジェットの枠線（グリッド線）を
 * まとめて設定できる。下部には従来のコアメニューにあった端末設定・電源メニューへの導線も残す。
 *
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onWallpaperModeChange 壁紙透過スイッチが切り替えられたときのコールバック。
 * @param isDarkTheme ダークテーマかどうか。
 * @param onDarkThemeChange テーマが選択されたときのコールバック（true=ダーク）。
 * @param themePreset 使っているテーマ（配色・フォントのプリセット）。
 * @param onThemePresetChange テーマが選択されたときのコールバック。
 * @param fontOption 使っているフォント。
 * @param onFontOptionChange フォントが選択されたときのコールバック。
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
 * @param showHeader ヘッダー（時刻・バッテリーなど）を表示しているかどうか（今の画面の向きのもの）。
 * @param onShowHeaderChange ヘッダーの表示スイッチが切り替えられたときのコールバック。
 * @param headerTitle ヘッダー中央（横画面・縦画面（大））に表示する3段の文字。
 * @param onHeaderTitleChange 上記が編集されたときのコールバック。
 * @param showDock DOCKを表示しているかどうか（今の画面の向きのもの）。
 * @param onShowDockChange DOCKの表示スイッチが切り替えられたときのコールバック。
 * @param shareDockAcrossOrientations 縦画面と横画面でDOCKに同じアプリの並びを使うかどうか。
 * @param onShareDockAcrossOrientationsChange 上記のスイッチが切り替えられたときのコールバック。
 * @param dockSlotsPerPage DOCKの1ページに並べるアイコン数（今の画面の向きのもの）。
 * @param onDockSlotsPerPageChange 上記が変更されたときのコールバック。
 * @param dockPageCount DOCKのページ数（今の画面の向きのもの）。
 * @param dockMinPageCount DOCKのアプリが入っているページ数（これより少なくはできない）。
 * @param onDockPageCountChange DOCKのページ数が変更されたときのコールバック。
 * @param showAddWidgetTile ホーム画面の空き領域に「+ ADD WIDGET」タイルを表示しているかどうか。
 * @param onShowAddWidgetTileChange 「+ ADD WIDGET」の表示スイッチが切り替えられたときのコールバック。
 * @param stackAutoRotate ウィジェットスタックの自動切り替えの設定（オンオフと間隔）。
 * @param onStackAutoRotateChange 上記が変更されたときのコールバック。
 * @param hiddenPanels 枠線を非表示にしているウィジェットの集合。
 * @param onSetAllBorders 全ウィジェットの枠線を一括で表示/非表示にするときのコールバック（true=表示）。
 * @param onTogglePanelBorder 個別のウィジェットの枠線が切り替えられたときのコールバック。
 * @param onOpenPowerMenu 電源メニューボタンがタップされたときのコールバック。
 * @param isPro PROを購入済みかどうか。PROでない場合、PROの機能には鍵つきのマークを付け、
 *   使おうとすると[onRequirePro]でPRO解放の案内を出す。
 * @param onRequirePro PROの機能を使おうとしたときのコールバック（使おうとした機能の名前。
 *   空文字なら機能を指定せずにPROの案内を出す）。
 * @param onExportBackup 「バックアップ・復元」の「書き出す」がタップされたときのコールバック。
 * @param onImportBackup 「バックアップ・復元」の「読み込む」がタップされたときのコールバック。
 * @param onDismiss シートが閉じられるときのコールバック。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeSheet(
    isWallpaperMode: Boolean,
    onWallpaperModeChange: (Boolean) -> Unit,
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    themePreset: ThemePreset,
    onThemePresetChange: (ThemePreset) -> Unit,
    fontOption: CyberFontOption,
    onFontOptionChange: (CyberFontOption) -> Unit,
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
    showHeader: Boolean,
    onShowHeaderChange: (Boolean) -> Unit,
    headerTitle: HeaderTitle,
    onHeaderTitleChange: (HeaderTitle) -> Unit,
    showDock: Boolean,
    onShowDockChange: (Boolean) -> Unit,
    shareDockAcrossOrientations: Boolean,
    onShareDockAcrossOrientationsChange: (Boolean) -> Unit,
    dockSlotsPerPage: Int,
    onDockSlotsPerPageChange: (Int) -> Unit,
    dockPageCount: Int,
    dockMinPageCount: Int,
    onDockPageCountChange: (Int) -> Unit,
    showAddWidgetTile: Boolean,
    onShowAddWidgetTileChange: (Boolean) -> Unit,
    stackAutoRotate: StackAutoRotateSettings,
    onStackAutoRotateChange: (StackAutoRotateSettings) -> Unit,
    hiddenPanels: Set<WidgetPanel>,
    onSetAllBorders: (Boolean) -> Unit,
    onTogglePanelBorder: (WidgetPanel) -> Unit,
    onOpenPowerMenu: () -> Unit,
    isPro: Boolean,
    onRequirePro: (featureName: String) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current
    var colorPickerTarget by remember { mutableStateOf<Int?>(null) } // パレットで編集中のアクセントカラー（1 or 2）
    var showPanelAccents by remember { mutableStateOf(false) }

    // PROの機能（DOCKの詳細設定・スタックの自動切り替え）は、PROでない場合は変えずにPRO解放の案内を出す
    // （初期の状態＝縦横で同じ並び・自動切り替えオフに戻す操作と、ページ数を減らす操作は誰でもできる）
    val gatedShareDockChange: (Boolean) -> Unit = { share ->
        if (share || isPro) onShareDockAcrossOrientationsChange(share) else onRequirePro("DOCK の縦横別々の並び")
    }
    val gatedDockSlotsPerPageChange: (Int) -> Unit = { count ->
        if (isPro) onDockSlotsPerPageChange(count) else onRequirePro("DOCK の1ページのアイコン数")
    }
    val gatedDockPageCountChange: (Int) -> Unit = { count ->
        if (count < dockPageCount || isPro) onDockPageCountChange(count) else onRequirePro("DOCK のページ数")
    }
    val gatedStackAutoRotateChange: (StackAutoRotateSettings) -> Unit = { settings ->
        if (!settings.enabled || isPro) onStackAutoRotateChange(settings) else onRequirePro("スタックの自動切り替え")
    }

    colorPickerTarget?.let { target ->
        val isAccent2 = target == 2
        AccentColorPickerDialog(
            currentColor = if (isAccent2) accentColor2 else accentColor,
            useOriginalIconColors = useOriginalIconColors,
            onColorSelected = if (isAccent2) onAccentColor2Change else onAccentColorChange,
            onUseOriginalIconColorsChange = onUseOriginalIconColorsChange,
            title = "ACCENT COLOR $target",
            defaultColor = if (isAccent2) DefaultAccentColor2 else DefaultAccentColor,
            isPro = isPro,
            onRequirePro = { onRequirePro("パレットでの自由な色選び") },
            onDismiss = { colorPickerTarget = null }
        )
    }

    // シートが画面の上端（ステータスバーの裏）まで届くと、ModalBottomSheetはシートの位置に
    // 応じてステータスバー分の上余白を増減させるため、ドラッグするたびにシートの高さ＝アンカーが
    // 変わって元の位置へ引き戻され、スワイプで閉じられなくなる（Galaxyのカバー画面など、項目が
    // 画面に収まりきらない小さい画面で発生）。中身の高さに上限を設けてシートが上端に届かない
    // ようにし、収まらない分は中身をスクロールさせる
    val windowInfo = LocalWindowInfo.current
    // DOCKの並びは画面の向きごとの設定なので、どちらの向きの設定かを表示する
    val isPortrait = LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT
    val orientationLabel = if (isPortrait) "縦画面" else "横画面"
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
                .consumeUpwardSheetFling()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("CUSTOMIZE // SYSTEM CONFIG", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            Spacer(modifier = Modifier.height(2.dp))

            // PRO（有料機能の買い切り解放）の状態と購入画面への入口
            ProStatusCard(isPro = isPro, onOpenPro = { onRequirePro("") })

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

            // テーマ（配色・フォントのプリセット）とライト/ダークの切り替え。プリセットはPROの機能
            CustomizeCard {
                CustomizeRowContent(
                    icon = if (isDarkTheme || themePreset != ThemePreset.STANDARD) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    title = "テーマ",
                    description = when {
                        themePreset != ThemePreset.STANDARD -> "${themePreset.label}（ライト/ダークを選ぶと STANDARD に戻ります）"
                        isDarkTheme -> "STANDARD // ダーク"
                        else -> "STANDARD // ライト"
                    }
                ) {
                    ThemeSegmentedToggle(
                        isDarkTheme = isDarkTheme || themePreset != ThemePreset.STANDARD,
                        onDarkThemeChange = onDarkThemeChange
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    ThemePreset.entries.forEach { preset ->
                        ThemePresetOption(
                            preset = preset,
                            isDarkTheme = isDarkTheme,
                            selected = preset == themePreset,
                            locked = !isPro && preset != ThemePreset.STANDARD,
                            onClick = {
                                if (preset == ThemePreset.STANDARD || isPro) onThemePresetChange(preset) else onRequirePro("テーマ（${preset.label}）")
                            }
                        )
                    }
                }
            }

            // フォント（アプリ全体の英数字のフォント）。SHARE TECH MONO以外はPROの機能
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.TextFields,
                    title = "フォント",
                    description = "${fontOption.label}（日本語は端末の標準フォントで表示します）"
                ) {}
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    CyberFontOption.entries.forEach { font ->
                        FontOptionChip(
                            font = font,
                            selected = font == fontOption,
                            locked = !isPro && font != CyberFontOption.SHARE_TECH_MONO,
                            onClick = {
                                if (font == CyberFontOption.SHARE_TECH_MONO || isPro) onFontOptionChange(font) else onRequirePro("フォント（${font.label}）")
                            }
                        )
                    }
                }
            }

            // アクセントカラー1・2の変更と、ウィジェットごとにどちらを使うかの選択
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.Palette,
                    title = "アクセントカラー",
                    description = "1・2をタップしてパレットで色を変えます"
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        AccentSwatchButton(label = "1", color = accentColor, onClick = { colorPickerTarget = 1 })
                        // アクセントカラー2はPROの機能
                        Box {
                            AccentSwatchButton(
                                label = "2",
                                color = accentColor2,
                                onClick = { if (isPro) colorPickerTarget = 2 else onRequirePro("アクセントカラー2") }
                            )
                            if (!isPro) ProBadge(modifier = Modifier.align(Alignment.BottomCenter))
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                // ウィジェットごとの配色（アクセントカラー1・2のどちらを使うか）はPROの機能
                ExpandHeader(
                    label = if (isPro) "ウィジェットごとに設定" else "ウィジェットごとに設定（PRO）",
                    expanded = showPanelAccents,
                    onToggle = { if (isPro) showPanelAccents = !showPanelAccents else onRequirePro("ウィジェットごとの配色") }
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

            // ヘッダー・DOCKの表示切り替え（非表示にすると、その分ウィジェットのエリアが広がる）
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.VerticalAlignTop,
                    title = "ヘッダーの表示（$orientationLabel）",
                    description = "非表示にすると、この画面は長押しメニューなどから開けます",
                    modifier = Modifier.clickable { onShowHeaderChange(!showHeader) }
                ) {
                    CyberSwitch(checked = showHeader, onCheckedChange = onShowHeaderChange)
                }
                // ヘッダー中央の3段の文字の編集（中央の表記が出る横画面のときだけ表示する）
                AnimatedVisibility(visible = showHeader && !isPortrait, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        Column(
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "ヘッダー中央の文字",
                                    fontFamily = CyberFont,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.text,
                                    modifier = Modifier.weight(1f)
                                )
                                ProBadgeIfLocked(isPro)
                                if (isPro && headerTitle != HeaderTitle.Default) {
                                    Text(
                                        "元に戻す",
                                        fontFamily = CyberFont,
                                        fontSize = 11.sp,
                                        color = colors.accent,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onHeaderTitleChange(HeaderTitle.Default) }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            // 文字の書き換えはPROの機能。PROでない場合は入力欄を触れなくし、タップでPRO解放の案内を出す
                            val shownTitle = if (isPro) headerTitle else HeaderTitle.Default
                            Box {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    HeaderTitleField(label = "上段", value = shownTitle.top, enabled = isPro) { onHeaderTitleChange(headerTitle.copy(top = it)) }
                                    HeaderTitleField(label = "中段", value = shownTitle.main, enabled = isPro) { onHeaderTitleChange(headerTitle.copy(main = it)) }
                                    HeaderTitleField(label = "下段", value = shownTitle.bottom, enabled = isPro) { onHeaderTitleChange(headerTitle.copy(bottom = it)) }
                                }
                                if (!isPro) {
                                    Box(modifier = Modifier.matchParentSize().clickable { onRequirePro("ヘッダー中央の文字の書き換え") })
                                }
                            }
                        }
                    }
                }
            }
            // DOCKの表示と、1ページのアイコン数・ページ数（今の画面の向きの設定。縦横で別々に保存する）
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.VerticalAlignBottom,
                    title = "DOCK の表示（$orientationLabel）",
                    description = "非表示にすると、その分ウィジェットのエリアが広がります",
                    modifier = Modifier.clickable { onShowDockChange(!showDock) }
                ) {
                    CyberSwitch(checked = showDock, onCheckedChange = onShowDockChange)
                }
                AnimatedVisibility(visible = showDock, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        // 縦画面と横画面で同じアプリを並べるかどうか（APP LISTと同様）
                        CustomizeRowContent(
                            icon = Icons.Outlined.ScreenRotation,
                            title = "縦画面・横画面で同じ並びにする",
                            description = if (shareDockAcrossOrientations) {
                                "縦画面と横画面で同じアプリを並べます"
                            } else {
                                "縦画面と横画面で別々に並べます（オンにすると今の向きの並びにそろえます）"
                            },
                            modifier = Modifier.clickable { gatedShareDockChange(!shareDockAcrossOrientations) }
                        ) {
                            ProBadgeIfLocked(isPro)
                            CyberSwitch(checked = shareDockAcrossOrientations, onCheckedChange = gatedShareDockChange)
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        Column(
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "今の画面の向き（$orientationLabel）の設定です",
                                    fontFamily = CyberFont,
                                    fontSize = 10.sp,
                                    color = colors.text.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                )
                                // アイコン数・ページ数の変更はPROの機能
                                if (!isPro) ProBadge()
                            }
                            StepperRow(
                                label = "1ページのアイコン数",
                                value = dockSlotsPerPage,
                                min = DOCK_MIN_SLOTS_PER_PAGE,
                                max = DOCK_MAX_SLOTS_PER_PAGE,
                                onChange = gatedDockSlotsPerPageChange
                            )
                            StepperRow(
                                label = "ページ数",
                                value = dockPageCount,
                                min = dockMinPageCount,
                                max = SLOT_GRID_MAX_PAGES,
                                onChange = gatedDockPageCountChange
                            )
                            if (dockMinPageCount > 1) {
                                Text(
                                    "アプリが入っている${dockMinPageCount}ページより少なくはできません",
                                    fontFamily = CyberFont,
                                    fontSize = 10.sp,
                                    color = colors.text.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
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

            // ウィジェットスタック（重ねたウィジェット）の自動切り替えのオンオフと間隔
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.ViewCarousel,
                    title = "スタックの自動切り替え",
                    description = "重ねたウィジェットを、一定の間隔で次に切り替えます",
                    modifier = Modifier.clickable { gatedStackAutoRotateChange(stackAutoRotate.copy(enabled = !stackAutoRotate.enabled)) }
                ) {
                    ProBadgeIfLocked(isPro)
                    CyberSwitch(
                        checked = stackAutoRotate.enabled,
                        onCheckedChange = { gatedStackAutoRotateChange(stackAutoRotate.copy(enabled = it)) }
                    )
                }
                AnimatedVisibility(visible = stackAutoRotate.enabled, enter = expandVertically(), exit = shrinkVertically()) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                        Column(
                            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("切り替えの間隔", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                StackAutoRotateIntervalOptions.forEach { seconds ->
                                    IntervalOption(
                                        label = stackAutoRotateIntervalLabel(seconds),
                                        selected = seconds == stackAutoRotate.intervalSeconds,
                                        onClick = { gatedStackAutoRotateChange(stackAutoRotate.copy(intervalSeconds = seconds)) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // グリッド線（ウィジェットの枠線）の編集
            GridLinesCard(
                hiddenPanels = hiddenPanels,
                onSetAllBorders = onSetAllBorders,
                onTogglePanelBorder = onTogglePanelBorder
            )

            // 設定のバックアップ（ファイルへ書き出す）と復元（ファイルから読み込む）。PROの機能
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.SettingsBackupRestore,
                    title = "バックアップ・復元",
                    description = "配置・配色・各種設定をファイルに保存し、別の端末や再インストール後に戻せます"
                ) {
                    ProBadgeIfLocked(isPro)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 12.dp)
                ) {
                    IntervalOption(label = "書き出す", selected = false, onClick = onExportBackup, modifier = Modifier.weight(1f))
                    IntervalOption(label = "読み込む", selected = false, onClick = onImportBackup, modifier = Modifier.weight(1f))
                }
            }

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

/**
 * PRO（有料機能の買い切り解放）の状態を示すカード。未購入ならタップでPRO解放の案内を開く。
 * デバッグビルドでは、Google Play に商品を登録しなくても動作を確かめられるよう、
 * 開発用にPROの状態を切り替えるスイッチも出す。
 */
@Composable
private fun ProStatusCard(isPro: Boolean, onOpenPro: () -> Unit) {
    val colors = LocalCyberColors.current
    CustomizeCard {
        CustomizeRowContent(
            icon = Icons.Outlined.WorkspacePremium,
            title = if (isPro) "GRIDLAUNCHER PRO // 解放済み" else "GRIDLAUNCHER PRO",
            description = if (isPro) "すべての機能が使えます。ありがとうございます！" else "スタック・配色・詳細設定などの機能を解放します",
            modifier = if (isPro) Modifier else Modifier.clickable(onClick = onOpenPro)
        ) {
            if (!isPro) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = colors.text.copy(alpha = 0.5f))
        }
        if (ProManager.isDebugBuild()) {
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { ProManager.setDebugOverride(!isPro) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    "開発用: PRO を有効にする（デバッグビルドのみ）",
                    fontFamily = CyberFont,
                    fontSize = 11.sp,
                    color = colors.text.copy(alpha = 0.7f),
                    modifier = Modifier.weight(1f)
                )
                CyberSwitch(checked = isPro, onCheckedChange = { ProManager.setDebugOverride(it) })
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

/**
 * テーマの選択肢1つ分。そのテーマの背景に、アクセントカラー1・2の帯と、テーマのフォントで書いた名前を
 * 小さく並べて、見た目が分かるようにする（STANDARDは今のライト/ダークの配色で表示する）。
 */
@Composable
private fun ThemePresetOption(
    preset: ThemePreset,
    isDarkTheme: Boolean,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalCyberColors.current
    val previewColors = preset.colors ?: if (isDarkTheme) {
        CyberColors(DarkBgColor, DarkPanelColor, preset.accent, DarkTextColor, DarkBorderColor, DarkCoreColor)
    } else {
        CyberColors(LightBgColor, LightPanelColor, preset.accent, LightTextColor, LightBorderColor, LightCoreColor)
    }
    Box(
        modifier = Modifier
            .size(width = 88.dp, height = 64.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(previewColors.bg)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else previewColors.border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                Box(modifier = Modifier.size(width = 22.dp, height = 5.dp).background(preset.accent))
                Box(modifier = Modifier.size(width = 12.dp, height = 5.dp).background(preset.accent2))
            }
            Text(
                preset.label,
                fontFamily = preset.font.fontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = previewColors.text,
                maxLines = 2,
                lineHeight = 12.sp
            )
        }
        if (locked) ProBadge(modifier = Modifier.align(Alignment.BottomEnd))
    }
}

/** フォントの選択肢1つ分。そのフォントで、時刻と名前の見本を表示する。 */
@Composable
private fun FontOptionChip(font: CyberFontOption, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    Box(
        modifier = Modifier
            .size(width = 96.dp, height = 56.dp)
            .clip(RoundedCornerShape(6.dp))
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column {
            Text("12:34", fontFamily = font.fontFamily, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.text, maxLines = 1)
            Text(font.label, fontFamily = font.fontFamily, fontSize = 8.sp, color = colors.text.copy(alpha = 0.6f), maxLines = 1)
        }
        if (locked) ProBadge(modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** 自動切り替えの間隔の選択肢1つ分。選ばれているものはアクセントカラーで塗りつぶす。 */
@Composable
private fun IntervalOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalCyberColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, if (selected) colors.accent else colors.border, RoundedCornerShape(4.dp))
            .background(if (selected) colors.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    ) {
        Text(
            label,
            fontFamily = CyberFont,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) colors.onAccent else colors.text.copy(alpha = 0.7f),
            maxLines = 1
        )
    }
}

/** ヘッダー中央の文字1段分の入力欄。 */
@Composable
private fun HeaderTitleField(label: String, value: String, enabled: Boolean = true, onValueChange: (String) -> Unit) {
    val colors = LocalCyberColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        label = { Text(label, fontFamily = CyberFont, fontSize = 11.sp) },
        singleLine = true,
        textStyle = TextStyle(fontFamily = CyberFont, fontSize = 13.sp, color = colors.text),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.accent,
            unfocusedBorderColor = colors.border,
            focusedLabelColor = colors.accent,
            unfocusedLabelColor = colors.text.copy(alpha = 0.5f),
            cursorColor = colors.accent
        ),
        modifier = Modifier.fillMaxWidth()
    )
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
