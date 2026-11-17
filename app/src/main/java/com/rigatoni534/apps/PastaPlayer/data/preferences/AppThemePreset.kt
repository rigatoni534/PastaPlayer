package rigatoni534.apps.PastaPlayer.data.preferences

import androidx.annotation.StringRes
import rigatoni534.apps.PastaPlayer.R

/**
 * App-wide theme family selected in the Themes settings section.
 *
 * [DYNAMIC] keeps the Android 12+ wallpaper-based Material You colors, the named
 * presets mimic common Material You palettes, and [CUSTOM] builds both schemes
 * from the user-picked [CustomThemeColors].
 */
enum class AppThemePreset(
    val storageKey: String,
    @StringRes val labelRes: Int,
    val seedLightArgb: Int? = null,
    val seedDarkArgb: Int? = null
) {
    DYNAMIC(
        storageKey = "dynamic",
        labelRes = R.string.settings_theme_preset_dynamic
    ),
    LAVENDER(
        storageKey = "lavender",
        labelRes = R.string.settings_theme_preset_lavender,
        seedLightArgb = 0xFF6750A4.toInt(),
        seedDarkArgb = 0xFFD0BCFF.toInt()
    ),
    OCEAN(
        storageKey = "ocean",
        labelRes = R.string.settings_theme_preset_ocean,
        seedLightArgb = 0xFF00639B.toInt(),
        seedDarkArgb = 0xFF9CCBFF.toInt()
    ),
    FOREST(
        storageKey = "forest",
        labelRes = R.string.settings_theme_preset_forest,
        seedLightArgb = 0xFF3F6832.toInt(),
        seedDarkArgb = 0xFFA6D68C.toInt()
    ),
    SUNSET(
        storageKey = "sunset",
        labelRes = R.string.settings_theme_preset_sunset,
        seedLightArgb = 0xFF8C4A1F.toInt(),
        seedDarkArgb = 0xFFFFB68A.toInt()
    ),
    ROSE(
        storageKey = "rose",
        labelRes = R.string.settings_theme_preset_rose,
        seedLightArgb = 0xFF8C4A60.toInt(),
        seedDarkArgb = 0xFFFFB0C8.toInt()
    ),
    SLATE(
        storageKey = "slate",
        labelRes = R.string.settings_theme_preset_slate,
        seedLightArgb = 0xFF5A5D63.toInt(),
        seedDarkArgb = 0xFFC4C7CE.toInt()
    ),
    CUSTOM(
        storageKey = "custom",
        labelRes = R.string.settings_theme_preset_custom
    );

    companion object {
        val default: AppThemePreset = DYNAMIC

        /** Named Material You palettes, excluding the dynamic and custom entries. */
        val palettes: List<AppThemePreset> = entries.filter {
            it != DYNAMIC && it != CUSTOM
        }

        fun fromStorageKey(key: String?): AppThemePreset =
            entries.firstOrNull { it.storageKey == key } ?: default
    }
}
