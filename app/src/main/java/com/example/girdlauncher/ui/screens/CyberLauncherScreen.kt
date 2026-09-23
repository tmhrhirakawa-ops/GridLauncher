package com.example.girdlauncher.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.girdlauncher.model.FolderInfo
import com.example.girdlauncher.model.GridItem
import com.example.girdlauncher.model.PlacedWidget
import com.example.girdlauncher.model.QuickActionId
import com.example.girdlauncher.model.WidgetPanel
import com.example.girdlauncher.ui.components.AddSlotChoiceDialog
import com.example.girdlauncher.ui.components.AppActionDialog
import com.example.girdlauncher.ui.components.PermissionRationaleDialog
import com.example.girdlauncher.ui.components.QuickActionSelectorDialog
import com.example.girdlauncher.ui.components.WidgetDeleteConfirmDialog
import com.example.girdlauncher.ui.components.WidgetTypeSelectorDialog
import com.example.girdlauncher.ui.sections.*
import com.example.girdlauncher.ui.theme.*
import com.example.girdlauncher.util.createFolder
import com.example.girdlauncher.util.deleteFolder
import com.example.girdlauncher.util.findFreeGridSlot
import com.example.girdlauncher.util.folderIdFromSlotValue
import com.example.girdlauncher.util.folderSlotValue
import com.example.girdlauncher.util.getInstalledApps
import com.example.girdlauncher.util.isFolderSlotValue
import com.example.girdlauncher.util.isNotificationListenerEnabled
import com.example.girdlauncher.util.loadFolders
import com.example.girdlauncher.util.loadHiddenWidgetPanels
import com.example.girdlauncher.util.loadPlacedWidgets
import com.example.girdlauncher.util.loadQuickActionSlots
import com.example.girdlauncher.util.resolveInstalledApp
import com.example.girdlauncher.util.requestUninstall
import com.example.girdlauncher.util.saveFolder
import com.example.girdlauncher.util.saveHiddenWidgetPanels
import com.example.girdlauncher.util.savePlacedWidgets
import com.example.girdlauncher.util.saveQuickActionSlots
import com.example.girdlauncher.util.CyberNotificationListener
import com.example.girdlauncher.util.WidgetLayoutMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * 編集モードで削除操作が要求されたスロットの情報。
 * 「スロットから削除」か「アンインストール」かをダイアログで選ばせるために保持する。
 */
