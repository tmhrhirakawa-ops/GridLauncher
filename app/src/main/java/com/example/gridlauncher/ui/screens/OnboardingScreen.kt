package com.example.gridlauncher.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.OnboardingSteps

/**
 * 初回起動時のオンボーディング画面。「ウェルカム」→ 各種権限/設定案内 → 「完了」の一本道。
 * どのステップも「次へ」でスキップして進める。
 *
 * @param onFinish 最後まで進んだときのコールバック（呼び出し側で完了フラグを保存する想定）。
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current

    // 0=ウェルカム, 1..N=各ステップ, N+1=完了
    var currentStep by remember { mutableIntStateOf(0) }
    val lastStep = OnboardingSteps.size + 1

    // 設定画面から戻ってきたときに、現在のステップの充足状況を再判定できるようにする
    var resumeSignal by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                resumeSignal++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Surface(color = colors.bg, modifier = Modifier.fillMaxSize()) {
        // Edge-to-Edge 表示のため、ナビゲーションバー（3ボタン/ジェスチャー）・ステータスバー・
        // ディスプレイカットアウトの分を避けてから余白を取る（ボタンがバーと重ならないように）
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(32.dp)) {
            // 進捗ドット（ウェルカム・完了を含めない、各ステップの数だけ表示）
            if (currentStep in 1..OnboardingSteps.size) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OnboardingSteps.indices.forEach { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == currentStep - 1) colors.accent else colors.border
                                )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (currentStep) {
                    0 -> {
                        Text(
                            "SYSTEM ONLINE",
                            fontFamily = CyberFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "GRIDLAUNCHER",
                            fontFamily = CyberFont,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.text,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "はじめる前に、いくつかの設定を確認しましょう。",
                            fontFamily = CyberFont,
                            fontSize = 13.sp,
                            color = colors.text.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                    lastStep -> {
                        Text(
                            "READY",
                            fontFamily = CyberFont,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "準備完了",
                            fontFamily = CyberFont,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "設定はあとからいつでも見直せます。さっそく使ってみましょう。",
                            fontFamily = CyberFont,
                            fontSize = 13.sp,
                            color = colors.text.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {
                        val step = OnboardingSteps[currentStep - 1]
                        val satisfied = remember(currentStep, resumeSignal) { step.isSatisfied(context) }
                        Text(
                            step.title,
                            fontFamily = CyberFont,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            step.description,
                            fontFamily = CyberFont,
                            fontSize = 13.sp,
                            color = colors.text.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (satisfied) {
                            Text(
                                "✓ 設定済み",
                                fontFamily = CyberFont,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        } else {
                            OutlinedButton(
                                onClick = { context.startActivity(step.settingsIntent(context)) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                                border = BorderStroke(1.dp, colors.accent)
                            ) {
                                Text("設定を開く", fontFamily = CyberFont, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (currentStep in 1 until lastStep) {
                    OutlinedButton(
                        onClick = { currentStep-- },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text.copy(alpha = 0.7f)),
                        border = BorderStroke(1.dp, colors.border)
                    ) {
                        Text("戻る", fontFamily = CyberFont, fontSize = 13.sp)
                    }
                } else {
                    Spacer(modifier = Modifier)
                }

                Button(
                    onClick = {
                        if (currentStep == lastStep) {
                            onFinish()
                        } else {
                            currentStep++
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = when (currentStep) {
                            0 -> "はじめる"
                            lastStep -> "ホームへ進む"
                            else -> "次へ"
                        },
                        fontFamily = CyberFont,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
