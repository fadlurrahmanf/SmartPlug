package com.smartplug.app.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartplug.app.BuildConfig
import com.smartplug.app.data.local.AppThemeMode
import com.smartplug.app.ui.components.SmartPlugCard
import com.smartplug.app.ui.localization.AppLanguage
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val appResetState by viewModel.appResetState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    var showAppResetConfirmation by remember { mutableStateOf(false) }

    Scaffold(topBar = { TopAppBar(title = { androidx.compose.material3.Text(localized(language, "Pengaturan", "Settings")) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SmartPlugCard {
                Text(localized(language, "Tampilan", "Appearance"), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    AppThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AppThemeMode.entries.size),
                            selected = settings.themeMode == mode,
                            onClick = { viewModel.setThemeMode(mode) },
                        ) {
                            Text(themeModeLabel(mode, language))
                        }
                    }
                }
            }

            SmartPlugCard {
                Text(localized(language, "Bahasa", "Language"), style = MaterialTheme.typography.titleMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    AppLanguage.entries.forEachIndexed { index, item ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = AppLanguage.entries.size),
                            selected = settings.language == item,
                            onClick = { viewModel.setLanguage(item) },
                        ) { Text(if (item == AppLanguage.INDONESIAN) "Bahasa Indonesia" else "English") }
                    }
                }
            }

            SmartPlugCard {
                SettingsToggleRow(
                    label = localized(language, "Suara tap", "Tap sound"),
                    checked = settings.soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
                )
            }

            SmartPlugCard {
                Text(localized(language, "Reset aplikasi", "Reset app"), style = MaterialTheme.typography.titleMedium)
                Text(
                    localized(
                        language,
                        "Factory reset seluruh perangkat yang terdaftar, lalu hapus perangkat, riwayat, nama beban, dan kredensial dari aplikasi.",
                        "Factory-reset every registered device, then remove devices, history, load names, and credentials from the app.",
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                )
                Button(
                    onClick = { showAppResetConfirmation = true },
                    enabled = !appResetState.isRunning,
                ) {
                    Text(if (appResetState.isRunning) localized(language, "Mereset…", "Resetting…") else localized(language, "Reset aplikasi & perangkat", "Reset app & devices"))
                }
                appResetState.summary?.let { summary ->
                    Text(summary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                }
            }

            SmartPlugCard {
                SettingsToggleRow(
                    label = localized(language, "Getaran (haptic)", "Haptic feedback"),
                    checked = settings.hapticEnabled,
                    onCheckedChange = viewModel::setHapticEnabled,
                )
            }

            SmartPlugCard {
                Text(localized(language, "Tentang", "About"), style = MaterialTheme.typography.titleMedium)
                Text(
                    "SmartPlug ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showAppResetConfirmation) {
        AppResetConfirmationDialog(
            language = language,
            onDismiss = { showAppResetConfirmation = false },
            onConfirm = {
                showAppResetConfirmation = false
                viewModel.resetAppAndRegisteredDevices()
            },
        )
    }
}

@Composable
private fun AppResetConfirmationDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    var phrase by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(language, "Reset aplikasi dan semua perangkat?", "Reset app and all devices?")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    localized(
                        language,
                        "Setiap perangkat yang masih dapat dihubungi akan menerima factory reset dengan konfirmasi tiga tahap. Perangkat offline tidak akan dihapus agar dapat dicoba lagi.",
                        "Each reachable device receives a triple-confirmed factory reset. Offline devices remain in the app so they can be retried.",
                    ),
                )
                OutlinedTextField(
                    value = phrase,
                    onValueChange = { phrase = it },
                    label = { Text(localized(language, "Ketik RESET APP", "Type RESET APP")) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = phrase == "RESET APP") {
                Text(localized(language, "Reset semua", "Reset all"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(localized(language, "Batal", "Cancel")) }
        },
    )
}

@Composable
private fun SettingsToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun themeModeLabel(mode: AppThemeMode, language: AppLanguage): String = when (mode) {
    AppThemeMode.SYSTEM -> localized(language, "Sistem", "System")
    AppThemeMode.LIGHT -> localized(language, "Terang", "Light")
    AppThemeMode.DARK -> localized(language, "Gelap", "Dark")
}
