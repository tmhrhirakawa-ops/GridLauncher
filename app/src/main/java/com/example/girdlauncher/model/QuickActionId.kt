package com.example.girdlauncher.model

/**
 * QUICK ACCESSセクションに配置できるボタンの種類。
 *
 * @property label ボタンに表示するラベル文字列。
 */
enum class QuickActionId(val label: String) {
    WIFI("WI-FI"),
    DISPLAY("DISPLAY"),
    BLUETOOTH("BLUETOOTH"),
    DEVELOP("DEVELOP"),
    COLOR("COLOR"),
    THEME("THEME"),
    VOLUME("VOLUME"),
    BRIGHTNESS("BRIGHT")
}
