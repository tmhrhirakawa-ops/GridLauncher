package com.example.girdlauncher.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.girdlauncher.model.AppInfo
import com.example.girdlauncher.model.FolderInfo
import com.example.girdlauncher.ui.components.AppCard
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * フォルダの中身を表示・編集するポップアップ。フォルダ名の変更、内包するアプリの
 * 追加・削除、アプリの起動ができる。
 *
 * @param folder 表示対象のフォルダ。
 * @param allApps インストールされているすべてのアプリのリスト（アプリ選択・アイコン解決に使用）。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param onDismiss ポップアップが閉じられるときのコールバック。
 * @param onRename フォルダ名が変更されたときのコールバック。
 * @param onAddApp 空きスロット（[index]）にアプリ（[packageName]）が追加されたときのコールバック。
 * @param onRemoveApp スロット（[index]）のアプリが削除されたときのコールバック。
 * @param onLaunchApp アプリ（[packageName]）が起動されたときのコールバック。
 */
@Composable
fun FolderContentsDialog(
    folder: FolderInfo,
    allApps: List<AppInfo>,
    isWallpaperMode: Boolean = false,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit,
    onAddApp: (index: Int, packageName: String) -> Unit,
    onRemoveApp: (index: Int) -> Unit,
    onLaunchApp: (packageName: String) -> Unit
) {
    val colors = LocalCyberColors.current
    var editedName by remember(folder.id) { mutableStateOf(folder.name) }
    var addTargetIndex by remember { mutableStateOf<Int?>(null) }
    // フォルダ名をタップ（編集開始）した時、またはアプリを長押しした時だけ削除バッジを表示する
    var isEditMode by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.bg,
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { isEditMode = false } // 空白部分をタップしたら編集モード解除
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                        .onFocusChanged { if (it.isFocused) isEditMode = true }
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

                            if (appInfo != null) {
                                AppCard(
                                    name = appInfo.label,
                                    packageName = appInfo.packageName,
                                    icon = appInfo.icon,
                                    isMonochrome = appInfo.iconIsMonochrome,
                                    isEditMode = isEditMode,
                                    isWallpaperMode = isWallpaperMode,
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    onClick = {
                                        if (isEditMode) {
                                            isEditMode = false
                                        } else {
                                            onLaunchApp(appInfo.packageName)
                                            onDismiss()
                                        }
                                    },
                                    onLongClick = { isEditMode = true },
                                    onRemoveClick = { onRemoveApp(index) }
                                )
                            } else {
                                Surface(
                                    onClick = { if (isEditMode) isEditMode = false else addTargetIndex = index },
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, colors.border),
                                    modifier = Modifier.weight(1f).fillMaxHeight()
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
