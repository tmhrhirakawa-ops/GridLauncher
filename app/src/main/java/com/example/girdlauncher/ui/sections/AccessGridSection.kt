package com.example.girdlauncher.ui.sections

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import com.example.girdlauncher.util.adaptiveSlotCount
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
 * ウィジェット全体の幅がこれを下回ったら、ヘッダーが窮屈だとみなしICON ONLYボタンの文字を
 * 「IO」に略す（「APP LIST // APP NODES」との衝突・折り返しでレイアウトが崩れるのを防ぐため）。
 */
private val HeaderNarrowWidthThreshold = 260.dp

/**
 * アプリアイコン・フォルダのグリッドを表示するセクション。
 *
 * 列数・行数は[baseColumns]・[baseRows]を基準に、ウィジェットの実際の描画サイズに応じて
 * 自動的に決まる。スロットが[MinSlotWidth]・[MinSlotHeight]を下回りそうなほど狭くなったときは
 * 列数・行数を減らしてスロット自体のサイズを確保し（アイコンだけの縮退表示にはせず、表示する
 * アプリの数を減らすことで読みやすさを保つ）、逆に[MaxSlotWidth]・[MaxSlotHeight]を超えて
 * 間延びしそうなほど広くなったときは列数・行数を増やして余白を詰める。
 *
 * ヘッダー右上の「ICON ONLY」ボタンでアイコンのみ表示（名前非表示・正方形スロット）に切り替え
 * られる。この場合はスロットが正方形になるよう列数から行数を導出し、名前がないぶん最小・最大
 * サイズのしきい値（[IconOnlySlotMinSize]・[IconOnlySlotMaxSize]）も通常モードより小さくする。
 *
 * @param items 表示するスロットの中身のリスト（アプリ・フォルダ・null=空きスロット）。
 * @param baseColumns グリッドの基準列数（ウィジェットのサイズがちょうどよいときに使う列数）。
 * @param baseRows グリッドの基準行数（ウィジェットのサイズがちょうどよいときに使う行数）。
 * @param isEditMode UIが編集モードかどうか。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param activeNotifications 通知（またはアプリバッジ）が来ているアプリのパッケージ名と件数のマップ。
 * @param openFolderId 現在ポップアップで開いているフォルダのID。該当するフォルダのカードは、
 *   ポップアップへ拡大するアニメーション（共有要素）のため見た目を隠す。
 * @param isIconOnly ICON ONLYモード（アイコンのみ表示・正方形スロット）かどうか。
 * @param onIconOnlyClick ICON ONLYボタンがクリックされたとき（オン・オフを切り替える）のコールバック。
 * @param useOriginalIconColors trueの場合、アイコンをアクセントカラーのデュオトーン加工をせず、
 *   アプリ本来の色のまま表示する。
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
    isIconOnly: Boolean = false,
    onIconOnlyClick: () -> Unit = {},
    useOriginalIconColors: Boolean = false,
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // ウィジェットが狭くリサイズされてヘッダーが窮屈になってきたら、ICON ONLYボタンの
        // 文字を「IO」に略してレイアウトが崩れないようにする
        val isHeaderNarrow = maxWidth < HeaderNarrowWidthThreshold
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(8.dp)
                .background(LocalCyberColors.current.accent))
            Spacer(modifier = Modifier.width(8.dp))
            Text("APP LIST", fontFamily = CyberFont, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
            Spacer(modifier = Modifier.weight(1f))
            // ICON ONLY切り替えボタン（オンのときは塗りつぶし、オフのときは枠線のみ）
            Text(
                text = if (isHeaderNarrow) "IO" else "ICON ONLY",
                fontFamily = CyberFont,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isIconOnly) LocalCyberColors.current.bg else LocalCyberColors.current.accent,
                modifier = Modifier
                    .then(
                        if (isIconOnly) {
                            Modifier.background(LocalCyberColors.current.accent, RoundedCornerShape(4.dp))
                        } else {
                            Modifier.border(1.dp, LocalCyberColors.current.accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        }
                    )
                    .clickable { onIconOnlyClick() }
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val columns: Int
            val rows: Int
            if (isIconOnly) {
                // 列数は幅から通常通り決め、行数はそのスロット1辺の長さになるべく近くなる数を
                // 逆算する（スロットを正方形にするため）
                columns = adaptiveSlotCount(maxWidth, baseColumns, IconOnlySlotMinSize, IconOnlySlotMaxSize, SlotSpacing)
                val slotSize = (maxWidth - SlotSpacing * (columns - 1)) / columns
                rows = squareCountForSlotSize(maxHeight, slotSize, SlotSpacing)
            } else {
                columns = adaptiveSlotCount(maxWidth, baseColumns, MinSlotWidth, MaxSlotWidth, SlotSpacing)
                rows = adaptiveSlotCount(maxHeight, baseRows, MinSlotHeight, MaxSlotHeight, SlotSpacing)
            }

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
                                            isCompact = isIconOnly,
                                            useOriginalIconColors = useOriginalIconColors,
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
}

/**
 * 1辺の長さが[targetSlotSize]になるべく近くなるような数を返す（ICON ONLYモードで、列数から
 * 決まったスロットの一辺の長さに行数を合わせ、正方形のスロットにするために使う）。
 */
private fun squareCountForSlotSize(availableSize: Dp, targetSlotSize: Dp, spacing: Dp): Int {
    val raw = (availableSize + spacing) / (targetSlotSize + spacing)
    return raw.coerceAtLeast(1f).roundToInt()
}
