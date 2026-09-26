package com.example.gridlauncher.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.ACCESS_GRID_MAX_PAGES
import com.example.gridlauncher.util.ACCESS_GRID_MAX_SPAN
import com.example.gridlauncher.util.ACCESS_GRID_MIN_SPAN
import com.example.gridlauncher.util.AccessGridSize

/**
 * APP LISTのヘッダーの歯車ボタンで開く、APP LISTの設定をするボトムシート。
 * ICON ONLYの切り替え・縦画面と横画面で同じ並びにするかどうか・アイコンの並び（列数×行数、またはAUTO）・
 * ページ数を設定できる。
 * アイコンの並びとページ数は、現在の画面モード（縦画面（小）・縦画面（大）・横画面）ごとに保存する。
 *
 * @param isIconOnly ICON ONLYモードかどうか。
 * @param onIconOnlyChange ICON ONLYスイッチが切り替えられたときのコールバック。
 * @param shareAcrossOrientations 縦画面と横画面で同じ並び（同じアプリ・フォルダの配置）を使うかどうか。
 * @param onShareAcrossOrientationsChange 上記のスイッチが切り替えられたときのコールバック。
 * @param gridSize 指定しているアイコンの並び。nullの場合はAUTO。
 * @param autoGridSize AUTOの場合に現在の大きさから自動で決まる並び（AUTOをオフにしたときの初期値にも使う）。
 * @param onGridSizeChange アイコンの並びが変更されたときのコールバック（nullはAUTO）。
 * @param pageCount 指定しているページ数。
 * @param minPageCount アプリ・フォルダが入っているページ数（これより少なくはできない）。
 * @param onPageCountChange ページ数が変更されたときのコールバック。
 * @param onDismiss シートが閉じられるときのコールバック。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListSettingsSheet(
    isIconOnly: Boolean,
    onIconOnlyChange: (Boolean) -> Unit,
    shareAcrossOrientations: Boolean,
    onShareAcrossOrientationsChange: (Boolean) -> Unit,
    gridSize: AccessGridSize?,
    autoGridSize: AccessGridSize?,
    onGridSizeChange: (AccessGridSize?) -> Unit,
    pageCount: Int,
    minPageCount: Int,
    onPageCountChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
    val isAuto = gridSize == null
    val shownSize = gridSize ?: autoGridSize

    // 画面の小さい端末でシートが上端に届くと、スワイプで閉じられなくなるため
    // （CustomizeSheetと同じ理由）、中身の高さに上限を設けてスクロールさせる
    val windowInfo = LocalWindowInfo.current
    val maxContentHeight = with(LocalDensity.current) { windowInfo.containerSize.height.toDp() } * 0.75f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.bg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxContentHeight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("APP LIST // CONFIG", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            Spacer(modifier = Modifier.height(2.dp))

            // ICON ONLY（アプリ名を出さず、アイコンだけを正方形のスロットに表示する）
            CustomizeRow(
                icon = Icons.Outlined.Apps,
                title = "ICON ONLY",
                description = "アプリ名を出さず、アイコンだけを表示します",
                onClick = { onIconOnlyChange(!isIconOnly) }
            ) {
                CyberSwitch(checked = isIconOnly, onCheckedChange = onIconOnlyChange)
            }

            // 縦画面と横画面で同じ並びにするかどうか。同じにするときは、今表示している向きの並びにそろえる
            CustomizeRow(
                icon = Icons.Outlined.ScreenRotation,
                title = "縦画面・横画面で同じ並びにする",
                description = if (shareAcrossOrientations) {
                    "縦画面と横画面で同じアプリを並べます"
                } else {
                    "縦画面と横画面で別々に並べます（オンにすると今の向きの並びにそろえます）"
                },
                onClick = { onShareAcrossOrientationsChange(!shareAcrossOrientations) }
            ) {
                CyberSwitch(checked = shareAcrossOrientations, onCheckedChange = onShareAcrossOrientationsChange)
            }

            // アイコンの並び（AUTO、または列数×行数を指定）
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.GridView,
                    title = "アイコンの並び",
                    description = buildString {
                        append(if (isAuto) "AUTO（大きさに合わせて自動）" else "手動で指定")
                        shownSize?.let { append("：${it.columns}×${it.rows}") }
                    }
                ) {}
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            // AUTOをオフにしたときは、今の並びから調整を始められるようにする
                            onGridSizeChange(if (isAuto) autoGridSize ?: AccessGridSize(3, 3) else null)
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text("AUTO", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text, modifier = Modifier.weight(1f))
                    CyberSwitch(
                        checked = isAuto,
                        onCheckedChange = { auto -> onGridSizeChange(if (auto) null else autoGridSize ?: AccessGridSize(3, 3)) }
                    )
                }
                AnimatedVisibility(visible = !isAuto && gridSize != null, enter = expandVertically(), exit = shrinkVertically()) {
                    val size = gridSize ?: return@AnimatedVisibility
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StepperRow(
                            label = "横（列）",
                            value = size.columns,
                            min = ACCESS_GRID_MIN_SPAN,
                            max = ACCESS_GRID_MAX_SPAN,
                            onChange = { onGridSizeChange(size.copy(columns = it)) }
                        )
                        StepperRow(
                            label = "縦（行）",
                            value = size.rows,
                            min = ACCESS_GRID_MIN_SPAN,
                            max = ACCESS_GRID_MAX_SPAN,
                            onChange = { onGridSizeChange(size.copy(rows = it)) }
                        )
                    }
                }
            }

            // ページ数（スロットが埋まっても自動では増えない）
            CustomizeCard {
                CustomizeRowContent(
                    icon = Icons.Outlined.ViewCarousel,
                    title = "ページ数",
                    description = if (minPageCount > 1) "アプリが入っている${minPageCount}ページより少なくはできません" else "横スワイプで切り替えるページの数"
                ) {
                    Stepper(
                        value = pageCount,
                        min = minPageCount,
                        max = ACCESS_GRID_MAX_PAGES,
                        onChange = onPageCountChange
                    )
                }
            }
        }
    }
}

/** ラベルと、−/＋で値を増減するステッパーを並べた行。 */
@Composable
private fun StepperRow(label: String, value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val colors = LocalCyberColors.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, fontFamily = CyberFont, fontSize = 12.sp, color = colors.text, modifier = Modifier.weight(1f))
        Stepper(value = value, min = min, max = max, onChange = onChange)
    }
}

/** −/＋で値を[min]〜[max]の範囲で増減するステッパー。 */
@Composable
private fun Stepper(value: Int, min: Int, max: Int, onChange: (Int) -> Unit) {
    val colors = LocalCyberColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .border(1.dp, colors.border, RoundedCornerShape(4.dp))
    ) {
        StepperButton(label = "−", enabled = value > min, onClick = { onChange(value - 1) })
        Text(
            text = value.toString(),
            fontFamily = CyberFont,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = colors.text,
            modifier = Modifier.widthIn(min = 32.dp).padding(horizontal = 4.dp),
            textAlign = TextAlign.Center
        )
        StepperButton(label = "＋", enabled = value < max, onClick = { onChange(value + 1) })
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(width = 36.dp, height = 32.dp)
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Text(
            label,
            fontFamily = CyberFont,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) colors.accent else colors.text.copy(alpha = 0.25f)
        )
    }
}
