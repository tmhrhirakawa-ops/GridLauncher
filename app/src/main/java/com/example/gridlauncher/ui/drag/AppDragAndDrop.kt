package com.example.gridlauncher.ui.drag

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.model.FolderInfo
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.ui.theme.LocalCyberColors
import kotlinx.coroutines.delay

/** ドラッグを始めた場所。 */
sealed interface AppDragSource {
    /** APP LIST（ACCESS GRID）の[index]番目のスロット。 */
    data class Grid(val index: Int) : AppDragSource

    /** DOCKの[index]番目のスロット。 */
    data class Dock(val index: Int) : AppDragSource

    /** QUICK ACCESSの[index]番目のスロット。 */
    data class QuickAccess(val index: Int) : AppDragSource

    /** フォルダ（[folderId]）の中の[index]番目のスロット。 */
    data class FolderSlot(val folderId: String, val index: Int) : AppDragSource

    /** アプリドロワー（ALL APPS）。スロットには属さない。 */
    data object Drawer : AppDragSource
}

/** ドラッグしているもの。 */
sealed interface AppDragItem {
    data class App(val appInfo: AppInfo) : AppDragItem
    data class Folder(val folder: FolderInfo) : AppDragItem
    data class QuickAction(val actionId: QuickActionId) : AppDragItem
}

/** ドロップ先。 */
sealed interface AppDropTarget {
    /** APP LIST（ACCESS GRID）の[index]番目のスロット。 */
    data class GridSlot(val index: Int) : AppDropTarget

    /** DOCKの[index]番目のスロット。 */
    data class DockSlot(val index: Int) : AppDropTarget

    /** QUICK ACCESSの[index]番目のスロット。 */
    data class QuickSlot(val index: Int) : AppDropTarget

    /** 開いているフォルダ（[folderId]）の中の[index]番目のスロット。 */
    data class FolderSlot(val folderId: String, val index: Int) : AppDropTarget

    /**
     * 開いているフォルダのポップアップ全体。スロット以外の余白に落としても、下に隠れている
     * APP LISTのスロットにドロップされないよう、ここで受け止める（何もしない）。
     */
    data object FolderPanel : AppDropTarget

    /** 画面上部の「削除」エリア（スロットから外す。アプリ自体は残る）。 */
    data object RemoveZone : AppDropTarget

    /** 画面上部の「アンインストール」エリア。 */
    data object UninstallZone : AppDropTarget
}

/** ドラッグ中のアイテムとその出どころ。 */
data class AppDragPayload(val source: AppDragSource, val item: AppDragItem) {
    /** 画面上部の「削除」エリアを使えるか（アプリドロワーのアプリはどのスロットにも属さないため不可）。 */
    val canRemove: Boolean get() = source != AppDragSource.Drawer

    /** 画面上部の「アンインストール」エリアを使えるか（アプリのみ）。 */
    val canUninstall: Boolean get() = item is AppDragItem.App

    /** [target]にドロップできるかどうか。できないドロップ先は強調もしない。 */
    fun accepts(target: AppDropTarget): Boolean = when (target) {
        is AppDropTarget.GridSlot -> item is AppDragItem.App || item is AppDragItem.Folder
        is AppDropTarget.DockSlot -> item is AppDragItem.App
        is AppDropTarget.QuickSlot -> item is AppDragItem.QuickAction
        // 開いているフォルダの中での並べ替えのみ（フォルダが開いている間、他の場所からは持ち込めない）
        is AppDropTarget.FolderSlot -> item is AppDragItem.App &&
            (source as? AppDragSource.FolderSlot)?.folderId == target.folderId
        AppDropTarget.FolderPanel -> source is AppDragSource.FolderSlot
        AppDropTarget.RemoveZone -> canRemove
        AppDropTarget.UninstallZone -> canUninstall
    }
}

