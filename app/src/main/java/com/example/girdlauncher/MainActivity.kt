package com.example.girdlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import android.content.Context
import android.os.BatteryManager
import android.app.usage.UsageStatsManager
import android.app.AppOpsManager
import android.os.Process
import android.os.Environment
import android.os.StatFs
import android.app.ActivityManager
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.basicMarquee
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.GirdLauncherTheme
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GirdLauncherTheme {
                CyberLauncherScreen()
            }
        }
    }
}

// ライトテーマ用のカラー定義（ZZZ風のベージュ×オレンジ）
val LightBgColor = Color(0xFFF7DFCB)
val LightPanelColor = Color(0xFFF2D3B8)
val LightAccentColor = Color(0xFFFF5722)
val LightTextColor = Color(0xFF4A4A4A)
val LightBorderColor = Color(0x334A4A4A)
val LightCoreColor = Color(0xFF1E3A8A)

// ダークテーマ用のカラー定義（黒背景×オレンジ）
val DarkBgColor = Color(0xFF121212)
val DarkPanelColor = Color(0xFF1E1E1E)
val DarkAccentColor = Color(0xFFFF5722)
val DarkTextColor = Color(0xFFE0E0E0)
val DarkBorderColor = Color(0x33E0E0E0)
val DarkCoreColor = Color(0xFF3B82F6) // ダークテーマ用に少し明るい青

// 現在のテーマに応じたカラーを保持するクラス
data class CyberColors(
    val bg: Color,
    val panel: Color,
    val accent: Color,
    val text: Color,
    val border: Color,
    val core: Color
)

// テーマプロバイダー（CompositionLocal）
val LocalCyberColors = staticCompositionLocalOf {
    CyberColors(LightBgColor, LightPanelColor, LightAccentColor, LightTextColor, LightBorderColor, LightCoreColor)
}

// サイバー風フォント
val CyberFont = FontFamily(
    Font(R.font.share_tech_mono)
)

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

@Composable
fun HeaderSectionLandscape() {
    val context = LocalContext.current
    
    // リアルタイム時計とバッテリーの状態管理
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var batteryLevel by remember { mutableStateOf(100) }
    
    // 1秒ごとに時刻とバッテリー状態を更新するコルーチン
    LaunchedEffect(Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            currentTime = System.currentTimeMillis()
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            delay(1000)
        }
    }

    // 時刻と日付のフォーマット
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd // EEE", Locale.ENGLISH) // 例: SEP 13 // SUN
    
    val timeString = timeFormat.format(Date(currentTime))
    val dateString = dateFormat.format(Date(currentTime)).uppercase()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(timeString, fontFamily = CyberFont, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text, letterSpacing = 2.sp)
            Text(dateString, fontFamily = CyberFont, fontSize = 12.sp, color = LocalCyberColors.current.text.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SYSTEM ONLINE", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
            Text("MAIN TERMINAL", fontFamily = CyberFont, fontSize = 24.sp, fontWeight = FontWeight.Black, color = LocalCyberColors.current.text, letterSpacing = 2.sp)
            Text("TOKYO // MAIN TERMINAL", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
        }
        
        // バッテリー残量とシステムステータスのUI
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text("BATTERY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
                Text("$batteryLevel%", fontFamily = CyberFont, fontSize = 24.sp, fontWeight = FontWeight.Black, color = LocalCyberColors.current.text)
            }
            Spacer(modifier = Modifier.width(16.dp))
            // バッテリーのサークルインジケーター（ZZZ風の円形UI）
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(56.dp)) {
                CircularProgressIndicator(
                    progress = { batteryLevel / 100f },
                    color = LocalCyberColors.current.accent,
                    trackColor = LocalCyberColors.current.border,
                    strokeWidth = 6.dp,
                    modifier = Modifier.fillMaxSize()
                )
                // 真ん中の青いコア（目のように見える部分）
                Box(modifier = Modifier.size(20.dp).background(LocalCyberColors.current.core, RoundedCornerShape(10.dp)))
            }
        }
    }
}

