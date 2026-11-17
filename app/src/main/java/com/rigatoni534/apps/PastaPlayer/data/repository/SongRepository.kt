package rigatoni534.apps.PastaPlayer.data.repository

import androidx.paging.PagingData
import rigatoni534.apps.PastaPlayer.data.model.Song
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

interface SongRepository {
    fun getSongs(): Flow<List<Song>>
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
    fun getSongsByArtist(artistId: Long): Flow<List<Song>>
    suspend fun searchSongs(query: String): List<Song>
    fun getSongById(songId: Long): Flow<Song?>
    fun getPaginatedSongs(sortOption: rigatoni534.apps.PastaPlayer.data.model.SortOption, storageFilter: rigatoni534.apps.PastaPlayer.data.model.StorageFilter): Flow<PagingData<Song>>
    @OptIn(ExperimentalCoroutinesApi::class)
    fun getPaginatedSongs(): Flow<PagingData<Song>>
    fun getPaginatedFavoriteSongs(
        sortOption: rigatoni534.apps.PastaPlayer.data.model.SortOption,
        storageFilter: rigatoni534.apps.PastaPlayer.data.model.StorageFilter
    ): Flow<PagingData<Song>>
    suspend fun getFavoriteSongsOnce(
        storageFilter: rigatoni534.apps.PastaPlayer.data.model.StorageFilter
    ): List<Song>
    fun getFavoriteSongCountFlow(
        storageFilter: rigatoni534.apps.PastaPlayer.data.model.StorageFilter
    ): Flow<Int>
}
