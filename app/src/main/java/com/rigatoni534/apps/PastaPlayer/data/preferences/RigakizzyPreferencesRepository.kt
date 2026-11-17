package rigatoni534.apps.PastaPlayer.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** How the Rich Presence lines are arranged. */
enum class RigakizzyPreset(val storageKey: String) {
    /** Title "PastaPlayer" · activity = song title · description = artist. */
    PLAYER_TITLE("player_title"),

    /** Title = artist · activity = song title · description = "PastaPlayer". */
    ARTIST_TITLE("artist_title"),

    /** Title = song title · activity = artist · description = "PastaPlayer". */
    SONG_TITLE("song_title"),

    /** User supplied templates with {song}, {artist}, {album} and {player} placeholders. */
    CUSTOM("custom");

    companion object {
        val default = PLAYER_TITLE
        fun fromStorageKey(key: String?): RigakizzyPreset =
            entries.firstOrNull { it.storageKey == key } ?: default
    }
}

data class RigakizzySettings(
    val enabled: Boolean = false,
    val token: String = "",
    val applicationId: String = DEFAULT_APPLICATION_ID,
    val preset: RigakizzyPreset = RigakizzyPreset.default,
    val customName: String = DEFAULT_CUSTOM_NAME,
    val customDetails: String = DEFAULT_CUSTOM_DETAILS,
    val customState: String = DEFAULT_CUSTOM_STATE,
    val playingAssetKey: String = DEFAULT_PLAYING_ASSET_KEY,
    val pausedAssetKey: String = DEFAULT_PAUSED_ASSET_KEY,
    val stoppedForSession: Boolean = false,
    val noticeAcknowledged: Boolean = false,
) {
    val isLoggedIn: Boolean get() = token.isNotBlank()

    /** Presence can only be published when the user enabled it, logged in and did not stop it. */
    val canPublish: Boolean get() = enabled && isLoggedIn && !stoppedForSession

    companion object {
        /** Application ID used for the default Rigakizzy presence. */
        const val DEFAULT_APPLICATION_ID = "1405290307788214282"
        const val DEFAULT_CUSTOM_NAME = "PastaPlayer"
        const val DEFAULT_CUSTOM_DETAILS = "{song}"
        const val DEFAULT_CUSTOM_STATE = "{artist}"
        const val DEFAULT_PLAYING_ASSET_KEY = "playing"
        const val DEFAULT_PAUSED_ASSET_KEY = "paused"
    }
}

@Singleton
class RigakizzyPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("rigakizzy_enabled_v1")
        val TOKEN = stringPreferencesKey("rigakizzy_discord_token_v1")
        val APPLICATION_ID = stringPreferencesKey("rigakizzy_application_id_v1")
        val PRESET = stringPreferencesKey("rigakizzy_preset_v1")
        val CUSTOM_NAME = stringPreferencesKey("rigakizzy_custom_name_v1")
        val CUSTOM_DETAILS = stringPreferencesKey("rigakizzy_custom_details_v1")
        val CUSTOM_STATE = stringPreferencesKey("rigakizzy_custom_state_v1")
        val PLAYING_ASSET = stringPreferencesKey("rigakizzy_playing_asset_v1")
        val PAUSED_ASSET = stringPreferencesKey("rigakizzy_paused_asset_v1")
        val STOPPED_FOR_SESSION = booleanPreferencesKey("rigakizzy_stopped_for_session_v1")
        val NOTICE_ACKNOWLEDGED = booleanPreferencesKey("rigakizzy_notice_acknowledged_v1")
    }

    val settingsFlow: Flow<RigakizzySettings> = dataStore.data.map { preferences ->
        RigakizzySettings(
            enabled = preferences[Keys.ENABLED] ?: false,
            token = preferences[Keys.TOKEN].orEmpty(),
            applicationId = preferences[Keys.APPLICATION_ID]
                ?.takeIf { it.isNotBlank() }
                ?: RigakizzySettings.DEFAULT_APPLICATION_ID,
            preset = RigakizzyPreset.fromStorageKey(preferences[Keys.PRESET]),
            customName = preferences[Keys.CUSTOM_NAME] ?: RigakizzySettings.DEFAULT_CUSTOM_NAME,
            customDetails = preferences[Keys.CUSTOM_DETAILS] ?: RigakizzySettings.DEFAULT_CUSTOM_DETAILS,
            customState = preferences[Keys.CUSTOM_STATE] ?: RigakizzySettings.DEFAULT_CUSTOM_STATE,
            playingAssetKey = preferences[Keys.PLAYING_ASSET] ?: RigakizzySettings.DEFAULT_PLAYING_ASSET_KEY,
            pausedAssetKey = preferences[Keys.PAUSED_ASSET] ?: RigakizzySettings.DEFAULT_PAUSED_ASSET_KEY,
            stoppedForSession = preferences[Keys.STOPPED_FOR_SESSION] ?: false,
            noticeAcknowledged = preferences[Keys.NOTICE_ACKNOWLEDGED] ?: false,
        )
    }

    suspend fun settingsOnce(): RigakizzySettings = settingsFlow.first()

    suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ENABLED] = enabled
            if (enabled) preferences[Keys.STOPPED_FOR_SESSION] = false
        }
    }

    suspend fun setToken(token: String) {
        dataStore.edit { preferences -> preferences[Keys.TOKEN] = token }
    }

    suspend fun setApplicationId(applicationId: String) {
        dataStore.edit { preferences ->
            preferences[Keys.APPLICATION_ID] = applicationId.trim()
        }
    }

    suspend fun setPreset(preset: RigakizzyPreset) {
        dataStore.edit { preferences -> preferences[Keys.PRESET] = preset.storageKey }
    }

    suspend fun setCustomTemplates(name: String, details: String, state: String) {
        dataStore.edit { preferences ->
            preferences[Keys.CUSTOM_NAME] = name.take(MAX_LINE_LENGTH)
            preferences[Keys.CUSTOM_DETAILS] = details.take(MAX_LINE_LENGTH)
            preferences[Keys.CUSTOM_STATE] = state.take(MAX_LINE_LENGTH)
        }
    }

    suspend fun setAssetKeys(playingKey: String, pausedKey: String) {
        dataStore.edit { preferences ->
            preferences[Keys.PLAYING_ASSET] = playingKey.trim().take(MAX_ASSET_KEY_LENGTH)
            preferences[Keys.PAUSED_ASSET] = pausedKey.trim().take(MAX_ASSET_KEY_LENGTH)
        }
    }

    suspend fun setStoppedForSession(stopped: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.STOPPED_FOR_SESSION] = stopped }
    }

    suspend fun setNoticeAcknowledged(acknowledged: Boolean) {
        dataStore.edit { preferences -> preferences[Keys.NOTICE_ACKNOWLEDGED] = acknowledged }
    }

    suspend fun logout() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.TOKEN)
            preferences[Keys.ENABLED] = false
            preferences[Keys.STOPPED_FOR_SESSION] = false
        }
    }

    companion object {
        const val MAX_LINE_LENGTH = 128
        const val MAX_ASSET_KEY_LENGTH = 64
    }
}
