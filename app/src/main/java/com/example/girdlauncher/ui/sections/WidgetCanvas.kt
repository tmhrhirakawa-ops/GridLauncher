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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.PlacedWidget
import com.example.girdlauncher.model.WidgetPanel
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.findFreeGridSlot
import kotlin.math.floor
import kotlin.math.roundToInt

/** ウィジェットがリサイズに対応している方向。[ResizeConstraints.axes]で使う。 */
enum class ResizeAxes { BOTH, HORIZONTAL, VERTICAL, NONE }

/**
 * ウィジェットインスタンスごとのリサイズ制約。
 *
 * 主に他アプリのAppWidget（[WidgetPanel.APPWIDGET]）向けで、`AppWidgetProviderInfo`が持つ
 * 実際の最小/最大サイズ・対応方向を反映するために使う。GirdLauncher内蔵の4種
 * （ACCESS GRID等）は、これまで通りグリッド全体を自由にリサイズできるデフォルト値のままでよい。
 *
 * @property minSize 実際の最小サイズ（dp）。nullなら従来通り「1グリッドセル」または
 *   （[WidgetCanvas]の`iconCellSizes`が指定されていれば）「アイコン1個分」が最小になる。
 * @property maxSize 実際の最大サイズ（dp）。nullなら従来通りグリッドの範囲までが最大になる。
 * @property axes 対応しているリサイズ方向。[ResizeAxes.NONE]の場合、リサイズハンドル自体を
 *   表示しない（移動は引き続きできる）。
 */
data class ResizeConstraints(
    val minSize: DpSize? = null,
    val maxSize: DpSize? = null,
    val axes: ResizeAxes = ResizeAxes.BOTH
)

