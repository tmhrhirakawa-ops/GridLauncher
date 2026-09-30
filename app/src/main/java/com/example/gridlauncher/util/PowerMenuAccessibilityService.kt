package com.example.gridlauncher.util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent

/**
 * カスタマイズ画面の電源メニューボタンから、端末標準の電源メニュー（電源を切る/再起動など）を
 * 開くためだけに使う最小限のアクセシビリティサービス。
 *
 * 電源メニューを直接開く公開APIは存在しないため、[AccessibilityService.performGlobalAction]の
 * [AccessibilityService.GLOBAL_ACTION_POWER_DIALOG]を使う。これにはユーザーが設定画面で
 * このサービスを手動で有効にする必要がある（通知アクセスと同様の「特別なアクセス」権限）。
 */
class PowerMenuAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: PowerMenuAccessibilityService? = null

        /**
         * 電源メニューを開く。サービスが有効化されていない場合はfalseを返す。
         */
        fun showPowerMenu(): Boolean = performAction(GLOBAL_ACTION_POWER_DIALOG)

        /** サービスが実際に動いている（アプリとつながっている）かどうか。 */
        fun isConnected(): Boolean = instance != null

        /**
         * 端末全体の操作（[AccessibilityService]の`GLOBAL_ACTION_*`。通知パネル・画面オフなど）を行う。
         * サービスが有効化されていない場合はfalseを返す。
         */
        fun performAction(globalAction: Int): Boolean {
            val service = instance ?: return false
            return service.performGlobalAction(globalAction)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 電源メニューを開く用途のみのため、イベントは使用しない
    }

    override fun onInterrupt() {
        // 使用しない
    }
}

/**
 * [PowerMenuAccessibilityService]が現在有効になっているかどうかを判定する。
 */
fun isPowerMenuAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponent = "${context.packageName}/${PowerMenuAccessibilityService::class.java.name}"
    val enabledServices = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabledServices) }
    while (splitter.hasNext()) {
        if (splitter.next().equals(expectedComponent, ignoreCase = true)) return true
    }
    return false
}

/** [PowerMenuAccessibilityService]の状態。 */
enum class AccessibilityServiceStatus {
    /** 設定でオンになっていて、実際に動いている。 */
    ENABLED,

    /**
     * 設定ではオンなのに、動いていない（アプリの更新直後などに起きることがある。
     * 設定で一度オフにしてからオンにし直すと直る）。
     */
    NOT_CONNECTED,

    /** 設定でオフになっている。 */
    DISABLED
}

/** [PowerMenuAccessibilityService]が今どの状態かを判定する。 */
fun accessibilityServiceStatus(context: Context): AccessibilityServiceStatus = when {
    !isPowerMenuAccessibilityServiceEnabled(context) -> AccessibilityServiceStatus.DISABLED
    !PowerMenuAccessibilityService.isConnected() -> AccessibilityServiceStatus.NOT_CONNECTED
    else -> AccessibilityServiceStatus.ENABLED
}

/**
 * 電源メニューを開く。サービスが有効化されていない（または接続前）場合は
 * [onNeedPermission]を呼び出して権限付与画面への案内に委ねる。
 */
fun openPowerMenuOrRequestPermission(context: Context, onNeedPermission: () -> Unit) {
    if (!isPowerMenuAccessibilityServiceEnabled(context) || !PowerMenuAccessibilityService.showPowerMenu()) {
        onNeedPermission()
    }
}

/**
 * 端末全体の操作（[AccessibilityService]の`GLOBAL_ACTION_*`）を行う。サービスが有効化されていない
 * （または接続前）場合は[onNeedPermission]を呼び出して権限付与画面への案内に委ねる。
 */
fun performSystemActionOrRequestPermission(context: Context, globalAction: Int, onNeedPermission: () -> Unit) {
    if (!isPowerMenuAccessibilityServiceEnabled(context) || !PowerMenuAccessibilityService.performAction(globalAction)) {
        onNeedPermission()
    }
}
