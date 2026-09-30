package com.example.gridlauncher.ui.screens

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import com.example.gridlauncher.model.FolderInfo
import com.example.gridlauncher.model.QuickActionId
import com.example.gridlauncher.ui.drag.AppDragItem
import com.example.gridlauncher.ui.drag.AppDragPayload
import com.example.gridlauncher.ui.drag.AppDragSource
import com.example.gridlauncher.ui.drag.AppDropTarget
import com.example.gridlauncher.util.createFolder
import com.example.gridlauncher.util.deleteFolder
import com.example.gridlauncher.util.folderIdFromSlotValue
import com.example.gridlauncher.util.folderSlotValue
import com.example.gridlauncher.util.isFolderSlotValue

// アプリアイコン・フォルダ・QUICK ACCESSのボタンを、ドラッグ＆ドロップしたときの並びの変え方。
// CyberLauncherScreen から、画面の状態に依存しない部分を切り出したもの。

/**
 * APP LIST・DOCK・フォルダの並び。スロットの値は、パッケージ名・フォルダの値（[folderSlotValue]）・空（""）のどれか。
 */
internal data class SlotArrangement(
    val grid: List<String>,
    val dock: List<String>,
    val folders: Map<String, FolderInfo>
)

/**
 * [applySlotDrop]の結果。
 *
 * @property arrangement ドロップを反映した並び。
 * @property closeFolder 開いているフォルダのポップアップを閉じるかどうか（フォルダの外へ持ち出した・フォルダがなくなった）。
 */
internal class SlotDropResult(val arrangement: SlotArrangement, val closeFolder: Boolean)

/**
 * QUICK ACCESSのボタンのドロップを反映した並びを返す。QUICK ACCESS内での並べ替え（入れ替え）と
 * 削除だけに対応し、それ以外（変化なしを含む）はnull。
 */
internal fun applyQuickActionDrop(
    slots: List<QuickActionId?>,
    source: AppDragSource,
    target: AppDropTarget
): List<QuickActionId?>? {
    val from = (source as? AppDragSource.QuickAccess)?.index ?: return null
    val newSlots = slots.toMutableList()
    fun ensureIndex(index: Int) {
        while (newSlots.size <= index) newSlots.add(null)
    }
    ensureIndex(from)
    when (target) {
        AppDropTarget.RemoveZone -> newSlots[from] = null
        is AppDropTarget.QuickSlot -> {
            if (target.index == from) return null
            ensureIndex(target.index)
            val existing = newSlots[target.index]
            newSlots[target.index] = newSlots[from]
            newSlots[from] = existing
        }
        else -> return null
    }
    return newSlots
}

/**
 * アプリ・フォルダのドロップを反映した並びを返す（何も変わらない場合はnull）。
 * フォルダの作成・削除はここで保存まで行うが、それ以外の並びの保存は呼び出し側で行う。
 *
 * - 空きスロットへ：移動する（アプリドロワーからなら追加）
 * - アプリの上へ：2つをまとめた新しいフォルダにする
 * - フォルダの上へ：フォルダの空きに入れる
 * - 使用中のスロットへ（フォルダ・DOCK）：ドラッグ元と入れ替える
 * - 「削除」エリアへ：ドラッグ元から外す（フォルダならフォルダごと消す）
 * - 開いているフォルダの中：フォルダの中で入れ替える
 */
