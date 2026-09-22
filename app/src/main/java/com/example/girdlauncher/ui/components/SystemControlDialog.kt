package com.example.girdlauncher.ui.components

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.core.net.toUri
import com.example.girdlauncher.ui.theme.LocalCyberColors
import com.example.girdlauncher.util.isNotificationListenerEnabled

/**
 * ポップアップの起点となったボタン（アンカー）の真上中央に、ポップアップを配置するための
 * [PopupPositionProvider]。ボタンの近くに表示してほしいという要望に対応するためのもの。
 */
private class AboveAnchorPositionProvider(private val marginPx: Int) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val y = anchorBounds.top - popupContentSize.height - marginPx
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceAtLeast(0)
        )
    }
}

/**
 * QUICK ACCESSの「VOLUME」ボタンから開く、メディア音量を調整するポップアップ。
 * ボタンのすぐ上に「にゅいっ」と弾むように表示される縦スライダーで、一番下のスピーカー
 * アイコンをタップするとミュートを切り替えられる。
 *
 * ミュート操作（[AudioManager.ADJUST_TOGGLE_MUTE]）には端末によって通知へのアクセス権限が
 * 必要なため、権限がない場合はタップ時に理由を説明したうえで権限付与画面へ案内する。
 *
 * @param onDismiss ポップアップが閉じられるときのコールバック。
 */
@Composable
fun VolumeControlDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }
    var showPermissionRationale by remember { mutableStateOf(false) }
    val isMuted = volume <= 0f

    fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun toggleMute() {
        if (!isNotificationListenerEnabled(context)) {
            showPermissionRationale = true
            return
        }
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_TOGGLE_MUTE, 0)
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume
    }

    AnchoredControlPopup(onDismiss = onDismiss) {
        VerticalLevelSlider(
            value = volume,
            fillColor = colors.accent,
            onValueChange = { newValue ->
                volume = newValue
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (newValue * maxVolume).roundToIntSafe(), 0)
            }
        )
        Spacer(modifier = Modifier.height(10.dp))
        Icon(
            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Mute",
            tint = if (isMuted) colors.text.copy(alpha = 0.4f) else colors.accent,
            modifier = Modifier
                .size(22.dp)
                .clickable { toggleMute() }
        )
    }

    if (showPermissionRationale) {
        PermissionRationaleDialog(
            message = "ミュート操作を行うには、GridLauncherへの通知へのアクセスを許可してください。",
            onConfirm = {
                showPermissionRationale = false
                openNotificationAccessSettings()
            },
            onDismiss = { showPermissionRationale = false }
        )
    }
}

/**
 * QUICK ACCESSの「BRIGHT」ボタンから開く、画面の明るさを調整するポップアップ。
 * 端末全体の明るさに恒久的に反映するため、システム設定を書き換える権限
 * （[Settings.System.canWrite]）を使う。権限がない場合はドラッグ時に理由を説明したうえで
 * 権限付与画面へ案内する。
 *
 * @param onDismiss ポップアップが閉じられるときのコールバック。
 */
@Composable
fun BrightnessControlDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalCyberColors.current
    var showPermissionRationale by remember { mutableStateOf(false) }
    var brightness by remember {
        mutableFloatStateOf(
            try {
                Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f
            } catch (e: Exception) {
                0.5f
            }
        )
    }

    fun openWriteSettingsSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, "package:${context.packageName}".toUri()).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    fun applyBrightness(newValue: Float) {
        if (!Settings.System.canWrite(context)) {
            showPermissionRationale = true
            return
        }
        brightness = newValue
        try {
            // 自動調光がオンだとすぐ上書きされてしまうため、手動モードに切り替える
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (newValue * 255).roundToIntSafe().coerceIn(1, 255)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    AnchoredControlPopup(onDismiss = onDismiss) {
        VerticalLevelSlider(
            value = brightness,
            fillColor = colors.core,
            onValueChange = { newValue -> applyBrightness(newValue) }
        )
    }

    if (showPermissionRationale) {
        PermissionRationaleDialog(
            message = "画面の明るさを端末全体に恒久的に反映するには、GridLauncherに「システム設定の変更」を許可してください。許可しない場合、他のアプリを開くと明るさが元に戻ります。",
            onConfirm = {
                showPermissionRationale = false
                openWriteSettingsSettings()
            },
            onDismiss = { showPermissionRationale = false }
        )
    }
}

private fun Float.roundToIntSafe(): Int = kotlin.math.round(this).toInt()

/**
 * ボタンの真上に「にゅいっ」と弾むように表示される、縦長のポップアップパネルの共通実装。
 */
@Composable
private fun AnchoredControlPopup(
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalCyberColors.current
    val density = LocalDensity.current
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Popup(
        popupPositionProvider = remember { AboveAnchorPositionProvider(marginPx = with(density) { 10.dp.roundToPx() }) },
        onDismissRequest = onDismiss
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = scaleIn(
                initialScale = 0.5f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(),
            exit = scaleOut(targetScale = 0.5f) + fadeOut()
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.bg,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(10.dp),
                    content = content
                )
            }
        }
    }
}

/**
 * 音量・明るさ調整用の縦長スライダー。タップ・ドラッグした位置（下=0f、上=1f）に応じて
 * 0f〜1fの値を返す。
 */
@Composable
private fun VerticalLevelSlider(
    value: Float,
    fillColor: Color,
    onValueChange: (Float) -> Unit
) {
    val colors = LocalCyberColors.current

    fun valueFromY(y: Float, height: Int): Float = (1f - (y / height)).coerceIn(0f, 1f)

    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = Modifier
            .width(32.dp)
            .height(130.dp)
            .background(colors.border, RoundedCornerShape(16.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset -> onValueChange(valueFromY(offset.y, size.height)) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onValueChange(valueFromY(change.position.y, size.height))
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(value.coerceIn(0f, 1f))
                .background(fillColor, RoundedCornerShape(16.dp))
        )
    }
}
