package com.example.gridlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper as WallpaperFilled
import androidx.compose.material.icons.outlined.BorderAll
import androidx.compose.material.icons.outlined.BorderClear
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper as WallpaperOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * バッテリーコア（歯車アイコン）をタップすると、その真上に「にゅいっ」と弾んで
 * 表示されるメニュー。設定・壁紙透過・枠線表示・電源の4つのボタンを2列×2行で並べる。
 *
 * @param onOpenSettings 設定ボタンがタップされたときのコールバック（端末の設定アプリを開く）。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onWallpaperToggle 壁紙透過切り替えボタンがタップされたときのコールバック。
 * @param bordersVisible ウィジェットの枠線が（すべて）表示されているかどうか。
 * @param onToggleAllBorders 枠線切り替えボタンがタップされたときのコールバック（全ウィジェット一括切り替え）。
 * @param onLongPressBorderToggle 枠線切り替えボタンが長押しされたときのコールバック（個別設定ダイアログを開く）。
 * @param onOpenPowerMenu 電源ボタンがタップされたときのコールバック（端末標準の電源メニューを開く）。
 * @param onDismiss メニューが閉じられるときのコールバック。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CoreMenuPopup(
    onOpenSettings: () -> Unit,
    isWallpaperMode: Boolean,
    onWallpaperToggle: () -> Unit,
    bordersVisible: Boolean,
    onToggleAllBorders: () -> Unit,
    onLongPressBorderToggle: () -> Unit,
    onOpenPowerMenu: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    val density = LocalDensity.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Popup(
        popupPositionProvider = remember { AboveAnchorPositionProvider(marginPx = with(density) { 10.dp.roundToPx() }) },
        onDismissRequest = onDismiss
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.3f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = scaleOut(targetScale = 0.3f) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.bg,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 設定（歯車）ボタン
                        Surface(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Settings",
                                    tint = colors.text.copy(alpha = 0.6f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        // 電源ボタン（端末標準の電源メニューを開く）
                        Surface(
                            onClick = onOpenPowerMenu,
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.PowerSettingsNew,
                                    contentDescription = "Power Menu",
                                    tint = colors.text.copy(alpha = 0.6f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 枠線切り替えボタン（タップで全ウィジェット一括切り替え、長押しで個別設定）
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, colors.border),
                            modifier = Modifier
                                .size(30.dp)
                                .combinedClickable(
                                    onClick = onToggleAllBorders,
                                    onLongClick = onLongPressBorderToggle
                                )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (bordersVisible) Icons.Outlined.BorderAll else Icons.Outlined.BorderClear,
                                    contentDescription = "Toggle Widget Borders",
                                    tint = if (bordersVisible) colors.accent else colors.text.copy(alpha = 0.6f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        // 壁紙切り替えボタン
                        Surface(
                            onClick = onWallpaperToggle,
                            shape = RoundedCornerShape(4.dp),
                            color = if (isWallpaperMode) colors.accent.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(1.dp, if (isWallpaperMode) colors.accent else colors.border),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isWallpaperMode) Icons.Filled.WallpaperFilled else Icons.Outlined.WallpaperOutlined,
                                    contentDescription = "Toggle Wallpaper",
                                    tint = if (isWallpaperMode) colors.accent else colors.text.copy(alpha = 0.6f),
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
