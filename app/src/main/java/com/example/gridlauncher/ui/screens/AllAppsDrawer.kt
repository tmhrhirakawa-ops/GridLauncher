package com.example.gridlauncher.ui.screens

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.ui.components.AppCard
import com.example.gridlauncher.ui.components.DockAppCard
import com.example.gridlauncher.ui.drag.AppDragItem
import com.example.gridlauncher.ui.drag.AppDragPayload
import com.example.gridlauncher.ui.drag.AppDragSource
import com.example.gridlauncher.ui.drag.LocalAppDragState
import com.example.gridlauncher.ui.drag.appDragSource
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.getFrequentApps
import com.example.gridlauncher.util.hasUsageStatsPermission
import android.content.res.Configuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val filteredApps = remember(allApps, searchQuery) { allApps.filter { it.label.contains(searchQuery, ignoreCase = true) } }
    val context = LocalContext.current
    // よく使うアプリは使用状況統計（過去1週間分）の集計が重いため、メインスレッドを止めないよう
    // バックグラウンドで取得する。検索中に表示から外れても再取得しないよう、ドロワーを開いている間は保持する
    val hasUsagePermission = remember { hasUsageStatsPermission(context) }
    val frequentApps by produceState(initialValue = emptyList<AppInfo>(), allApps, hasUsagePermission) {
        if (hasUsagePermission) {
            value = withContext(Dispatchers.IO) { getFrequentApps(context, allApps) }
        }
    }
    // アプリを長押し→ドラッグでホーム画面に配置する間は、ドロワーを閉じずに透明にしておく。
    // ドロワーは別ウィンドウのため、閉じるとドラッグ中の指の追跡が途切れてしまう
    // （ドロップ後にホーム画面側がドロワーを閉じる）
    val dragState = LocalAppDragState.current
    val isDraggingFromDrawer = dragState?.payload?.source == AppDragSource.Drawer

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = LocalCyberColors.current.bg,
        scrimColor = if (isDraggingFromDrawer) Color.Transparent else BottomSheetDefaults.ScrimColor,
        modifier = Modifier
            .fillMaxHeight(0.95f) // 画面の95%の高さまで表示
            .alpha(if (isDraggingFromDrawer) 0f else 1f)
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
                if (hasUsagePermission) {
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
                                    isMonochrome = appInfo.iconIsMonochrome,
                                    icon = appInfo.icon,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .fillMaxHeight() // 幅を80dpに固定して統一
                                        .appDragSource { AppDragPayload(AppDragSource.Drawer, AppDragItem.App(appInfo)) },
                                    onClick = {
                                        val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                                        if (launchIntent != null) {
                                            context.startActivity(launchIntent)
                                            onDismiss()
                                        }
                                    },
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
                        isMonochrome = appInfo.iconIsMonochrome,
                        icon = appInfo.icon,
                        modifier = Modifier
                            .aspectRatio(2.5f) // ACCESS GRIDの比率に近い形
                            .appDragSource { AppDragPayload(AppDragSource.Drawer, AppDragItem.App(appInfo)) },
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.packageName)
                            if (launchIntent != null) {
                                context.startActivity(launchIntent)
                                onDismiss()
                            }
                        },
                    )
                }
            }
        }
    }
}
