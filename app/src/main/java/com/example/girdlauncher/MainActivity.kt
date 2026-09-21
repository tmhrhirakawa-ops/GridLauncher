package com.example.girdlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.girdlauncher.ui.screens.CyberLauncherScreen
import com.example.girdlauncher.ui.theme.GirdLauncherTheme

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
}

@Preview(showBackground = true, widthDp = 1280, heightDp = 800, device = "spec:width=1280dp,height=800dp,dpi=240,isRound=false,chinSize=0dp,orientation=landscape")
@Composable
fun LauncherPreview() {
    GirdLauncherTheme {
        CyberLauncherScreen()
    }
}
