package com.example.gridlauncher.util

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import androidx.core.content.edit
import com.example.gridlauncher.model.AppInfo
import java.text.Collator
import java.util.Locale

/**
 * ALL APPS・SELECT APPのアプリ一覧の並べ替えの種類。
 *
 * @property label 表示する名前。
 * @property ascendingLabel 昇順の説明（例: 古い順）。
 * @property descendingLabel 降順の説明（例: 新しい順）。
 */
enum class AppSortKey(val label: String, val ascendingLabel: String, val descendingLabel: String) {
    INSTALL("インストール順", "古い順", "新しい順"),
    NAME("名前順", "A→Z・あ→ん", "Z→A・ん→あ"),
    CATEGORY("カテゴリ順", "カテゴリ名の順", "カテゴリ名の逆順")
}

/**
 * アプリ一覧の並べ替え。
 *
 * @property key 並べ替えの種類。
 * @property descending 降順かどうか。
 */
data class AppSortOrder(val key: AppSortKey, val descending: Boolean) {
    companion object {
        /** 初期値（インストールした順・古い順）。 */
        val Default = AppSortOrder(AppSortKey.INSTALL, descending = false)
    }
}

/**
 * 並べ替えたアプリ一覧のまとまり。カテゴリ順のときはカテゴリごとに分け、[title]に見出しを入れる。
 *
 * @property title 見出し（カテゴリ順以外はnull＝見出しなし）。
 * @property apps このまとまりのアプリ。
 */
data class AppListSection(val title: String?, val apps: List<AppInfo>)

private const val KEY_SORT_KEY = "app_sort_key"
private const val KEY_SORT_DESCENDING = "app_sort_descending"

/** 保存したアプリ一覧の並べ替えを読み込む（未設定ならインストールした順・古い順）。 */
fun loadAppSortOrder(prefs: SharedPreferences): AppSortOrder = AppSortOrder(
    key = prefs.getString(KEY_SORT_KEY, null)?.let { name -> AppSortKey.entries.firstOrNull { it.name == name } }
        ?: AppSortOrder.Default.key,
    descending = prefs.getBoolean(KEY_SORT_DESCENDING, AppSortOrder.Default.descending)
)

/** アプリ一覧の並べ替えを保存する（ALL APPS・SELECT APPで共通）。 */
fun saveAppSortOrder(prefs: SharedPreferences, order: AppSortOrder) {
    prefs.edit {
        putString(KEY_SORT_KEY, order.key.name)
        putBoolean(KEY_SORT_DESCENDING, order.descending)
    }
}

/**
 * [apps]を[order]で並べ替え、まとまりに分ける。
 *
 * - インストール順・名前順: 見出しなしの1つのまとまり。名前は端末の言語の並び（日本語なら五十音順）で比べる。
 * - カテゴリ順: アプリが申告しているカテゴリ（ゲーム・音楽など）ごとに分け、カテゴリ名の順（降順なら逆順）に
 *   並べる。カテゴリを申告していないアプリは、並び順にかかわらず最後の「その他」にまとめる。
 *   各カテゴリの中は名前順。
 */
fun sortAppsForList(context: Context, apps: List<AppInfo>, order: AppSortOrder): List<AppListSection> {
    val collator = Collator.getInstance(Locale.getDefault())
    val byName = compareBy<AppInfo, String>(collator) { it.label }
    return when (order.key) {
        AppSortKey.INSTALL -> {
            val sorted = apps.sortedByInstallOrder()
            listOf(AppListSection(null, if (order.descending) sorted.reversed() else sorted))
        }
        AppSortKey.NAME -> {
            val sorted = apps.sortedWith(byName)
            listOf(AppListSection(null, if (order.descending) sorted.reversed() else sorted))
        }
        AppSortKey.CATEGORY -> {
            val (categorized, uncategorized) = apps.partition { it.category != ApplicationInfo.CATEGORY_UNDEFINED }
            val sections = categorized
                .groupBy { it.category }
                .map { (category, members) ->
                    val title = ApplicationInfo.getCategoryTitle(context, category)?.toString() ?: "その他"
                    AppListSection(title, members.sortedWith(byName))
                }
                .sortedWith(compareBy(collator) { it.title.orEmpty() })
            val ordered = if (order.descending) sections.reversed() else sections
            if (uncategorized.isEmpty()) ordered else ordered + AppListSection("その他", uncategorized.sortedWith(byName))
        }
    }
}
