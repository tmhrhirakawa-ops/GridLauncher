package com.example.gridlauncher.ui.screens

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.model.FolderInfo
import com.example.gridlauncher.model.GridItem
import com.example.gridlauncher.model.PlacedWidget
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.billing.ProManager
import com.example.gridlauncher.ui.LocalHomePressedSignal
import com.example.gridlauncher.ui.components.AppActionDialog
import com.example.gridlauncher.ui.components.ProUpgradeDialog
import com.example.gridlauncher.ui.components.AppSlotStyleDialog
import com.example.gridlauncher.ui.components.IconPackPickerDialog
import com.example.gridlauncher.ui.components.HomeLongPressMenu
import com.example.gridlauncher.ui.components.AppWidgetHostSection
import com.example.gridlauncher.ui.components.MissingPermissionsSheet
import com.example.gridlauncher.ui.components.PermissionRationaleDialog
import com.example.gridlauncher.ui.components.QuickActionSelectorDialog
import com.example.gridlauncher.ui.components.StandaloneAppSlotSection
import com.example.gridlauncher.ui.components.WidgetDeleteConfirmDialog
import com.example.gridlauncher.ui.components.WidgetTypeSelectorDialog
import com.example.gridlauncher.ui.sections.*
import com.example.gridlauncher.ui.drag.AppDragItem
import com.example.gridlauncher.ui.drag.AppDragOverlay
import com.example.gridlauncher.ui.drag.AppDragPayload
import com.example.gridlauncher.ui.drag.AppDragSource
import com.example.gridlauncher.ui.drag.AppDropTarget
import com.example.gridlauncher.ui.drag.LocalAppDragState
import com.example.gridlauncher.ui.drag.boundsOnScreen
import com.example.gridlauncher.ui.drag.rememberAppDragState
import com.example.gridlauncher.ui.theme.*
import com.example.gridlauncher.util.SlotGridSize
import com.example.gridlauncher.util.DockLayout
import com.example.gridlauncher.util.HeaderTitle
import com.example.gridlauncher.util.loadHeaderTitle
import com.example.gridlauncher.util.saveHeaderTitle
import com.example.gridlauncher.util.loadDockLayout
import com.example.gridlauncher.util.dockAppsKeyFor
import com.example.gridlauncher.util.loadShareDockAcrossOrientations
import com.example.gridlauncher.util.saveShareDockAcrossOrientations
import com.example.gridlauncher.util.saveDockLayout
import com.example.gridlauncher.util.AppWidgetHostManager
import com.example.gridlauncher.util.allocateNextAppSlotInstanceId
import com.example.gridlauncher.util.clearAppSlotAssignment
import com.example.gridlauncher.util.findFreeGridSlot
import com.example.gridlauncher.util.findFreeGridSlotNear
import com.example.gridlauncher.util.folderIdFromSlotValue
import com.example.gridlauncher.util.isFolderSlotValue
import com.example.gridlauncher.util.loadAppSlotAssignments
import com.example.gridlauncher.util.loadAccent2WidgetPanels
import com.example.gridlauncher.util.loadSlotGridPageCount
import com.example.gridlauncher.util.SlotGridSection
import com.example.gridlauncher.util.loadSlotGridSize
import com.example.gridlauncher.util.loadShareGridAcrossOrientations
import com.example.gridlauncher.util.gridAppsKeyFor
import com.example.gridlauncher.util.loadFolders
import com.example.gridlauncher.util.loadHiddenWidgetPanels
import com.example.gridlauncher.util.loadPlacedWidgets
import com.example.gridlauncher.util.loadQuickActionSlots
import com.example.gridlauncher.util.loadQuickButtonStyle
import com.example.gridlauncher.util.loadShareQuickActionsAcrossOrientations
import com.example.gridlauncher.util.quickActionsKeyFor
import com.example.gridlauncher.util.saveShareQuickActionsAcrossOrientations
import com.example.gridlauncher.util.OnboardingSteps
import com.example.gridlauncher.util.openPowerMenuOrRequestPermission
import com.example.gridlauncher.util.performSystemActionOrRequestPermission
import com.example.gridlauncher.util.AccessibilityServiceStatus
import com.example.gridlauncher.util.accessibilityServiceStatus
import com.example.gridlauncher.util.DefaultGestureBindings
import com.example.gridlauncher.util.GestureAction
import com.example.gridlauncher.util.GestureBinding
import com.example.gridlauncher.util.HomeGesture
import com.example.gridlauncher.util.loadGestureBindings
import com.example.gridlauncher.util.saveGestureBinding
import com.example.gridlauncher.util.requiredSlotGridPages
import com.example.gridlauncher.util.IconPackManager
import kotlinx.coroutines.delay
import com.example.gridlauncher.util.normalizeStacks
import com.example.gridlauncher.util.LauncherBackup
import com.example.gridlauncher.util.loadStackAutoRotateSettings
import com.example.gridlauncher.util.saveStackAutoRotateSettings
import com.example.gridlauncher.util.saveAccent2WidgetPanels
import com.example.gridlauncher.util.saveSlotGridPageCount
import com.example.gridlauncher.util.saveSlotGridSize
import com.example.gridlauncher.util.saveShareGridAcrossOrientations
import com.example.gridlauncher.util.requestUninstall
import com.example.gridlauncher.util.saveAppSlotAssignment
import com.example.gridlauncher.util.saveFolder
import com.example.gridlauncher.util.saveHiddenWidgetPanels
import com.example.gridlauncher.util.savePlacedWidgets
import com.example.gridlauncher.util.saveQuickActionSlots
import com.example.gridlauncher.util.saveQuickButtonStyle
import com.example.gridlauncher.util.CyberNotificationListener
import com.example.gridlauncher.util.WidgetLayoutMode
import androidx.compose.ui.graphics.Color

/**
 * [android.appwidget.AppWidgetHost.startAppWidgetConfigureActivityForResult]が要求する
 * [Activity]を、Composeの[LocalContext]（`ContextWrapper`でラップされていることがある）から
 * たどって取得する。
 */
private tailrec fun Context.findActivity(): Activity = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("Activityが見つかりませんでした: $this")
}

/**
 * 未設定の権限/設定を知らせるボトムシートを、アプリプロセスの起動につき1回だけ表示する
 * ためのフラグ。ホーム画面に戻るたびに毎回表示されると煩わしいため、画面回転等での
 * 再コンポジションをまたいでプロセスが生きている間は表示済みとして扱う。
 */
private object MissingPermissionsSheetState {
    var shownThisProcess = false
}

/**
 * ウィジェット編集モードでAPP SLOTの✗ボタンが押されたときの対象。
 * 「スロットから削除」か「アンインストール」かをダイアログで選ばせるために保持する。
 * （APP LIST・DOCK・QUICK ACCESS・フォルダの中は、長押し→ドラッグで削除する）
 *
 * @property instanceId 対象のAPP SLOTのinstanceId。
 */
private data class PendingAppSlotRemoval(
    val instanceId: Int,
    val packageName: String,
    val label: String
)

/**
 * アプリを、ウィジェットキャンバスのスロット以外の場所へドロップしたときの、APP SLOT（ウィジェット）
 * として置く待ち。アイコンのみかアイコン＋名前かを選んでもらうまで保持する。
 *
 * @property payload ドロップしたもの（置いたあと、元の場所から外すのに使う）。
 * @property appInfo ドロップしたアプリ。
 * @property positionOnScreen 指を離した位置（スクリーン座標）。この近くの空いている場所に置く。
 */
private data class PendingAppSlotDrop(
    val payload: AppDragPayload,
    val appInfo: AppInfo,
    val positionOnScreen: Offset
)

