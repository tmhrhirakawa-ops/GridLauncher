package com.example.gridlauncher.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.components.consumeUpwardSheetFling
import com.example.gridlauncher.model.WidgetPanel
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * QUICK ACCESSのGRIDボタンで開く、グリッド線（ウィジェットの枠線）の設定をするボトムシート。
 * カスタマイズ画面のグリッド線の項目と同じ内容で、ウィジェットごとの設定を最初から開いておく。
 *
 * @param hiddenPanels 枠線を非表示にしているウィジェットの集合。
 * @param onSetAllBorders 全ウィジェットの枠線を一括で表示/非表示にするときのコールバック（true=表示）。
 * @param onTogglePanelBorder 個別のウィジェットの枠線が切り替えられたときのコールバック。
 * @param onDismiss シートが閉じられるときのコールバック。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridLinesSheet(
    hiddenPanels: Set<WidgetPanel>,
    onSetAllBorders: (Boolean) -> Unit,
    onTogglePanelBorder: (WidgetPanel) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalCyberColors.current
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
                .consumeUpwardSheetFling()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("GRID LINES // CONFIG", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.accent)
            Spacer(modifier = Modifier.height(2.dp))
            GridLinesCard(
                hiddenPanels = hiddenPanels,
                onSetAllBorders = onSetAllBorders,
                onTogglePanelBorder = onTogglePanelBorder,
                initiallyExpanded = true
            )
        }
    }
}