@Composable
fun HeaderSectionPortrait() {
    val context = LocalContext.current
    
    // リアルタイム時計とバッテリーの状態管理
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    var batteryLevel by remember { mutableStateOf(100) }
    
    LaunchedEffect(Unit) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        while (true) {
            currentTime = System.currentTimeMillis()
            batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            delay(1000)
        }
    }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd // EEE", Locale.ENGLISH)
    val timeString = timeFormat.format(Date(currentTime))
    val dateString = dateFormat.format(Date(currentTime)).uppercase()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column {
            Text(timeString, fontFamily = CyberFont, fontSize = 56.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text, letterSpacing = 2.sp)
            Text(dateString, fontFamily = CyberFont, fontSize = 12.sp, color = LocalCyberColors.current.text.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(6.dp))
                Text("22° // TOKYO", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text, fontWeight = FontWeight.Bold)
            }
        }
        
        // 縦画面は右上に青いコア（FAIRY）とバッテリーを配置
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("SYSTEM // STANDBY", fontFamily = CyberFont, fontSize = 8.sp, color = LocalCyberColors.current.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Text("$batteryLevel%", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(
                    progress = { batteryLevel / 100f },
                    color = LocalCyberColors.current.accent,
                    trackColor = LocalCyberColors.current.border,
                    strokeWidth = 6.dp,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.size(24.dp).background(LocalCyberColors.current.core, RoundedCornerShape(12.dp)))
            }
            Text("BATTERY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.core, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun AccessGridSection(
    apps: List<AppInfo?>,
    columns: Int,
    rows: Int,
    isPortrait: Boolean = false, // 縦画面かどうかのフラグを追加
    isEditMode: Boolean = false,
    onAddClick: (Int) -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(8.dp)
                .background(LocalCyberColors.current.accent))
            Spacer(modifier = Modifier.width(8.dp))
            if (isPortrait) {
                Text("N.E.P.T // COVER TERMINAL", fontFamily = CyberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
            } else {
                Text("ACCESS GRID", fontFamily = CyberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                Text(" // APP NODES", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        // アプリを指定された行数・列数で分割
        val pageSize = columns * rows
        // 少なくとも1ページ分は空きスロットを表示する
        val pageCount = maxOf(1, (apps.size + 1) / pageSize + if ((apps.size + 1) % pageSize == 0) 0 else 1)
        val pagerState = rememberPagerState(pageCount = { pageCount })
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val startIndex = page * pageSize
            val pageApps = apps.drop(startIndex).take(pageSize)
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 行のループ
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 列のループ
                        for (colIndex in 0 until columns) {
                            val appIndex = rowIndex + (colIndex * rows) // 縦埋めから横埋めに変更が必要な場合はここを修正
                            val globalIndex = startIndex + appIndex
                            
                            if (appIndex < pageApps.size && pageApps[appIndex] != null) {
                                val appInfo = pageApps[appIndex]!!
                                AppCard(
                                    name = appInfo.label,
                                    icon = appInfo.icon,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    isEditMode = isEditMode,
                                    onClick = {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                        }
                                    },
                                    onLongClick = onLongClick,
                                    onRemoveClick = { onRemoveClick(globalIndex) }
                                )
                            } else {
                                // 空きスロット（タップでアプリ追加）
                                Surface(
                                    onClick = { onAddClick(globalIndex) },
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, LocalCyberColors.current.border),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("EMPTY SLOT", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
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

data class AppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable
)

fun getInstalledApps(packageManager: PackageManager): List<AppInfo> {
    val intent = Intent(Intent.ACTION_MAIN, null)
    intent.addCategory(Intent.CATEGORY_LAUNCHER)
    val resolvedInfos = packageManager.queryIntentActivities(intent, 0)
    
    return resolvedInfos.map { resolveInfo ->
        AppInfo(
            label = resolveInfo.loadLabel(packageManager).toString(),
            packageName = resolveInfo.activityInfo.packageName,
            icon = resolveInfo.loadIcon(packageManager)
        )
    }.sortedBy { it.label } // 名前順でソート
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    name: String,
    icon: Drawable?,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    val bitmap = icon.toBitmap().asImageBitmap()
                    Image(
                        bitmap = bitmap,
                        contentDescription = name,
                        modifier = Modifier.size(24.dp),
                        colorFilter = ColorFilter.tint(LocalCyberColors.current.accent, BlendMode.SrcIn)
                    )
                } else {
                    Text("★", fontSize = 20.sp, color = LocalCyberColors.current.text)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name, 
                    fontFamily = CyberFont, 
                    fontSize = 12.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = LocalCyberColors.current.text, 
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.Red, RoundedCornerShape(10.dp))
                        .clickable { onRemoveClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun BottomDockSection(
    apps: List<AppInfo?>,
    isEditMode: Boolean = false,
    onAddClick: (Int) -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // ドックは最大8個まで
    val maxDockApps = 8
    
    // 横画面の場合はLazyRowではなく通常のRowを使って均等配置する
    if (!isLandscape) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            items(maxDockApps) { index ->
                if (index < apps.size && apps[index] != null) {
                    val appInfo = apps[index]!!
                    DockAppCard(
                        name = appInfo.label,
                        icon = appInfo.icon,
                        modifier = Modifier.fillMaxHeight().aspectRatio(1.8f),
                        isEditMode = isEditMode,
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            }
                        },
                        onLongClick = onLongClick,
                        onRemoveClick = { onRemoveClick(index) }
                    )
                } else {
                    // 空きスロット（タップでアプリ追加）
                    Surface(
                        onClick = { onAddClick(index) },
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, LocalCyberColors.current.border),
                        modifier = Modifier.fillMaxHeight().aspectRatio(1.8f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("EMPTY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            for (index in 0 until maxDockApps) {
                if (index < apps.size && apps[index] != null) {
                    val appInfo = apps[index]!!
                    DockAppCard(
                        name = appInfo.label,
                        icon = appInfo.icon,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                            }
                        },
                        onLongClick = onLongClick,
                        onRemoveClick = { onRemoveClick(index) }
                    )
                } else {
                    // 空きスロット（タップでアプリ追加）
                    Surface(
                        onClick = { onAddClick(index) },
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, LocalCyberColors.current.border),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("EMPTY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockAppCard(
    name: String,
    icon: Drawable?,
    modifier: Modifier = Modifier,
    isEditMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon != null) {
                    val bitmap = icon.toBitmap().asImageBitmap()
                    Image(
                        bitmap = bitmap,
                        contentDescription = name,
                        modifier = Modifier.size(20.dp), // アイコンサイズを少し小さく
                        colorFilter = ColorFilter.tint(LocalCyberColors.current.accent, BlendMode.SrcIn)
                    )
                } else {
                    Text("★", fontSize = 18.sp, color = LocalCyberColors.current.text)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = name, 
                    fontFamily = CyberFont, 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = LocalCyberColors.current.text, 
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(16.dp)
                        .background(Color.Red, RoundedCornerShape(8.dp))
                        .clickable { onRemoveClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun CalendarSection(modifier: Modifier = Modifier) {
    val currentDate = java.time.LocalDate.now()
    val currentMonth = java.time.YearMonth.now()
    val monthString = currentMonth.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase() + " " + currentMonth.year
    
    // カレンダーの計算
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    // getDayOfWeek() は月曜=1, 日曜=7. 日曜始まりにするための計算
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ヘッダー部分
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(8.dp)
                    .background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CALENDAR", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                Text(" // MONTHLY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.weight(1f))
                Text(monthString, fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(0.dp))
            
            // 曜日ヘッダー
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(0.dp))
            
            // カレンダーグリッド
            val totalCells = startDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            
            Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - startDayOfWeek + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val isToday = dayNumber == currentDate.dayOfMonth
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = if (isToday) LocalCyberColors.current.accent else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontFamily = CyberFont,
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isToday) Color.White else LocalCyberColors.current.text
                                        )
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

@Composable
fun DeviceStatusSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    // 定期的に状態を更新するための状態変数
    var trigger by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            delay(5000) // 5秒ごとに更新
            trigger++
        }
    }

    // ストレージ情報の取得
    val statFs = remember(trigger) { StatFs(Environment.getDataDirectory().path) }
    val totalStorageBytes = statFs.totalBytes
    val availableStorageBytes = statFs.availableBytes
    val usedStorageBytes = totalStorageBytes - availableStorageBytes
    
    val totalStorageGB = String.format(Locale.US, "%.1f", totalStorageBytes / (1024.0 * 1024 * 1024))
    val usedStorageGB = String.format(Locale.US, "%.1f", usedStorageBytes / (1024.0 * 1024 * 1024))
    val storageUsageRatio = if (totalStorageBytes > 0) usedStorageBytes.toFloat() / totalStorageBytes.toFloat() else 0f

    // メモリ（RAM）情報の取得
    val activityManager = remember { context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager }
    val memoryInfo = remember(trigger) { ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) } }
    val totalMemBytes = memoryInfo.totalMem
    val availMemBytes = memoryInfo.availMem
    val usedMemBytes = totalMemBytes - availMemBytes
    
    val totalMemGB = String.format(Locale.US, "%.1f", totalMemBytes / (1024.0 * 1024 * 1024))
    val usedMemGB = String.format(Locale.US, "%.1f", usedMemBytes / (1024.0 * 1024 * 1024))
    val memUsageRatio = if (totalMemBytes > 0) usedMemBytes.toFloat() / totalMemBytes.toFloat() else 0f

    // キャッシュクリア風のエフェクト用
    var isOptimizing by remember { mutableStateOf(false) }
    
    LaunchedEffect(isOptimizing) {
        if (isOptimizing) {
            delay(1500) // 最適化中...の演出時間
            trigger++ // 再取得
            isOptimizing = false
        }
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ヘッダー部分
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(6.dp)
                    .background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SYSTEM", fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                Text(" // MONITOR", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween // 隙間を均等に
            ) {
                // ストレージゲージ
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("STORAGE", fontFamily = CyberFont, fontSize = 9.sp, color = LocalCyberColors.current.text, fontWeight = FontWeight.Bold)
                        Text("${usedStorageGB}GB / ${totalStorageGB}GB", fontFamily = CyberFont, fontSize = 9.sp, color = LocalCyberColors.current.text)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp) // ゲージを少し細くして被りを防ぐ
                            .background(LocalCyberColors.current.border, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(storageUsageRatio)
                                .fillMaxHeight()
                                .background(Color(0xFF4DD0E1), RoundedCornerShape(2.dp))
                        )
                    }
                }
                
                // メモリゲージ
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Text("MEMORY", fontFamily = CyberFont, fontSize = 9.sp, color = LocalCyberColors.current.text, fontWeight = FontWeight.Bold)
                        Text("${usedMemGB}GB / ${totalMemGB}GB", fontFamily = CyberFont, fontSize = 9.sp, color = LocalCyberColors.current.text)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp) // ゲージを少し細くして被りを防ぐ
                            .background(LocalCyberColors.current.border, RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(memUsageRatio)
                                .fillMaxHeight()
                                .background(LocalCyberColors.current.core, RoundedCornerShape(2.dp))
                        )
                    }
                }
                
                // 最適化ボタン（横幅いっぱい）
                Surface(
                    onClick = {
                        if (!isOptimizing) {
                            isOptimizing = true
                            System.gc() 
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = if (isOptimizing) LocalCyberColors.current.border else LocalCyberColors.current.accent,
                    modifier = Modifier.fillMaxWidth().height(28.dp) // 高さを細くして被りを防ぐ
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(if (isOptimizing) "⌛" else "⚡", fontSize = 14.sp, color = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isOptimizing) "OPTIMIZING..." else "OPTIMIZE SYSTEM", fontFamily = CyberFont, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun QuickAccessSection(modifier: Modifier = Modifier, onThemeToggle: () -> Unit = {}) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.fillMaxWidth()
    ) {
         Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(6.dp)
                    .background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(8.dp))
                Text("QUICK", fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                Text(" // ACCESS", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // 1段目
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = "SETTINGS", 
                        modifier = Modifier.weight(1f), 
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                    QuickButton(
                        text = "WI-FI", 
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                // 2段目
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = "DISPLAY", 
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                    QuickButton(
                        text = "BLUETOOTH", 
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        }
                    )
                }
                // 3段目
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickButton(
                        text = "DEVELOP", 
                        modifier = Modifier.weight(1f),
                        onClick = {
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
                    )
                    QuickButton(
                        text = "THEME", 
                        modifier = Modifier.weight(1f), 
                        onClick = onThemeToggle
                    )
                }
            }
         }
    }
}

