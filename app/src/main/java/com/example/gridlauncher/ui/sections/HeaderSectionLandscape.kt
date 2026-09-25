package com.example.gridlauncher.ui.sections

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.components.NowPlayingWidget
import com.example.gridlauncher.ui.components.rememberNowPlayingDisplayState
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.CyberNotificationListener
import com.example.gridlauncher.util.rememberBatteryLevel
import com.example.gridlauncher.util.rememberCurrentTimeMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 横画面用のヘッダーセクション。時刻やバッテリーのステータスを表示します。
 *
 * @param nowPlaying 現在再生中のメディア情報。nullの場合は何も表示しない。
 * @param onCoreClick バッテリーコア（歯車アイコン）がタップされたときのコールバック（カスタマイズ画面を開く）。
 */
@Composable
fun HeaderSectionLandscape(
    nowPlaying: CyberNotificationListener.NowPlayingInfo? = null,
    onCoreClick: () -> Unit = {}
) {
    // 再生中メディアの表示状態（消えるときのアニメーション中も直前の内容を表示し続ける）
    val nowPlayingDisplay = rememberNowPlayingDisplayState(nowPlaying)

    // 時刻（分単位）とバッテリー残量。ポーリングせずシステムのブロードキャストで更新し、
    // ホーム画面が見えている間だけ受信する
    val currentTime = rememberCurrentTimeMillis()
    val batteryLevel = rememberBatteryLevel()

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("MMM dd // EEE", Locale.ENGLISH) } // 例: SEP 13 // SUN
    
    val timeString = timeFormat.format(Date(currentTime))
    val dateString = dateFormat.format(Date(currentTime)).uppercase()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(timeString, fontFamily = CyberFont, fontSize = 42.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(6.dp))
                Text(dateString, fontFamily = CyberFont, fontSize = 12.sp, color = LocalCyberColors.current.text.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("SYSTEM ONLINE", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
            Text("MAIN TERMINAL", fontFamily = CyberFont, fontSize = 24.sp, fontWeight = FontWeight.Black, color = LocalCyberColors.current.text, letterSpacing = 2.sp)
            Text("TOKYO // MAIN TERMINAL", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
        }
        
        // バッテリー残量とシステムステータスのUI
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 消えるときはその場で左右から中央へ縮むように消滅させる
            if (nowPlayingDisplay.keepInLayout) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.graphicsLayer {
                        scaleX = nowPlayingDisplay.scale
                        alpha = nowPlayingDisplay.scale
                    }
                ) {
                    nowPlayingDisplay.displayed?.let { info -> NowPlayingWidget(info = info) }
                    Spacer(modifier = Modifier.width(16.dp))
                }
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
                // 真ん中の青い歯車（タップするとカスタマイズ画面がボトムシートで開く）
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Menu",
                        tint = LocalCyberColors.current.core,
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(onClick = onCoreClick)
                    )
                }
            }
        }
    }
}

