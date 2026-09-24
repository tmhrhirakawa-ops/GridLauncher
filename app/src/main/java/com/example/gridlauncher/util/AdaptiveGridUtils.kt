package com.example.gridlauncher.util

import androidx.compose.ui.unit.Dp

/**
 * [preferredCount]を基準に、各スロットのサイズが[minSlotSize]〜[maxSlotSize]の範囲に収まるように
 * 数を調整する。[preferredCount]のままだとスロットが[minSlotSize]未満になってしまう場合は数を
 * 減らし、逆に[maxSlotSize]を超えて間延びしてしまう場合は数を増やす。どちらの範囲にも収まって
 * いれば[preferredCount]をそのまま使う。
 *
 * APP LISTのアプリ一覧・QUICK ACCESSのボタン一覧など、ウィジェットの実際の描画サイズに応じて
 * 列数・行数を自動調整するセクションで共通して使う。
 */
fun adaptiveSlotCount(availableSize: Dp, preferredCount: Int, minSlotSize: Dp, maxSlotSize: Dp, spacing: Dp): Int {
    var count = preferredCount.coerceAtLeast(1)
    // 狭すぎる場合は数を減らしてスロットのサイズを確保する
    while (count > 1) {
        val slotSize = (availableSize - spacing * (count - 1)) / count
        if (slotSize >= minSlotSize) break
        count--
    }
    // 広すぎて間延びする場合は数を増やして余白を詰める
    while (true) {
        val slotSize = (availableSize - spacing * (count - 1)) / count
        if (slotSize <= maxSlotSize) break
        count++
    }
    return count
}
