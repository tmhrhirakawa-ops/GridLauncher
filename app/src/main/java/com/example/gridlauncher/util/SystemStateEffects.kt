package com.example.gridlauncher.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleStartEffect

/**
 * 現在時刻（ミリ秒）を返し、分が変わるたび・端末の時刻/タイムゾーンが変更されたときに更新する。
 *
 * 表示は分単位（HH:mm）なので、1秒ごとにポーリングする代わりにシステムが毎分配信する
 * [Intent.ACTION_TIME_TICK]で更新する。受信はホーム画面が見えている間（ライフサイクルが
 * STARTED以上）だけに限定し、他のアプリを使っている間に無駄にCPUを起こさないようにする。
 * 再表示時にはその時点の時刻へすぐ更新する。
 */
@Composable
fun rememberCurrentTimeMillis(): Long {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LifecycleStartEffect(context) {
        now = System.currentTimeMillis()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                now = System.currentTimeMillis()
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
    return now
}

/**
 * バッテリー残量（%）を返し、残量が変わったときに更新する。
 *
 * 1秒ごとに[BatteryManager]へ問い合わせる代わりに、システムの[Intent.ACTION_BATTERY_CHANGED]
 * （sticky broadcastなので登録直後に現在値も取得できる）で更新する。受信はホーム画面が
 * 見えている間だけに限定する。
 */
@Composable
fun rememberBatteryLevel(): Int {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(100) }
    LifecycleStartEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context, intent: Intent) {
                intent.batteryPercent()?.let { level = it }
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED
        )
        sticky?.batteryPercent()?.let { level = it }
        onStopOrDispose { context.unregisterReceiver(receiver) }
    }
    return level
}

/** [Intent.ACTION_BATTERY_CHANGED]のIntentから残量（%）を求める。取得できなければnull。 */
private fun Intent.batteryPercent(): Int? {
    val rawLevel = getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (rawLevel >= 0 && scale > 0) rawLevel * 100 / scale else null
}
