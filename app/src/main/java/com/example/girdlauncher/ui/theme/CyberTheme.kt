package com.example.girdlauncher.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.example.girdlauncher.R

/**
 * Light theme background color.
 */
val LightBgColor = Color(0xFFF7DFCB)

/**
 * Light theme panel color.
 */
val LightPanelColor = Color(0xFFF2D3B8)

/**
 * Light theme accent color.
 */
val LightAccentColor = Color(0xFFFF5722)

/**
 * Light theme text color.
 */
val LightTextColor = Color(0xFF4A4A4A)

/**
 * Light theme border color.
 */
val LightBorderColor = Color(0x334A4A4A)

/**
 * Light theme core color.
 */
val LightCoreColor = Color(0xFF1E3A8A)


/**
 * Dark theme background color.
 */
val DarkBgColor = Color(0xFF121212)

/**
 * Dark theme panel color.
 */
val DarkPanelColor = Color(0xFF1E1E1E)

/**
 * Dark theme accent color.
 */
val DarkAccentColor = Color(0xFFFF5722)

/**
 * Dark theme text color.
 */
val DarkTextColor = Color(0xFFE0E0E0)

/**
 * Dark theme border color.
 */
val DarkBorderColor = Color(0x33E0E0E0)

/**
 * Dark theme core color.
 */
val DarkCoreColor = Color(0xFF3B82F6) // ダークテーマ用に少し明るい青

/**
 * Data class holding the colors for the Cyber theme.
 *
 * @property bg Background color.
 * @property panel Panel color.
 * @property accent Accent color.
 * @property text Text color.
 * @property border Border color.
 * @property core Core color.
 */
data class CyberColors(
    val bg: Color,
    val panel: Color,
    val accent: Color,
    val text: Color,
    val border: Color,
    val core: Color
)

/**
 * CompositionLocal for providing [CyberColors] down the compose tree.
 */
val LocalCyberColors = staticCompositionLocalOf {
    CyberColors(LightBgColor, LightPanelColor, LightAccentColor, LightTextColor, LightBorderColor, LightCoreColor)
}

/**
 * Cyber font family used across the app.
 */
val CyberFont = FontFamily(
    Font(R.font.share_tech_mono)
)
