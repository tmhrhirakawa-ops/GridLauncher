package com.example.gridlauncher.util

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

/**
 * 通知アクセス（[NotificationListenerService]へのバインド許可）が現在有効かどうかを判定する。
 * 通知バッジ・再生中メディアの取得や、QUICK ACCESSのミュート操作に必要。
 */
fun isNotificationListenerEnabled(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

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
     * @property position [lastPositionUpdateTime]時点での再生位置（ミリ秒）。
     * @property duration 再生コンテンツの総再生時間（ミリ秒）。取得できない場合は0以下。
     * @property playbackSpeed 再生速度（等倍なら1.0）。再生中の経過時間の補間に使う。
     * @property lastPositionUpdateTime [position]が計測された時刻（[android.os.SystemClock.elapsedRealtime]基準）。
     */
    data class NowPlayingInfo(
        val packageName: String,
        val title: String?,
        val artist: String?,
        val isPlaying: Boolean,
        val position: Long = 0L,
        val duration: Long = 0L,
        val playbackSpeed: Float = 1f,
        val lastPositionUpdateTime: Long = 0L
    )

    companion object {
        // パッケージ名 -> 通知件数（バッジ用件数）
        private val _activeNotifications = MutableStateFlow<Map<String, Int>>(emptyMap())
        val activeNotifications: StateFlow<Map<String, Int>> = _activeNotifications

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

        // 再生中のセッションがある間のポーリング間隔
        private const val ActivePollIntervalMs = 3000L

        // 再生中のセッションがない（停止中・一時停止中）状態でのポーリング間隔。バッテリー消費を抑えるため長めに空ける
        private const val IdlePollIntervalMs = 20000L

        // 接続中のサービス本体（ホーム画面の表示状態を伝えるため）。サービスのライフサイクルを
        // 延命しないよう弱参照で持つ
        private var instance: WeakReference<CyberNotificationListener>? = null

        // ホーム画面（再生中メディア・通知バッジを表示する画面）が見えているかどうか
        private var isUiVisible = false

        /**
         * ホーム画面が見えているかどうかを伝える。保険のポーリングは表示中だけ行い、
         * 再表示されたときはその場で最新の状態に取り直す。メインスレッドから呼ぶこと。
         */
        fun setUiVisible(visible: Boolean) {
            isUiVisible = visible
            instance?.get()?.onUiVisibilityChanged(visible)
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
    // 保険として定期的にセッション一覧を再取得する。
    // 表示先のホーム画面が見えている間だけ行い（見えていない間の変化は再表示時にまとめて取り直す）、
    // 再生中は短い間隔、それ以外は間隔を大きく空けてバッテリー消費を抑える
    // （検出自体はOnActiveSessionsChangedListenerがリアルタイムに拾うので、
    // このポーリングはあくまでOEM対策の保険）
    private val pollHandler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            pollActiveSessions()
            schedulePoll()
        }
    }

    private fun schedulePoll() {
        pollHandler.removeCallbacks(pollRunnable)
        if (!isUiVisible) return
        val isPlaying = currentController?.isActivelyEngaged() == true
        pollHandler.postDelayed(pollRunnable, if (isPlaying) ActivePollIntervalMs else IdlePollIntervalMs)
    }

    private fun onUiVisibilityChanged(visible: Boolean) {
        pollHandler.removeCallbacks(pollRunnable)
        if (visible) {
            pollActiveSessions()
            schedulePoll()
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
        instance = WeakReference(this)
        pollActiveSessions()
        schedulePoll()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
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

            val counts = notifications.filter { sbn ->
                // 通知マークとしてふさわしいものだけをフィルタリング
                // フォアグラウンドサービス通知（音楽プレイヤー、歩数計など）は除外する
                // ただし、アプリのバッジ（未読件数）は常駐通知（isOngoing）として
                // 実装されることが多いため、isOngoingでは除外しない
                val isForeground = (sbn.notification.flags and android.app.Notification.FLAG_FOREGROUND_SERVICE) != 0
                // グループ通知の「まとめ」通知は個別の通知ではないため件数に含めない
                val isGroupSummary = (sbn.notification.flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0

                !isForeground && !isGroupSummary
            }.groupingBy { it.packageName }.eachCount()

            _activeNotifications.value = counts
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

        val playbackState = controller.playbackState
        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING

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

        // 再生中のメディアがなくなったときにNOW PLAYINGウィジェットへ表示できるよう、最後に再生したものとして残す
        if (title != null) saveLastPlayedMedia(this, LastPlayedMedia(controller.packageName, title, artist))

        _nowPlaying.value = NowPlayingInfo(
            packageName = controller.packageName,
            title = title,
            artist = artist,
            isPlaying = isPlaying,
            position = playbackState?.position ?: 0L,
            duration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L,
            playbackSpeed = playbackState?.playbackSpeed ?: 1f,
            lastPositionUpdateTime = playbackState?.lastPositionUpdateTime ?: 0L
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
