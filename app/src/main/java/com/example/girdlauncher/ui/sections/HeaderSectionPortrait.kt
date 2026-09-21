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
 * 縦画面用のヘッダーセクション。時刻やバッテリーのステータスを表示します。
 */
@Composable
fun HeaderSectionPortrait(
    isWallpaperMode: Boolean = false,
    onWallpaperToggle: () -> Unit = {}
) {
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
                // 壁紙切り替えボタン
                IconButton(onClick = onWallpaperToggle, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (isWallpaperMode) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = "Toggle Wallpaper",
                        tint = LocalCyberColors.current.accent
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                
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
