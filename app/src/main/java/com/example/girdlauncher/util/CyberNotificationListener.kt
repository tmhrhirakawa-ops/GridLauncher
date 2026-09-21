package com.example.girdlauncher.util

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * デバイスの通知状態・再生中メディアを監視するサービス。
 * 通知が来ているアプリのパッケージ名と、現在再生中のメディア情報を状態として保持します。
 */
class CyberNotificationListener : NotificationListenerService() {

    /**
     * 現在再生中（または一時停止中）のメディアの情報。
     *
     * @property packageName メディアを再生しているアプリのパッケージ名。
     * @property title 再生中のタイトル。
     * @property artist アーティスト/チャンネル名など。
     * @property isPlaying 再生中かどうか（falseの場合は一時停止中）。
     */
    data class NowPlayingInfo(
        val packageName: String,
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean
    )

    companion object {
        private val _activeNotifications = MutableStateFlow<Set<String>>(emptySet())
        val activeNotifications: StateFlow<Set<String>> = _activeNotifications

        private val _nowPlaying = MutableStateFlow<NowPlayingInfo?>(null)
        val nowPlaying: StateFlow<NowPlayingInfo?> = _nowPlaying

        // 操作対象となる現在アクティブなMediaController（サービスが生きている間のみ有効）
        private var activeController: MediaController? = null

        // ユーザーが「削除」した再生セッション。再度再生が始まるまでは表示を復活させない
        private var dismissedToken: MediaSession.Token? = null

        fun play() = activeController?.transportControls?.play()
        fun pause() = activeController?.transportControls?.pause()
        fun skipToNext() = activeController?.transportControls?.skipToNext()
        fun skipToPrevious() = activeController?.transportControls?.skipToPrevious()

        /**
         * 再生を停止したうえで、ウィジェットの表示も消す。
         */
        fun stopAndDismiss() {
            activeController?.transportControls?.stop()
            dismissedToken = activeController?.sessionToken
            _nowPlaying.value = null
        }
    }

    private var currentController: MediaController? = null

    private val mediaControllerCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) = refreshNowPlaying()
        override fun onMetadataChanged(metadata: MediaMetadata?) = refreshNowPlaying()
        override fun onSessionDestroyed() {
            currentController?.unregisterCallback(this)
            currentController = null
            activeController = null
            _nowPlaying.value = null
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateActiveSessions(controllers)
        }

    // OEM（省電力機能など）によってはセッション変更イベントが確実に届かないことがあるため、
    // 保険として定期的にセッション一覧を再取得する
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            pollActiveSessions()
            pollHandler.postDelayed(this, 3000)
        }
    }

    private fun pollActiveSessions() {
        try {
            val mediaSessionManager = getSystemService(MediaSessionManager::class.java)
            val component = ComponentName(this, CyberNotificationListener::class.java)
            updateActiveSessions(mediaSessionManager.getActiveSessions(component))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()

        try {
            val mediaSessionManager = getSystemService(MediaSessionManager::class.java)
            val component = ComponentName(this, CyberNotificationListener::class.java)
            mediaSessionManager.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
        } catch (e: Exception) {
            // 通知アクセス権限が未許可などで取得できない場合は無視する
            e.printStackTrace()
        }
        pollActiveSessions()
        pollHandler.postDelayed(pollRunnable, 3000)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        pollHandler.removeCallbacks(pollRunnable)
        try {
            val mediaSessionManager = getSystemService(MediaSessionManager::class.java)
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        currentController?.unregisterCallback(mediaControllerCallback)
        currentController = null
        activeController = null
        _nowPlaying.value = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        updateNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        updateNotifications()
    }

    private fun updateNotifications() {
        try {
            val notifications = getActiveNotifications() ?: return

            val packages = notifications.filter { sbn ->
                // 通知マークとしてふさわしいものだけをフィルタリング
                // フォアグラウンドサービス通知（音楽プレイヤー、歩数計など）は除外する
                // ただし、アプリのバッジ（未読件数）は常駐通知（isOngoing）として
                // 実装されることが多いため、isOngoingでは除外しない
                val isForeground = (sbn.notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE) != 0

                !isForeground
            }.map { it.packageName }.toSet()

            _activeNotifications.value = packages
        } catch (e: Exception) {
            // セキュリティ例外などで取得できない場合は無視する
            e.printStackTrace()
        }
    }

    /**
     * アクティブなメディアセッション一覧から、表示・操作対象を選び直す。
     * 再生中のセッションを優先し、なければ一時停止中のセッションを使う。
     * 一度も再生されていない（NONE/STOPPEDの）セッションは候補にしない。
     */
    private fun updateActiveSessions(controllers: List<MediaController>?) {
        val list = controllers ?: emptyList()
        val active = list.firstOrNull { it.isActivelyEngaged() }
        val paused = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PAUSED }
        val candidate = active ?: paused

        if (candidate?.sessionToken != currentController?.sessionToken) {
            currentController?.unregisterCallback(mediaControllerCallback)
            currentController = candidate
            activeController = candidate
            candidate?.registerCallback(mediaControllerCallback)
        }
        refreshNowPlaying()
    }

    private fun refreshNowPlaying() {
        val controller = currentController
        if (controller == null) {
            _nowPlaying.value = null
            return
        }

        val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING

        if (controller.sessionToken == dismissedToken) {
            if (isPlaying) {
                dismissedToken = null // 再度再生が始まったので表示を復活させる
            } else {
                _nowPlaying.value = null
                return
            }
        }

        val metadata = controller.metadata
        // アプリによってはTITLE/ARTISTではなくDISPLAY_TITLE/DISPLAY_SUBTITLEにしか値を入れていないことがあるため、両方を見る
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)

        _nowPlaying.value = NowPlayingInfo(
            packageName = controller.packageName,
            title = title,
            artist = artist,
            isPlaying = isPlaying
        )
    }

    private fun MediaController.isActivelyEngaged(): Boolean {
        return when (playbackState?.state) {
            PlaybackState.STATE_PLAYING,
            PlaybackState.STATE_BUFFERING,
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING -> true
            else -> false
        }
    }
}
