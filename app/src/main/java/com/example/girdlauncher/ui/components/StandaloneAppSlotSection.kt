package com.example.girdlauncher.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.AppInfo
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors

/**
 * APP LISTの1マス分（アプリショートカット1個）だけを独立したウィジェットとして表示する
 * セクション。中身の描画は既存の[AppCard]をそのまま使い、APP LISTのグリッド内の1マスと
 * 見た目を完全に一致させる（そのため、他のウィジェット種類のような外枠の枠線設定は反映しない）。
 *
 * この中身（[AppCard]または空スロットタイル）はウィジェットの全面を占めるため、独自の
 * 「スロット編集モード」は持たせず、ウィジェット全体の編集モード（[isWidgetEditMode]）と
 * 一体で扱う。こうしないと、中身が持つ独自のタップ/長押しジェスチャーが常にウィジェット枠
 * （移動・リサイズハンドルを出すための、キャンバス側の長押し検出）より先に処理を奪ってしまい、
 * 長押ししてもウィジェット編集モードに入れなくなる（中に空白部分が一切無い、常にコンテンツが
 * 全面を占めるこのウィジェット固有の問題）。
 *
 * @param packageName 割り当てられているアプリのパッケージ名。空文字列は未割り当てを表す。
 * @param allApps インストールされているすべてのアプリのリスト（[packageName]の解決に使う）。
 * @param isIconOnly trueならアイコンのみ、falseならアイコン＋名前で表示する。
 * @param isWidgetEditMode ウィジェット編集モード（移動・リサイズハンドル表示中）かどうか。
 *   このモード中は✗バッジを表示し、本体タップでウィジェット編集モードを抜けるようにする。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param activeNotifications 通知（またはアプリバッジ）が来ているアプリのパッケージ名と件数のマップ。
 * @param useOriginalIconColors trueの場合、アイコンをアクセントカラーのデュオトーン加工をせず、
 *   アプリ本来の色のまま表示する。
 * @param onAssignClick 未割り当て、またはアプリが見つからない状態でタップされたときの
 *   コールバック（アプリ選択ダイアログを開く想定）。
 * @param onLongClick 長押しされたときのコールバック（ウィジェット編集モードに入る想定）。
 * @param onRemoveClick 編集モードで✗バッジがタップされたときのコールバック
 *   （このスロットの割り当てだけを外す想定。ウィジェット自体の削除は別のドラッグ操作で行う）。
 * @param onExitWidgetEditMode 編集モード中に、✗バッジ以外の部分がタップされたときの
 *   コールバック（ウィジェット編集モードを抜ける想定）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StandaloneAppSlotSection(
    packageName: String,
    allApps: List<AppInfo>,
    isIconOnly: Boolean,
    isWidgetEditMode: Boolean = false,
    isWallpaperMode: Boolean = false,
    activeNotifications: Map<String, Int> = emptyMap(),
    useOriginalIconColors: Boolean = false,
    modifier: Modifier = Modifier,
    onAssignClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {},
    onExitWidgetEditMode: () -> Unit = {}
) {
    val context = LocalContext.current
    val appInfo = if (packageName.isEmpty()) null else allApps.find { it.packageName == packageName }

    when {
        packageName.isEmpty() -> EmptyAppSlotTile(
            label = "EMPTY",
            onClick = { if (isWidgetEditMode) onExitWidgetEditMode() else onAssignClick() },
            onLongClick = onLongClick,
            modifier = modifier
        )
        appInfo == null -> EmptyAppSlotTile(
            label = "見つかりません",
            onClick = { if (isWidgetEditMode) onExitWidgetEditMode() else onAssignClick() },
            onLongClick = onLongClick,
            modifier = modifier
        )
        else -> AppCard(
            name = appInfo.label,
            packageName = appInfo.packageName,
            icon = appInfo.icon,
            isMonochrome = appInfo.iconIsMonochrome,
            isEditMode = isWidgetEditMode,
            notificationCount = activeNotifications[appInfo.packageName] ?: 0,
            isWallpaperMode = isWallpaperMode,
            isCompact = isIconOnly,
            useOriginalIconColors = useOriginalIconColors,
            // 右上のリサイズハンドル（キャンバス側でこの種類だけ非表示にしている）と被らないよう、
            // 標準の4dpより内側に寄せる
            removeBadgeInset = 10.dp,
            modifier = modifier,
            onClick = {
                if (isWidgetEditMode) {
                    onExitWidgetEditMode()
                } else {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                    launchIntent?.let { context.startActivity(it) }
                }
            },
            onLongClick = onLongClick,
            onRemoveClick = onRemoveClick
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmptyAppSlotTile(label: String, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalCyberColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Transparent, RoundedCornerShape(4.dp))
            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontFamily = CyberFont, fontSize = 10.sp, color = colors.text.copy(alpha = 0.3f))
    }
}
