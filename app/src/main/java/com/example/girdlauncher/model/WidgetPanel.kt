package com.example.girdlauncher.model

/**
 * 枠線の表示/非表示を個別に切り替えられるウィジェットパネルの種類。
 *
 * @property label 設定ダイアログに表示するラベル文字列。
 */
enum class WidgetPanel(val label: String) {
    ACCESS_GRID("ACCESS GRID"),
    CALENDAR("CALENDAR"),
    DEVICE_STATUS("SYSTEM MONITOR"),
    QUICK_ACCESS("QUICK ACCESS"),

    /**
     * 他アプリが提供する本物のAndroid AppWidget（[android.appwidget.AppWidgetHost]で
     * ホストするRemoteViewsベースのウィジェット）。このパネルだけは他の4種と異なり、
     * 同時に複数配置できる（[com.example.girdlauncher.model.PlacedWidget.appWidgetId]で
     * インスタンスを区別する）。
     */
    APPWIDGET("APP WIDGET")
}
