package com.example.girdlauncher.ui.sections

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
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
    var trigger by remember { mutableIntStateOf(0) }
    
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
    // 最適化で解放できたメモリ量（バイト）。nullの間は結果表示を隠す
    var optimizeFreedBytes by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(isOptimizing) {
        if (isOptimizing) {
            val before = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.availMem
            System.gc()
            delay(1500) // 最適化中...の演出時間
            System.gc()
            val after = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }.availMem

            trigger++ // ゲージを再取得
            optimizeFreedBytes = after - before
            isOptimizing = false
        }
    }

    // 結果表示を数秒後に自動で消す
    LaunchedEffect(optimizeFreedBytes) {
        if (optimizeFreedBytes != null) {
            delay(3000)
            optimizeFreedBytes = null
        }
    }

    // 最適化中の演出用アニメーション
    val infiniteTransition = rememberInfiniteTransition(label = "optimizeAnim")
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(900, easing = LinearEasing)),
        label = "iconRotation"
    )
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(animation = tween(1100, easing = LinearEasing)),
        label = "scanProgress"
    )

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
                            optimizeFreedBytes = null
                            isOptimizing = true
                        }
                    },
                    shape = RoundedCornerShape(4.dp),
                    color = if (isOptimizing) LocalCyberColors.current.border else LocalCyberColors.current.accent,
                    modifier = Modifier.fillMaxWidth().height(28.dp) // 高さを細くして被りを防ぐ
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        if (isOptimizing) {
                            // スキャンしているような光の帯が横切るエフェクト
                            Box(
                                modifier = Modifier
                                    .width(maxWidth * 0.4f)
                                    .fillMaxHeight()
                                    .offset { IntOffset((maxWidth * scanProgress).roundToPx(), 0) }
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.White.copy(alpha = 0.45f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (isOptimizing) "⚙" else "⚡",
                                fontSize = 14.sp,
                                color = Color.White,
                                modifier = if (isOptimizing) {
                                    Modifier.graphicsLayer { rotationZ = iconRotation }
                                } else {
                                    Modifier
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isOptimizing) "OPTIMIZING..." else "OPTIMIZE SYSTEM", fontFamily = CyberFont, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // 最適化結果の表示（数秒でフェードアウト）
                AnimatedVisibility(
                    visible = optimizeFreedBytes != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    val freedMB = (optimizeFreedBytes ?: 0L) / (1024 * 1024)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (freedMB > 0) "✔ ${freedMB}MB FREED" else "✔ SYSTEM OPTIMAL",
                            fontFamily = CyberFont,
                            fontSize = 9.sp,
                            color = LocalCyberColors.current.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
