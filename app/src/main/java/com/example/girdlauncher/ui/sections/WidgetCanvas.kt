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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.PlacedWidget
import com.example.girdlauncher.model.WidgetPanel
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
 *   表示され、四隅につまむと太くなるリサイズハンドルと、上部に移動用のバーが出る。
 *   誤って移動してしまわないよう、移動は上部のバーをドラッグしたときだけ、リサイズは四隅の
 *   ハンドルをドラッグしたときだけ行われる（ウィジェット本体のドラッグでは移動もリサイズも
 *   起きない）。削除は、移動ドラッグ中にDock付近へ浮かぶ削除ゾーンにドロップして行う
 *   （ウィジェット自体には削除ボタンを表示しない）。
 * - **スロット編集モード**: 個々のアプリアイコン・ボタン自体を長押しすると入る、既存の編集モード
 *   （`AccessGridSection`/`QuickAccessSection`が内部で管理する✗バッジの表示・非表示）。
 *
 * 移動・リサイズ中は、指の動きにそのまま滑らかに追従する（グリッドへのスナップはしない）。
 * 代わりに、指を離したときに実際にスナップする位置・サイズを、破線枠のガイドとして
 * リアルタイムに表示する。ガイドは他のウィジェットと重ならない、直近で確定可能だった位置に
 * 固定され続けるため、指を重なる位置へ動かしてもガイドはそこへ追従しない。指を離すと、常に
 * このガイドの位置・サイズで確定する。
 *
 * @param columns グリッドの列数。
 * @param rows グリッドの行数。
 * @param placedWidgets 現在配置されているウィジェットの一覧。
 * @param isWidgetEditMode ウィジェット編集モードかどうか。
 * @param iconCellSizes キャンバスのセル1つ分の実サイズ（dp）を受け取り、ウィジェットの種類ごとの
 *   アイコン1個分のサイズ（キャンバスのセル単位、小数可）を返す関数。指定された種類は、リサイズ時に
 *   キャンバスの粗いセル単位ではなく、このアイコン1個分の固定サイズを単位として1行・1列ずつ
 *   スナップする（指定がない種類は従来通りセル単位でスナップ）。呼び出し側がセルサイズ算出のために
 *   別途`BoxWithConstraints`で画面を測り直さずに済むよう、ここで測定済みの値をそのまま渡す。
 * @param deleteZoneBoundsInRoot 「ここにドラッグして削除」ゾーンのルート座標系での範囲。
 *   移動ドラッグ中の指の位置がこの範囲に入ると、指を離したときに移動を確定する代わりに
 *   [onRequestDeleteConfirm]を呼ぶ。
 * @param onLayoutChange 配置（追加・削除・移動・リサイズ）が変わったときのコールバック。
 * @param onRequestAddWidget 「+ ADD WIDGET」タイルがタップされたときのコールバック。
 * @param onWidgetLongClick ウィジェット本体（個々のスロット以外）が長押しされたときのコールバック
 *   （ウィジェット編集モードに入る）。
 * @param onExitWidgetEditMode ウィジェット編集モード中にウィジェット本体がタップされたときの
 *   コールバック。
 * @param onWidgetDragStateChanged ウィジェットの移動ドラッグの状態が変わるたびに呼ばれるコール
 *   バック（種類、ドラッグ中かどうか、現在[deleteZoneBoundsInRoot]の上にいるかどうか）。呼び出し
 *   側はこれを使って「ここにドラッグして削除」ゾーンの表示・非表示や、ホバー中のハイライトを
 *   切り替えられる。
 * @param onRequestDeleteConfirm 移動ドラッグの指を[deleteZoneBoundsInRoot]内で離したときの
 *   コールバック（種類）。呼び出し側はここで削除確認ダイアログを表示する想定で、実際の削除は
 *   呼び出し側が[onLayoutChange]で行う。
 * @param content 実際のウィジェットの中身を描画するスロット（[WidgetPanel]の種類、現在表示中の
 *   （ドラッグでリサイズ中はそのライブプレビュー値を含む）colSpan・rowSpan、[iconCellSizes]が
 *   このウィジェットの種類に対して返したアイコン1個分のサイズ（未指定なら`null`）、サイズ確定済みの
 *   [Modifier]、現在リサイズドラッグ中かどうかを受け取り、既存の`AccessGridSection`等を呼び出す）。
 *   呼び出し側はこのcolSpan・rowSpanを使って、内部のスロット数などをリサイズ中もリアルタイムに
 *   追従させられる。リサイズ中かどうかのフラグは、呼び出し側が「ドラッグ中はライブプレビューだけ
 *   行い、指を離してサイズが確定したタイミングでだけ内部状態を変更する」といった処理の分岐に使える
 *   （例：QUICK ACCESSがウィジェットサイズに収まらなくなったスロットを、ドラッグ中は表示から
 *   隠すだけにして、指を離した時にだけ実際に空きスロットとして確定させる）。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.WidgetCanvas(
    columns: Int,
    rows: Int,
    placedWidgets: List<PlacedWidget>,
    isWidgetEditMode: Boolean,
    iconCellSizes: (cellWidth: Dp, cellHeight: Dp) -> Map<WidgetPanel, Pair<Float, Float>> = { _, _ -> emptyMap() },
    deleteZoneBoundsInRoot: Rect? = null,
    onLayoutChange: (List<PlacedWidget>) -> Unit,
    onRequestAddWidget: () -> Unit,
    onWidgetLongClick: () -> Unit,
    onExitWidgetEditMode: () -> Unit,
    onWidgetDragStateChanged: (type: WidgetPanel, dragging: Boolean, overDeleteZone: Boolean) -> Unit = { _, _, _ -> },
    onRequestDeleteConfirm: (WidgetPanel) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.(WidgetPanel, Float, Float, Pair<Float, Float>?, Modifier, Boolean) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows
        val resolvedIconCellSizes = iconCellSizes(cellWidth, cellHeight)

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
                    iconCellSize = resolvedIconCellSizes[widget.type],
                    deleteZoneBoundsInRoot = deleteZoneBoundsInRoot,
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
                    onWidgetLongClick = onWidgetLongClick,
                    onExitWidgetEditMode = onExitWidgetEditMode,
                    onDragStateChanged = { dragging, overDeleteZone -> onWidgetDragStateChanged(widget.type, dragging, overDeleteZone) },
                    onRequestDeleteConfirm = { onRequestDeleteConfirm(widget.type) }
                ) { liveColSpan, liveRowSpan, boxModifier, isResizing ->
                    content(widget.type, liveColSpan, liveRowSpan, resolvedIconCellSizes[widget.type], boxModifier, isResizing)
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

/**
 * リサイズハンドルのタッチ領域の一辺のサイズ。実機での検証で44dpでも指の当たり判定としては
 * シビアすぎた（ドラッグ開始位置が数px当たり判定の外にずれるだけで一切反応しなくなる）ため、
 * 見た目のブラケット（[ResizeHandleVisualSize]）よりかなり広めに取っている。
 */
private val ResizeHandleTouchSize = 56.dp

/** リサイズハンドルの見た目のブラケットのサイズ。[ResizeHandleTouchSize]とは独立して見た目を保つ。 */
private val ResizeHandleVisualSize = 40.dp

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
    deleteZoneBoundsInRoot: Rect?,
    onMoved: (col: Float, row: Float) -> Unit,
    onResized: (col: Float, row: Float, colSpan: Float, rowSpan: Float) -> Unit,
    onWidgetLongClick: () -> Unit,
    onExitWidgetEditMode: () -> Unit,
    onDragStateChanged: (dragging: Boolean, overDeleteZone: Boolean) -> Unit,
    onRequestDeleteConfirm: () -> Unit,
    content: @Composable (colSpan: Float, rowSpan: Float, modifier: Modifier, isResizing: Boolean) -> Unit
) {
    val colors = LocalCyberColors.current
    val density = LocalDensity.current

    // ドラッグ中（確定前）のライブプレビュー用オフセット（生のドラッグ量。指の動きをそのまま積算する）
    var dragOffsetPx by remember { mutableStateOf(Offset.Zero) }
    var resizeDeltaPx by remember { mutableStateOf(Offset.Zero) }
    // 現在ドラッグ中のリサイズコーナー（nullならリサイズドラッグ中ではない）
    var activeCorner by remember { mutableStateOf<ResizeCorner?>(null) }
    // 移動ドラッグ中、指が「ここにドラッグして削除」ゾーンの上にあるかどうか
    var isOverDeleteZone by remember { mutableStateOf(false) }
    // スナップ先ガイドが指すべき、直近で確定可能だった（＝他のウィジェットと重ならない）位置・
    // サイズ。ドラッグ中に指が重なる位置へ入っても、ここは最後に有効だった値のまま動かさない
    // （＝ガイドが「最終的にここへスナップされる」場所に固定され続ける）。ドラッグ開始時に
    // ウィジェットの現在値でリセットする
    var lastValidMoveCol by remember { mutableStateOf(widget.col) }
    var lastValidMoveRow by remember { mutableStateOf(widget.row) }
    var lastValidResizeCol by remember { mutableStateOf(widget.col) }
    var lastValidResizeRow by remember { mutableStateOf(widget.row) }
    var lastValidResizeColSpan by remember { mutableStateOf(widget.colSpan) }
    var lastValidResizeRowSpan by remember { mutableStateOf(widget.rowSpan) }
    // 移動ハンドルバーのルート座標系での位置（指のルート座標を求めるために使う）
    var barCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // 四隅のリサイズハンドルそれぞれのルート座標系での位置
    var cornerCoordinates by remember { mutableStateOf<Map<ResizeCorner, LayoutCoordinates>>(emptyMap()) }
    // ウィジェット本体Box自身のルート座標系での位置（本体側のpointerInputでの当たり判定に使う）
    var widgetBoxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val cellWidthPx = with(density) { cellWidth.toPx() }
    val cellHeightPx = with(density) { cellHeight.toPx() }
    val baseWidthPx = cellWidthPx * widget.colSpan
    val baseHeightPx = cellHeightPx * widget.rowSpan

    // 移動：グリッドへスナップしない連続値（指に滑らかに追従させる表示用）
    val maxCol = floor(columns - widget.colSpan).coerceAtLeast(0f)
    val maxRow = floor(rows - widget.rowSpan).coerceAtLeast(0f)
    val rawDeltaCol = dragOffsetPx.x / cellWidthPx
    val rawDeltaRow = dragOffsetPx.y / cellHeightPx
    val rawMoveCol = (widget.col + rawDeltaCol).coerceIn(0f, maxCol)
    val rawMoveRow = (widget.row + rawDeltaRow).coerceIn(0f, maxRow)

    // 指の生の移動量から、グリッドにスナップした移動先候補を求める（有効かどうかのチェックは
    // 呼び出し側で行う）
    fun snappedMoveCandidate(deltaPx: Offset): PlacedWidget {
        val col = (widget.col + (deltaPx.x / cellWidthPx).roundToInt()).coerceIn(0f, maxCol)
        val row = (widget.row + (deltaPx.y / cellHeightPx).roundToInt()).coerceIn(0f, maxRow)
        return widget.copy(col = col, row = row)
    }

    // リサイズ：アイコン1個分の固定サイズが指定されている種類はそのサイズを単位に、未指定の種類は
    // キャンバスのセル1つを単位にスナップする（固定サイズを使うことで、現在のwidget.colSpanに
    // 依存せず常に「アイコン1個分」ぴったりで増減できる＝アイコン数が1行・1列ずつ変わる）。
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

    // 表示用の連続値（スナップしない、滑らかに追従するサイズ）
    val rawColSpan = ((baseWidthPx + widthDeltaPx) / cellWidthPx).coerceIn(minColSpan, maxColSpan)
    val rawRowSpan = ((baseHeightPx + heightDeltaPx) / cellHeightPx).coerceIn(minRowSpan, maxRowSpan)
    val rawResizeCol = if (leftEdgeMoves) (rightEdge - rawColSpan) else widget.col
    val rawResizeRow = if (topEdgeMoves) (bottomEdge - rawRowSpan) else widget.row

    // 指の生のリサイズ量から、アイコン単位にスナップしたリサイズ先候補を求める（有効かどうかの
    // チェックは呼び出し側で行う）
    fun snappedResizeCandidate(corner: ResizeCorner, deltaPx: Offset): PlacedWidget {
        val cornerLeftEdgeMoves = corner == ResizeCorner.TOP_LEFT || corner == ResizeCorner.BOTTOM_LEFT
        val cornerTopEdgeMoves = corner == ResizeCorner.TOP_LEFT || corner == ResizeCorner.TOP_RIGHT
        val cornerWidthDeltaPx = if (cornerLeftEdgeMoves) -deltaPx.x else deltaPx.x
        val cornerHeightDeltaPx = if (cornerTopEdgeMoves) -deltaPx.y else deltaPx.y
        val cornerMaxColSpan = if (cornerLeftEdgeMoves) rightEdge else (columns - widget.col)
        val cornerMaxRowSpan = if (cornerTopEdgeMoves) bottomEdge else (rows - widget.row)
        val colSpan = (((baseWidthPx + cornerWidthDeltaPx) / iconWidthPx).roundToInt().coerceAtLeast(1) * (iconWidthPx / cellWidthPx))
            .coerceIn(minColSpan, cornerMaxColSpan)
        val rowSpan = (((baseHeightPx + cornerHeightDeltaPx) / iconHeightPx).roundToInt().coerceAtLeast(1) * (iconHeightPx / cellHeightPx))
            .coerceIn(minRowSpan, cornerMaxRowSpan)
        val col = if (cornerLeftEdgeMoves) (rightEdge - colSpan) else widget.col
        val row = if (cornerTopEdgeMoves) (bottomEdge - rowSpan) else widget.row
        return widget.copy(col = col, row = row, colSpan = colSpan, rowSpan = rowSpan)
    }

    val isResizing = activeCorner != null
    val isMoving = dragOffsetPx != Offset.Zero && !isResizing
    val isDragging = isResizing || isMoving

    // 実際に表示する位置・サイズ：指の動きに滑らかに追従する連続値。どちらでもなければ確定済みの値
    // （どちらも非ドラッグ時はwidget.col等とほぼ一致するので、常にこの式で問題ない）
    val displayCol = if (isResizing) rawResizeCol else rawMoveCol
    val displayRow = if (isResizing) rawResizeRow else rawMoveRow
    val displayColSpan = if (isResizing) rawColSpan else widget.colSpan
    val displayRowSpan = if (isResizing) rawRowSpan else widget.rowSpan

    // スナップ先ガイドの位置・サイズ（指を離したときに実際にスナップする場所のプレビュー）。
    // 直近で確定可能だった位置に固定し続ける（lastValidMoveCol等）ため、指が他のウィジェットと
    // 重なる位置に入っても、ガイドはそこへは追従せず最後に有効だった場所にとどまる
    val guideCol = if (isResizing) lastValidResizeCol else lastValidMoveCol
    val guideRow = if (isResizing) lastValidResizeRow else lastValidMoveRow
    val guideColSpan = if (isResizing) lastValidResizeColSpan else widget.colSpan
    val guideRowSpan = if (isResizing) lastValidResizeRowSpan else widget.rowSpan

    val onMoveDragEnd: () -> Unit = {
        if (isOverDeleteZone) {
            onRequestDeleteConfirm()
        } else {
            onMoved(lastValidMoveCol, lastValidMoveRow)
        }
        dragOffsetPx = Offset.Zero
        isOverDeleteZone = false
        onDragStateChanged(false, false)
    }
    val currentOnMoveDragEnd = rememberUpdatedState(onMoveDragEnd)
    val currentIsWidgetEditMode = rememberUpdatedState(isWidgetEditMode)
    val currentOnWidgetLongClick = rememberUpdatedState(onWidgetLongClick)
    val currentOnExitWidgetEditMode = rememberUpdatedState(onExitWidgetEditMode)

    // スナップ先ガイド（破線枠。ドラッグ中のみ表示。削除ゾーンの上にいる間は移動先の意味が
    // なくなるため隠す）。直近で有効だった位置に固定し続けるため常に確定可能な位置を指しており、
    // 警告色は不要
    if (isDragging && !isOverDeleteZone) {
        Box(
            modifier = Modifier
                .offset(x = cellWidth * guideCol, y = cellHeight * guideRow)
                .size(width = cellWidth * guideColSpan, height = cellHeight * guideRowSpan)
                .padding(4.dp)
                .border(2.dp, colors.accent.copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                .background(colors.accent.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
        )
    }

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
            .then(if (isOverDeleteZone) Modifier.alpha(0.35f) else Modifier)
            .onGloballyPositioned { widgetBoxCoordinates = it }
            // ウィジェットのヘッダーなど、個々のスロット（アプリアイコン・ボタンなど）が
            // 独自にタップ/長押しを処理していない「余白」部分でのみ、この検出が働く
            // （子のクリック領域が先に消費するため、ここには落ちてこない）。
            // ここでは「タップで編集モード終了」「長押しで編集モードに入る」のみを扱う。移動は
            // 上部のバー、リサイズは四隅のハンドルにジェスチャーを分離してあるため、本体では
            // ドラッグを一切扱わない。ただし、動きの有無にかかわらず必ずイベントを消費する必要が
            // ある：消費せずにいると、本体を長押ししたままドラッグしてしまったとき（＝リサイズ
            // しようとして誤って動かしてしまったとき）に、その未消費のドラッグが祖先のSurfaceに
            // ある背景ドラッグ検出（detectDragGestures）にまで届いてしまい、編集モードが
            // 意図せず解除されてしまう不具合があったため
            //
            // 移動バー・リサイズハンドルの範囲内から始まったジェスチャーは、それぞれが持つ
            // 独自のpointerInputに完全に委ねる。当初は「子が先にconsumeしたかどうか」で
            // 本体側が身を引く（wasHijackedByChild）方式だったが、これは子と本体のどちらが
            // 先にイベントを消費するかという実装依存の競合に頼っており、実機によってはこの
            // 競合の決着が安定せず、本体側が先にすべて消費してしまってハンドルが完全に無反応に
            // なる不具合があった。そのため、ここでは競合に頼らず、ダウン位置が事前に計測して
            // ある各ハンドルの範囲内かどうかを最初に判定し、範囲内であれば本体側は一切手を出さず
            // （consumeもせず）即座に手を引くようにしている
            .pointerInput(widget.type, isWidgetEditMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)

                    if (currentIsWidgetEditMode.value) {
                        val downRoot = widgetBoxCoordinates?.localToRoot(down.position)
                        val isOnHandle = downRoot != null && (
                            barCoordinates?.boundsInRoot()?.contains(downRoot) == true ||
                                cornerCoordinates.values.any { it.boundsInRoot().contains(downRoot) }
                            )
                        if (isOnHandle) return@awaitEachGesture
                    }

                    val downTime = down.uptimeMillis
                    val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
                    val touchSlop = viewConfiguration.touchSlop
                    var totalDistance = 0f
                    var isLongPress = false
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.isConsumed) break
                        change.consume()
                        totalDistance += (change.position - change.previousPosition).getDistance()
                        if (!currentIsWidgetEditMode.value && change.uptimeMillis - downTime >= longPressTimeoutMillis) {
                            isLongPress = true
                            break
                        }
                        if (!change.pressed) break
                    }
                    if (currentIsWidgetEditMode.value) {
                        // 編集モード中：指がほぼ動かなかった場合だけタップとみなして終了する
                        // （大きく動いた場合は本体の誤ドラッグとみなし、何もしない）
                        if (totalDistance < touchSlop) {
                            currentOnExitWidgetEditMode.value()
                        }
                    } else if (isLongPress) {
                        currentOnWidgetLongClick.value()
                    }
                }
            }
    ) {
        content(displayColSpan, displayRowSpan, Modifier.fillMaxSize(), isResizing)

        if (isWidgetEditMode) {
            // 移動ハンドルバー（上部中央。四隅のリサイズハンドルと被らないよう左右に余白を取る）
            MoveHandleBar(
                onDragStart = {
                    lastValidMoveCol = widget.col
                    lastValidMoveRow = widget.row
                    onDragStateChanged(true, false)
                },
                onDrag = { localPosition, amount ->
                    dragOffsetPx += amount
                    val candidate = snappedMoveCandidate(dragOffsetPx)
                    if (otherWidgets.none { it.overlaps(candidate) }) {
                        lastValidMoveCol = candidate.col
                        lastValidMoveRow = candidate.row
                    }
                    val rootPosition = barCoordinates?.localToRoot(localPosition)
                    isOverDeleteZone = deleteZoneBoundsInRoot != null && rootPosition != null &&
                        deleteZoneBoundsInRoot.contains(rootPosition)
                    onDragStateChanged(true, isOverDeleteZone)
                },
                onDragEnd = { currentOnMoveDragEnd.value() },
                onDragCancel = {
                    dragOffsetPx = Offset.Zero
                    isOverDeleteZone = false
                    onDragStateChanged(false, false)
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .padding(horizontal = ResizeHandleTouchSize + 4.dp)
                    .fillMaxWidth()
                    .onGloballyPositioned { barCoordinates = it }
            )

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
                    modifier = Modifier
                        .align(alignment)
                        .onGloballyPositioned { cornerCoordinates = cornerCoordinates + (corner to it) }
                ) {
                    CornerResizeHandle(
                        corner = corner,
                        onDrag = { amount ->
                            if (activeCorner != corner) {
                                // 新しいリサイズジェスチャーの開始：直近有効値をウィジェットの
                                // 現在値でリセットする
                                activeCorner = corner
                                lastValidResizeCol = widget.col
                                lastValidResizeRow = widget.row
                                lastValidResizeColSpan = widget.colSpan
                                lastValidResizeRowSpan = widget.rowSpan
                            }
                            resizeDeltaPx += amount
                            val candidate = snappedResizeCandidate(corner, resizeDeltaPx)
                            if (otherWidgets.none { it.overlaps(candidate) }) {
                                lastValidResizeCol = candidate.col
                                lastValidResizeRow = candidate.row
                                lastValidResizeColSpan = candidate.colSpan
                                lastValidResizeRowSpan = candidate.rowSpan
                            }
                        },
                        onDragEnd = {
                            onResized(lastValidResizeCol, lastValidResizeRow, lastValidResizeColSpan, lastValidResizeRowSpan)
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
 * ウィジェットの移動用ハンドルバー。ここを指で押して動かすとドラッグを検出し、それ以外
 * （リサイズハンドルや本体のドラッグなど）では移動が始まらないようにするための専用の当たり判定。
 * 中央に小さなグリップ（つまみ）を表示して、ここが動かせる場所であることを示す。
 *
 * 当初は誤操作防止のため長押し（[detectDragGesturesAfterLongPress]）を要求していたが、実機の
 * 指では長押し中のわずかな震え（touch slop超え）で長押し自体が頻繁にキャンセルされてしまい、
 * ほとんど反応しなかった。バー自体が専用の小さな当たり判定として独立しているため、長押しを
 * 要求しなくても誤って移動してしまう心配はなく、四隅のリサイズハンドルと同様に即座にドラッグを
 * 検出する方式に変更した。
 */
@Composable
private fun MoveHandleBar(
    onDragStart: () -> Unit,
    onDrag: (localPosition: Offset, amount: Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalCyberColors.current
    // isWidgetEditModeがtrueの間だけ生成される（このコンポーザブル自体が編集モードに入るたびに
    // 新しくマウントされる）ため、pointerInput(Unit)が古いクロージャに固定される問題は
    // 通常は起きないが、念のため他のドラッグハンドルと同様にrememberUpdatedStateで保護する
    val currentOnDragStart = rememberUpdatedState(onDragStart)
    val currentOnDrag = rememberUpdatedState(onDrag)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    val currentOnDragCancel = rememberUpdatedState(onDragCancel)
    Box(
        modifier = modifier
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.accent.copy(alpha = 0.16f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { currentOnDragStart.value() },
                    onDragEnd = { currentOnDragEnd.value() },
                    onDragCancel = { currentOnDragCancel.value() }
                ) { change, amount ->
                    change.consume()
                    currentOnDrag.value(change.position, amount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.accent.copy(alpha = 0.8f))
        )
    }
}

/**
 * ウィジェットの角につまめる、太い枠線のブラケット形のリサイズハンドル。
 * タッチ領域は見た目より大きめ（[ResizeHandleTouchSize]）に取り、角のブラケット自体は
 * [CornerBracket]で描画する。
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
    val bracketAlignment = when (corner) {
        ResizeCorner.TOP_LEFT -> Alignment.TopStart
        ResizeCorner.TOP_RIGHT -> Alignment.TopEnd
        ResizeCorner.BOTTOM_LEFT -> Alignment.BottomStart
        ResizeCorner.BOTTOM_RIGHT -> Alignment.BottomEnd
    }
    Box(
        modifier = Modifier
            .size(ResizeHandleTouchSize)
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
        CornerBracket(
            corner = corner,
            modifier = Modifier.align(bracketAlignment).size(ResizeHandleVisualSize)
        )
    }
}

/** ウィジェットの角に表示する、太いL字の枠線ブラケット。 */
@Composable
private fun CornerBracket(corner: ResizeCorner, modifier: Modifier = Modifier) {
    val accent = LocalCyberColors.current.accent
    Canvas(modifier = modifier) {
        val strokeWidthPx = 3.dp.toPx()
        val armLength = size.minDimension * 0.45f
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
