package com.example.gridlauncher.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.ui.components.AppGridTile
import com.example.gridlauncher.ui.components.AppTileGrid
import com.example.gridlauncher.ui.theme.CyberFont
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
    val filteredApps = allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalCyberColors.current.bg,
        modifier = Modifier.fillMaxHeight(0.95f) // AllAppsDrawerと同じ高さに
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            // ヘッダー（タイトル）
            Text("SELECT APP", fontFamily = CyberFont, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.accent)
            Spacer(modifier = Modifier.height(16.dp))

            // 検索バー
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("SEARCH APPS...", fontFamily = CyberFont, fontSize = 14.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f)) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CyberFont, color = LocalCyberColors.current.text),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LocalCyberColors.current.accent,
                    unfocusedBorderColor = LocalCyberColors.current.border,
                    cursorColor = LocalCyberColors.current.accent
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
            
            // アプリ一覧（AllAppsDrawerと同じく、インストールした順に並べる）
            AppTileGrid(
                apps = filteredApps,
                resetKey = searchQuery,
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
