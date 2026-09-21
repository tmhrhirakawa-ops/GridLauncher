package com.example.girdlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * 縦画面用に特別にスタイル設定された、クイックアクセスアクション用のボタン。
 *
 * @param text ボタンに表示するテキスト。
 * @param modifier レイアウトに適用するModifier。
 * @param onClick ボタンがクリックされたときに呼び出されるコールバック。
 */
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
            Text(
                text = text, 
                fontFamily = CyberFont, 
                fontSize = 10.sp, 
                color = LocalCyberColors.current.accent, 
                fontWeight = FontWeight.Bold
            )
        }
    }
}
