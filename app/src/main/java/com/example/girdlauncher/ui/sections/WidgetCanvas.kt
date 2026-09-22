package com.example.girdlauncher.ui.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * ACCESS GRID・CALENDAR・SYSTEM MONITOR・QUICK ACCESSを、追加・削除・リサイズ・移動できる
 * ウィジェットとして配置するキャンバス。[columns]×[rows]の粗いグリッド上に、各ウィジェットを
 * 矩形（[PlacedWidget]）として配置する。
 *
 * 編集モードには2種類あり、それぞれ独立している：
 * - **ウィジェット編集モード**（[isWidgetEditMode]）: ウィジェットのヘッダーなど、個々のスロット
 *   （アプリアイコン・ボタンなど）ではない部分を長押しすると入る。入ると全ウィジェットに枠線が
 *   表示され、四隅につまむと太くなるリサイズハンドルが出る。ヘッダーなどを長押ししたまま
 *   ドラッグすると移動する。
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
 *   コールバック（ウィジェット編集モードに入る。指を離さず続けてドラッグすれば、そのまま移動できる）。
 * @param onExitWidgetEditMode ウィジェット編集モード中にウィジェット本体がタップ（長押しの閾値に
 *   達する前に指を離す）されたときのコールバック。
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
                    onResized = { newCol, newRow, newColSpan, newRowSpan ->
                        onLayoutChange(
                            placedWidgets.map {
                                if (it.type == widget.type) {
                                    it.copy(col = newCol, row = newRow, colSpan = newColSpan, rowSpan = newRowSpan)
                                } else it
                            }
                        )
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

/** ウィジェットの四隅のリサイズハンドル。ドラッグすると、その角を固定点として反対側の辺が伸縮する。 */
private enum class ResizeCorner {
    TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
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
    onMoved: (col: Float, row: Float) -> Unit,
    onResized: (col: Float, row: Float, colSpan: Float, rowSpan: Float) -> Unit,
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
    // 現在ドラッグ中のリサイズコーナー（nullならリサイズドラッグ中ではない）
    var activeCorner by remember { mutableStateOf<ResizeCorner?>(null) }

    val cellWidthPx = with(density) { cellWidth.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }
    val baseWidthPx = cellWidthPx * widget.colSpan
    val baseHeightPx = cellHeightPx * widget.rowSpan

    // 移動ドラッグ中、現在の生ドラッグ量がスナップする先の列・行（グリッド範囲内にクランプ済み）
    val maxCol = floor(columns - widget.colSpan).coerceAtLeast(0f)
    val maxRow = floor(rows - widget.rowSpan).coerceAtLeast(0f)
    val snappedCol = (widget.col + (dragOffsetPx.x / cellWidthPx).roundToInt()).coerceIn(0f, maxCol)
    val snappedRow = (widget.row + (dragOffsetPx.y / cellHeightPx).roundToInt()).coerceIn(0f, maxRow)

    // リサイズドラッグ中、現在の生ドラッグ量がスナップする先の位置・サイズ。アイコン1個分の固定
    // サイズが指定されている種類はそのサイズを単位に、未指定の種類はキャンバスのセル1つを単位に
    // スナップする（固定サイズを使うことで、現在のwidget.colSpanに依存せず常に「アイコン1個分」
    // ぴったりで増減できる＝アイコン数が1行・1列ずつ変わる）。
    // ドラッグ中のコーナーが左端・上端の場合は、その辺を動かし反対側の辺（右端・下端）を固定点にする
    val iconWidthPx = (iconCellSize?.first ?: 1f) * cellWidthPx
    val iconHeightPx = (iconCellSize?.second ?: 1f) * cellHeightPx
    val leftEdgeMoves = activeCorner == ResizeCorner.TOP_LEFT || activeCorner == ResizeCorner.BOTTOM_LEFT
    val topEdgeMoves = activeCorner == ResizeCorner.TOP_LEFT || activeCorner == ResizeCorner.TOP_RIGHT
    val widthDeltaPx = if (leftEdgeMoves) -resizeDeltaPx.x else resizeDeltaPx.x
    val heightDeltaPx = if (topEdgeMoves) -resizeDeltaPx.y else resizeDeltaPx.y
    val minColSpan = iconWidthPx / cellWidthPx
    val minRowSpan = iconHeightPx / cellHeightPx
    val rightEdge = widget.col + widget.colSpan
    val bottomEdge = widget.row + widget.rowSpan
    val maxColSpan = if (leftEdgeMoves) rightEdge else (columns - widget.col)
    val maxRowSpan = if (topEdgeMoves) bottomEdge else (rows - widget.row)
    val snappedColSpan = (((baseWidthPx + widthDeltaPx) / iconWidthPx).roundToInt().coerceAtLeast(1) * (iconWidthPx / cellWidthPx))
        .coerceIn(minColSpan, maxColSpan)
    val snappedRowSpan = (((baseHeightPx + heightDeltaPx) / iconHeightPx).roundToInt().coerceAtLeast(1) * (iconHeightPx / cellHeightPx))
        .coerceIn(minRowSpan, maxRowSpan)
    val snappedResizeCol = if (leftEdgeMoves) (rightEdge - snappedColSpan) else widget.col
    val snappedResizeRow = if (topEdgeMoves) (bottomEdge - snappedRowSpan) else widget.row

    // 表示中の位置・サイズ：リサイズドラッグ中はそのライブプレビュー、移動ドラッグ中は移動の
    // ライブプレビュー、どちらでもなければ確定済みの値
    val displayCol = if (activeCorner != null) snappedResizeCol else snappedCol
    val displayRow = if (activeCorner != null) snappedResizeRow else snappedRow
    val displayColSpan = if (activeCorner != null) snappedColSpan else widget.colSpan
    val displayRowSpan = if (activeCorner != null) snappedRowSpan else widget.rowSpan

    val onMoveDragEnd: () -> Unit = {
        val candidate = widget.copy(col = snappedCol, row = snappedRow)
        if (otherWidgets.none { it.overlaps(candidate) }) {
            onMoved(snappedCol, snappedRow)
        }
        dragOffsetPx = Offset.Zero
    }
    val currentOnMoveDragEnd = rememberUpdatedState(onMoveDragEnd)
    val currentIsWidgetEditMode = rememberUpdatedState(isWidgetEditMode)
    val currentOnWidgetLongClick = rememberUpdatedState(onWidgetLongClick)
    val currentOnExitWidgetEditMode = rememberUpdatedState(onExitWidgetEditMode)

    Box(
        modifier = Modifier
            .offset(x = cellWidth * displayCol, y = cellHeight * displayRow)
            .size(
                width = cellWidth * displayColSpan,
                height = cellHeight * displayRowSpan
            )
            .padding(4.dp)
            .then(
                if (isWidgetEditMode) Modifier.border(1.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(6.dp)) else Modifier
            )
            // ウィジェットのヘッダーなど、個々のスロット（アプリアイコン・ボタンなど）が
            // 独自にタップ/長押しを処理していない「余白」部分でのみ、この検出が働く
            // （子のクリック領域が先に消費するため、ここには落ちてこない）。
            // タップ（短押し）と長押し→ドラッグを、1つのジェスチャーループで自前判定する
            // （detectTapGesturesとdetectDragGesturesAfterLongPressを別々のpointerInput
            // ブロックとして重ねると、同じジェスチャーを2つの検出器が競合して処理してしまい、
            // 「長押しで編集モードに入った直後、指を離すとタップとも判定されて即座に編集モードが
            // 解除される」不具合が出たため、1つの検出器だけで完結させている）
            .pointerInput(widget.type) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
                    var isLongPress = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed) break
                        // このジェスチャーは自分たちで最後まで判定・処理するので、祖先の
                        // Surfaceにある背景タップ用のclickable/detectDragGesturesに同じ
                        // ジェスチャーを二重処理させないよう、動きの有無にかかわらず必ず消費する
                        // （消費しないと、指を動かさない長押しの場合にdrag()のonDragが一度も
                        // 呼ばれず、そのタップが祖先のclickableにも届いて編集モードが直後に
                        // 解除されてしまっていた）
                        change.consume()
                        // 経過時間のチェックを先に行う：指を静止したまま押し続けている間は
                        // 中間イベントが来ず、離した瞬間のUPイベントしか観測できないことがある。
                        // pressedのチェックを先にしてしまうと、長押しの閾値を超えていても
                        // 「離された＝タップ」と誤判定してしまうため、必ず経過時間を先に見る
                        if (change.uptimeMillis - down.uptimeMillis >= longPressTimeoutMillis) {
                            isLongPress = true
                            break
                        }
                        if (!change.pressed) break
                    }

                    if (isLongPress) {
                        // 長押しが成立。編集モードでなければここで入り、指を離さず
                        // ドラッグを続ければそのまま移動できる
                        if (!currentIsWidgetEditMode.value) {
                            currentOnWidgetLongClick.value()
                        }
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                            val delta = change.position - change.previousPosition
                            if (delta != Offset.Zero) {
                                dragOffsetPx += delta
                                moved = true
                            }
                        }
                        if (moved) {
                            currentOnMoveDragEnd.value()
                        } else {
                            dragOffsetPx = Offset.Zero
                        }
                    } else {
                        // 長押しタイムアウトより前に指が離れた＝タップ
                        if (currentIsWidgetEditMode.value) {
                            currentOnExitWidgetEditMode.value()
                        }
                    }
                }
            }
    ) {
        content(displayColSpan, displayRowSpan, Modifier.fillMaxSize())

        if (isWidgetEditMode) {
            // 削除バッジ（右上。四隅のリサイズハンドルと被らないよう少し内側に寄せる）
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 26.dp, end = 26.dp)
            ) {
                RemoveBadge(onClick = onRemove)
            }

            // 四隅のリサイズハンドル。どこをつまんでも、その角を固定点として反対側の辺が伸縮する
            for (corner in ResizeCorner.entries) {
                val alignment = when (corner) {
                    ResizeCorner.TOP_LEFT -> Alignment.TopStart
                    ResizeCorner.TOP_RIGHT -> Alignment.TopEnd
                    ResizeCorner.BOTTOM_LEFT -> Alignment.BottomStart
                    ResizeCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
                }
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                    exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                    modifier = Modifier.align(alignment)
                ) {
                    CornerResizeHandle(
                        corner = corner,
                        onDrag = { amount ->
                            activeCorner = corner
                            resizeDeltaPx += amount
                        },
                        onDragEnd = {
                            val candidate = widget.copy(
                                col = snappedResizeCol,
                                row = snappedResizeRow,
                                colSpan = snappedColSpan,
                                rowSpan = snappedRowSpan
                            )
                            if (otherWidgets.none { it.overlaps(candidate) }) {
                                onResized(snappedResizeCol, snappedResizeRow, snappedColSpan, snappedRowSpan)
                            }
                            resizeDeltaPx = Offset.Zero
                            activeCorner = null
                        },
                        onDragCancel = {
                            resizeDeltaPx = Offset.Zero
                            activeCorner = null
                        }
                    )
                }
            }
        }
    }
}

