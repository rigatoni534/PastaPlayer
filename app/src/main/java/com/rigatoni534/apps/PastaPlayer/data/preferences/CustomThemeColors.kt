package rigatoni534.apps.PastaPlayer.data.preferences

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import rigatoni534.apps.PastaPlayer.R

/**
 * Colors the user can control for the custom theme. Every other Material 3 role
 * (on-colors, containers, outline variants) is derived from these in
 * `rigatoni534.apps.PastaPlayer.ui.theme.AppThemes`.
 */
enum class CustomThemeColorRole(@StringRes val labelRes: Int) {
    PRIMARY(R.string.settings_theme_role_primary),
    SECONDARY(R.string.settings_theme_role_secondary),
    TERTIARY(R.string.settings_theme_role_tertiary),
    BACKGROUND(R.string.settings_theme_role_background),
    SURFACE(R.string.settings_theme_role_surface),
    SURFACE_VARIANT(R.string.settings_theme_role_surface_variant),
    ERROR(R.string.settings_theme_role_error),
    OUTLINE(R.string.settings_theme_role_outline)
}

@Serializable
data class CustomThemeColors(
    val primary: Int = 0xFF6750A4.toInt(),
    val secondary: Int = 0xFF625B71.toInt(),
    val tertiary: Int = 0xFF7D5260.toInt(),
    val background: Int = 0xFFFFFBFE.toInt(),
    val surface: Int = 0xFFFFFBFE.toInt(),
    val surfaceVariant: Int = 0xFFE7E0EC.toInt(),
    val error: Int = 0xFFB3261E.toInt(),
    val outline: Int = 0xFF79747E.toInt()
) {
    fun colorFor(role: CustomThemeColorRole): Int = when (role) {
        CustomThemeColorRole.PRIMARY -> primary
        CustomThemeColorRole.SECONDARY -> secondary
        CustomThemeColorRole.TERTIARY -> tertiary
        CustomThemeColorRole.BACKGROUND -> background
        CustomThemeColorRole.SURFACE -> surface
        CustomThemeColorRole.SURFACE_VARIANT -> surfaceVariant
        CustomThemeColorRole.ERROR -> error
        CustomThemeColorRole.OUTLINE -> outline
    }

    fun withColor(role: CustomThemeColorRole, argb: Int): CustomThemeColors = when (role) {
        CustomThemeColorRole.PRIMARY -> copy(primary = argb)
        CustomThemeColorRole.SECONDARY -> copy(secondary = argb)
        CustomThemeColorRole.TERTIARY -> copy(tertiary = argb)
        CustomThemeColorRole.BACKGROUND -> copy(background = argb)
        CustomThemeColorRole.SURFACE -> copy(surface = argb)
        CustomThemeColorRole.SURFACE_VARIANT -> copy(surfaceVariant = argb)
        CustomThemeColorRole.ERROR -> copy(error = argb)
        CustomThemeColorRole.OUTLINE -> copy(outline = argb)
    }

    fun toJson(): String = json.encodeToString(serializer(), this)

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        fun fromJson(raw: String?): CustomThemeColors {
            if (raw.isNullOrBlank()) return CustomThemeColors()
            return runCatching { json.decodeFromString(serializer(), raw) }
                .getOrElse { CustomThemeColors() }
        }
    }
}
