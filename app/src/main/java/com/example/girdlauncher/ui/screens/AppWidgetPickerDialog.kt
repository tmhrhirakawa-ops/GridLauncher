package com.example.girdlauncher.ui.screens

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.loadWidgetPreviewBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** 一覧に表示する、解決済みの1エントリ分の情報。 */
private data class WidgetProviderEntry(
    val info: AppWidgetProviderInfo,
    val appLabel: String,
    val widgetLabel: String,
    val appIcon: ImageBitmap,
    val previewImage: ImageBitmap?
)

/**
 * 他アプリが提供するAppWidgetを、プレビュー画像つきの一覧から選択させるボトムシートダイアログ。
 *
 * Androidの標準ウィジェット選択画面（`ACTION_APPWIDGET_PICK`）はアプリ名がテキストで並ぶだけで
 * どんな見た目のウィジェットか分かりづらいため、代わりにこの自作の一覧を使う。選択後の
 * バインド許可確認・設定画面起動・配置確定は呼び出し側（[com.example.girdlauncher.ui.screens.CyberLauncherScreen]）
 * が行う想定で、ここでは選択させるところまでを担当する。
 *
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 * @param onSelect ウィジェットが選択されたときのコールバック。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppWidgetPickerDialog(onDismiss: () -> Unit, onSelect: (AppWidgetProviderInfo) -> Unit) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current
    var searchQuery by remember { mutableStateOf("") }
    // 情報量の少ない小さな画面でも一覧をできるだけ広く使えるよう、検索欄は普段は
    // 虫眼鏡アイコンだけの最小状態にしておき、タップしたときだけ展開する
    var isSearchExpanded by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var isLoading by remember { mutableStateOf(true) }
    var entries by remember { mutableStateOf<List<WidgetProviderEntry>>(emptyList()) }

    LaunchedEffect(isSearchExpanded) {
        if (isSearchExpanded) searchFocusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        entries = withContext(Dispatchers.Default) {
            val packageManager = context.packageManager
            AppWidgetManager.getInstance(context).installedProviders.mapNotNull { info ->
                try {
                    val appInfo = packageManager.getApplicationInfo(info.provider.packageName, 0)
                    val appIcon = appInfo.loadIcon(packageManager).toBitmap(width = 96, height = 96).asImageBitmap()
                    WidgetProviderEntry(
                        info = info,
                        appLabel = appInfo.loadLabel(packageManager).toString(),
                        widgetLabel = info.loadLabel(packageManager),
                        appIcon = appIcon,
                        previewImage = loadWidgetPreviewBitmap(context, info)
                    )
                } catch (e: Exception) {
                    null
                }
            }.sortedWith(compareBy({ it.appLabel }, { it.widgetLabel }))
        }
        isLoading = false
    }

    val filteredEntries = entries.filter {
        it.appLabel.contains(searchQuery, ignoreCase = true) || it.widgetLabel.contains(searchQuery, ignoreCase = true)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.bg,
        modifier = Modifier.fillMaxHeight(0.95f)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (isSearchExpanded) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("SEARCH WIDGETS...", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text.copy(alpha = 0.5f)) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontFamily = CyberFont, color = colors.text),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accent,
                            unfocusedBorderColor = colors.border,
                            cursorColor = colors.accent
                        ),
                        trailingIcon = {
                            IconButton(onClick = { isSearchExpanded = false; searchQuery = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "検索を閉じる", tint = colors.text)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(searchFocusRequester)
                    )
                } else {
                    Text(
                        "外部ウィジェットを追加",
                        fontFamily = CyberFont,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { isSearchExpanded = true }) {
                        Icon(Icons.Filled.Search, contentDescription = "検索", tint = colors.text)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "読み込み中...",
                        fontFamily = CyberFont,
                        fontSize = 12.sp,
                        color = colors.text.copy(alpha = 0.5f)
                    )
                }
            } else if (filteredEntries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "ウィジェットが見つかりません",
                        fontFamily = CyberFont,
                        fontSize = 12.sp,
                        color = colors.text.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize().padding(bottom = 16.dp)
                ) {
                    items(filteredEntries) { entry ->
                        WidgetProviderCard(entry = entry, onClick = { onSelect(entry.info) })
                    }
                }
            }
        }
    }
}

@Composable
private fun WidgetProviderCard(entry: WidgetProviderEntry, onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = colors.panel.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = entry.appIcon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(entry.widgetLabel, fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.text)
                    Text(entry.appLabel, fontFamily = CyberFont, fontSize = 10.sp, color = colors.text.copy(alpha = 0.6f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(colors.bg, RoundedCornerShape(4.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (entry.previewImage != null) {
                    Image(
                        bitmap = entry.previewImage,
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                    )
                } else {
                    Image(
                        bitmap = entry.appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}
