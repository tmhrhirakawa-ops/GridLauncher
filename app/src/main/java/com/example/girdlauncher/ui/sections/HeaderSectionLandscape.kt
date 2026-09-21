package com.example.girdlauncher.ui.sections

import android.content.Context
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton

/**
 * 横画面用のヘッダーセクション。時刻やバッテリーのステータスを表示します。
 */
@Composable
fun HeaderSectionLandscape(
    isWallpaperMode: Boolean = false,
    onWallpaperToggle: () -> Unit = {}
) {
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
            // 壁紙切り替えボタン
            IconButton(onClick = onWallpaperToggle) {
                Icon(
                    imageVector = if (isWallpaperMode) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = "Toggle Wallpaper",
                    tint = LocalCyberColors.current.accent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            
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
