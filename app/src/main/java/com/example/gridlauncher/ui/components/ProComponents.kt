package com.example.gridlauncher.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.gridlauncher.billing.ProManager
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/** PROで使える機能の一覧（購入画面に表示する）。 */
private val ProFeatureList = listOf(
    "テーマ（配色・フォントのプリセット）とフォントの変更",
    "設定のバックアップ・復元",
    "ウィジェットのスタックと自動切り替え",
    "アクセントカラー2・ウィジェットごとの配色・パレットでの自由な色選び",
    "ヘッダー中央の文字の書き換え",
    "NOW PLAYING ウィジェット",
    "APP LIST・QUICK ACCESS・DOCK の並び・ページ数の詳細設定",
    "外部ウィジェットを3個以上配置"
)

/** PROの機能であることを示す小さな鍵つきのマーク。PRO解放済みのときは表示しない想定。 */
@Composable
fun ProBadge(modifier: Modifier = Modifier) {
    val colors = LocalCyberColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .background(colors.accent)
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.onAccent, modifier = Modifier.size(9.dp))
        Spacer(modifier = Modifier.width(2.dp))
        Text("PRO", fontFamily = CyberFont, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = colors.onAccent)
    }
}

/**
 * PROの機能を使おうとしたときに出す、PRO解放の案内と購入のダイアログ。
 *
 * @param featureName 使おうとした機能の名前（nullなら機能を指定せず、PROの案内だけを出す）。
 * @param onDismiss ダイアログが閉じられるときのコールバック。
 */
@Composable
fun ProUpgradeDialog(featureName: String?, onDismiss: () -> Unit) {
    val colors = LocalCyberColors.current
    val context = LocalContext.current
    val price by ProManager.price.collectAsState()
    val isPro by ProManager.isPro.collectAsState()
    var message by remember { mutableStateOf<String?>(null) }

    // 購入が完了してPROになったら、自動で閉じる
    LaunchedEffect(isPro) {
        if (isPro) onDismiss()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = colors.bg,
            border = BorderStroke(1.dp, colors.accent.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(colors.accent))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GRIDLAUNCHER PRO", fontFamily = CyberFont, fontSize = 16.sp, fontWeight = FontWeight.Black, color = colors.text)
                }
                if (featureName != null) {
                    Text("「$featureName」は PRO の機能です。", fontFamily = CyberFont, fontSize = 12.sp, color = colors.accent)
                }
                Text("一度の購入で、次の機能がすべて使えるようになります。", fontFamily = CyberFont, fontSize = 11.sp, color = colors.text.copy(alpha = 0.7f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                    ProFeatureList.forEach { feature ->
                        Text("・$feature", fontFamily = CyberFont, fontSize = 11.sp, color = colors.text)
                    }
                }
                message?.let { Text(it, fontFamily = CyberFont, fontSize = 10.sp, color = colors.text.copy(alpha = 0.6f)) }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.accent)
                        .clickable {
                            val activity = context.findActivity()
                            val launched = activity != null && ProManager.launchPurchase(activity)
                            if (!launched) {
                                message = "Google Play に接続できないため、今は購入できません。しばらくしてからもう一度お試しください。"
                            }
                        }
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        if (price != null) "PRO を解放する（$price）" else "PRO を解放する",
                        fontFamily = CyberFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onAccent
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "購入を復元",
                        fontFamily = CyberFont,
                        fontSize = 11.sp,
                        color = colors.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                ProManager.refreshPurchases()
                                message = "購入履歴を確認しています…（購入済みなら自動で解放されます）"
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        "閉じる",
                        fontFamily = CyberFont,
                        fontSize = 11.sp,
                        color = colors.text.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 6.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/** Composeの[Context]から、それを持っている[Activity]を探す（購入画面を開くのに必要）。 */
private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
