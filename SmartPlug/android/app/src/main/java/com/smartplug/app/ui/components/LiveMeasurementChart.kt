package com.smartplug.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import android.graphics.Paint
import com.smartplug.app.ui.screens.devicedetail.LiveMeasurementPoint
import com.smartplug.app.ui.localization.LocalAppLanguage
import com.smartplug.app.ui.localization.localized
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class LiveMetric(val label: String, val unit: String, val color: Color) {
    VOLTAGE("Voltage", "V", Color(0xFF1B998B)),
    CURRENT("Current", "A", Color(0xFFF0A202)),
    WATT("Watt", "W", Color(0xFFEE6C4D)),
    ENERGY("Energy", "Wh", Color(0xFF477BFF)),
    POWER_FACTOR("Power factor", "PF", Color(0xFF8B5CF6)),
}

@Composable
fun LiveMeasurementChart(
    points: List<LiveMeasurementPoint>,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val language = LocalAppLanguage.current
    var metric by remember { mutableStateOf(LiveMetric.VOLTAGE) }
    var energyInKwh by remember { mutableStateOf(false) }
    var rangeMinutes by remember { mutableStateOf(1f) }
    var showZeroLine by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<LiveMeasurementPoint?>(null) }
    // The dense chart remains immediately available, but does not push essential controls and
    // measurements below the fold when the device page opens.
    var minimized by remember { mutableStateOf(true) }
    val cutoff = System.currentTimeMillis() - rangeMinutes.toLong() * 60_000L
    val visible = points.filter { it.timestampMs >= cutoff }
    val unit = if (metric == LiveMetric.ENERGY && energyInKwh) "kWh" else metric.unit
    val values = visible.map { point -> metricValue(point, metric, energyInKwh) }
    val outlineColor = MaterialTheme.colorScheme.outline

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(localized(language, "Tren live", "Live trend"), style = MaterialTheme.typography.titleMedium)
            OutlinedButton(onClick = { minimized = !minimized }) { Text(if (minimized) localized(language, "Tampilkan", "Show") else localized(language, "Minimalkan", "Minimize")) }
        }
        if (minimized) return@Column
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            LiveMetric.entries.forEach { item ->
                FilterChip(selected = metric == item, onClick = { metric = item; selected = null }, label = { Text(item.label) })
            }
        }
        if (metric == LiveMetric.ENERGY) {
            OutlinedButton(onClick = { energyInKwh = !energyInKwh }) { Text(if (energyInKwh) "kWh" else "Wh") }
        }
        Text(localized(language, "Rentang X: ${rangeMinutes.toInt()} menit", "X range: ${rangeMinutes.toInt()} min"), style = MaterialTheme.typography.labelLarge)
        Slider(value = rangeMinutes, onValueChange = { rangeMinutes = it }, valueRange = 1f..60f, steps = 58)
        OutlinedButton(onClick = { showZeroLine = !showZeroLine }) {
            Text(if (showZeroLine) localized(language, "Garis nol: tampil", "Zero line: shown") else localized(language, "Garis nol: hidden", "Zero line: hidden"))
        }
        OutlinedButton(onClick = onClear) { Text(localized(language, "Hapus grafik", "Clear chart")) }
        if (values.size < 2) {
            Text(localized(language, "Menunggu minimal dua sampel untuk membentuk grafik.", "Waiting for at least two samples to draw the chart."), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val minValue = values.min()
            val maxValue = values.max()
            val low = if (showZeroLine) min(minValue, 0.0) else minValue
            val high = if (showZeroLine) max(maxValue, 0.0) else maxValue
            val paddedRange = max(abs(high - low) * 1.15, 0.001)
            Canvas(
                modifier = Modifier.fillMaxWidth().height(230.dp).background(MaterialTheme.colorScheme.surfaceVariant)
                    .pointerInput(visible, metric, energyInKwh) {
                        detectTapGestures { offset ->
                            val index = ((offset.x / size.width) * (visible.size - 1)).toInt().coerceIn(0, visible.lastIndex)
                            selected = visible[index]
                        }
                    },
            ) {
                // Reserve room for real axis tick labels. Grid lines alone made the
                // graph look precise while hiding the actual scale.
                val left = 44f
                val top = 10f
                // Keep a dedicated gutter at the right for the live endpoint
                // marker, so the last reading never overlaps the Y-axis scale.
                val right = size.width - 58f
                val bottom = size.height - 26f
                val plotWidth = right - left
                val plotHeight = bottom - top
                val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = outlineColor.copy(alpha = 0.9f).toArgb()
                    textSize = 22f
                    textAlign = Paint.Align.RIGHT
                }
                repeat(5) { tick ->
                    val x = left + plotWidth * tick / 4f
                    val y = top + plotHeight * tick / 4f
                    drawLine(outlineColor.copy(alpha = 0.35f), Offset(x, top), Offset(x, bottom), 1f)
                    drawLine(outlineColor.copy(alpha = 0.35f), Offset(left, y), Offset(right, y), 1f)
                    val tickValue = high - (high - low) * tick / 4.0
                    drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(format(tickValue), left - 5f, y + 7f, tickPaint) }
                    tickPaint.textAlign = Paint.Align.CENTER
                    val ageSeconds = ((4 - tick) * rangeMinutes * 60f).toInt()
                    val label = if (tick == 4) "now" else if (ageSeconds >= 60) "-${ageSeconds / 60}m" else "-${ageSeconds}s"
                    drawIntoCanvas { canvas -> canvas.nativeCanvas.drawText(label, x, bottom + 20f, tickPaint) }
                    tickPaint.textAlign = Paint.Align.RIGHT
                }
                val path = Path()
                visible.forEachIndexed { index, point ->
                    val x = left + plotWidth * index / (visible.size - 1).toFloat()
                    val value = metricValue(point, metric, energyInKwh)
                    val y = bottom - (((value - low) / paddedRange).toFloat() * plotHeight)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                if (showZeroLine && 0.0 in low..high) {
                    val y = bottom - (((0.0 - low) / paddedRange).toFloat() * plotHeight)
                    drawLine(outlineColor, Offset(left, y), Offset(right, y), strokeWidth = 1f)
                }
                drawPath(path, metric.color, style = Stroke(width = 4f))
                visible.forEachIndexed { index, point ->
                    val x = left + plotWidth * index / (visible.size - 1).toFloat()
                    val y = bottom - (((metricValue(point, metric, energyInKwh) - low) / paddedRange).toFloat() * plotHeight)
                    drawCircle(metric.color, radius = 3.5f, center = Offset(x, y))
                }
                val latest = visible.last()
                val latestValue = metricValue(latest, metric, energyInKwh)
                val latestY = bottom - (((latestValue - low) / paddedRange).toFloat() * plotHeight)
                val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = metric.color.toArgb()
                    textSize = 24f
                    textAlign = Paint.Align.LEFT
                    isFakeBoldText = true
                }
                drawLine(metric.color.copy(alpha = 0.8f), Offset(right, latestY), Offset(right + 4f, latestY), 2f)
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawText(format(latestValue), right + 7f, latestY + 8f, valuePaint)
                }
            }
        }
        selected?.let { point ->
            Text(
                localized(language,
                    "Dipilih: ${format(metricValue(point, metric, energyInKwh))} $unit · ${((System.currentTimeMillis() - point.timestampMs) / 1000).coerceAtLeast(0)} dtk lalu",
                    "Selected: ${format(metricValue(point, metric, energyInKwh))} $unit · ${((System.currentTimeMillis() - point.timestampMs) / 1000).coerceAtLeast(0)} sec ago"),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

private fun metricValue(point: LiveMeasurementPoint, metric: LiveMetric, energyInKwh: Boolean): Double = when (metric) {
    LiveMetric.VOLTAGE -> point.measurement.voltageV
    LiveMetric.CURRENT -> point.measurement.currentA
    LiveMetric.WATT -> point.measurement.activePowerW
    LiveMetric.ENERGY -> point.measurement.energyWh.coerceAtLeast(0.0) / if (energyInKwh) 1000.0 else 1.0
    LiveMetric.POWER_FACTOR -> point.measurement.powerFactor
}

private fun format(value: Double): String = String.format(Locale.US, if (abs(value) < 10) "%.3f" else "%.1f", value)