/**
 * アプリアイコン・フォルダのドラッグ＆ドロップの状態。
 *
 * アプリドロワー（ボトムシート）はホーム画面とは別のウィンドウに描画されるため、指の位置と
 * ドロップ先の範囲はウィンドウに依存しない画面全体の座標（スクリーン座標）で扱う。
 * ドロップ先は、各スロット・削除エリアが[appDropTarget]で自分の範囲を登録しておき、
 * 指の位置から当たり判定する。
 */
@Stable
class AppDragState internal constructor() {
    /** ドラッグ中のアイテム。ドラッグしていないときはnull。 */
    var payload by mutableStateOf<AppDragPayload?>(null)
        private set

    /** 指の位置（スクリーン座標）。 */
    var pointerOnScreen by mutableStateOf(Offset.Zero)
        private set

    /** 指が今重なっているドロップ先。 */
    var hoveredTarget by mutableStateOf<AppDropTarget?>(null)
        private set

    /**
     * フォルダの中から持ち上げたアプリを、一度ポップアップの外へ持ち出したかどうか。
     * 持ち出した後は、そのドラッグが終わるまでポップアップを隠したままにし、ポップアップの
     * 下に隠れていたスロットにも置けるよう、フォルダの中のスロットを当たり判定から外す。
     */
    var isCarriedOutOfFolder by mutableStateOf(false)
        private set

    val isDragging: Boolean get() = payload != null

    internal var onDrop: (AppDragPayload, AppDropTarget?) -> Unit = { _, _ -> }

    // ドロップ先ごとの範囲（スクリーン座標）。当たり判定にだけ使うため、状態（State）にはしない
    private val targetBounds = HashMap<AppDropTarget, Rect>()

    // 長押し→ドラッグで持ち上げられる要素の範囲（スクリーン座標）。ウィジェット本体の
    // 長押し判定（ウィジェット編集モード）より、これらの要素の長押しを優先させるために使う
    private val sourceBounds = HashMap<Any, Rect>()

    internal fun registerSource(key: Any, bounds: Rect) {
        sourceBounds[key] = bounds
    }

    internal fun unregisterSource(key: Any) {
        sourceBounds.remove(key)
    }

    /** [positionOnScreen]が、長押し→ドラッグで持ち上げられる要素の上かどうか。 */
    fun isOverDragSource(positionOnScreen: Offset): Boolean =
        sourceBounds.values.any { it.contains(positionOnScreen) }

    internal fun registerTarget(target: AppDropTarget, bounds: Rect) {
        targetBounds[target] = bounds
    }

    internal fun unregisterTarget(target: AppDropTarget) {
        targetBounds.remove(target)
        if (hoveredTarget == target) hoveredTarget = null
    }

    internal fun start(payload: AppDragPayload, positionOnScreen: Offset) {
        this.payload = payload
        moveTo(positionOnScreen)
    }

    internal fun moveTo(positionOnScreen: Offset) {
        pointerOnScreen = positionOnScreen
        refreshHover()
    }

    /** ページ送りなどで、指は動いていないがドロップ先の位置が変わったときに当たり判定をやり直す。 */
    internal fun refreshHover() {
        val currentPayload = payload ?: run {
            hoveredTarget = null
            return
        }
        val position = pointerOnScreen
        // 重なって表示されるものほど優先して判定する。削除エリアはヘッダーの上に、フォルダの
        // ポップアップはAPP LISTの上に重ねて表示するため、その下にあるスロットより先に見る
        val hovered = targetBounds.entries
            .filter { (target, bounds) ->
                bounds.contains(position) && currentPayload.accepts(target) &&
                    !(isCarriedOutOfFolder && target.isInsideFolder)
            }
            .minByOrNull { (target, _) -> target.hitPriority }
            ?.key
        // フォルダの中から持ち上げたアプリが、ポップアップの外に出た時点で「持ち出した」とみなす
        if (currentPayload.source is AppDragSource.FolderSlot && hovered?.isInsideFolder != true) {
            isCarriedOutOfFolder = true
        }
        hoveredTarget = hovered
    }

