package com.example.gridlauncher.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * グリッド内でアプリアイコンとラベルを表示するためのコンポーザブル。
 *
 * @param name アプリの名前。
 * @param icon アプリのアイコン。
 * @param modifier レイアウトに適用するModifier。
 * @param packageName アプリのパッケージ名。デュオトーン加工のキャッシュキーに使用。
 * @param isMonochrome [icon]がモノクロレイヤー由来かどうか。デュオトーン加工方法の選択に使う。
 * @param isEditMode UIが編集モードかどうか。
 * @param notificationCount 通知（またはアプリバッジ）の件数。0以下の場合はバッジを表示しない。
 * @param isWallpaperMode 壁紙透過モードかどうか。
 * @param isCompact trueの場合、アプリ名は表示せずアイコンのみを中央に表示する
 *   （APP LISTのICON ONLYモードなど、正方形のスロット向け）。falseの場合は、アイコンと名前を
 *   横に並べる。ただし名前が横に入り切らないほど狭いスロットでは、アイコンの下に小さめに表示する
 *   （[cardLabelPlacement]参照）。
 * @param useOriginalIconColors trueの場合、アクセントカラーのデュオトーン加工をせず、
 *   アプリ本来の色のアイコンをそのまま表示する。
 * @param onClick カードがクリックされたときのコールバック。
 * @param onLongClick カードが長押しされたときのコールバック。
 * @param onRemoveClick 編集モードで削除アイコンがクリックされたときのコールバック。
 * @param removeBadgeInset 削除アイコン（✗バッジ）を右上の角からどれだけ内側にずらすか。
 *   このカードがウィジェット全体を占める場合（APP SLOTなど）、標準の4dpだと右上の
 *   リサイズハンドルの当たり判定と重なってしまうため、その場合だけ大きめの値を渡す。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppCard(
    name: String,
    icon: Drawable,
    modifier: Modifier = Modifier,
    packageName: String = "",
    isMonochrome: Boolean = false,
    isEditMode: Boolean = false,
    notificationCount: Int = 0,
    isWallpaperMode: Boolean = false,
    isCompact: Boolean = false,
    useOriginalIconColors: Boolean = false,
    removeBadgeInset: Dp = 4.dp,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRemoveClick: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (isWallpaperMode) LocalCyberColors.current.panel.copy(alpha = 0.55f) else LocalCyberColors.current.panel,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // 実アイコンを、ロゴの形は保ちつつアクセントカラーのデュオトーンに加工して表示する
            // （useOriginalIconColorsがtrueのときは加工せず本来の色のまま表示する）
            val bitmap = rememberAppIconBitmap(packageName, icon, isMonochrome, useOriginalIconColors)

            when (cardLabelPlacement(maxWidth, maxHeight, isCompact)) {
                CardLabelPlacement.NONE -> {
                    // アイコンのみを中央に表示する（アプリ名は表示しない）
                    Image(
                        bitmap = bitmap,
                        contentDescription = name,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
                CardLabelPlacement.BESIDE -> {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = name,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        CardLabel(text = name, fontSize = 12)
                    }
                }
                CardLabelPlacement.BELOW -> {
                    // 名前がアイコンの横に入り切らない狭いスロットでは、アイコンの下に小さめに表示する
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .align(Alignment.Center)
                    ) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = name,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        CardLabel(text = name, fontSize = 9)
                    }
                }
            }

            // 通知バッジ（件数表示）
            if (notificationCount > 0 && !isEditMode) {
                val label = if (notificationCount > 99) "99+" else notificationCount.toString()
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                        .border(1.dp, LocalCyberColors.current.accent, RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontFamily = CyberFont,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalCyberColors.current.accent,
                        maxLines = 1
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isEditMode,
                enter = scaleIn(initialScale = 0.4f) + fadeIn(),
                exit = scaleOut(targetScale = 0.4f) + fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd).padding(removeBadgeInset)
            ) {
                RemoveBadge(onClick = onRemoveClick)
            }
        }
    }
}

/**
 * アプリ名をアイコンの横に並べる表示に必要な、スロットの最小の幅。アイコン（28dp）と余白だけで
 * 約64dpを使うため、これより狭いと名前がほとんど表示できない。その場合はアイコンの下に表示する。
 */
private val LabelBesideMinWidth = 100.dp

/** アイコンの下にアプリ名を表示するのに必要な、スロットの最小の高さ。これより低い場合はアイコンのみにする。 */
private val LabelBelowMinHeight = 40.dp

/** カードの中でのアプリ名（フォルダ名）の表示位置。 */
internal enum class CardLabelPlacement {
    /** アイコンの横に並べる（通常）。 */
    BESIDE,

    /** アイコンの下に小さめに表示する（名前が横に入り切らない狭いスロット）。 */
    BELOW,

    /** 表示しない（ICON ONLYモード、または名前を入れる高さもない場合）。 */
    NONE
}

/** スロットの大きさ（[width]×[height]）から、アプリ名の表示位置を決める。 */
internal fun cardLabelPlacement(width: Dp, height: Dp, isCompact: Boolean): CardLabelPlacement = when {
    isCompact -> CardLabelPlacement.NONE
    width >= LabelBesideMinWidth -> CardLabelPlacement.BESIDE
    height >= LabelBelowMinHeight -> CardLabelPlacement.BELOW
    else -> CardLabelPlacement.NONE
}

/** カードに表示するアプリ名（フォルダ名）。入り切らない場合は流れるように表示する。 */
@Composable
internal fun CardLabel(text: String, fontSize: Int) {
    Text(
        text = text,
        fontFamily = CyberFont,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        color = LocalCyberColors.current.text,
        maxLines = 1,
        modifier = Modifier.basicMarquee()
    )
}
