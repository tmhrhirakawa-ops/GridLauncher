package com.example.girdlauncher.ui.screens

import android.content.Context
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
import androidx.compose.ui.unit.dp
import com.example.girdlauncher.ui.sections.*
import com.example.girdlauncher.ui.theme.*
import com.example.girdlauncher.util.getInstalledApps
import com.example.girdlauncher.util.CyberNotificationListener

/**
 * ランチャーのメイン画面。デバイスの向きや画面サイズに基づいて、
 * すべてのセクションのレイアウトを調整します。
 */
@Composable
fun CyberLauncherScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE) }
    val allApps = remember { getInstalledApps(context.packageManager) }
    
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
        mutableStateOf(prefs.getString("grid_apps", "")?.split(",")?.toMutableList() ?: mutableListOf<String>()) 
    }
    
    // DockApps: SharedPreferencesから保存されたパッケージ名リストを読み込む
    var dockPackages by remember { 
        mutableStateOf(prefs.getString("dock_apps", "")?.split(",")?.toMutableList() ?: mutableListOf<String>()) 
    }

    val gridApps = gridPackages.map { pkg -> if (pkg.isEmpty()) null else allApps.find { it.packageName == pkg } }
    val dockApps = dockPackages.map { pkg -> if (pkg.isEmpty()) null else allApps.find { it.packageName == pkg } }

    var appSelectorTarget by remember { mutableStateOf<String?>(null) } // "grid" または "dock"
    var targetIndex by remember { mutableStateOf<Int?>(null) } // 追加する位置（インデックス）を保持
    var showAllAppsDrawer by remember { mutableStateOf(false) } // アプリドロワーの表示状態
    
    var isEditMode by remember { mutableStateOf(false) } // 編集モード

    // 通知の監視
    val activeNotifications by CyberNotificationListener.activeNotifications.collectAsState()

    val configuration = LocalConfiguration.current
    val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
    // 画面の幅（dp）を取得
    val screenWidthDp = configuration.screenWidthDp

    if (showAllAppsDrawer) {
        CompositionLocalProvider(LocalCyberColors provides colors) {
            AllAppsDrawer(
                allApps = allApps,
                onDismiss = { showAllAppsDrawer = false }
            )
        }
    }

    if (appSelectorTarget != null && targetIndex != null) {
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
                        prefs.edit().putString("grid_apps", newPackages.joinToString(",")).apply()
                    } else if (appSelectorTarget == "dock") {
                        val newPackages = dockPackages.toMutableList()
                        while (newPackages.size <= targetIndex!!) {
                            newPackages.add("")
                        }
                        newPackages[targetIndex!!] = packageName
                        dockPackages = newPackages
                        prefs.edit().putString("dock_apps", newPackages.joinToString(",")).apply()
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
            color = LocalCyberColors.current.bg
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
                    HeaderSectionPortrait()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(modifier = Modifier.weight(1.5f)) {
                        AccessGridSection(
                            apps = gridApps,
                            columns = 3,
                            rows = 3,
                            isPortrait = isPortrait, // isPortrait を渡す
                            isEditMode = isEditMode,
                            activeNotifications = activeNotifications, // 追加
                            onAddClick = { index -> 
                                appSelectorTarget = "grid" 
                                targetIndex = index
                            },
                            onLongClick = { isEditMode = true },
                            onRemoveClick = { index ->
                                val newPackages = gridPackages.toMutableList()
                                if (index < newPackages.size) {
                                    newPackages[index] = ""
                                    gridPackages = newPackages
                                    prefs.edit().putString("grid_apps", newPackages.joinToString(",")).apply()
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
                            onThemeToggle = {
                                val newTheme = !isDarkTheme
                                isDarkTheme = newTheme
                                prefs.edit().putBoolean("is_dark_theme", newTheme).apply()
                            }
                        )
                    }
                } else {
                    // 横画面（ランドスケープ/メイン画面）のレイアウト
                    HeaderSectionLandscape()
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
                                activeNotifications = activeNotifications, // 追加
                                onAddClick = { index -> 
                                    appSelectorTarget = "grid" 
                                    targetIndex = index
                                },
                                onLongClick = { isEditMode = true },
                                onRemoveClick = { index ->
                                    val newPackages = gridPackages.toMutableList()
                                    if (index < newPackages.size) {
                                        newPackages[index] = ""
                                        gridPackages = newPackages
                                        prefs.edit().putString("grid_apps", newPackages.joinToString(",")).apply()
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
                                    onThemeToggle = {
                                        val newTheme = !isDarkTheme
                                        isDarkTheme = newTheme
                                        prefs.edit().putBoolean("is_dark_theme", newTheme).apply()
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
                    activeNotifications = activeNotifications, // 追加
                    onAddClick = { index -> 
                        appSelectorTarget = "dock" 
                        targetIndex = index
                    },
                    onLongClick = { isEditMode = true },
                    onRemoveClick = { index ->
                        val newPackages = dockPackages.toMutableList()
                        if (index < newPackages.size) {
                            newPackages[index] = ""
                            dockPackages = newPackages
                            prefs.edit().putString("dock_apps", newPackages.joinToString(",")).apply()
                        }
                    }
                )
                
                // ナビゲーションバー/タスクバー用の余白（システムバーと被らないようにさらにスペースを確保）
                Spacer(modifier = Modifier.height(if (isPortrait) 32.dp else 40.dp))
            }
        }
    }
}
