# MD3 — the PastaPlayer Material 3 design system

A small, dependency-light design system extracted from PastaPlayer so other apps can use the
same Material 3 look: one theme entry point, a tonal-palette engine that builds full
`ColorScheme`s from a seed colour or from artwork, the type scale, the shape scale, and a
system-bar helper.

Everything lives in one Gradle module (`:md3`), namespace and package `rigatoni534.apps.md3`.

| File | What it gives you |
| --- | --- |
| `Md3Theme.kt` | `Md3Theme`, `Md3StatusBarStyle`, `Md3DarkColorScheme`, `Md3LightColorScheme`, `LocalMd3DarkTheme`, `LocalMd3ShowScrollbar` |
| `Md3Color.kt` | The palette constants |
| `Md3ColorRoles.kt` | Seed/artwork → `ColorSchemePair` generation, palette scoring and extraction config |
| `Md3PaletteStyle.kt` | The four Material palette styles (tonal spot, vibrant, expressive, fruit salad) |
| `Md3ColorAccuracy.kt` | Bounds for the artwork colour-accuracy level |
| `Md3ColorSchemePair.kt` | `ColorSchemePair(light, dark)` |
| `Md3Typography.kt` | `Md3Typography`, `ExpTitleTypography`, `GoogleSansRounded`, `MontserratFamily` |
| `Md3Shapes.kt` / `ShapeCache.kt` | `Md3Shapes` and cached smooth-corner shapes |

## Requirements

* Android Gradle Plugin 9.x, Kotlin 2.4, JDK 21
* `compileSdk = 37`, `minSdk = 30` (lowering `minSdk` works, the dynamic-colour path is guarded by `Build.VERSION.SDK_INT >= S`)
* A Compose BOM recent enough for Material 3 1.3+ (this repo pins `2026.06.00`)

The module declares its dependencies as `api`, so you do **not** need to add Compose,
Material 3, `ui-text-google-fonts` or the smooth-corner library yourself — but adding the
Compose BOM in your app is still recommended so every module resolves the same versions.

## Installation

### Copy the module (simplest)

1. Copy the `md3/` directory into your project root.
2. In `settings.gradle.kts`:

   ```kotlin
   include(":md3")
   ```

3. In your app module:

   ```kotlin
   dependencies {
       implementation(project(":md3"))
   }
   ```

4. `ShapeCache` uses `com.github.racra:smooth-corner-rect-android-compose`, which is hosted on
   JitPack. Add the repository if you do not have it yet:

   ```kotlin
   dependencyResolutionManagement {
       repositories {
           google()
           mavenCentral()
           maven("https://jitpack.io") {
               content { includeGroup("com.github.racra") }
           }
       }
   }
   ```

### As a git submodule

```bash
git submodule add https://github.com/rigatoni534/PastaPlayer.git vendor/pastaplay
```

Then add `include(":md3")` with a project directory override:

```kotlin
include(":md3")
project(":md3").projectDir = file("vendor/pastaplay/md3")
```

## Quick start

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Md3Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("Hello, Material You")
                }
            }
        }
    }
}
```

That is the whole integration. You get Material 3 typography, shapes, dynamic (wallpaper)
colour on Android 12+, the palette below on older devices, transparent system bars with the
right icon brightness, and the `LocalMd3DarkTheme` / `LocalMd3ShowScrollbar` locals.

## Theming

```kotlin
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
)
```

The colour scheme is resolved in this order:

1. `colorScheme` — used exactly as given.
2. `colorSchemePair` — the light or dark side is picked from `darkTheme`.
3. Platform dynamic colour, when `dynamicColor = true` and the device runs Android 12+.
4. `Md3DarkColorScheme` / `Md3LightColorScheme` (the bundled palette).

| Parameter | Use it when |
| --- | --- |
| `colorScheme` | You computed a single scheme and know which one you want. |
| `colorSchemePair` | You have content-driven themes (album art, a brand colour) and want the theme to pick light/dark. |
| `dynamicColor = false` | Your brand colours must win over the wallpaper. |
| `styleSystemBars = false` | Your activity already manages the system bars (or you are not in an `Activity`). |

### Palette from a seed colour

```kotlin
val schemePair = generateColorSchemeFromSeed(
    seedColor = Color(0xFF6750A4),
    paletteStyle = Md3PaletteStyle.TONAL_SPOT
)

