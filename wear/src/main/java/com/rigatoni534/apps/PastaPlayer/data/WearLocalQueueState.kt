package rigatoni534.apps.PastaPlayer.data

import rigatoni534.apps.PastaPlayer.shared.WearLibraryItem

data class WearLocalQueueState(
    val items: List<WearLibraryItem> = emptyList(),
    val currentIndex: Int = -1,
)
