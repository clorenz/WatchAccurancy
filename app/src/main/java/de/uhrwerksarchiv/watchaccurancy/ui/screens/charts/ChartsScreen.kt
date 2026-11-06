package de.uhrwerksarchiv.watchaccurancy.ui.screens.charts

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import de.uhrwerksarchiv.watchaccurancy.data.model.MeasurementLog
import de.uhrwerksarchiv.watchaccurancy.data.model.PositionStatistic
import de.uhrwerksarchiv.watchaccurancy.data.model.RateCalculator
import de.uhrwerksarchiv.watchaccurancy.data.model.RatePoint
import de.uhrwerksarchiv.watchaccurancy.data.model.Watch
import de.uhrwerksarchiv.watchaccurancy.data.model.WatchPosition
import de.uhrwerksarchiv.watchaccurancy.data.model.WatchStatistics
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    watches: List<Watch>,
    selectedWatchId: Long?,
    ratePoints: List<RatePoint>,
    statistics: WatchStatistics,
    filterPosition: WatchPosition?,
    onSelectWatch: (Long) -> Unit,
    onFilterPosition: (WatchPosition?) -> Unit,
    onUpdateLog: (MeasurementLog) -> Unit,
    onDeleteLog: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var metric by remember { mutableStateOf(ChartMetric.RATE_PER_DAY) }
    var showWatchDropdown by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<MeasurementLog?>(null) }
    var logToEdit by remember { mutableStateOf<MeasurementLog?>(null) }

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

        // 7. MEASUREMENT HISTORY TABLE WITH EDIT & DELETE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Messpunkt-Historie (${ratePoints.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tippe auf ✎ zum Korrigieren",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ratePoints.reversed().forEach { point ->
                MeasurementHistoryItem(
                    point = point,
                    onEdit = { logToEdit = point.log },
                    onDelete = { logToDelete = point.log }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // Edit Measurement Dialog (Messung nachträglich korrigieren)
    logToEdit?.let { log ->
        EditMeasurementDialog(
            log = log,
            onDismiss = { logToEdit = null },
            onConfirm = { updated ->
                onUpdateLog(updated)
                logToEdit = null
            }
        )
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
private fun PositionStatsCard(positionStats: Map<WatchPosition, PositionStatistic>) {
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
    onEdit: () -> Unit,
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
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
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
                IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_log_btn_${point.log.id}")) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Messpunkt bearbeiten",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_log_btn_${point.log.id}")) {
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

/**
 * Dialog zur nachträglichen Korrektur einer Messung.
 */
@Composable
private fun EditMeasurementDialog(
    log: MeasurementLog,
    onDismiss: () -> Unit,
    onConfirm: (MeasurementLog) -> Unit
) {
    var editedPosition by remember { mutableStateOf(log.position) }
    var editedWatchTimeMs by remember { mutableStateOf(log.watchTimestampMs) }
    var editedExtraValue by remember { mutableStateOf(log.extraValue.toString()) }
    var editedNotes by remember { mutableStateOf(log.notes) }
    var showPosDropdown by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY) }

    // Berechne die resultierende Abweichung live bei Änderungen
    val currentDeviationSec = (editedWatchTimeMs - log.ntpTimestampMs) / 1000.0
    val devSign = if (currentDeviationSec >= 0) "+" else ""

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Messung korrigieren",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "NTP-Atomzeitpunkt: ${timeFormatter.format(Date(log.ntpTimestampMs))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Ziel-Uhrenzeit anpassen
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Eingestellte Uhrenzeit:",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = timeFormatter.format(Date(editedWatchTimeMs)),
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Abweichung: $devSign${String.format(Locale.US, "%.2fs", currentDeviationSec)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Schnellkorrektur für Zeigerzeit (+/- Sekunden oder Minuten)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { editedWatchTimeMs -= 60000L },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("-1m", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { editedWatchTimeMs -= 1000L },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("-1s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { editedWatchTimeMs += 1000L },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+1s", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { editedWatchTimeMs += 60000L },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("+1m", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Uhrenlage Dropdown
                Column {
                    Text(text = "Uhrenlage:", style = MaterialTheme.typography.labelSmall)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPosDropdown = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
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
                                            .background(editedPosition.color, CircleShape)
                                    )
                                    Text(
                                        text = "${editedPosition.germanTitle} (${editedPosition.code})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = showPosDropdown,
                            onDismissRequest = { showPosDropdown = false }
                        ) {
                            WatchPosition.entries.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.germanTitle} (${p.code})") },
                                    onClick = {
                                        editedPosition = p
                                        showPosDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Zusatzwert
                OutlinedTextField(
                    value = editedExtraValue,
                    onValueChange = { editedExtraValue = it },
                    label = { Text("Zusatzwert (z.B. Temperatur °C)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notizen
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Notiz") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val extra = editedExtraValue.toDoubleOrNull() ?: log.extraValue
                    val updated = log.copy(
                        watchTimestampMs = editedWatchTimeMs,
                        positionCode = editedPosition.code,
                        extraValue = extra,
                        notes = editedNotes
                    )
                    onConfirm(updated)
                }
            ) {
                Text("Änderungen speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}
