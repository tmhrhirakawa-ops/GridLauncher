package com.example.girdlauncher.util

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * デバイスの通知状態を監視するサービス。
 * 通知が来ているアプリのパッケージ名を状態として保持します。
 */
class CyberNotificationListener : NotificationListenerService() {

    companion object {
        private val _activeNotifications = MutableStateFlow<Set<String>>(emptySet())
        val activeNotifications: StateFlow<Set<String>> = _activeNotifications
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotifications()
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
}
