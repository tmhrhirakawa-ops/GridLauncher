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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.GridItem
import com.example.girdlauncher.ui.components.AppCard
import com.example.girdlauncher.ui.components.FolderCard
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import androidx.compose.ui.text.font.FontWeight

/** スロットがこれより狭くなるとアプリ名が読めなくなるとみなす、1スロットの最小幅。 */
private val MinSlotWidth = 108.dp

/** スロットがこれより低くなると窮屈になるとみなす、1スロットの最小高さ。 */
private val MinSlotHeight = 56.dp

/** スロットがこれより広くなると余白が間延びして見えるとみなす、1スロットの最大幅。 */
private val MaxSlotWidth = 180.dp

/** スロットがこれより高くなると余白が間延びして見えるとみなす、1スロットの最大高さ。 */
private val MaxSlotHeight = 72.dp

/** スロット間の余白。列数・行数の算出にもこの値を使う。 */
private val SlotSpacing = 12.dp

/**
 * アプリアイコン・フォルダのグリッドを表示するセクション。
 *
 * 列数・行数は[baseColumns]・[baseRows]を基準に、ウィジェットの実際の描画サイズに応じて
 * 自動的に決まる。スロットが[MinSlotWidth]・[MinSlotHeight]を下回りそうなほど狭くなったときは
 * 列数・行数を減らしてスロット自体のサイズを確保し（アイコンだけの縮退表示にはせず、表示する
 * アプリの数を減らすことで読みやすさを保つ）、逆に[MaxSlotWidth]・[MaxSlotHeight]を超えて
 * 間延びしそうなほど広くなったときは列数・行数を増やして余白を詰める。
 *
 * @param items 表示するスロットの中身のリスト（アプリ・フォルダ・null=空きスロット）。
 * @param baseColumns グリッドの基準列数（ウィジェットのサイズがちょうどよいときに使う列数）。
 * @param baseRows グリッドの基準行数（ウィジェットのサイズがちょうどよいときに使う行数）。
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
    baseColumns: Int,
    baseRows: Int,
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
            Spacer(modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns = adaptiveSlotCount(maxWidth, baseColumns, MinSlotWidth, MaxSlotWidth, SlotSpacing)
            val rows = adaptiveSlotCount(maxHeight, baseRows, MinSlotHeight, MaxSlotHeight, SlotSpacing)

            // アプリ・フォルダを実際の行数・列数で分割
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
                    verticalArrangement = Arrangement.spacedBy(SlotSpacing)
                ) {
                    // 行のループ
                    for (rowIndex in 0 until rows) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(SlotSpacing)
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
}

/**
 * [preferredCount]を基準に、各スロットのサイズが[minSlotSize]〜[maxSlotSize]の範囲に収まるように
 * 数を調整する。[preferredCount]のままだとスロットが[minSlotSize]未満になってしまう場合は数を
 * 減らし、逆に[maxSlotSize]を超えて間延びしてしまう場合は数を増やす。どちらの範囲にも収まって
 * いれば[preferredCount]をそのまま使う。
 */
private fun adaptiveSlotCount(availableSize: Dp, preferredCount: Int, minSlotSize: Dp, maxSlotSize: Dp, spacing: Dp): Int {
    var count = preferredCount.coerceAtLeast(1)
    // 狭すぎる場合は数を減らしてスロットのサイズを確保する
    while (count > 1) {
        val slotSize = (availableSize - spacing * (count - 1)) / count
        if (slotSize >= minSlotSize) break
        count--
    }
    // 広すぎて間延びする場合は数を増やして余白を詰める
    while (true) {
        val slotSize = (availableSize - spacing * (count - 1)) / count
        if (slotSize <= maxSlotSize) break
        count++
    }
    return count
}
