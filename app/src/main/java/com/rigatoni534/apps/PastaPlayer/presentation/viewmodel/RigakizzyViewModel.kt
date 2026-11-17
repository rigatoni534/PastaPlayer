package rigatoni534.apps.PastaPlayer.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rigatoni534.apps.PastaPlayer.data.discord.DiscordAccountApi
import rigatoni534.apps.PastaPlayer.data.discord.RigakizzyController
import rigatoni534.apps.PastaPlayer.data.discord.RigakizzyStatus
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzyPreferencesRepository
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzyPreset
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzySettings

data class RigakizzyLoginState(
    val inProgress: Boolean = false,
    val error: String? = null,
    val username: String? = null,
)

@HiltViewModel
class RigakizzyViewModel @Inject constructor(
    private val preferencesRepository: RigakizzyPreferencesRepository,
    private val controller: RigakizzyController,
    private val accountApi: DiscordAccountApi,
) : ViewModel() {

    val settings: StateFlow<RigakizzySettings> = preferencesRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RigakizzySettings(),
    )

    val status: StateFlow<RigakizzyStatus> = controller.status

    private val _loginState = MutableStateFlow(RigakizzyLoginState())
    val loginState: StateFlow<RigakizzyLoginState> = _loginState.asStateFlow()

    fun loginWithToken(token: String) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) return
        _loginState.value = RigakizzyLoginState(inProgress = true)
        viewModelScope.launch {
            val username = accountApi.fetchUsername(trimmed)
            if (username == null) {
                _loginState.value = RigakizzyLoginState(error = "token_rejected")
            } else {
                preferencesRepository.setToken(trimmed)
                preferencesRepository.setEnabled(true)
                _loginState.value = RigakizzyLoginState(username = username)
            }
        }
    }

    fun clearLoginError() {
        _loginState.value = _loginState.value.copy(error = null)
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setEnabled(enabled) }
    }

    fun logout() {
        viewModelScope.launch { preferencesRepository.logout() }
    }

    fun setPreset(preset: RigakizzyPreset) {
        viewModelScope.launch { preferencesRepository.setPreset(preset) }
    }

    fun setApplicationId(applicationId: String) {
        viewModelScope.launch { preferencesRepository.setApplicationId(applicationId) }
    }

    fun resetApplicationId() {
        viewModelScope.launch {
            preferencesRepository.setApplicationId(RigakizzySettings.DEFAULT_APPLICATION_ID)
        }
    }

    fun setCustomTemplates(name: String, details: String, state: String) {
        viewModelScope.launch { preferencesRepository.setCustomTemplates(name, details, state) }
    }

    fun setAssetKeys(playingKey: String, pausedKey: String) {
        viewModelScope.launch { preferencesRepository.setAssetKeys(playingKey, pausedKey) }
    }

    fun stopForSession() {
        controller.stopForSession()
    }

    fun resumeAfterStop() {
        controller.resumeAfterStop()
    }

    fun acknowledgeNotice() {
        viewModelScope.launch { preferencesRepository.setNoticeAcknowledged(true) }
    }
}
