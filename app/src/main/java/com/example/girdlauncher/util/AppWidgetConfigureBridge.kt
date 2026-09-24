package com.example.girdlauncher.util

/**
 * ウィジェットの設定画面（configure activity）の起動結果を、旧来の
 * `Activity.onActivityResult`経路からCompose側へ橋渡しするための一時的な保持場所。
 *
 * [android.appwidget.AppWidgetHost.startAppWidgetConfigureActivityForResult]は、対象アプリの
 * configure activityが`exported="false"`（多くのOEM製ウィジェットがそう）であっても、
 * システム（AppWidgetService）が発行した[android.content.IntentSender]経由で起動するため
 * 正しく動作する（`Intent(ACTION_APPWIDGET_CONFIGURE)`を自前で組み立てて直接
 * `startActivity`するとSecurityExceptionで落ちる機種がある）。ただしこのAPIは
 * `Activity.startIntentSenderForResult`を使う昔ながらの`requestCode`＋`onActivityResult`
 * 方式のため、[com.example.girdlauncher.MainActivity]側でoverrideした
 * `onActivityResult`からここへ結果を渡す。
 */
object AppWidgetConfigureResultBridge {
    const val REQUEST_CODE = 4210

    /** 起動時に登録し、結果が返ってきたら一度だけ呼ばれる（呼び出し後は自動でnullに戻す）。 */
    var onResult: ((resultCode: Int) -> Unit)? = null
}
