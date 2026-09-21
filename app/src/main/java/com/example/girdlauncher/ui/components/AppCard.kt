package com.example.girdlauncher.ui.components

import android.graphics.drawable.Drawable
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
 * グリッド内でアプリアイコンとラベルを表示するためのコンポーザブル。
 *
 * @param name アプリの名前。
 * @param icon アプリのアイコン。
 * @param modifier レイアウトに適用するModifier。
 * @param packageName アプリのパッケージ名。カスタムアイコンの判定に使用。
 * @param appCategory アプリのカテゴリ。フォールバックアイコンの判定に使用。
 * @param isEditMode UIが編集モードかどうか。
 * @param hasNotification 通知（またはアプリバッジ）があるかどうか。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onClick カードがクリックされたときのコールバック。
 * @param onLongClick カードが長押しされたときのコールバック。
 * @param onRemoveClick 編集モードで削除アイコンがクリックされたときのコールバック。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    name: String,
    icon: Drawable?,
    modifier: Modifier = Modifier,
    packageName: String = "",
    appCategory: Int = android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED,
    isEditMode: Boolean = false,
    hasNotification: Boolean = false,
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
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val customIcon = customIconMap[packageName]
                if (customIcon != null) {
                    Icon(
                        imageVector = customIcon,
                        contentDescription = name,
                        tint = LocalCyberColors.current.accent,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    // カテゴリに応じてフォールバックのアイコンを変える
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
                            modifier = Modifier.size(24.dp)
                        )
                    } else if (name.isNotEmpty() && name.first().isLetterOrDigit()) {
                        // カスタムアイコンがない場合は、アプリ名の頭文字を表示する
                        val firstChar = name.first().uppercaseChar()
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = firstChar.toString(),
                                fontFamily = CyberFont,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = LocalCyberColors.current.accent
                            )
                        }
                    } else if (icon != null) {
                        val bitmap = icon.toBitmap().asImageBitmap()
                        Image(
                            bitmap = bitmap,
                            contentDescription = name,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(LocalCyberColors.current.accent, BlendMode.SrcIn)
                        )
                    } else {
                        Text("★", fontSize = 20.sp, color = LocalCyberColors.current.text)
                    }
                }
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
            
            // 通知バッジ
            if (hasNotification && !isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(8.dp)
                        .background(LocalCyberColors.current.accent, RoundedCornerShape(4.dp))
                )
            }
            
            if (isEditMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(20.dp)
                        .background(Color.Red, RoundedCornerShape(10.dp))
                        .clickable { onRemoveClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("×", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
