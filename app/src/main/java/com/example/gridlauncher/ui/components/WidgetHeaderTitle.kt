package com.example.gridlauncher.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/** 見出しを縮めるときの、いちばん小さい文字の大きさ。 */
private val MinHeaderFontSize = 7.sp

/**
 * ウィジェットのヘッダーの見出し（「SYSTEM MONITOR」など）。行の残りの幅を使い、必ず1行で表示する。
 *
 * 選んだフォントによって同じ文字数でも横幅が大きく違う（ORBITRON などは SHARE TECH MONO より3割ほど広い）
 * ため、幅が足りないときだけ[fontSize]から少しずつ文字を小さくして収める（[MinHeaderFontSize]まで。
 * それでも入らなければ末尾を「…」で省略する）。右側に並べたボタンは押し出さない。
 *
 * @param text 見出しの文字。
 * @param fontSize 幅が足りているときの文字の大きさ。
 */
@Composable
fun RowScope.WidgetHeaderTitle(text: String, fontSize: TextUnit, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = TextStyle(
            fontFamily = CyberFont,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            color = LocalCyberColors.current.text
        ),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        autoSize = TextAutoSize.StepBased(minFontSize = MinHeaderFontSize, maxFontSize = fontSize, stepSize = 0.5.sp),
        modifier = modifier.weight(1f)
    )
}
