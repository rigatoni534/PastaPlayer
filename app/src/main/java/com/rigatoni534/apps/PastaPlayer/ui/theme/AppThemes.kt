package rigatoni534.apps.PastaPlayer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import rigatoni534.apps.PastaPlayer.data.preferences.AppThemePreset
import rigatoni534.apps.PastaPlayer.data.preferences.CustomThemeColors
import rigatoni534.apps.PastaPlayer.presentation.viewmodel.ColorSchemePair

/**
 * Builds the app-wide Material 3 color schemes for the themes picked in Settings.
 *
 * - [AppThemePreset.DYNAMIC] returns `null` so the caller keeps the platform
 *   dynamic (wallpaper) colors.
 * - Named presets derive a cohesive light/dark pair from a single seed hue.
 * - [AppThemePreset.CUSTOM] derives every Material 3 role from the colors the
 *   user picked, so containers and on-colors stay readable.
 */
object AppThemes {

    fun colorSchemePairFor(
        preset: AppThemePreset,
        customColors: CustomThemeColors
    ): ColorSchemePair? = when (preset) {
        AppThemePreset.DYNAMIC -> null
        AppThemePreset.CUSTOM -> ColorSchemePair(
            light = customColorScheme(customColors, dark = false),
            dark = customColorScheme(customColors, dark = true)
        )
        else -> {
            val lightSeed = preset.seedLightArgb ?: return null
            val darkSeed = preset.seedDarkArgb ?: lightSeed
            ColorSchemePair(
                light = seedColorScheme(lightSeed, dark = false),
                dark = seedColorScheme(darkSeed, dark = true)
            )
        }
    }

    /** Primary/secondary/tertiary of the light scheme, for swatches in Settings. */
    fun previewColors(
        preset: AppThemePreset,
        customColors: CustomThemeColors
    ): List<Color> {
        val scheme = when (preset) {
            AppThemePreset.DYNAMIC -> null
            AppThemePreset.CUSTOM -> customColorScheme(customColors, dark = false)
            else -> preset.seedLightArgb?.let { seedColorScheme(it, dark = false) }
        } ?: lightColorScheme()
        return listOf(scheme.primary, scheme.secondary, scheme.tertiary)
    }

    // ---------------------------------------------------------------------
    // Seed-derived presets
    // ---------------------------------------------------------------------

