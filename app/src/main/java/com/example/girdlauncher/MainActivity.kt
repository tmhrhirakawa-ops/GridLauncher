package com.example.girdlauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.girdlauncher.ui.screens.CyberLauncherScreen
import com.example.girdlauncher.ui.theme.GirdLauncherTheme
import com.example.girdlauncher.util.AppWidgetConfigureResultBridge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GirdLauncherTheme {
                CyberLauncherScreen()
            }
        }
    }

    // AppWidgetHost.startAppWidgetConfigureActivityForResult()（ウィジェットの設定画面起動）は
    // 昔ながらのrequestCode方式のため、ここで受け取ってCompose側へ橋渡しする
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppWidgetConfigureResultBridge.REQUEST_CODE) {
            val callback = AppWidgetConfigureResultBridge.onResult
            AppWidgetConfigureResultBridge.onResult = null
            callback?.invoke(resultCode)
        }
    }
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, device = "spec:width=1280dp,height=800dp,dpi=240,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun LauncherPreview() {
    GirdLauncherTheme {
        CyberLauncherScreen()
    }
}
