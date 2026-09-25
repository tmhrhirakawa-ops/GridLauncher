package com.example.gridlauncher.ui.components

import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.example.gridlauncher.ui.theme.LocalCyberColors
import com.example.gridlauncher.util.loadOriginalIconBitmap
import com.example.gridlauncher.util.toDuotoneImageBitmap

/** 加工済みアイコンのキャッシュの上限（バイト）。128px四方のARGBアイコンで約120個分。 */
private const val ICON_CACHE_MAX_BYTES = 8 * 1024 * 1024

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
    return remember(packageName, icon, isMonochrome, accent, useOriginalIconColors) {
        val iconId = System.identityHashCode(icon)
        val duotoneKey = "duotone|$packageName|$iconId|$isMonochrome|${accent.toArgb()}"
        val key = if (useOriginalIconColors) "original|$packageName|$iconId" else duotoneKey
        appIconCache.get(key) ?: run {
            val bitmap = if (useOriginalIconColors) {
                loadOriginalIconBitmap(context, packageName)
            } else {
                null
            } ?: appIconCache.get(duotoneKey) ?: toDuotoneImageBitmap(icon, isMonochrome, accent)
            appIconCache.put(key, bitmap)
            bitmap
        }
    }
}
