package com.example.gridlauncher.ui.sections

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.GridItem
import com.example.gridlauncher.ui.components.AppCard
import com.example.gridlauncher.ui.components.FolderCard
import com.example.gridlauncher.ui.drag.AppDragItem
import com.example.gridlauncher.ui.drag.AppDragPayload
import com.example.gridlauncher.ui.drag.AppDragSource
import com.example.gridlauncher.ui.drag.AppDropTarget
import com.example.gridlauncher.ui.drag.LocalAppDragState
import com.example.gridlauncher.ui.drag.appDragSource
import com.example.gridlauncher.ui.drag.appDropTarget
import com.example.gridlauncher.ui.drag.dragEdgeAutoScroll
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.SlotGridSize
import com.example.gridlauncher.util.adaptiveSlotCount
import com.example.gridlauncher.util.requiredSlotGridPages
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt

/** スロットがこれより狭くなるとアプリ名が読めなくなるとみなす、1スロットの最小幅。 */
private val MinSlotWidth = 108.dp

/** スロットがこれより低くなると窮屈になるとみなす、1スロットの最小高さ。 */
private val MinSlotHeight = 56.dp

/** スロットがこれより広くなると余白が間延びして見えるとみなす、1スロットの最大幅。 */
private val MaxSlotWidth = 180.dp

/** スロットがこれより高くなると余白が間延びして見えるとみなす、1スロットの最大高さ。 */
private val MaxSlotHeight = 72.dp

/**
 * ICON ONLYモード（正方形スロット・アイコンのみ表示）のときの、1スロットの最小の一辺の長さ。
 * 名前を表示しないため、通常モードよりずっと小さくても問題ない
 */
private val IconOnlySlotMinSize = 56.dp

/**
 * ICON ONLYモードのときの、1スロットの最大の一辺の長さ。アイコン単体だと通常モードより
 * 小さいサイズで間延びして見え始めるため、通常モードより小さい値にしている
 */