Md3Theme(colorSchemePair = schemePair) { /* content */ }
```

`Md3PaletteStyle` maps to the Material colour utilities' schemes:

| Value | Feel |
| --- | --- |
| `TONAL_SPOT` | Default Material You balance (the `.default`) |
| `VIBRANT` | Saturated, playful primaries |
| `EXPRESSIVE` | Unusual hues, strong containers |
| `FRUIT_SALAD` | Two-hue, high-contrast mix |

Every call returns a full light **and** dark scheme with containers, on-colours, outlines and
inverse roles filled in, so you never have to patch roles by hand.

### Palette from artwork

```kotlin
val seed: Color = extractSeedColor(bitmap)          // artwork → representative colour
val pair = generateColorSchemeFromSeed(seed)        // colour → light/dark schemes
```

`extractSeedColor` quantises and scores the bitmap so covers that are mostly one hue still
produce a usable accent, and mostly-neutral covers fall back to a near-neutral scheme instead
of a muddy colour. Results are cached; call `clearExtractedColorCache()` when you change the
accuracy setting.

```kotlin
val config = ColorExtractionConfig(
    downscaleMaxDimension = 128,          // analysis size, bigger = slower but finer
    quantizerMaxColors = 128,
    accuracyLevel = 6                     // see below
)
extractSeedColor(bitmap, config)
```

**Accuracy** (`Md3ColorAccuracy`, 0–10, default `0`) trades "average of the cover" for "most
faithful to the cover's real colour". Higher values pull yellows away from green and blues away
from teal at the cost of some contrast consistency. `Md3ColorAccuracy.clamp(value)` keeps a
persisted setting in range.

`ColorScoringConfig` exposes the scoring weights if you want to tune extraction further.

### Monochrome

```kotlin
val pair = generateMonochromeColorSchemeFromSeed(seed)   // grayscale, artwork-derived
```

Useful for "unknown artist" style fallbacks where a hue would be misleading.

## Palette constants

The bundled palette used by `Md3DarkColorScheme` / `Md3LightColorScheme`:

| Dark | Light |
| --- | --- |
| `Md3PurpleDark` `#1E1234` (background) | `Md3LightBackground` `#F7F2FF` |
| `Md3Surface` `#2A1F40` | `Md3LightSurface` `#FBF8FF` |
| `Md3PurplePrimary` `#AB47BC` | `Md3LightPrimary` `#6C4FF5` |
| `Md3Pink` `#F06292` | `Md3LightPrimaryContainer` `#E3DBFF` |
| `Md3Orange` `#FF8A65` | `Md3LightSurfaceVariant` `#E8DEF9` |
| `Md3LightPurple` `#E1BEE7` | `Md3LightOnSurface` `#1E1237` |
| `Md3White`, `Md3Black` | `Md3LightOutline` `#78659A` |

Use them directly when you need a colour that must not follow `MaterialTheme.colorScheme`
(logos, artwork placeholders, gradients).

## Typography

`Md3Typography` is the default type scale and is built on **Google Sans Flex** loaded from
`res/font/gflex_variable.ttf` with the `ROND` axis raised to 100, which gives the rounded
Google Sans Rounded look without shipping a second font file.

```kotlin
Text("Now playing", style = MaterialTheme.typography.headlineSmall)
```

Extras:

* `ExpTitleTypography` — oversized Marquee-style display/title scale (Montserrat, fetched
  through the Google Fonts provider, so it needs Play services on the device).
* `GoogleSansRounded`, `MontserratFamily` — the raw `FontFamily`s, for `Text` overrides.
* Pass your own scale with `Md3Theme(typography = myTypography)`.

Fonts and the `com_google_android_gms_fonts_certs` array are part of the module, nothing to
copy.

## Shapes

```kotlin
Md3Shapes      // small 8dp · medium 16dp · large 24dp — the MaterialTheme.shapes default
ShapeCache     // pre-built absolute smooth-corner shapes
```

