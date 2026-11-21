package rigatoni534.apps.PastaPlayer.data.preferences

/**
 * Which custom action buttons are allowed in the media playback notification.
 *
 * Every button defaults to on, matching how the notification behaved before this was
 * configurable. The previous/play/next transport buttons are always provided by Media3.
 */
data class NotificationButtonPreferences(
    val showLike: Boolean = true,
    val showShuffle: Boolean = true,
    val showRepeat: Boolean = true,
    val showClose: Boolean = true
)

/** Identifies one configurable notification button for the settings UI. */
enum class NotificationButton {
    LIKE,
    SHUFFLE,
    REPEAT,
    CLOSE
}
