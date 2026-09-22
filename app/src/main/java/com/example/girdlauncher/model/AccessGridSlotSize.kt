package com.example.girdlauncher.model

/**
 * APP LISTウィジェット内のアプリ一覧の表示密度（スロットサイズ）。
 * ウィジェットの外枠サイズはそのままに、列数・行数だけを増減させてアイコンの大きさを変える。
 * ヘッダーのボタンをタップするたびにS→M→Lの順で切り替わる。
 * 実際の列数・行数（アイコン1個分のサイズ）は、呼び出し側（[com.example.girdlauncher.ui.screens]の
 * 画面）で、画面モードごとの基準サイズと実際の画面寸法から算出する。
 *
 * @property label ボタンに表示する文字。
 */
enum class AccessGridSlotSize(val label: String) {
    S("S"),
    M("M"),
    L("L");

    /** タップ時の次のサイズ（S→M→L→S…の順）を返す。 */
    fun next(): AccessGridSlotSize = entries[(ordinal + 1) % entries.size]
}
