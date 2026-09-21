package com.example.girdlauncher.ui.sections

import android.content.Intent
import androidx.compose.foundation.BorderStroke
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
import com.example.girdlauncher.ui.components.QuickButtonPortrait
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * 縦画面用のクイックアクセスセクション。
 *
 * @param modifier レイアウトに適用するModifier。
 * @param onThemeToggle テーマ切り替えボタンがクリックされたときのコールバック。
 */
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