    private fun seedColorScheme(seedArgb: Int, dark: Boolean): ColorScheme {
        val hsl = FloatArray(3)
        android.graphics.Color.colorToHSL(seedArgb, hsl)
        val hue = hsl[0]
        val saturation = hsl[1].coerceIn(0.18f, 0.92f)
        val tertiaryHue = hue + 60f
        val errorHue = 6f

        fun tone(h: Float = hue, s: Float = saturation, l: Float): Color =
            Color.hsl(
                hue = ((h % 360f) + 360f) % 360f,
                saturation = s.coerceIn(0f, 1f),
                lightness = l.coerceIn(0f, 1f)
            )

        return if (dark) {
            darkColorScheme(
                primary = tone(l = 0.80f),
                onPrimary = tone(s = saturation * 0.7f, l = 0.18f),
                primaryContainer = tone(s = saturation, l = 0.32f),
                onPrimaryContainer = tone(s = saturation * 0.85f, l = 0.90f),
                inversePrimary = tone(l = 0.40f),
                secondary = tone(s = saturation * 0.45f, l = 0.80f),
                onSecondary = tone(s = saturation * 0.45f, l = 0.20f),
                secondaryContainer = tone(s = saturation * 0.45f, l = 0.30f),
                onSecondaryContainer = tone(s = saturation * 0.45f, l = 0.90f),
                tertiary = tone(h = tertiaryHue, s = saturation * 0.60f, l = 0.80f),
                onTertiary = tone(h = tertiaryHue, s = saturation * 0.60f, l = 0.20f),
                tertiaryContainer = tone(h = tertiaryHue, s = saturation * 0.55f, l = 0.30f),
                onTertiaryContainer = tone(h = tertiaryHue, s = saturation * 0.55f, l = 0.90f),
                background = tone(s = 0.06f, l = 0.07f),
                onBackground = tone(s = 0.04f, l = 0.92f),
                surface = tone(s = 0.06f, l = 0.07f),
                onSurface = tone(s = 0.04f, l = 0.92f),
                surfaceVariant = tone(s = 0.08f, l = 0.20f),
                onSurfaceVariant = tone(s = 0.06f, l = 0.80f),
                surfaceTint = tone(l = 0.80f),
                inverseSurface = tone(s = 0.04f, l = 0.92f),
                inverseOnSurface = tone(s = 0.06f, l = 0.14f),
                outline = tone(s = 0.06f, l = 0.55f),
                outlineVariant = tone(s = 0.06f, l = 0.30f),
                scrim = Color.Black,
                surfaceBright = tone(s = 0.06f, l = 0.24f),
                surfaceDim = tone(s = 0.06f, l = 0.07f),
                surfaceContainerLowest = tone(s = 0.06f, l = 0.04f),
                surfaceContainerLow = tone(s = 0.06f, l = 0.10f),
                surfaceContainer = tone(s = 0.06f, l = 0.13f),
                surfaceContainerHigh = tone(s = 0.06f, l = 0.17f),
                surfaceContainerHighest = tone(s = 0.06f, l = 0.21f),
                error = tone(h = errorHue, s = 0.85f, l = 0.75f),
                onError = tone(h = errorHue, s = 0.80f, l = 0.20f),
                errorContainer = tone(h = errorHue, s = 0.70f, l = 0.30f),
                onErrorContainer = tone(h = errorHue, s = 0.70f, l = 0.90f)
            )
        } else {
            lightColorScheme(
                primary = tone(l = 0.40f),
                onPrimary = tone(s = 0.08f, l = 0.99f),
                primaryContainer = tone(s = saturation * 0.85f, l = 0.90f),
                onPrimaryContainer = tone(s = saturation, l = 0.14f),
                inversePrimary = tone(s = saturation * 0.85f, l = 0.80f),
                secondary = tone(s = saturation * 0.45f, l = 0.40f),
                onSecondary = tone(s = saturation * 0.10f, l = 0.99f),
                secondaryContainer = tone(s = saturation * 0.45f, l = 0.90f),
                onSecondaryContainer = tone(s = saturation * 0.45f, l = 0.14f),
                tertiary = tone(h = tertiaryHue, s = saturation * 0.60f, l = 0.40f),
                onTertiary = tone(h = tertiaryHue, s = 0.10f, l = 0.99f),
                tertiaryContainer = tone(h = tertiaryHue, s = saturation * 0.55f, l = 0.90f),
                onTertiaryContainer = tone(h = tertiaryHue, s = saturation * 0.55f, l = 0.14f),
                background = tone(s = 0.03f, l = 0.99f),
                onBackground = tone(s = 0.03f, l = 0.10f),
                surface = tone(s = 0.03f, l = 0.99f),
                onSurface = tone(s = 0.03f, l = 0.10f),
                surfaceVariant = tone(s = 0.08f, l = 0.93f),
                onSurfaceVariant = tone(s = 0.05f, l = 0.30f),
                surfaceTint = tone(l = 0.40f),
                inverseSurface = tone(s = 0.05f, l = 0.20f),
                inverseOnSurface = tone(s = 0.03f, l = 0.95f),
                outline = tone(s = 0.05f, l = 0.50f),
                outlineVariant = tone(s = 0.06f, l = 0.80f),
                scrim = Color.Black,
                surfaceBright = tone(s = 0.03f, l = 0.99f),
                surfaceDim = tone(s = 0.03f, l = 0.87f),
                surfaceContainerLowest = tone(s = 0.03f, l = 1f),
                surfaceContainerLow = tone(s = 0.03f, l = 0.965f),
                surfaceContainer = tone(s = 0.03f, l = 0.94f),
                surfaceContainerHigh = tone(s = 0.03f, l = 0.915f),
                surfaceContainerHighest = tone(s = 0.03f, l = 0.89f),
                error = tone(h = errorHue, s = 0.85f, l = 0.42f),
                onError = tone(h = errorHue, s = 0.10f, l = 0.99f),
                errorContainer = tone(h = errorHue, s = 0.80f, l = 0.90f),
                onErrorContainer = tone(h = errorHue, s = 0.80f, l = 0.14f)
            )
        }
    }

    // ---------------------------------------------------------------------
    // Custom palette
    // ---------------------------------------------------------------------

