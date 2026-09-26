package com.smartplug.app.ui.screens.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.smartplug.app.domain.model.DailyScheduleEntry
import com.smartplug.app.ui.components.SmartPlugCard
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized
import kotlinx.coroutines.delay
import java.util.TimeZone

@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    viewModel: ScheduleViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current
    var showAdd by remember { mutableStateOf(false) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { nowMs = System.currentTimeMillis(); delay(1_000) } }
    // Refresh the firmware-owned next occurrence so the countdown rolls to tomorrow after an
    // event fires, even while this page stays open.
    LaunchedEffect(Unit) { while (true) { delay(10_000); viewModel.refresh() } }
    val timezoneOffset = TimeZone.getDefault().getOffset(nowMs) / 60_000
    val schedule = uiState.schedule

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized(language, "Jadwal", "Schedule")) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, localized(language, "Kembali", "Back")) } },
                actions = { IconButton(onClick = { showAdd = true }, enabled = schedule != null) { Icon(Icons.Filled.Add, localized(language, "Tambah jadwal", "Add schedule")) } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            schedule?.let { value ->
                SmartPlugCard {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(localized(language, "Aktifkan jadwal", "Enable schedule"), style = MaterialTheme.typography.titleMedium)
                            Text(
                                if (value.clockSynchronized) localized(language, "Waktu SmartPlug tersinkronisasi", "SmartPlug time is synchronized")
                                else localized(language, "Menunggu waktu internet (NTP)", "Waiting for internet time (NTP)"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(checked = value.enabled, onCheckedChange = { viewModel.setEnabled(it, timezoneOffset) })
                    }
                    if (value.enabled && value.nextTurnOn != null) {
                        val seconds = (value.nextRemainingSeconds - (nowMs - uiState.scheduleReceivedAtMs).coerceAtLeast(0) / 1_000L).coerceAtLeast(0)
                        Text(
                            localized(language, "Berikutnya: ${if (value.nextTurnOn) "ON" else "OFF"} dalam ${formatCountdown(seconds)}", "Next: ${if (value.nextTurnOn) "ON" else "OFF"} in ${formatCountdown(seconds)}"),
                            modifier = Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                value.entries.forEachIndexed { index, entry ->
                    ScheduleEntryCard(
                        entry = entry,
                        canMoveUp = index > 0,
                        canMoveDown = index < value.entries.lastIndex,
                        onMoveUp = { viewModel.move(index, index - 1) },
                        onMoveDown = { viewModel.move(index, index + 1) },
                        onDelete = { viewModel.delete(index) },
                    )
                }
                if (value.entries.isEmpty()) {
                    Text(localized(language, "Belum ada jadwal. Tekan + untuk menambahkan ON/OFF harian.", "No schedules yet. Tap + to add a daily ON/OFF action."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } ?: Text(if (uiState.isLoading) localized(language, "Memuat jadwal…", "Loading schedule…") else scheduleError(language, uiState.error))
            uiState.error?.takeIf { schedule != null }?.let { Text(scheduleError(language, it), color = MaterialTheme.colorScheme.error) }
        }
    }
    if (showAdd) {
        AddScheduleDialog(
            onDismiss = { showAdd = false },
            onAdd = { hour, minute, turnOn, event ->
                viewModel.add(hour, minute, turnOn, event, timezoneOffset) { showAdd = false }
            },
        )
    }
}

@Composable
private fun ScheduleEntryCard(entry: DailyScheduleEntry, canMoveUp: Boolean, canMoveDown: Boolean, onMoveUp: () -> Unit, onMoveDown: () -> Unit, onDelete: () -> Unit) {
    val language = LocalAppLanguage.current
    SmartPlugCard {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(if (entry.turnOn) "ON" else "OFF", style = MaterialTheme.typography.titleMedium)
                Text(localized(language, "Setiap hari ${"%02d:%02d".format(entry.hour, entry.minute)}", "Every day at ${"%02d:%02d".format(entry.hour, entry.minute)}"), style = MaterialTheme.typography.bodyMedium)
                if (entry.event.isNotBlank()) Text(entry.event, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onMoveUp, enabled = canMoveUp) { Icon(Icons.Filled.ArrowUpward, "Move up") }
            IconButton(onClick = onMoveDown, enabled = canMoveDown) { Icon(Icons.Filled.ArrowDownward, "Move down") }
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "Delete") }
        }
    }
}

@Composable
private fun AddScheduleDialog(onDismiss: () -> Unit, onAdd: (Int, Int, Boolean, String) -> Unit) {
    val language = LocalAppLanguage.current
    var hour by remember { mutableStateOf("00") }
    var minute by remember { mutableStateOf("00") }
    var turnOn by remember { mutableStateOf(true) }
    var event by remember { mutableStateOf("") }
    val valid = hour.toIntOrNull() in 0..23 && minute.toIntOrNull() in 0..59
    val eventValid = event.length <= 24 && event.all { character ->
        character.code in 0x20..0x7e && character !in setOf(',', ':', '"', '\\')
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localized(language, "Tambah jadwal", "Add schedule")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = turnOn, onClick = { turnOn = true }, label = { Text("ON") })
                    FilterChip(selected = !turnOn, onClick = { turnOn = false }, label = { Text("OFF") })
                }
                Text(localized(language, "Setiap hari pada waktu berikut (format 24 jam)", "Every day at (24-hour time)"))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = hour, onValueChange = { hour = it.take(2) }, label = { Text("HH") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minute, onValueChange = { minute = it.take(2) }, label = { Text("MM") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(
                    value = event,
                    onValueChange = { event = it.take(24) },
                    label = { Text(localized(language, "Kegiatan", "Event")) },
                    placeholder = { Text(localized(language, "Contoh: Pompa air", "Example: Water pump")) },
                    singleLine = true,
                    isError = !eventValid,
                    supportingText = if (!eventValid) ({ Text(localized(language, "Gunakan huruf/angka biasa; tanpa koma, titik dua, kutip, atau garis miring.", "Use plain letters/numbers; commas, colons, quotes, and backslashes are not allowed.")) }) else null,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onAdd(hour.toInt(), minute.toInt(), turnOn, event) }, enabled = valid && eventValid) { Text(localized(language, "Tambah", "Add")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localized(language, "Batal", "Cancel")) } },
    )
}

private fun formatCountdown(seconds: Long): String = "%02d:%02d".format(seconds / 60, seconds % 60)

private fun scheduleError(language: com.smartplug.app.ui.localization.AppLanguage, error: String?): String = when (error) {
    "schedule_requires_direct_firmware" -> localized(language, "Jadwal server belum tersedia pada firmware ServerSmartPlug ini.", "Server schedules are not yet available on this ServerSmartPlug firmware.")
    "schedule_full" -> localized(language, "Maksimum delapan jadwal per SmartPlug.", "A SmartPlug can have up to eight schedules.")
    "schedule_event_storage_full" -> localized(language, "Nama kegiatan terlalu panjang untuk penyimpanan jadwal SmartPlug. Pendekkan atau hapus nama kegiatan lain.", "Event labels exceed this SmartPlug's schedule storage. Shorten this label or remove another event label.")
    "invalid_schedule_entry" -> localized(language, "Data jadwal tidak valid.", "The schedule entry is invalid.")
    else -> localized(language, "Jadwal tidak dapat dimuat.", "Schedule could not be loaded.")
}
