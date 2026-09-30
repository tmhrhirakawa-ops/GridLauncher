package com.example.gridlauncher.ui.screens

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.gridlauncher.model.PlacedWidget
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.sections.ResizeAxes
import com.example.gridlauncher.ui.sections.ResizeConstraints
import com.example.gridlauncher.util.WidgetLayoutMode
import com.example.gridlauncher.util.findFreeGridSlot
import com.example.gridlauncher.util.findFreeGridSlotForSize

// ホーム画面のウィジェットの配置に関する決まりと、新しく置く場所・大きさの計算。
// CyberLauncherScreen から、画面の状態に依存しない部分を切り出したもの。

/**
 * 他アプリのAppWidgetが申告する最小サイズ（[AppWidgetProviderInfo.minResizeWidth]等）を
 * 何倍まで許容するか。1.0だと申告値を厳密に守るが、大きめの最小値を申告しているウィジェットが
 * 他のランチャーに比べてかなり大きく見えてしまうため、画質が粗くなるリスクと引き換えに
 * 半分まではリサイズできるようにする。
 */
private const val MinSizeRelaxFactor = 0.5f

/** ウィジェット種類のうち、常に1個までしか同時配置できないもの（それ以外は複数配置できる）。 */
internal val SingleInstanceWidgetPanels = setOf(
    WidgetPanel.ACCESS_GRID, WidgetPanel.CALENDAR, WidgetPanel.DEVICE_STATUS, WidgetPanel.QUICK_ACCESS,
    WidgetPanel.CLOCK, WidgetPanel.BATTERY, WidgetPanel.NOW_PLAYING
)

/** PROを購入していないと追加できないウィジェットの種類。 */
internal val ProOnlyWidgetPanels = setOf(WidgetPanel.NOW_PLAYING)

/** PROを購入していない場合に配置できる、外部ウィジェットの数。 */
internal const val FreeAppWidgetLimit = 2

/** APP SLOT（単体ウィジェット）を新規追加するときの、見た目として妥当な初期サイズ（dp）。 */
private val AppSlotIconOnlyTargetSize = DpSize(60.dp, 60.dp)
private val AppSlotNamedTargetSize = DpSize(140.dp, 64.dp)

/** APP SLOT（単体ウィジェット）の種類かどうか。 */
internal val WidgetPanel.isAppSlot: Boolean
    get() = this == WidgetPanel.APP_SLOT_ICON_ONLY || this == WidgetPanel.APP_SLOT_NAMED

/** APP SLOTを新規追加するときの初期サイズ（dp）。 */
internal fun appSlotTargetSize(type: WidgetPanel): DpSize =
    if (type == WidgetPanel.APP_SLOT_ICON_ONLY) AppSlotIconOnlyTargetSize else AppSlotNamedTargetSize

/**
 * APP LIST内部のアプリ一覧の基準列数・行数（列, 行）。画面モードごとに従来の「Mサイズ」と同じ値を使う。
 * ウィジェット自体がリサイズされた場合は、AccessGridSection側が実際の描画サイズを見て、スロットが
 * 窮屈になりすぎなければ減らし、間延びしすぎるようなら増やす形でこの基準値から調整する
 */
internal fun accessGridBaseSize(mode: WidgetLayoutMode): Pair<Int, Int> = when (mode) {
    WidgetLayoutMode.SMALL_PORTRAIT -> 3 to 3
    WidgetLayoutMode.LARGE_PORTRAIT -> 4 to 3
    WidgetLayoutMode.LANDSCAPE -> 3 to 5
}

/**
 * ウィジェットが実際に許容する最小サイズ（[MinSizeRelaxFactor]適用後、dp単位）を求める。
 * リサイズの下限（[appWidgetResizeConstraints]）と、新規追加時の「画面に入り切るか」判定の
 * 両方で同じ基準を使うための共通関数。
 */
private fun relaxedMinSizeDp(info: AppWidgetProviderInfo): DpSize {
    val declaredMinWidth = if (info.minResizeWidth > 0) info.minResizeWidth else info.minWidth
    val declaredMinHeight = if (info.minResizeHeight > 0) info.minResizeHeight else info.minHeight
    return DpSize((declaredMinWidth * MinSizeRelaxFactor).dp, (declaredMinHeight * MinSizeRelaxFactor).dp)
}

/**
 * ウィジェットのリサイズの制約。他アプリのAppWidgetは、種類（WidgetPanel.APPWIDGET）ではなく
 * インスタンス（appWidgetId）ごとに実際の最小/最大サイズ・対応するリサイズ方向が異なるため、
 * AppWidgetProviderInfoから解決する。GridLauncher内蔵のウィジェットはデフォルト値（制約なし）のままでよい
 */
