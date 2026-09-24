package com.example.gridlauncher.util

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.gridlauncher.model.FolderInfo
import java.util.UUID

/**
 * グリッドのスロット値（"grid_apps"/"dock_apps"のカンマ区切り要素）でフォルダを指す際の接頭辞。
 * パッケージ名には":"が含まれないため、通常のアプリのパッケージ名と衝突しない。
 */
private const val FOLDER_SLOT_PREFIX = "folder:"

/** スロット値がフォルダを指しているかどうか。 */
fun isFolderSlotValue(value: String): Boolean = value.startsWith(FOLDER_SLOT_PREFIX)

/** スロット値からフォルダIDを取り出す。フォルダを指すスロット値でない場合はnull。 */
fun folderIdFromSlotValue(value: String): String? =
    value.takeIf { isFolderSlotValue(it) }?.removePrefix(FOLDER_SLOT_PREFIX)

/** フォルダIDからスロット値を作る。 */
fun folderSlotValue(folderId: String): String = "$FOLDER_SLOT_PREFIX$folderId"

/**
 * 新しい空のフォルダを作成し、SharedPreferencesに保存します。
 *
 * @param prefs 保存先の [SharedPreferences]。
 * @param name フォルダの初期名。
 * @return 作成された [FolderInfo]。
 */
fun createFolder(prefs: SharedPreferences, name: String): FolderInfo {
    val folder = FolderInfo(
        id = UUID.randomUUID().toString(),
        name = name,
        packageNames = List(FolderInfo.CAPACITY) { "" }
    )
    saveFolder(prefs, folder)
    return folder
}

/**
 * フォルダの内容（名前・中身のアプリ）を保存します。フォルダIDが未登録の場合は
 * フォルダID一覧にも追加します。
 */
fun saveFolder(prefs: SharedPreferences, folder: FolderInfo) {
    prefs.edit {
        val ids = prefs.getString(KEY_FOLDER_IDS, "")
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.toMutableList()
            ?: mutableListOf()
        if (folder.id !in ids) {
            ids.add(folder.id)
            putString(KEY_FOLDER_IDS, ids.joinToString(","))
        }
        putString(folderNameKey(folder.id), folder.name)
        putString(folderAppsKey(folder.id), folder.packageNames.joinToString(","))
    }
}

/**
 * フォルダをSharedPreferencesから完全に削除します（フォルダ自体をグリッドから外す時に使う。
 * 中身のアプリ自体はアンインストールされない）。
 */
fun deleteFolder(prefs: SharedPreferences, folderId: String) {
    prefs.edit {
        val ids = prefs.getString(KEY_FOLDER_IDS, "")
            ?.split(",")
            ?.filter { it.isNotEmpty() && it != folderId }
            ?: emptyList()
        putString(KEY_FOLDER_IDS, ids.joinToString(","))
        remove(folderNameKey(folderId))
        remove(folderAppsKey(folderId))
    }
}

/**
 * 保存されているすべてのフォルダを読み込みます。
 *
 * @return フォルダIDをキーにした [FolderInfo] のマップ。
 */
fun loadFolders(prefs: SharedPreferences): Map<String, FolderInfo> {
    val ids = prefs.getString(KEY_FOLDER_IDS, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
    return ids.associateWith { id ->
        val name = prefs.getString(folderNameKey(id), "") ?: ""
        val packageNames = prefs.getString(folderAppsKey(id), "")
            ?.split(",")
            ?: List(FolderInfo.CAPACITY) { "" }
        FolderInfo(id = id, name = name, packageNames = packageNames)
    }
}

private const val KEY_FOLDER_IDS = "folder_ids"
private fun folderNameKey(id: String) = "folder_${id}_name"
private fun folderAppsKey(id: String) = "folder_${id}_apps"
