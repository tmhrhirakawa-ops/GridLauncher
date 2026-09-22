package com.example.girdlauncher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.toDuotoneImageBitmap

/**
 * グリッド内でアプリアイコンとラベルを表示するためのコンポーザブル。
 *
 * @param name アプリの名前。
 * @param icon アプリのアイコン。
 * @param modifier レイアウトに適用するModifier。
 * @param packageName アプリのパッケージ名。デュオトーン加工のキャッシュキーに使用。
 * @param isMonochrome [icon]がモノクロレイヤー由来かどうか。デュオトーン加工方法の選択に使う。
 * @param isEditMode UIが編集モードかどうか。
 * @param notificationCount 通知（またはアプリバッジ）の件数。0以下の場合はバッジを表示しない。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param isCompact trueの場合、アプリ名は表示せずアイコンのみを中央に表示する
 *   （APP LISTのSサイズなど、正方形に近い小さなセル向け）。falseの場合は従来通り、
 *   アイコンと名前を横に並べる。
 * @param onClick カードがクリックされたときのコールバック。
 * @param onLongClick カードが長押しされたときのコールバック。
 * @param onRemoveClick 編集モードで削除アイコンがクリックされたときのコールバック。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    name: String,
    icon: Drawable,
    modifier: Modifier = Modifier,
    packageName: String = "",
    isMonochrome: Boolean = false,
    isEditMode: Boolean = false,
    notificationCount: Int = 0,
    isWallpaperMode: Boolean = false,
    isCompact: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isWallpaperMode) LocalCyberColors.current.panel.copy(alpha = 0.55f) else LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 実アイコンを、ロゴの形は保ちつつアクセントカラーのデュオトーンに加工して表示
            val accent = LocalCyberColors.current.accent
            val bitmap = remember(packageName, icon, isMonochrome, accent) { toDuotoneImageBitmap(icon, isMonochrome, accent) }

            if (isCompact) {
                // アイコンのみを中央に表示する（アプリ名は表示しない）
                Image(
                    bitmap = bitmap,
                    contentDescription = name,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.Center)
                )
            } else {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = name,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        fontFamily = CyberFont,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalCyberColors.current.text,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee()
                    )
                }
            }

            // 通知バッジ（件数表示）
            if (notificationCount > 0 && !isEditMode) {
                val label = if (notificationCount > 99) "99+" else notificationCount.toString()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .border(1.dp, LocalCyberColors.current.accent, RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = CyberFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalCyberColors.current.accent,
                        maxLines = 1
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isEditMode,
                enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                RemoveBadge(onClick = onRemoveClick)
            }
        }
    }
}