    internal fun drop() {
        val droppedPayload = payload ?: return
        val target = hoveredTarget
        reset()
        onDrop(droppedPayload, target)
    }

    internal fun cancel() {
        reset()
    }

    private fun reset() {
        payload = null
        hoveredTarget = null
        isCarriedOutOfFolder = false
    }

    /** 開いているフォルダのポップアップの中のドロップ先かどうか。 */
    private val AppDropTarget.isInsideFolder: Boolean
        get() = this is AppDropTarget.FolderSlot || this == AppDropTarget.FolderPanel

    /** 当たり判定の優先度（小さいほど優先）。 */
    private val AppDropTarget.hitPriority: Int
        get() = when (this) {
            AppDropTarget.RemoveZone, AppDropTarget.UninstallZone -> 0
            is AppDropTarget.FolderSlot -> 1
            AppDropTarget.FolderPanel -> 2
            else -> 3
        }
}

/**
 * ホーム画面全体で共有するドラッグ＆ドロップの状態。未提供の場所（プレビュー等）ではnullになり、
 * ドラッグ関連の修飾子は何もしない。
 */
val LocalAppDragState = staticCompositionLocalOf<AppDragState?> { null }

/**
 * [AppDragState]を作る。[onDrop]はドロップ先が決まったとき（どこにも重なっていなければnull）に呼ばれる。
 */
@Composable
fun rememberAppDragState(onDrop: (AppDragPayload, AppDropTarget?) -> Unit): AppDragState {
    val currentOnDrop by rememberUpdatedState(onDrop)
    return remember {
        AppDragState().apply { this.onDrop = { payload, target -> currentOnDrop(payload, target) } }
    }
}

/**
 * レイアウトの範囲を、祖先（ページャーなど）の表示範囲で切り取ったうえでスクリーン座標で返す。
 * 画面外のページにあるスロットに誤ってドロップされないよう、切り取った範囲を使う。
 */
internal fun LayoutCoordinates.boundsOnScreen(): Rect {
    val windowOffsetOnScreen = localToScreen(Offset.Zero) - localToWindow(Offset.Zero)
    return boundsInWindow().translate(windowOffsetOnScreen)
}

/**
 * この要素を長押し→ドラッグで持ち上げられるようにする。[payload]は長押しが成立した時点で
 * 呼ばれ、nullを返した場合はドラッグしない。
 *
 * 長押し時の処理（編集モードに入るなど）は要素側のcombinedClickableに任せ、ここでは長押しの
 * 成立を独自に判定して、その後の指の動きを追う。指を動かさずに離した場合は、元の場所にドロップしたものとして扱われる。
 */
@Composable
fun Modifier.appDragSource(payload: () -> AppDragPayload?): Modifier {
    val dragState = LocalAppDragState.current ?: return this
    val currentPayload by rememberUpdatedState(payload)
    val coordinatesHolder = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val sourceKey = remember { Any() }
    DisposableEffect(dragState, sourceKey) {
        onDispose { dragState.unregisterSource(sourceKey) }
    }
    return this
        .onGloballyPositioned {
            coordinatesHolder[0] = it
            dragState.registerSource(sourceKey, it.boundsOnScreen())
        }
        .pointerInput(dragState) {
            // 要素側のcombinedClickableは、長押しが成立すると指を離すまでの移動イベントをすべて
            // 消費してしまう（detectTapGesturesのconsumeUntilUp）。子→親の順に届くMainパスで
            // 待っていると、その消費によってドラッグが打ち切られてしまうため、親→子の順に届く
            // Initialパスで先に観測する。長押しが成立するまでは一切消費しないため、タップや
            // ページャーのスワイプはこれまで通り要素側・ページャー側で処理される
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val touchSlop = viewConfiguration.touchSlop
                // 長押しのタイムアウトまで、指を離したり大きく動かしたりしなければ長押しとみなす
                val interrupted = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: return@withTimeoutOrNull true
                        if (!change.pressed) return@withTimeoutOrNull true
                        if ((change.position - down.position).getDistance() > touchSlop) return@withTimeoutOrNull true
                    }
                }
                if (interrupted == true) return@awaitEachGesture

                val coordinates = coordinatesHolder[0] ?: return@awaitEachGesture
                val dragPayload = currentPayload() ?: return@awaitEachGesture
                if (dragState.isDragging) return@awaitEachGesture
                dragState.start(dragPayload, coordinates.localToScreen(down.position))

                // 長押し成立後は、指を離すまでの移動をこちらで消費して追いかける。途中で要素が
                // 破棄されるなどしてジェスチャーが中断された場合も、ドラッグ状態が残らないようにする
                var finished = false
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            finished = true
                            dragState.drop()
                            break
                        }
                        change.consume()
                        coordinatesHolder[0]?.let { dragState.moveTo(it.localToScreen(change.position)) }
                    }
                } finally {
                    if (!finished) dragState.cancel()
                }
            }
        }
}

