package com.example.gridlauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.edit
import com.example.gridlauncher.util.KEY_GRID_APPS
import com.example.gridlauncher.util.KEY_GRID_APPS_LANDSCAPE

/**
 * [android.content.pm.PackageInstaller.uninstall] が要求する
 * ステータス通知（IntentSender）の受け口。
 *
 * アンインストール確認画面の表示要求（[PackageInstaller.STATUS_PENDING_USER_ACTION]）を
 * 中継するほか、実際にアンインストールが完了した（[PackageInstaller.STATUS_SUCCESS]）場合のみ
 * ホーム画面のスロットからそのアプリを取り除く。ユーザーが確認画面でキャンセルした場合は
 * スロットのアプリをそのまま残す。
 */
class UninstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                confirmIntent?.let {
                    it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(it)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
                val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
                removeFromSlots(context, packageName)
            }
            // それ以外（キャンセル・失敗）は何もしない。スロットのアプリはそのまま残す。
        }
    }

    private fun removeFromSlots(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences("cyber_launcher", Context.MODE_PRIVATE)
        prefs.edit {
            // 横画面用のAPP LIST（縦画面と横画面で別々に並べている場合のみ存在）も対象にする
            for (key in listOf(KEY_GRID_APPS, KEY_GRID_APPS_LANDSCAPE, "dock_apps")) {
                val packages = prefs.getString(key, "")?.split(",") ?: continue
                if (packageName !in packages) continue
                val cleared = packages.map { if (it == packageName) "" else it }
                putString(key, cleared.joinToString(","))
            }
        }
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "package_name"
    }
}
