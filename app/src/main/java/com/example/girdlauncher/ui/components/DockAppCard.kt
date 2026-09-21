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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import com.example.girdlauncher.util.customIconMap

/**
 * ボトムドック内でアプリアイコンとラベルを表示するためのコンポーザブル。
 *
 * @param name アプリの名前。
 * @param icon アプリのアイコン。
 * @param modifier レイアウトに適用するModifier。
 * @param packageName アプリのパッケージ名。カスタムアイコンの判定に使用。
 * @param appCategory アプリのカテゴリ。フォールバックアイコンの判定に使用。
 * @param isEditMode UIが編集モードかどうか。
 * @param notificationCount 通知（またはアプリバッジ）の件数。0以下の場合はバッジを表示しない。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onClick カードがクリックされたときのコールバック。
 * @param onLongClick カードが長押しされたときのコールバック。
 * @param onRemoveClick 編集モードで削除アイコンがクリックされたときのコールバック。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DockAppCard(
    name: String,
    icon: Drawable?,
    modifier: Modifier = Modifier,
    packageName: String = "",
    appCategory: Int = android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED,
    isEditMode: Boolean = false,
    notificationCount: Int = 0,
    isWallpaperMode: Boolean = false,
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
                val customIcon = customIconMap[packageName]
                if (customIcon != null) {
                    Icon(
                        imageVector = customIcon,
                        contentDescription = name,
                        tint = LocalCyberColors.current.accent,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    val fallbackIcon = when (appCategory) {
                        android.content.pm.ApplicationInfo.CATEGORY_GAME -> Icons.Outlined.VideogameAsset
                        android.content.pm.ApplicationInfo.CATEGORY_AUDIO -> Icons.Outlined.Headset
                        android.content.pm.ApplicationInfo.CATEGORY_VIDEO -> Icons.Outlined.Movie
                        android.content.pm.ApplicationInfo.CATEGORY_IMAGE -> Icons.Outlined.Image
                        android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> Icons.Outlined.People
                        android.content.pm.ApplicationInfo.CATEGORY_NEWS -> Icons.AutoMirrored.Outlined.Article
                        android.content.pm.ApplicationInfo.CATEGORY_MAPS -> Icons.Outlined.Map
                        android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> Icons.Outlined.WorkOutline
                        else -> null
                    }
                    if (fallbackIcon != null) {
                        Icon(
                            imageVector = fallbackIcon,
                            contentDescription = name,
                            tint = LocalCyberColors.current.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    } else if (name.isNotEmpty() && name.first().isLetterOrDigit()) {
                        val firstChar = name.first().uppercaseChar()
                        Box(
                            modifier = Modifier.size(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstChar.toString(),
                                fontFamily = CyberFont,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalCyberColors.current.accent
                            )
                        }
                    } else if (icon != null) {
                        val bitmap = icon.toBitmap().asImageBitmap()
                        Image(
                            bitmap = bitmap,
                            contentDescription = name,
                            modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(LocalCyberColors.current.accent, BlendMode.SrcIn)
                        )
                    } else {
                        Text("★", fontSize = 18.sp, color = LocalCyberColors.current.text)
                    }
                }
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
                        .background(LocalCyberColors.current.accent, RoundedCornerShape(7.dp))
                        .padding(horizontal = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = CyberFont,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
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
