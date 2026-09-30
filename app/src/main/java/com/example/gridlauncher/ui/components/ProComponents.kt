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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

/** 購入画面に並べる、PROで使える機能のグループ（見出し・ひとこと・機能の一覧）。 */
private data class ProFeatureGroup(val tag: String, val caption: String, val features: List<String>)

/** PROで使える機能の一覧（購入画面に表示する）。 */
private val ProFeatureGroups = listOf(
    ProFeatureGroup(
        "VISUAL", "見た目を思いのままに",
        listOf(
            "テーマのプリセットとフォントの変更",
            "アイコンパック（Nova Launcher 対応のもの）",
            "アクセントカラー2・ウィジェットごとの配色・自由な色選び",
            "ヘッダーの文字を好きな言葉に"
        )
    ),
    ProFeatureGroup(
        "WIDGET", "ウィジェットをもっと自由に",
        listOf(
            "ウィジェットを重ねてスワイプで切り替え（自動切り替えつき）",
            "NOW PLAYING ウィジェット",
            "外部ウィジェットを無制限に配置"
        )
    ),
    ProFeatureGroup(
        "CONTROL", "操作を自分好みに",
        listOf(
            "ジェスチャーに動作を割り当て（通知パネル・画面オフなど）",
            "APP LIST・QUICK ACCESS・DOCK の並びとページ数の細かい設定"
        )
    ),
    ProFeatureGroup(
        "BACKUP", "設定を守る",
        listOf("設定のバックアップ・復元（機種変更にも）")
    )
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
                Text("ACCESS LEVEL: UNRESTRICTED", fontFamily = CyberFont, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                if (featureName != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = colors.accent, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ACCESS DENIED:「$featureName」は PRO モジュールです", fontFamily = CyberFont, fontSize = 11.sp, color = colors.accent)
                    }
                }
                Text(
                    "ホーム画面を、完全に自分仕様のターミナルへ。\n一度の購入で、すべての拡張モジュールが解放されます。",
                    fontFamily = CyberFont,
                    fontSize = 11.sp,
                    color = colors.text.copy(alpha = 0.7f)
                )
                // 機能の一覧は長いので、画面の低い端末や横画面でもボタンまで届くよう、この部分だけスクロールさせる
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    ProFeatureGroups.forEach { group ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("[ ${group.tag} ]", fontFamily = CyberFont, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = colors.accent)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(group.caption, fontFamily = CyberFont, fontSize = 10.sp, color = colors.text.copy(alpha = 0.6f))
                            }
                            group.features.forEach { feature ->
                                Text("・$feature", fontFamily = CyberFont, fontSize = 11.sp, color = colors.text, modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
                Text(
                    "買い切り・サブスクなし／同じ Google アカウントなら機種変更後も復元できます／広告なし・通信なし",
                    fontFamily = CyberFont,
                    fontSize = 9.sp,
                    color = colors.text.copy(alpha = 0.5f)
                )
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
