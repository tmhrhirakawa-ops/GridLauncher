package com.example.gridlauncher.util

import com.example.gridlauncher.model.PlacedWidget

/**
 * スタックの整合性を保つ。メンバーが1つしか残っていないスタックは、普通のウィジェットに戻す
 * （削除や取り出しで1つになった場合など）。
 */
fun List<PlacedWidget>.normalizeStacks(): List<PlacedWidget> {
    val memberCounts = filter { it.stackId >= 0 }.groupingBy { it.stackId }.eachCount()
    return map { if (it.stackId >= 0 && (memberCounts[it.stackId] ?: 0) <= 1) it.copy(stackId = -1) else it }
}

/**
 * [draggedKeys]（[PlacedWidget.instanceKey]）のウィジェットを、[targetGroupKey]（[PlacedWidget.groupKey]）の
 * ウィジェット（またはスタック）の上に重ねてスタックにする。重ねたウィジェットは重ね先の位置・サイズに
 * そろえ、スタックの最後のページに加える。
 *
 * @return 新しい配置と、重ね先のスタックのID。重ね先が見つからない場合は元の配置と-1。
 */
fun List<PlacedWidget>.stackOnto(draggedKeys: Set<String>, targetGroupKey: String): Pair<List<PlacedWidget>, Int> {
    val target = filter { it.groupKey == targetGroupKey && it.instanceKey !in draggedKeys }
    val base = target.firstOrNull() ?: return this to -1
    val stackId = if (base.stackId >= 0) base.stackId else (maxOfOrNull { it.stackId } ?: -1) + 1
    val dragged = filter { it.instanceKey in draggedKeys }.map {
        it.copy(col = base.col, row = base.row, colSpan = base.colSpan, rowSpan = base.rowSpan, stackId = stackId)
    }
    val rest = filter { it.instanceKey !in draggedKeys }
        .map { if (it.groupKey == targetGroupKey) it.copy(stackId = stackId) else it }
    // スタック内のページ順は一覧での並び順なので、重ね先の最後のメンバーの直後に差し込む
    val lastTargetIndex = rest.indexOfLast { it.stackId == stackId }
    val result = rest.toMutableList().apply { addAll(lastTargetIndex + 1, dragged) }
    return result.normalizeStacks() to stackId
}

/** スタックから[memberKey]のウィジェットを取り出し、([col], [row])に単体のウィジェットとして置く。 */
fun List<PlacedWidget>.pullOutOfStack(memberKey: String, col: Float, row: Float): List<PlacedWidget> =
    map { if (it.instanceKey == memberKey) it.copy(col = col, row = row, stackId = -1) else it }.normalizeStacks()
