package com.example.gridlauncher.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.ui.components.AppCard
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * グリッドスロットやドックに追加するアプリを選択するためのボトムシートダイアログ。
 *
 * @param allApps インストールされているすべてのアプリのリスト。
 * @param onDismiss ダイアログが閉じられるときに呼び出されるコールバック。
 * @param onAppSelected 選択したアプリのパッケージ名とともに呼び出されるコールバック。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectorDialog(allApps: List<AppInfo>, onDismiss: () -> Unit, onAppSelected: (String) -> Unit) {
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
            
            // アプリ一覧グリッド (AllAppsDrawerと同じデザイン)
            val configuration = LocalConfiguration.current
            val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT
            LazyVerticalGrid(
                columns = GridCells.Fixed(if (isPortrait) 3 else 5), // 縦画面なら3列、横画面なら5列
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredApps) { appInfo ->
                    AppCard(
                        name = appInfo.label,
                        packageName = appInfo.packageName,
                        isMonochrome = appInfo.iconIsMonochrome,
                        icon = appInfo.icon,
                        modifier = Modifier.aspectRatio(2.5f), // ACCESS GRIDの比率に近い形
                        onClick = {
                            onAppSelected(appInfo.packageName)
                        }
                    )
                }
            }
        }
    }
}