/**
 * この要素をドロップ先として登録する。ドラッグ中に指が重なっている間は、アクセントカラーの
 * 枠線で強調する（描画フェーズでのみ状態を読むため、再コンポジションは起きない）。
 */
@Composable
fun Modifier.appDropTarget(target: AppDropTarget, highlight: Boolean = true): Modifier {
    val dragState = LocalAppDragState.current ?: return this
    DisposableEffect(dragState, target) {
        onDispose { dragState.unregisterTarget(target) }
    }
    val accent = LocalCyberColors.current.accent
    return this
        .onGloballyPositioned { dragState.registerTarget(target, it.boundsOnScreen()) }
        .then(
            if (highlight) {
                Modifier.drawWithContent {
                    drawContent()
                    if (dragState.hoveredTarget == target) {
                        val strokeWidth = 2.dp.toPx()
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                            size = size.copy(width = size.width - strokeWidth, height = size.height - strokeWidth),
                            cornerRadius = CornerRadius(4.dp.toPx()),
                            style = Stroke(width = strokeWidth)
                        )
                    }
                }
            } else {
                Modifier
            }
        )
}

/**
 * ドラッグ中に指をこの要素（ページャー）の左右の端に持っていくと、一定時間ごとに隣のページへ
 * 自動でスクロールする。
 */
@Composable
fun Modifier.dragEdgeAutoScroll(pagerState: PagerState): Modifier {
    val dragState = LocalAppDragState.current ?: return this
    var bounds by remember { mutableStateOf<Rect?>(null) }
    val minEdgeWidthPx = with(LocalDensity.current) { 24.dp.toPx() }
    // -1: 左端、1: 右端、0: 端ではない
    val edgeDirection by remember(dragState) {
        derivedStateOf {
            val area = bounds
            if (!dragState.isDragging || area == null || area.isEmpty) return@derivedStateOf 0
            val position = dragState.pointerOnScreen
            if (position.y < area.top || position.y > area.bottom) return@derivedStateOf 0
            val edgeWidth = maxOf(minEdgeWidthPx, area.width * 0.12f)
            when {
                position.x in area.left..(area.left + edgeWidth) -> -1
                position.x in (area.right - edgeWidth)..area.right -> 1
                else -> 0
            }
        }
    }
    LaunchedEffect(edgeDirection) {
        if (edgeDirection == 0) return@LaunchedEffect
        // 端に触れただけで即座にめくれないよう、少し留まってからスクロールする
        delay(400)
        while (true) {
            val targetPage = (pagerState.currentPage + edgeDirection).coerceIn(0, pagerState.pageCount - 1)
            if (targetPage == pagerState.currentPage) break
            pagerState.animateScrollToPage(targetPage)
            dragState.refreshHover()
            delay(600)
        }
    }
    return onGloballyPositioned { bounds = it.boundsOnScreen() }
}
