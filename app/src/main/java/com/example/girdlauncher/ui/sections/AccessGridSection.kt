package com.example.girdlauncher.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.AppInfo
import com.example.girdlauncher.ui.components.AppCard
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import androidx.compose.ui.text.font.FontWeight

/**
 * アプリアイコンのグリッドを表示するセクション。
 *
 * @param apps 表示するアプリのリスト。
 * @param columns グリッドの列数。
 * @param rows グリッドの行数。
 * @param isPortrait デバイスの向きが縦（ポートレート）かどうか。
 * @param isEditMode UIが編集モードかどうか。
 * @param activeNotifications 通知が来ているアプリのパッケージ名のセット。
 * @param onAddClick 空きスロットがクリックされたときのコールバック。
 * @param onLongClick アプリが長押しされたときのコールバック。
 * @param onRemoveClick 削除アイコンがクリックされたときのコールバック。
 */
@Composable
fun AccessGridSection(
    apps: List<AppInfo?>,
    columns: Int,
    rows: Int,
    isPortrait: Boolean = false,
    isEditMode: Boolean = false,
    isWallpaperMode: Boolean = false,
    activeNotifications: Set<String> = emptySet(),
    onAddClick: (Int) -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(8.dp)
                .background(LocalCyberColors.current.accent))
            Spacer(modifier = Modifier.width(8.dp))
            if (isPortrait) {
                Text("N.E.P.T // COVER TERMINAL", fontFamily = CyberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
            } else {
                Text("ACCESS GRID", fontFamily = CyberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                Text(" // APP NODES", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        // アプリを指定された行数・列数で分割
        val pageSize = columns * rows
        // 少なくとも1ページ分は空きスロットを表示する
        val pageCount = maxOf(1, ((apps.size + 1) / pageSize) + if (((apps.size + 1) % pageSize) == 0) 0 else 1)
        val pagerState = rememberPagerState(pageCount = { pageCount })
        
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val startIndex = page * pageSize
            val pageApps = apps.asSequence().drop(startIndex).take(pageSize).toList()
            
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 行のループ
                for (rowIndex in 0 until rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 列のループ
                        for (colIndex in 0 until columns) {
                            val appIndex = rowIndex + (colIndex * rows) // 縦埋めから横埋めに変更が必要な場合はここを修正
                            val globalIndex = startIndex + appIndex
                            
                            if (appIndex < pageApps.size && pageApps[appIndex] != null) {
                                val appInfo = pageApps[appIndex]!!
                                val hasNotif = activeNotifications.contains(appInfo.packageName)
                                AppCard(
                                    name = appInfo.label,
                                    packageName = appInfo.packageName,
                                    icon = appInfo.icon,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    isEditMode = isEditMode,
                                    hasNotification = hasNotif,
                                    isWallpaperMode = isWallpaperMode,
                                    onClick = {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                        launchIntent?.let {
                                            context.startActivity(it)
                                        }
                                    },
                                    onLongClick = onLongClick,
                                    onRemoveClick = { onRemoveClick(globalIndex) }
                                )
                            } else {
                                // 空きスロット（タップでアプリ追加）
                                Surface(
                                    onClick = { onAddClick(globalIndex) },
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, LocalCyberColors.current.border),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("EMPTY SLOT", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
