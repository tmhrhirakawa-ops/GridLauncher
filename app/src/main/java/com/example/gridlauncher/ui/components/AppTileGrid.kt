package com.example.gridlauncher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.model.AppInfo
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.AppListSection

/** 1マスの最小の幅。この幅が入るだけ列を並べる。 */
private val MinCellWidth = 80.dp

/**
 * アプリを、縦にスクロールするグリッドに並べる（ALL APPS・SELECT APP用）。
 * 列数は表示できる幅に合わせて決める。
 *
 * @param sections 並べるアプリのまとまり（この順に、左上から横に詰めて並べる）。見出しのあるまとまり
 *   （カテゴリ順のときのカテゴリ）は、その前に横幅いっぱいの見出しを入れる。
 * @param modifier レイアウトに適用するModifier。
 * @param resetKey この値が変わったら先頭までスクロールを戻す（検索語・並べ替えなど）。
 * @param tile 1マス分の表示。通常は[AppGridTile]を使う。
 */
@Composable
fun AppTileGrid(
    sections: List<AppListSection>,
    modifier: Modifier = Modifier,
    resetKey: Any? = null,
    tile: @Composable (app: AppInfo, modifier: Modifier) -> Unit
) {
    val gridState = rememberLazyGridState()
    LaunchedEffect(resetKey) {
        if (gridState.firstVisibleItemIndex != 0) gridState.scrollToItem(0)
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = MinCellWidth),
        state = gridState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.consumeUpwardSheetFling()
    ) {
        sections.forEach { section ->
            section.title?.let { title ->
                item(key = "section:$title", span = { GridItemSpan(maxLineSpan) }) {
                    AppSectionHeader(title = title, count = section.apps.size)
                }
            }
            items(section.apps, key = { it.packageName }) { app ->
                tile(app, Modifier.fillMaxWidth())
            }
        }
    }
}

/** カテゴリ順のときの、カテゴリの見出し（名前と件数、下に細い線）。 */
@Composable
private fun AppSectionHeader(title: String, count: Int) {
    val colors = LocalCyberColors.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(6.dp).background(colors.accent))
            Spacer(modifier = Modifier.width(6.dp))
            Text(title.uppercase(), fontFamily = CyberFont, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent, maxLines = 1)
            Spacer(modifier = Modifier.width(6.dp))
            Text("// $count", fontFamily = CyberFont, fontSize = 10.sp, color = colors.text.copy(alpha = 0.5f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))
    }
}

/**
 * [AppTileGrid]の1マス分。大きめのアイコンの下にアプリ名を表示する。
 *
 * @param app 表示するアプリ。
 * @param onClick タップされたときのコールバック。
 * @param modifier レイアウトに適用するModifier（ドラッグ元の指定などもここで渡す）。
 * @param useOriginalIconColors trueの場合、アイコンをアクセントカラーで加工せず本来の色で表示する。
 * @param iconSize アイコンの大きさ。加工時の解像度（最大160px）でぼやけない程度にしておく。
 */
@Composable
fun AppGridTile(
    app: AppInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    useOriginalIconColors: Boolean = false,
    iconSize: Dp = 40.dp
) {
    val colors = LocalCyberColors.current
    val bitmap = rememberAppIconBitmap(app.packageName, app.icon, app.iconIsMonochrome, useOriginalIconColors)
    // 枠線・背景は、APP LISTなどのアプリカード（AppCard）と同じ見た目にそろえる
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = colors.panel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp)
        ) {
            Image(bitmap = bitmap, contentDescription = app.label, modifier = Modifier.size(iconSize))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                app.label,
                fontFamily = CyberFont,
                fontSize = 11.sp,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