@Composable
fun QuickButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.height(32.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickAccessSectionPortrait(modifier: Modifier = Modifier, onThemeToggle: () -> Unit = {}) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("QUICK", fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
            Spacer(modifier = Modifier.height(8.dp))
            
            QuickButtonPortrait("WIFI", modifier = Modifier.weight(1f), onClick = {
                val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            })
            Spacer(modifier = Modifier.height(8.dp))
            QuickButtonPortrait("DISP", modifier = Modifier.weight(1f), onClick = {
                val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                context.startActivity(intent)
            })
            Spacer(modifier = Modifier.height(8.dp))
            QuickButtonPortrait("DEV", modifier = Modifier.weight(1f), onClick = {
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    val fallbackIntent = Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                    context.startActivity(fallbackIntent)
                }
            })
            Spacer(modifier = Modifier.height(8.dp))
            QuickButtonPortrait("THEME", modifier = Modifier.weight(1f), onClick = onThemeToggle)
        }
    }
}

@Composable
fun QuickButtonPortrait(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, device = "spec:width=1280dp,height=800dp,dpi=240,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun LauncherPreview() {
    GirdLauncherTheme {
        CyberLauncherScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorDialog(allApps: List<AppInfo>, onDismiss: () -> Unit, onAppSelected: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalCyberColors.current.bg,
        modifier = Modifier.fillMaxHeight(0.95f) // AllAppsDrawerと同じ高さに
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // ヘッダー（タイトル）
            Text("SELECT APP", fontFamily = CyberFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.accent)
            Spacer(modifier = Modifier.height(16.dp))

            // 検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("SEARCH APPS...", fontFamily = CyberFont, fontSize = 14.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f)) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CyberFont, color = LocalCyberColors.current.text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalCyberColors.current.accent,
                    unfocusedBorderColor = LocalCyberColors.current.border,
                    cursorColor = LocalCyberColors.current.accent
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            
            // アプリ一覧グリッド (AllAppsDrawerと同じデザイン)
            val configuration = LocalConfiguration.current
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isPortrait) 3 else 5), // 縦画面なら3列、横画面なら5列
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps) { appInfo ->
                    AppCard(
                        name = appInfo.label,
                        icon = appInfo.icon,
                        modifier = Modifier.aspectRatio(2.5f), // ACCESS GRIDの比率に近い形
                        onClick = {
                            onAppSelected(appInfo.packageName)
                        }
                    )
                }
            }
        }
    }
}

