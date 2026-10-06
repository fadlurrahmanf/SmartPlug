package com.smartplug.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartplug.app.ui.screens.devicedetail.LiveMeasurementPoint
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized
import java.util.Locale
import kotlin.math.abs

enum class LiveMetric(val label: String, val unit: String, val color: Color) {
    // Energy comes first: cumulative kWh is the number this app is about, then power.
    ENERGY("Energy", "kWh", Color(0xFF477BFF)),
    WATT("Watt", "W", Color(0xFFEE6C4D)),
    VOLTAGE("Volt", "V", Color(0xFF1B998B)),
    CURRENT("Amp", "A", Color(0xFFF0A202)),
    POWER_FACTOR("PF", "%", Color(0xFF8B5CF6)),
}

/** Fixed four-minute window, matching the X axis labels below. */
private const val WINDOW_MINUTES = 4

@Composable
fun LiveMeasurementChart(
    points: List<LiveMeasurementPoint>,
    @Suppress("UNUSED_PARAMETER") onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    var metric by remember { mutableStateOf(LiveMetric.ENERGY) }
    var minimized by remember { mutableStateOf(false) }
    val cutoff = System.currentTimeMillis() - WINDOW_MINUTES * 60_000L
    val visible = points.filter { it.timestampMs >= cutoff }
    val values = visible.map { metricValue(it, metric) }

    SmartPlugCard(modifier = modifier) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                localized(language, "Tren live", "Live trend"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            AssistChip(
                onClick = { minimized = !minimized },
                label = { Text(if (minimized) localized(language, "Tampilkan", "Show") else localized(language, "Minimalkan", "Minimize")) },
            )
        }
        if (minimized) return@SmartPlugCard
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            LiveMetric.entries.forEach { item ->
                FilterChip(
                    selected = metric == item,
                    onClick = { metric = item },
                    modifier = Modifier.weight(1f),
                    label = {
                        Text(item.label, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                if (values.isEmpty()) "–" else format(values.last()),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                " ${metric.unit}",
                style = MaterialTheme.typography.titleMedium,
                color = metric.color,
                modifier = Modifier.padding(bottom = 4.dp).weight(1f),
            )
            Text(
                metricName(metric, language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        if (values.size < 2) {
            Text(
                localized(language, "Menunggu minimal dua sampel untuk membentuk grafik.", "Waiting for at least two samples to draw the chart."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            TickChart(
                values = values,
                xLabels = listOf("-4m", "-3m", "-2m", "-1m", "now"),
                color = metric.color,
                animateKey = metric,
            )
        }
    }
}

private fun metricName(metric: LiveMetric, language: com.smartplug.app.ui.localization.AppLanguage): String = when (metric) {
    LiveMetric.ENERGY -> localized(language, "Energi kumulatif", "Cumulative energy")
    LiveMetric.WATT -> localized(language, "Daya aktif", "Active power")
    LiveMetric.VOLTAGE -> localized(language, "Tegangan", "Voltage")
    LiveMetric.CURRENT -> localized(language, "Arus", "Current")
    LiveMetric.POWER_FACTOR -> "Power factor"
}

private fun metricValue(point: LiveMeasurementPoint, metric: LiveMetric): Double = when (metric) {
    LiveMetric.VOLTAGE -> point.measurement.voltageV
    LiveMetric.CURRENT -> point.measurement.currentA
    LiveMetric.WATT -> point.measurement.activePowerW
    LiveMetric.ENERGY -> point.measurement.energyWh.coerceAtLeast(0.0) / 1000.0
    LiveMetric.POWER_FACTOR -> point.measurement.powerFactor * 100.0
}

// Very small non-zero readings (e.g. a few Wh expressed in kWh) need more decimals, otherwise the
// value would show as 0.000 while the chart is clearly moving.
private fun format(value: Double): String = String.format(
    Locale.US,
    when {
        value != 0.0 && abs(value) < 0.01 -> "%.5f"
        abs(value) < 10 -> "%.3f"
        else -> "%.1f"
    },
    value,
)
