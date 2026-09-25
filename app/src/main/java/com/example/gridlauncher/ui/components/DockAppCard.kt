package com.example.gridlauncher.ui.components

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * ボトムドック内でアプリアイコンとラベルを表示するためのコンポーザブル。
 *
 * @param name アプリの名前。
 * @param icon アプリのアイコン。
 * @param modifier レイアウトに適用するModifier。
 * @param packageName アプリのパッケージ名。デュオトーン加工のキャッシュキーに使用。
 * @param isMonochrome [icon]がモノクロレイヤー由来かどうか。デュオトーン加工方法の選択に使う。
 * @param isEditMode UIが編集モードかどうか。
 * @param notificationCount 通知（またはアプリバッジ）の件数。0以下の場合はバッジを表示しない。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param useOriginalIconColors trueの場合、アクセントカラーのデュオトーン加工をせず、
 *   アプリ本来の色のアイコンをそのまま表示する。
 * @param onClick カードがクリックされたときのコールバック。
 * @param onLongClick カードが長押しされたときのコールバック。
 * @param onRemoveClick 編集モードで削除アイコンがクリックされたときのコールバック。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockAppCard(
    name: String,
    icon: Drawable,
    modifier: Modifier = Modifier,
    packageName: String = "",
    isMonochrome: Boolean = false,
    isEditMode: Boolean = false,
    notificationCount: Int = 0,
    isWallpaperMode: Boolean = false,
    useOriginalIconColors: Boolean = false,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 実アイコンを、ロゴの形は保ちつつアクセントカラーのデュオトーンに加工して表示する
                // （useOriginalIconColorsがtrueのときは加工せず本来の色のまま表示する）
                val bitmap = rememberAppIconBitmap(packageName, icon, isMonochrome, useOriginalIconColors)
                Image(
                    bitmap = bitmap,
                    contentDescription = name,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = name, 
                    fontFamily = CyberFont, 
                    fontSize = 9.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = LocalCyberColors.current.text, 
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
            
            if (notificationCount > 0 && !isEditMode) {
                val label = if (notificationCount > 99) "99+" else notificationCount.toString()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .defaultMinSize(minWidth = 14.dp, minHeight = 14.dp)
                        .border(1.dp, LocalCyberColors.current.accent, RoundedCornerShape(3.dp))
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = CyberFont,
                        fontSize = 7.sp,
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
                modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
            ) {
                RemoveBadge(onClick = onRemoveClick, size = 16.dp)
            }
        }
    }
}
