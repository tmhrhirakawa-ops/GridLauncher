package com.example.girdlauncher.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as lazyListItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.model.AppInfo
import com.example.girdlauncher.ui.components.AppActionDialog
import com.example.girdlauncher.ui.components.AppCard
import com.example.girdlauncher.ui.components.DockAppCard
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.getFrequentApps
import com.example.girdlauncher.util.hasUsageStatsPermission
import com.example.girdlauncher.util.requestUninstall
import android.content.res.Configuration

/**
 * インストールされているすべてのアプリと、よく使うアプリを表示するボトムシートドロワー。
 *
 * @param allApps インストールされているすべてのアプリのリスト。
 * @param onDismiss ドロワーが閉じられるときに呼び出されるコールバック。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllAppsDrawer(allApps: List<AppInfo>, onDismiss: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredApps = allApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
    val context = LocalContext.current
    var uninstallTarget by remember { mutableStateOf<AppInfo?>(null) } // 長押しでアンインストール確認中のアプリ

    uninstallTarget?.let { appInfo ->
        AppActionDialog(
            appName = appInfo.label,
            onDismiss = { uninstallTarget = null },
            onUninstall = {
                requestUninstall(context, appInfo.packageName)
                uninstallTarget = null
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalCyberColors.current.bg,
        modifier = Modifier.fillMaxHeight(0.95f) // 画面の95%の高さまで表示
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
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

            // よく使うアプリ（検索していないときのみ表示）
            if (searchQuery.isEmpty()) {
                val hasPermission = remember { hasUsageStatsPermission(context) }
                
                if (hasPermission) {
                    val frequentApps = remember { getFrequentApps(context, allApps) }
                    
                    if (frequentApps.isNotEmpty()) {
                        Text("FREQUENT APPS", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.accent)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        ) {
                            lazyListItems(frequentApps) { appInfo ->
                                DockAppCard(
                                    name = appInfo.label,
                                    packageName = appInfo.packageName,
                                    icon = appInfo.icon,
                                    modifier = Modifier.width(80.dp).fillMaxHeight(), // 幅を80dpに固定して統一
                                    onClick = {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                            onDismiss()
                                        }
                                    },
                                    onLongClick = { uninstallTarget = appInfo }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                } else {
                    // 権限がない場合は、権限設定画面へのボタンを表示
                    Surface(
                        onClick = {
                            val intent = Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, LocalCyberColors.current.accent.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("⚠️ REQUIRES USAGE ACCESS TO SHOW FREQUENT APPS", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 全アプリのグリッド
            Text("ALL APPS // NODES", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.accent)
            Spacer(modifier = Modifier.height(8.dp))
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
                        icon = appInfo.icon,
                        modifier = Modifier.aspectRatio(2.5f), // ACCESS GRIDの比率に近い形
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                                onDismiss()
                            }
                        },
                        onLongClick = { uninstallTarget = appInfo }
                    )
                }
            }
        }
    }
}
