package rigatoni534.apps.PastaPlayer.presentation.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import rigatoni534.apps.PastaPlayer.R
import rigatoni534.apps.PastaPlayer.data.discord.RigakizzyStatus
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzyPreset
import rigatoni534.apps.PastaPlayer.data.preferences.RigakizzySettings
import rigatoni534.apps.PastaPlayer.presentation.viewmodel.RigakizzyViewModel

/**
 * Settings → Rigakizzy: Discord login, presence format, application ID and the
 * "stop for session" control.
 */
@Composable
fun RigakizzySettingsSection(
    viewModel: RigakizzyViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val loginState by viewModel.loginState.collectAsStateWithLifecycle()
    var showLogin by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.settings_category_rigakizzy_title),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
        )

        WarningCard(
            title = stringResource(R.string.rigakizzy_warning_title),
            body = stringResource(R.string.rigakizzy_warning_body)
        )

        ActionSettingsItem(
            title = stringResource(R.string.rigakizzy_account_title),
            subtitle = if (settings.isLoggedIn) {
                loginState.username?.let { stringResource(R.string.rigakizzy_logged_in_as, it) }
                    ?: stringResource(R.string.rigakizzy_login_connected_subtitle)
            } else {
                stringResource(R.string.rigakizzy_login_subtitle)
            },
            icon = {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            },
            primaryActionLabel = if (settings.isLoggedIn) {
                stringResource(R.string.rigakizzy_logout_button)
            } else {
                stringResource(R.string.rigakizzy_login_button)
            },
            onPrimaryAction = {
                if (settings.isLoggedIn) {
                    viewModel.logout()
                } else {
                    viewModel.clearLoginError()
                    showLogin = true
                }
            },
            enabled = !loginState.inProgress
        )

        if (loginState.inProgress) {
            StatusSurface(text = stringResource(R.string.rigakizzy_login_validating))
        }
        if (loginState.error != null) {
            StatusSurface(
                text = stringResource(R.string.rigakizzy_login_failed),
                isError = true
            )
        }

        SwitchSettingItem(
            title = stringResource(R.string.rigakizzy_enable_title),
            subtitle = if (settings.isLoggedIn) {
                stringResource(R.string.rigakizzy_enable_subtitle)
            } else {
                stringResource(R.string.rigakizzy_enable_requires_login)
            },
            checked = settings.enabled,
            onCheckedChange = { enabled ->
                if (enabled && !settings.isLoggedIn) {
                    viewModel.clearLoginError()
                    showLogin = true
                } else {
                    viewModel.setEnabled(enabled)
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        StatusSurface(text = statusLabel(status))

        ThemeSelectorItem(
            label = stringResource(R.string.rigakizzy_preset_title),
            description = stringResource(R.string.rigakizzy_preset_subtitle),
            options = mapOf(
                RigakizzyPreset.PLAYER_TITLE.storageKey to stringResource(R.string.rigakizzy_preset_player_title),
                RigakizzyPreset.ARTIST_TITLE.storageKey to stringResource(R.string.rigakizzy_preset_artist_title),
                RigakizzyPreset.SONG_TITLE.storageKey to stringResource(R.string.rigakizzy_preset_song_title),
                RigakizzyPreset.CUSTOM.storageKey to stringResource(R.string.rigakizzy_preset_custom),
            ),
            selectedKey = settings.preset.storageKey,
            onSelectionChanged = { key -> viewModel.setPreset(RigakizzyPreset.fromStorageKey(key)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        if (settings.preset == RigakizzyPreset.CUSTOM) {
            CustomPresenceEditor(
                settings = settings,
                onSave = viewModel::setCustomTemplates
            )
        }

        EditableFieldCard(
            title = stringResource(R.string.rigakizzy_application_id_title),
            subtitle = stringResource(R.string.rigakizzy_application_id_subtitle),
            initialValue = settings.applicationId,
            placeholder = RigakizzySettings.DEFAULT_APPLICATION_ID,
            onSave = viewModel::setApplicationId,
            onReset = viewModel::resetApplicationId
        )

        ActionSettingsItem(
            title = stringResource(R.string.rigakizzy_stop_title),
            subtitle = stringResource(R.string.rigakizzy_stop_subtitle),
            icon = { Icon(imageVector = Icons.Rounded.Close, contentDescription = null) },
            primaryActionLabel = if (settings.stoppedForSession) {
                stringResource(R.string.rigakizzy_resume_button)
            } else {
                stringResource(R.string.rigakizzy_stop_button)
            },
            onPrimaryAction = {
                if (settings.stoppedForSession) viewModel.resumeAfterStop() else viewModel.stopForSession()
            },
            enabled = settings.isLoggedIn && settings.enabled
        )

        Text(
            text = stringResource(R.string.rigakizzy_image_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        AssetKeysEditor(
            settings = settings,
            onSave = viewModel::setAssetKeys
        )
    }

    if (showLogin) {
        DiscordLoginDialog(
            onDismiss = { showLogin = false },
            onToken = { token ->
                showLogin = false
                viewModel.loginWithToken(token)
            }
        )
    }
}

@Composable
private fun statusLabel(status: RigakizzyStatus): String = when (status) {
    RigakizzyStatus.DISABLED -> stringResource(R.string.rigakizzy_status_disabled)
    RigakizzyStatus.NOT_LOGGED_IN -> stringResource(R.string.rigakizzy_status_not_logged_in)
    RigakizzyStatus.STOPPED -> stringResource(R.string.rigakizzy_status_stopped)
    RigakizzyStatus.IDLE -> stringResource(R.string.rigakizzy_status_idle)
    RigakizzyStatus.CONNECTING -> stringResource(R.string.rigakizzy_status_connecting)
    RigakizzyStatus.CONNECTED -> stringResource(R.string.rigakizzy_status_connected)
}

@Composable
fun WarningCard(title: String, body: String) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun StatusSurface(text: String, isError: Boolean = false) {
    Surface(
        color = if (isError) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun CustomPresenceEditor(
    settings: RigakizzySettings,
    onSave: (String, String, String) -> Unit,
) {
    var name by remember(settings.customName) { mutableStateOf(settings.customName) }
    var details by remember(settings.customDetails) { mutableStateOf(settings.customDetails) }
    var state by remember(settings.customState) { mutableStateOf(settings.customState) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.rigakizzy_custom_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.rigakizzy_custom_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.rigakizzy_custom_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                label = { Text(stringResource(R.string.rigakizzy_custom_details)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = state,
                onValueChange = { state = it },
                label = { Text(stringResource(R.string.rigakizzy_custom_state)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = { onSave(name, details, state) },
                enabled = name != settings.customName ||
                    details != settings.customDetails ||
                    state != settings.customState
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}

@Composable
private fun EditableFieldCard(
    title: String,
    subtitle: String,
    initialValue: String,
    placeholder: String,
    onSave: (String) -> Unit,
    onReset: (() -> Unit)? = null,
) {
    var value by remember(initialValue) { mutableStateOf(initialValue) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onSave(value) },
                    enabled = value != initialValue && value.isNotBlank()
                ) {
                    Text(stringResource(R.string.common_save))
                }
                if (onReset != null) {
                    TextButton(onClick = { onReset() }) {
                        Text(stringResource(R.string.rigakizzy_reset_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetKeysEditor(
    settings: RigakizzySettings,
    onSave: (String, String) -> Unit,
) {
    var playingKey by remember(settings.playingAssetKey) { mutableStateOf(settings.playingAssetKey) }
    var pausedKey by remember(settings.pausedAssetKey) { mutableStateOf(settings.pausedAssetKey) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.rigakizzy_assets_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(R.string.rigakizzy_assets_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = playingKey,
                onValueChange = { playingKey = it },
                label = { Text(stringResource(R.string.rigakizzy_playing_key)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = pausedKey,
                onValueChange = { pausedKey = it },
                label = { Text(stringResource(R.string.rigakizzy_paused_key)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(
                onClick = { onSave(playingKey, pausedKey) },
                enabled = playingKey != settings.playingAssetKey || pausedKey != settings.pausedAssetKey
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}

/**
 * Discord login WebView, ported from Kizzy/Rigakizzy's `DiscordLoginWebView`.
 * The user signs in normally; PastaPlayer reads the resulting session token from the
 * page's localStorage and stores it locally.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DiscordLoginDialog(
    onDismiss: () -> Unit,
    onToken: (String) -> Unit,
) {
    var webView by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.rigakizzy_login_dialog_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.rigakizzy_login_dialog_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.rigakizzy_close)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    Text(
                        text = stringResource(R.string.rigakizzy_login_dialog_waiting),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            addJavascriptInterface(
                                object {
                                    @JavascriptInterface
                                    fun onToken(token: String) {
                                        Handler(Looper.getMainLooper()).post { onToken(token) }
                                    }
                                },
                                "PastaPlayerDiscord"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView, url: String?) {
                                    super.onPageFinished(view, url)
                                    view.evaluateJavascript(TOKEN_SNIPPET, null)
                                }

                                override fun doUpdateVisitedHistory(
                                    view: WebView,
                                    url: String?,
                                    isReload: Boolean,
                                ) {
                                    super.doUpdateVisitedHistory(view, url, isReload)
                                    view.evaluateJavascript(TOKEN_SNIPPET, null)
                                }
                            }
                            loadUrl("https://discord.com/login")
                            webView = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp)
                )
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(stringResource(R.string.rigakizzy_close))
                }
            }
        }
    }

    LaunchedEffect(webView) {
        val view = webView ?: return@LaunchedEffect
        repeat(40) {
            delay(1_500)
            view.evaluateJavascript(TOKEN_SNIPPET, null)
        }
    }
}

private const val TOKEN_SNIPPET = """
(function() {
  try {
    var token = null;
    try { token = window.localStorage.getItem('token'); } catch (e) {}
    if (!token) {
      try {
        var frame = document.createElement('iframe');
        frame.style.display = 'none';
        document.body.appendChild(frame);
        token = frame.contentWindow.localStorage.getItem('token');
        document.body.removeChild(frame);
      } catch (e) {}
    }
    if (token) {
      token = token.replace(/^"|"${'$'}/g, '');
      if (token.length > 20) { PastaPlayerDiscord.onToken(token); }
    }
  } catch (e) {}
})();
"""
