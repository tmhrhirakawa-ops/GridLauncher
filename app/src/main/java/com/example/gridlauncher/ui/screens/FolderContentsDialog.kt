package com.example.gridlauncher.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.model.FolderInfo
import com.example.gridlauncher.ui.components.AppCard
import com.example.gridlauncher.ui.drag.AppDragItem
import com.example.gridlauncher.ui.drag.AppDragPayload
import com.example.gridlauncher.ui.drag.AppDragSource
import com.example.gridlauncher.ui.drag.AppDropTarget
import com.example.gridlauncher.ui.drag.LocalAppDragState
import com.example.gridlauncher.ui.drag.appDragSource
import com.example.gridlauncher.ui.drag.appDropTarget
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * フォルダの中身を表示・編集するポップアップ。フォルダ名の変更、内包するアプリの
 * 追加・削除、アプリの起動ができる。
 *
 * グリッド上のフォルダアイコンからそのまま拡大するアニメーション（Container Transform）で
 * 表示するため、システムの[androidx.compose.ui.window.Dialog]ではなく、呼び出し元
 * （[com.example.gridlauncher.ui.screens.CyberLauncherScreen]）と同じ[SharedTransitionLayout]内の
 * オーバーレイとして描画する。そのため背景の暗幕・戻るボタンでの終了は自前で用意している。
 *
 * 中のアプリは長押し→ドラッグで、フォルダ内での並べ替え・フォルダの外（APP LIST・DOCK）への
 * 持ち出し・削除・アンインストールができる（[com.example.gridlauncher.ui.drag]参照）。
 * 持ち出すためにポップアップの外へ指を動かしている間は、下のホーム画面が見えるよう透明にする。
 *
 * @param folder 表示対象のフォルダ。
 * @param allApps インストールされているすべてのアプリのリスト（アプリ選択・アイコン解決に使用）。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param useOriginalIconColors trueの場合、アプリアイコンをアクセントカラーのデュオトーン
 *   加工をせず、本来の色のまま表示する。
 * @param animatedVisibilityScope 呼び出し元の`AnimatedVisibility`のスコープ（共有要素アニメーションに使用）。
 * @param onDismiss ポップアップが閉じられるときのコールバック。
 * @param onRename フォルダ名が変更されたときのコールバック。
 * @param onAddApp 空きスロット（[index]）にアプリ（[packageName]）が追加されたときのコールバック。
 * @param onLaunchApp アプリ（[packageName]）が起動されたときのコールバック。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.FolderContentsDialog(
    folder: FolderInfo,
    allApps: List<AppInfo>,
    isWallpaperMode: Boolean = false,
    useOriginalIconColors: Boolean = false,
    animatedVisibilityScope: AnimatedVisibilityScope,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onAddApp: (index: Int, packageName: String) -> Unit,
    onLaunchApp: (packageName: String) -> Unit
) {
    val colors = LocalCyberColors.current
    var editedName by remember(folder.id) { mutableStateOf(folder.name) }
    var addTargetIndex by remember { mutableStateOf<Int?>(null) }
    // フォルダ名をタップしたときだけ、名前の入力欄を表示する
    var isEditingName by remember { mutableStateOf(false) }
    val dragState = LocalAppDragState.current
    // フォルダ名がタップされたときだけ入力欄にフォーカス・キーボード表示を要求する
    var focusNameField by remember { mutableStateOf(false) }
    val nameFocusRequester = remember { FocusRequester() }

    LaunchedEffect(focusNameField) {
        if (focusNameField) {
            nameFocusRequester.requestFocus()
            focusNameField = false
        }
    }

    BackHandler(onBack = onDismiss)

    // 背景の暗幕。Dialogが自動でやっていた分を自前で用意する
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 中のアプリをポップアップの外へ一度持ち出したら、そのドラッグが終わるまで全体を
            // 透明にし、ポップアップの下に隠れていたスロットも含めてホーム画面にドロップ先を
            // 探せるようにする。ドラッグ中の指の追跡は持ち上げたアプリ側が続けるため、
            // ポップアップ自体は閉じずに残しておく（持ち出しをやめて何もない所に離すと再表示される）
            .graphicsLayer {
                val isCarriedOut = dragState?.isCarriedOutOfFolder == true &&
                    (dragState.payload?.source as? AppDragSource.FolderSlot)?.folderId == folder.id
                alpha = if (isCarriedOut) 0f else 1f
            }
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.bg,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .sharedBounds(
                    rememberSharedContentState(key = folder.id),
                    animatedVisibilityScope = animatedVisibilityScope
                )
                .appDropTarget(AppDropTarget.FolderPanel, highlight = false)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isEditingName = false } // 空白部分をタップしたら名前の編集を終える（暗幕への伝播も防ぐ）
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (isEditingName) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = {
                            editedName = it
                            onRename(it)
                        },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = CyberFont, fontSize = 16.sp, color = colors.text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(nameFocusRequester)
                    )
                } else {
                    Text(
                        text = editedName,
                        fontFamily = CyberFont,
                        fontSize = 16.sp,
                        color = colors.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                isEditingName = true
                                focusNameField = true
                            }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(colors.border)
                )
                Spacer(modifier = Modifier.height(16.dp))

                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 3) {
                            val index = row * 3 + col
                            val packageName = folder.packageNames.getOrNull(index)?.takeIf { it.isNotEmpty() }
                            val appInfo = packageName?.let { pkg -> allApps.find { it.packageName == pkg } }
                            // どのスロットもドロップ先にする。持ち上げ中のスロットは薄く表示して「抜けた」ことを示す
                            val slotModifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .appDropTarget(AppDropTarget.FolderSlot(folder.id, index))
                                .graphicsLayer {
                                    alpha = if (dragState?.payload?.source == AppDragSource.FolderSlot(folder.id, index)) 0.3f else 1f
                                }

                            if (appInfo != null) {
                                AppCard(
                                    name = appInfo.label,
                                    packageName = appInfo.packageName,
                                    icon = appInfo.icon,
                                    isMonochrome = appInfo.iconIsMonochrome,
                                    isWallpaperMode = isWallpaperMode,
                                    useOriginalIconColors = useOriginalIconColors,
                                    modifier = slotModifier.appDragSource {
                                        AppDragPayload(AppDragSource.FolderSlot(folder.id, index), AppDragItem.App(appInfo))
                                    },
                                    onClick = {
                                        onLaunchApp(appInfo.packageName)
                                        onDismiss()
                                    }
                                )
                            } else {
                                Surface(
                                    onClick = { addTargetIndex = index },
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = slotModifier
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "EMPTY",
                                            fontFamily = CyberFont,
                                            fontSize = 9.sp,
                                            color = colors.text.copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (row < 2) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("閉じる", fontFamily = CyberFont, fontSize = 12.sp, color = colors.text.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    if (addTargetIndex != null) {
        AppSelectorDialog(
            allApps = allApps,
            onDismiss = { addTargetIndex = null },
            onAppSelected = { packageName ->
                onAddApp(addTargetIndex!!, packageName)
                addTargetIndex = null
            }
        )
    }
}