// 直近1週間でよく使われたアプリの上位8個を取得する関数
@android.annotation.SuppressLint("MissingPermission")
fun getFrequentApps(context: Context, allApps: List<AppInfo>): List<AppInfo> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = java.util.Calendar.getInstance()
    val endTime = calendar.timeInMillis
    calendar.add(java.util.Calendar.DAY_OF_YEAR, -7)
    val startTime = calendar.timeInMillis

    val usageStatsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, startTime, endTime)
    
    // パッケージ名ごとの使用時間を集計
    val usageMap = usageStatsList.associateBy({ it.packageName }, { it.totalTimeInForeground })
    
    return allApps
        .map { app -> Pair(app, usageMap[app.packageName] ?: 0L) }
        .filter { it.second > 0L } // 少しでも使われた形跡があるもの
        .sortedByDescending { it.second } // 使用時間が長い順
        .take(8) // 上位8個
        .map { it.first }
}

// UsageStats（使用状況アクセス権限）が許可されているかチェック
fun hasUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsDrawer(allApps: List<AppInfo>, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalCyberColors.current.bg,
        modifier = Modifier.fillMaxHeight(0.95f) // 画面の95%の高さまで表示
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // 検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("SEARCH APPS...", fontFamily = CyberFont, fontSize = 14.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f)) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CyberFont, color = LocalCyberColors.current.text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalCyberColors.current.accent,
                    unfocusedBorderColor = LocalCyberColors.current.border,
                    cursorColor = LocalCyberColors.current.accent
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // よく使うアプリ（検索していないときのみ表示）
            if (searchQuery.isEmpty()) {
                val hasPermission = remember { hasUsageStatsPermission(context) }
                
                if (hasPermission) {
                    val frequentApps = remember { getFrequentApps(context, allApps) }
                    
                    if (frequentApps.isNotEmpty()) {
                        Text("FREQUENT APPS", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.accent)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) {
                            lazyListItems(frequentApps) { appInfo ->
                                DockAppCard(
                                    name = appInfo.label,
                                    icon = appInfo.icon,
                                    modifier = Modifier.width(80.dp).fillMaxHeight(), // 幅を80dpに固定して統一
                                    onClick = {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                            onDismiss()
                                        }
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // 権限がない場合は、権限設定画面へのボタンを表示
                    Surface(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, LocalCyberColors.current.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("⚠️ REQUIRES USAGE ACCESS TO SHOW FREQUENT APPS", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 全アプリのグリッド
            Text("ALL APPS // NODES", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.accent)
            Spacer(modifier = Modifier.height(8.dp))
            val configuration = LocalConfiguration.current
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isPortrait) 3 else 5), // 縦画面なら3列、横画面なら5列
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps) { appInfo ->
                    AppCard(
                        name = appInfo.label,
                        icon = appInfo.icon,
                        modifier = Modifier.aspectRatio(2.5f), // ACCESS GRIDの比率に近い形
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                                onDismiss()
                            }
                        }
                    )
                }
            }
        }
    }
}
