package com.example.girdlauncher.ui.sections

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.components.QuickButton
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * デバイスの様々な設定にアクセスするためのクイックアクセスボタンを提供するセクション。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param onThemeToggle テーマ切り替えボタンがクリックされたときのコールバック。
 */
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