/**
 * ランチャーのメイン画面。デバイスの向きや画面サイズに基づいて、
 * すべてのセクションのレイアウトを調整します。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CyberLauncherScreen() {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE) }
    // インストール済みアプリの一覧（アプリのインストール・アンインストール・更新で、その場で更新される）
    val allApps by rememberInstalledApps(context)
    AppWidgetHostManager.ensureInitialized(context)

    // PRO（有料機能の買い切り解放）を購入済みかどうか。未購入でPROの機能を使おうとしたときは、
    // その機能の名前を[proPromptFeature]に入れて、PRO解放の案内（購入画面）を表示する
    val isPro by ProManager.isPro.collectAsState()
    var proPromptFeature by remember { mutableStateOf<String?>(null) }
    // PROならtrueを返す。未購入ならPRO解放の案内を出してfalseを返す
    fun requirePro(featureName: String): Boolean {
        if (isPro) return true
        proPromptFeature = featureName
        return false
    }

    // 見た目の設定（ライト/ダーク・テーマ・フォント・アクセントカラー・アイコンパック）。
    // テーマ・フォント・アイコンパックはPROの機能で、PROでない場合は従来どおりの配色・標準のフォント・標準のアイコンを使う
    val theme = rememberLauncherThemeState(prefs)
    ApplyLauncherThemeEffects(context, theme, isPro)
    val activeThemePreset = theme.activeThemePreset(isPro)
    val activeIconPackPackage = theme.activeIconPackPackage(isPro)
    var showIconPackPicker by remember { mutableStateOf(false) }
    val colors = theme.colors(isPro)
    val colors2 = colors.copy(accent = theme.accentColor2)

    // 初回起動時のオンボーディング（デフォルトのホームアプリ設定・通知アクセス・バッテリー
    // 最適化除外・使用状況アクセスへの案内）。完了するまでは、それ以降のメインUI用の状態
    // （アプリ一覧の読み込み等）を準備する必要がないため、ここで早期リターンする
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_completed", false)) }
    if (showOnboarding) {
        // オンボーディングの背景画像はダーク前提のデザインのため、テーマ設定にかかわらずダークの
        // 配色で表示する（アクセントカラーはユーザーの設定を使う）
        val onboardingColors = CyberColors(DarkBgColor, DarkPanelColor, theme.accentColor, DarkTextColor, DarkBorderColor, DarkCoreColor)
        CompositionLocalProvider(LocalCyberColors provides onboardingColors) {
            OnboardingScreen(
                onFinish = {
                    prefs.edit { putBoolean("onboarding_completed", true) }
                    // オンボーディング内で案内済みの内容なので、完了直後に未設定権限の
                    // ボトムシートを重ねて出す必要はない（次回のアプリ起動から対象にする）
                    MissingPermissionsSheetState.shownThisProcess = true
                    showOnboarding = false
                }
            )
        }
        return
    }

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    // ナビゲーションバーが占有している領域（3ボタンナビなら約48dp、ジェスチャーナビなら横棒の分だけ、
    // ジェスチャーのヒントを消していれば0）。固定の余白ではなく、これに合わせて画面端の余白を決める
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues()
    val layoutDirection = LocalLayoutDirection.current
    // 画面下端の余白。ナビゲーションバーがあるときは、その上に少しだけ隙間を空ける（デザイン上の余白を
    // 足すと3ボタンナビで空きすぎるため）。ないとき（ジェスチャーのヒント非表示など）はデザイン上の余白にする
    val bottomScreenPadding = maxOf(
        if (isPortrait) 16.dp else 24.dp,
        navigationBarPadding.calculateBottomPadding() + 4.dp
    )

    // APP LISTのICON ONLYモード（アイコンのみ表示・正方形スロット）かどうか。APP LISTの設定画面で切り替える。
    // 縦画面・横画面を切り替えても意図せず引き継がれないよう、それぞれ別に記憶する。
    var accessGridIconOnlyPortrait by remember { mutableStateOf(prefs.getBoolean("access_grid_icon_only_portrait", false)) }
    var accessGridIconOnlyLandscape by remember { mutableStateOf(prefs.getBoolean("access_grid_icon_only_landscape", false)) }
    val accessGridIconOnly = if (isPortrait) accessGridIconOnlyPortrait else accessGridIconOnlyLandscape
    fun setAccessGridIconOnly(iconOnly: Boolean) {
        if (isPortrait) {
            accessGridIconOnlyPortrait = iconOnly
            prefs.edit { putBoolean("access_grid_icon_only_portrait", iconOnly) }
        } else {
            accessGridIconOnlyLandscape = iconOnly
            prefs.edit { putBoolean("access_grid_icon_only_landscape", iconOnly) }
        }
    }

    // アプリアイコンをアクセントカラーのデュオトーン加工をせず、本来の色のまま表示するかどうか。
    // カラーパレット下部のチェックボックスで切り替える。
    var useOriginalIconColors by remember { mutableStateOf(prefs.getBoolean("use_original_icon_colors", false)) }
    fun toggleUseOriginalIconColors() {
        useOriginalIconColors = !useOriginalIconColors
        prefs.edit { putBoolean("use_original_icon_colors", useOriginalIconColors) }
    }

    // 各ウィジェットパネルの枠線表示設定（非表示にしているものだけを保持する）
    var hiddenWidgetPanels by remember { mutableStateOf(loadHiddenWidgetPanels(prefs)) }
    fun setAllWidgetBorders(visible: Boolean) {
        hiddenWidgetPanels = if (visible) emptySet() else WidgetPanel.entries.toSet()
        saveHiddenWidgetPanels(prefs, hiddenWidgetPanels)
    }
    fun toggleWidgetPanelBorder(panel: WidgetPanel) {
        hiddenWidgetPanels = if (panel in hiddenWidgetPanels) hiddenWidgetPanels - panel else hiddenWidgetPanels + panel
        saveHiddenWidgetPanels(prefs, hiddenWidgetPanels)
    }

    // アクセントカラー2を使うウィジェットパネル（それ以外はアクセントカラー1を使う）
    var accent2WidgetPanels by remember { mutableStateOf(loadAccent2WidgetPanels(prefs)) }
    fun setWidgetPanelAccent(panel: WidgetPanel, useAccent2: Boolean) {
        accent2WidgetPanels = if (useAccent2) accent2WidgetPanels + panel else accent2WidgetPanels - panel
        saveAccent2WidgetPanels(prefs, accent2WidgetPanels)
    }

    // 縦画面と横画面でAPP LISTに同じ並びを使うかどうか（APP LISTの設定画面で切り替える）。
    // 別々にする場合は、横画面では専用の並び（KEY_GRID_APPS_LANDSCAPE）を使う
    var shareGridAcrossOrientations by remember { mutableStateOf(loadShareGridAcrossOrientations(prefs)) }
    val gridAppsKey = gridAppsKeyFor(shareGridAcrossOrientations, isPortrait)

    // GridApps: SharedPreferencesから保存されたパッケージ名リストを読み込む（今の向きで使う並び）
    var gridPackages by remember(gridAppsKey) {
        mutableStateOf(prefs.getString(gridAppsKey, "")?.split(",") ?: emptyList())
    }

    // DockApps: SharedPreferencesから保存されたパッケージ名リストを読み込む
    // 縦画面と横画面で同じ並びにするかどうか（APP LISTと同様。別々のときは横画面用の並びを使う）
    var shareDockAcrossOrientations by remember { mutableStateOf(loadShareDockAcrossOrientations(prefs)) }
    val dockAppsKey = dockAppsKeyFor(shareDockAcrossOrientations, isPortrait)
    var dockPackages by remember(dockAppsKey) {
        mutableStateOf(prefs.getString(dockAppsKey, "")?.split(",") ?: emptyList())
    }

    // フォルダ: SharedPreferencesから保存されたフォルダ（ID→FolderInfo）を読み込む
    // （ドックはフォルダに対応しないため、グリッドのみで使う）
    var folders by remember { mutableStateOf(loadFolders(prefs)) }

    // QUICK ACCESSのボタン構成: SharedPreferencesから読み込む（アプリグリッドと同様に追加・削除可能）
    // 縦画面と横画面で同じ並びを使うかどうか（QUICK ACCESSの設定画面で切り替える）
    var shareQuickActionsAcrossOrientations by remember { mutableStateOf(loadShareQuickActionsAcrossOrientations(prefs)) }
    val quickActionsKey = quickActionsKeyFor(shareQuickActionsAcrossOrientations, isPortrait)
    var quickActionSlots by remember(quickActionsKey) { mutableStateOf(loadQuickActionSlots(prefs, quickActionsKey)) }
    // 縦画面と横画面でQUICK ACCESSに同じ並びを使うかどうかを切り替える。同じにする場合は、
    // 今表示している向きの並びにそろえる
    fun setShareQuickActionsAcrossOrientations(share: Boolean) {
        if (share == shareQuickActionsAcrossOrientations) return
        saveShareQuickActionsAcrossOrientations(prefs, share, keepLandscape = !isPortrait)
        shareQuickActionsAcrossOrientations = share
        quickActionSlots = loadQuickActionSlots(prefs, quickActionsKeyFor(share, isPortrait))
    }
    // 別のスロットに設定済みのボタンを空きスロットに設定しようとしたときの確認待ち（設定先のスロットとボタン）
    var pendingQuickActionMove by remember { mutableStateOf<Pair<Int, QuickActionId>?>(null) }
    // QUICK ACCESSの[index]番目のスロットに[action]を設定する。他のスロットに同じボタンがあれば、そちらは空にする
    fun assignQuickAction(index: Int, action: QuickActionId) {
        val newSlots = quickActionSlots.map { if (it == action) null else it }.toMutableList()
        while (newSlots.size <= index) newSlots.add(null)
        newSlots[index] = action
        quickActionSlots = newSlots
        saveQuickActionSlots(prefs, quickActionsKey, newSlots)
    }
    var quickActionAddIndex by remember { mutableStateOf<Int?>(null) } // QUICK ACCESSの空きスロットタップ時（ボタン種類選択待ち）

    // パッケージ名からアプリ情報を引くための索引。スロットごとに全アプリを線形探索しないようにする
    val appsByPackage = remember(allApps) { allApps.associateBy { it.packageName } }

    // 通知バッジの更新など、スロット構成と無関係な再コンポジションのたびに作り直さないよう、
    // 構成要素が変わったときだけ組み立て直す
    val gridItems: List<GridItem?> = remember(gridPackages, folders, appsByPackage) {
        gridPackages.map { pkg ->
            when {
                pkg.isEmpty() -> null
                isFolderSlotValue(pkg) -> folderIdFromSlotValue(pkg)?.let { folders[it] }?.let { GridItem.FolderItem(it) }
                else -> appsByPackage[pkg]?.let { GridItem.AppItem(it) }
            }
        }
    }
    val dockApps = remember(dockPackages, appsByPackage) {
        dockPackages.map { pkg -> if (pkg.isEmpty()) null else appsByPackage[pkg] }
    }
    // DOCKの並び（1ページのアイコン数・ページ数）。画面の向きごとに保存する（カスタマイズ画面で変更）
    var dockLayout by remember(isPortrait) { mutableStateOf(loadDockLayout(prefs, isPortrait)) }
    // アプリが入っているページ数。これより少ないページ数にはできない（設定より多ければこちらで表示する）
    val dockMinPageCount = requiredSlotGridPages(dockPackages, dockLayout.slotsPerPage) { it.isEmpty() }
    fun updateDockLayout(layout: DockLayout) {
        dockLayout = layout
        saveDockLayout(prefs, isPortrait, layout)
    }

    var appSelectorTarget by remember { mutableStateOf<String?>(null) } // "grid" または "dock"
    var targetIndex by remember { mutableStateOf<Int?>(null) } // 追加する位置（インデックス）を保持
    var openFolderId by remember { mutableStateOf<String?>(null) } // 中身を表示中のフォルダ
    // ポップアップを閉じるアニメーション中もフォルダの中身を表示し続けるため、openFolderIdが
    // nullになった後も直前に表示していたフォルダの情報を保持しておく
    var displayedFolder by remember { mutableStateOf<FolderInfo?>(null) }
    var showAllAppsDrawer by remember { mutableStateOf(false) } // アプリドロワーの表示状態
    var showCustomizeSheet by remember { mutableStateOf(false) } // カスタマイズ画面の表示状態
    var homeMenuOffset by remember { mutableStateOf<Offset?>(null) } // 何もないところを長押しした位置（メニュー表示中のみ）
    // ウィジェットキャンバスの空き領域に「+ ADD WIDGET」タイルを表示するかどうか（カスタマイズ画面で切り替える）
    var showAddWidgetTile by remember { mutableStateOf(prefs.getBoolean("show_add_widget_tile", true)) }
    // ウィジェットスタックの自動切り替え（オンオフと間隔。カスタマイズ画面で変更する）
    var stackAutoRotate by remember { mutableStateOf(loadStackAutoRotateSettings(prefs)) }
    // ヘッダー・DOCKを表示するかどうか（カスタマイズ画面で切り替える。非表示にするとウィジェットのエリアが広がる）。
    // 画面の向きごとに保存する（向きごとの値が未設定なら、向きで分ける前の共通の設定を引き継ぐ）
    val orientationSuffix = if (isPortrait) "portrait" else "landscape"
    val showHeaderKey = "show_header_$orientationSuffix"
    val showDockKey = "show_dock_$orientationSuffix"
    var showHeader by remember(showHeaderKey) {
        mutableStateOf(prefs.getBoolean(showHeaderKey, prefs.getBoolean("show_header", true)))
    }
    var showDock by remember(showDockKey) {
        mutableStateOf(prefs.getBoolean(showDockKey, prefs.getBoolean("show_dock", true)))
    }
    // ヘッダー中央（横画面・縦画面（大））に表示する3段の文字（カスタマイズ画面で書き換える）
    var headerTitle by remember { mutableStateOf(loadHeaderTitle(prefs)) }
    var showPowerPermissionRationale by remember { mutableStateOf(false) } // アクセシビリティサービスの権限案内
    // 上の権限案内で、何をするためにアクセシビリティサービスが必要なのか（例: 電源メニュー（電源を切る/再起動）を開く）
    var accessibilityRationaleReason by remember { mutableStateOf("電源メニュー（電源を切る/再起動）を開く") }

    // ホーム画面のジェスチャー（上下スワイプ・ダブルタップ）への、アクションの割り当て。
    // 割り当ての変更はPROの機能で、PROでない場合は初期の割り当て（上スワイプで ALL APPS）で動く
    var gestureBindings by remember { mutableStateOf(loadGestureBindings(prefs)) }
    val activeGestureBindings = if (isPro) gestureBindings else DefaultGestureBindings
    val isDoubleTapAssigned = activeGestureBindings[HomeGesture.DOUBLE_TAP]?.action != GestureAction.NONE
    var gestureAppPickerTarget by remember { mutableStateOf<HomeGesture?>(null) } // 起動するアプリを選んでいるジェスチャー
    // ジェスチャーに割り当てたアクションを実行する
    fun runHomeGesture(gesture: HomeGesture) {
        // 割り当ては、実行するこの時点の最新の状態から読む（関数の外で計算した値を使うと、ジェスチャーの
        // 検出処理が持っている古い関数が、変更前の割り当てで動いてしまうため）
        val bindings = if (isPro) gestureBindings else DefaultGestureBindings
        val binding = bindings[gesture] ?: return
        val action = binding.action
        if (action == GestureAction.NONE) return
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        when (action) {
            GestureAction.NONE -> Unit
            GestureAction.ALL_APPS -> showAllAppsDrawer = true
            GestureAction.CUSTOMIZE -> showCustomizeSheet = true
            GestureAction.LAUNCH_APP -> binding.packageName
                ?.let { context.packageManager.getLaunchIntentForPackage(it) }
                ?.let { context.startActivity(it) }
            else -> action.globalAction?.let { globalAction ->
                // 通知パネル・画面オフなどは、アクセシビリティサービスで行う（無効なら有効化を案内する）
                performSystemActionOrRequestPermission(context, globalAction) {
                    accessibilityRationaleReason = "ジェスチャーで「${action.label}」"
                    showPowerPermissionRationale = true
                }
            }
        }
    }
    // ジェスチャーの検出（pointerInput）は一度だけ起動するため、常に最新の割り当てで実行できるようにする
    // （関数の参照はComposeが使い回すことがあるため、毎回新しいラムダで包んで渡す）
    val currentRunHomeGesture = rememberUpdatedState<(HomeGesture) -> Unit> { gesture -> runHomeGesture(gesture) }
    var pendingAppSlotRemoval by remember { mutableStateOf<PendingAppSlotRemoval?>(null) } // APP SLOTの✗ボタン押下時の操作選択待ち

    // APP SLOT（単体ウィジェット）ごとに割り当てられているアプリ。画面モードをまたいで共有する
    var appSlotAssignments by remember { mutableStateOf(loadAppSlotAssignments(prefs)) }
    var appSlotPickerInstanceId by remember { mutableStateOf<Int?>(null) } // アプリ選択ダイアログ表示中のAPP SLOT

    // openFolderIdが指すフォルダの最新情報をdisplayedFolderに反映する。openFolderIdがnullに
    // なった後（閉じるアニメーション中）はこのeffectが再実行されないため、直前の内容がそのまま残る。
    LaunchedEffect(openFolderId, folders) {
        val id = openFolderId
        if (id != null) {
            displayedFolder = folders[id]
        }
    }

    // ウィジェットキャンバス（ホーム画面中央のウィジェットを置く領域）のスクリーン座標での範囲。
    // アプリをスロット以外の場所へドロップしたとき、そこがキャンバスの中かどうかの判定に使う
    var widgetCanvasBoundsOnScreen by remember { mutableStateOf(Rect.Zero) }
    // アプリをキャンバスのスロット以外の場所へドロップしたときの、ウィジェットとして置く待ち
    // （アイコンのみ・アイコン＋名前を選ぶダイアログの表示中のみ）
    var pendingAppSlotDrop by remember { mutableStateOf<PendingAppSlotDrop?>(null) }

    // アプリアイコン・フォルダ・QUICK ACCESSのボタンのドラッグ＆ドロップの結果を反映する
    fun handleAppDrop(payload: AppDragPayload, target: AppDropTarget?, positionOnScreen: Offset? = null) {
        val source = payload.source
        // アプリドロワーから持ってきた場合は、ドロップ先にかかわらずドロワーを閉じる
        // （ドラッグ中は透明にして開いたままにしている）
        if (source == AppDragSource.Drawer) showAllAppsDrawer = false
        // アプリを、ウィジェットキャンバスのスロット以外の場所へドロップした場合は、その場に
        // APP SLOT（ウィジェット）として置く。アイコンのみかアイコン＋名前かを選んでもらってから置く
        if (target == null && positionOnScreen != null && payload.item is AppDragItem.App &&
            widgetCanvasBoundsOnScreen.contains(positionOnScreen)
        ) {
            pendingAppSlotDrop = PendingAppSlotDrop(payload, payload.item.appInfo, positionOnScreen)
            return
        }
        // どこにも重なっていない、またはフォルダのポップアップの余白に落とした場合は何もしない
        if (target == null || target == AppDropTarget.FolderPanel) return

        // QUICK ACCESSのボタンは、QUICK ACCESS内での並べ替え（入れ替え）と削除のみ
        if (payload.item is AppDragItem.QuickAction) {
            val slots = applyQuickActionDrop(quickActionSlots, source, target) ?: return
            quickActionSlots = slots
            saveQuickActionSlots(prefs, quickActionsKey, slots)
            return
        }

        if (target == AppDropTarget.UninstallZone) {
            // スロットはここでは消さない。実際にアンインストールが完了した場合のみ
            // UninstallResultReceiverが取り除く
            (payload.item as? AppDragItem.App)?.let { requestUninstall(context, it.appInfo.packageName) }
            return
        }

        // APP LIST・DOCK・フォルダの間での移動・入れ替え・フォルダの作成など
        val result = applySlotDrop(context, prefs, SlotArrangement(gridPackages, dockPackages, folders), payload, target) ?: return
        val (grid, dock, editedFolders) = result.arrangement
        if (result.closeFolder) openFolderId = null
        editedFolders.values.forEach { folder ->
            if (folders[folder.id] != folder) saveFolder(prefs, folder)
        }
        if (editedFolders != folders) folders = editedFolders
        if (grid != gridPackages) {
            gridPackages = grid
            prefs.edit { putString(gridAppsKey, grid.joinToString(",")) }
        }
        if (dock != dockPackages) {
            dockPackages = dock
            prefs.edit { putString(dockAppsKey, dock.joinToString(",")) }
        }
    }
    // （関数の参照はComposeが使い回すことがあるため、ラムダで包んで渡す）
    val appDragState = rememberAppDragState { payload, target, position -> handleAppDrop(payload, target, position) }

    // アンインストールが実際に完了すると、UninstallResultReceiverがバックグラウンドで
    // SharedPreferencesのAPP LIST・DOCKの並びを直接書き換える。ここではその変更を
    // 検知して、画面上のgridPackages/dockPackagesに反映する。
    DisposableEffect(prefs, gridAppsKey, dockAppsKey) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
                gridAppsKey -> gridPackages = sharedPrefs.getString(gridAppsKey, "")?.split(",") ?: emptyList()
                dockAppsKey -> dockPackages = sharedPrefs.getString(dockAppsKey, "")?.split(",") ?: emptyList()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // 縦画面と横画面でAPP LISTに同じ並びを使うかどうかを切り替える。同じにする場合は、
    // 今表示している向きの並びにそろえる
    fun setShareGridAcrossOrientations(share: Boolean) {
        if (share == shareGridAcrossOrientations) return
        saveShareGridAcrossOrientations(prefs, share, keepLandscape = !isPortrait)
        shareGridAcrossOrientations = share
        folders = loadFolders(prefs)
        val key = gridAppsKeyFor(share, isPortrait)
        gridPackages = prefs.getString(key, "")?.split(",") ?: emptyList()
    }

    // 縦画面と横画面でDOCKに同じ並びを使うかどうかを切り替える。同じにする場合は、
    // 今表示している向きの並びにそろえる
    fun setShareDockAcrossOrientations(share: Boolean) {
        if (share == shareDockAcrossOrientations) return
        saveShareDockAcrossOrientations(prefs, share, keepLandscape = !isPortrait)
        shareDockAcrossOrientations = share
        dockPackages = prefs.getString(dockAppsKeyFor(share, isPortrait), "")?.split(",") ?: emptyList()
    }

    // ウィジェット編集モード（ウィジェットのヘッダーなど、個々のスロット以外の長押しで入る。
    // 移動・リサイズ・削除ハンドル表示用）。アプリ・QUICK ACCESSのボタンなど個々のスロットは、
    // 編集モードではなく長押し→ドラッグで移動・削除する
    var isWidgetEditMode by remember { mutableStateOf(false) }
    fun enterWidgetEditMode() {
        isWidgetEditMode = true
    }

    // ウィジェットを移動ドラッグ中にDock付近へ表示する「ここにドラッグして削除」ゾーン関連の状態。
    // draggingWidgetは現在移動ドラッグ中のウィジェット（非ドラッグ中はnull）で、これに応じて
    // ゾーンの表示・非表示を切り替える。deleteZoneBoundsInRootはそのゾーンのルート座標系での
    // 範囲（当たり判定に使う）。pendingDeleteWidgetはゾーンにドロップされ、削除確認
    // ダイアログを表示中のウィジェット。
    // （WidgetPanelではなくPlacedWidget自体を保持するのは、APPWIDGET（外部ウィジェット）が
    // 同じ種類を複数配置できるため、種類だけでは対象を一意に特定できないため）
    var draggingWidget by remember { mutableStateOf<PlacedWidget?>(null) }
    var isDraggedWidgetOverDeleteZone by remember { mutableStateOf(false) }
    var deleteZoneBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var pendingDeleteWidget by remember { mutableStateOf<PlacedWidget?>(null) }

    // アプリ起動などでランチャーがバックグラウンドに回ったら編集モードを自動解除する
    // （編集モードのままアプリを開いてしまい、戻ってきても編集モードが残る問題への対処）
    // ON_STOPではなくON_RESUMEで解除する: ON_STOPは他のアクティビティに覆われた瞬間
    // （システムのアンインストール確認画面が開いた直後なども含む）に発火してしまい、
    // そのタイミングで大きな再コンポジションが走ると、一部端末でその確認画面自体が
    // 開いた直後に閉じてしまう不具合があったため。
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { isWidgetEditMode = false }
    val lifecycleOwner = LocalLifecycleOwner.current

    // 他アプリのAppWidget（外部ウィジェット）のRemoteViews更新を受け取れるよう、
    // ランチャーが表示されている間だけAppWidgetHostをlisten状態にする。
    // 通知サービスにも表示状態を伝え、再生中メディアの保険のポーリングを表示中だけに限定させる
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    AppWidgetHostManager.host.startListening()
                    CyberNotificationListener.setUiVisible(true)
                }
                Lifecycle.Event.ON_STOP -> {
                    AppWidgetHostManager.host.stopListening()
                    CyberNotificationListener.setUiVisible(false)
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            CyberNotificationListener.setUiVisible(false)
        }
    }

    // オンボーディング完了後も、未設定の権限/設定があればプロセス起動につき1回だけ
    // ボトムシートで知らせる（ホーム画面に戻るたびに毎回出ると煩わしいため、
    // ON_RESUMEではなくプロセス起動時のみをトリガーにする）
    var showMissingPermissionsSheet by rememberSaveable { mutableStateOf(false) }
    // シートから設定画面を開いたかどうか。デフォルトのホームアプリを変えると、システムがホーム画面を
    // 起動し直す（ホームボタンと同じIntentが届く）ため、戻ってきた直後の1回はシートを閉じずに残す
    var missingPermissionsSettingsOpened by rememberSaveable { mutableStateOf(false) }
    var missingPermissionsResumeSignal by remember { mutableIntStateOf(0) }
    // アクセシビリティサービスの状態（カスタマイズ画面のジェスチャーの欄に表示する）。設定画面から戻って
    // きたとき（ON_RESUME）に取り直す。サービスの接続は少し遅れることがあるため、少し待ってからもう一度確かめる
    val initialAccessibilityStatus = remember { accessibilityServiceStatus(context) }
    val accessibilityStatus by produceState(initialAccessibilityStatus, missingPermissionsResumeSignal) {
        value = accessibilityServiceStatus(context)
        delay(1000)
        value = accessibilityServiceStatus(context)
    }
    fun openAccessibilitySettings() {
        context.startActivity(Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
    LaunchedEffect(Unit) {
        if (!MissingPermissionsSheetState.shownThisProcess) {
            MissingPermissionsSheetState.shownThisProcess = true
            if (OnboardingSteps.any { !it.isSatisfied(context) }) {
                showMissingPermissionsSheet = true
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { missingPermissionsResumeSignal++ }
    if (showMissingPermissionsSheet) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            MissingPermissionsSheet(
                resumeSignal = missingPermissionsResumeSignal,
                onOpenSettings = { missingPermissionsSettingsOpened = true },
                onDismiss = {
                    showMissingPermissionsSheet = false
                    missingPermissionsSettingsOpened = false
                }
            )
        }
    }

    // 壁紙透過モード
    var isWallpaperMode by remember { mutableStateOf(prefs.getBoolean("is_wallpaper_mode", false)) }

    // 通知の監視
    val activeNotifications by CyberNotificationListener.activeNotifications.collectAsState()
    // 再生中メディアの監視
    val nowPlaying by CyberNotificationListener.nowPlaying.collectAsState()

    // 画面の幅（dp）を取得
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val screenWidthDp = with(density) { windowInfo.containerSize.width.toDp().value.toInt() }

    // ACCESS GRID/CALENDAR/SYSTEM MONITOR/QUICK ACCESSをウィジェットとして配置するキャンバス。
    // 画面モードごとに別々のグリッド寸法・配置を持つ。
    val widgetLayoutMode = when {
        isPortrait && screenWidthDp < 600 -> WidgetLayoutMode.SMALL_PORTRAIT
        isPortrait -> WidgetLayoutMode.LARGE_PORTRAIT
        else -> WidgetLayoutMode.LANDSCAPE
    }
    // APP LISTのアイコンの並び（nullはAUTO）とページ数。ウィジェットの大きさは画面モードごとに
    // 違うため、画面モードごとに保存する
    var accessGridSize by remember(widgetLayoutMode) { mutableStateOf(loadSlotGridSize(prefs, SlotGridSection.ACCESS_GRID, widgetLayoutMode)) }
    var accessGridPageCount by remember(widgetLayoutMode) { mutableIntStateOf(loadSlotGridPageCount(prefs, SlotGridSection.ACCESS_GRID, widgetLayoutMode)) }
    // APP LISTが実際に測った、AUTOの場合の並びと1ページのスロット数（設定画面の表示に使う）
    var accessGridAutoSize by remember(widgetLayoutMode) { mutableStateOf<SlotGridSize?>(null) }
    var accessGridPageSize by remember(widgetLayoutMode) { mutableIntStateOf(0) }
    var showAppListSettings by remember { mutableStateOf(false) }
    // QUICK ACCESSのスロットの並び（nullはAUTO）とページ数（APP LISTと同じく画面モードごとに保存する）
    var quickAccessGridSize by remember(widgetLayoutMode) { mutableStateOf(loadSlotGridSize(prefs, SlotGridSection.QUICK_ACCESS, widgetLayoutMode)) }
    var quickAccessPageCount by remember(widgetLayoutMode) { mutableIntStateOf(loadSlotGridPageCount(prefs, SlotGridSection.QUICK_ACCESS, widgetLayoutMode)) }
    var quickAccessAutoSize by remember(widgetLayoutMode) { mutableStateOf<SlotGridSize?>(null) }
    var quickAccessPageSize by remember(widgetLayoutMode) { mutableIntStateOf(0) }
    var showQuickAccessSettings by remember { mutableStateOf(false) }
    var showGridLinesSheet by remember { mutableStateOf(false) } // QUICK ACCESSのGRIDボタンで開くグリッド線の設定
    // QUICK ACCESSのボタンの表示スタイル（アイコンのみ・アイコン＋名前・名前のみ）
    var quickButtonStyle by remember { mutableStateOf(loadQuickButtonStyle(prefs)) }

    var placedWidgets by remember(widgetLayoutMode) { mutableStateOf(loadPlacedWidgets(prefs, widgetLayoutMode)) }
    fun updatePlacedWidgets(newWidgets: List<PlacedWidget>) {
        // 削除などでメンバーが1つだけになったスタックは、普通のウィジェットに戻す
        val normalized = newWidgets.normalizeStacks()
        placedWidgets = normalized
        savePlacedWidgets(prefs, widgetLayoutMode, normalized)
    }
    // NOW PLAYINGウィジェットをホーム画面に置いているときは、ヘッダーには再生中メディアを表示しない
    val headerNowPlaying = nowPlaying.takeIf { placedWidgets.none { it.type == WidgetPanel.NOW_PLAYING } }
    var showWidgetTypeSelector by remember { mutableStateOf(false) } // 「+ ADD WIDGET」タップ時（種類選択待ち）
    var showAppWidgetPicker by remember { mutableStateOf(false) } // 「＋ 外部ウィジェットを追加」タップ時（プレビュー付き一覧表示中）

    // WidgetCanvasが測定したセル1つ分の実サイズ（dp）。新規追加する外部ウィジェットを、
    // 種類ごとの固定サイズではなく実際の推奨サイズ（dp）に応じたセル数で配置するために使う
    // （画面モードが変わるとグリッド寸法自体が変わるため、モードごとに保持し直す）
    var canvasCellSize by remember(widgetLayoutMode) { mutableStateOf<DpSize?>(null) }
    // ホーム画面全体（長押しを検出する背景のSurface）と、ウィジェットキャンバスのルート座標系での範囲。
    // 背景の長押しの位置にウィジェットがあるかどうかの判定に使う
    var homeSurfaceBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var widgetCanvasBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    // 背景のSurface内の位置[offset]に、配置済みのウィジェットがあるかどうか
    fun isOverPlacedWidget(offset: Offset): Boolean {
        val cellSize = canvasCellSize ?: return false
        val local = offset + homeSurfaceBoundsInRoot.topLeft - widgetCanvasBoundsInRoot.topLeft
        val col = local.x / with(density) { cellSize.width.toPx() }
        val row = local.y / with(density) { cellSize.height.toPx() }
        return placedWidgets.any { col >= it.col && col < it.col + it.colSpan && row >= it.row && row < it.row + it.rowSpan }
    }
    // 外部ウィジェットが、許容する最小サイズでもこの画面のグリッドに入り切らなかった
    // （または配置しようとした時点で空きがなかった）ことを知らせるエラーダイアログの表示状態
    var appWidgetTooLargeError by remember { mutableStateOf(false) }
    // 長押しメニューの「ウィジェットを追加」が押されたが、ホーム画面に空きがないことを知らせるダイアログの表示状態
    var showNoWidgetSpaceError by remember { mutableStateOf(false) }

    // アプリをスロット以外の場所へドロップしたとき、選んだ形（アイコンのみ・アイコン＋名前）の
    // APP SLOTとして、指を離した位置にいちばん近い空いている場所に置く。APP LIST・DOCK・フォルダから
    // 持ってきたアプリは元の場所から外す（移動）。アプリドロワーから持ってきた場合は追加になる
    fun placeAppSlotAtDrop(drop: PendingAppSlotDrop, type: WidgetPanel) {
        val cellSize = canvasCellSize ?: return
        val cellWidthPx = with(density) { cellSize.width.toPx() }
        val cellHeightPx = with(density) { cellSize.height.toPx() }
        val targetSize = appSlotTargetSize(type)
        val local = drop.positionOnScreen - widgetCanvasBoundsOnScreen.topLeft
        val slot = findFreeGridSlotNear(
            placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows,
            desiredColSpan = targetSize.width / cellSize.width,
            desiredRowSpan = targetSize.height / cellSize.height,
            centerCol = local.x / cellWidthPx,
            centerRow = local.y / cellHeightPx
        ) ?: run {
            showNoWidgetSpaceError = true
            return
        }
        val (col, row, colSpan, rowSpan) = slot
        val instanceId = allocateNextAppSlotInstanceId(prefs)
        val packageName = drop.appInfo.packageName
        saveAppSlotAssignment(prefs, instanceId, packageName)
        appSlotAssignments = appSlotAssignments + (instanceId to packageName)
        updatePlacedWidgets(
            placedWidgets + PlacedWidget(type = type, instanceId = instanceId, col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
        )
        // 元の場所から外す（「削除」エリアに落としたときと同じ処理。アプリドロワーからの場合は外す元がない）
        if (drop.payload.source != AppDragSource.Drawer) handleAppDrop(drop.payload, AppDropTarget.RemoveZone)
    }

    // ホームボタンが押されたら、開いているポップアップ・ボトムシート・ダイアログ・フォルダ・
    // 編集モード・ドラッグなどをすべて閉じて、ホーム画面の状態に戻る
    // （各ウィジェットの中のポップアップは、それぞれがLocalHomePressedSignalを見て閉じる）
    val homePressedSignal = LocalHomePressedSignal.current
    LaunchedEffect(homePressedSignal) {
        if (homePressedSignal == 0) return@LaunchedEffect
        showAllAppsDrawer = false
        showCustomizeSheet = false
        proPromptFeature = null
        gestureAppPickerTarget = null
        showIconPackPicker = false
        showAppListSettings = false
        showQuickAccessSettings = false
        showGridLinesSheet = false
        pendingQuickActionMove = null
        // 未設定項目のシートから設定画面を開いて戻ってきたとき（ホームアプリの変更でホーム画面が
        // 起動し直されたとき）は、シートを閉じずに残す
        if (missingPermissionsSettingsOpened) {
            missingPermissionsSettingsOpened = false
        } else {
            showMissingPermissionsSheet = false
        }
        showPowerPermissionRationale = false
        homeMenuOffset = null
        openFolderId = null
        appSelectorTarget = null
        targetIndex = null
        quickActionAddIndex = null
        appSlotPickerInstanceId = null
        pendingAppSlotRemoval = null
        pendingDeleteWidget = null
        showWidgetTypeSelector = false
        showAppWidgetPicker = false
        appWidgetTooLargeError = false
        showNoWidgetSpaceError = false
        isWidgetEditMode = false
        appDragState.cancel()
    }

    // 外部ウィジェット（他アプリのAppWidget）を、推奨サイズに応じたセル数で空いている場所に置く
    fun placeNewAppWidget(appWidgetId: Int) {
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
        val slot = slotForNewAppWidget(info, canvasCellSize, placedWidgets, widgetLayoutMode)
        if (slot != null) {
            val (col, row, colSpan, rowSpan) = slot
            updatePlacedWidgets(
                placedWidgets + PlacedWidget(
                    type = WidgetPanel.APPWIDGET,
                    appWidgetId = appWidgetId,
                    col = col,
                    row = row,
                    colSpan = colSpan,
                    rowSpan = rowSpan
                )
            )
        } else {
            // 許容する最小サイズでも入り切らない、または空きスペースがなければ確保したIDを破棄し、
            // 追加できなかったことを知らせる
            AppWidgetHostManager.host.deleteAppWidgetId(appWidgetId)
            appWidgetTooLargeError = true
        }
    }

    // 外部ウィジェットを追加するフロー（バインド許可確認→必要なら設定画面→配置）
    val startAppWidgetAddFlow = rememberAppWidgetAddFlow(activity) { appWidgetId -> placeNewAppWidget(appWidgetId) }

    // 設定のバックアップ（書き出し）と復元（読み込み）。ファイルの場所は端末標準のファイル選択画面で選ぶ。
    // 復元は今の設定をすべて置き換えるため、ファイルを選んだあとに確認してから行う
    val backupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val message = LauncherBackup.export(context, uri).fold(
            onSuccess = { "設定を書き出しました" },
            onFailure = { "書き出せませんでした（${it.message}）" }
        )
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestoreUri = uri
    }
    fun restoreBackup(uri: Uri) {
        LauncherBackup.restore(context, uri).fold(
            onSuccess = { result ->
                val note = if (result.removedAppWidgetCount > 0) {
                    "（この端末にない外部ウィジェット ${result.removedAppWidgetCount} 個は外しました）"
                } else ""
                Toast.makeText(context, "設定を読み込みました$note", Toast.LENGTH_LONG).show()
                // すべての画面の状態を、読み込んだ設定から作り直す
                context.findActivity().recreate()
            },
            onFailure = { Toast.makeText(context, "読み込めませんでした（${it.message}）", Toast.LENGTH_LONG).show() }
        )
    }

    // 自作の一覧（AppWidgetPickerDialog）でウィジェットが選択されたとき。許容する最小サイズでも
    // この画面に入り切らないものは、追加の手続きを始める前に知らせる
    fun startBindFlow(info: AppWidgetProviderInfo) {
        if (!appWidgetFitsOnScreen(info, canvasCellSize, widgetLayoutMode)) {
            appWidgetTooLargeError = true
            return
        }
        startAppWidgetAddFlow(info)
    }

    if (showAllAppsDrawer) {
        CompositionLocalProvider(LocalCyberColors provides colors, LocalAppDragState provides appDragState) {
            AllAppsDrawer(
                allApps = allApps,
                useOriginalIconColors = useOriginalIconColors,
                onDismiss = { showAllAppsDrawer = false }
            )
        }
    }

    // バッテリーコア（歯車）のタップで開くカスタマイズ画面
    if (showCustomizeSheet) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            CustomizeSheet(
                isWallpaperMode = isWallpaperMode,
                onWallpaperModeChange = { enabled ->
                    isWallpaperMode = enabled
                    prefs.edit { putBoolean("is_wallpaper_mode", enabled) }
                },
                isDarkTheme = theme.isDarkTheme,
                onDarkThemeChange = { dark -> theme.updateDarkTheme(dark) },
                themePreset = activeThemePreset,
                onThemePresetChange = { preset -> theme.selectThemePreset(preset) },
                fontOption = theme.activeFontOption(isPro),
                onFontOptionChange = { font -> theme.updateFontOption(font) },
                // アイコンパック（PROの機能）
                iconPackLabel = remember(activeIconPackPackage) {
                    activeIconPackPackage?.let { packageName ->
                        runCatching {
                            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(packageName, 0)).toString()
                        }.getOrNull()
                    }
                },
                onOpenIconPackPicker = { if (requirePro("アイコンパック")) showIconPackPicker = true },
                iconPackUsePackColors = theme.iconPackUsePackColors,
                onIconPackUsePackColorsChange = { usePackColors -> theme.updateIconPackUsePackColors(usePackColors) },
                accentColor = theme.accentColor,
                onAccentColorChange = { color -> theme.updateAccentColor(color) },
                accentColor2 = theme.accentColor2,
                onAccentColor2Change = { color -> theme.updateAccentColor2(color) },
                accent2Panels = accent2WidgetPanels,
                onPanelAccentChange = { panel, useAccent2 -> setWidgetPanelAccent(panel, useAccent2) },
                useOriginalIconColors = useOriginalIconColors,
                onUseOriginalIconColorsChange = { enabled ->
                    if (enabled != useOriginalIconColors) toggleUseOriginalIconColors()
                },
                onOpenAppListSettings = {
                    // シートを重ねず、カスタマイズ画面を閉じてからAPP LISTの設定画面を開く
                    showCustomizeSheet = false
                    showAppListSettings = true
                },
                onOpenQuickAccessSettings = {
                    // シートを重ねず、カスタマイズ画面を閉じてからQUICK ACCESSの設定画面を開く
                    showCustomizeSheet = false
                    showQuickAccessSettings = true
                },
                showHeader = showHeader,
                onShowHeaderChange = { visible ->
                    showHeader = visible
                    prefs.edit { putBoolean(showHeaderKey, visible) }
                },
                headerTitle = headerTitle,
                onHeaderTitleChange = { title ->
                    headerTitle = title
                    saveHeaderTitle(prefs, title)
                },
                showDock = showDock,
                onShowDockChange = { visible ->
                    showDock = visible
                    prefs.edit { putBoolean(showDockKey, visible) }
                },
                shareDockAcrossOrientations = shareDockAcrossOrientations,
                onShareDockAcrossOrientationsChange = ::setShareDockAcrossOrientations,
                dockSlotsPerPage = dockLayout.slotsPerPage,
                onDockSlotsPerPageChange = { updateDockLayout(dockLayout.copy(slotsPerPage = it)) },
                dockPageCount = maxOf(dockLayout.pageCount, dockMinPageCount),
                dockMinPageCount = dockMinPageCount,
                onDockPageCountChange = { updateDockLayout(dockLayout.copy(pageCount = it)) },
                showAddWidgetTile = showAddWidgetTile,
                onShowAddWidgetTileChange = { visible ->
                    showAddWidgetTile = visible
                    prefs.edit { putBoolean("show_add_widget_tile", visible) }
                },
                stackAutoRotate = stackAutoRotate,
                onStackAutoRotateChange = { settings ->
                    stackAutoRotate = settings
                    saveStackAutoRotateSettings(prefs, settings)
                },
                hiddenPanels = hiddenWidgetPanels,
                onSetAllBorders = { visible -> setAllWidgetBorders(visible) },
                onTogglePanelBorder = { panel -> toggleWidgetPanelBorder(panel) },
                onOpenPowerMenu = {
                    openPowerMenuOrRequestPermission(context) {
                        accessibilityRationaleReason = "電源メニュー（電源を切る/再起動）を開く"
                        showPowerPermissionRationale = true
                    }
                },
                isPro = isPro,
                // 空文字（PROのカードから）のときは、機能を指定せずにPROの案内を出す
                onRequirePro = { feature -> if (feature.isEmpty()) proPromptFeature = "" else requirePro(feature) },
                // 設定のバックアップ・復元（PROの機能）
                // ホーム画面のジェスチャーの割り当て（PROの機能。PROでない場合は初期の割り当てを表示する）
                gestureBindings = activeGestureBindings,
                onGestureBindingChange = { gesture, binding ->
                    gestureBindings = gestureBindings + (gesture to binding)
                    saveGestureBinding(prefs, gesture, binding)
                    // アクセシビリティサービスが必要な動作を割り当てたのに、まだ使えない状態なら、その場で案内する
                    if (binding.action.globalAction != null && accessibilityServiceStatus(context) != AccessibilityServiceStatus.ENABLED) {
                        accessibilityRationaleReason = "ジェスチャーで「${binding.action.label}」"
                        showPowerPermissionRationale = true
                    }
                },
                onPickGestureApp = { gesture -> gestureAppPickerTarget = gesture },
                accessibilityStatus = accessibilityStatus,
                onOpenAccessibilitySettings = ::openAccessibilitySettings,
                appLabel = { packageName -> appsByPackage[packageName]?.label },
                onExportBackup = {
                    if (requirePro("設定のバックアップ")) backupExportLauncher.launch(LauncherBackup.suggestedFileName())
                },
                onImportBackup = {
                    if (requirePro("設定の復元")) backupImportLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
                onDismiss = { showCustomizeSheet = false }
            )
        }
    }

    if (showPowerPermissionRationale) {
        // 設定ではオンなのに動いていない場合（アプリの更新直後などに起きる）は、オフ→オンでの直し方を案内する
        val isEnabledButNotConnected = remember {
            accessibilityServiceStatus(context) == AccessibilityServiceStatus.NOT_CONNECTED
        }
        CompositionLocalProvider(LocalCyberColors provides colors) {
            PermissionRationaleDialog(
                message = if (isEnabledButNotConnected) {
                    "${accessibilityRationaleReason}には、GridLauncherのアクセシビリティサービスが必要です。" +
                        "設定ではオンになっていますが、動いていません。設定画面で一度オフにしてから、オンにし直してください。"
                } else {
                    "${accessibilityRationaleReason}には、GridLauncherのアクセシビリティサービスを有効にしてください。" +
                        "（すでにオンなのに動かない場合は、一度オフにしてからオンにし直してください）"
                },
                onConfirm = {
                    showPowerPermissionRationale = false
                    openAccessibilitySettings()
                },
                onDismiss = { showPowerPermissionRationale = false }
            )
        }
    }

    // APP SLOTの✗ボタンが押されたときの「スロットから削除」か「アンインストール」かの選択ダイアログ
    pendingAppSlotRemoval?.let { removal ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppActionDialog(
                appName = removal.label,
                onDismiss = { pendingAppSlotRemoval = null },
                onRemoveFromSlot = {
                    // ウィジェット自体（PlacedWidget）は消さず、割り当てだけ外す
                    clearAppSlotAssignment(prefs, removal.instanceId)
                    appSlotAssignments = appSlotAssignments - removal.instanceId
                    pendingAppSlotRemoval = null
                },
                onUninstall = {
                    // スロットはここでは消さない。ユーザーが確認画面で実際にアンインストールを
                    // 完了した場合のみ、割り当てが外れたアプリとして扱われる。
                    requestUninstall(context, removal.packageName)
                    pendingAppSlotRemoval = null
                }
            )
        }
    }

    if ((appSelectorTarget != null) && (targetIndex != null)) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppSelectorDialog(
                allApps = allApps,
                useOriginalIconColors = useOriginalIconColors,
                onDismiss = { 
                    appSelectorTarget = null 
                    targetIndex = null
                },
                onAppSelected = { packageName ->
                    if (appSelectorTarget == "grid") {
                        val newPackages = gridPackages.toMutableList()
                        while (newPackages.size <= targetIndex!!) {
                            newPackages.add("")
                        }
                        newPackages[targetIndex!!] = packageName
                        gridPackages = newPackages
                        prefs.edit { putString(gridAppsKey, newPackages.joinToString(",")) }
                    } else if (appSelectorTarget == "dock") {
                        val newPackages = dockPackages.toMutableList()
                        while (newPackages.size <= targetIndex!!) {
                            newPackages.add("")
                        }
                        newPackages[targetIndex!!] = packageName
                        dockPackages = newPackages
                        prefs.edit { putString(dockAppsKey, newPackages.joinToString(",")) }
                    }
                    appSelectorTarget = null
                    targetIndex = null
                }
            )
        }
    }

    // APP SLOT（単体ウィジェット）の空きスロットタップ時、割り当てるアプリを選ばせる
    appSlotPickerInstanceId?.let { instanceId ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppSelectorDialog(
                allApps = allApps,
                useOriginalIconColors = useOriginalIconColors,
                onDismiss = { appSlotPickerInstanceId = null },
                onAppSelected = { packageName ->
                    saveAppSlotAssignment(prefs, instanceId, packageName)
                    appSlotAssignments = appSlotAssignments + (instanceId to packageName)
                    appSlotPickerInstanceId = null
                }
            )
        }
    }

    // QUICK ACCESSの空きスロットタップ時、追加するボタンの種類を選ばせる
    quickActionAddIndex?.let { index ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            QuickActionSelectorDialog(
                assignedActions = quickActionSlots.filterNotNull().toSet(),
                onDismiss = { quickActionAddIndex = null },
                onSelect = { actionId ->
                    quickActionAddIndex = null
                    // 別のスロットに設定済みのボタンは、移動してよいか確認してから設定する
                    if (actionId in quickActionSlots) {
                        pendingQuickActionMove = index to actionId
                    } else {
                        assignQuickAction(index, actionId)
                    }
                }
            )
        }
    }

    // 別のスロットに設定済みのボタンを選んだときの確認。移動すると元のスロットは空になる
    pendingQuickActionMove?.let { (index, actionId) ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            LauncherAlertDialog(
                title = "設定済みのボタンです",
                message = "「${actionId.label}」は別のスロットに設定されています。このスロットに移動しますか？（元のスロットは空になります）",
                confirmLabel = "移動する",
                onConfirm = {
                    assignQuickAction(index, actionId)
                    pendingQuickActionMove = null
                },
                dismissLabel = "キャンセル",
                onDismiss = { pendingQuickActionMove = null }
            )
        }
    }

    // 「+ ADD WIDGET」タップ時、追加するウィジェットの種類を選ばせる
    if (showWidgetTypeSelector) {
        val placedTypes = placedWidgets.map { it.type }.toSet()
        // APPWIDGET（外部ウィジェット）は複数配置が前提で「未配置」の一覧には馴染まないため、
        // ここには出さず「＋ 外部ウィジェットを追加」という別の導線（onSelectExternal）にする。
        // APP SLOT（単体ウィジェット）の2種類も同様に複数配置が前提だが、こちらは特別な追加
        // 導線を必要としないため、一覧に常に含める（「配置済みなら除外」は単一インスタンス限定の
        // 4種にのみ適用する）
        val availableWidgets = WidgetPanel.entries
            .filter { it != WidgetPanel.APPWIDGET }
            .filterNot { it in SingleInstanceWidgetPanels && it in placedTypes }
        CompositionLocalProvider(LocalCyberColors provides colors) {
            WidgetTypeSelectorDialog(
                availableWidgets = availableWidgets,
                lockedWidgets = if (isPro) emptySet() else ProOnlyWidgetPanels,
                onDismiss = { showWidgetTypeSelector = false },
                onSelect = select@{ type ->
                    if (type in ProOnlyWidgetPanels && !requirePro("${type.label} ウィジェット")) return@select
                    val slot = slotForNewWidget(type, canvasCellSize, placedWidgets, widgetLayoutMode)
                    if (slot != null) {
                        val (col, row, colSpan, rowSpan) = slot
                        val instanceId = if (type.isAppSlot) allocateNextAppSlotInstanceId(prefs) else -1
                        updatePlacedWidgets(
                            placedWidgets + PlacedWidget(
                                type = type,
                                instanceId = instanceId,
                                col = col,
                                row = row,
                                colSpan = colSpan,
                                rowSpan = rowSpan
                            )
                        )
                    }
                    showWidgetTypeSelector = false
                },
                onSelectExternal = external@{
                    // 外部ウィジェットは無料では[FreeAppWidgetLimit]個まで
                    val appWidgetCount = placedWidgets.count { it.type == WidgetPanel.APPWIDGET }
                    if (appWidgetCount >= FreeAppWidgetLimit && !requirePro("外部ウィジェットを${FreeAppWidgetLimit + 1}個以上配置")) {
                        return@external
                    }
                    showWidgetTypeSelector = false
                    showAppWidgetPicker = true
                }
            )
        }
    }

    // APP LISTのヘッダーの歯車ボタンで開く、APP LISTの設定（ICON ONLY・アイコンの並び・ページ数）
    if (showAppListSettings) {
        // アプリ・フォルダが入っているページより少なくはできない
        val minPageCount = requiredSlotGridPages(gridPackages, accessGridPageSize) { it.isEmpty() }
        CompositionLocalProvider(LocalCyberColors provides colors) {
            SlotGridSettingsSheet(
                title = "APP LIST",
                itemName = "アプリ",
                isIconOnly = accessGridIconOnly,
                onIconOnlyChange = { setAccessGridIconOnly(it) },
                shareAcrossOrientations = shareGridAcrossOrientations,
                onShareAcrossOrientationsChange = { setShareGridAcrossOrientations(it) },
                gridSize = accessGridSize,
                autoGridSize = accessGridAutoSize,
                onGridSizeChange = { size ->
                    accessGridSize = size
                    saveSlotGridSize(prefs, SlotGridSection.ACCESS_GRID, widgetLayoutMode, size)
                },
                pageCount = maxOf(accessGridPageCount, minPageCount),
                minPageCount = minPageCount,
                onPageCountChange = { count ->
                    accessGridPageCount = count
                    saveSlotGridPageCount(prefs, SlotGridSection.ACCESS_GRID, widgetLayoutMode, count)
                },
                isPro = isPro,
                onRequirePro = { requirePro(it) },
                onDismiss = { showAppListSettings = false }
            )
        }
    }

    // QUICK ACCESSのヘッダーの歯車ボタンで開く、QUICK ACCESSの設定（縦横で同じ並びにするか・
    // スロットの並び・ページ数）
    if (showQuickAccessSettings) {
        // ボタンが入っているページより少なくはできない
        val minPageCount = requiredSlotGridPages(quickActionSlots, quickAccessPageSize) { it == null }
        CompositionLocalProvider(LocalCyberColors provides colors) {
            SlotGridSettingsSheet(
                title = "QUICK ACCESS",
                itemName = "ボタン",
                buttonStyle = quickButtonStyle,
                onButtonStyleChange = { style ->
                    quickButtonStyle = style
                    saveQuickButtonStyle(prefs, style)
                },
                shareAcrossOrientations = shareQuickActionsAcrossOrientations,
                onShareAcrossOrientationsChange = { setShareQuickActionsAcrossOrientations(it) },
                gridSize = quickAccessGridSize,
                autoGridSize = quickAccessAutoSize,
                onGridSizeChange = { size ->
                    quickAccessGridSize = size
                    saveSlotGridSize(prefs, SlotGridSection.QUICK_ACCESS, widgetLayoutMode, size)
                },
                pageCount = maxOf(quickAccessPageCount, minPageCount),
                minPageCount = minPageCount,
                onPageCountChange = { count ->
                    quickAccessPageCount = count
                    saveSlotGridPageCount(prefs, SlotGridSection.QUICK_ACCESS, widgetLayoutMode, count)
                },
                isPro = isPro,
                onRequirePro = { requirePro(it) },
                onDismiss = { showQuickAccessSettings = false }
            )
        }
    }

    // QUICK ACCESSのGRIDボタンで開く、グリッド線（ウィジェットごとの枠線）の設定
    if (showGridLinesSheet) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            GridLinesSheet(
                hiddenPanels = hiddenWidgetPanels,
                onSetAllBorders = { visible -> setAllWidgetBorders(visible) },
                onTogglePanelBorder = { panel -> toggleWidgetPanelBorder(panel) },
                onDismiss = { showGridLinesSheet = false }
            )
        }
    }

    // 「＋ 外部ウィジェットを追加」で開く、プレビュー画像つきの自作ウィジェット選択一覧。
    // 他のダイアログ同様、CompositionLocalProviderのスコープ外で呼ぶとLocalCyberColorsの
    // デフォルト値（ライトテーマ固定）にフォールバックしてしまうため、明示的にテーマを渡す
    if (showAppWidgetPicker) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppWidgetPickerDialog(
                onDismiss = { showAppWidgetPicker = false },
                onSelect = { info ->
                    showAppWidgetPicker = false
                    startBindFlow(info)
                }
            )
        }
    }

    // 許容する最小サイズでもこの画面のグリッドに入り切らなかった（または空きがなかった）場合の通知
    if (appWidgetTooLargeError) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            LauncherAlertDialog(
                title = "追加できません",
                message = "このウィジェットは、許容する最小サイズでもこの画面には入り切らないため追加できませんでした。",
                confirmLabel = "閉じる",
                onConfirm = { appWidgetTooLargeError = false },
                onDismiss = { appWidgetTooLargeError = false }
            )
        }
    }

    // 長押しメニューからウィジェットを追加しようとしたが、ホーム画面に空きがなかった場合の通知
    if (showNoWidgetSpaceError) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            LauncherAlertDialog(
                title = "空きがありません",
                message = "ホーム画面にウィジェットを置く空きがありません。既存のウィジェットを縮小するか削除してから追加してください。",
                confirmLabel = "閉じる",
                onConfirm = { showNoWidgetSpaceError = false },
                onDismiss = { showNoWidgetSpaceError = false }
            )
        }
    }

    CompositionLocalProvider(LocalCyberColors provides colors, LocalAppDragState provides appDragState) {
        // フォルダを開いたときに、グリッド上のフォルダアイコンそのものがポップアップへ
        // 拡大していくコンテナ変形アニメーション（共有要素）を実現するため、メインの
        // グリッドとフォルダポップアップを同じSharedTransitionLayout内に配置する。
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { homeSurfaceBoundsInRoot = it.boundsInRoot() }
                .pointerInput(Unit) {
                    // 上下のスワイプ（1回のドラッグにつき1回だけ、割り当てたアクションを実行する）
                    var swipeHandled = false
                    detectDragGestures(onDragStart = { swipeHandled = false }) { change, dragAmount ->
                        // Y方向（縦）の移動量が一定以上なら、上スワイプ・下スワイプとみなす
                        when {
                            swipeHandled -> Unit
                            dragAmount.y < -20 -> {
                                swipeHandled = true
                                currentRunHomeGesture.value(HomeGesture.SWIPE_UP)
                                change.consume()
                            }
                            dragAmount.y > 20 -> {
                                swipeHandled = true
                                currentRunHomeGesture.value(HomeGesture.SWIPE_DOWN)
                                change.consume()
                            }
                            else -> isWidgetEditMode = false
                        }
                    }
                }
                // ダブルタップに何か割り当てているときだけ検出する（検出すると、1回のタップの判定が
                // ダブルタップの待ち時間の分だけ遅れるため）
                .pointerInput(isDoubleTapAssigned) {
                    detectTapGestures(
                        // 空白タップで編集モード解除
                        onTap = { isWidgetEditMode = false },
                        onDoubleTap = if (isDoubleTapAssigned) {
                            { currentRunHomeGesture.value(HomeGesture.DOUBLE_TAP) }
                        } else null,
                        // 何もないところの長押しで、ウィジェット追加・カスタマイズのメニューを表示
                        onLongPress = { offset ->
                            // ウィジェットの上の長押しはウィジェット側が編集モードに入るので、ここでは
                            // メニューを出さない（指がほとんど動かないと、ウィジェット側が長押しを
                            // 横取りする前にこちらの長押しの時間切れが来てしまうため、位置で判定する）
                            if (!isOverPlacedWidget(offset)) {
                                isWidgetEditMode = false
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                homeMenuOffset = offset
                            }
                        }
                    )
                },
            color = if (isWallpaperMode) Color.Transparent else LocalCyberColors.current.bg
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = if (isPortrait) 32.dp else 40.dp,
                        // ナビゲーションバー（3ボタンナビ・ジェスチャーナビの横棒。横画面では左右に出ることも
                        // ある）と重ならないよう、実際に占有している分だけ余白を足す
                        start = (if (isPortrait) 16.dp else 32.dp) + navigationBarPadding.calculateStartPadding(layoutDirection),
                        end = (if (isPortrait) 16.dp else 32.dp) + navigationBarPadding.calculateEndPadding(layoutDirection),
                        bottom = bottomScreenPadding
                    )
            ) {
                // ヘッダー（カスタマイズ画面で非表示にでき、その分ウィジェットのエリアが広がる）
                if (showHeader) {
                    LauncherHeader(
                        mode = widgetLayoutMode,
                        nowPlaying = headerNowPlaying,
                        title = if (isPro) headerTitle else HeaderTitle.Default,
                        onCoreClick = { showCustomizeSheet = true }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    HeaderDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // APP LIST内部のアプリ一覧の基準列数・行数（画面モードごと）
                val (accessGridMColumns, accessGridMRows) = accessGridBaseSize(widgetLayoutMode)

                // ACCESS GRID/CALENDAR/SYSTEM MONITOR/QUICK ACCESSを、追加・削除・リサイズ・
                // 移動できるウィジェットとして配置するキャンバス。
                WidgetCanvas(
                    columns = widgetLayoutMode.columns,
                    rows = widgetLayoutMode.rows,
                    placedWidgets = placedWidgets,
                    isWidgetEditMode = isWidgetEditMode,
                    resizeConstraints = { widget -> appWidgetResizeConstraints(context, widget) },
                    hideTopRightCorner = { widget -> widget.type.isAppSlot },
                    deleteZoneBoundsInRoot = deleteZoneBoundsInRoot,
                    onCellSizeMeasured = { w, h -> canvasCellSize = DpSize(w, h) },
                    onLayoutChange = { updatePlacedWidgets(it) },
                    showAddWidgetTile = showAddWidgetTile,
                    onRequestAddWidget = { showWidgetTypeSelector = true },
                    onWidgetLongClick = { enterWidgetEditMode() },
                    onExitWidgetEditMode = { isWidgetEditMode = false },
                    onWidgetDragStateChanged = { widget, dragging, overDeleteZone ->
                        draggingWidget = if (dragging) widget else null
                        isDraggedWidgetOverDeleteZone = overDeleteZone
                    },
                    onRequestDeleteConfirm = { widget -> pendingDeleteWidget = widget },
                    stackAutoRotateIntervalMillis = stackAutoRotate.takeIf { it.enabled && isPro }?.let { it.intervalSeconds * 1000L },
                    canStack = isPro,
                    onStackLocked = { requirePro("ウィジェットのスタック") },
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned {
                            widgetCanvasBoundsInRoot = it.boundsInRoot()
                            widgetCanvasBoundsOnScreen = it.boundsOnScreen()
                        }
                ) { type, appWidgetId, instanceId, _, _, _, boxModifier, isResizing ->
                    // アクセントカラー2に設定されたウィジェットだけ、配色のaccentを差し替えて描画する
                    CompositionLocalProvider(LocalCyberColors provides if (isPro && type in accent2WidgetPanels) colors2 else colors) {
                    when (type) {
                        WidgetPanel.ACCESS_GRID -> {
                            AccessGridSection(
                                items = gridItems,
                                baseColumns = accessGridMColumns,
                                baseRows = accessGridMRows,
                                isWallpaperMode = isWallpaperMode,
                                activeNotifications = activeNotifications,
                                openFolderId = openFolderId,
                                showBorder = WidgetPanel.ACCESS_GRID !in hiddenWidgetPanels,
                                isIconOnly = accessGridIconOnly,
                                gridSize = accessGridSize,
                                pageCount = accessGridPageCount,
                                onSettingsClick = { showAppListSettings = true },
                                onLayoutMeasured = { autoColumns, autoRows, pageSize ->
                                    accessGridAutoSize = SlotGridSize(autoColumns, autoRows)
                                    accessGridPageSize = pageSize
                                },
                                useOriginalIconColors = useOriginalIconColors,
                                onAddClick = { index ->
                                    // 空きスロットはそのままアプリ選択を開く（フォルダはアプリ同士を重ねて作る）
                                    appSelectorTarget = "grid"
                                    targetIndex = index
                                },
                                onFolderClick = { folderItem -> openFolderId = folderItem.folder.id }
                            )
                        }
                        WidgetPanel.CALENDAR -> CalendarSection(
                            modifier = boxModifier,
                            showBorder = WidgetPanel.CALENDAR !in hiddenWidgetPanels
                        )
                        WidgetPanel.DEVICE_STATUS -> DeviceStatusSection(
                            modifier = boxModifier,
                            showBorder = WidgetPanel.DEVICE_STATUS !in hiddenWidgetPanels
                        )
                        WidgetPanel.CLOCK -> ClockWidgetSection(
                            modifier = boxModifier,
                            showBorder = WidgetPanel.CLOCK !in hiddenWidgetPanels
                        )
                        WidgetPanel.BATTERY -> BatteryWidgetSection(
                            modifier = boxModifier,
                            showBorder = WidgetPanel.BATTERY !in hiddenWidgetPanels,
                            onCoreClick = { showCustomizeSheet = true }
                        )
                        WidgetPanel.NOW_PLAYING -> NowPlayingSection(
                            info = nowPlaying,
                            modifier = boxModifier,
                            showBorder = WidgetPanel.NOW_PLAYING !in hiddenWidgetPanels
                        )
                        WidgetPanel.QUICK_ACCESS -> QuickAccessSection(
                            modifier = boxModifier,
                            slots = quickActionSlots,
                            isWallpaperMode = isWallpaperMode,
                            gridSize = quickAccessGridSize,
                            pageCount = quickAccessPageCount,
                            accentColor = theme.accentColor,
                            useOriginalIconColors = useOriginalIconColors,
                            showBorder = WidgetPanel.QUICK_ACCESS !in hiddenWidgetPanels,
                            buttonStyle = quickButtonStyle,
                            onWallpaperModeToggle = {
                                isWallpaperMode = !isWallpaperMode
                                prefs.edit { putBoolean("is_wallpaper_mode", isWallpaperMode) }
                            },
                            // グリッド線（ウィジェットごとの枠線）の設定画面を開く
                            onGridLinesClick = { showGridLinesSheet = true },
                            onCustomizeClick = { showCustomizeSheet = true },
                            onSettingsClick = { showQuickAccessSettings = true },
                            onLayoutMeasured = { autoColumns, autoRows, pageSize ->
                                quickAccessAutoSize = SlotGridSize(autoColumns, autoRows)
                                quickAccessPageSize = pageSize
                            },
                            onThemeToggle = {
                                // プリセットのテーマを使っている場合は、従来の配色に戻したうえでライト/ダークを切り替える
                                theme.updateDarkTheme(if (activeThemePreset == ThemePreset.STANDARD) !theme.isDarkTheme else false)
                            },
                            onAccentColorChange = { color -> theme.updateAccentColor(color) },
                            onUseOriginalIconColorsChange = { toggleUseOriginalIconColors() },
                            onAddClick = { index -> quickActionAddIndex = index }
                        )
                        WidgetPanel.APPWIDGET -> AppWidgetHostSection(
                            appWidgetId = appWidgetId,
                            modifier = boxModifier,
                            showBorder = WidgetPanel.APPWIDGET !in hiddenWidgetPanels,
                            isResizing = isResizing
                        )
                        WidgetPanel.APP_SLOT_ICON_ONLY, WidgetPanel.APP_SLOT_NAMED -> StandaloneAppSlotSection(
                            packageName = appSlotAssignments[instanceId] ?: "",
                            allApps = allApps,
                            isIconOnly = type == WidgetPanel.APP_SLOT_ICON_ONLY,
                            isWidgetEditMode = isWidgetEditMode,
                            isWallpaperMode = isWallpaperMode,
                            activeNotifications = activeNotifications,
                            useOriginalIconColors = useOriginalIconColors,
                            modifier = boxModifier,
                            onAssignClick = { appSlotPickerInstanceId = instanceId },
                            onLongClick = { enterWidgetEditMode() },
                            onRemoveClick = {
                                val packageName = appSlotAssignments[instanceId]
                                if (packageName != null) {
                                    val label = appsByPackage[packageName]?.label ?: packageName
                                    pendingAppSlotRemoval = PendingAppSlotRemoval(instanceId, packageName, label)
                                }
                            },
                            onExitWidgetEditMode = { isWidgetEditMode = false }
                        )
                    }
                    }
                }

                // 下段: よく使うアプリ（ドック）。カスタマイズ画面で非表示にでき、その分ウィジェットの
                // エリアが広がる
                if (showDock) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HeaderDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    BottomDockSection(
                        apps = dockApps,
                        slotsPerPage = dockLayout.slotsPerPage,
                        pageCount = maxOf(dockLayout.pageCount, dockMinPageCount),
                        isWallpaperMode = isWallpaperMode,
                        activeNotifications = activeNotifications,
                        useOriginalIconColors = useOriginalIconColors,
                        onAddClick = { index ->
                            appSelectorTarget = "dock"
                            targetIndex = index
                        }
                    )
                }
            }

            // 長押しメニュー。長押し位置はこのSurface内の座標なので、Surfaceの直下に置く
            homeMenuOffset?.let { offset ->
                HomeLongPressMenu(
                    pressOffset = offset,
                    onAddWidget = {
                        homeMenuOffset = null
                        // 1マス分の空きもなければ、種類を選ばせても配置できないため先に知らせる
                        // （「+ ADD WIDGET」タイルの表示条件と同じ判定）
                        if (findFreeGridSlot(placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows) == null) {
                            showNoWidgetSpaceError = true
                        } else {
                            showWidgetTypeSelector = true
                        }
                    },
                    onOpenCustomize = {
                        homeMenuOffset = null
                        showCustomizeSheet = true
                    },
                    onDismiss = { homeMenuOffset = null }
                )
            }
        }

        // フォルダの中身を表示・編集するポップアップ。メイングリッドと同じ
        // SharedTransitionLayout内に配置し、フォルダアイコンからの拡大アニメーションを実現する。
        AnimatedVisibility(
            visible = openFolderId != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            displayedFolder?.let { folder ->
                FolderContentsDialog(
                    folder = folder,
                    allApps = allApps,
                    isWallpaperMode = isWallpaperMode,
                    useOriginalIconColors = useOriginalIconColors,
                    activeNotifications = activeNotifications,
                    animatedVisibilityScope = this,
                    onDismiss = { openFolderId = null },
                    onRename = { newName ->
                        val updated = folder.copy(name = newName)
                        folders = folders + (updated.id to updated)
                        saveFolder(prefs, updated)
                    },
                    onAddApp = { index, packageName ->
                        val newPackages = folder.packageNames.toMutableList()
                        while (newPackages.size <= index) {
                            newPackages.add("")
                        }
                        newPackages[index] = packageName
                        val updated = folder.copy(packageNames = newPackages)
                        folders = folders + (updated.id to updated)
                        saveFolder(prefs, updated)
                    },
                    onLaunchApp = { packageName ->
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                        launchIntent?.let { context.startActivity(it) }
                    }
                )
            }
        }

        // ウィジェットの移動ドラッグ中にDock付近へ浮かせる「ここにドラッグして削除」ゾーン。
        // Dockより後ろ（Column外）に配置しているため、Dockの上にも重なって表示される。
        Box(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = draggingWidget != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomScreenPadding + if (isPortrait) 42.dp else 6.dp)
                    .onGloballyPositioned { coordinates -> deleteZoneBoundsInRoot = coordinates.boundsInRoot() }
            ) {
                DeleteWidgetDropZone(isActive = isDraggedWidgetOverDeleteZone)
            }
        }

        // アプリアイコン・フォルダのドラッグ中に重ねて表示する、指についてくるアイコンと
        // 画面上部の「削除」「アンインストール」エリア
        AppDragOverlay(useOriginalIconColors = useOriginalIconColors)
        }
    }

    // 「ここにドラッグして削除」ゾーンにドロップされたウィジェットの削除確認。
    // 上のCompositionLocalProviderのスコープ外にあるため、テーマ（colors）を
    // 明示的に渡し直さないとLocalCyberColorsのデフォルト値（ライトテーマ固定）に
    // フォールバックしてしまい、実際のテーマ設定に関わらず常に同じ配色になってしまう
    // アプリをスロット以外の場所へドロップしたときの、APP SLOT（ウィジェット）として置く形の選択
    pendingAppSlotDrop?.let { drop ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppSlotStyleDialog(
                app = drop.appInfo,
                useOriginalIconColors = useOriginalIconColors,
                onSelect = { type ->
                    pendingAppSlotDrop = null
                    placeAppSlotAtDrop(drop, type)
                },
                onDismiss = { pendingAppSlotDrop = null }
            )
        }
    }

    // 使うアイコンパックの選択
    if (showIconPackPicker) {
        val iconPacks = remember { IconPackManager.installedIconPacks(context) }
        CompositionLocalProvider(LocalCyberColors provides colors) {
            IconPackPickerDialog(
                iconPacks = iconPacks,
                selectedPackage = theme.iconPackPackage,
                onSelect = { packageName ->
                    theme.updateIconPack(packageName)
                    showIconPackPicker = false
                },
                onDismiss = { showIconPackPicker = false }
            )
        }
    }

    // ジェスチャーに「アプリを起動」を割り当てるときの、起動するアプリの選択
    gestureAppPickerTarget?.let { gesture ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppSelectorDialog(
                allApps = allApps,
                useOriginalIconColors = useOriginalIconColors,
                onDismiss = { gestureAppPickerTarget = null },
                onAppSelected = { packageName ->
                    val binding = GestureBinding(GestureAction.LAUNCH_APP, packageName)
                    gestureBindings = gestureBindings + (gesture to binding)
                    saveGestureBinding(prefs, gesture, binding)
                    gestureAppPickerTarget = null
                }
            )
        }
    }

    // 復元の確認（今の設定がすべて置き換わるため）
    pendingRestoreUri?.let { uri ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            LauncherAlertDialog(
                title = "設定を読み込みますか？",
                message = "今の配置・配色・各種設定は、選んだファイルの内容にすべて置き換わります。" +
                    "外部ウィジェットは、この端末にないものは読み込まれません。",
                confirmLabel = "読み込む",
                onConfirm = {
                    pendingRestoreUri = null
                    restoreBackup(uri)
                },
                dismissLabel = "キャンセル",
                onDismiss = { pendingRestoreUri = null }
            )
        }
    }

    // PROの機能を使おうとしたとき（またはカスタマイズ画面のPROの項目から）の、PRO解放の案内と購入画面。
    // 機能の名前が空の場合は、機能を指定せずにPROの案内だけを出す
    proPromptFeature?.let { feature ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            ProUpgradeDialog(featureName = feature.ifEmpty { null }, onDismiss = { proPromptFeature = null })
        }
    }

    pendingDeleteWidget?.let { widget ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            val widgetLabel = when (widget.type) {
                WidgetPanel.APPWIDGET -> AppWidgetManager.getInstance(context).getAppWidgetInfo(widget.appWidgetId)
                    ?.loadLabel(context.packageManager)
                    ?: widget.type.label
                WidgetPanel.APP_SLOT_ICON_ONLY, WidgetPanel.APP_SLOT_NAMED -> {
                    val assignedLabel = appSlotAssignments[widget.instanceId]
                        ?.let { pkg -> appsByPackage[pkg]?.label }
                    if (assignedLabel != null) "${widget.type.label}（$assignedLabel）" else widget.type.label
                }
                else -> widget.type.label
            }
            WidgetDeleteConfirmDialog(
                widgetLabel = widgetLabel,
                onConfirm = {
                    if (widget.type == WidgetPanel.APPWIDGET) {
                        AppWidgetHostManager.host.deleteAppWidgetId(widget.appWidgetId)
                    }
                    if (widget.type.isAppSlot) {
                        clearAppSlotAssignment(prefs, widget.instanceId)
                        appSlotAssignments = appSlotAssignments - widget.instanceId
                    }
                    updatePlacedWidgets(placedWidgets.filter { it.instanceKey != widget.instanceKey })
                    pendingDeleteWidget = null
                },
                onDismiss = { pendingDeleteWidget = null }
            )
        }
    }
}
