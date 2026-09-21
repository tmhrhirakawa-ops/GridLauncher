package com.example.girdlauncher.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.girdlauncher.ui.components.AppActionDialog
import com.example.girdlauncher.ui.sections.*
import com.example.girdlauncher.ui.theme.*
import com.example.girdlauncher.util.getInstalledApps
import com.example.girdlauncher.util.requestUninstall
import com.example.girdlauncher.util.CyberNotificationListener
import androidx.compose.ui.graphics.Color

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
 * ランチャーのメイン画面。デバイスの向きや画面サイズに基づいて、
 * すべてのセクションのレイアウトを調整します。
 */
@Composable
fun CyberLauncherScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE) }
    var allApps by remember { mutableStateOf(getInstalledApps(context.packageManager)) }

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
                allApps = getInstalledApps(context.packageManager)
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
    val colors = if (isDarkTheme) {
        CyberColors(DarkBgColor, DarkPanelColor, DarkAccentColor, DarkTextColor, DarkBorderColor, DarkCoreColor)
    } else {
        CyberColors(LightBgColor, LightPanelColor, LightAccentColor, LightTextColor, LightBorderColor, LightCoreColor)
    }

    // GridApps: SharedPreferencesから保存されたパッケージ名リストを読み込む
    var gridPackages by remember {
        mutableStateOf(prefs.getString("grid_apps", "")?.split(",") ?: emptyList())
    }

    // DockApps: SharedPreferencesから保存されたパッケージ名リストを読み込む
    var dockPackages by remember {
        mutableStateOf(prefs.getString("dock_apps", "")?.split(",") ?: emptyList())
    }

    val gridApps = gridPackages.map { pkg -> if (pkg.isEmpty()) null else allApps.find { it.packageName == pkg } }
    val dockApps = dockPackages.map { pkg -> if (pkg.isEmpty()) null else allApps.find { it.packageName == pkg } }

    var appSelectorTarget by remember { mutableStateOf<String?>(null) } // "grid" または "dock"
    var targetIndex by remember { mutableStateOf<Int?>(null) } // 追加する位置（インデックス）を保持
    var showAllAppsDrawer by remember { mutableStateOf(false) } // アプリドロワーの表示状態
    var pendingRemoval by remember { mutableStateOf<PendingRemoval?>(null) } // ✗ボタン押下時の操作選択待ち

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

    var isEditMode by remember { mutableStateOf(false) } // 編集モード

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

    CompositionLocalProvider(LocalCyberColors provides colors) {
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
                        }
                    }
                }
                .clickable { isEditMode = false }, // 空白タップで編集モード解除
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
                if (isPortrait) {
                    // 縦画面（ポートレート/カバー画面）のレイアウト
                    HeaderSectionPortrait(nowPlaying = nowPlaying)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.weight(1.5f)) {
                        AccessGridSection(
                            apps = gridApps,
                            columns = 3,
                            rows = 3,
                            isPortrait = isPortrait, // isPortrait を渡す
                            isEditMode = isEditMode,
                            isWallpaperMode = isWallpaperMode,
                            activeNotifications = activeNotifications, // 追加
                            onAddClick = { index ->
                                appSelectorTarget = "grid"
                                targetIndex = index
                            },
                            onLongClick = { isEditMode = true },
                            onRemoveClick = { index ->
                                gridApps.getOrNull(index)?.let { appInfo ->
                                    pendingRemoval = PendingRemoval("grid", index, appInfo.packageName, appInfo.label)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ウィジェットエリア
                    Row(modifier = Modifier.weight(1f)) {
                        // 画面幅が狭い（おおよそ600dp未満のスマホサイズのポートレートなど）場合はDeviceStatusを表示、広い場合はカレンダーを表示
                        if (screenWidthDp < 600) {
                            DeviceStatusSection(modifier = Modifier.weight(1.5f))
                        } else {
                            CalendarSection(modifier = Modifier.weight(2f)) // カレンダーウィジェットを広く
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        // 横画面と同じ2列×3行のQUICK ACCESSを使用する
                        QuickAccessSection(
                            modifier = Modifier.weight(1f),
                            isWallpaperMode = isWallpaperMode,
                            onWallpaperToggle = {
                                isWallpaperMode = !isWallpaperMode
                                prefs.edit { putBoolean("is_wallpaper_mode", isWallpaperMode) }
                            },
                            onThemeToggle = {
                                val newTheme = !isDarkTheme
                                isDarkTheme = newTheme
                                prefs.edit { putBoolean("is_dark_theme", newTheme) }
                            }
                        )
                    }
                } else {
                    // 横画面（ランドスケープ/メイン画面）のレイアウト
                    HeaderSectionLandscape(nowPlaying = nowPlaying)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.weight(1f)) {
                        // 左側: アプリグリッド (weight 1f)
                        Box(modifier = Modifier.weight(1f)) {
                            AccessGridSection(
                                apps = gridApps,
                                columns = 3,
                                rows = 5,
                                isPortrait = isPortrait, // isPortrait を渡す
                                isEditMode = isEditMode,
                                isWallpaperMode = isWallpaperMode,
                                activeNotifications = activeNotifications, // 追加
                                onAddClick = { index -> 
                                    appSelectorTarget = "grid" 
                                    targetIndex = index
                                },
                                onLongClick = { isEditMode = true },
                                onRemoveClick = { index ->
                                    gridApps.getOrNull(index)?.let { appInfo ->
                                        pendingRemoval = PendingRemoval("grid", index, appInfo.packageName, appInfo.label)
                                    }
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // 右側: ウィジェットエリア (weight 1f に戻す)
                        Column(modifier = Modifier.weight(1f)) {
                            CalendarSection(modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.height(16.dp))
                            // 下段エリアの高さを縦方向に広げる (weight を 1.5f に設定)
                            Row(modifier = Modifier.weight(0.8f)) {
                                DeviceStatusSection(modifier = Modifier.weight(1f))
                                Spacer(modifier = Modifier.width(16.dp))
                                // QUICK ACCESS は横幅を戻す
                                QuickAccessSection(
                                    modifier = Modifier.weight(1f),
                                    isWallpaperMode = isWallpaperMode,
                                    onWallpaperToggle = {
                                        isWallpaperMode = !isWallpaperMode
                                        prefs.edit { putBoolean("is_wallpaper_mode", isWallpaperMode) }
                                    },
                                    onThemeToggle = {
                                        val newTheme = !isDarkTheme
                                        isDarkTheme = newTheme
                                        prefs.edit { putBoolean("is_dark_theme", newTheme) }
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                // 下段: よく使うアプリ（ドック）
                BottomDockSection(
                    apps = dockApps,
                    isEditMode = isEditMode,
                    isWallpaperMode = isWallpaperMode,
                    activeNotifications = activeNotifications, // 追加
                    onAddClick = { index -> 
                        appSelectorTarget = "dock" 
                        targetIndex = index
                    },
                    onLongClick = { isEditMode = true },
                    onRemoveClick = { index ->
                        dockApps.getOrNull(index)?.let { appInfo ->
                            pendingRemoval = PendingRemoval("dock", index, appInfo.packageName, appInfo.label)
                        }
                    }
                )
                
                // ナビゲーションバー/タスクバー用の余白（システムバーと被らないようにさらにスペースを確保）
                Spacer(modifier = Modifier.height(if (isPortrait) 32.dp else 40.dp))
            }
        }
    }
}
