package com.example.gridlauncher.ui.screens

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit
import com.example.gridlauncher.ui.components.DefaultAccentColor2
import com.example.gridlauncher.ui.theme.CyberColors
import com.example.gridlauncher.ui.theme.CyberFontOption
import com.example.gridlauncher.ui.theme.DarkAccentColor
import com.example.gridlauncher.ui.theme.DarkBgColor
import com.example.gridlauncher.ui.theme.DarkBorderColor
import com.example.gridlauncher.ui.theme.DarkCoreColor
import com.example.gridlauncher.ui.theme.DarkPanelColor
import com.example.gridlauncher.ui.theme.DarkTextColor
import com.example.gridlauncher.ui.theme.LightAccentColor
import com.example.gridlauncher.ui.theme.LightBgColor
import com.example.gridlauncher.ui.theme.LightBorderColor
import com.example.gridlauncher.ui.theme.LightCoreColor
import com.example.gridlauncher.ui.theme.LightPanelColor
import com.example.gridlauncher.ui.theme.LightTextColor
import com.example.gridlauncher.ui.theme.ThemePreset
import com.example.gridlauncher.ui.theme.applyCyberFont
import com.example.gridlauncher.ui.theme.loadCyberFontOption
import com.example.gridlauncher.ui.theme.loadThemePreset
import com.example.gridlauncher.ui.theme.saveCyberFontOption
import com.example.gridlauncher.ui.theme.saveThemePreset
import com.example.gridlauncher.util.ActiveIconPack
import com.example.gridlauncher.util.IconPackManager
import com.example.gridlauncher.util.LoadedIconPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ホーム画面の見た目の設定（ライト/ダーク・テーマ・フォント・アクセントカラー・アイコンパック）。
 * 変更すると、その場で SharedPreferences にも保存する。
 *
 * テーマ・フォント・アイコンパックはPROの機能のため、実際に使う値は `active*` / [colors] に
 * PROかどうかを渡して求める（PROでない場合は従来どおりの配色・標準のフォント・標準のアイコン）。
 */
@Stable
internal class LauncherThemeState(private val prefs: SharedPreferences, systemDark: Boolean) {
    /** ライト/ダーク（未設定ならシステムの設定）。 */
    var isDarkTheme by mutableStateOf(prefs.getBoolean("is_dark_theme", systemDark))
        private set
    /** 選んでいるテーマ（配色・フォントのプリセット）。 */
    var themePreset by mutableStateOf(loadThemePreset(prefs))
        private set
    /** 選んでいるフォント。 */
    var fontOption by mutableStateOf(loadCyberFontOption(prefs))
        private set
    /** アクセントカラー1（デフォルトは従来通りのオレンジ）。 */
    var accentColor by mutableStateOf(Color(prefs.getInt("accent_color", LightAccentColor.toArgb())))
        private set
    /** アクセントカラー2。カスタマイズ画面でウィジェットごとに1と2のどちらを使うか選べる。 */
    var accentColor2 by mutableStateOf(Color(prefs.getInt("accent_color_2", DefaultAccentColor2.toArgb())))
        private set
    /** 選んでいるアイコンパックのパッケージ名（nullなら使わない）。 */
    var iconPackPackage by mutableStateOf(IconPackManager.loadSelectedPackage(prefs))
        private set
    /** アイコンパックのアイコンを、アクセントカラーに染めずに本来の色で使うかどうか。 */
    var iconPackUsePackColors by mutableStateOf(IconPackManager.loadUsePackColors(prefs))
        private set

    fun activeThemePreset(isPro: Boolean): ThemePreset = if (isPro) themePreset else ThemePreset.STANDARD
    fun activeFontOption(isPro: Boolean): CyberFontOption = if (isPro) fontOption else CyberFontOption.SHARE_TECH_MONO
    fun activeIconPackPackage(isPro: Boolean): String? = if (isPro) iconPackPackage else null

    /** 画面に使う配色（アクセントカラー1）。 */
    fun colors(isPro: Boolean): CyberColors = (activeThemePreset(isPro).colors ?: if (isDarkTheme) {
        CyberColors(DarkBgColor, DarkPanelColor, DarkAccentColor, DarkTextColor, DarkBorderColor, DarkCoreColor)
    } else {
        CyberColors(LightBgColor, LightPanelColor, LightAccentColor, LightTextColor, LightBorderColor, LightCoreColor)
    }).copy(accent = accentColor)

    fun updateAccentColor(color: Color) {
        accentColor = color
        prefs.edit { putInt("accent_color", color.toArgb()) }
    }

    fun updateAccentColor2(color: Color) {
        accentColor2 = color
        prefs.edit { putInt("accent_color_2", color.toArgb()) }
    }

    fun updateFontOption(font: CyberFontOption) {
        fontOption = font
        saveCyberFontOption(prefs, font)
    }

    fun updateIconPack(packageName: String?) {
        iconPackPackage = packageName
        IconPackManager.saveSelectedPackage(prefs, packageName)
    }

    fun updateIconPackUsePackColors(usePackColors: Boolean) {
        iconPackUsePackColors = usePackColors
        IconPackManager.saveUsePackColors(prefs, usePackColors)
    }

    /** テーマを切り替える。テーマのアクセントカラー1・2とフォントも一緒に設定する（あとから個別に変えられる）。 */
    fun selectThemePreset(preset: ThemePreset) {
        themePreset = preset
        saveThemePreset(prefs, preset)
        updateAccentColor(preset.accent)
        updateAccentColor2(preset.accent2)
        updateFontOption(preset.font)
    }

    /**
     * ライト/ダークを切り替える。プリセットのテーマ（ダーク系の固定の配色）を使っている場合は、
     * 従来どおりの配色（STANDARD）に戻してから切り替える
     */
    fun updateDarkTheme(dark: Boolean) {
        if (themePreset != ThemePreset.STANDARD) {
            themePreset = ThemePreset.STANDARD
            saveThemePreset(prefs, ThemePreset.STANDARD)
        }
        isDarkTheme = dark
        prefs.edit { putBoolean("is_dark_theme", dark) }
    }
}

/** ホーム画面の見た目の設定を読み込む。 */
@Composable
internal fun rememberLauncherThemeState(prefs: SharedPreferences): LauncherThemeState {
    val systemDark = isSystemInDarkTheme()
    return remember(prefs) { LauncherThemeState(prefs, systemDark) }
}

/**
 * 見た目の設定のうち、画面全体に効かせるもの（フォントとアイコンパック）を反映する。
 * アイコンパックの読み込み（appfilter.xml の解析）は数千件あることもあるため、メインスレッド以外で行う
 */
@Composable
internal fun ApplyLauncherThemeEffects(context: Context, theme: LauncherThemeState, isPro: Boolean) {
    val activeFontOption = theme.activeFontOption(isPro)
    LaunchedEffect(activeFontOption) { applyCyberFont(activeFontOption) }

    val activeIconPackPackage = theme.activeIconPackPackage(isPro)
    val loadedIconPack by produceState<LoadedIconPack?>(null, activeIconPackPackage) {
        value = activeIconPackPackage?.let { packageName ->
            withContext(Dispatchers.IO) { IconPackManager.load(context, packageName) }
        }
    }
    val usePackColors = theme.iconPackUsePackColors
    LaunchedEffect(loadedIconPack, usePackColors) {
        IconPackManager.applyIconPack(loadedIconPack?.let { ActiveIconPack(it, usePackColors) })
    }
}