private val IconOnlySlotMaxSize = 64.dp

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
 * ICON ONLYモード（APP LISTの設定画面で切り替える）では、アイコンのみ表示（名前非表示・正方形スロット）に
 * なる。この場合はスロットが正方形になるよう列数から行数を導出し、名前がないぶん最小・最大
 * サイズのしきい値（[IconOnlySlotMinSize]・[IconOnlySlotMaxSize]）も通常モードより小さくする。
 *
 * アプリ・フォルダは長押し→ドラッグで移動・フォルダ化・削除する（[com.example.gridlauncher.ui.drag]参照）。
 *
 * @param items 表示するスロットの中身のリスト（アプリ・フォルダ・null=空きスロット）。
 * @param baseColumns グリッドの基準列数（ウィジェットのサイズがちょうどよいときに使う列数）。
 * @param baseRows グリッドの基準行数（ウィジェットのサイズがちょうどよいときに使う行数）。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param activeNotifications 通知（またはアプリバッジ）が来ているアプリのパッケージ名と件数のマップ。
 * @param openFolderId 現在ポップアップで開いているフォルダのID。該当するフォルダのカードは、
 *   ポップアップへ拡大するアニメーション（共有要素）のため見た目を隠す。
 * @param isIconOnly ICON ONLYモード（アイコンのみ表示・正方形スロット）かどうか。
 * @param gridSize 設定画面で指定したアイコンの並び（列数×行数）。nullの場合はAUTO（上記の自動判定）。
 * @param pageCount 設定画面で指定したページ数。スロットが埋まっても自動では増やさないが、
 *   アプリ・フォルダが入っているページより少なくはしない。
 * @param useOriginalIconColors trueの場合、アイコンをアクセントカラーのデュオトーン加工をせず、
 *   アプリ本来の色のまま表示する。
 * @param onSettingsClick ヘッダーの歯車ボタン（APP LISTの設定）がクリックされたときのコールバック。
 * @param onLayoutMeasured AUTOの場合の列数・行数と、実際の1ページのスロット数が決まるたびに呼ばれる
 *   コールバック（設定画面で現在の並びや、必要な最小ページ数を表示するために使う）。
 * @param onAddClick 空きスロットがクリックされたときのコールバック。
 * @param onFolderClick フォルダがクリックされたときのコールバック。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.AccessGridSection(
    items: List<GridItem?>,
    baseColumns: Int,
    baseRows: Int,
    isWallpaperMode: Boolean = false,
    activeNotifications: Map<String, Int> = emptyMap(),
    openFolderId: String? = null,
    showBorder: Boolean = true,
    isIconOnly: Boolean = false,
    gridSize: SlotGridSize? = null,
    pageCount: Int = 1,
    useOriginalIconColors: Boolean = false,
    onSettingsClick: () -> Unit = {},
    onLayoutMeasured: (autoColumns: Int, autoRows: Int, pageSize: Int) -> Unit = { _, _, _ -> },
    onAddClick: (Int) -> Unit,
    onFolderClick: (GridItem.FolderItem) -> Unit = {}
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
            Spacer(modifier = Modifier.weight(1f))
            // APP LISTの設定（ICON ONLY・アイコンの並び・ページ数）を開く歯車ボタン
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = "APP LIST Settings",
                tint = LocalCyberColors.current.accent,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(onClick = onSettingsClick)
                    .padding(3.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // AUTO（ウィジェットの大きさから自動で決める）の場合の列数・行数
            val autoColumns: Int
            val autoRows: Int
            if (isIconOnly) {
                // 列数は幅から通常通り決め、行数はそのスロット1辺の長さになるべく近くなる数を
                // 逆算する（スロットを正方形にするため）
                autoColumns = adaptiveSlotCount(maxWidth, baseColumns, IconOnlySlotMinSize, IconOnlySlotMaxSize, SlotSpacing)
                val slotSize = (maxWidth - SlotSpacing * (autoColumns - 1)) / autoColumns
                autoRows = squareCountForSlotSize(maxHeight, slotSize, SlotSpacing)
            } else {
                autoColumns = adaptiveSlotCount(maxWidth, baseColumns, MinSlotWidth, MaxSlotWidth, SlotSpacing)
                autoRows = adaptiveSlotCount(maxHeight, baseRows, MinSlotHeight, MaxSlotHeight, SlotSpacing)
            }
            // 設定画面で並びを指定している場合はそちらを使う
            val columns = gridSize?.columns ?: autoColumns
            val rows = gridSize?.rows ?: autoRows

            // アプリ・フォルダを実際の行数・列数で分割
            val pageSize = columns * rows
            LaunchedEffect(autoColumns, autoRows, pageSize) {
                onLayoutMeasured(autoColumns, autoRows, pageSize)
            }
            // ページ数は設定画面で指定した数（スロットが埋まっても自動では増やさない）。ただし、
            // アプリ・フォルダが入っているページが隠れないよう、それより少なくはしない
            val displayedPageCount = maxOf(pageCount, requiredSlotGridPages(items, pageSize) { it == null })
            val pagerState = rememberPagerState(pageCount = { displayedPageCount })
            val dragState = LocalAppDragState.current

            HorizontalPager(
                state = pagerState,
                // ドラッグ中に端でページ送りしても、ドラッグ元のスロット（持ち上げたアプリ）が
                // 破棄されてドラッグが途切れないよう、全ページを保持しておく
                beyondViewportPageCount = displayedPageCount - 1,
                modifier = Modifier.fillMaxSize().dragEdgeAutoScroll(pagerState)
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
                                // どのスロットもドロップ先にする。持ち上げ中のスロットは薄く表示して「抜けた」ことを示す
                                val slotModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .appDropTarget(AppDropTarget.GridSlot(globalIndex))
                                    .graphicsLayer {
                                        alpha = if (dragState?.payload?.source == AppDragSource.Grid(globalIndex)) 0.3f else 1f
                                    }

                                when (item) {
                                    is GridItem.AppItem -> {
                                        val appInfo = item.appInfo
                                        val notifCount = activeNotifications[appInfo.packageName] ?: 0
                                        AppCard(
                                            name = appInfo.label,
                                            packageName = appInfo.packageName,
                                            isMonochrome = appInfo.iconIsMonochrome,
                                            icon = appInfo.icon,
                                            modifier = slotModifier.appDragSource {
                                                AppDragPayload(AppDragSource.Grid(globalIndex), AppDragItem.App(appInfo))
                                            },
                                            notificationCount = notifCount,
                                            isWallpaperMode = isWallpaperMode,
                                            isCompact = isIconOnly,
                                            useOriginalIconColors = useOriginalIconColors,
                                            onClick = {
                                                context.packageManager.getLaunchIntentForPackage(appInfo.packageName)?.let {
                                                    context.startActivity(it)
                                                }
                                            }
                                        )
                                    }
                                    is GridItem.FolderItem -> {
                                        FolderCard(
                                            name = item.folder.name,
                                            modifier = slotModifier
                                                .appDragSource {
                                                    AppDragPayload(AppDragSource.Grid(globalIndex), AppDragItem.Folder(item.folder))
                                                }
                                                .sharedElementWithCallerManagedVisibility(
                                                    rememberSharedContentState(key = item.folder.id),
                                                    visible = item.folder.id != openFolderId
                                                ),
                                            isWallpaperMode = isWallpaperMode,
                                            onClick = { onFolderClick(item) }
                                        )
                                    }
                                    null -> {
                                        // 空きスロット（タップでアプリ追加。編集モード中は編集モード終了のみ）
                                        Surface(
                                            onClick = { onAddClick(globalIndex) },
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color.Transparent,
                                            border = BorderStroke(1.dp, LocalCyberColors.current.border),
                                            modifier = slotModifier
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
 * 1辺の長さが[targetSlotSize]になるべく近くなるような数を返す（ICON ONLYモードで、列数から
 * 決まったスロットの一辺の長さに行数を合わせ、正方形のスロットにするために使う）。
 */
private fun squareCountForSlotSize(availableSize: Dp, targetSlotSize: Dp, spacing: Dp): Int {
    val raw = (availableSize + spacing) / (targetSlotSize + spacing)
    return raw.coerceAtLeast(1f).roundToInt()
}
