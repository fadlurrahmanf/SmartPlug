package com.smartplug.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.smartplug.app.domain.model.DailyScheduleEntry
import com.smartplug.app.ui.theme.AccentGreen

/** 24 hour strip: green where the plug is scheduled ON, a blue marker for the current time. */
@Composable
fun ScheduleRail(entries: List<DailyScheduleEntry>, nowMinute: Int, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    val marker = MaterialTheme.colorScheme.primary
    val intervals = scheduleOnIntervals(entries)
    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(track),
        ) {
            intervals.forEach { (start, end) ->
                drawRect(
                    AccentGreen.copy(alpha = 0.45f),
                    topLeft = Offset(size.width * start / 1440f, 0f),
                    size = Size(size.width * (end - start) / 1440f, size.height),
                )
            }
            val x = size.width * nowMinute.coerceIn(0, 1440) / 1440f
            drawLine(marker, Offset(x, 0f), Offset(x, size.height), strokeWidth = 5f)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** ON spans in minutes of the day: each ON entry lasts until the next OFF entry (wrapping midnight). */
private fun scheduleOnIntervals(entries: List<DailyScheduleEntry>): List<Pair<Int, Int>> {
    val sorted = entries.sortedBy { it.hour * 60 + it.minute }
    if (sorted.isEmpty()) return emptyList()
    val result = mutableListOf<Pair<Int, Int>>()
    sorted.forEachIndexed { i, entry ->
        if (!entry.turnOn) return@forEachIndexed
        val start = entry.hour * 60 + entry.minute
        var end = 1440
        for (step in 1..sorted.size) {
            val candidate = sorted[(i + step) % sorted.size]
            if (!candidate.turnOn) {
                val minute = candidate.hour * 60 + candidate.minute
                end = if (i + step >= sorted.size) minute + 1440 else minute
                break
            }
        }
        if (end <= 1440) {
            if (end > start) result += start to end
        } else {
            result += start to 1440
            if (end - 1440 > 0) result += 0 to (end - 1440)
        }
    }
    return result
}

/** Four-segment progress strip for the pairing flows; segments up to [current] are filled. */
@Composable
fun StepperHeader(current: Int, modifier: Modifier = Modifier, total: Int = 4) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { index ->
            val color by animateColorAsState(
                if (index < current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                label = "stepSegment",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}
