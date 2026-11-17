package rigatoni534.apps.PastaPlayer.data.provider

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SharedArtworkContentProviderTest {

    @Test
    fun buildSongUri_usesDedicatedArtworkAuthority() {
        val uri = SharedArtworkContentProvider.buildSongUriString(
            packageName = "rigatoni534.apps.PastaPlayer",
            songId = 42L
        )

        assertThat(uri).isEqualTo("content://rigatoni534.apps.PastaPlayer.artwork/song/42")
    }

    @Test
    fun buildSongUri_preservesCacheBustToken() {
        val uri = SharedArtworkContentProvider.buildSongUriString(
            packageName = "rigatoni534.apps.PastaPlayer",
            songId = 42L,
            cacheBustToken = "1234"
        )

        assertThat(uri)
            .isEqualTo("content://rigatoni534.apps.PastaPlayer.artwork/song/42?t=1234")
    }

    @Test
    fun parseSongId_rejectsOtherAuthorities() {
        val songId = SharedArtworkContentProvider.parseSongId(
            uriString = "content://example.com.artwork/song/42",
            packageName = "rigatoni534.apps.PastaPlayer"
        )

        assertThat(songId).isNull()
    }

    @Test
    fun parseSongId_readsSharedArtworkSongUri() {
        val songId = SharedArtworkContentProvider.parseSongId(
            uriString = "content://rigatoni534.apps.PastaPlayer.artwork/song/42",
            packageName = "rigatoni534.apps.PastaPlayer"
        )

        assertThat(songId).isEqualTo(42L)
    }
}
