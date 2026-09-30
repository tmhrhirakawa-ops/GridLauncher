package com.example.gridlauncher.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.util.Xml
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import org.xmlpull.v1.XmlPullParser

/**
 * インストール済みのアイコンパック。
 *
 * @property packageName アイコンパックのパッケージ名。
 * @property label アイコンパックの名前。
 */
data class IconPackInfo(val packageName: String, val label: String)

/**
 * 読み込んだアイコンパック。アプリ（起動するアクティビティ）ごとの、パックのアイコンの画像名を持つ。
 *
 * @property packageName アイコンパックのパッケージ名。
 */
class LoadedIconPack internal constructor(
    val packageName: String,
    private val resources: Resources,
    private val drawableByComponent: Map<String, String>,
    private val drawableByPackage: Map<String, String>
) {
    // 画像名からリソースIDへの変換結果（見つからなかったものは0）
    private val resourceIdCache = HashMap<String, Int>()

    /**
     * [appPackageName]のアプリの、アイコンパックのアイコンを返す。起動するアクティビティが
     * 分かればそれに合うものを優先し、なければ同じアプリの別のアクティビティ用のものを使う。
     * パックにそのアプリのアイコンがなければnull。
     */
    fun loadIcon(context: Context, appPackageName: String): Drawable? {
        val component = context.packageManager.getLaunchIntentForPackage(appPackageName)?.component
        val drawableName = component?.let { drawableByComponent["${it.packageName}/${it.className}"] }
            ?: drawableByPackage[appPackageName]
            ?: return null
        val resourceId = resourceIdCache.getOrPut(drawableName) {
            resources.getIdentifier(drawableName, "drawable", packageName).takeIf { it != 0 }
                ?: resources.getIdentifier(drawableName, "mipmap", packageName)
        }
        if (resourceId == 0) return null
        return try {
            @Suppress("DEPRECATION")
            resources.getDrawableForDensity(resourceId, DisplayMetrics.DENSITY_XXHIGH, null)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }
}

/**
 * 今使っているアイコンパック。
 *
 * @property pack 読み込んだアイコンパック。
 * @property usePackColors パックのアイコンを本来の色のまま表示するかどうか（falseならアクセントカラーで加工する）。
 */
data class ActiveIconPack(val pack: LoadedIconPack, val usePackColors: Boolean)

/**
 * アイコンパック（Nova Launcher・ADW・Apex・Go Launcher などと同じ、`appfilter.xml` 形式のもの）の
 * 検出・読み込みと、今使っているアイコンパックの管理。
 */
object IconPackManager {
    /** アイコンパックが対応ランチャーに自分を知らせるためのアクション（どれかに対応していればアイコンパックとみなす）。 */
    private val IconPackActions = listOf(
        "org.adw.launcher.THEMES",
        "com.novalauncher.THEME",
        "com.teslacoilsw.launcher.THEME",
        "com.anddoes.launcher.THEME",
        "com.gau.go.launcherex.theme",
        "com.fede.launcher.THEME_ICONPACK"
    )

    private const val KEY_ICON_PACK = "icon_pack_package"
    private const val KEY_USE_PACK_COLORS = "icon_pack_use_pack_colors"

    /**
     * 今使っているアイコンパック（使っていなければnull）。スナップショットの状態として持つので、
     * これを読んでいるアイコンの表示は、切り替えると自動で描き直される。
     */
    var active: ActiveIconPack? by mutableStateOf(null)
        private set

    /** 今使うアイコンパックを設定する（nullで使わない）。 */
    fun applyIconPack(iconPack: ActiveIconPack?) {
        active = iconPack
    }

    /** インストールされているアイコンパックの一覧（名前順）。 */
    fun installedIconPacks(context: Context): List<IconPackInfo> {
        val packageManager = context.packageManager
        return IconPackActions
            .flatMap { action -> packageManager.queryIntentActivities(Intent(action), PackageManager.GET_META_DATA) }
            .map { it.activityInfo.packageName }
            .distinct()
            .mapNotNull { packageName ->
                try {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    IconPackInfo(packageName, packageManager.getApplicationLabel(appInfo).toString())
                } catch (_: PackageManager.NameNotFoundException) {
                    null
                }
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * アイコンパックの`appfilter.xml`（`res/xml`、なければ`assets`）を読み込む。
     * 数千件あるパックもあるため、メインスレッド以外で呼ぶこと。読み込めなければnull。
     */
    fun load(context: Context, packageName: String): LoadedIconPack? = try {
        val resources = context.packageManager.getResourcesForApplication(packageName)
        val drawableByComponent = HashMap<String, String>()
        val drawableByPackage = HashMap<String, String>()

        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        val parser: XmlPullParser = if (xmlId != 0) {
            resources.getXml(xmlId)
        } else {
            Xml.newPullParser().apply { setInput(resources.assets.open("appfilter.xml"), "UTF-8") }
        }
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                parseComponent(component)?.let { (appPackage, activityClass) ->
                    if (!drawable.isNullOrEmpty()) {
                        drawableByComponent.putIfAbsent("$appPackage/$activityClass", drawable)
                        drawableByPackage.putIfAbsent(appPackage, drawable)
                    }
                }
            }
            eventType = parser.next()
        }
        LoadedIconPack(packageName, resources, drawableByComponent, drawableByPackage)
    } catch (_: Exception) {
        // パックが削除された・形式が壊れているなどの場合は、アイコンパックを使わない
        null
    }

    /**
     * `ComponentInfo{パッケージ名/アクティビティ名}` を分解する。アクティビティ名が「.」で始まる
     * 省略形の場合は、パッケージ名を補う。
     */
    private fun parseComponent(component: String?): Pair<String, String>? {
        val inner = component?.removePrefix("ComponentInfo{")?.removeSuffix("}") ?: return null
        val appPackage = inner.substringBefore("/", "").takeIf { it.isNotEmpty() } ?: return null
        val activity = inner.substringAfter("/", "").takeIf { it.isNotEmpty() } ?: return null
        return appPackage to (if (activity.startsWith(".")) appPackage + activity else activity)
    }

    /** 保存したアイコンパックのパッケージ名を読み込む（使っていなければnull）。 */
    fun loadSelectedPackage(prefs: SharedPreferences): String? = prefs.getString(KEY_ICON_PACK, null)

    /** 使うアイコンパックを保存する（nullで使わない）。 */
    fun saveSelectedPackage(prefs: SharedPreferences, packageName: String?) {
        prefs.edit { if (packageName == null) remove(KEY_ICON_PACK) else putString(KEY_ICON_PACK, packageName) }
    }

    /** パックのアイコンを本来の色のまま表示するかどうかを読み込む（未設定ならそのまま表示する）。 */
    fun loadUsePackColors(prefs: SharedPreferences): Boolean = prefs.getBoolean(KEY_USE_PACK_COLORS, true)

    /** パックのアイコンを本来の色のまま表示するかどうかを保存する。 */
    fun saveUsePackColors(prefs: SharedPreferences, usePackColors: Boolean) {
        prefs.edit { putBoolean(KEY_USE_PACK_COLORS, usePackColors) }
    }
}
