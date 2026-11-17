package rigatoni534.apps.PastaPlayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rigatoni534.apps.PastaPlayer.data.preferences.UserPreferencesRepository
import rigatoni534.apps.PastaPlayer.data.vault.SongVaultManager

@HiltViewModel
class SongVaultViewModel @Inject constructor(
    private val songVaultManager: SongVaultManager,
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    data class VaultedSongUi(
        val id: String,
        val title: String,
        val artist: String,
        val sizeLabel: String,
    )

    data class UiState(
        val enabled: Boolean = true,
        val isLoading: Boolean = true,
        val songCount: Int = 0,
        val totalSizeLabel: String = "",
        val songs: List<VaultedSongUi> = emptyList(),
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.songVaultEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(enabled = enabled) }
            }
        }
        refresh()
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSongVaultEnabled(enabled)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val summary = songVaultManager.getSummary()
            val songs = songVaultManager.getVaultedSongs()
            _uiState.update { state ->
                state.copy(
                    isLoading = false,
                    songCount = summary.songCount,
                    totalSizeLabel = formatBytes(summary.totalBytes),
                    songs = songs.map { song ->
                        VaultedSongUi(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            sizeLabel = formatBytes(song.bytes),
                        )
                    },
                )
            }
        }
    }

    fun restoreSong(songId: String, onResult: (SongVaultManager.VaultResult) -> Unit) {
        viewModelScope.launch {
            val result = songVaultManager.restoreSong(songId)
            refresh()
            onResult(result)
        }
    }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 MB"
            val kilo = 1024.0
            val mega = kilo * 1024.0
            val giga = mega * 1024.0
            return when {
                bytes >= giga -> String.format(Locale.US, "%.1f GB", bytes / giga)
                bytes >= mega -> String.format(Locale.US, "%.1f MB", bytes / mega)
                bytes >= kilo -> String.format(Locale.US, "%.0f KB", bytes / kilo)
                else -> "$bytes B"
            }
        }
    }
}
