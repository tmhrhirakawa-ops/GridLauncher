package com.example.gridlauncher.ui.sections

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.ui.components.DockAppCard
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

/**
 * よく使うアプリを表示するボトムドックセクション。
 *
 * 1ページに[slotsPerPage]個ぴったりが画面内に収まるサイズでスロットを均等配置し、
 * それを超える分はページとして横にスワイプ（スナップ）して切り替える
 * （無段階の自由スクロールにはしない）。
 *
 * アプリは長押し→ドラッグで移動・削除する（[com.example.gridlauncher.ui.drag]参照）。
 *
 * @param apps 表示するアプリのリスト。
 * @param slotsPerPage 1ページに並べるスロット数（カスタマイズ画面で設定）。
 * @param pageCount ページ数（カスタマイズ画面で設定。アプリが入っているページ数以上）。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param activeNotifications 通知（またはアプリバッジ）が来ているアプリのパッケージ名と件数のマップ。
 * @param useOriginalIconColors trueの場合、アイコンをアクセントカラーのデュオトーン加工をせず、
 *   アプリ本来の色のまま表示する。
 * @param onAddClick 空きスロットがクリックされたときのコールバック。
 */
@Composable
fun BottomDockSection(
    apps: List<AppInfo?>,
    slotsPerPage: Int,
    pageCount: Int,
    isWallpaperMode: Boolean = false,
    activeNotifications: Map<String, Int> = emptyMap(),
    useOriginalIconColors: Boolean = false,
    onAddClick: (Int) -> Unit
) {
    val context = LocalContext.current
    val dragState = LocalAppDragState.current

    // 1スロット分。どのスロットもドロップ先にし、アプリがあるスロットは長押し→ドラッグで持ち上げられる
    val dockSlot: @Composable RowScope.(Int) -> Unit = { index ->
        val slotModifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .appDropTarget(AppDropTarget.DockSlot(index))
            .graphicsLayer {
                // 持ち上げ中のスロットは薄く表示して「抜けた」ことを示す
                alpha = if (dragState?.payload?.source == AppDragSource.Dock(index)) 0.3f else 1f
            }
        val appInfo = apps.getOrNull(index)
        if (appInfo != null) {
            DockAppCard(
                name = appInfo.label,
                packageName = appInfo.packageName,
                isMonochrome = appInfo.iconIsMonochrome,
                icon = appInfo.icon,
                modifier = slotModifier.appDragSource {
                    AppDragPayload(AppDragSource.Dock(index), AppDragItem.App(appInfo))
                },
                notificationCount = activeNotifications[appInfo.packageName] ?: 0,
                isWallpaperMode = isWallpaperMode,
                useOriginalIconColors = useOriginalIconColors,
                onClick = {
                    context.packageManager.getLaunchIntentForPackage(appInfo.packageName)?.let {
                        context.startActivity(it)
                    }
                }
            )
        } else {
            // 空きスロット（タップでアプリ追加）
            Surface(
                onClick = { onAddClick(index) },
                shape = RoundedCornerShape(4.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, LocalCyberColors.current.border),
                modifier = slotModifier
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("EMPTY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.3f))
                }
            }
        }
    }

    // slotsPerPage個ぴったりが画面幅に収まる均等サイズで並べ、それを超える分は
    // ページ送り（スワイプでスナップ）にする（無段階スクロールにはしない）
    val pages = pageCount.coerceAtLeast(1)
    val pagerState = rememberPagerState(pageCount = { pages })
    // アイコン数が多いときは、アイコンを小さくしすぎないよう間隔を詰める
    val slotSpacing = if (slotsPerPage > 5) 8.dp else 12.dp

    HorizontalPager(
        state = pagerState,
        pageSpacing = slotSpacing,
        // 1ページだけのときはスワイプしても動かないようにする
        userScrollEnabled = pages > 1,
        // ドラッグ中に端でページ送りしても、ドラッグ元のスロットが破棄されないよう全ページを保持する
        beyondViewportPageCount = pages - 1,
        modifier = Modifier.fillMaxWidth().height(60.dp).dragEdgeAutoScroll(pagerState)
    ) { page ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(slotSpacing),
            modifier = Modifier.fillMaxSize()
        ) {
            for (col in 0 until slotsPerPage) {
                dockSlot(page * slotsPerPage + col)
            }
        }
    }
}
