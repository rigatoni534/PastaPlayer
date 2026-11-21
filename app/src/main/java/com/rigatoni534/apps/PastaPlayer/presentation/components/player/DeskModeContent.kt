package rigatoni534.apps.PastaPlayer.presentation.components.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import rigatoni534.apps.PastaPlayer.R
import rigatoni534.apps.PastaPlayer.data.model.Lyrics
import rigatoni534.apps.PastaPlayer.data.model.Song
import rigatoni534.apps.PastaPlayer.presentation.components.OptimizedAlbumArt
import rigatoni534.apps.PastaPlayer.presentation.components.SafeOriginalAlbumArtSize
import rigatoni534.apps.PastaPlayer.utils.formatDuration

/** Unwraps the hosting Activity so the player can request a landscape orientation. */
internal fun Context.findActivity(): Activity? {
    var current: Context = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

/**
 * Desk mode: a landscape, lyrics-first view of the current song.
 *
 * The artwork fills the screen as the backdrop, half of it stays readable for the lyrics and
 * the only controls are a pause button over the artwork and a seek bar along the bottom.
 */
@Composable
internal fun DeskModeContent(
    song: Song,
    lyricsProvider: () -> Lyrics?,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
    isPlayingProvider: () -> Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var position by remember(song.id) { mutableLongStateOf(positionProvider()) }
    var duration by remember(song.id) { mutableLongStateOf(durationProvider().coerceAtLeast(0L)) }
    var isPlaying by remember(song.id) { mutableStateOf(isPlayingProvider()) }
    var dragPosition by remember { mutableStateOf<Float?>(null) }

    // Poll the player instead of collecting the high-frequency position flow: this screen
    // only needs line-level lyric timing, not frame-level progress.
    LaunchedEffect(song.id) {
        while (true) {
            position = positionProvider()
            duration = durationProvider().coerceAtLeast(0L)
            isPlaying = isPlayingProvider()
            delay(200)
        }
    }

    val progress = if (duration > 0L) {
        (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val sliderValue = dragPosition ?: progress

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        OptimizedAlbumArt(
            uri = song.albumArtUriString,
            title = song.title,
            modifier = Modifier.fillMaxSize(),
            targetSize = SafeOriginalAlbumArtSize
        )

        // Darken the left half so the lyrics stay legible over any artwork, and fade the
        // bottom so the seek bar does not sit on top of a busy edge.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.88f),
                            0.55f to Color.Black.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.25f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.25f),
                            0.6f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 40.dp, top = 28.dp, bottom = 104.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DeskLyricsColumn(
                lyrics = lyricsProvider(),
                position = position,
                duration = duration,
                modifier = Modifier.weight(0.62f)
            )
            Spacer(modifier = Modifier.width(24.dp))
            Column(
                modifier = Modifier
                    .weight(0.38f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.displayArtist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 16.dp)
        ) {
            Slider(
                value = sliderValue,
                onValueChange = { dragPosition = it },
                onValueChangeFinished = {
                    dragPosition?.let { fraction ->
                        if (duration > 0L) onSeek((fraction * duration).toLong())
                    }
                    dragPosition = null
                },
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val shownPosition = dragPosition
                Text(
                    text = formatDuration(
                        if (shownPosition != null) (shownPosition * duration).toLong() else position
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        // Desk mode hides every control, but there still has to be a way back out.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable { onExit() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.rounded_keyboard_arrow_down_24),
                contentDescription = stringResource(R.string.player_cd_collapse),
                tint = Color.White
            )
        }
    }
}

@Composable
private fun DeskLyricsColumn(
    lyrics: Lyrics?,
    position: Long,
    duration: Long,
    modifier: Modifier = Modifier
) {
    val synced = lyrics?.synced.orEmpty()
    val plainLines = lyrics?.plain.orEmpty().filter { it.isNotBlank() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            synced.isNotEmpty() -> {
                val activeIndex = synced.indexOfLast { it.time <= position }.coerceAtLeast(0)
                for (offset in -2..2) {
                    val line = synced.getOrNull(activeIndex + offset) ?: continue
                    DeskLyricLine(text = line.line, isActive = offset == 0)
                }
            }

            plainLines.isNotEmpty() -> {
                // Plain lyrics have no timing, so scroll them proportionally through the song.
                val fraction = if (duration > 0L) {
                    (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
                } else {
                    0f
                }
                val activeIndex = (fraction * (plainLines.size - 1)).toInt()
                    .coerceIn(0, plainLines.lastIndex)
                for (offset in -2..2) {
                    val line = plainLines.getOrNull(activeIndex + offset) ?: continue
                    DeskLyricLine(text = line, isActive = offset == 0)
                }
            }

            else -> {
                Text(
                    text = stringResource(R.string.desk_mode_no_lyrics),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DeskLyricLine(text: String, isActive: Boolean) {
    Text(
        text = text,
        style = if (isActive) {
            MaterialTheme.typography.headlineSmall
        } else {
            MaterialTheme.typography.titleMedium
        },
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = Color.White.copy(alpha = if (isActive) 1f else 0.45f),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(vertical = 3.dp)
    )
}
