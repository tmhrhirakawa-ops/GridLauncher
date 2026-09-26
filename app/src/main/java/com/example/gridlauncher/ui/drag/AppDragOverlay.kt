package com.example.gridlauncher.ui.drag

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.components.rememberAppIconBitmap
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import kotlin.math.roundToInt

/** 指についてくるアイコンの一辺の大きさ。 */
private val DraggedIconSize = 56.dp

/** 削除系エリアの強調色（ウィジェットの「ここにドラッグして削除」と同じ危険色）。 */
private val DangerColor = Color(0xFFFF3B4E)

/**
 * ドラッグ中に画面へ重ねて表示するもの一式。指についてくるアイコンと、画面上部の
 * 「削除」「アンインストール」エリアを描画する。タッチは受け取らない（ドラッグ中の指は
 * ドラッグを始めた要素が追い続けている）。
 *
 * @param useOriginalIconColors アプリアイコンを本来の色で表示するかどうか。
 */
@Composable
fun AppDragOverlay(useOriginalIconColors: Boolean) {
    val dragState = LocalAppDragState.current ?: return
    val payload = dragState.payload
    val overlayCoordinates = remember { arrayOfNulls<LayoutCoordinates>(1) }

    Box(modifier = Modifier.fillMaxSize().onGloballyPositioned { overlayCoordinates[0] = it }) {
        // 画面上部の削除エリア。ドラッグ中だけ上からスライドして出てくる
        AnimatedVisibility(
            visible = payload != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            // 閉じるアニメーション中もエリアの構成が変わらないよう、最後のpayloadで判断する
            val lastPayload = remember { arrayOfNulls<AppDragPayload>(1) }
            if (payload != null) lastPayload[0] = payload
            val shownPayload = lastPayload[0]
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // アプリドロワーから持ってきたアプリはどのスロットにも属していないため「削除」は出さない
                if (shownPayload?.canRemove == true) {
                    DropZone(
                        target = AppDropTarget.RemoveZone,
                        icon = Icons.Outlined.RemoveCircleOutline,
                        label = "削除",
                        modifier = Modifier.weight(1f)
                    )
                }
                // アンインストールできるのはアプリだけ（フォルダ・QUICK ACCESSのボタンには出さない）
                if (shownPayload?.canUninstall == true) {
                    DropZone(
                        target = AppDropTarget.UninstallZone,
                        icon = Icons.Filled.Delete,
                        label = "アンインストール",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 指についてくるアイコン。位置は描画直前に読むことで、指の移動のたびに再コンポジションしない
        if (payload != null) {
            val halfSizePx = with(LocalDensity.current) { (DraggedIconSize / 2).toPx() }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset {
                        val local = overlayCoordinates[0]?.screenToLocal(dragState.pointerOnScreen) ?: Offset.Zero
                        IntOffset((local.x - halfSizePx).roundToInt(), (local.y - halfSizePx).roundToInt())
                    }
                    .size(DraggedIconSize)
                    .graphicsLayer {
                        scaleX = 1.1f
                        scaleY = 1.1f
                        alpha = 0.9f
                    }
            ) {
                DraggedItemIcon(item = payload.item, useOriginalIconColors = useOriginalIconColors)
            }
        }
    }
}

@Composable
private fun DraggedItemIcon(item: AppDragItem, useOriginalIconColors: Boolean) {
    val colors = LocalCyberColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.panel,
        border = BorderStroke(1.dp, colors.accent),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(contentAlignment = Alignment.Center) {
            when (item) {
                is AppDragItem.App -> {
                    val appInfo = item.appInfo
                    Image(
                        bitmap = rememberAppIconBitmap(appInfo.packageName, appInfo.icon, appInfo.iconIsMonochrome, useOriginalIconColors),
                        contentDescription = appInfo.label,
                        modifier = Modifier.size(34.dp)
                    )
                }
                is AppDragItem.Folder -> Icon(
                    imageVector = Icons.Outlined.Folder,
                    contentDescription = item.folder.name,
                    tint = colors.accent,
                    modifier = Modifier.size(34.dp)
                )
                is AppDragItem.QuickAction -> Text(
                    text = item.actionId.label,
                    fontFamily = CyberFont,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    maxLines = 1
                )
            }
        }
    }
}

/** 画面上部の「削除」「アンインストール」エリア1つ分。指が重なると危険色で強調する。 */
@Composable
private fun DropZone(target: AppDropTarget, icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val colors = LocalCyberColors.current
    val dragState = LocalAppDragState.current
    val isActive = dragState?.hoveredTarget == target
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) DangerColor.copy(alpha = 0.3f) else colors.bg.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, DangerColor.copy(alpha = if (isActive) 1f else 0.6f)),
        modifier = modifier.appDropTarget(target, highlight = false)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = DangerColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(label, fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DangerColor)
        }
    }
}