/**
 * ACCESS GRID・CALENDAR・SYSTEM MONITOR・QUICK ACCESSを、追加・削除・リサイズ・移動できる
 * ウィジェットとして配置するキャンバス。[columns]×[rows]の粗いグリッド上に、各ウィジェットを
 * 矩形（[PlacedWidget]）として配置する。
 *
 * 編集モードには2種類あり、それぞれ独立している：
 * - **ウィジェット編集モード**（[isWidgetEditMode]）: ウィジェットのヘッダーなど、個々のスロット
 *   （アプリアイコン・ボタンなど）ではない部分を長押しすると入る。入ると全ウィジェットに枠線が
 *   表示され、四隅につまむと太くなるリサイズハンドルが出る。移動はウィジェット全域
 *   （四隅のリサイズハンドルの範囲を除く）をドラッグすることで行う。削除は、移動ドラッグ中に
 *   Dock付近へ浮かぶ削除ゾーンにドロップして行う（ウィジェット自体には削除ボタンを表示しない）。
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
 * @param resizeConstraints ウィジェットインスタンスごとのリサイズ制約（[ResizeConstraints]）を
 *   返す関数。他アプリのAppWidgetのように、種類だけでなくインスタンスごとに実際の最小/最大
 *   サイズ・対応方向が異なるものに使う。指定がない種類・インスタンスはデフォルト値
 *   （制約なし＝従来通りグリッド全体まで自由にリサイズ可能）を返せばよい。
 * @param hideTopRightCorner 右上のリサイズハンドルを表示・処理しないウィジェットインスタンスを
 *   判定する関数。中身（[content]）が右上に独自の重要な当たり判定（削除バッジなど）を持つ
 *   場合、右上のリサイズハンドルと競合してしまうため、その種類だけリサイズを他の3つの角に
 *   譲る（右上以外の角からでも縦横どちらのサイズも変えられるため、機能的な制約にはならない）。
 * @param deleteZoneBoundsInRoot 「ここにドラッグして削除」ゾーンのルート座標系での範囲。
 *   移動ドラッグ中の指の位置がこの範囲に入ると、指を離したときに移動を確定する代わりに
 *   [onRequestDeleteConfirm]を呼ぶ。
 * @param onCellSizeMeasured グリッドのセル1つ分の実サイズ（dp）が測定・変化するたびに呼ばれる
 *   コールバック。呼び出し側が「新規ウィジェットを実サイズ（dp）に応じたセル数で配置したい」
 *   といった場合に、別途`BoxWithConstraints`で測り直さずに済むよう、ここで測定済みの値を渡す。
 * @param onLayoutChange 配置（追加・削除・移動・リサイズ）が変わったときのコールバック。
 * @param onRequestAddWidget 「+ ADD WIDGET」タイルがタップされたときのコールバック。
 * @param onWidgetLongClick ウィジェット本体（個々のスロット以外）が長押しされたときのコールバック
 *   （ウィジェット編集モードに入る）。
 * @param onExitWidgetEditMode ウィジェット編集モード中にウィジェット本体がタップされたときの
 *   コールバック。
 * @param onWidgetDragStateChanged ウィジェットの移動ドラッグの状態が変わるたびに呼ばれるコール
 *   バック（対象の[PlacedWidget]、ドラッグ中かどうか、現在[deleteZoneBoundsInRoot]の上にいるか
 *   どうか）。呼び出し側はこれを使って「ここにドラッグして削除」ゾーンの表示・非表示や、
 *   ホバー中のハイライトを切り替えられる。
 * @param onRequestDeleteConfirm 移動ドラッグの指を[deleteZoneBoundsInRoot]内で離したときの
 *   コールバック（対象の[PlacedWidget]）。呼び出し側はここで削除確認ダイアログを表示する想定で、
 *   実際の削除は呼び出し側が[onLayoutChange]で行う。
 * @param content 実際のウィジェットの中身を描画するスロット（[WidgetPanel]の種類、
 *   [WidgetPanel.APPWIDGET]の場合のみ意味を持つ`appWidgetId`（それ以外は-1）、
 *   [WidgetPanel.APP_SLOT_ICON_ONLY]/[WidgetPanel.APP_SLOT_NAMED]の場合のみ意味を持つ
 *   `instanceId`（それ以外は-1）、現在表示中の
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
    resizeConstraints: (widget: PlacedWidget) -> ResizeConstraints = { ResizeConstraints() },
    hideTopRightCorner: (widget: PlacedWidget) -> Boolean = { false },
    deleteZoneBoundsInRoot: Rect? = null,
    onCellSizeMeasured: (cellWidth: Dp, cellHeight: Dp) -> Unit = { _, _ -> },
    onLayoutChange: (List<PlacedWidget>) -> Unit,
    onRequestAddWidget: () -> Unit,
    onWidgetLongClick: () -> Unit,
    onExitWidgetEditMode: () -> Unit,
    onWidgetDragStateChanged: (widget: PlacedWidget, dragging: Boolean, overDeleteZone: Boolean) -> Unit = { _, _, _ -> },
    onRequestDeleteConfirm: (PlacedWidget) -> Unit = {},
    modifier: Modifier = Modifier,
    content: @Composable SharedTransitionScope.(WidgetPanel, Int, Int, Float, Float, Pair<Float, Float>?, Modifier, Boolean) -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val cellWidth = maxWidth / columns
        val cellHeight = maxHeight / rows
        val resolvedIconCellSizes = iconCellSizes(cellWidth, cellHeight)

        // 呼び出し側（新規ウィジェット追加時のサイズ決定など）がセル1つ分の実サイズ（dp）を
        // 知りたい場合があるが、それを測れるのはこのBoxWithConstraintsの中だけなので、
        // 測定結果をそのまま伝える
        LaunchedEffect(cellWidth, cellHeight) {
            onCellSizeMeasured(cellWidth, cellHeight)
        }

        placedWidgets.forEach { widget ->
            key(widget.instanceKey) {
                val otherWidgets = remember(placedWidgets) { placedWidgets.filter { it.instanceKey != widget.instanceKey } }
                // AppWidgetManagerへの問い合わせを毎フレーム走らせないよう、インスタンスごとに一度だけ解決する
                val resolvedResizeConstraints = remember(widget.instanceKey) { resizeConstraints(widget) }
                val resolvedHideTopRightCorner = remember(widget.instanceKey) { hideTopRightCorner(widget) }
                WidgetSlot(
                    widget = widget,
                    columns = columns,
                    rows = rows,
                    cellWidth = cellWidth,
                    cellHeight = cellHeight,
                    isWidgetEditMode = isWidgetEditMode,
                    otherWidgets = otherWidgets,
                    iconCellSize = resolvedIconCellSizes[widget.type],
                    resizeConstraints = resolvedResizeConstraints,
                    hideTopRightCorner = resolvedHideTopRightCorner,
                    deleteZoneBoundsInRoot = deleteZoneBoundsInRoot,
                    onMoved = { newCol, newRow ->
                        onLayoutChange(placedWidgets.map { if (it.instanceKey == widget.instanceKey) it.copy(col = newCol, row = newRow) else it })
                    },
                    onResized = { newCol, newRow, newColSpan, newRowSpan ->
                        onLayoutChange(
                            placedWidgets.map {
                                if (it.instanceKey == widget.instanceKey) {
                                    it.copy(col = newCol, row = newRow, colSpan = newColSpan, rowSpan = newRowSpan)
                                } else it
                            }
                        )
                    },
                    onWidgetLongClick = onWidgetLongClick,
                    onExitWidgetEditMode = onExitWidgetEditMode,
                    onDragStateChanged = { dragging, overDeleteZone -> onWidgetDragStateChanged(widget, dragging, overDeleteZone) },
                    onRequestDeleteConfirm = { onRequestDeleteConfirm(widget) }
                ) { liveColSpan, liveRowSpan, boxModifier, isResizing ->
                    content(widget.type, widget.appWidgetId, widget.instanceId, liveColSpan, liveRowSpan, resolvedIconCellSizes[widget.type], boxModifier, isResizing)
                }
            }
        }

        // 空いている領域があれば常に追加導線を表示する（外部ウィジェット＝APPWIDGETは複数配置が
        // 前提で「未配置の種類がない」状態にはならないため、既存4種の空き状況にかかわらず表示する）
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
    resizeConstraints: ResizeConstraints,
    hideTopRightCorner: Boolean,
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
    // ウィジェットが対応していない方向（resizeConstraints.axes）には、そもそも辺を動かさない
    // （AppWidgetのresizeModeがHORIZONTAL/VERTICAL/NONEの場合を反映する。4隅のハンドル自体は
    // 残るが、対応していない方向の辺はドラッグしても動かなくなる）
    val allowHorizontalResize = resizeConstraints.axes != ResizeAxes.NONE && resizeConstraints.axes != ResizeAxes.VERTICAL
    val allowVerticalResize = resizeConstraints.axes != ResizeAxes.NONE && resizeConstraints.axes != ResizeAxes.HORIZONTAL
    val widthDeltaPx = if (!allowHorizontalResize) 0f else if (leftEdgeMoves) -resizeDeltaPx.x else resizeDeltaPx.x
    val heightDeltaPx = if (!allowVerticalResize) 0f else if (topEdgeMoves) -resizeDeltaPx.y else resizeDeltaPx.y
    val rightEdge = widget.col + widget.colSpan
    val bottomEdge = widget.row + widget.rowSpan
    val maxColSpanFromGrid = if (leftEdgeMoves) rightEdge else (columns - widget.col)
    val maxRowSpanFromGrid = if (topEdgeMoves) bottomEdge else (rows - widget.row)
    // アイコン単位/1セルの最小値と、ウィジェット固有の実際の最小サイズ（dp→セル単位）の
    // どちらか大きい方を実際の最小値にする。ただしグリッドの空きを超える最小値にはしない
    // （coerceIn(min, max)はmin>maxで例外を投げるため、他ウィジェットとの兼ね合いでグリッドが
    // 狭い場合でもクラッシュしないようにする安全策）
    val providerMinColSpan = resizeConstraints.minSize?.let { it.width / cellWidth } ?: 0f
    val providerMinRowSpan = resizeConstraints.minSize?.let { it.height / cellHeight } ?: 0f
    val minColSpan = maxOf(iconWidthPx / cellWidthPx, providerMinColSpan).coerceAtMost(maxColSpanFromGrid)
    val minRowSpan = maxOf(iconHeightPx / cellHeightPx, providerMinRowSpan).coerceAtMost(maxRowSpanFromGrid)
    // ウィジェット固有の実際の最大サイズが指定されていれば、グリッド境界までの最大値との
    // どちらか小さい方を実際の最大値にする
    val providerMaxColSpan = resizeConstraints.maxSize?.let { it.width / cellWidth }
    val providerMaxRowSpan = resizeConstraints.maxSize?.let { it.height / cellHeight }
    val maxColSpan = (providerMaxColSpan?.coerceAtMost(maxColSpanFromGrid) ?: maxColSpanFromGrid).coerceAtLeast(minColSpan)
    val maxRowSpan = (providerMaxRowSpan?.coerceAtMost(maxRowSpanFromGrid) ?: maxRowSpanFromGrid).coerceAtLeast(minRowSpan)

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
        val cornerWidthDeltaPx = if (!allowHorizontalResize) 0f else if (cornerLeftEdgeMoves) -deltaPx.x else deltaPx.x
        val cornerHeightDeltaPx = if (!allowVerticalResize) 0f else if (cornerTopEdgeMoves) -deltaPx.y else deltaPx.y
        val cornerMaxColSpanFromGrid = if (cornerLeftEdgeMoves) rightEdge else (columns - widget.col)
        val cornerMaxRowSpanFromGrid = if (cornerTopEdgeMoves) bottomEdge else (rows - widget.row)
        val cornerMaxColSpan = (providerMaxColSpan?.coerceAtMost(cornerMaxColSpanFromGrid) ?: cornerMaxColSpanFromGrid).coerceAtLeast(minColSpan)
        val cornerMaxRowSpan = (providerMaxRowSpan?.coerceAtMost(cornerMaxRowSpanFromGrid) ?: cornerMaxRowSpanFromGrid).coerceAtLeast(minRowSpan)
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
    // ウィジェット全域が移動のドラッグ判定になる（四隅のリサイズハンドルの範囲を除く）。
    // 以前は上部の専用バーだけが移動ハンドルだったが、当たり判定が狭すぎたため本体全域に変更した
    val onBodyDragStart: () -> Unit = {
        lastValidMoveCol = widget.col
        lastValidMoveRow = widget.row
        onDragStateChanged(true, false)
    }
    val onBodyDrag: (position: Offset, amount: Offset) -> Unit = { position, amount ->
        dragOffsetPx += amount
        val candidate = snappedMoveCandidate(dragOffsetPx)
        if (otherWidgets.none { it.overlaps(candidate) }) {
            lastValidMoveCol = candidate.col
            lastValidMoveRow = candidate.row
        }
        val rootPosition = widgetBoxCoordinates?.localToRoot(position)
        isOverDeleteZone = deleteZoneBoundsInRoot != null && rootPosition != null &&
            deleteZoneBoundsInRoot.contains(rootPosition)
        onDragStateChanged(true, isOverDeleteZone)
    }
    val currentOnMoveDragEnd = rememberUpdatedState(onMoveDragEnd)
    val currentOnBodyDragStart = rememberUpdatedState(onBodyDragStart)
    val currentOnBodyDrag = rememberUpdatedState(onBodyDrag)
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
            // 編集モード中はウィジェット全域（四隅のリサイズハンドルの範囲を除く）が移動の
            // ドラッグ判定になる。編集モードでないときは、個々のスロット（アプリアイコン・
            // ボタンなど）が独自にタップ/長押しを処理していない「余白」部分でのみ、長押しで
            // 編集モードに入る検出が働く（子のクリック領域が先に消費するため、ここには
            // 落ちてこない）。ただし、動きの有無にかかわらず必ずイベントを消費する必要がある：
            // 消費せずにいると、その未消費のドラッグが祖先のSurfaceにある背景ドラッグ検出
            // （detectDragGestures）にまで届いてしまい、編集モードが意図せず解除されてしまう
            // 不具合があったため
            //
            // リサイズハンドルの範囲内から始まったジェスチャーは、その独自のpointerInputに
            // 完全に委ねる。当初は「子が先にconsumeしたかどうか」で本体側が身を引く
            // （wasHijackedByChild）方式だったが、これは子と本体のどちらが先にイベントを
            // 消費するかという実装依存の競合に頼っており、実機によってはこの競合の決着が
            // 安定せず、本体側が先にすべて消費してしまってハンドルが完全に無反応になる
            // 不具合があった。そのため、ここでは競合に頼らず、ダウン位置が事前に計測してある
            // 各ハンドルの範囲内かどうかを最初に判定し、範囲内であれば本体側は一切手を出さず
            // （consumeもせず）即座に手を引くようにしている。
            //
            // 中身（QUICK ACCESSのボタン等）が独自にクリックを処理している領域の上から
            // ドラッグ・長押しを始めた場合でも、必ずウィジェット側の移動・編集モード突入として
            // 認識できるようにするため、PointerEventPass.Initial（祖先→子の順）で先に観測する。
            // ここで単純なタップだったと判明するまでは一切consumeしないため、動きの小さい
            // 普通のタップは今まで通り中身のクリックとして正常に発火する。ドラッグ（スロップ超え）
            // または長押し（タイムアウト到達）と判断した瞬間にだけconsumeし、以降のイベントを
            // ウィジェット側が奪い取る。子のMainパス処理より必ず先に観測できるため、子の
            // クリック判定に先を越されることはない
            .pointerInput(widget.instanceKey, isWidgetEditMode) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)

                    if (currentIsWidgetEditMode.value) {
                        val downRoot = widgetBoxCoordinates?.localToRoot(down.position)
                        val isOnHandle = downRoot != null && cornerCoordinates.values.any { it.boundsInRoot().contains(downRoot) }
                        if (isOnHandle) return@awaitEachGesture

                        val touchSlop = viewConfiguration.touchSlop
                        var totalDistance = 0f
                        var dragStarted = false
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!dragStarted && change.isConsumed) break
                            val delta = change.position - change.previousPosition
                            totalDistance += delta.getDistance()
                            if (!dragStarted && totalDistance >= touchSlop) {
                                dragStarted = true
                                currentOnBodyDragStart.value()
                            }
                            if (dragStarted) {
                                change.consume()
                                currentOnBodyDrag.value(change.position, delta)
                            }
                            if (!change.pressed) break
                        }
                        if (dragStarted) {
                            currentOnMoveDragEnd.value()
                        } else {
                            // 指がほぼ動かなかった場合はタップとみなし、編集モードを抜ける
                            currentOnExitWidgetEditMode.value()
                        }
                    } else {
                        val downTime = down.uptimeMillis
                        val longPressTimeoutMillis = viewConfiguration.longPressTimeoutMillis
                        var isLongPress = false
                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (change.isConsumed) break
                            if (change.uptimeMillis - downTime >= longPressTimeoutMillis) {
                                change.consume()
                                isLongPress = true
                                break
                            }
                            if (!change.pressed) break
                        }
                        if (isLongPress) {
                            currentOnWidgetLongClick.value()
                        }
                    }
                }
            }
    ) {
        content(displayColSpan, displayRowSpan, Modifier.fillMaxSize(), isResizing)

        if (isWidgetEditMode) {
            // 移動グリップ（上部中央、見た目のみ。ドラッグ判定自体はウィジェット全域が持つため、
            // このグリップ自体には当たり判定を持たせていない）
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 4.dp)
                    .width(28.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.accent.copy(alpha = 0.8f))
            )

            // 四隅のリサイズハンドル。どこをつまんでも、その角を固定点として反対側の辺が伸縮する。
            // リサイズに一切対応していないウィジェット（resizeConstraints.axes == NONE）では、
            // そもそもハンドル自体を表示しない（移動は引き続きウィジェット全域から行える）。
            // hideTopRightCornerがtrueの種類は、右上を中身側の当たり判定（削除バッジなど）に
            // 譲り、残り3つの角からリサイズする
            val visibleCorners = when {
                resizeConstraints.axes == ResizeAxes.NONE -> emptyList()
                hideTopRightCorner -> ResizeCorner.entries.filter { it != ResizeCorner.TOP_RIGHT }
                else -> ResizeCorner.entries
            }
            for (corner in visibleCorners) {
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
