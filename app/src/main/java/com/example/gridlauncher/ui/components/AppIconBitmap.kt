package com.example.gridlauncher.ui.components

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.ActiveIconPack
import com.example.gridlauncher.util.IconPackManager
import com.example.gridlauncher.util.loadOriginalIconBitmap
import com.example.gridlauncher.util.toOriginalIconBitmap
import com.example.gridlauncher.util.toDuotoneImageBitmap

/** 加工済みアイコンのキャッシュの上限（バイト）。160px四方のARGBアイコンで約120個分。 */
private const val ICON_CACHE_MAX_BYTES = 12 * 1024 * 1024

/**
 * 加工済みアプリアイコンのキャッシュ。同じアプリがグリッド・ドック・アプリドロワーなど複数の
 * 場所に表示されても、ピクセル単位のデュオトーン加工やPackageManagerからの読み込みを
 * 一度で済ませ、アプリドロワーのスクロールで同じアイコンを何度も加工し直さないようにする。
 */
private val appIconCache = object : LruCache<String, ImageBitmap>(ICON_CACHE_MAX_BYTES) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}

/**
 * アプリカードに表示するアイコンを返す。現在のアクセントカラーでデュオトーン加工したもの
 * （[useOriginalIconColors]がtrueのときは加工していない本来の色のもの）をキャッシュから返し、
 * なければ作ってキャッシュする。
 *
 * キーには[icon]のインスタンスも含めるため、アプリが更新されてアイコンが差し替わった場合は
 * 自動的に作り直される。
 *
 * アイコンパック（[IconPackManager.active]）を使っている場合は、パックにそのアプリのアイコンがあれば
 * それを表示する（パックの色のまま、またはアクセントカラーで加工して）。パックにないアプリは、
 * これまでどおりのアイコンにする。
 */
@Composable
fun rememberAppIconBitmap(
    packageName: String,
    icon: Drawable,
    isMonochrome: Boolean,
    useOriginalIconColors: Boolean
): ImageBitmap {
    val accent = LocalCyberColors.current.accent
    val context = LocalContext.current
    val iconPack = IconPackManager.active
    return remember(packageName, icon, isMonochrome, accent, useOriginalIconColors, iconPack) {
        iconPack?.let { iconPackBitmap(context, it, packageName, accent, useOriginalIconColors) }
            ?: standardIconBitmap(context, packageName, icon, isMonochrome, accent, useOriginalIconColors)
    }
}

// アイコンパックにアイコンがないと分かったアプリ（「パッケージ名|アプリのパッケージ名」）。
// 表示のたびにパックを探し直さないようにする
private val missingIconPackIcons = HashSet<String>()

/** アイコンパックの、[packageName]のアプリのアイコン。パックにない場合はnull。 */
private fun iconPackBitmap(
    context: Context,
    iconPack: ActiveIconPack,
    packageName: String,
    accent: Color,
    useOriginalIconColors: Boolean
): ImageBitmap? {
    val packPackage = iconPack.pack.packageName
    if ("$packPackage|$packageName" in missingIconPackIcons) return null
    val keepColors = iconPack.usePackColors || useOriginalIconColors
    val key = "pack|$packPackage|$packageName|${if (keepColors) "original" else accent.toArgb()}"
    appIconCache.get(key)?.let { return it }
    val drawable = iconPack.pack.loadIcon(context, packageName) ?: run {
        missingIconPackIcons.add("$packPackage|$packageName")
        return null
    }
    val bitmap = if (keepColors) toOriginalIconBitmap(drawable) else toDuotoneImageBitmap(drawable, isMonochrome = false, accent = accent)
    appIconCache.put(key, bitmap)
    return bitmap
}

/** アイコンパックを使わない場合の、これまでどおりのアイコン（デュオトーン加工、または本来の色）。 */
private fun standardIconBitmap(
    context: Context,
    packageName: String,
    icon: Drawable,
    isMonochrome: Boolean,
    accent: Color,
    useOriginalIconColors: Boolean
): ImageBitmap {
    val iconId = System.identityHashCode(icon)
    val duotoneKey = "duotone|$packageName|$iconId|$isMonochrome|${accent.toArgb()}"
    val key = if (useOriginalIconColors) "original|$packageName|$iconId" else duotoneKey
    return appIconCache.get(key) ?: run {
        val bitmap = if (useOriginalIconColors) {
            loadOriginalIconBitmap(context, packageName)
        } else {
            null
        } ?: appIconCache.get(duotoneKey) ?: toDuotoneImageBitmap(icon, isMonochrome, accent)
        appIconCache.put(key, bitmap)
        bitmap
    }
}