    private fun customColorScheme(colors: CustomThemeColors, dark: Boolean): ColorScheme {
        val primary = Color(colors.primary)
        val secondary = Color(colors.secondary)
        val tertiary = Color(colors.tertiary)
        val error = Color(colors.error)
        val outline = Color(colors.outline)

        val lightSurface = Color(colors.surface)
        val lightBackground = Color(colors.background)
        val lightVariant = Color(colors.surfaceVariant)

        if (!dark) {
            val primaryContainer = lerp(lightSurface, primary, 0.78f)
            val secondaryContainer = lerp(lightSurface, secondary, 0.78f)
            val tertiaryContainer = lerp(lightSurface, tertiary, 0.78f)
            val errorContainer = lerp(lightSurface, error, 0.80f)
            return lightColorScheme(
                primary = primary,
                onPrimary = contentColorOn(primary),
                primaryContainer = primaryContainer,
                onPrimaryContainer = contentColorOn(primaryContainer),
                inversePrimary = lerp(primary, Color.White, 0.55f),
                secondary = secondary,
                onSecondary = contentColorOn(secondary),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = contentColorOn(secondaryContainer),
                tertiary = tertiary,
                onTertiary = contentColorOn(tertiary),
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = contentColorOn(tertiaryContainer),
                background = lightBackground,
                onBackground = contentColorOn(lightBackground),
                surface = lightSurface,
                onSurface = contentColorOn(lightSurface),
                surfaceVariant = lightVariant,
                onSurfaceVariant = contentColorOn(lightVariant).copy(alpha = 0.80f),
                surfaceTint = primary,
                inverseSurface = contentColorOn(lightSurface),
                inverseOnSurface = lightSurface,
                outline = outline,
                outlineVariant = lerp(outline, lightSurface, 0.55f),
                scrim = Color.Black,
                surfaceBright = lerp(lightSurface, Color.White, 0.35f),
                surfaceDim = lerp(lightSurface, Color.Black, 0.10f),
                surfaceContainerLowest = lerp(lightSurface, Color.White, 0.55f),
                surfaceContainerLow = lerp(lightSurface, primary, 0.03f),
                surfaceContainer = lerp(lightSurface, primary, 0.06f),
                surfaceContainerHigh = lerp(lightSurface, primary, 0.09f),
                surfaceContainerHighest = lerp(lightSurface, primary, 0.12f),
                error = error,
                onError = contentColorOn(error),
                errorContainer = errorContainer,
                onErrorContainer = contentColorOn(errorContainer)
            )
        }

        val darkSurface = darkenForDarkMode(lightSurface)
        val darkBackground = darkenForDarkMode(lightBackground)
        val darkVariant = lerp(lightVariant, Color.Black, 0.72f)
        val primaryContainer = lerp(darkSurface, primary, 0.28f)
        val secondaryContainer = lerp(darkSurface, secondary, 0.28f)
        val tertiaryContainer = lerp(darkSurface, tertiary, 0.28f)
        val errorContainer = lerp(darkSurface, error, 0.30f)
        return darkColorScheme(
            primary = brightenForDarkMode(primary),
            onPrimary = contentColorOn(brightenForDarkMode(primary)),
            primaryContainer = primaryContainer,
            onPrimaryContainer = contentColorOn(primaryContainer),
            inversePrimary = primary,
            secondary = brightenForDarkMode(secondary),
            onSecondary = contentColorOn(brightenForDarkMode(secondary)),
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = contentColorOn(secondaryContainer),
            tertiary = brightenForDarkMode(tertiary),
            onTertiary = contentColorOn(brightenForDarkMode(tertiary)),
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = contentColorOn(tertiaryContainer),
            background = darkBackground,
            onBackground = contentColorOn(darkBackground),
            surface = darkSurface,
            onSurface = contentColorOn(darkSurface),
            surfaceVariant = darkVariant,
            onSurfaceVariant = contentColorOn(darkVariant).copy(alpha = 0.80f),
            surfaceTint = brightenForDarkMode(primary),
            inverseSurface = contentColorOn(darkSurface),
            inverseOnSurface = darkSurface,
            outline = lerp(outline, Color.White, 0.35f),
            outlineVariant = lerp(outline, darkSurface, 0.55f),
            scrim = Color.Black,
            surfaceBright = lerp(darkSurface, Color.White, 0.16f),
            surfaceDim = lerp(darkSurface, Color.Black, 0.20f),
            surfaceContainerLowest = lerp(darkSurface, Color.Black, 0.25f),
            surfaceContainerLow = lerp(darkSurface, brightenForDarkMode(primary), 0.04f),
            surfaceContainer = lerp(darkSurface, brightenForDarkMode(primary), 0.07f),
            surfaceContainerHigh = lerp(darkSurface, brightenForDarkMode(primary), 0.10f),
            surfaceContainerHighest = lerp(darkSurface, brightenForDarkMode(primary), 0.13f),
            error = brightenForDarkMode(error),
            onError = contentColorOn(brightenForDarkMode(error)),
            errorContainer = errorContainer,
            onErrorContainer = contentColorOn(errorContainer)
        )
    }

    /** Picks a readable foreground for [background] based on its luminance. */
    private fun contentColorOn(background: Color): Color =
        if (background.luminance() > 0.45f) Color(0xFF17161B) else Color(0xFFFFFBFF)

    private fun darkenForDarkMode(color: Color): Color =
        if (color.luminance() > 0.25f) lerp(color, Color.Black, 0.84f) else color

    private fun brightenForDarkMode(color: Color): Color =
        if (color.luminance() < 0.45f) lerp(color, Color.White, 0.40f) else color
}
