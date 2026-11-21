package rigatoni534.apps.PastaPlayer.presentation.components

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.size.Size

private const val DefaultBlurSampleSizePx = 256

/**
 * Mirrors Settings → "Disable Blur Effects".
 *
 * Provided once at the activity root so every blurred layer shares a single DataStore
 * read instead of each one subscribing on its own. Defaults to `false` (blur on) so
 * layouts outside that provider keep their previous behaviour.
 */
internal val LocalBlurEffectsDisabled = staticCompositionLocalOf { false }

/**
 * Draws [uri] blurred, then feathers it out with [mask].
 *
 * The layer is composited offscreen so [mask] can be drawn with `BlendMode.DstIn`,
 * which multiplies the blurred artwork by the mask's alpha. That is what lets artwork
 * dissolve into the surrounding surface instead of ending at a hard edge.
 *
 * [Modifier.blur] is backed by RenderEffect and only renders on Android 12+; on older
 * versions it is a silent no-op, so this draws nothing rather than leaving a sharp,
 * doubled copy of the artwork on screen.
 */
@Composable
internal fun BlurredArtworkLayer(
    uri: String?,
    title: String,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 40.dp,
    alpha: Float = 1f,
    scale: Float = 1f,
    sampleSizePx: Int = DefaultBlurSampleSizePx,
    mask: DrawScope.() -> Unit
) {
    // Blurring is the entire point of this composable, so the shared "Disable Blur
    // Effects" value is honoured here instead of at every call site.
    val blurDisabled = LocalBlurEffectsDisabled.current

    if (blurDisabled || uri.isNullOrBlank() || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .drawWithContent {
                drawContent()
                this.mask()
            }
    ) {
        OptimizedAlbumArt(
            uri = uri,
            title = title,
            // A small decode is enough for something that is about to be blurred and
            // it keeps the extra layer cheap next to the full-size album art.
            targetSize = Size(sampleSizePx, sampleSizePx),
            modifier = Modifier
                .fillMaxSize()
                .blur(blurRadius)
        )
    }
}
