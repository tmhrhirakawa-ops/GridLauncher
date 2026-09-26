package com.example.gridlauncher.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DeveloperMode
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.Opacity
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.gridlauncher.model.QuickActionId

/** QUICK ACCESSのボタンに表示するアイコン。 */
val QuickActionId.icon: ImageVector
    get() = when (this) {
        QuickActionId.SETTINGS -> Icons.Outlined.Settings
        QuickActionId.WIFI -> Icons.Outlined.Wifi
        QuickActionId.DISPLAY -> Icons.Outlined.DisplaySettings
        QuickActionId.BLUETOOTH -> Icons.Outlined.Bluetooth
        QuickActionId.DEVELOP -> Icons.Outlined.DeveloperMode
        QuickActionId.COLOR -> Icons.Outlined.Palette
        QuickActionId.THEME -> Icons.Outlined.Contrast
        QuickActionId.VOLUME -> Icons.AutoMirrored.Outlined.VolumeUp
        QuickActionId.BRIGHTNESS -> Icons.Outlined.BrightnessMedium
        QuickActionId.APP -> Icons.Outlined.Apps
        QuickActionId.WALLPAPER -> Icons.Outlined.Wallpaper
        QuickActionId.WALLPAPER_TRANSPARENT -> Icons.Outlined.Opacity
        QuickActionId.GRID_LINES -> Icons.Outlined.GridOn
        QuickActionId.CUSTOMIZE -> Icons.Outlined.Tune
    }