private data class PendingRemoval(
    val target: String, // "grid" または "dock"
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
    val prefs = remember { context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE) }
    var allApps by remember { mutableStateOf(getInstalledApps(context.packageManager)) }

    // 通知アクセス権限（通知バッジ・再生中メディア・QUICK ACCESSのミュート操作に必要）が
    // 未許可の場合、初回起動時に一度だけ権限付与画面へ案内する（案内前に理由を説明するダイアログを挟む）
    var showNotificationAccessRationale by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val alreadyPrompted = prefs.getBoolean("notification_access_prompted", false)
        if (!alreadyPrompted && !isNotificationListenerEnabled(context)) {
            prefs.edit { putBoolean("notification_access_prompted", true) }
            showNotificationAccessRationale = true
        }
    }

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

    // APP LISTのICON ONLYモード（アイコンのみ表示・正方形スロット）かどうか。ヘッダーのボタンで切り替える。
    var accessGridIconOnly by remember { mutableStateOf(prefs.getBoolean("access_grid_icon_only", false)) }
    fun toggleAccessGridIconOnly() {
        accessGridIconOnly = !accessGridIconOnly
        prefs.edit { putBoolean("access_grid_icon_only", accessGridIconOnly) }
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
    fun toggleAllWidgetBorders() {
        val allPanels = WidgetPanel.entries.toSet()
        hiddenWidgetPanels = if (hiddenWidgetPanels == allPanels) emptySet() else allPanels
        saveHiddenWidgetPanels(prefs, hiddenWidgetPanels)
    }
    fun toggleWidgetPanelBorder(panel: WidgetPanel) {
        hiddenWidgetPanels = if (panel in hiddenWidgetPanels) hiddenWidgetPanels - panel else hiddenWidgetPanels + panel
        saveHiddenWidgetPanels(prefs, hiddenWidgetPanels)
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

    val gridItems: List<GridItem?> = gridPackages.map { pkg ->
        when {
            pkg.isEmpty() -> null
            isFolderSlotValue(pkg) -> folderIdFromSlotValue(pkg)?.let { folders[it] }?.let { GridItem.FolderItem(it) }
            else -> allApps.find { it.packageName == pkg }?.let { GridItem.AppItem(it) }
        }
    }
    val dockApps = dockPackages.map { pkg -> if (pkg.isEmpty()) null else allApps.find { it.packageName == pkg } }

    var appSelectorTarget by remember { mutableStateOf<String?>(null) } // "grid" または "dock"
    var targetIndex by remember { mutableStateOf<Int?>(null) } // 追加する位置（インデックス）を保持
    var addSlotChoiceIndex by remember { mutableStateOf<Int?>(null) } // グリッドの空きスロットタップ時（アプリ/フォルダ選択待ち）
    var openFolderId by remember { mutableStateOf<String?>(null) } // 中身を表示中のフォルダ
    // ポップアップを閉じるアニメーション中もフォルダの中身を表示し続けるため、openFolderIdが
    // nullになった後も直前に表示していたフォルダの情報を保持しておく
    var displayedFolder by remember { mutableStateOf<FolderInfo?>(null) }
    var showAllAppsDrawer by remember { mutableStateOf(false) } // アプリドロワーの表示状態
    var pendingRemoval by remember { mutableStateOf<PendingRemoval?>(null) } // ✗ボタン押下時の操作選択待ち

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
        if (removal.target == "grid") {
            val newPackages = gridPackages.toMutableList()
            if (removal.index < newPackages.size) {
                newPackages[removal.index] = ""
                gridPackages = newPackages
                prefs.edit { putString("grid_apps", newPackages.joinToString(",")) }
            }
        } else {
            val newPackages = dockPackages.toMutableList()
            if (removal.index < newPackages.size) {
                newPackages[removal.index] = ""
                dockPackages = newPackages
                prefs.edit { putString("dock_apps", newPackages.joinToString(",")) }
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
    // draggingWidgetTypeは現在移動ドラッグ中のウィジェット（非ドラッグ中はnull）で、これに応じて
    // ゾーンの表示・非表示を切り替える。deleteZoneBoundsInRootはそのゾーンのルート座標系での
    // 範囲（当たり判定に使う）。pendingDeleteWidgetTypeはゾーンにドロップされ、削除確認
    // ダイアログを表示中のウィジェット。
    var draggingWidgetType by remember { mutableStateOf<WidgetPanel?>(null) }
    var isDraggedWidgetOverDeleteZone by remember { mutableStateOf(false) }
    var deleteZoneBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var pendingDeleteWidgetType by remember { mutableStateOf<WidgetPanel?>(null) }

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

    // 壁紙透過モード
    var isWallpaperMode by remember { mutableStateOf(prefs.getBoolean("is_wallpaper_mode", false)) }

    // 通知の監視
    val activeNotifications by CyberNotificationListener.activeNotifications.collectAsState()
    // 再生中メディアの監視
    val nowPlaying by CyberNotificationListener.nowPlaying.collectAsState()

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
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

    // 初回起動時、通知アクセス権限が未許可なら理由を説明してから権限付与画面へ案内する
    if (showNotificationAccessRationale) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            PermissionRationaleDialog(
                message = "通知バッジや再生中メディアの表示、QUICK ACCESSのミュート操作を使うには、GridLauncherへの通知へのアクセスを許可してください。",
                onConfirm = {
                    showNotificationAccessRationale = false
                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                onDismiss = { showNotificationAccessRationale = false }
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
        val availableWidgets = WidgetPanel.entries.filterNot { it in placedTypes }
        CompositionLocalProvider(LocalCyberColors provides colors) {
            WidgetTypeSelectorDialog(
                availableWidgets = availableWidgets,
                onDismiss = { showWidgetTypeSelector = false },
                onSelect = { type ->
                    val slot = findFreeGridSlot(placedWidgets, widgetLayoutMode.columns, widgetLayoutMode.rows)
                    if (slot != null) {
                        val (col, row, colSpan, rowSpan) = slot
                        updatePlacedWidgets(placedWidgets + PlacedWidget(type, col.toFloat(), row.toFloat(), colSpan.toFloat(), rowSpan.toFloat()))
                    }
                    showWidgetTypeSelector = false
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
                .clickable { isEditMode = false; isWidgetEditMode = false }, // 空白タップで編集モード解除
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
                        isWallpaperMode = isWallpaperMode,
                        onWallpaperToggle = {
                            isWallpaperMode = !isWallpaperMode
                            prefs.edit { putBoolean("is_wallpaper_mode", isWallpaperMode) }
                        },
                        hiddenPanels = hiddenWidgetPanels,
                        onToggleAllBorders = { toggleAllWidgetBorders() },
                        onTogglePanelBorder = { panel -> toggleWidgetPanelBorder(panel) }
                    )
                } else if (isPortrait) {
                    // 縦画面（大）: タブレットサイズや展開状態の大画面のレイアウト
                    HeaderSectionPortrait(
                        nowPlaying = nowPlaying,
                        isLarge = true,
                        isWallpaperMode = isWallpaperMode,
                        onWallpaperToggle = {
                            isWallpaperMode = !isWallpaperMode
                            prefs.edit { putBoolean("is_wallpaper_mode", isWallpaperMode) }
                        },
                        hiddenPanels = hiddenWidgetPanels,
                        onToggleAllBorders = { toggleAllWidgetBorders() },
                        onTogglePanelBorder = { panel -> toggleWidgetPanelBorder(panel) }
                    )
                } else {
                    // 横画面（ランドスケープ/メイン画面）のレイアウト
                    HeaderSectionLandscape(
                        nowPlaying = nowPlaying,
                        isWallpaperMode = isWallpaperMode,
                        onWallpaperToggle = {
                            isWallpaperMode = !isWallpaperMode
                            prefs.edit { putBoolean("is_wallpaper_mode", isWallpaperMode) }
                        },
                        hiddenPanels = hiddenWidgetPanels,
                        onToggleAllBorders = { toggleAllWidgetBorders() },
                        onTogglePanelBorder = { panel -> toggleWidgetPanelBorder(panel) }
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
                    deleteZoneBoundsInRoot = deleteZoneBoundsInRoot,
                    onLayoutChange = { updatePlacedWidgets(it) },
                    onRequestAddWidget = { showWidgetTypeSelector = true },
                    onWidgetLongClick = { enterWidgetEditMode() },
                    onExitWidgetEditMode = { isWidgetEditMode = false },
                    onWidgetDragStateChanged = { type, dragging, overDeleteZone ->
                        draggingWidgetType = if (dragging) type else null
                        isDraggedWidgetOverDeleteZone = overDeleteZone
                    },
                    onRequestDeleteConfirm = { type -> pendingDeleteWidgetType = type },
                    modifier = Modifier.weight(1f)
                ) { type, _, _, _, boxModifier, isResizing ->
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
                visible = draggingWidgetType != null,
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
    pendingDeleteWidgetType?.let { widgetType ->
        CompositionLocalProvider(LocalCyberColors provides colors) {
            WidgetDeleteConfirmDialog(
                widgetLabel = widgetType.label,
                onConfirm = {
                    updatePlacedWidgets(placedWidgets.filter { it.type != widgetType })
                    pendingDeleteWidgetType = null
                },
                onDismiss = { pendingDeleteWidgetType = null }
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
