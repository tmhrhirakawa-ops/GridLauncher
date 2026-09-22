package com.example.girdlauncher.ui.sections

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import com.example.girdlauncher.model.GridItem
import com.example.girdlauncher.ui.components.AppCard
import com.example.girdlauncher.ui.components.FolderCard
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import androidx.compose.ui.text.font.FontWeight

/**
 * アプリアイコン・フォルダのグリッドを表示するセクション。
 *
 * @param items 表示するスロットの中身のリスト（アプリ・フォルダ・null=空きスロット）。
 * @param columns グリッドの列数。
 * @param rows グリッドの行数。
 * @param isPortrait デバイスの向きが縦（ポートレート）かどうか。
 * @param isEditMode UIが編集モードかどうか。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param activeNotifications 通知（またはアプリバッジ）が来ているアプリのパッケージ名と件数のマップ。
 * @param openFolderId 現在ポップアップで開いているフォルダのID。該当するフォルダのカードは、
 *   ポップアップへ拡大するアニメーション（共有要素）のため見た目を隠す。
 * @param onAddClick 空きスロットがクリックされたときのコールバック。
 * @param onFolderClick フォルダがクリックされたとき（編集モードでない場合）のコールバック。
 * @param onLongClick アプリ・フォルダが長押しされたときのコールバック。
 * @param onRemoveClick 削除アイコンがクリックされたときのコールバック。
 * @param onExitEditMode 編集モード中に削除アイコン以外の部分がタップされたときのコールバック。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccessGridSection(
    items: List<GridItem?>,
    columns: Int,
    rows: Int,
    isPortrait: Boolean = false,
    isEditMode: Boolean = false,
    isWallpaperMode: Boolean = false,
    activeNotifications: Map<String, Int> = emptyMap(),
    openFolderId: String? = null,
    showBorder: Boolean = true,
    onAddClick: (Int) -> Unit,
    onFolderClick: (GridItem.FolderItem) -> Unit = {},
    onLongClick: () -> Unit = {},
    onRemoveClick: (Int) -> Unit = {},
    onExitEditMode: () -> Unit = {}
) {
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (showBorder) LocalCyberColors.current.panel.copy(alpha = 0.5f) else Color.Transparent,
        border = if (showBorder) BorderStroke(1.dp, LocalCyberColors.current.border) else null,
        modifier = Modifier.fillMaxSize()
    ) {
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(8.dp)
                .background(LocalCyberColors.current.accent))
            Spacer(modifier = Modifier.width(8.dp))
            Text("APP LIST", fontFamily = CyberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
            Text(" // APP NODES", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Normal, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.height(6.dp))

        // アプリ・フォルダを指定された行数・列数で分割
        val pageSize = columns * rows
        // 少なくとも1ページ分は空きスロットを表示する
        val pageCount = maxOf(1, ((items.size + 1) / pageSize) + if (((items.size + 1) % pageSize) == 0) 0 else 1)
        val pagerState = rememberPagerState(pageCount = { pageCount })

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val startIndex = page * pageSize
            val pageItems = items.asSequence().drop(startIndex).take(pageSize).toList()

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
                            val itemIndex = rowIndex + (colIndex * rows) // 縦埋めから横埋めに変更が必要な場合はここを修正
                            val globalIndex = startIndex + itemIndex
                            val item = pageItems.getOrNull(itemIndex)

                            when (item) {
                                is GridItem.AppItem -> {
                                    val appInfo = item.appInfo
                                    val notifCount = activeNotifications[appInfo.packageName] ?: 0
                                    AppCard(
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
                                        onRemoveClick = { onRemoveClick(globalIndex) }
                                    )
                                }
                                is GridItem.FolderItem -> {
                                    FolderCard(
                                        name = item.folder.name,
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .sharedElementWithCallerManagedVisibility(
                                                rememberSharedContentState(key = item.folder.id),
                                                visible = item.folder.id != openFolderId
                                            ),
                                        isEditMode = isEditMode,
                                        isWallpaperMode = isWallpaperMode,
                                        onClick = {
                                            if (isEditMode) onExitEditMode() else onFolderClick(item)
                                        },
                                        onLongClick = onLongClick,
                                        onRemoveClick = { onRemoveClick(globalIndex) }
                                    )
                                }
                                null -> {
                                    // 空きスロット（タップでアプリ追加。編集モード中は編集モード終了のみ）
                                    Surface(
                                        onClick = { if (isEditMode) onExitEditMode() else onAddClick(globalIndex) },
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
    }
}
