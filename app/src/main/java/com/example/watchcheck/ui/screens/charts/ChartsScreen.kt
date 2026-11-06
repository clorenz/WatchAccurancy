package com.example.watchcheck.ui.screens.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchcheck.data.model.MeasurementLog
import com.example.watchcheck.data.model.RateCalculator
import com.example.watchcheck.data.model.RatePoint
import com.example.watchcheck.data.model.Watch
import com.example.watchcheck.data.model.WatchPosition
import com.example.watchcheck.data.model.WatchStatistics
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    watches: List<Watch>,
    selectedWatchId: Long?,
    ratePoints: List<RatePoint>,
    statistics: WatchStatistics,
    filterPosition: WatchPosition?,
    onSelectWatch: (Long) -> Unit,
    onFilterPosition: (WatchPosition?) -> Unit,
    onDeleteLog: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var metric by remember { mutableStateOf(ChartMetric.RATE_PER_DAY) }
    var showWatchDropdown by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<MeasurementLog?>(null) }

    val selectedWatch = watches.firstOrNull { it.id == selectedWatchId }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. WATCH SELECTOR
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showWatchDropdown = true }
                .testTag("chart_watch_selector"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text(
                            text = "Ausgewählte Uhr:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedWatch?.model ?: "Keine Uhr gewählt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }

            DropdownMenu(
                expanded = showWatchDropdown,
                onDismissRequest = { showWatchDropdown = false }
            ) {
                watches.forEach { watch ->
                    DropdownMenuItem(
                        text = { Text(watch.model, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            onSelectWatch(watch.id)
                            showWatchDropdown = false
                        }
                    )
                }
            }
        }

        // 2. METRIC SELECTOR TABS
        PrimaryTabRow(
            selectedTabIndex = if (metric == ChartMetric.RATE_PER_DAY) 0 else 1,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = metric == ChartMetric.RATE_PER_DAY,
                onClick = { metric = ChartMetric.RATE_PER_DAY },
                text = { Text("Gangabweichung (s/d)") },
                modifier = Modifier.testTag("metric_rate_tab")
            )
            Tab(
                selected = metric == ChartMetric.CUMULATIVE_DEVIATION,
                onClick = { metric = ChartMetric.CUMULATIVE_DEVIATION },
                text = { Text("Abweichung (s)") },
                modifier = Modifier.testTag("metric_deviation_tab")
            )
        }

        // 3. POSITION FILTER ROW
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = "Nach Uhrenlage filtern:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterPosition == null,
                    onClick = { onFilterPosition(null) },
                    label = { Text("Alle Lagen") },
                    leadingIcon = if (filterPosition == null) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null,
                    modifier = Modifier.testTag("filter_pos_all")
                )

                WatchPosition.entries.forEach { pos ->
                    val isPosSelected = filterPosition == pos
                    FilterChip(
                        selected = isPosSelected,
                        onClick = { onFilterPosition(if (isPosSelected) null else pos) },
                        label = { Text(pos.shortLabel) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(pos.color, CircleShape)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = pos.color.copy(alpha = 0.2f),
                            selectedLabelColor = pos.color
                        ),
                        modifier = Modifier.testTag("filter_pos_${pos.code}")
                    )
                }
            }
        }

        // 4. THE INTERACTIVE CANVAS CHART
        WatchPrecisionChart(
            ratePoints = ratePoints,
            metric = metric,
            modifier = Modifier.fillMaxWidth()
        )

        // 5. STATISTICS SUMMARY CARD
        StatisticsSummaryCard(statistics = statistics)

        // 6. POSITION TABLE CARD (LAGENVERGLEICH)
        if (statistics.positionStats.isNotEmpty()) {
            PositionStatsCard(positionStats = statistics.positionStats)
        }

        // 7. MEASUREMENT HISTORY TABLE
        Text(
            text = "Messpunkt-Historie (${ratePoints.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ratePoints.reversed().forEach { point ->
                MeasurementHistoryItem(
                    point = point,
                    onDelete = { logToDelete = point.log }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Delete Log Dialog
    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title = { Text("Messpunkt löschen?") },
            text = { Text("Diesen Messpunkt vom ${RateCalculator.formatDateTime(log.ntpTimestampMs)} löschen?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteLog(log.id)
                        logToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun StatisticsSummaryCard(statistics: WatchStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gang-Statistik",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // COSC Chronometer Status Indicator (-4 bis +6 s/d)
                val avg = statistics.averageRatePerDay
                if (avg != null) {
                    val isCosc = avg in -4.0..6.0
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCosc) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (isCosc) "COSC Norm: Erfüllt" else "COSC Norm: Abweichend",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCosc) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn(
                    label = "Mittlerer Gang",
                    value = statistics.averageRatePerDay?.let {
                        val sign = if (it >= 0) "+" else ""
                        String.format(Locale.US, "%s%.2f s/d", sign, it)
                    } ?: "---"
                )
                StatColumn(
                    label = "Min / Max Gang",
                    value = if (statistics.minRatePerDay != null && statistics.maxRatePerDay != null) {
                        String.format(Locale.US, "%.1f / %.1f", statistics.minRatePerDay, statistics.maxRatePerDay)
                    } else "---"
                )
                StatColumn(
                    label = "Gesamt-Delta",
                    value = statistics.totalDriftSeconds?.let {
                        val sign = if (it >= 0) "+" else ""
                        String.format(Locale.US, "%s%.1f s", sign, it)
                    } ?: "---"
                )
                StatColumn(
                    label = "Beobachtung",
                    value = statistics.totalObservationDays?.let {
                        String.format(Locale.US, "%.1f T", it)
                    } ?: "---"
                )
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PositionStatsCard(positionStats: Map<WatchPosition, com.example.watchcheck.data.model.PositionStatistic>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Lagenvergleich (Gang je Position)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            positionStats.forEach { (pos, stat) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(pos.color, CircleShape)
                        )
                        Text(
                            text = "${pos.shortLabel} - ${pos.germanTitle}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${stat.count}×",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val rateStr = stat.averageRatePerDay?.let {
                            val sign = if (it >= 0) "+" else ""
                            String.format(Locale.US, "%s%.2f s/d", sign, it)
                        } ?: "---"
                        Text(
                            text = rateStr,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = pos.color
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementHistoryItem(
    point: RatePoint,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(point.log.position.color, CircleShape)
                )
                Column {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = RateCalculator.formatDateTime(point.log.ntpTimestampMs),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = point.log.position.color.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = point.log.position.code,
                                style = MaterialTheme.typography.labelSmall,
                                color = point.log.position.color,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "Abweichung: ${point.formattedDeviation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (point.log.notes.isNotBlank()) {
                        Text(
                            text = point.log.notes,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = point.formattedRate,
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (point.rateSecondsPerDay != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Messpunkt löschen",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}
