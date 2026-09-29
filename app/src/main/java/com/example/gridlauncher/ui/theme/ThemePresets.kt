package com.example.gridlauncher.ui.theme

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.edit
import com.example.gridlauncher.R

/**
 * アプリ全体で使うフォントの選択肢（すべて Google Fonts の OFL ライセンスのもの。ライセンス文は
 * `assets/licenses/` に同梱）。日本語の文字は、どれを選んでも端末の標準フォントで表示される。
 *
 * @property label 選択肢として表示する名前。
 */
enum class CyberFontOption(val label: String) {
    SHARE_TECH_MONO("SHARE TECH MONO"),
    ORBITRON("ORBITRON"),
    VT323("VT323"),
    RAJDHANI("RAJDHANI"),
    AUDIOWIDE("AUDIOWIDE");

    /** このフォントの[FontFamily]。 */
    val fontFamily: FontFamily by lazy {
        when (this) {
            SHARE_TECH_MONO -> FontFamily(Font(R.font.share_tech_mono))
            ORBITRON -> FontFamily(Font(R.font.orbitron))
            VT323 -> FontFamily(Font(R.font.vt323))
            RAJDHANI -> FontFamily(
                Font(R.font.rajdhani_medium, FontWeight.Normal),
                Font(R.font.rajdhani_bold, FontWeight.Bold)
            )
            AUDIOWIDE -> FontFamily(Font(R.font.audiowide))
        }
    }
}

/**
 * 背景・パネル・文字・枠線・コア色・アクセントカラー1/2・フォントをまとめて切り替えるテーマ。
 * [STANDARD]は従来どおり（ライト/ダークの切り替えと、自分で選んだアクセントカラー）で、
 * それ以外はダーク系の固定の配色になる（アクセントカラーは選んだあとに変えることもできる）。
 *
 * @property label 選択肢として表示する名前。
 * @property colors 背景などの配色（[STANDARD]ではnull＝ライト/ダークの配色を使う）。
 * @property accent 選んだときに設定するアクセントカラー1。
 * @property accent2 選んだときに設定するアクセントカラー2。
 * @property font 選んだときに設定するフォント。
 */
enum class ThemePreset(
    val label: String,
    val colors: CyberColors?,
    val accent: Color,
    val accent2: Color,
    val font: CyberFontOption
) {
    STANDARD("STANDARD", null, LightAccentColor, Color(0xFF00E5FF), CyberFontOption.SHARE_TECH_MONO),
    MATRIX(
        "MATRIX",
        CyberColors(Color(0xFF020A04), Color(0xFF06140A), Color(0xFF39FF14), Color(0xFFB6FFC4), Color(0x3339FF14), Color(0xFF00C853)),
        Color(0xFF39FF14), Color(0xFF00E676), CyberFontOption.VT323
    ),
    SYNTHWAVE(
        "SYNTHWAVE",
        CyberColors(Color(0xFF120824), Color(0xFF1C0F36), Color(0xFFFF2BD6), Color(0xFFF5E6FF), Color(0x33F5E6FF), Color(0xFF7C4DFF)),
        Color(0xFFFF2BD6), Color(0xFF00E5FF), CyberFontOption.AUDIOWIDE
    ),
    AMBER_TERMINAL(
        "AMBER TERMINAL",
        CyberColors(Color(0xFF0F0A00), Color(0xFF1A1200), Color(0xFFFFB000), Color(0xFFFFC46B), Color(0x33FFB000), Color(0xFFFF8F00)),
        Color(0xFFFFB000), Color(0xFFFF6D00), CyberFontOption.VT323
    ),
    ICE(
        "ICE",
        CyberColors(Color(0xFF06121C), Color(0xFF0C1E2C), Color(0xFF80D8FF), Color(0xFFE3F6FF), Color(0x33E3F6FF), Color(0xFF4FC3F7)),
        Color(0xFF80D8FF), Color(0xFFB388FF), CyberFontOption.ORBITRON
    ),
    CRIMSON(
        "CRIMSON",
        CyberColors(Color(0xFF0E0406), Color(0xFF1C0A0E), Color(0xFFFF3B4E), Color(0xFFFFE3E6), Color(0x33FFE3E6), Color(0xFFFF1744)),
        Color(0xFFFF3B4E), Color(0xFFFFD600), CyberFontOption.RAJDHANI
    )
}

private const val KEY_THEME_PRESET = "theme_preset"
private const val KEY_FONT = "cyber_font"

/** 保存したテーマを読み込む（未設定なら[ThemePreset.STANDARD]）。 */
fun loadThemePreset(prefs: SharedPreferences): ThemePreset =
    prefs.getString(KEY_THEME_PRESET, null)?.let { name -> ThemePreset.entries.firstOrNull { it.name == name } }
        ?: ThemePreset.STANDARD

/** テーマを保存する。 */
fun saveThemePreset(prefs: SharedPreferences, preset: ThemePreset) {
    prefs.edit { putString(KEY_THEME_PRESET, preset.name) }
}

/** 保存したフォントを読み込む（未設定なら[CyberFontOption.SHARE_TECH_MONO]）。 */
fun loadCyberFontOption(prefs: SharedPreferences): CyberFontOption =
    prefs.getString(KEY_FONT, null)?.let { name -> CyberFontOption.entries.firstOrNull { it.name == name } }
        ?: CyberFontOption.SHARE_TECH_MONO

/** フォントを保存する。 */
fun saveCyberFontOption(prefs: SharedPreferences, font: CyberFontOption) {
    prefs.edit { putString(KEY_FONT, font.name) }
}
