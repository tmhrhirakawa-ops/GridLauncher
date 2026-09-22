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
 * 編集モード（[isEditMode]、アプリの長押しなどで入る既存のグローバルな編集モードと共通）中は、
 * 各ウィジェットの左上に移動ハンドル、右上に削除バッジ、右下にリサイズハンドルを表示する。
 * ドラッグ確定時に他のウィジェットと重なる場合は、元の位置・サイズへスナップバックする。
 *
 * @param columns グリッドの列数。
 * @param rows グリッドの行数。
 * @param placedWidgets 現在配置されているウィジェットの一覧。
 * @param isEditMode UIが編集モードかどうか。
 * @param onLayoutChange 配置（追加・削除・移動・リサイズ）が変わったときのコールバック。
 * @param onRequestAddWidget 「+ ADD WIDGET」タイルがタップされたときのコールバック。
 * @param onLongClick ウィジェットが長押しされたときのコールバック（編集モードに入る）。
 * @param onExitEditMode 編集モード中にウィジェット本体がタップされたときのコールバック。
 * @param content 実際のウィジェットの中身を描画するスロット（[WidgetPanel]の種類とサイズ確定済みの
 *   [Modifier]を受け取り、既存の`AccessGridSection`等を呼び出す）。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.WidgetCanvas(
    columns: Int,
    rows: Int,
    placedWidgets: List<PlacedWidget>,
    isEditMode: Boolean,
    onLayoutChange: (List<PlacedWidget>) -> Unit,
    onRequestAddWidget: () -> Unit,
    onLongClick: () -> Unit,
    onExitEditMode: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.(WidgetPanel, Modifier) -> Unit
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
                    isEditMode = isEditMode,
                    otherWidgets = otherWidgets,
                    onMoved = { newCol, newRow ->
                        onLayoutChange(placedWidgets.map { if (it.type == widget.type) it.copy(col = newCol, row = newRow) else it })
                    },
                    onResized = { newColSpan, newRowSpan ->
                        onLayoutChange(placedWidgets.map { if (it.type == widget.type) it.copy(colSpan = newColSpan, rowSpan = newRowSpan) else it })
                    },
                    onRemove = {
                        onLayoutChange(placedWidgets.filter { it.type != widget.type })
                    },
                    onLongClick = onLongClick,
                    onExitEditMode = onExitEditMode
                ) { boxModifier ->
                    content(widget.type, boxModifier)
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
    isEditMode: Boolean,
    otherWidgets: List<PlacedWidget>,
    onMoved: (col: Int, row: Int) -> Unit,
    onResized: (colSpan: Int, rowSpan: Int) -> Unit,
    onRemove: () -> Unit,
    onLongClick: () -> Unit,
    onExitEditMode: () -> Unit,
    content: @Composable (Modifier) -> Unit
) {
    val colors = LocalCyberColors.current
    val density = LocalDensity.current

    // ドラッグ中（確定前）のライブプレビュー用オフセット
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var resizeDeltaPx by remember { mutableStateOf(Offset.Zero) }

    val baseX = cellWidth * widget.col
    val baseY = cellHeight * widget.row
    val baseWidth = cellWidth * widget.colSpan
    val baseHeight = cellHeight * widget.rowSpan

    val dragOffsetXDp = with(density) { dragOffsetPx.x.toDp() }
    val dragOffsetYDp = with(density) { dragOffsetPx.y.toDp() }
    val resizeDeltaXDp = with(density) { resizeDeltaPx.x.toDp() }
    val resizeDeltaYDp = with(density) { resizeDeltaPx.y.toDp() }

    Box(
        modifier = Modifier
            .offset(x = baseX + dragOffsetXDp, y = baseY + dragOffsetYDp)
            .size(
                width = (baseWidth + resizeDeltaXDp).coerceAtLeast(cellWidth),
                height = (baseHeight + resizeDeltaYDp).coerceAtLeast(cellHeight)
            )
            .padding(4.dp)
    ) {
        content(Modifier.fillMaxSize())

        if (isEditMode) {
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
                        val deltaCol = (dragOffsetPx.x / with(density) { cellWidth.toPx() }).roundToInt()
                        val deltaRow = (dragOffsetPx.y / with(density) { cellHeight.toPx() }).roundToInt()
                        val newCol = (widget.col + deltaCol).coerceIn(0, (columns - widget.colSpan).coerceAtLeast(0))
                        val newRow = (widget.row + deltaRow).coerceIn(0, (rows - widget.rowSpan).coerceAtLeast(0))
                        val candidate = widget.copy(col = newCol, row = newRow)
                        if (otherWidgets.none { it.overlaps(candidate) }) {
                            onMoved(newCol, newRow)
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
                        val deltaColSpan = (resizeDeltaPx.x / with(density) { cellWidth.toPx() }).roundToInt()
                        val deltaRowSpan = (resizeDeltaPx.y / with(density) { cellHeight.toPx() }).roundToInt()
                        val newColSpan = (widget.colSpan + deltaColSpan).coerceIn(1, columns - widget.col)
                        val newRowSpan = (widget.rowSpan + deltaRowSpan).coerceIn(1, rows - widget.row)
                        val candidate = widget.copy(colSpan = newColSpan, rowSpan = newRowSpan)
                        if (otherWidgets.none { it.overlaps(candidate) }) {
                            onResized(newColSpan, newRowSpan)
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
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(colors.bg.copy(alpha = 0.92f))
            .border(1.dp, colors.accent, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragCancel
                ) { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
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
