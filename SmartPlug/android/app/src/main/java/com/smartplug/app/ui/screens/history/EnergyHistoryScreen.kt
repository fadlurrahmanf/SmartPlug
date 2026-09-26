package com.smartplug.app.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartplug.app.ui.components.EmptyState
import com.smartplug.app.ui.components.EnergyLineChart
import com.smartplug.app.ui.components.FullScreenError
import com.smartplug.app.ui.components.FullScreenLoading
import com.smartplug.app.ui.components.SmartPlugCard
import com.smartplug.app.domain.model.EnergyHistoryPoint
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized
import java.util.Locale

@Composable
fun EnergyHistoryScreen(
    deviceId: String,
    onBack: () -> Unit,
    viewModel: EnergyHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = LocalAppLanguage.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localized(language, "Riwayat pengukuran", "Measurement history")) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = localized(language, "Kembali", "Back")) }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryRange.entries.forEach { range ->
                    FilterChip(
                        selected = uiState.resolution == range.resolution,
                        onClick = { viewModel.loadRange(range) },
                        label = { Text(range.label(language)) },
                    )
                }
            }

            Column(modifier = Modifier.padding(top = 16.dp).verticalScroll(rememberScrollState())) {
                when {
                    uiState.isLoading -> FullScreenLoading()
                    uiState.error != null && uiState.points.isEmpty() -> FullScreenError(
                        message = uiState.error ?: localized(language, "Riwayat tidak tersedia", "History is unavailable"),
                        onRetry = { viewModel.loadRange(HistoryRange.LAST_DAY) },
                    )
                    uiState.points.isEmpty() -> EmptyState(
                        title = localized(language, "Belum ada riwayat", "No history yet"),
                        description = localized(language, "Mode langsung menyimpan satu titik tiap menit saat halaman perangkat terbuka. Mode server memuat riwayat dari SD card ServerSmartPlug.", "Direct mode saves one point per minute while the device page is open. Server mode loads history from the ServerSmartPlug SD card."),
                    )
                    else -> {
                        HistorySummary(points = uiState.points, languageEnglish = language.name == "ENGLISH")
                        HistoryMetricCard(localized(language, "Tegangan (V)", "Voltage (V)"), uiState.points, "V") { it.voltageV }
                        HistoryMetricCard(localized(language, "Arus (A)", "Current (A)"), uiState.points, "A") { it.currentA }
                        HistoryMetricCard(localized(language, "Daya aktif (W)", "Active power (W)"), uiState.points, "W") { it.activePowerW }
                        HistoryMetricCard(localized(language, "Daya semu (VA)", "Apparent power (VA)"), uiState.points, "VA") { it.apparentPowerVa }
                        HistoryMetricCard(localized(language, "Power factor (%)", "Power factor (%)"), uiState.points, "%") { it.powerFactor * 100.0 }
                        HistoryMetricCard(localized(language, "Energi kumulatif (kWh)", "Cumulative energy (kWh)"), uiState.points, "kWh") { it.energyWh.coerceAtLeast(0.0) / 1000.0 }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryMetricCard(
    title: String,
    points: List<EnergyHistoryPoint>,
    unit: String,
    value: (EnergyHistoryPoint) -> Double,
) {
    SmartPlugCard(modifier = Modifier.padding(top = 12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        EnergyLineChart(points = points, valueSelector = value, unit = unit)
    }
}

@Composable
private fun HistorySummary(points: List<EnergyHistoryPoint>, languageEnglish: Boolean) {
    val energyUsedKwh = ((points.last().energyWh - points.first().energyWh) / 1000.0).coerceAtLeast(0.0)
    SmartPlugCard {
        Text(if (languageEnglish) "Summary" else "Ringkasan", style = MaterialTheme.typography.titleMedium)
        Text(
            if (languageEnglish) "${points.size} recorded point(s) · ${formatHistory(energyUsedKwh)} kWh used in this range"
            else "${points.size} titik tercatat · ${formatHistory(energyUsedKwh)} kWh terpakai pada rentang ini",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        HistoryRangeRow(if (languageEnglish) "Voltage" else "Tegangan", points.map { it.voltageV }, "V")
        HistoryRangeRow(if (languageEnglish) "Current" else "Arus", points.map { it.currentA }, "A")
        HistoryRangeRow(if (languageEnglish) "Active power" else "Daya aktif", points.map { it.activePowerW }, "W")
        HistoryRangeRow("Power factor", points.map { it.powerFactor * 100.0 }, "%")
    }
}

@Composable
private fun HistoryRangeRow(label: String, values: List<Double>, unit: String) {
    if (values.isEmpty()) return
    Row(modifier = Modifier.fillMaxWidth().padding(top = 5.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(
            "${formatHistory(values.min())} / ${formatHistory(values.average())} / ${formatHistory(values.max())} $unit",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

private fun formatHistory(value: Double): String = String.format(Locale.US, "%.2f", value)

private fun HistoryRange.label(language: com.smartplug.app.ui.localization.AppLanguage): String = when (this) {
    HistoryRange.LAST_HOUR -> localized(language, "1 jam", "1 hour")
    HistoryRange.LAST_DAY -> localized(language, "24 jam", "24 hours")
    HistoryRange.LAST_WEEK -> localized(language, "7 hari", "7 days")
    HistoryRange.LAST_MONTH -> localized(language, "30 hari", "30 days")
}
