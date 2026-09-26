package com.example.gridlauncher.model

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

    /** ヘッダーと同じ時刻・日付の表示（ヘッダーを非表示にしたときなどに使う）。 */
    CLOCK("CLOCK"),

    /** ヘッダーと同じバッテリー残量の表示（中央の歯車でカスタマイズ画面を開く）。 */
    BATTERY("BATTERY"),

    /**
     * APP LISTの1マス分（アプリショートカット1個）だけを独立したウィジェットとして配置したもの。
     * アイコンのみ・正方形の表示スタイル。[APPWIDGET]と同様に複数配置できる
     * （[com.example.gridlauncher.model.PlacedWidget.instanceId]でインスタンスを区別する）。
     */
    APP_SLOT_ICON_ONLY("APP SLOT (ICON ONLY)"),

    /** [APP_SLOT_ICON_ONLY]と同じだが、アイコンに加えてアプリ名も表示するスタイル。 */
    APP_SLOT_NAMED("APP SLOT"),

    /**
     * 他アプリが提供する本物のAndroid AppWidget（[android.appwidget.AppWidgetHost]で
     * ホストするRemoteViewsベースのウィジェット）。このパネルだけは他の4種と異なり、
     * 同時に複数配置できる（[com.example.gridlauncher.model.PlacedWidget.appWidgetId]で
     * インスタンスを区別する）。
     */
    APPWIDGET("APP WIDGET")
}
