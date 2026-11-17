package rigatoni534.apps.PastaPlayer.presentation.viewmodel

import android.content.Context
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class DeviceLibraryMetrics(
    val songCount: Int = 0,
    val storageUsedBytes: Long = 0L,
    val isLoading: Boolean = true
)

/**
 * Reads the on-device music library (MediaStore) so the home screen can show
 * how many songs are stored locally and how much space they take up.
 */
@HiltViewModel
class HomeMetricsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _metrics = MutableStateFlow(DeviceLibraryMetrics())
    val metrics: StateFlow<DeviceLibraryMetrics> = _metrics.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { queryDeviceAudio() }
            }.onSuccess { (songCount, storageUsedBytes) ->
                _metrics.value = DeviceLibraryMetrics(
                    songCount = songCount,
                    storageUsedBytes = storageUsedBytes,
                    isLoading = false
                )
            }.onFailure { throwable ->
                Timber.e(throwable, "Failed to read device audio metrics")
                _metrics.value = _metrics.value.copy(isLoading = false)
            }
        }
    }

    private fun queryDeviceAudio(): Pair<Int, Long> {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.SIZE
        )
        var songCount = 0
        var storageUsedBytes = 0L

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null
        )?.use { cursor ->
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            while (cursor.moveToNext()) {
                songCount++
                storageUsedBytes += cursor.getLong(sizeColumn)
            }
        }

        return songCount to storageUsedBytes
    }
}
