package com.example.girdlauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * 編集モードでスロットの右上に表示する削除バッジ。
 *
 * @param onClick タップされたときのコールバック。
 * @param modifier レイアウトに適用するModifier。
 * @param size バッジ全体のサイズ。
 */
@Composable
fun RemoveBadge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    val dangerColor = Color(0xFFFF3B4E)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(LocalCyberColors.current.bg.copy(alpha = 0.92f))
            .border(1.dp, dangerColor, CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Remove" },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = null,
            tint = dangerColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
