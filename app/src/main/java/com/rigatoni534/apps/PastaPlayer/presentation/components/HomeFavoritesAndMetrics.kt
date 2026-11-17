package rigatoni534.apps.PastaPlayer.presentation.components

import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import rigatoni534.apps.PastaPlayer.R
import rigatoni534.apps.PastaPlayer.data.model.Playlist
import rigatoni534.apps.PastaPlayer.data.model.Song
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape

private val HomeCardShape = AbsoluteSmoothCornerShape(28.dp, 60)
private val HomeTileShape = AbsoluteSmoothCornerShape(22.dp, 60)

private data class HomeMetric(
    @StringRes val labelRes: Int,
    val value: String,
    val icon: ImageVector
)

/**
 * Compact device metrics for the top of the home screen: local library size,
 * storage consumed by it, plus the user's own playlist and favorite counts.
 */
@Composable
fun HomeMetricsRow(
    songsOnDevice: Int,
    storageUsedBytes: Long,
    playlistCount: Int,
    favoriteSongCount: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val storageText = remember(storageUsedBytes) {
        if (storageUsedBytes <= 0L) {
            "—"
        } else {
            Formatter.formatShortFileSize(context, storageUsedBytes)
        }
    }
    val metrics = listOf(
        HomeMetric(R.string.home_metrics_songs_on_device, songsOnDevice.toString(), Icons.Rounded.MusicNote),
        HomeMetric(R.string.home_metrics_storage_used, storageText, Icons.Rounded.Storage),
        HomeMetric(R.string.home_metrics_playlists, playlistCount.toString(), Icons.Rounded.QueueMusic),
        HomeMetric(R.string.home_metrics_favorites, favoriteSongCount.toString(), Icons.Rounded.Favorite)
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        metrics.chunked(2).forEach { rowMetrics ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowMetrics.forEach { metric ->
                    HomeMetricTile(metric = metric, modifier = Modifier.weight(1f))
                }
                if (rowMetrics.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun HomeMetricTile(
    metric: HomeMetric,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = HomeTileShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Icon(
                    imageVector = metric.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .padding(7.dp)
                        .size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = metric.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(metric.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Favorite playlists and favorite songs, shown at the top of the home screen.
 * Falls back to a short hint when the user has not favorited anything yet.
 */
@Composable
fun HomeFavoritesSection(
    favoritePlaylists: List<Playlist>,
    favoriteSongs: List<Song>,
    onPlaylistClick: (Playlist) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
    maxSongs: Int = 6
) {
    val songs = remember(favoriteSongs, maxSongs) {
        favoriteSongs.sortedBy { it.title.lowercase() }.take(maxSongs)
    }

    Surface(
        modifier = modifier,
        shape = HomeCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_favorites_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.home_favorites_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (songs.isNotEmpty()) {
                    FilledTonalIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        onClick = onShuffle
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = stringResource(R.string.home_favorites_shuffle)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = onPlayAll
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.home_favorites_play_all)
                        )
                    }
                }
            }

            if (favoritePlaylists.isEmpty() && songs.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_favorites_empty_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                return@Column
            }

            if (favoritePlaylists.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_favorites_playlists_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = favoritePlaylists, key = { it.id }) { playlist ->
                        HomeFavoritePlaylistCard(
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist) }
                        )
                    }
                }
            }

            if (songs.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.home_favorites_songs_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                )
                songs.forEach { song ->
                    HomeFavoriteSongRow(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeFavoritePlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(104.dp)
            .clip(HomeTileShape)
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        PlaylistCover(
            playlist = playlist,
            playlistSongs = emptyList(),
            size = 92.dp
        )
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = stringResource(
                if (playlist.songIds.size == 1) R.string.song_count_singular else R.string.song_count_plural,
                playlist.songIds.size
            ),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HomeFavoriteSongRow(
    song: Song,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmartImage(
            model = song.albumArtUriString,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            shape = AbsoluteSmoothCornerShape(12.dp, 60)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.displayArtist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            imageVector = Icons.Rounded.Favorite,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
    }
}
