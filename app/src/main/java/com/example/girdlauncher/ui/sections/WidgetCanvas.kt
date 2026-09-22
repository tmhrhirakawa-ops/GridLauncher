package com.example.girdlauncher.ui.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.PlacedWidget
import com.example.girdlauncher.model.WidgetPanel
import com.example.girdlauncher.ui.components.RemoveBadge
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.findFreeGridSlot
import kotlin.math.roundToInt

/**
 * ACCESS GRID・CALENDAR・SYSTEM MONITOR・QUICK ACCESSを、追加・削除・リサイズ・移動できる
 * ウィジェットとして配置するキャンバス。[columns]×[rows]の粗いグリッド上に、各ウィジェットを
 * 矩形（[PlacedWidget]）として配置する。
 *
 * 編集モードには2種類あり、それぞれ独立している：
 * - **ウィジェット編集モード**（[isWidgetEditMode]）: ウィジェットのヘッダーなど、個々のスロット
 *   （アプリアイコン・ボタンなど）ではない部分を長押しすると入る。移動・リサイズ・削除ハンドルを表示。
 * - **スロット編集モード**: 個々のアプリアイコン・ボタン自体を長押しすると入る、既存の編集モード
 *   （`AccessGridSection`/`QuickAccessSection`が内部で管理する✗バッジの表示・非表示）。
 * ドラッグ確定時に他のウィジェットと重なる場合は、元の位置・サイズへスナップバックする。
 *
 * @param columns グリッドの列数。
 * @param rows グリッドの行数。
 * @param placedWidgets 現在配置されているウィジェットの一覧。
 * @param isWidgetEditMode ウィジェット編集モードかどうか。
 * @param iconCellSizes ウィジェットの種類ごとの、アイコン1個分のサイズ（キャンバスのセル単位、小数可）。
 *   指定された種類は、リサイズ時にキャンバスの粗いセル単位ではなく、このアイコン1個分の固定サイズを
 *   単位として1行・1列ずつスナップする（指定がない種類は従来通りセル単位でスナップ）。
 * @param onLayoutChange 配置（追加・削除・移動・リサイズ）が変わったときのコールバック。
 * @param onRequestAddWidget 「+ ADD WIDGET」タイルがタップされたときのコールバック。
 * @param onWidgetLongClick ウィジェットのヘッダーなど（個々のスロット以外）が長押しされたときの
 *   コールバック（ウィジェット編集モードに入る）。
 * @param onExitWidgetEditMode ウィジェット編集モード中にウィジェット本体がタップされたときの
 *   コールバック。
 * @param content 実際のウィジェットの中身を描画するスロット（[WidgetPanel]の種類、現在表示中の
 *   （ドラッグでリサイズ中はそのライブプレビュー値を含む）colSpan・rowSpan、サイズ確定済みの
 *   [Modifier]を受け取り、既存の`AccessGridSection`等を呼び出す）。呼び出し側はこのcolSpan・
 *   rowSpanを使って、内部のスロット数などをリサイズ中もリアルタイムに追従させられる。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.WidgetCanvas(
    columns: Int,
    rows: Int,
    placedWidgets: List<PlacedWidget>,
    isWidgetEditMode: Boolean,
    iconCellSizes: Map<WidgetPanel, Pair<Float, Float>> = emptyMap(),
    onLayoutChange: (List<PlacedWidget>) -> Unit,
    onRequestAddWidget: () -> Unit,
    onWidgetLongClick: () -> Unit,
    onExitWidgetEditMode: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.(WidgetPanel, Float, Float, Modifier) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows

        placedWidgets.forEach { widget ->
            key(widget.type) {
                val otherWidgets = remember(placedWidgets) { placedWidgets.filter { it.type != widget.type } }
                WidgetSlot(
                    widget = widget,
                    columns = columns,
                    rows = rows,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    isWidgetEditMode = isWidgetEditMode,
                    otherWidgets = otherWidgets,
                    iconCellSize = iconCellSizes[widget.type],
                    onMoved = { newCol, newRow ->
                        onLayoutChange(placedWidgets.map { if (it.type == widget.type) it.copy(col = newCol, row = newRow) else it })
                    },
                    onResized = { newColSpan, newRowSpan ->
                        onLayoutChange(placedWidgets.map { if (it.type == widget.type) it.copy(colSpan = newColSpan, rowSpan = newRowSpan) else it })
                    },
                    onRemove = {
                        onLayoutChange(placedWidgets.filter { it.type != widget.type })
                    },
                    onWidgetLongClick = onWidgetLongClick,
                    onExitWidgetEditMode = onExitWidgetEditMode
                ) { liveColSpan, liveRowSpan, boxModifier ->
                    content(widget.type, liveColSpan, liveRowSpan, boxModifier)
                }
            }
        }

        // 未配置のウィジェットがあれば、空いている領域に追加導線を表示する
        val unplacedTypes = WidgetPanel.entries.filterNot { type -> placedWidgets.any { it.type == type } }
        if (unplacedTypes.isNotEmpty()) {
            val freeSlot = remember(placedWidgets, columns, rows) { findFreeGridSlot(placedWidgets, columns, rows) }
            freeSlot?.let { (freeCol, freeRow, freeColSpan, freeRowSpan) ->
                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * freeCol, y = cellHeight * freeRow)
                        .size(cellWidth * freeColSpan, cellHeight * freeRowSpan)
                        .padding(4.dp)
                ) {
                    AddWidgetTile(onClick = onRequestAddWidget)
                }
            }
        }
    }
}

@Composable
private fun WidgetSlot(
    widget: PlacedWidget,
    columns: Int,
    rows: Int,
    cellWidth: Dp,
    cellHeight: Dp,
    isWidgetEditMode: Boolean,
    otherWidgets: List<PlacedWidget>,
    iconCellSize: Pair<Float, Float>?,
    onMoved: (col: Int, row: Int) -> Unit,
    onResized: (colSpan: Float, rowSpan: Float) -> Unit,
    onRemove: () -> Unit,
    onWidgetLongClick: () -> Unit,
    onExitWidgetEditMode: () -> Unit,
    content: @Composable (colSpan: Float, rowSpan: Float, modifier: Modifier) -> Unit
) {
    val colors = LocalCyberColors.current
    val density = LocalDensity.current

    // ドラッグ中（確定前）のライブプレビュー用オフセット（生のドラッグ量。指の動きをそのまま積算する）
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var resizeDeltaPx by remember { mutableStateOf(Offset.Zero) }

    val cellWidthPx = with(density) { cellWidth.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }
    val baseWidthPx = cellWidthPx * widget.colSpan
    val baseHeightPx = cellHeightPx * widget.rowSpan

    // 移動ドラッグ中、現在の生ドラッグ量がスナップする先の列・行（グリッド範囲内にクランプ済み）
    val maxCol = kotlin.math.floor(columns - widget.colSpan).toInt().coerceAtLeast(0)
    val maxRow = kotlin.math.floor(rows - widget.rowSpan).toInt().coerceAtLeast(0)
    val snappedCol = (widget.col + (dragOffsetPx.x / cellWidthPx).roundToInt()).coerceIn(0, maxCol)
    val snappedRow = (widget.row + (dragOffsetPx.y / cellHeightPx).roundToInt()).coerceIn(0, maxRow)

    // リサイズドラッグ中、現在の生ドラッグ量がスナップする先のサイズ。アイコン1個分の固定サイズが
    // 指定されている種類はそのサイズを単位に、未指定の種類はキャンバスのセル1つを単位にスナップする
    // （固定サイズを使うことで、現在のwidget.colSpanに依存せず常に「アイコン1個分」ぴったりで
    // 増減できる＝アイコン数が1行・1列ずつ変わる）
    val iconWidthPx = (iconCellSize?.first ?: 1f) * cellWidthPx
    val iconHeightPx = (iconCellSize?.second ?: 1f) * cellHeightPx
    val maxColSpan = (columns - widget.col).toFloat()
    val maxRowSpan = (rows - widget.row).toFloat()
    val snappedColSpan = (((baseWidthPx + resizeDeltaPx.x) / iconWidthPx).roundToInt().coerceAtLeast(1) * (iconWidthPx / cellWidthPx))
        .coerceIn(iconWidthPx / cellWidthPx, maxColSpan)
    val snappedRowSpan = (((baseHeightPx + resizeDeltaPx.y) / iconHeightPx).roundToInt().coerceAtLeast(1) * (iconHeightPx / cellHeightPx))
        .coerceIn(iconHeightPx / cellHeightPx, maxRowSpan)

    Box(
        modifier = Modifier
            .offset(x = cellWidth * snappedCol, y = cellHeight * snappedRow)
            .size(
                width = cellWidth * snappedColSpan,
                height = cellHeight * snappedRowSpan
            )
            .padding(4.dp)
            // ウィジェットのヘッダーなど、個々のスロット（アプリアイコン・ボタンなど）が
            // 独自にタップ/長押しを処理していない「余白」部分でのみ、この検出が働く
            // （子のクリック領域が先に消費するため、ここには落ちてこない）。
            .pointerInput(widget.type) {
                detectTapGestures(
                    onTap = { onExitWidgetEditMode() },
                    onLongPress = { onWidgetLongClick() }
                )
            }
    ) {
        content(snappedColSpan, snappedRowSpan, Modifier.fillMaxSize())

        if (isWidgetEditMode) {
            // 移動ハンドル（左上）
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp)
            ) {
                EditHandle(
                    icon = Icons.Filled.OpenWith,
                    contentDescription = "Move Widget",
                    onDrag = { amount -> dragOffsetPx += amount },
                    onDragEnd = {
                        // ライブプレビューと同じスナップ先（snappedCol/snappedRow）をそのまま確定させる
                        val candidate = widget.copy(col = snappedCol, row = snappedRow)
                        if (otherWidgets.none { it.overlaps(candidate) }) {
                            onMoved(snappedCol, snappedRow)
                        }
                        dragOffsetPx = Offset.Zero
                    },
                    onDragCancel = { dragOffsetPx = Offset.Zero }
                )
            }

            // 削除バッジ（右上）
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                RemoveBadge(onClick = onRemove)
            }

            // リサイズハンドル（右下）
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
            ) {
                EditHandle(
                    icon = Icons.Filled.ZoomOutMap,
                    contentDescription = "Resize Widget",
                    onDrag = { amount -> resizeDeltaPx += amount },
                    onDragEnd = {
                        // ライブプレビューと同じスナップ先（snappedColSpan/snappedRowSpan）をそのまま確定させる
                        val candidate = widget.copy(colSpan = snappedColSpan, rowSpan = snappedRowSpan)
                        if (otherWidgets.none { it.overlaps(candidate) }) {
                            onResized(snappedColSpan, snappedRowSpan)
                        }
                        resizeDeltaPx = Offset.Zero
                    },
                    onDragCancel = { resizeDeltaPx = Offset.Zero }
                )
            }
        }
    }
}

@Composable
private fun EditHandle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    val colors = LocalCyberColors.current
    // pointerInput(Unit)のジェスチャー検出コルーチンは初回のみ起動し、以降のwidget編集モード中の
    // 再コンポジションでは再起動しない。そのため、onDrag/onDragEnd/onDragCancelを直接渡すと初回の
    // 古いクロージャ（＝リサイズ前のwidgetサイズなど）に固定されてしまい、2回目以降のドラッグ確定時に
    // 古い値を基準に計算されて元のサイズへ戻ってしまう。rememberUpdatedStateで常に最新のラムダを
    // 参照するようにする
    val currentOnDrag = rememberUpdatedState(onDrag)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    val currentOnDragCancel = rememberUpdatedState(onDragCancel)
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(colors.bg.copy(alpha = 0.92f))
            .border(1.dp, colors.accent, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { currentOnDragEnd.value() },
                    onDragCancel = { currentOnDragCancel.value() }
                ) { change, dragAmount ->
                    change.consume()
                    currentOnDrag.value(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = colors.accent,
            modifier = Modifier.size(13.dp)
        )
    }
}

@Composable
private fun AddWidgetTile(onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("+ ADD WIDGET", fontFamily = CyberFont, fontSize = 10.sp, color = colors.text.copy(alpha = 0.3f))
        }
    }
}