internal fun appWidgetResizeConstraints(context: Context, widget: PlacedWidget): ResizeConstraints {
    if (widget.type != WidgetPanel.APPWIDGET) return ResizeConstraints()
    val info = AppWidgetManager.getInstance(context).getAppWidgetInfo(widget.appWidgetId)
        ?: return ResizeConstraints()
    // ウィジェットが申告する最小サイズを厳密に守ると、Claudeのように大きめの最小値を
    // 申告しているウィジェットが他ランチャーに比べてかなり大きく見えてしまうため、
    // 申告値の半分まではリサイズを許容する（画質が粗くなるリスクとのトレードオフ）
    val minSize = relaxedMinSizeDp(info)
    val maxSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        info.maxResizeWidth > 0 && info.maxResizeHeight > 0
    ) {
        DpSize(info.maxResizeWidth.dp, info.maxResizeHeight.dp)
    } else {
        null
    }
    val axes = when {
        info.resizeMode and AppWidgetProviderInfo.RESIZE_BOTH == AppWidgetProviderInfo.RESIZE_BOTH -> ResizeAxes.BOTH
        info.resizeMode and AppWidgetProviderInfo.RESIZE_HORIZONTAL != 0 -> ResizeAxes.HORIZONTAL
        info.resizeMode and AppWidgetProviderInfo.RESIZE_VERTICAL != 0 -> ResizeAxes.VERTICAL
        else -> ResizeAxes.NONE
    }
    return ResizeConstraints(minSize = minSize, maxSize = maxSize, axes = axes)
}

/**
 * ウィジェットが許容する最小サイズ（相対緩和後）でも、このグリッドの列数・行数に収まらない
 * 場合はfalse。セルの大きさ[cellSize]がまだ測定できていない場合は判断できないため許可扱いにする
 * （実際に配置しようとするタイミング＝[slotForNewAppWidget]で改めてチェックする）
 */
internal fun appWidgetFitsOnScreen(info: AppWidgetProviderInfo, cellSize: DpSize?, mode: WidgetLayoutMode): Boolean {
    if (cellSize == null) return true
    val minSize = relaxedMinSizeDp(info)
    val minColSpan = minSize.width / cellSize.width
    val minRowSpan = minSize.height / cellSize.height
    return minColSpan <= mode.columns && minRowSpan <= mode.rows
}

/**
 * 新しく追加する外部ウィジェットを置く場所（列, 行, 列数, 行数）。空きがなければnull。
 *
 * 種類ごとの決め打ちの大きさではなく、ウィジェットの推奨サイズ（AppWidgetProviderInfo.minWidth/minHeight）に
 * 応じたセル数で配置する。推奨サイズで空きがなければ、許容する最小サイズ（緩和後）まで縮めて再挑戦する
 */
internal fun slotForNewAppWidget(
    info: AppWidgetProviderInfo?,
    cellSize: DpSize?,
    placedWidgets: List<PlacedWidget>,
    mode: WidgetLayoutMode
): FloatArray? {
    if (info == null || cellSize == null) return slotForDefaultSize(placedWidgets, mode)
    val preferredColSpan = info.minWidth.dp / cellSize.width
    val preferredRowSpan = info.minHeight.dp / cellSize.height
    val relaxedMinSize = relaxedMinSizeDp(info)
    val minColSpan = relaxedMinSize.width / cellSize.width
    val minRowSpan = relaxedMinSize.height / cellSize.height
    return findFreeGridSlotForSize(placedWidgets, mode.columns, mode.rows, preferredColSpan, preferredRowSpan)
        ?: findFreeGridSlotForSize(placedWidgets, mode.columns, mode.rows, minColSpan, minRowSpan)
}

/**
 * 「+ ADD WIDGET」から新しく追加するウィジェットを置く場所（列, 行, 列数, 行数）。空きがなければnull。
 * APP SLOTはアイコン1個分の見た目の大きさに合わせ、それ以外は標準の大きさで置く
 */
internal fun slotForNewWidget(
    type: WidgetPanel,
    cellSize: DpSize?,
    placedWidgets: List<PlacedWidget>,
    mode: WidgetLayoutMode
): FloatArray? {
    if (!type.isAppSlot || cellSize == null) return slotForDefaultSize(placedWidgets, mode)
    val targetSize = appSlotTargetSize(type)
    return findFreeGridSlotForSize(
        placedWidgets, mode.columns, mode.rows,
        targetSize.width / cellSize.width, targetSize.height / cellSize.height
    )
}

private fun slotForDefaultSize(placedWidgets: List<PlacedWidget>, mode: WidgetLayoutMode): FloatArray? =
    findFreeGridSlot(placedWidgets, mode.columns, mode.rows)?.let {
        floatArrayOf(it[0].toFloat(), it[1].toFloat(), it[2].toFloat(), it[3].toFloat())
    }
