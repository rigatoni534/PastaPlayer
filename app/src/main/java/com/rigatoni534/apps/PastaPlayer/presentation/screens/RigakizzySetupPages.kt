package rigatoni534.apps.PastaPlayer.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rigatoni534.apps.PastaPlayer.R
import rigatoni534.apps.PastaPlayer.presentation.viewmodel.RigakizzyViewModel
import rigatoni534.apps.PastaPlayer.presentation.viewmodel.SongVaultViewModel

/**
 * First-run setup step for Rigakizzy: explains the feature, warns about the account risk,
 * and lets the user log in to Discord before the option becomes available.
 */
@Composable
fun RigakizzySetupPage(
    viewModel: RigakizzyViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.rigakizzy_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.rigakizzy_setup_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.rigakizzy_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        WarningCard(
            title = stringResource(R.string.rigakizzy_warning_title),
            body = stringResource(R.string.rigakizzy_warning_body)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = acknowledged,
                onCheckedChange = { acknowledged = it }
            )
            Text(
                text = stringResource(R.string.rigakizzy_setup_ack),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (settings.isLoggedIn) {
            Text(
                text = stringResource(R.string.rigakizzy_setup_connected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Button(
                onClick = { showLogin = true },
                enabled = acknowledged,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.rigakizzy_setup_login))
            }
        }

        Text(
            text = stringResource(R.string.rigakizzy_setup_later),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.setup_extras_themes_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
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

/** First-run setup step for Song Vault. */
@Composable
fun VaultSetupPage(
    viewModel: SongVaultViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.vault_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = stringResource(R.string.vault_setup_subtitle),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.vault_setup_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        WarningCard(
            title = stringResource(R.string.song_vault_experimental_title),
            body = stringResource(R.string.song_vault_experimental_settings_body)
        )

        SwitchSettingItem(
            title = stringResource(R.string.song_vault_enable_title),
            subtitle = stringResource(R.string.song_vault_enable_description),
            checked = uiState.enabled,
            onCheckedChange = viewModel::setEnabled
        )
    }
}
