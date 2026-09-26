package com.smartplug.app.ui.screens.devicedetail

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartplug.app.domain.model.IntegrationMode
import com.smartplug.app.domain.model.RelayState
import com.smartplug.app.ui.components.FullScreenLoading
import com.smartplug.app.ui.components.MeasurementGrid
import com.smartplug.app.ui.components.LiveMeasurementChart
import com.smartplug.app.ui.components.PollWhileVisible
import com.smartplug.app.ui.components.StatusPill
import com.smartplug.app.ui.components.rememberTapFeedback
import com.smartplug.app.ui.components.relayStatusColor
import com.smartplug.app.ui.components.relayStatusLabel
import com.smartplug.app.ui.components.toTiles
import com.smartplug.app.ui.theme.AccentGreen
import com.smartplug.app.ui.theme.AccentRed
import com.smartplug.app.ui.localization.AppLanguage
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized
import com.smartplug.app.util.PollingCadence
import kotlinx.coroutines.delay

@Composable
fun DeviceDetailScreen(
    deviceId: String,
    onOpenHistory: () -> Unit,
    onOpenSchedule: () -> Unit,
    onBack: () -> Unit,
    onDeviceRemoved: () -> Unit,
    viewModel: DeviceDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    val tapFeedback = rememberTapFeedback()
    var showMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var confirmAction by remember { mutableStateOf<ConfirmAction?>(null) }
    var showTimerDialog by remember { mutableStateOf(false) }
    var showNameLoadDialog by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(1_000) } }

    LaunchedEffect(uiState.isRemoved) {
        if (uiState.isRemoved) onDeviceRemoved()
    }

    val cadence = if (uiState.liveModeRequested) PollingCadence.LIVE else PollingCadence.NORMAL
    PollWhileVisible(intervalMs = cadence.intervalMs, key = uiState.liveModeRequested) {
        viewModel.refresh()
    }

    val device = uiState.device
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(device?.displayName ?: "SmartPlug") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = localized(language, "Kembali", "Back")) }
                },
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Filled.History, contentDescription = localized(language, "Riwayat energi", "Energy history"))
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = localized(language, "Menu", "Menu"))
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(text = { Text(localized(language, "Ganti nama", "Rename")) }, onClick = {
                            showMenu = false
                            showRenameDialog = true
                        })
                        DropdownMenuItem(
                            text = { Text(localized(language, "Timer", "Timer")) },
                            onClick = { showMenu = false; showTimerDialog = true },
                        )
                        DropdownMenuItem(
                            text = { Text(localized(language, "Jadwal", "Schedule")) },
                            onClick = { showMenu = false; onOpenSchedule() },
                        )
                        DropdownMenuItem(
                            text = { Text(localized(language, "Reset kWh", "Reset kWh")) },
                            onClick = { showMenu = false; confirmAction = ConfirmAction.RESET_ENERGY },
                        )
                        DropdownMenuItem(
                            text = { Text(localized(language, "Reset pabrik", "Factory reset")) },
                            onClick = { showMenu = false; confirmAction = ConfirmAction.FACTORY_RESET },
                        )
                    }
                },
            )
        },
    ) { padding ->
        if (device == null) {
            FullScreenLoading(modifier = Modifier.padding(padding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusSection(uiState)
            uiState.status?.takeIf { it.timerRemainingMs > 0 }?.let { status ->
                val remainingMs = countdownRemaining(status.timerRemainingMs, uiState.statusReceivedAtMs, nowMs)
                Text(
                    if (status.timerArmed) localized(language, "Timer bersenjata: mulai saat relay ON · ${formatDuration(remainingMs)}", "Timer armed: starts when relay is ON · ${formatDuration(remainingMs)}")
                    else localized(language, "Timer aktif: ${formatDuration(remainingMs)}", "Timer active: ${formatDuration(remainingMs)}"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            uiState.status?.takeIf { it.scheduleRemainingMs > 0 && it.scheduleTurnOn != null }?.let { status ->
                val remainingMs = countdownRemaining(status.scheduleRemainingMs, uiState.statusReceivedAtMs, nowMs)
                Text(
                    localized(
                        language,
                        "Jadwal berikutnya: ${if (status.scheduleTurnOn == true) "ON" else "OFF"} · ${formatDuration(remainingMs)}",
                        "Next schedule: ${if (status.scheduleTurnOn == true) "ON" else "OFF"} · ${formatDuration(remainingMs)}",
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }

            RelaySection(
                relayState = uiState.status?.relayState ?: RelayState.UNKNOWN,
                relayUiState = uiState.relayUiState,
                relayActuationEnabled = uiState.status?.relayActuationEnabled ?: true,
                onSetRelay = { targetOn ->
                    tapFeedback.onConfirm()
                    viewModel.setRelay(targetOn)
                },
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(localized(language, "Live 1 dtk", "Live 1 sec"), style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = uiState.liveModeRequested,
                    onCheckedChange = {
                        tapFeedback.onTap()
                        viewModel.setLiveMode(it)
                    },
                )
            }

            uiState.measurement?.let { measurement ->
                MeasurementGrid(
                    tiles = measurement.toTiles(
                        rates = uiState.ratesPerMinute,
                        languageEnglish = language == AppLanguage.ENGLISH,
                        energySavedWh = uiState.status?.energySavedWh,
                        energyNextSaveMs = uiState.status?.energyNextSaveMs?.let {
                            countdownRemaining(it, uiState.statusReceivedAtMs, nowMs)
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    onCurrentClick = if (uiState.canSaveLoadSignature) { { showNameLoadDialog = true } } else null,
                )
            }
            uiState.detectedLoadName?.let { name ->
                Text(localized(language, "Perkiraan beban aktif: $name", "Estimated active load: $name"), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            }
            if (uiState.vampireEnergySuspected) {
                Text(
                    localized(language, "Kemungkinan energi vampire: daya rendah dan stabil selama 15 menit.", "Possible standby / vampire energy: low, stable power has continued for 15 minutes."),
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            if (uiState.status?.relayState == RelayState.ON && !uiState.canSaveLoadSignature) {
                Text(localized(language, "Nama beban tersedia setelah arus stabil selama 1 menit.", "Load naming is available after current is stable for one minute."), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            LiveMeasurementChart(
                points = uiState.livePoints,
                onClear = viewModel::clearLiveGraph,
                modifier = Modifier.fillMaxWidth(),
            )

            uiState.lastError?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            initialName = device?.displayName.orEmpty(),
            onConfirm = { newName ->
                viewModel.renameDevice(newName)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }

    confirmAction?.let { action ->
        TripleConfirmDialog(
            action = action,
            onComplete = {
                when (action) {
                    ConfirmAction.RESET_ENERGY -> viewModel.resetEnergy()
                    ConfirmAction.FACTORY_RESET -> viewModel.factoryReset()
                }
                confirmAction = null
            },
            onDismiss = { confirmAction = null },
        )
    }
    if (showTimerDialog) {
        TimerDialog(
            onApply = { days, hours, minutes, seconds ->
                tapFeedback.onConfirm()
                viewModel.applyTimer(days, hours, minutes, seconds)
                showTimerDialog = false
            },
            onReset = {
                tapFeedback.onConfirm()
                viewModel.resetTimer()
                showTimerDialog = false
            },
            onDismiss = { showTimerDialog = false },
        )
    }
    if (showNameLoadDialog) {
        NameLoadDialog(
            onSave = { name -> viewModel.saveLoadSignature(name); showNameLoadDialog = false },
            onDismiss = { showNameLoadDialog = false },
        )
    }
}

@Composable
private fun StatusSection(uiState: DeviceDetailUiState) {
    val language = LocalAppLanguage.current
    val status = uiState.status
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatusPill(
            label = if (status?.fresh == true) localized(language, "Data aktif", "Live data") else localized(language, "Data tidak segar", "Stale data"),
            color = if (status?.fresh == true) AccentGreen else AccentRed,
        )
        StatusPill(
            label = when {
                status?.hasSample == true -> localized(language, "Sensor aktif", "Sensor active")
                status != null -> localized(language, "Sensor menunggu", "Sensor waiting")
                else -> localized(language, "Sensor belum diperiksa", "Sensor unchecked")
            },
            color = if (status?.hasSample == true) AccentGreen else AccentRed,
        )
        StatusPill(
            label = if (status?.wifiConnected == true) localized(language, "Wi-Fi tersambung", "Wi-Fi connected") else localized(language, "Wi-Fi terputus", "Wi-Fi disconnected"),
            color = if (status?.wifiConnected == true) AccentGreen else AccentRed,
        )
    }
}

@Composable
private fun RelaySection(
    relayState: RelayState,
    relayUiState: RelayUiState,
    relayActuationEnabled: Boolean,
    onSetRelay: (Boolean) -> Unit,
) {
    val language = LocalAppLanguage.current
    val isBusy = relayUiState == RelayUiState.SENDING || relayUiState == RelayUiState.WAITING_SETTLE
    var position by remember { mutableStateOf(0.5f) }
    LaunchedEffect(relayState, isBusy) { if (!isBusy) position = 0.5f }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        AnimatedContent(targetState = relayState, label = "relayState") { state ->
            StatusPill(label = relayStatusLabel(state), color = relayStatusColor(state))
        }
        Text(localized(language, "Geser kiri: OFF  •  tengah: netral  •  kanan: ON", "Slide left: OFF  •  center: neutral  •  right: ON"), modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelMedium)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("OFF", color = AccentRed)
            Slider(
                value = position,
                onValueChange = { position = it },
                onValueChangeFinished = {
                    when {
                        position <= 0.2f && relayState != RelayState.OFF -> onSetRelay(false)
                        position >= 0.8f && relayState != RelayState.ON -> onSetRelay(true)
                    }
                    if (!isBusy) position = 0.5f
                },
                enabled = !isBusy && relayActuationEnabled,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text("ON", color = AccentGreen)
        }
        if (isBusy) Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp).size(18.dp), strokeWidth = 2.dp)
            Text(if (relayUiState == RelayUiState.SENDING) localized(language, "Mengirim perintah...", "Sending command...") else localized(language, "Menunggu status...", "Waiting for status..."))
        }
        if (!relayActuationEnabled) {
            Text(
                localized(language, "Kontrol relay dinonaktifkan pada firmware ini", "Relay control is disabled in this firmware"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RenameDialog(initialName: String, onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    val language = LocalAppLanguage.current
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(language, "Ganti nama SmartPlug", "Rename SmartPlug")) },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(localized(language, "Simpan", "Save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localized(language, "Batal", "Cancel")) } },
    )
}

private enum class ConfirmAction { RESET_ENERGY, FACTORY_RESET }

@Composable
private fun TripleConfirmDialog(action: ConfirmAction, onComplete: () -> Unit, onDismiss: () -> Unit) {
    val language = LocalAppLanguage.current
    var step by remember(action) { mutableStateOf(1) }
    val title = if (action == ConfirmAction.RESET_ENERGY) localized(language, "Reset counter kWh?", "Reset kWh counter?") else localized(language, "Factory reset SmartPlug?", "Factory reset SmartPlug?")
    val message = if (action == ConfirmAction.RESET_ENERGY) {
        localized(language, "Counter energi perangkat akan kembali ke nol. Riwayat server sebelum reset tetap disimpan.", "The device energy counter will return to zero. Server history before the reset is retained.")
    } else {
        localized(language, "Wi-Fi, konfigurasi, sesi, dan kredensial perangkat akan dihapus. Perangkat akan reboot ke mode pemasangan.", "Wi-Fi, configuration, sessions, and device credentials will be erased. The device will reboot into pairing mode.")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title ($step/3)") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = { if (step == 3) onComplete() else step++ }) {
                Text(if (step == 3) localized(language, "Ya, jalankan", "Yes, proceed") else localized(language, "Saya mengerti", "I understand"))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localized(language, "Batal", "Cancel")) } },
    )
}

@Composable
private fun TimerDialog(
    onApply: (Int, Int, Int, Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val language = LocalAppLanguage.current
    var days by remember { mutableStateOf(0) }
    var hours by remember { mutableStateOf(0) }
    var minutes by remember { mutableStateOf(0) }
    var seconds by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(language, "Timer SmartPlug", "SmartPlug timer")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(localized(language, "Timer disimpan di SmartPlug/server. Jika disetel saat relay OFF, hitung mundur mulai ketika relay ON.", "The timer is stored on the SmartPlug/server. If set while the relay is OFF, countdown starts when the relay turns ON."), style = MaterialTheme.typography.bodySmall)
                TimerField(localized(language, "Hari", "Days"), days) { days = it.coerceAtLeast(0) }
                TimerField(localized(language, "Jam", "Hours"), hours) { hours = it.coerceIn(0, 23) }
                TimerField(localized(language, "Menit", "Minutes"), minutes) { minutes = it.coerceIn(0, 59) }
                TimerField(localized(language, "Detik", "Seconds"), seconds) { seconds = it.coerceIn(0, 59) }
            }
        },
        confirmButton = { TextButton(onClick = { onApply(days, hours, minutes, seconds) }) { Text(localized(language, "Terapkan ke SmartPlug", "Apply to SmartPlug")) } },
        dismissButton = {
            Row {
                TextButton(onClick = onReset) { Text(localized(language, "Reset + stop", "Reset + stop")) }
                TextButton(onClick = onDismiss) { Text(localized(language, "Batal", "Cancel")) }
            }
        },
    )
}

@Composable
private fun TimerField(label: String, value: Int, onChange: (Int) -> Unit) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { onChange(it.filter(Char::isDigit).toIntOrNull() ?: 0) },
        label = { Text(label) },
        singleLine = true,
    )
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val days = totalSeconds / 86_400
    val hours = totalSeconds % 86_400 / 3_600
    val minutes = totalSeconds % 3_600 / 60
    val seconds = totalSeconds % 60
    return if (days > 0) "%dd %02d:%02d:%02d".format(days, hours, minutes, seconds)
    else "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun countdownRemaining(sourceMs: Long, receivedAtMs: Long, nowMs: Long): Long =
    (sourceMs - (nowMs - receivedAtMs).coerceAtLeast(0L)).coerceAtLeast(0L)

@Composable
private fun NameLoadDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    val language = LocalAppLanguage.current
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(language, "Beri nama beban", "Name this load")) },
        text = { Column { Text(localized(language, "Pencocokan memakai arus dan power factor, sehingga hasilnya adalah perkiraan.", "Matching uses current and power factor, so it is an estimate."), style = MaterialTheme.typography.bodySmall); OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(localized(language, "Contoh: Pompa air", "Example: Water pump")) }, singleLine = true) } },
        confirmButton = { TextButton(onClick = { onSave(name) }, enabled = name.isNotBlank()) { Text(localized(language, "Simpan", "Save")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localized(language, "Batal", "Cancel")) } },
    )
}