`ShapeCache.smooth8`, `smooth10`, `smooth12`, `smooth14`, `smooth16`, `smooth20`, `smooth24`,
`smooth28`, `smooth32`, `smoothPill` — these are singletons because `AbsoluteSmoothCornerShape`
computes Bézier curves analytically, which is expensive inside list items.

```kotlin
Card(shape = ShapeCache.smooth16) { /* … */ }
```

Override the scale with `Md3Theme(shapes = …)` if you need different radii globally.

## Composition locals

| Local | Default | Meaning |
| --- | --- | --- |
| `LocalMd3DarkTheme` | `false` | `true` inside a dark `Md3Theme`, for places that cannot read `colorScheme`. |
| `LocalMd3ShowScrollbar` | `true` | Whether scrollbars should be drawn; set it once at the root. |

```kotlin
CompositionLocalProvider(LocalMd3ShowScrollbar provides false) {
    Md3Theme { App() }
}
```

## System bars

`Md3Theme` calls `Md3StatusBarStyle` for you. Call it directly if you need specific colours:

```kotlin
Md3StatusBarStyle(
    color = MaterialTheme.colorScheme.surface,
    useDarkIcons = true,
    navigationColor = MaterialTheme.colorScheme.surface
)
```

It makes both bars transparent, disables the Android 10+ contrast scrims and sets the icon
brightness from the supplied colours (or lets you pass `useDarkIcons` /
`useDarkNavigationIcons` explicitly). It needs an `Activity` in the context chain and does
nothing in previews or non-activity hosts.

## API reference

```kotlin
// Theme
@Composable fun Md3Theme(darkTheme, colorScheme, colorSchemePair, dynamicColor, typography, shapes, styleSystemBars, content)
@Composable fun Md3StatusBarStyle(color, useDarkIcons, navigationColor, useDarkNavigationIcons)
val Md3DarkColorScheme: ColorScheme
val Md3LightColorScheme: ColorScheme

// Colour
data class ColorSchemePair(val light: ColorScheme, val dark: ColorScheme)
fun extractSeedColor(bitmap: Bitmap, config: ColorExtractionConfig = ColorExtractionConfig()): Color
fun generateColorSchemeFromSeed(seedColor: Color, paletteStyle: Md3PaletteStyle = Md3PaletteStyle.default): ColorSchemePair
fun generateMonochromeColorSchemeFromSeed(seedColor: Color): ColorSchemePair
fun clearExtractedColorCache()
data class ColorExtractionConfig(downscaleMaxDimension, quantizerMaxColors, scoring, accuracyLevel) { val normalizedAccuracy: Double }
data class ColorScoringConfig(targetChroma, weightProportion, weightChromaAbove, weightChromaBelow, cutoffChroma, cutoffExcitedProportion, maxColorCount, maxHueDifference, minHueDifference)
enum class Md3PaletteStyle(storageKey, label) { TONAL_SPOT, VIBRANT, EXPRESSIVE, FRUIT_SALAD }
object Md3ColorAccuracy { MIN, MAX, DEFAULT, STEPS, clamp(Int) }

// Type & shape
val Md3Typography: Typography
val ExpTitleTypography: Typography
val GoogleSansRounded: FontFamily
val MontserratFamily: FontFamily
val Md3Shapes: Shapes
object ShapeCache { smooth8 … smoothPill }
```

## How PastaPlayer itself uses it

The app keeps only product-specific theming and delegates the rest:

* `PastaPlayer/ui/theme/PastaPlayTheme.kt` — thin wrapper that resolves the album-art palette
  (or the Settings preset) and calls `Md3Theme(colorSchemePair = …)`.
* `PastaPlayer/ui/theme/AppThemes.kt` — the Settings presets (dynamic, lavender, ocean, …) and
  the custom-colour theme builder.
* `PastaPlayer/ui/theme/GenreColors.kt` — genre → colour mapping for library shelves.

That is the intended split: **the library owns the design system, the app owns its identity.**

## License

Same as the rest of the repository — see [LICENSE](../LICENSE).
