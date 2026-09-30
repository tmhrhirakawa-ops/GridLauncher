package com.example.gridlauncher.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.AppSortKey
import com.example.gridlauncher.util.AppSortOrder
import com.example.gridlauncher.util.loadAppSortOrder
import com.example.gridlauncher.util.saveAppSortOrder

/**
 * 保存しているアプリ一覧の並べ替え（ALL APPS・SELECT APPで共通）と、それを変えて保存する関数を返す。
 */
@Composable
fun rememberAppSortOrder(): Pair<AppSortOrder, (AppSortOrder) -> Unit> {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE) }
    var order by remember { mutableStateOf(loadAppSortOrder(prefs)) }
    return order to { newOrder ->
        order = newOrder
        saveAppSortOrder(prefs, newOrder)
    }
}

/**
 * ALL APPS・SELECT APPのアプリ一覧の並べ替えボタン。タップすると、並べ替えの種類
 * （インストール順・名前順・カテゴリ順）と、昇順・降順を選ぶメニューを開く。
 *
 * @param order 今の並べ替え。
 * @param onOrderChange 並べ替えが選ばれたときのコールバック。
 */
@Composable
fun AppSortButton(order: AppSortOrder, onOrderChange: (AppSortOrder) -> Unit) {
    val colors = LocalCyberColors.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable { expanded = true }
        ) {
            Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = "Sort", tint = colors.accent, modifier = Modifier.size(24.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.panel
        ) {
            MenuSectionLabel("並べ替え")
            AppSortKey.entries.forEach { key ->
                SortMenuOption(label = key.label, selected = key == order.key) {
                    onOrderChange(order.copy(key = key))
                }
            }
            Box(modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(colors.border))
            MenuSectionLabel("順番")
            SortMenuOption(label = "昇順（${order.key.ascendingLabel}）", selected = !order.descending) {
                onOrderChange(order.copy(descending = false))
            }
            SortMenuOption(label = "降順（${order.key.descendingLabel}）", selected = order.descending) {
                onOrderChange(order.copy(descending = true))
            }
        }
    }
}

@Composable
private fun MenuSectionLabel(text: String) {
    Text(
        text,
        fontFamily = CyberFont,
        fontSize = 10.sp,
        color = LocalCyberColors.current.text.copy(alpha = 0.5f),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

/** メニューの選択肢1つ分（選ばれているものは印とアクセントカラーで示す）。選んでもメニューは閉じない。 */
@Composable
private fun SortMenuOption(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            label,
            fontFamily = CyberFont,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) colors.accent else colors.text
        )
    }
}
