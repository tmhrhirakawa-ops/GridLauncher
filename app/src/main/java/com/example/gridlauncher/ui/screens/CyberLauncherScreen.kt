package com.example.gridlauncher.ui.screens

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gridlauncher.model.FolderInfo
import com.example.gridlauncher.model.GridItem
import com.example.gridlauncher.model.PlacedWidget
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.components.AddSlotChoiceDialog
import com.example.gridlauncher.ui.components.AppActionDialog
import com.example.gridlauncher.ui.components.DefaultAccentColor2
import com.example.gridlauncher.ui.components.HomeLongPressMenu
import com.example.gridlauncher.ui.components.AppWidgetHostSection
import com.example.gridlauncher.ui.components.MissingPermissionsSheet
import com.example.gridlauncher.ui.components.PermissionRationaleDialog
import com.example.gridlauncher.ui.components.QuickActionSelectorDialog
import com.example.gridlauncher.ui.components.StandaloneAppSlotSection
import com.example.gridlauncher.ui.components.WidgetDeleteConfirmDialog
import com.example.gridlauncher.ui.components.WidgetTypeSelectorDialog
import com.example.gridlauncher.ui.sections.*
import com.example.gridlauncher.ui.theme.*
import com.example.gridlauncher.util.AppWidgetConfigureResultBridge
import com.example.gridlauncher.util.AppWidgetHostManager
import com.example.gridlauncher.util.allocateNextAppSlotInstanceId
import com.example.gridlauncher.util.clearAppSlotAssignment
import com.example.gridlauncher.util.createFolder
import com.example.gridlauncher.util.deleteFolder
import com.example.gridlauncher.util.findFreeGridSlot
import com.example.gridlauncher.util.findFreeGridSlotForSize
import com.example.gridlauncher.util.folderIdFromSlotValue
import com.example.gridlauncher.util.folderSlotValue
import com.example.gridlauncher.util.getInstalledApps
import com.example.gridlauncher.util.isFolderSlotValue
import com.example.gridlauncher.util.loadAppSlotAssignments
import com.example.gridlauncher.util.loadAccent2WidgetPanels
import com.example.gridlauncher.util.loadFolders
import com.example.gridlauncher.util.loadHiddenWidgetPanels
import com.example.gridlauncher.util.loadPlacedWidgets
import com.example.gridlauncher.util.loadQuickActionSlots
import com.example.gridlauncher.util.OnboardingSteps
import com.example.gridlauncher.util.openPowerMenuOrRequestPermission
import com.example.gridlauncher.util.resolveInstalledApp
import com.example.gridlauncher.util.saveAccent2WidgetPanels
import com.example.gridlauncher.util.requestUninstall
import com.example.gridlauncher.util.saveAppSlotAssignment
import com.example.gridlauncher.util.saveFolder
import com.example.gridlauncher.util.saveHiddenWidgetPanels
import com.example.gridlauncher.util.savePlacedWidgets
import com.example.gridlauncher.util.saveQuickActionSlots
import com.example.gridlauncher.util.CyberNotificationListener
import com.example.gridlauncher.util.WidgetLayoutMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 他アプリのAppWidgetが申告する最小サイズ（[AppWidgetProviderInfo.minResizeWidth]等）を
 * 何倍まで許容するか。1.0だと申告値を厳密に守るが、大きめの最小値を申告しているウィジェットが
 * 他のランチャーに比べてかなり大きく見えてしまうため、画質が粗くなるリスクと引き換えに
 * 半分まではリサイズできるようにする。
 */
private const val MinSizeRelaxFactor = 0.5f

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
 * ウィジェットが実際に許容する最小サイズ（[MinSizeRelaxFactor]適用後、dp単位）を求める。
 * [com.example.gridlauncher.ui.screens.appWidgetResizeConstraints]（リサイズの下限）と
 * 新規追加時の「画面に入り切るか」判定の両方で同じ基準を使うための共通関数。
 */
/** ウィジェット種類のうち、常に1個までしか同時配置できないもの（それ以外は複数配置できる）。 */
private val SingleInstanceWidgetPanels = setOf(
    WidgetPanel.ACCESS_GRID, WidgetPanel.CALENDAR, WidgetPanel.DEVICE_STATUS, WidgetPanel.QUICK_ACCESS
)

/** APP SLOT（単体ウィジェット）を新規追加するときの、見た目として妥当な初期サイズ（dp）。 */
private val AppSlotIconOnlyTargetSize = DpSize(60.dp, 60.dp)
private val AppSlotNamedTargetSize = DpSize(140.dp, 64.dp)

/**
 * 未設定の権限/設定を知らせるボトムシートを、アプリプロセスの起動につき1回だけ表示する
 * ためのフラグ。ホーム画面に戻るたびに毎回表示されると煩わしいため、画面回転等での
 * 再コンポジションをまたいでプロセスが生きている間は表示済みとして扱う。
 */
private object MissingPermissionsSheetState {
    var shownThisProcess = false
}

private fun relaxedMinSizeDp(info: AppWidgetProviderInfo): DpSize {
    val declaredMinWidth = if (info.minResizeWidth > 0) info.minResizeWidth else info.minWidth
    val declaredMinHeight = if (info.minResizeHeight > 0) info.minResizeHeight else info.minHeight
    return DpSize((declaredMinWidth * MinSizeRelaxFactor).dp, (declaredMinHeight * MinSizeRelaxFactor).dp)
}

