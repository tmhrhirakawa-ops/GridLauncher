package com.example.gridlauncher.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.OnboardingSteps

/**
 * アプリ起動時、未設定の権限/設定がある場合に一度だけ表示するボトムシート。
 * 各項目は「設定を開く」ボタンを持ち、既に設定済みの項目はボタンがグレーアウトして
 * 「設定済みです」と表示される。
 *
 * @param resumeSignal 設定画面から戻ってきた際に各項目の充足状況を再判定させるためのキー。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingPermissionsSheet(
    resumeSignal: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.panel
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                "未設定の項目があります",
                fontFamily = CyberFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.text
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "あとからいつでも設定できます。",
                fontFamily = CyberFont,
                fontSize = 12.sp,
                color = colors.text.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(20.dp))

            OnboardingSteps.forEach { step ->
                val satisfied = remember(resumeSignal) { step.isSatisfied(context) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            step.title,
                            fontFamily = CyberFont,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.text
                        )
                        Text(
                            step.description,
                            fontFamily = CyberFont,
                            fontSize = 11.sp,
                            color = colors.text.copy(alpha = 0.6f)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { context.startActivity(step.settingsIntent(context)) },
                        enabled = !satisfied,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                    ) {
                        Text(
                            if (satisfied) "設定済みです" else "設定を開く",
                            fontFamily = CyberFont,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
