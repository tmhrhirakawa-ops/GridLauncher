package com.example.gridlauncher.model

/**
 * QUICK ACCESSセクションに配置できるボタンの種類。
 *
 * @property label ボタンに表示するラベル文字列。
 * @property description ボタン追加時の選択画面に表示する説明文。
 */
enum class QuickActionId(val label: String, val description: String) {
    SETTINGS("SETTING", "端末の設定画面を開く"),
    WIFI("WI-FI", "Wi-Fi設定画面を開く"),
    DISPLAY("DISPLAY", "画面設定画面を開く"),
    BLUETOOTH("BLUETOOTH", "Bluetooth設定画面を開く"),
    DEVELOP("DEVELOP", "開発者向けオプションを開く"),
    COLOR("COLOR", "アクセントカラーを変更する"),
    THEME("THEME", "ライト/ダークテーマを切り替える"),
    VOLUME("VOLUME", "音量を調整する"),
    BRIGHTNESS("BRIGHT", "画面の明るさを調整する"),
    APP("APP", "アプリの設定画面を開く"),
    WALLPAPER("WALL", "壁紙の設定画面を開く")
}