internal fun applySlotDrop(
    context: Context,
    prefs: SharedPreferences,
    current: SlotArrangement,
    payload: AppDragPayload,
    target: AppDropTarget
): SlotDropResult? {
    val source = payload.source
    val grid = current.grid.toMutableList()
    val dock = current.dock.toMutableList()
    val editedFolders = current.folders.toMutableMap()
    fun MutableList<String>.ensureIndex(index: Int) {
        while (size <= index) add("")
    }
    fun updateFolderSlot(folderId: String, index: Int, value: String) {
        val folder = editedFolders[folderId] ?: return
        editedFolders[folderId] = folder.copy(packageNames = folder.packageNames.toMutableList().also { it[index] = value })
    }
    val draggedValue = when (val item = payload.item) {
        is AppDragItem.App -> item.appInfo.packageName
        is AppDragItem.Folder -> folderSlotValue(item.folder.id)
        is AppDragItem.QuickAction -> return null
    }
    // ドラッグ元のスロットの中身を置き換える（""で空ける。入れ替えの場合は相手の値を入れる）。
    // アプリドロワーから持ってきた場合は、元のスロットがないため何もしない
    fun replaceSource(value: String) {
        when (source) {
            is AppDragSource.Grid -> { grid.ensureIndex(source.index); grid[source.index] = value }
            is AppDragSource.Dock -> { dock.ensureIndex(source.index); dock[source.index] = value }
            is AppDragSource.FolderSlot -> updateFolderSlot(source.folderId, source.index, value)
            is AppDragSource.QuickAccess, AppDragSource.Drawer -> Unit
        }
    }

    when (target) {
        AppDropTarget.RemoveZone -> {
            (payload.item as? AppDragItem.Folder)?.let { item ->
                editedFolders.remove(item.folder.id)
                deleteFolder(prefs, item.folder.id)
            }
            replaceSource("")
        }
        // 開いているフォルダの中での並べ替え（入れ替え）
        is AppDropTarget.FolderSlot -> {
            val from = source as? AppDragSource.FolderSlot ?: return null
            if (from.index == target.index) return null
            val existing = editedFolders[target.folderId]?.packageNames?.getOrNull(target.index) ?: return null
            updateFolderSlot(target.folderId, target.index, draggedValue)
            updateFolderSlot(from.folderId, from.index, existing)
        }
        is AppDropTarget.GridSlot -> {
            if (source == AppDragSource.Grid(target.index)) return null
            grid.ensureIndex(target.index)
            val existing = grid[target.index]
            // フォルダの中のアプリを、そのフォルダ自身の上に落とした場合は何もしない
            if (source is AppDragSource.FolderSlot && existing == folderSlotValue(source.folderId)) return null
            val item = payload.item
            when {
                existing.isEmpty() -> {
                    replaceSource("")
                    grid[target.index] = draggedValue
                }
                // フォルダの上に重ねたアプリは、フォルダの空きに追加する
                item is AppDragItem.App && isFolderSlotValue(existing) -> {
                    val folderId = folderIdFromSlotValue(existing) ?: return null
                    val folder = editedFolders[folderId] ?: return null
                    val packageName = item.appInfo.packageName
                    val emptyIndex = folder.packageNames.indexOfFirst { it.isEmpty() }
                    if (packageName in folder.packageNames) {
                        // 既に入っているアプリは重複させず、ドラッグ元から外すだけにする
                        if (source == AppDragSource.Drawer) return null
                    } else if (emptyIndex < 0) {
                        Toast.makeText(context, "フォルダがいっぱいです", Toast.LENGTH_SHORT).show()
                        return null
                    } else {
                        updateFolderSlot(folderId, emptyIndex, packageName)
                    }
                    replaceSource("")
                }
                // アプリの上に重ねたアプリは、2つをまとめた新しいフォルダにする
                item is AppDragItem.App -> {
                    if (existing == item.appInfo.packageName) return null
                    val created = createFolder(prefs, "新しいフォルダ")
                    editedFolders[created.id] = created.copy(
                        packageNames = created.packageNames.toMutableList().also {
                            it[0] = existing
                            it[1] = item.appInfo.packageName
                        }
                    )
                    replaceSource("")
                    grid[target.index] = folderSlotValue(created.id)
                }
                // フォルダを他のスロットに重ねた場合は、位置を入れ替える
                else -> {
                    if (source !is AppDragSource.Grid) return null
                    replaceSource(existing)
                    grid[target.index] = draggedValue
                }
            }
        }
        is AppDropTarget.DockSlot -> {
            if (source == AppDragSource.Dock(target.index)) return null
            dock.ensureIndex(target.index)
            val existing = dock[target.index]
            if (existing.isEmpty()) {
                replaceSource("")
            } else {
                // 使用中のスロットに重ねた場合は、ドラッグ元と入れ替える
                // （アプリドロワーから持ってきた場合は入れ替え先がないため何もしない）
                if (source == AppDragSource.Drawer) return null
                replaceSource(existing)
            }
            dock[target.index] = draggedValue
        }
        else -> return null
    }

    // フォルダの中から取り出して中身が1つだけになったフォルダは、フォルダをやめて
    // 残ったアプリそのものの表示に戻す（フォルダの中の空きスロットにアプリを追加していく
    // ときなどは判定せず、取り出したときだけ判定する）
    var closeFolder = false
    if (source is AppDragSource.FolderSlot) {
        val remaining = editedFolders[source.folderId]?.packageNames?.filter { it.isNotEmpty() }
        val folderSlotIndex = grid.indexOf(folderSlotValue(source.folderId))
        if (remaining != null && remaining.size == 1 && folderSlotIndex >= 0) {
            grid[folderSlotIndex] = remaining.first()
            editedFolders.remove(source.folderId)
            deleteFolder(prefs, source.folderId)
        }
        // フォルダの外へ持ち出した場合や、フォルダ自体がなくなった場合はポップアップを閉じる
        val movedOut = target is AppDropTarget.GridSlot || target is AppDropTarget.DockSlot
        closeFolder = movedOut || source.folderId !in editedFolders
    }

    return SlotDropResult(SlotArrangement(grid, dock, editedFolders), closeFolder)
}
