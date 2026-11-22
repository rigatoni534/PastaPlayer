package rigatoni534.apps.md3

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.graphics.ColorUtils

val LocalMd3DarkTheme = staticCompositionLocalOf { false }
val LocalMd3ShowScrollbar = staticCompositionLocalOf { true }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Suppress("DEPRECATION")
@Composable
fun Md3StatusBarStyle(
    color: Color,
    useDarkIcons: Boolean = ColorUtils.calculateLuminance(color.toArgb()) > 0.55,
    navigationColor: Color? = null,
    useDarkNavigationIcons: Boolean = navigationColor
        ?.let { ColorUtils.calculateLuminance(it.toArgb()) > 0.55 }
        ?: useDarkIcons
) {
    val view = LocalView.current
    if (view.isInEditMode) return

    val updateNavigationBar = navigationColor != null
    SideEffect {
        val window = view.context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
        }

        WindowCompat.getInsetsController(window, view).run {
            isAppearanceLightStatusBars = useDarkIcons

            if (updateNavigationBar) {
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = false
                }
                isAppearanceLightNavigationBars = useDarkNavigationIcons
            }
        }
    }
}

val Md3DarkColorScheme = darkColorScheme(
    primary = Md3PurplePrimary,
    secondary = Md3Pink,
    tertiary = Md3Orange,
    background = Md3PurpleDark,
    surface = Md3Surface,
    onPrimary = Md3White,
    onSecondary = Md3White,
    onTertiary = Md3White,
    onBackground = Md3White,
    onSurface = Md3LightPurple, // Texto sobre superficies
    error = Color(0xFFFF5252),
    onError = Md3White
)

val Md3LightColorScheme = lightColorScheme(
    primary = Md3LightPrimary,
    onPrimary = Md3White,
    primaryContainer = Md3LightPrimaryContainer,
    onPrimaryContainer = Md3LightOnPrimaryContainer,
    secondary = Md3Pink,
    onSecondary = Md3White,
    secondaryContainer = Md3Pink.copy(alpha = 0.15f),
    onSecondaryContainer = Md3Pink.copy(alpha = 0.85f),
    tertiary = Md3Orange,
    onTertiary = Md3Black,
    background = Md3LightBackground,
    onBackground = Md3LightOnSurface,
    surface = Md3LightSurface,
    onSurface = Md3LightOnSurface,
    surfaceVariant = Md3LightSurfaceVariant,
    onSurfaceVariant = Md3LightOnSurfaceVariant,
    outline = Md3LightOutline,
    outlineVariant = Md3LightOutline.copy(alpha = 0.6f),
    surfaceTint = Md3LightPrimary,
    error = Color(0xFFD32F2F),
    onError = Md3White
)

/**
 * Material 3 theme entry point for the design system.
 *
 * Resolution order for the colour scheme:
 * 1. [colorScheme], if given — used exactly as provided.
 * 2. [colorSchemePair] — the light or dark side is picked from [darkTheme]. Use this for
 *    content-driven themes such as album-art or genre palettes; build one with
 *    [generateColorSchemeFromSeed].
 * 3. Platform dynamic colour (Android 12+) when [dynamicColor] is `true`.
 * 4. [Md3DarkColorScheme] / [Md3LightColorScheme].
 *
 * ```kotlin
 * Md3Theme {
 *     Surface { Text("Hello, Material You") }
 * }
 * ```
 *
 * @param darkTheme whether the dark variant should be used.
 * @param colorScheme a fully built scheme that overrides every other source.
 * @param colorSchemePair a light/dark pair to pick from, e.g. from `generateColorSchemeFromSeed`.
 * @param dynamicColor whether wallpaper-based dynamic colour may be used.
 * @param typography the type scale, [Md3Typography] by default.
 * @param shapes the shape scale, [Md3Shapes] by default.
 * @param styleSystemBars whether the status/navigation bars should be made transparent and
 *   their icon brightness matched to the scheme. Turn off when the host owns the system bars.
 * @param content the composables to theme.
 */
@Composable
fun Md3Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: ColorScheme? = null,
    colorSchemePair: ColorSchemePair? = null,
    dynamicColor: Boolean = true,
    typography: Typography = Md3Typography,
    shapes: Shapes = Md3Shapes,
    styleSystemBars: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val finalColorScheme = colorScheme ?: when {
        colorSchemePair != null -> if (darkTheme) colorSchemePair.dark else colorSchemePair.light
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> try {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } catch (e: Exception) {
            // Some devices ship a broken dynamic colour provider; fall back to the palette.
            if (darkTheme) Md3DarkColorScheme else Md3LightColorScheme
        }
        darkTheme -> Md3DarkColorScheme
        else -> Md3LightColorScheme
    }

    if (styleSystemBars) {
        Md3StatusBarStyle(
            color = finalColorScheme.background,
            navigationColor = finalColorScheme.background
        )
    }

    CompositionLocalProvider(LocalMd3DarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = finalColorScheme,
            typography = typography,
            shapes = shapes,
            content = content
        )
    }
}
