package com.example.gridlauncher.ui.sections

import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.components.CoreMenuPopup
import com.example.gridlauncher.ui.components.NowPlayingWidget
import com.example.gridlauncher.ui.components.PermissionRationaleDialog
import com.example.gridlauncher.ui.components.WidgetBorderSettingsDialog
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.CyberNotificationListener
import com.example.gridlauncher.util.openPowerMenuOrRequestPermission
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 縦画面用のヘッダーセクション。時刻やバッテリーのステータスを表示します。
 *
 * @param nowPlaying 現在再生中のメディア情報。nullの場合は何も表示しない。
 * @param isLarge 縦画面（大）かどうか。trueの場合、中央に「MAIN TERMINAL」の表記を追加する。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onWallpaperToggle 壁紙透過切り替えボタンがクリックされたときのコールバック。
 * @param hiddenPanels 枠線を非表示にしているウィジェットの集合。
 * @param onToggleAllBorders 枠線切り替えボタンがタップされたときのコールバック（全ウィジェット一括切り替え）。
 * @param onTogglePanelBorder 枠線切り替えボタンの長押しメニューで、個別のウィジェットが切り替えられたときのコールバック。
 */
@Composable
fun HeaderSectionPortrait(
    nowPlaying: CyberNotificationListener.NowPlayingInfo? = null,
    isLarge: Boolean = false,
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

    // NowPlayingの消滅アニメーション中も直前の内容を表示し続けるため、nullになった後も
    // 直前の非nullの値を保持しておく（フォルダを閉じるときのdisplayedFolderと同じパターン）
    var displayedNowPlaying by remember { mutableStateOf(nowPlaying) }
    LaunchedEffect(nowPlaying) {
        if (nowPlaying != null) {
            displayedNowPlaying = nowPlaying
        }
    }

    // NowPlayingの表示/非表示は、AnimatedVisibilityのshrink/expand（レイアウト幅そのものを
    // 変える方式）ではなく、graphicsLayerのscaleXで描画だけを縮める方式にしている。
    // 幅を変える方式だと、右隣のバッテリー表示に合わせてRow全体が右詰めで再配置されるため、
    // 右端が固定されたまま左端だけが動く「右への一方通行」に見えてしまう。scaleXなら
    // レイアウト上のサイズは変えず見た目だけを縮めるので、周りの表示位置を動かさずに
    // その場（中心）から左右へ均等に縮んで消える
    var keepNowPlayingInLayout by remember { mutableStateOf(nowPlaying != null) }
    LaunchedEffect(nowPlaying != null) {
        if (nowPlaying != null) keepNowPlayingInLayout = true
    }
    val nowPlayingScale by animateFloatAsState(
        targetValue = if (nowPlaying != null) 1f else 0f,
        animationSpec = tween(durationMillis = 220),
        label = "nowPlayingScale",
        finishedListener = { value -> if (value == 0f) keepNowPlayingInLayout = false }
    )

    // リアルタイム時計とバッテリーの状態管理
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var batteryLevel by remember { mutableIntStateOf(100) }
    
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

    // SpaceBetweenのRowだと、左側カラムの残り幅を吸収する形になり中央のMAIN TERMINALが
    // 真ん中に来ないため、Box+align(Alignment.Center)で画面幅全体の中央に配置する。
    Box(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.align(Alignment.TopStart)) {
            Text(timeString, fontFamily = CyberFont, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(6.dp))
                Text(dateString, fontFamily = CyberFont, fontSize = 12.sp, color = LocalCyberColors.current.text.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 縦画面（大）のみ、中央に「MAIN TERMINAL」の表記を追加する
        // NowPlaying表示中は右側の表示と被らないよう、中央より少し左に寄せる
        if (isLarge) {
            val terminalAlignment = if (keepNowPlayingInLayout) BiasAlignment(-0.4f, 0f) else Alignment.Center
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(terminalAlignment)
            ) {
                Text("SYSTEM ONLINE", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
                Text("MAIN TERMINAL", fontFamily = CyberFont, fontSize = 24.sp, fontWeight = FontWeight.Black, color = LocalCyberColors.current.text, letterSpacing = 2.sp)
                Text("TOKYO // MAIN TERMINAL", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            // 再生中のメディアがあれば、バッテリー表示の左側に表示する。
            // 消えるときはその場で左右から中央へ縮むように消滅させる
            if (keepNowPlayingInLayout) {
                displayedNowPlaying?.let { info ->
                    NowPlayingWidget(
                        info = info,
                        compact = true,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = nowPlayingScale
                                alpha = nowPlayingScale
                            }
                            .padding(end = 8.dp)
                    )
                }
            }

            // 縦画面は右上に青いコア（Core）とバッテリーを配置
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Spacer(modifier = Modifier.height(8.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
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
                                .size(30.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("BATTERY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.core, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("$batteryLevel%", fontFamily = CyberFont, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text, modifier = Modifier.padding(top = 4.dp))
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
