package com.example.girdlauncher.ui.sections

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * ストレージとメモリの使用状況など、デバイスのステータスを表示するセクション。
 *
 * @param modifier レイアウトに適用するModifier。
 */
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
                    .background(LocalCyberColors.current.accent)
                )
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
