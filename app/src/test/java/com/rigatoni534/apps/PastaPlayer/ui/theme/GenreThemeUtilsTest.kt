package rigatoni534.apps.PastaPlayer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.common.truth.Truth.assertThat
import rigatoni534.apps.PastaPlayer.data.model.Genre
import rigatoni534.apps.md3.Md3PaletteStyle
import org.junit.Test
import rigatoni534.apps.md3.generateColorSchemeFromSeed
import rigatoni534.apps.md3.generateMonochromeColorSchemeFromSeed

class GenreThemeUtilsTest {

    @Test
    fun getGenreDetailColorScheme_usesGenreCardContainerAsSeedInLightTheme() {
        val genre = Genre(id = "rock", name = "Rock")
        val paletteStyle = Md3PaletteStyle.VIBRANT
        val cardColor = GenreThemeUtils.getGenreThemeColor(
            genre = genre,
            isDark = false,
            fallbackGenreId = genre.id
        )

        val actual = GenreThemeUtils.getGenreDetailColorScheme(
            genre = genre,
            isDark = false,
            fallbackGenreId = genre.id,
            paletteStyle = paletteStyle
        )
        val expected = generateColorSchemeFromSeed(
            seedColor = cardColor.container,
            paletteStyle = paletteStyle
        ).light

        assertThat(actual.toArgbList()).containsExactlyElementsIn(expected.toArgbList()).inOrder()
    }

    @Test
    fun getGenreDetailColorScheme_usesGenreCardContainerAsSeedInDarkTheme() {
        val genre = Genre(id = "hip_hop", name = "Hip Hop")
        val paletteStyle = Md3PaletteStyle.EXPRESSIVE
        val cardColor = GenreThemeUtils.getGenreThemeColor(
            genre = genre,
            isDark = true,
            fallbackGenreId = genre.id
        )

        val actual = GenreThemeUtils.getGenreDetailColorScheme(
            genre = genre,
            isDark = true,
            fallbackGenreId = genre.id,
            paletteStyle = paletteStyle
        )
        val expected = generateColorSchemeFromSeed(
            seedColor = cardColor.container,
            paletteStyle = paletteStyle
        ).dark

        assertThat(actual.toArgbList()).containsExactlyElementsIn(expected.toArgbList()).inOrder()
    }

    @Test
    fun getGenreDetailColorScheme_forUnknownGenre_usesMonochromeScheme() {
        val actual = GenreThemeUtils.getGenreDetailColorScheme(
            genre = null,
            isDark = false,
            fallbackGenreId = "unknown",
            paletteStyle = Md3PaletteStyle.FRUIT_SALAD
        )
        val expected = generateMonochromeColorSchemeFromSeed(Color(0xFF7C7D84)).light

        assertThat(actual.toArgbList()).containsExactlyElementsIn(expected.toArgbList()).inOrder()
    }

    private fun ColorScheme.toArgbList(): List<Int> {
        return listOf(
            primary,
            onPrimary,
            primaryContainer,
            onPrimaryContainer,
            inversePrimary,
            secondary,
            onSecondary,
            secondaryContainer,
            onSecondaryContainer,
            tertiary,
            onTertiary,
            tertiaryContainer,
            onTertiaryContainer,
            background,
            onBackground,
            surface,
            onSurface,
            surfaceVariant,
            onSurfaceVariant,
            surfaceTint,
            inverseSurface,
            inverseOnSurface,
            error,
            onError,
            errorContainer,
            onErrorContainer,
            outline,
            outlineVariant,
            scrim,
            surfaceBright,
            surfaceDim,
            surfaceContainer,
            surfaceContainerHigh,
            surfaceContainerHighest,
            surfaceContainerLow,
            surfaceContainerLowest,
            primaryFixed,
            primaryFixedDim,
            onPrimaryFixed,
            onPrimaryFixedVariant,
            secondaryFixed,
            secondaryFixedDim,
            onSecondaryFixed,
            onSecondaryFixedVariant,
            tertiaryFixed,
            tertiaryFixedDim,
            onTertiaryFixed,
            onTertiaryFixedVariant
        ).map(Color::toArgb)
    }
}
