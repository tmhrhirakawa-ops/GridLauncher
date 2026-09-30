package com.example.gridlauncher.util

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import com.example.gridlauncher.model.WidgetPanel
import org.json.JSONArray
import org.json.JSONObject

/**
 * ランチャーの設定（ウィジェットの配置・スタック・APP LIST・DOCK・フォルダ・配色・テーマなど）の
 * バックアップ・復元。すべての設定は SharedPreferences の "cyber_launcher" にあるため、それを丸ごと
 * JSONファイルに書き出し、読み込むときはそれで置き換える。
 *
 * - PROの購入状態（[ExcludedKeys]）は含めない（別の端末で読み込んだだけでPROにならないように）。
 * - 外部ウィジェット（他アプリのAppWidget）は、ウィジェットごとのIDが端末・インストールごとに
 *   異なるため復元できない。読み込み先に存在しないものは配置から外す。
 */
object LauncherBackup {
    private const val PREFS_NAME = "cyber_launcher"
    private const val FORMAT = "gridlauncher-backup"
    private const val VERSION = 1

    /** バックアップに含めない設定（PROの購入状態・開発用の切り替え）。 */
    private val ExcludedKeys = setOf("pro_unlocked_cached", "pro_debug_override")

    /**
     * 復元の結果。
     *
     * @property removedAppWidgetCount 読み込み先に存在せず、配置から外した外部ウィジェットの数。
     */
    data class RestoreResult(val removedAppWidgetCount: Int)

    /** バックアップのファイル名の候補（例: gridlauncher-backup-20260930.json）。 */
    fun suggestedFileName(): String {
        val date = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(java.util.Date())
        return "gridlauncher-backup-$date.json"
    }

    /** 今の設定を[uri]のファイルに書き出す。 */
    fun export(context: Context, uri: Uri): Result<Unit> = runCatching {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entries = JSONObject()
        prefs.all.forEach { (key, value) ->
            if (key in ExcludedKeys || value == null) return@forEach
            val entry = JSONObject()
            when (value) {
                is String -> entry.put("t", "s").put("v", value)
                is Boolean -> entry.put("t", "b").put("v", value)
                is Int -> entry.put("t", "i").put("v", value)
                is Long -> entry.put("t", "l").put("v", value)
                is Float -> entry.put("t", "f").put("v", value.toDouble())
                is Set<*> -> entry.put("t", "ss").put("v", JSONArray(value.filterIsInstance<String>()))
                else -> return@forEach
            }
            entries.put(key, entry)
        }
        val root = JSONObject()
            .put("format", FORMAT)
            .put("version", VERSION)
            .put("exportedAt", System.currentTimeMillis())
            .put("prefs", entries)
        val output = context.contentResolver.openOutputStream(uri, "wt") ?: error("ファイルを開けませんでした")
        output.bufferedWriter().use { it.write(root.toString(2)) }
    }

    /**
     * [uri]のファイルから設定を読み込み、今の設定と置き換える。ファイルがバックアップの形式でない
     * 場合は、今の設定を変えずに失敗を返す。読み込んだあとは、画面を作り直して反映すること。
     */
    fun restore(context: Context, uri: Uri): Result<RestoreResult> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: error("ファイルを開けませんでした")
        val root = JSONObject(text)
        require(root.optString("format") == FORMAT) { "GridLauncher のバックアップファイルではありません" }
        require(root.optInt("version") <= VERSION) { "新しいバージョンのアプリで作られたバックアップです" }
        val entries = root.getJSONObject("prefs")

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 置き換える前に、今の配置にある外部ウィジェットのIDを控えておく（復元後に使われなくなったものを解放する）
        val previousAppWidgetIds = placedAppWidgetIds(prefs)

        prefs.edit(commit = true) {
            prefs.all.keys.filter { it !in ExcludedKeys }.forEach { remove(it) }
            entries.keys().forEach { key ->
                if (key in ExcludedKeys) return@forEach
                val entry = entries.getJSONObject(key)
                when (entry.getString("t")) {
                    "s" -> putString(key, entry.getString("v"))
                    "b" -> putBoolean(key, entry.getBoolean("v"))
                    "i" -> putInt(key, entry.getInt("v"))
                    "l" -> putLong(key, entry.getLong("v"))
                    "f" -> putFloat(key, entry.getDouble("v").toFloat())
                    "ss" -> {
                        val array = entry.getJSONArray("v")
                        putStringSet(key, (0 until array.length()).map { array.getString(it) }.toSet())
                    }
                }
            }
        }

        // この端末に存在しない外部ウィジェットを配置から外す（外すとメンバーが1つになったスタックは解除される）
        val appWidgetManager = AppWidgetManager.getInstance(context)
        var removedCount = 0
        WidgetLayoutMode.entries.filter { hasSavedWidgetLayout(prefs, it) }.forEach { mode ->
            val widgets = loadPlacedWidgets(prefs, mode)
            val kept = widgets.filter { widget ->
                widget.type != WidgetPanel.APPWIDGET || appWidgetManager.getAppWidgetInfo(widget.appWidgetId) != null
            }
            if (kept.size != widgets.size) {
                removedCount += widgets.size - kept.size
                savePlacedWidgets(prefs, mode, kept.normalizeStacks())
            }
        }

        // 置き換えで使われなくなった外部ウィジェットは、ホストから解放する
        val restoredAppWidgetIds = placedAppWidgetIds(prefs)
        AppWidgetHostManager.ensureInitialized(context)
        (previousAppWidgetIds - restoredAppWidgetIds).forEach { AppWidgetHostManager.host.deleteAppWidgetId(it) }

        RestoreResult(removedAppWidgetCount = removedCount)
    }

    private fun placedAppWidgetIds(prefs: SharedPreferences): Set<Int> =
        WidgetLayoutMode.entries
            .filter { hasSavedWidgetLayout(prefs, it) }
            .flatMap { loadPlacedWidgets(prefs, it) }
            .filter { it.type == WidgetPanel.APPWIDGET }
            .map { it.appWidgetId }
            .toSet()
}
