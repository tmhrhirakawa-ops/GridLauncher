package com.example.gridlauncher.model

/**
 * グリッド上に配置できるフォルダを表すデータクラス。
 *
 * @property id フォルダを一意に識別するID（SharedPreferencesのキーにも使う）。
 * @property name フォルダ名（ユーザーが変更可能）。
 * @property packageNames フォルダ内のアプリのパッケージ名リスト。3×3の9個固定で、
 *   空きスロットは空文字列で表す（グリッド/ドックの保存形式と同じ考え方）。
 */
data class FolderInfo(
    val id: String,
    val name: String,
    val packageNames: List<String>
) {
    companion object {
        const val CAPACITY = 9
    }
}

/**
 * グリッド/ドックの1スロットの中身を表すモデル。
 * nullの場合は空きスロットを意味する（既存のAppInfo?と同じ扱い）。
 */
sealed class GridItem {
    data class AppItem(val appInfo: AppInfo) : GridItem()
    data class FolderItem(val folder: FolderInfo) : GridItem()
}