/**
 * ウィジェットの角につまめる、太い枠線のブラケット形のリサイズハンドル。
 * タッチ領域は見た目より大きめに取り、角のブラケット自体は[CornerBracket]で描画する。
 */
@Composable
private fun CornerResizeHandle(
    corner: ResizeCorner,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit
) {
    // pointerInput(Unit)は初回のみ起動するため、onDrag/onDragEnd/onDragCancelを直接渡すと初回の
    // 古いクロージャに固定されてしまう。rememberUpdatedStateで常に最新のラムダを参照する
    val currentOnDrag = rememberUpdatedState(onDrag)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    val currentOnDragCancel = rememberUpdatedState(onDragCancel)
    Box(
        modifier = Modifier
            .size(28.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { currentOnDragEnd.value() },
                    onDragCancel = { currentOnDragCancel.value() }
                ) { change, dragAmount ->
                    change.consume()
                    currentOnDrag.value(dragAmount)
                }
            }
    ) {
        CornerBracket(corner = corner, modifier = Modifier.size(28.dp))
    }
}

/** ウィジェットの角に表示する、太いL字の枠線ブラケット。 */
@Composable
private fun CornerBracket(corner: ResizeCorner, modifier: Modifier = Modifier) {
    val accent = LocalCyberColors.current.accent
    Canvas(modifier = modifier) {
        val strokeWidthPx = 3.dp.toPx()
        val armLength = size.minDimension * 0.6f
        val half = strokeWidthPx / 2f
        when (corner) {
            ResizeCorner.TOP_LEFT -> {
                drawLine(accent, Offset(0f, half), Offset(armLength, half), strokeWidth = strokeWidthPx)
                drawLine(accent, Offset(half, 0f), Offset(half, armLength), strokeWidth = strokeWidthPx)
            }
            ResizeCorner.TOP_RIGHT -> {
                drawLine(accent, Offset(size.width - armLength, half), Offset(size.width, half), strokeWidth = strokeWidthPx)
                drawLine(accent, Offset(size.width - half, 0f), Offset(size.width - half, armLength), strokeWidth = strokeWidthPx)
            }
            ResizeCorner.BOTTOM_LEFT -> {
                drawLine(accent, Offset(0f, size.height - half), Offset(armLength, size.height - half), strokeWidth = strokeWidthPx)
                drawLine(accent, Offset(half, size.height - armLength), Offset(half, size.height), strokeWidth = strokeWidthPx)
            }
            ResizeCorner.BOTTOM_RIGHT -> {
                drawLine(accent, Offset(size.width - armLength, size.height - half), Offset(size.width, size.height - half), strokeWidth = strokeWidthPx)
                drawLine(accent, Offset(size.width - half, size.height - armLength), Offset(size.width - half, size.height), strokeWidth = strokeWidthPx)
            }
        }
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