/**
 * 編集モードで削除操作が要求されたスロットの情報。
 * 「スロットから削除」か「アンインストール」かをダイアログで選ばせるために保持する。
 */
private data class PendingRemoval(
    val target: String, // "grid"・"dock"・"app_slot"
    // "grid"/"dock"の場合は配列インデックス、"app_slot"の場合はAPP SLOTのinstanceId
    val index: Int,
    val packageName: String,
    val label: String
)

/**
 * ヘッダー下部に引く区切り線。全モード（横画面・縦画面（小）・縦画面（大））共通で使う。
 */
@Composable
private fun HeaderDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalCyberColors.current.border)
    )
}

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
    var allApps by remember { mutableStateOf(getInstalledApps(context.packageManager)) }
    AppWidgetHostManager.ensureInitialized(context)

    // アプリのインストール・アンインストール・更新を検知して、SELECT APPやアプリドロワーの
    // 一覧をその場で更新する。
    DisposableEffect(context) {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                // 変更があったのは1パッケージだけなので、インストール済み全アプリを再取得・
                // 再加工するのではなく、その1件だけを差し替える（他アプリのアイコン処理を
                // 無駄に繰り返さないため）
                val packageName = intent.data?.schemeSpecificPart ?: return
                when (intent.action) {
                    Intent.ACTION_PACKAGE_REMOVED -> {
                        // アップデートに伴う一時的なREMOVEDは無視する（続けてADDEDが届く）
                        if (!intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                            allApps = allApps.filterNot { it.packageName == packageName }
                        }
                    }
                    Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> {
                        val updated = resolveInstalledApp(context.packageManager, packageName)
                        if (updated != null) {
                            allApps = (allApps.filterNot { it.packageName == packageName } + updated).sortedBy { it.label }
                        }
                    }
                }
            }
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // テーマ判定（SharedPreferencesから取得、なければシステム設定）
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(prefs.getBoolean("is_dark_theme", systemDark)) }
    // メインテーマのアクセントカラー（デフォルトは従来通りのオレンジ）。カラーパレットで変更可能。
    var accentColor by remember { mutableStateOf(Color(prefs.getInt("accent_color", LightAccentColor.toArgb()))) }
    val colors = (if (isDarkTheme) {
        CyberColors(DarkBgColor, DarkPanelColor, DarkAccentColor, DarkTextColor, DarkBorderColor, DarkCoreColor)
    } else {
        CyberColors(LightBgColor, LightPanelColor, LightAccentColor, LightTextColor, LightBorderColor, LightCoreColor)
    }).copy(accent = accentColor)
    // アクセントカラー2。カスタマイズ画面でウィジェットごとに1と2のどちらを使うか選べる
    var accentColor2 by remember { mutableStateOf(Color(prefs.getInt("accent_color_2", DefaultAccentColor2.toArgb()))) }
    val colors2 = colors.copy(accent = accentColor2)

    // 初回起動時のオンボーディング（デフォルトのホームアプリ設定・通知アクセス・バッテリー
    // 最適化除外・使用状況アクセスへの案内）。完了するまでは、それ以降のメインUI用の状態
    // （アプリ一覧の読み込み等）を準備する必要がないため、ここで早期リターンする
    var showOnboarding by remember { mutableStateOf(!prefs.getBoolean("onboarding_completed", false)) }
    if (showOnboarding) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
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

    // APP LISTのICON ONLYモード（アイコンのみ表示・正方形スロット）かどうか。ヘッダーのボタンで切り替える。
    // 縦画面・横画面を切り替えても意図せず引き継がれないよう、それぞれ別に記憶する。
    var accessGridIconOnlyPortrait by remember { mutableStateOf(prefs.getBoolean("access_grid_icon_only_portrait", false)) }
    var accessGridIconOnlyLandscape by remember { mutableStateOf(prefs.getBoolean("access_grid_icon_only_landscape", false)) }
    val accessGridIconOnly = if (isPortrait) accessGridIconOnlyPortrait else accessGridIconOnlyLandscape
    fun toggleAccessGridIconOnly() {
        if (isPortrait) {
            accessGridIconOnlyPortrait = !accessGridIconOnlyPortrait
            prefs.edit { putBoolean("access_grid_icon_only_portrait", accessGridIconOnlyPortrait) }
        } else {
            accessGridIconOnlyLandscape = !accessGridIconOnlyLandscape
            prefs.edit { putBoolean("access_grid_icon_only_landscape", accessGridIconOnlyLandscape) }
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

    // GridApps: SharedPreferencesから保存されたパッケージ名リストを読み込む
    var gridPackages by remember {
        mutableStateOf(prefs.getString("grid_apps", "")?.split(",") ?: emptyList())
    }

    // DockApps: SharedPreferencesから保存されたパッケージ名リストを読み込む
    var dockPackages by remember {
        mutableStateOf(prefs.getString("dock_apps", "")?.split(",") ?: emptyList())
    }

    // フォルダ: SharedPreferencesから保存されたフォルダ（ID→FolderInfo）を読み込む
    // （ドックはフォルダに対応しないため、グリッドのみで使う）
    var folders by remember { mutableStateOf(loadFolders(prefs)) }

    // QUICK ACCESSのボタン構成: SharedPreferencesから読み込む（アプリグリッドと同様に追加・削除可能）
    var quickActionSlots by remember { mutableStateOf(loadQuickActionSlots(prefs)) }
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

    var appSelectorTarget by remember { mutableStateOf<String?>(null) } // "grid" または "dock"
    var targetIndex by remember { mutableStateOf<Int?>(null) } // 追加する位置（インデックス）を保持
    var addSlotChoiceIndex by remember { mutableStateOf<Int?>(null) } // グリッドの空きスロットタップ時（アプリ/フォルダ選択待ち）
    var openFolderId by remember { mutableStateOf<String?>(null) } // 中身を表示中のフォルダ
    // ポップアップを閉じるアニメーション中もフォルダの中身を表示し続けるため、openFolderIdが
    // nullになった後も直前に表示していたフォルダの情報を保持しておく
    var displayedFolder by remember { mutableStateOf<FolderInfo?>(null) }
    var showAllAppsDrawer by remember { mutableStateOf(false) } // アプリドロワーの表示状態
    var showCustomizeSheet by remember { mutableStateOf(false) } // カスタマイズ画面の表示状態
    var homeMenuOffset by remember { mutableStateOf<Offset?>(null) } // 何もないところを長押しした位置（メニュー表示中のみ）
    // ウィジェットキャンバスの空き領域に「+ ADD WIDGET」タイルを表示するかどうか（カスタマイズ画面で切り替える）
    var showAddWidgetTile by remember { mutableStateOf(prefs.getBoolean("show_add_widget_tile", true)) }
    var showPowerPermissionRationale by remember { mutableStateOf(false) } // 電源メニュー用の権限案内
    var pendingRemoval by remember { mutableStateOf<PendingRemoval?>(null) } // ✗ボタン押下時の操作選択待ち

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

    // グリッドのスロット（アプリ or フォルダ）を削除する。フォルダはアンインストールの概念が
    // ないため、確認ダイアログなしでスロットとフォルダ自体を即座に削除する。
    fun removeGridItem(index: Int) {
        when (val item = gridItems.getOrNull(index)) {
            is GridItem.FolderItem -> {
                deleteFolder(prefs, item.folder.id)
                folders = folders - item.folder.id
                val newPackages = gridPackages.toMutableList()
                if (index < newPackages.size) {
                    newPackages[index] = ""
                    gridPackages = newPackages
                    prefs.edit { putString("grid_apps", newPackages.joinToString(",")) }
                }
            }
            is GridItem.AppItem -> {
                pendingRemoval = PendingRemoval("grid", index, item.appInfo.packageName, item.appInfo.label)
            }
            null -> Unit
        }
    }

    // QUICK ACCESSのスロットを空にする
    fun removeQuickAction(index: Int) {
        val newSlots = quickActionSlots.toMutableList()
        if (index < newSlots.size) {
            newSlots[index] = null
            quickActionSlots = newSlots
            saveQuickActionSlots(prefs, newSlots)
        }
    }

    // 指定したスロットを空にする（スロットからの削除。アプリ自体はアンインストールしない）
    fun clearSlot(removal: PendingRemoval) {
        when (removal.target) {
            "grid" -> {
                val newPackages = gridPackages.toMutableList()
                if (removal.index < newPackages.size) {
                    newPackages[removal.index] = ""
                    gridPackages = newPackages
                    prefs.edit { putString("grid_apps", newPackages.joinToString(",")) }
                }
            }
            "app_slot" -> {
                // "app_slot"の場合はindexを配列インデックスではなくinstanceIdとして使う。
                // ウィジェット自体（PlacedWidget）は消さず、割り当てだけ外す
                clearAppSlotAssignment(prefs, removal.index)
                appSlotAssignments = appSlotAssignments - removal.index
            }
            else -> {
                val newPackages = dockPackages.toMutableList()
                if (removal.index < newPackages.size) {
                    newPackages[removal.index] = ""
                    dockPackages = newPackages
                    prefs.edit { putString("dock_apps", newPackages.joinToString(",")) }
                }
            }
        }
    }

    // アンインストールが実際に完了すると、UninstallResultReceiverがバックグラウンドで
    // SharedPreferencesの"grid_apps"/"dock_apps"を直接書き換える。ここではその変更を
    // 検知して、画面上のgridPackages/dockPackagesに反映する。
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            when (key) {
                "grid_apps" -> gridPackages = sharedPrefs.getString("grid_apps", "")?.split(",") ?: emptyList()
                "dock_apps" -> dockPackages = sharedPrefs.getString("dock_apps", "")?.split(",") ?: emptyList()
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    // スロット編集モード（アプリアイコン・ボタンなど個々のスロットの長押しで入る。✗バッジ表示用）
    var isEditMode by remember { mutableStateOf(false) }
    // ウィジェット編集モード（ウィジェットのヘッダーなど、個々のスロット以外の長押しで入る。
    // 移動・リサイズ・削除ハンドル表示用）。スロット編集モードとは独立しており、片方に入ると
    // もう片方は自動的に抜ける。
    var isWidgetEditMode by remember { mutableStateOf(false) }
    fun enterSlotEditMode() {
        isEditMode = true
        isWidgetEditMode = false
    }
    fun enterWidgetEditMode() {
        isWidgetEditMode = true
        isEditMode = false
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
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEditMode = false
                isWidgetEditMode = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
    var showMissingPermissionsSheet by remember { mutableStateOf(false) }
    var missingPermissionsResumeSignal by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (!MissingPermissionsSheetState.shownThisProcess) {
            MissingPermissionsSheetState.shownThisProcess = true
            if (OnboardingSteps.any { !it.isSatisfied(context) }) {
                showMissingPermissionsSheet = true
            }
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missingPermissionsResumeSignal++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (showMissingPermissionsSheet) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            MissingPermissionsSheet(
                resumeSignal = missingPermissionsResumeSignal,
                onDismiss = { showMissingPermissionsSheet = false }
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
    var placedWidgets by remember(widgetLayoutMode) { mutableStateOf(loadPlacedWidgets(prefs, widgetLayoutMode)) }
    fun updatePlacedWidgets(newWidgets: List<PlacedWidget>) {
        placedWidgets = newWidgets
        savePlacedWidgets(prefs, widgetLayoutMode, newWidgets)
    }
    var showWidgetTypeSelector by remember { mutableStateOf(false) } // 「+ ADD WIDGET」タップ時（種類選択待ち）
    var showAppWidgetPicker by remember { mutableStateOf(false) } // 「＋ 外部ウィジェットを追加」タップ時（プレビュー付き一覧表示中）

    // WidgetCanvasが測定したセル1つ分の実サイズ（dp）。新規追加する外部ウィジェットを、
    // 種類ごとの固定サイズではなく実際の推奨サイズ（dp）に応じたセル数で配置するために使う
    // （画面モードが変わるとグリッド寸法自体が変わるため、モードごとに保持し直す）
    var canvasCellSize by remember(widgetLayoutMode) { mutableStateOf<DpSize?>(null) }
    // 外部ウィジェットが、許容する最小サイズでもこの画面のグリッドに入り切らなかった
    // （または配置しようとした時点で空きがなかった）ことを知らせるエラーダイアログの表示状態
    var appWidgetTooLargeError by remember { mutableStateOf(false) }
    // 長押しメニューの「ウィジェットを追加」が押されたが、ホーム画面に空きがないことを知らせるダイアログの表示状態
    var showNoWidgetSpaceError by remember { mutableStateOf(false) }

    // 外部ウィジェット（他アプリのAppWidget）を追加するフロー。
    // allocateAppWidgetId()で確保したIDを、選択→バインド許可確認→（必要なら設定画面）→配置確定、
    // の間ずっと覚えておく必要があるため、ここに保持する
    var pendingAppWidgetId by remember { mutableIntStateOf(-1) }

    fun placeNewAppWidget(appWidgetId: Int) {
        // 種類問わず同じ標準サイズで配置していたのを、外部ウィジェットについては実際の
        // 推奨サイズ（AppWidgetProviderInfo.minWidth/minHeight）に応じたセル数で配置するようにし、
        // 「常に大きめの決め打ちサイズで追加される」問題を解消する。推奨サイズで空きがなければ、
        // 許容する最小サイズ（緩和後）まで縮めて再挑戦する
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)
        val cellSize = canvasCellSize
        val slot = if (info != null && cellSize != null) {
            val preferredColSpan = info.minWidth.dp / cellSize.width
            val preferredRowSpan = info.minHeight.dp / cellSize.height
            val relaxedMinSize = relaxedMinSizeDp(info)
            val minColSpan = relaxedMinSize.width / cellSize.width
            val minRowSpan = relaxedMinSize.height / cellSize.height
            findFreeGridSlotForSize(placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows, preferredColSpan, preferredRowSpan)
                ?: findFreeGridSlotForSize(placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows, minColSpan, minRowSpan)
        } else {
            findFreeGridSlot(placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows)?.let {
                floatArrayOf(it[0].toFloat(), it[1].toFloat(), it[2].toFloat(), it[3].toFloat())
            }
        }
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

    // バインド許可が下りた（＝appWidgetIdが実際に使える状態になった）直後の共通処理。
    // 設定画面（configure）を持つウィジェットならそちらを起動し、なければそのまま配置を確定する。
    //
    // 設定画面の起動には、自前でIntent(ACTION_APPWIDGET_CONFIGURE)を組み立てて直接startActivityは
    // しない。多くのOEM製ウィジェット（例: Samsung Notesの「ノートのショートカット」）は設定画面が
    // exported="false"であり、直接起動するとSecurityExceptionでクラッシュする。
    // AppWidgetHost.startAppWidgetConfigureActivityForResult()はシステムが発行した
    // IntentSender経由で起動するため、exportedでない設定画面も正しく開ける
    // （結果はAppWidgetConfigureResultBridge経由でMainActivity.onActivityResultから受け取る）
    fun proceedAfterBind(appWidgetId: Int) {
        val configureComponent = AppWidgetManager.getInstance(context).getAppWidgetInfo(appWidgetId)?.configure
        if (configureComponent != null) {
            AppWidgetConfigureResultBridge.onResult = { resultCode ->
                if (resultCode == Activity.RESULT_OK) {
                    placeNewAppWidget(appWidgetId)
                } else {
                    AppWidgetHostManager.host.deleteAppWidgetId(appWidgetId)
                }
            }
            AppWidgetHostManager.host.startAppWidgetConfigureActivityForResult(
                activity, appWidgetId, 0, AppWidgetConfigureResultBridge.REQUEST_CODE, null
            )
        } else {
            placeNewAppWidget(appWidgetId)
        }
    }

    // このアプリはBIND_APPWIDGET権限を持たない（サードパーティのランチャーは通常持てない）ため、
    // bindAppWidgetIdIfAllowedは基本的にfalseを返す。その場合はACTION_APPWIDGET_BINDで
    // システムのバインド確認ダイアログを挟む、というのが非特権ランチャーの標準的な実装方法
    val appWidgetBindLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val id = pendingAppWidgetId
        pendingAppWidgetId = -1
        if (id == -1) return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            proceedAfterBind(id)
        } else {
            AppWidgetHostManager.host.deleteAppWidgetId(id)
        }
    }

    // 自作の一覧（AppWidgetPickerDialog）でウィジェットが選択されたときの、バインド開始処理
    // 他アプリのAppWidgetは、種類（WidgetPanel.APPWIDGET）ではなくインスタンス（appWidgetId）ごとに
    // 実際の最小/最大サイズ・対応するリサイズ方向が異なるため、AppWidgetProviderInfoから解決する。
    // GridLauncher内蔵の4種はデフォルト値（制約なし）のままでよい
    fun appWidgetResizeConstraints(widget: PlacedWidget): ResizeConstraints {
        if (widget.type != WidgetPanel.APPWIDGET) return ResizeConstraints()
        val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(widget.appWidgetId)
            ?: return ResizeConstraints()
        // ウィジェットが申告する最小サイズを厳密に守ると、Claudeのように大きめの最小値を
        // 申告しているウィジェットが他ランチャーに比べてかなり大きく見えてしまうため、
        // 申告値の半分まではリサイズを許容する（画質が粗くなるリスクとのトレードオフ）
        val minSize = relaxedMinSizeDp(info)
        val maxSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            info.maxResizeWidth > 0 && info.maxResizeHeight > 0
        ) {
            DpSize(info.maxResizeWidth.dp, info.maxResizeHeight.dp)
        } else {
            null
        }
        val axes = when {
            info.resizeMode and AppWidgetProviderInfo.RESIZE_BOTH == AppWidgetProviderInfo.RESIZE_BOTH -> ResizeAxes.BOTH
            info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0 -> ResizeAxes.HORIZONTAL
            info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0 -> ResizeAxes.VERTICAL
            else -> ResizeAxes.NONE
        }
        return ResizeConstraints(minSize = minSize, maxSize = maxSize, axes = axes)
    }

    // ウィジェットが許容する最小サイズ（相対緩和後）でも、このグリッドの列数・行数に収まらない
    // 場合はfalse。measureSizeがまだ測定できていない場合は判断できないため許可扱いにする
    // （実際に配置しようとするタイミング＝placeNewAppWidgetで改めてチェックする）
    fun appWidgetFitsOnScreen(info: AppWidgetProviderInfo): Boolean {
        val cellSize = canvasCellSize ?: return true
        val minSize = relaxedMinSizeDp(info)
        val minColSpan = minSize.width / cellSize.width
        val minRowSpan = minSize.height / cellSize.height
        return minColSpan <= widgetLayoutMode.columns && minRowSpan <= widgetLayoutMode.rows
    }

    fun startBindFlow(info: AppWidgetProviderInfo) {
        if (!appWidgetFitsOnScreen(info)) {
            appWidgetTooLargeError = true
            return
        }
        val id = AppWidgetHostManager.host.allocateAppWidgetId()
        val alreadyBound = AppWidgetManager.getInstance(context).bindAppWidgetIdIfAllowed(id, info.provider)
        if (alreadyBound) {
            proceedAfterBind(id)
        } else {
            pendingAppWidgetId = id
            appWidgetBindLauncher.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                }
            )
        }
    }

    if (showAllAppsDrawer) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AllAppsDrawer(
                allApps = allApps,
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
                isDarkTheme = isDarkTheme,
                onDarkThemeChange = { dark ->
                    isDarkTheme = dark
                    prefs.edit { putBoolean("is_dark_theme", dark) }
                },
                accentColor = accentColor,
                onAccentColorChange = { color ->
                    accentColor = color
                    prefs.edit { putInt("accent_color", color.toArgb()) }
                },
                accentColor2 = accentColor2,
                onAccentColor2Change = { color ->
                    accentColor2 = color
                    prefs.edit { putInt("accent_color_2", color.toArgb()) }
                },
                accent2Panels = accent2WidgetPanels,
                onPanelAccentChange = { panel, useAccent2 -> setWidgetPanelAccent(panel, useAccent2) },
                useOriginalIconColors = useOriginalIconColors,
                onUseOriginalIconColorsChange = { enabled ->
                    if (enabled != useOriginalIconColors) toggleUseOriginalIconColors()
                },
                showAddWidgetTile = showAddWidgetTile,
                onShowAddWidgetTileChange = { visible ->
                    showAddWidgetTile = visible
                    prefs.edit { putBoolean("show_add_widget_tile", visible) }
                },
                hiddenPanels = hiddenWidgetPanels,
                onSetAllBorders = { visible -> setAllWidgetBorders(visible) },
                onTogglePanelBorder = { panel -> toggleWidgetPanelBorder(panel) },
                onOpenPowerMenu = {
                    openPowerMenuOrRequestPermission(context) {
                        showPowerPermissionRationale = true
                    }
                },
                onDismiss = { showCustomizeSheet = false }
            )
        }
    }

    if (showPowerPermissionRationale) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            PermissionRationaleDialog(
                message = "電源メニュー（電源を切る/再起動）を開くには、GridLauncherのアクセシビリティサービスを有効にしてください。",
                onConfirm = {
                    showPowerPermissionRationale = false
                    val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                onDismiss = { showPowerPermissionRationale = false }
            )
        }
    }

    // 編集モードで✗ボタンが押されたときの「スロットから削除」か「アンインストール」かの選択ダイアログ
    pendingRemoval?.let { removal ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppActionDialog(
                appName = removal.label,
                onDismiss = { pendingRemoval = null },
                onRemoveFromSlot = {
                    clearSlot(removal)
                    pendingRemoval = null
                },
                onUninstall = {
                    // スロットはここでは消さない。ユーザーが確認画面で実際にアンインストールを
                    // 完了した場合のみ、UninstallResultReceiverがSharedPreferencesを書き換え、
                    // 下のリスナー経由でgridPackages/dockPackagesに反映される。
                    requestUninstall(context, removal.packageName)
                    pendingRemoval = null
                }
            )
        }
    }

    if ((appSelectorTarget != null) && (targetIndex != null)) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AppSelectorDialog(
                allApps = allApps,
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
                        prefs.edit { putString("grid_apps", newPackages.joinToString(",")) }
                    } else if (appSelectorTarget == "dock") {
                        val newPackages = dockPackages.toMutableList()
                        while (newPackages.size <= targetIndex!!) {
                            newPackages.add("")
                        }
                        newPackages[targetIndex!!] = packageName
                        dockPackages = newPackages
                        prefs.edit { putString("dock_apps", newPackages.joinToString(",")) }
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
                onDismiss = { appSlotPickerInstanceId = null },
                onAppSelected = { packageName ->
                    saveAppSlotAssignment(prefs, instanceId, packageName)
                    appSlotAssignments = appSlotAssignments + (instanceId to packageName)
                    appSlotPickerInstanceId = null
                }
            )
        }
    }

    // グリッドの空きスロットタップ時、「アプリを追加」か「フォルダを作成」かを選ばせる
    addSlotChoiceIndex?.let { index ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AddSlotChoiceDialog(
                onDismiss = { addSlotChoiceIndex = null },
                onAddApp = {
                    appSelectorTarget = "grid"
                    targetIndex = index
                    addSlotChoiceIndex = null
                },
                onCreateFolder = {
                    val folder = createFolder(prefs, "新しいフォルダ")
                    folders = folders + (folder.id to folder)
                    val newPackages = gridPackages.toMutableList()
                    while (newPackages.size <= index) {
                        newPackages.add("")
                    }
                    newPackages[index] = folderSlotValue(folder.id)
                    gridPackages = newPackages
                    prefs.edit { putString("grid_apps", newPackages.joinToString(",")) }
                    addSlotChoiceIndex = null
                }
            )
        }
    }

    // QUICK ACCESSの空きスロットタップ時、追加するボタンの種類を選ばせる
    quickActionAddIndex?.let { index ->
        val usedActions = quickActionSlots.filterNotNull().toSet()
        val availableActions = QuickActionId.entries.filter { it !in usedActions }
        CompositionLocalProvider(LocalCyberColors provides colors) {
            QuickActionSelectorDialog(
                availableActions = availableActions,
                onDismiss = { quickActionAddIndex = null },
                onSelect = { actionId ->
                    val newSlots = quickActionSlots.toMutableList()
                    while (newSlots.size <= index) {
                        newSlots.add(null)
                    }
                    newSlots[index] = actionId
                    quickActionSlots = newSlots
                    saveQuickActionSlots(prefs, newSlots)
                    quickActionAddIndex = null
                }
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
                onDismiss = { showWidgetTypeSelector = false },
                onSelect = { type ->
                    val isAppSlot = type == WidgetPanel.APP_SLOT_ICON_ONLY || type == WidgetPanel.APP_SLOT_NAMED
                    val cellSize = canvasCellSize
                    val slot = if (isAppSlot && cellSize != null) {
                        val targetSize = if (type == WidgetPanel.APP_SLOT_ICON_ONLY) AppSlotIconOnlyTargetSize else AppSlotNamedTargetSize
                        findFreeGridSlotForSize(
                            placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows,
                            targetSize.width / cellSize.width, targetSize.height / cellSize.height
                        )
                    } else {
                        findFreeGridSlot(placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows)?.let {
                            floatArrayOf(it[0].toFloat(), it[1].toFloat(), it[2].toFloat(), it[3].toFloat())
                        }
                    }
                    if (slot != null) {
                        val (col, row, colSpan, rowSpan) = slot
                        val instanceId = if (isAppSlot) allocateNextAppSlotInstanceId(prefs) else -1
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
                onSelectExternal = {
                    showWidgetTypeSelector = false
                    showAppWidgetPicker = true
                }
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
            AlertDialog(
                onDismissRequest = { appWidgetTooLargeError = false },
                containerColor = colors.panel,
                title = {
                    Text("追加できません", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
                },
                text = {
                    Text(
                        "このウィジェットは、許容する最小サイズでもこの画面には入り切らないため追加できませんでした。",
                        fontFamily = CyberFont,
                        fontSize = 12.sp,
                        color = colors.text.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { appWidgetTooLargeError = false }) {
                        Text("閉じる", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
                    }
                }
            )
        }
    }

    // 長押しメニューからウィジェットを追加しようとしたが、ホーム画面に空きがなかった場合の通知
    if (showNoWidgetSpaceError) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AlertDialog(
                onDismissRequest = { showNoWidgetSpaceError = false },
                containerColor = colors.panel,
                title = {
                    Text("空きがありません", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text)
                },
                text = {
                    Text(
                        "ホーム画面にウィジェットを置く空きがありません。既存のウィジェットを縮小するか削除してから追加してください。",
                        fontFamily = CyberFont,
                        fontSize = 12.sp,
                        color = colors.text.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showNoWidgetSpaceError = false }) {
                        Text("閉じる", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
                    }
                }
            )
        }
    }

    CompositionLocalProvider(LocalCyberColors provides colors) {
        // フォルダを開いたときに、グリッド上のフォルダアイコンそのものがポップアップへ
        // 拡大していくコンテナ変形アニメーション（共有要素）を実現するため、メインの
        // グリッドとフォルダポップアップを同じSharedTransitionLayout内に配置する。
        SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        // Y方向（縦）の移動量がマイナス（上方向）に一定以上でドロワーを表示
                        if (isEditMode && dragAmount.y < -20) {
                            showAllAppsDrawer = true
                            isEditMode = false
                            change.consume()
                        } else if (!isEditMode && dragAmount.y < -20) {
                            showAllAppsDrawer = true
                            change.consume()
                        } else {
                            isEditMode = false
                            isWidgetEditMode = false
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        // 空白タップで編集モード解除
                        onTap = { isEditMode = false; isWidgetEditMode = false },
                        // 何もないところの長押しで、ウィジェット追加・カスタマイズのメニューを表示
                        onLongPress = { offset ->
                            isEditMode = false
                            isWidgetEditMode = false
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            homeMenuOffset = offset
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
                        start = if (isPortrait) 16.dp else 32.dp,
                        end = if (isPortrait) 16.dp else 32.dp,
                        bottom = if (isPortrait) 16.dp else 24.dp
                    )
            ) {
                if (isPortrait && screenWidthDp < 600) {
                    // 縦画面（小）: スマホサイズのカバー画面などのレイアウト
                    HeaderSectionPortrait(
                        nowPlaying = nowPlaying,
                        onCoreClick = { showCustomizeSheet = true }
                    )
                } else if (isPortrait) {
                    // 縦画面（大）: タブレットサイズや展開状態の大画面のレイアウト
                    HeaderSectionPortrait(
                        nowPlaying = nowPlaying,
                        isLarge = true,
                        onCoreClick = { showCustomizeSheet = true }
                    )
                } else {
                    // 横画面（ランドスケープ/メイン画面）のレイアウト
                    HeaderSectionLandscape(
                        nowPlaying = nowPlaying,
                        onCoreClick = { showCustomizeSheet = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                HeaderDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // ACCESS GRID内部のアプリ一覧の基準列数・行数。画面モードごとに従来の
                // 「Mサイズ」と同じ値を使う。ウィジェット自体がリサイズされた場合は、
                // AccessGridSection側が実際の描画サイズを見て、スロットが窮屈になりすぎなければ
                // 減らし、間延びしすぎるようなら増やす形でこの基準値から調整する
                val accessGridMColumns: Int
                val accessGridMRows: Int
                when (widgetLayoutMode) {
                    WidgetLayoutMode.SMALL_PORTRAIT -> {
                        accessGridMColumns = 3; accessGridMRows = 3
                    }
                    WidgetLayoutMode.LARGE_PORTRAIT -> {
                        accessGridMColumns = 4; accessGridMRows = 3
                    }
                    WidgetLayoutMode.LANDSCAPE -> {
                        accessGridMColumns = 3; accessGridMRows = 5
                    }
                }

                // ACCESS GRID/CALENDAR/SYSTEM MONITOR/QUICK ACCESSを、追加・削除・リサイズ・
                // 移動できるウィジェットとして配置するキャンバス。
                WidgetCanvas(
                    columns = widgetLayoutMode.columns,
                    rows = widgetLayoutMode.rows,
                    placedWidgets = placedWidgets,
                    isWidgetEditMode = isWidgetEditMode,
                    resizeConstraints = ::appWidgetResizeConstraints,
                    hideTopRightCorner = { widget ->
                        widget.type == WidgetPanel.APP_SLOT_ICON_ONLY || widget.type == WidgetPanel.APP_SLOT_NAMED
                    },
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
                    modifier = Modifier.weight(1f)
                ) { type, appWidgetId, instanceId, _, _, _, boxModifier, isResizing ->
                    // アクセントカラー2に設定されたウィジェットだけ、配色のaccentを差し替えて描画する
                    CompositionLocalProvider(LocalCyberColors provides if (type in accent2WidgetPanels) colors2 else colors) {
                    when (type) {
                        WidgetPanel.ACCESS_GRID -> {
                            AccessGridSection(
                                items = gridItems,
                                baseColumns = accessGridMColumns,
                                baseRows = accessGridMRows,
                                isEditMode = isEditMode,
                                isWallpaperMode = isWallpaperMode,
                                activeNotifications = activeNotifications,
                                openFolderId = openFolderId,
                                showBorder = WidgetPanel.ACCESS_GRID !in hiddenWidgetPanels,
                                isIconOnly = accessGridIconOnly,
                                onIconOnlyClick = { toggleAccessGridIconOnly() },
                                useOriginalIconColors = useOriginalIconColors,
                                onAddClick = { index -> addSlotChoiceIndex = index },
                                onFolderClick = { folderItem -> openFolderId = folderItem.folder.id },
                                onLongClick = { enterSlotEditMode() },
                                onRemoveClick = { index -> removeGridItem(index) },
                                onExitEditMode = { isEditMode = false }
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
                        WidgetPanel.QUICK_ACCESS -> QuickAccessSection(
                            modifier = boxModifier,
                            slots = quickActionSlots,
                            isEditMode = isEditMode,
                            isWallpaperMode = isWallpaperMode,
                            isResizing = isResizing,
                            accentColor = accentColor,
                            useOriginalIconColors = useOriginalIconColors,
                            showBorder = WidgetPanel.QUICK_ACCESS !in hiddenWidgetPanels,
                            onSlotsChanged = { newSlots ->
                                quickActionSlots = newSlots
                                saveQuickActionSlots(prefs, newSlots)
                            },
                            onThemeToggle = {
                                val newTheme = !isDarkTheme
                                isDarkTheme = newTheme
                                prefs.edit { putBoolean("is_dark_theme", newTheme) }
                            },
                            onAccentColorChange = { color ->
                                accentColor = color
                                prefs.edit { putInt("accent_color", color.toArgb()) }
                            },
                            onUseOriginalIconColorsChange = { toggleUseOriginalIconColors() },
                            onAddClick = { index -> quickActionAddIndex = index },
                            onLongClick = { enterSlotEditMode() },
                            onRemoveClick = { index -> removeQuickAction(index) },
                            onExitEditMode = { isEditMode = false }
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
                                    pendingRemoval = PendingRemoval("app_slot", instanceId, packageName, label)
                                }
                            },
                            onExitWidgetEditMode = { isWidgetEditMode = false }
                        )
                    }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HeaderDivider()
                Spacer(modifier = Modifier.height(16.dp))
                // 下段: よく使うアプリ（ドック）
                BottomDockSection(
                    apps = dockApps,
                    isEditMode = isEditMode,
                    isWallpaperMode = isWallpaperMode,
                    activeNotifications = activeNotifications, // 追加
                    useOriginalIconColors = useOriginalIconColors,
                    onAddClick = { index ->
                        appSelectorTarget = "dock"
                        targetIndex = index
                    },
                    onLongClick = { enterSlotEditMode() },
                    onRemoveClick = { index ->
                        dockApps.getOrNull(index)?.let { appInfo ->
                            pendingRemoval = PendingRemoval("dock", index, appInfo.packageName, appInfo.label)
                        }
                    },
                    onExitEditMode = { isEditMode = false }
                )
                
                // ナビゲーションバー/タスクバー用の余白（システムバーと被らないようにさらにスペースを確保）
                Spacer(modifier = Modifier.height(if (isPortrait) 32.dp else 40.dp))
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
                    onRemoveApp = { index ->
                        val newPackages = folder.packageNames.toMutableList()
                        if (index < newPackages.size) {
                            newPackages[index] = ""
                            val updated = folder.copy(packageNames = newPackages)
                            folders = folders + (updated.id to updated)
                            saveFolder(prefs, updated)
                        }
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
                    .padding(bottom = if (isPortrait) 90.dp else 70.dp)
                    .onGloballyPositioned { coordinates -> deleteZoneBoundsInRoot = coordinates.boundsInRoot() }
            ) {
                DeleteWidgetDropZone(isActive = isDraggedWidgetOverDeleteZone)
            }
        }
        }
    }

    // 「ここにドラッグして削除」ゾーンにドロップされたウィジェットの削除確認。
    // 上のCompositionLocalProviderのスコープ外にあるため、テーマ（colors）を
    // 明示的に渡し直さないとLocalCyberColorsのデフォルト値（ライトテーマ固定）に
    // フォールバックしてしまい、実際のテーマ設定に関わらず常に同じ配色になってしまう
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
                    if (widget.type == WidgetPanel.APP_SLOT_ICON_ONLY || widget.type == WidgetPanel.APP_SLOT_NAMED) {
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

/**
 * ウィジェットの移動ドラッグ中にDock付近へ表示する、「ここにドラッグして削除」ゾーン。
 * ドラッグ中の指がこの範囲に入っている間は[isActive]がtrueになり、危険色で強調表示する。
 */
@Composable
private fun DeleteWidgetDropZone(isActive: Boolean) {
    val colors = LocalCyberColors.current
    val dangerColor = Color(0xFFFF3B4E)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) dangerColor.copy(alpha = 0.3f) else colors.bg.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, dangerColor.copy(alpha = if (isActive) 1f else 0.6f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = dangerColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isActive) "指を離すと削除します" else "ここにドラッグして削除",
                fontFamily = CyberFont,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = dangerColor
            )
        }
    }
}
