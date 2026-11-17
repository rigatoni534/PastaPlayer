package rigatoni534.apps.PastaPlayer.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import rigatoni534.apps.PastaPlayer.R
import rigatoni534.apps.PastaPlayer.data.preferences.AppThemeMode
import rigatoni534.apps.PastaPlayer.data.preferences.AppThemePreset
import rigatoni534.apps.PastaPlayer.data.preferences.CustomThemeColorRole
import rigatoni534.apps.PastaPlayer.data.preferences.CustomThemeColors
import rigatoni534.apps.PastaPlayer.ui.theme.AppThemes

/**
 * Settings > Themes: dynamic Material You with light/dark, ready-made Material You
 * palettes, and a fully custom palette with an RGB/hex color picker per role.
 */
@Composable
fun ThemesSettingsSection(
    appThemeMode: String,
    onThemeModeSelected: (String) -> Unit,
    selectedPreset: AppThemePreset,
    onPresetSelected: (AppThemePreset) -> Unit,
    customColors: CustomThemeColors,
    onCustomColorChanged: (CustomThemeColorRole, Int) -> Unit
) {
    var editingRole by remember { mutableStateOf<CustomThemeColorRole?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeSectionHeader(stringResource(R.string.settings_themes_default_section))

        ThemeChoiceCard(
            title = stringResource(R.string.settings_themes_dynamic_title),
            subtitle = stringResource(R.string.settings_themes_dynamic_subtitle),
            swatches = AppThemes.previewColors(AppThemePreset.DYNAMIC, customColors),
            selected = selectedPreset == AppThemePreset.DYNAMIC,
            onClick = { onPresetSelected(AppThemePreset.DYNAMIC) }
        )

        ThemeSelectorItem(
            label = stringResource(R.string.settings_app_theme_title),
            description = stringResource(R.string.settings_app_theme_subtitle),
            options = mapOf(
                AppThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                AppThemeMode.DARK to stringResource(R.string.settings_theme_dark),
                AppThemeMode.FOLLOW_SYSTEM to stringResource(R.string.settings_theme_follow_system)
            ),
            selectedKey = appThemeMode,
            onSelectionChanged = onThemeModeSelected,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        Spacer(Modifier.height(8.dp))
        ThemeSectionHeader(stringResource(R.string.settings_themes_presets_section))
        Text(
            text = stringResource(R.string.settings_themes_presets_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
        )

        AppThemePreset.palettes.chunked(2).forEach { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowPresets.forEach { preset ->
                    ThemeChoiceCard(
                        title = stringResource(preset.labelRes),
                        subtitle = null,
                        swatches = AppThemes.previewColors(preset, customColors),
                        selected = selectedPreset == preset,
                        onClick = { onPresetSelected(preset) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPresets.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        ThemeSectionHeader(stringResource(R.string.settings_themes_custom_section))
        Text(
            text = stringResource(R.string.settings_themes_custom_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 4.dp)
        )

        ThemeChoiceCard(
            title = stringResource(R.string.settings_themes_custom_enable),
            subtitle = stringResource(R.string.settings_themes_custom_enable_subtitle),
            swatches = AppThemes.previewColors(AppThemePreset.CUSTOM, customColors),
            selected = selectedPreset == AppThemePreset.CUSTOM,
            onClick = { onPresetSelected(AppThemePreset.CUSTOM) }
        )

        CustomThemeColorRole.entries.forEach { role ->
            CustomThemeColorRow(
                role = role,
                argb = customColors.colorFor(role),
                onClick = { editingRole = role }
            )
        }
    }

    editingRole?.let { role ->
        CustomThemeColorDialog(
            role = role,
            initialColor = customColors.colorFor(role),
            onDismiss = { editingRole = null },
            onConfirm = { argb ->
                onCustomColorChanged(role, argb)
                editingRole = null
            }
        )
    }
}

@Composable
private fun ThemeSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun ThemeChoiceCard(
    title: String,
    subtitle: String?,
    swatches: List<Color>,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    swatches.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                }
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.settings_themes_selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CustomThemeColorRow(
    role: CustomThemeColorRole,
    argb: Int,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(10.dp)
    val roleLabel = stringResource(role.labelRes)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = roleLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatHexColor(argb),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Rounded.Edit,
                contentDescription = stringResource(R.string.settings_themes_edit_color, roleLabel),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun CustomThemeColorDialog(
    role: CustomThemeColorRole,
    initialColor: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var red by remember(initialColor) { mutableIntStateOf((initialColor shr 16) and 0xFF) }
    var green by remember(initialColor) { mutableIntStateOf((initialColor shr 8) and 0xFF) }
    var blue by remember(initialColor) { mutableIntStateOf(initialColor and 0xFF) }
    var hexInput by remember(initialColor) { mutableStateOf(formatHexColor(initialColor)) }

    fun updateChannels(newRed: Int, newGreen: Int, newBlue: Int) {
        red = newRed.coerceIn(0, 255)
        green = newGreen.coerceIn(0, 255)
        blue = newBlue.coerceIn(0, 255)
        hexInput = formatHexColor(argbOf(red, green, blue))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_themes_color_dialog_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(role.labelRes),
                    style = MaterialTheme.typography.titleSmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(argbOf(red, green, blue)))
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { raw ->
                        val sanitized = raw
                            .uppercase()
                            .filter { character ->
                                character.isDigit() || character in 'A'..'F' || character == '#'
                            }
                            .take(7)
                        hexInput = sanitized
                        parseHexColor(sanitized)?.let { parsed ->
                            red = (parsed shr 16) and 0xFF
                            green = (parsed shr 8) and 0xFF
                            blue = parsed and 0xFF
                        }
                    },
                    label = { Text(stringResource(R.string.settings_themes_hex_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth()
                )
                ColorChannelRow(label = "R", value = red) { updateChannels(it, green, blue) }
                ColorChannelRow(label = "G", value = green) { updateChannels(red, it, blue) }
                ColorChannelRow(label = "B", value = blue) { updateChannels(red, green, it) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(argbOf(red, green, blue)) }) {
                Text(stringResource(R.string.common_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        }
    )
}

@Composable
private fun ColorChannelRow(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(16.dp)
        )
        OutlinedTextField(
            value = value.toString(),
            onValueChange = { text ->
                text.filter { it.isDigit() }
                    .take(3)
                    .toIntOrNull()
                    ?.takeIf { it in 0..255 }
                    ?.let(onValueChange)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(88.dp)
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun argbOf(red: Int, green: Int, blue: Int): Int =
    (0xFF shl 24) or (red shl 16) or (green shl 8) or blue

private fun formatHexColor(argb: Int): String = "#%06X".format(argb and 0xFFFFFF)

private fun parseHexColor(input: String): Int? {
    val hex = input.removePrefix("#")
    if (hex.length != 6) return null
    val value = hex.toIntOrNull(16) ?: return null
    return (0xFF shl 24) or value
}
