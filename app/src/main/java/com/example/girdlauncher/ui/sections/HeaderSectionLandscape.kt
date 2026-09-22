package com.example.girdlauncher.ui.sections

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.WidgetPanel
import com.example.girdlauncher.ui.components.CoreMenuPopup
import com.example.girdlauncher.ui.components.NowPlayingWidget
import com.example.girdlauncher.ui.components.PermissionRationaleDialog
import com.example.girdlauncher.ui.components.WidgetBorderSettingsDialog
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.CyberNotificationListener
import com.example.girdlauncher.util.openPowerMenuOrRequestPermission
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 横画面用のヘッダーセクション。時刻やバッテリーのステータスを表示します。
 *
 * @param nowPlaying 現在再生中のメディア情報。nullの場合は何も表示しない。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onWallpaperToggle 壁紙透過切り替えボタンがクリックされたときのコールバック。
 * @param hiddenPanels 枠線を非表示にしているウィジェットの集合。
 * @param onToggleAllBorders 枠線切り替えボタンがタップされたときのコールバック（全ウィジェット一括切り替え）。
 * @param onTogglePanelBorder 枠線切り替えボタンの長押しメニューで、個別のウィジェットが切り替えられたときのコールバック。
 */
@Composable
fun HeaderSectionLandscape(
    nowPlaying: CyberNotificationListener.NowPlayingInfo? = null,
    isWallpaperMode: Boolean = false,
    onWallpaperToggle: () -> Unit = {},
    hiddenPanels: Set<WidgetPanel> = emptySet(),
    onToggleAllBorders: () -> Unit = {},
    onTogglePanelBorder: (WidgetPanel) -> Unit = {}
) {
    val context = LocalContext.current
    var showCoreMenu by remember { mutableStateOf(false) }
    var showBorderSettings by remember { mutableStateOf(false) }
    var showPowerPermissionRationale by remember { mutableStateOf(false) }
    
    // リアルタイム時計とバッテリーの状態管理
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryLevel by remember { mutableIntStateOf(100) }
    
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
            if (nowPlaying != null) {
                NowPlayingWidget(info = nowPlaying)
                Spacer(modifier = Modifier.width(16.dp))
            }
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
                // 真ん中の青い歯車（タップすると壁紙透過・枠線切り替えメニューがにゅいっと出てくる）
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Menu",
                        tint = LocalCyberColors.current.core,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { showCoreMenu = true }
                    )
                    if (showCoreMenu) {
                        CoreMenuPopup(
                            onOpenSettings = {
                                showCoreMenu = false
                                val intent = Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            },
                            isWallpaperMode = isWallpaperMode,
                            onWallpaperToggle = onWallpaperToggle,
                            bordersVisible = hiddenPanels.isEmpty(),
                            onToggleAllBorders = onToggleAllBorders,
                            onLongPressBorderToggle = {
                                showCoreMenu = false
                                showBorderSettings = true
                            },
                            onOpenPowerMenu = {
                                showCoreMenu = false
                                openPowerMenuOrRequestPermission(context) {
                                    showPowerPermissionRationale = true
                                }
                            },
                            onDismiss = { showCoreMenu = false }
                        )
                    }
                }
            }
        }
    }

    if (showBorderSettings) {
        WidgetBorderSettingsDialog(
            hiddenPanels = hiddenPanels,
            onTogglePanel = onTogglePanelBorder,
            onDismiss = { showBorderSettings = false }
        )
    }

    if (showPowerPermissionRationale) {
        PermissionRationaleDialog(
            message = "電源メニュー（電源を切る/再起動）を開くには、GirdLauncherのアクセシビリティサービスを有効にしてください。",
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
