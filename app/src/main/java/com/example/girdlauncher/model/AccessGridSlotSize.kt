package com.example.girdlauncher.model

/**
 * APP LISTウィジェット内のアプリ一覧の表示密度（スロットサイズ）。
 * ウィジェットの外枠サイズはそのままに、列数・行数だけを増減させてアイコンの大きさを変える。
 * ヘッダーのボタンをタップするたびにS→M→Lの順で切り替わる。
 *
 * @property label ボタンに表示する文字。
 * @property colDelta 画面モードごとの基準列数（L）に対する加算量。
 * @property rowDelta 画面モードごとの基準行数（L）に対する加算量。
 */
enum class AccessGridSlotSize(val label: String, val colDelta: Int, val rowDelta: Int) {
    S("S", 2, 2),
    M("M", 1, 1),
    L("L", 0, 0);

    /** タップ時の次のサイズ（S→M→L→S…の順）を返す。 */
    fun next(): AccessGridSlotSize = entries[(ordinal + 1) % entries.size]
}
