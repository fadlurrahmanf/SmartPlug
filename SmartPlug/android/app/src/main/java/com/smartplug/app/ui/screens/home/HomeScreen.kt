package com.smartplug.app.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartplug.app.ui.components.EmptyState
import com.smartplug.app.ui.components.PollWhileVisible
import com.smartplug.app.ui.components.SmartPlugCard
import com.smartplug.app.ui.screens.devices.DeviceListViewModel
import com.smartplug.app.util.PollingCadence
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized
import java.util.Locale

/** Beranda: a quick "how is my home doing" summary, not a duplicate of the full device list. */
@Composable
fun HomeScreen(
    onOpenDevice: (String) -> Unit,
    onAddSmartPlug: () -> Unit,
    viewModel: DeviceListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current

    PollWhileVisible(intervalMs = PollingCadence.DEVICE_LIST.intervalMs) {
        viewModel.refreshAll()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("SmartPlug") }) }) { padding ->
        if (uiState.rows.isEmpty()) {
            EmptyState(
                title = localized(language, "Selamat datang di SmartPlug", "Welcome to SmartPlug"),
                description = localized(language, "Pantau konsumsi energi dan kendalikan relay dari satu aplikasi.", "Monitor energy use and control relays from one app."),
                modifier = Modifier.padding(padding),
                action = { Button(onClick = onAddSmartPlug) { Text(localized(language, "Tambah SmartPlug", "Add SmartPlug")) } },
            )
            return@Scaffold
        }

        val onlineCount = uiState.rows.count { it.online }
        val readableRows = uiState.rows.filter { it.measurement != null }
        val totalPower = readableRows.sumOf { it.measurement?.activePowerW?.coerceAtLeast(0.0) ?: 0.0 }
        val totalKwh = readableRows.sumOf { (it.measurement?.energyWh ?: 0.0).coerceAtLeast(0.0) / 1000.0 }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SmartPlugCard(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(localized(language, "Ringkasan rumah", "Home overview"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        localized(language, "$onlineCount dari ${uiState.rows.size} SmartPlug online", "$onlineCount of ${uiState.rows.size} SmartPlugs online"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                        OverviewValue(
                            localized(language, "Total daya aktif", "Total active power"),
                            "${format1(totalPower)} W",
                            Modifier.weight(1f),
                        )
                        OverviewValue(
                            localized(language, "Total energi", "Total energy"),
                            "${formatKwh(totalKwh)} kWh",
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            item {
                Text(localized(language, "Perangkat", "Devices"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            items(uiState.rows, key = { it.device.deviceId }) { row ->
                SmartPlugCard(modifier = Modifier.fillMaxWidth().clickable { onOpenDevice(row.device.deviceId) }) {
                    Column {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(row.device.displayName, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(if (row.online) "●" else "●", color = if (row.online) Color(0xFF208B5D) else Color(0xFFC23B3B))
                        }
                        Text(
                            if (row.online) localized(language, "Online", "Online") else localized(language, "Offline", "Offline"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        row.measurement?.let { measurement ->
                            val power = measurement.activePowerW.coerceAtLeast(0.0)
                            val kwh = measurement.energyWh.coerceAtLeast(0.0) / 1000.0
                            val powerPercent = if (totalPower > 0.0) power / totalPower * 100.0 else 0.0
                            val kwhPercent = if (totalKwh > 0.0) kwh / totalKwh * 100.0 else 0.0
                            Text(
                                "${format1(power)} W · ${format1(powerPercent)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                            Text(
                                "${formatKwh(kwh)} kWh · ${format1(kwhPercent)}%",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

private fun format1(value: Double): String = String.format(Locale.US, "%.1f", value)
private fun formatKwh(value: Double): String = String.format(Locale.US, "%.5f", value)
