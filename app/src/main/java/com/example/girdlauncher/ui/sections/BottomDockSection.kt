package com.example.girdlauncher.ui.sections

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.AppInfo
import com.example.girdlauncher.ui.components.DockAppCard
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/** 縦画面のドックで1ページに表示するスロット数。この数ぴったりで折り返してページ送りする。 */
private const val SlotsPerPage = 4

/**
 * よく使うアプリを表示するボトムドックセクション。
 *
 * 縦画面では、[SlotsPerPage]個ぴったりが画面内に収まるサイズでスロットを均等配置し、
 * それを超える分はページとして横にスワイプ（スナップ）して切り替える
 * （無段階の自由スクロールにはしない）。
 *
 * @param apps 表示するアプリのリスト。
 * @param isEditMode UIが編集モードかどうか。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param activeNotifications 通知（またはアプリバッジ）が来ているアプリのパッケージ名と件数のマップ。
 * @param onAddClick 空きスロットがクリックされたときのコールバック。
 * @param onLongClick アプリが長押しされたときのコールバック。
 * @param onRemoveClick 削除アイコンがクリックされたときのコールバック。
 * @param onExitEditMode 編集モード中に削除アイコン以外の部分がタップされたときのコールバック。
 */
@Composable
fun BottomDockSection(
    apps: List<AppInfo?>,
    isEditMode: Boolean = false,
    isWallpaperMode: Boolean = false,
    activeNotifications: Map<String, Int> = emptyMap(),
    onAddClick: (Int) -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: (Int) -> Unit = {},
    onExitEditMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    
    // ドックは最大8個まで
    val maxDockApps = 8
    
    // 縦画面：SlotsPerPage個ぴったりが画面幅に収まる均等サイズで並べ、それを超える分は
    // ページ送り（スワイプでスナップ）にする（無段階スクロールにはしない）
    if (!isLandscape) {
        val pageCount = maxOf(1, (maxDockApps + SlotsPerPage - 1) / SlotsPerPage)
        val pagerState = rememberPagerState(pageCount = { pageCount })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) { page ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                for (col in 0 until SlotsPerPage) {
                    val index = page * SlotsPerPage + col
                    if (index < apps.size && apps[index] != null) {
                        val appInfo = apps[index]!!
                        val notifCount = activeNotifications[appInfo.packageName] ?: 0
                        DockAppCard(
                            name = appInfo.label,
                            packageName = appInfo.packageName,
                            isMonochrome = appInfo.iconIsMonochrome,
                            icon = appInfo.icon,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            isEditMode = isEditMode,
                            notificationCount = notifCount,
                            isWallpaperMode = isWallpaperMode,
                            onClick = {
                                if (isEditMode) {
                                    onExitEditMode()
                                } else {
                                    val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                    launchIntent?.let {
                                        context.startActivity(it)
                                    }
                                }
                            },
                            onLongClick = onLongClick,
                            onRemoveClick = { onRemoveClick(index) }
                        )
                    } else {
                        // 空きスロット（タップでアプリ追加）
                        Surface(
                            onClick = { if (isEditMode) onExitEditMode() else onAddClick(index) },
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, LocalCyberColors.current.border),
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("EMPTY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
                            }
                        }
                    }
                }
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            for (index in 0 until maxDockApps) {
                if (index < apps.size && apps[index] != null) {
                    val appInfo = apps[index]!!
                    val notifCount = activeNotifications[appInfo.packageName] ?: 0
                    DockAppCard(
                        name = appInfo.label,
                        packageName = appInfo.packageName,
                        isMonochrome = appInfo.iconIsMonochrome,
                        icon = appInfo.icon,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isEditMode = isEditMode,
                        notificationCount = notifCount,
                        isWallpaperMode = isWallpaperMode,
                        onClick = {
                            if (isEditMode) {
                                onExitEditMode()
                            } else {
                                val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                launchIntent?.let {
                                    context.startActivity(it)
                                }
                            }
                        },
                        onLongClick = onLongClick,
                        onRemoveClick = { onRemoveClick(index) }
                    )
                } else {
                    // 空きスロット（タップでアプリ追加）
                    Surface(
                        onClick = { if (isEditMode) onExitEditMode() else onAddClick(index) },
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, LocalCyberColors.current.border),
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("EMPTY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}
