package com.example.gridlauncher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.ui.components.AppGridTile
import com.example.gridlauncher.ui.components.AppTileGrid
import com.example.gridlauncher.ui.components.AppSortButton
import com.example.gridlauncher.ui.components.rememberAppSortOrder
import com.example.gridlauncher.util.sortAppsForList
import com.example.gridlauncher.ui.components.SearchableSheetHeader
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * グリッドスロットやドックに追加するアプリを選択するためのボトムシートダイアログ。
 *
 * @param allApps インストールされているすべてのアプリのリスト。
 * @param onDismiss ダイアログが閉じられるときに呼び出されるコールバック。
 * @param onAppSelected 選択したアプリのパッケージ名とともに呼び出されるコールバック。
 * @param useOriginalIconColors trueの場合、アイコンをアクセントカラーで加工せず本来の色のまま表示する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorDialog(
    allApps: List<AppInfo>,
    onDismiss: () -> Unit,
    onAppSelected: (String) -> Unit,
    useOriginalIconColors: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = remember(allApps, searchQuery) { allApps.filter { it.label.contains(searchQuery, ignoreCase = true) } }
    // 並べ替え（インストール順・名前順・カテゴリ順と昇順・降順。ALL APPSと共通で保存する）
    val context = LocalContext.current
    val (sortOrder, setSortOrder) = rememberAppSortOrder()
    val sortedSections = remember(filteredApps, sortOrder) { sortAppsForList(context, filteredApps, sortOrder) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalCyberColors.current.bg,
        modifier = Modifier.fillMaxHeight(0.95f) // AllAppsDrawerと同じ高さに
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // ヘッダー（タイトルと、右上の虫眼鏡で開く検索ボックス）
            SearchableSheetHeader(
                title = "SELECT APP",
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                actions = { AppSortButton(order = sortOrder, onOrderChange = setSortOrder) }
            )
            Spacer(modifier = Modifier.height(16.dp))

            // アプリ一覧（AllAppsDrawerと同じく、インストールした順に並べる）
            AppTileGrid(
                sections = sortedSections,
                resetKey = searchQuery to sortOrder,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { appInfo, tileModifier ->
                AppGridTile(
                    app = appInfo,
                    useOriginalIconColors = useOriginalIconColors,
                    modifier = tileModifier,
                    onClick = { onAppSelected(appInfo.packageName) }
                )
            }
        }
    }
}
