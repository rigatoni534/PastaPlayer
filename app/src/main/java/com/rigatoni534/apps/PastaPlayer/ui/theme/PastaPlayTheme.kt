package rigatoni534.apps.PastaPlayer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import rigatoni534.apps.md3.ColorSchemePair
import rigatoni534.apps.md3.Md3Theme

/**
 * App-level entry point for the PastaPlayer theming rules on top of the shared MD3
 * design system in `:md3`.
 *
 * When [colorSchemePairOverride] is `null` the platform dynamic (wallpaper) colours are
 * used on Android 12+, otherwise the palette is taken from the current album artwork or
 * the theme preset picked in Settings.
 */
@Composable
fun PastaPlayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorSchemePairOverride: ColorSchemePair? = null,
    content: @Composable () -> Unit
) {
    Md3Theme(
        darkTheme = darkTheme,
        colorSchemePair = colorSchemePairOverride,
        content = content
    )
}
