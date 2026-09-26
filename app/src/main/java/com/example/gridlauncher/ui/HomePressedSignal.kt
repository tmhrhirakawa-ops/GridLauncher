package com.example.gridlauncher.ui

import androidx.compose.runtime.compositionLocalOf

/**
 * ホームボタンが押された回数。ランチャーを表示中（または他のアプリから戻るとき）にホームボタンが
 * 押されるたびに1ずつ増える。各画面はこの値の変化を合図に、開いているポップアップ・ボトムシート・
 * 編集モードなどを閉じてホーム画面の状態に戻る（0は「まだ押されていない」）。
 */
val LocalHomePressedSignal = compositionLocalOf { 0 }
