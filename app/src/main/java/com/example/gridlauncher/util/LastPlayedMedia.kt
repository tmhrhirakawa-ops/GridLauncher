package com.example.gridlauncher.util

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.view.KeyEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

/**
 * 最後に再生されたメディア。再生中のメディアがないときに、NOW PLAYINGウィジェットに表示する。
 *
 * @property packageName 再生していたアプリのパッケージ名。
 * @property title タイトル。
 * @property artist アーティスト/チャンネル名など。
 */
data class LastPlayedMedia(val packageName: String, val title: String?, val artist: String?)

private const val KEY_PACKAGE = "last_played_package"
private const val KEY_TITLE = "last_played_title"
private const val KEY_ARTIST = "last_played_artist"

private fun launcherPrefs(context: Context): SharedPreferences =
    context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE)

private fun loadLastPlayedMedia(prefs: SharedPreferences): LastPlayedMedia? {
    val packageName = prefs.getString(KEY_PACKAGE, null) ?: return null
    return LastPlayedMedia(packageName, prefs.getString(KEY_TITLE, null), prefs.getString(KEY_ARTIST, null))
}

/**
 * 最後に再生されたメディアとして保存する。再生情報は数秒ごとに取り直されるため、
 * 内容が変わったときだけ書き込む。
 */
fun saveLastPlayedMedia(context: Context, media: LastPlayedMedia) {
    val prefs = launcherPrefs(context)
    if (loadLastPlayedMedia(prefs) == media) return
    prefs.edit {
        putString(KEY_PACKAGE, media.packageName)
        putString(KEY_TITLE, media.title)
        putString(KEY_ARTIST, media.artist)
    }
}

/** 最後に再生されたメディア。保存し直されると更新される。 */
@Composable
fun rememberLastPlayedMedia(): LastPlayedMedia? {
    val context = LocalContext.current
    val prefs = remember { launcherPrefs(context) }
    var media by remember { mutableStateOf(loadLastPlayedMedia(prefs)) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPrefs, key ->
            if (key == KEY_PACKAGE || key == KEY_TITLE || key == KEY_ARTIST) media = loadLastPlayedMedia(sharedPrefs)
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return media
}

/**
 * 再生中のセッションがないときに、最後に再生していたアプリで再生を再開させる。
 * システムに「再生」のメディアボタンを送ると、最後にメディアを再生していたアプリへ届く
 * （アプリが終了していても、メディアボタンに対応していれば再生が再開される）。
 */
fun resumeLastPlayedMedia(context: Context) {
    val audioManager = context.getSystemService(AudioManager::class.java) ?: return
    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
    audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
}
