package rigatoni534.apps.PastaPlayer.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import rigatoni534.apps.PastaPlayer.R

/**
 * Shown before the first song is moved into the Song Vault: the feature is experimental
 * and moving audio into app-private storage is not guaranteed to keep it playable.
 */
@Composable
fun SongVaultExperimentalDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null
            )
        },
        title = { Text(stringResource(R.string.song_vault_experimental_title)) },
        text = { Text(stringResource(R.string.song_vault_experimental_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.song_vault_experimental_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}
