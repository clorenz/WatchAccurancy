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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Watch
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.text.style.TextAlign
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

enum class OverviewViewMode {
    TABLE,
    CHART
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    watches: List<Watch>,
    selectedWatchId: Long?,
    ratePoints: List<RatePoint>,
    statistics: WatchStatistics,
    filterPosition: WatchPosition?,
    distinctPeriods: List<Int> = emptyList(),
    selectedPeriodSetId: Int? = null,
    allWatchLogs: List<MeasurementLog> = emptyList(),
    onSelectWatch: (Long) -> Unit,
    onSelectPeriod: (Int?) -> Unit = {},
    onFilterPosition: (WatchPosition?) -> Unit,
    onUpdateLog: (MeasurementLog) -> Unit,
    onDeleteLog: (Long) -> Unit,
    initialViewMode: OverviewViewMode = OverviewViewMode.TABLE,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(initialViewMode) }
    var chartMetric by remember { mutableStateOf(ChartMetric.RATE_PER_DAY) }
    var showWatchDropdown by remember { mutableStateOf(false) }

    var logToDelete by remember { mutableStateOf<MeasurementLog?>(null) }
    var logToEdit by remember { mutableStateOf<MeasurementLog?>(null) }
    var infoLogToShow by remember { mutableStateOf<RatePoint?>(null) }

    val selectedWatch = watches.firstOrNull { it.id == selectedWatchId }
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. WATCH SELECTOR HEADER
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
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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
                        if (selectedWatch != null && selectedWatch.caliber.isNotBlank()) {
                            Text(
                                text = "Werk: ${selectedWatch.caliber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                        text = {
                            Column {
                                Text(watch.model, fontWeight = FontWeight.Bold)
                                if (watch.caliber.isNotBlank()) {
                                    Text("Kaliber: ${watch.caliber}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        onClick = {
                            onSelectWatch(watch.id)
                            showWatchDropdown = false
                        }
                    )
                }
            }
        }

        // 2. VIEW MODE SWITCHER TABS (Tabelle vs. Diagramm)
        PrimaryTabRow(
            selectedTabIndex = if (viewMode == OverviewViewMode.TABLE) 0 else 1,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = viewMode == OverviewViewMode.TABLE,
                onClick = { viewMode = OverviewViewMode.TABLE },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Messergebnisse (Tabelle)")
                    }
                },
                modifier = Modifier.testTag("tab_view_table")
            )
            Tab(
                selected = viewMode == OverviewViewMode.CHART,
                onClick = { viewMode = OverviewViewMode.CHART },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Diagramm & Statistik")
                    }
                },
                modifier = Modifier.testTag("tab_view_chart")
            )
        }

        // 3. MESSPERIODEN TABS (WIE BEI WATCHCHECK)
        if (distinctPeriods.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Messperiode (WatchCheck):",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (selectedPeriodSetId != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "Periode #${selectedPeriodSetId + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "Alle Perioden",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val allSelected = selectedPeriodSetId == null
                    val selectedIndex = if (allSelected) 0 else {
                        val idx = distinctPeriods.indexOf(selectedPeriodSetId)
                        if (idx >= 0) idx + 1 else 0
                    }

                    ScrollableTabRow(
                        selectedTabIndex = selectedIndex,
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = allSelected,
                            onClick = { onSelectPeriod(null) },
                            text = {
                                Text(
                                    text = "Alle (${allWatchLogs.size})",
                                    fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("tab_period_all")
                        )

                        distinctPeriods.forEach { pId ->
                            val isSelected = selectedPeriodSetId == pId
                            val pCount = allWatchLogs.count { it.setId == pId }
                            Tab(
                                selected = isSelected,
                                onClick = { onSelectPeriod(pId) },
                                text = {
                                    Text(
                                        text = "Periode ${pId + 1} ($pCount)",
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.testTag("tab_period_$pId")
                            )
                        }
                    }
                }
            }
        }

        // QUICK STATS SUMMARY CHIP
        StatisticsSummaryCard(statistics = statistics)

        if (viewMode == OverviewViewMode.TABLE) {
            // TABELLEN-ANSICHT: Referenz, Abweichung, Täglicher Gang, Position, (I)-Button
            Text(
                text = "Messergebnisse (${ratePoints.size} Einträge)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tippe auf eine Zeile zum Editieren oder auf (i) für Notizen.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (ratePoints.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Noch keine Messpunkte vorhanden. Führe im Reiter 'Messung' die erste Messung durch!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // TABELLEN-HEADER
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Referenz (NTP)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1.5f)
                            )
                            Text(
                                text = "Abweich.",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(0.9f)
                            )
                            Text(
                                text = "Tägl. Gang",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1.0f)
                            )
                            Text(
                                text = "Lage",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(0.8f)
                            )
                            Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Info",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        // TABELLEN-ZEILEN (Chronologisch absteigend: neueste zuerst)
                        ratePoints.reversed().forEachIndexed { index, point ->
                            val isEven = index % 2 == 0
                            val rowBg = if (isEven) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(rowBg)
                                    .clickable { logToEdit = point.log }
                                    .padding(horizontal = 8.dp, vertical = 8.dp)
                                    .testTag("table_row_${point.log.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 1. REFERENZ (NTP-Messzeitpunkt) MIT SEKUNDEN
                                val dateFmt = remember { SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.GERMANY) }
                                Text(
                                    text = dateFmt.format(Date(point.log.ntpTimestampMs)),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1.5f)
                                )

                                // 2. ABWEICHUNG (absolute Abweichung in s)
                                val devSec = point.cumulativeDeviationSeconds
                                val devSign = if (devSec >= 0) "+" else ""
                                Text(
                                    text = String.format(Locale.US, "%s%.2fs", devSign, devSec),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    color = if (devSec >= 0) MaterialTheme.colorScheme.primary else Color(0xFFE65100),
                                    modifier = Modifier.weight(1.0f)
                                )

                                // 3. TÄGLICHER GANG (s/d)
                                val rateText = if (point.rateSecondsPerDay != null) {
                                    val r = point.rateSecondsPerDay
                                    val rSign = if (r >= 0) "+" else ""
                                    String.format(Locale.US, "%s%.2f", rSign, r)
                                } else {
                                    "Ref."
                                }
                                Text(
                                    text = rateText,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    color = if (point.rateSecondsPerDay != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1.1f)
                                )

                                // 4. POSITION (Uhrenlage)
                                Box(
                                    modifier = Modifier.weight(0.9f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = point.log.position.color.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = point.log.position.code,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = point.log.position.color,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                // 5. (I) INFO-BUTTON
                                val hasInfo = point.log.notes.isNotBlank() || point.log.extraValue != 0.0
                                IconButton(
                                    onClick = { infoLogToShow = point },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("info_btn_${point.log.id}")
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = "Zusatzinfo",
                                        tint = if (hasInfo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }

        } else {
            // DIAGRAMM-ANSICHT
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Position Filter Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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

                // Interactive Canvas Chart
                WatchPrecisionChart(
                    ratePoints = ratePoints,
                    metric = chartMetric,
                    modifier = Modifier.fillMaxWidth()
                )

                // Lagen-Vergleichstabelle
                if (statistics.positionStats.isNotEmpty()) {
                    PositionStatsCard(positionStats = statistics.positionStats)
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // 1. INFO-DIALOG: Zeigt alle Freitexte und Details beim Antippen des (I)-Buttons
    infoLogToShow?.let { pt ->
        val log = pt.log
        AlertDialog(
            onDismissRequest = { infoLogToShow = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Messpunkt-Details & Info", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Info-Freitext
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Eingegebener Info-Freitext:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (log.notes.isNotBlank()) log.notes else "(Kein Info-Freitext für diesen Messpunkt hinterlegt)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Zusatzwert (z.B. Temperatur)
                    if (log.extraValue != 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Zusatzwert (z.B. Temp.):", style = MaterialTheme.typography.bodySmall)
                            Text("${log.extraValue} °C", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    // Uhrenlage
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Uhrenlage:", style = MaterialTheme.typography.bodySmall)
                        Surface(
                            color = log.position.color.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "${log.position.germanTitle} (${log.position.code})",
                                style = MaterialTheme.typography.labelSmall,
                                color = log.position.color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // Zeitstempel
                    val fmt = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("NTP-Referenz:", style = MaterialTheme.typography.bodySmall)
                        Text(fmt.format(Date(log.ntpTimestampMs)), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Zeiger-Uhrenzeit:", style = MaterialTheme.typography.bodySmall)
                        Text(fmt.format(Date(log.watchTimestampMs)), fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Täglicher Gang:", style = MaterialTheme.typography.bodySmall)
                        Text(pt.formattedRate, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toEdit = log
                        infoLogToShow = null
                        logToEdit = toEdit
                    }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Bearbeiten")
                }
            },
            dismissButton = {
                TextButton(onClick = { infoLogToShow = null }) {
                    Text("Schließen")
                }
            }
        )
    }

    // 2. KORREKTUR-DIALOG (EDIT MEASUREMENT)
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

    // 3. LÖSCHEN-DIALOG
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
    var editedSetId by remember { mutableStateOf(log.setId) }
    var showPosDropdown by remember { mutableStateOf(false) }

    val timeFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.GERMANY) }
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

                // Messperiode (Set)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Messperiode (Set):", style = MaterialTheme.typography.bodySmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { if (editedSetId > 0) editedSetId -= 1 },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("-1", fontSize = 11.sp)
                        }
                        Text(
                            text = "Periode #${editedSetId + 1}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                        OutlinedButton(
                            onClick = { editedSetId += 1 },
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+1", fontSize = 11.sp)
                        }
                    }
                }

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

                // Info-Freitext
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it },
                    label = { Text("Info-Freitext / Notiz") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("edit_info_text")
                )

                // Zusatzwert
                OutlinedTextField(
                    value = editedExtraValue,
                    onValueChange = { editedExtraValue = it },
                    label = { Text("Zusatzwert (z.B. Temperatur °C)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val extra = editedExtraValue.toDoubleOrNull() ?: log.extraValue
                    val updated = log.copy(
                        setId = editedSetId,
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
