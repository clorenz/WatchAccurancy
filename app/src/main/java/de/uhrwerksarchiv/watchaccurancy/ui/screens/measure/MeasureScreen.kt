package de.uhrwerksarchiv.watchaccurancy.ui.screens.measure

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import de.uhrwerksarchiv.watchaccurancy.data.model.MeasurementLog
import de.uhrwerksarchiv.watchaccurancy.data.model.RateCalculator
import de.uhrwerksarchiv.watchaccurancy.data.model.Watch
import de.uhrwerksarchiv.watchaccurancy.data.model.WatchPosition
import de.uhrwerksarchiv.watchaccurancy.data.ntp.NtpStatus
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeasureScreen(
    watches: List<Watch>,
    selectedWatchId: Long?,
    selectedPosition: WatchPosition,
    targetWatchTimeMs: Long,
    ntpStatus: NtpStatus,
    existingPeriods: List<Int> = emptyList(),
    allWatchLogs: List<MeasurementLog> = emptyList(),
    presetTemperature: Double = 35.0,
    onSelectWatch: (Long) -> Unit,
    onSetPosition: (WatchPosition) -> Unit,
    onSetTemperature: (Double) -> Unit = {},
    onAdjustTargetMinute: (Int) -> Unit,
    onSetTargetToNextMinute: () -> Unit,
    onSyncNtp: () -> Unit,
    onRecordMeasurement: (notes: String, extraValue: Double, startNewPeriod: Boolean, targetSetId: Int?, onComplete: () -> Unit) -> Unit,
    onNavigateToOverview: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf("") }
    var extraValueText by remember { mutableStateOf("35") }
    var showWatchDropdown by remember { mutableStateOf(false) }
    var showPositionDropdown by remember { mutableStateOf(false) }

    // Synchronisiere voreingestellte Temperatur bei Uhrenwechsel oder Positionsanpassung
    LaunchedEffect(presetTemperature, selectedWatchId) {
        extraValueText = if (presetTemperature % 1.0 == 0.0) {
            presetTemperature.toInt().toString()
        } else {
            presetTemperature.toString()
        }
    }

    var currentLiveNtpTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentLiveNtpTime = System.currentTimeMillis() + ntpStatus.offsetMs
            delay(100)
        }
    }

    var isNewPeriod by remember { mutableStateOf(false) }
    val latestPeriodId = existingPeriods.maxOrNull() ?: 0
    var selectedPeriodId by remember(existingPeriods) { mutableStateOf(latestPeriodId) }

    val selectedWatch = watches.firstOrNull { it.id == selectedWatchId }

    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Exception) {}
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. WATCH SELECTOR
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showWatchDropdown = true }
                .testTag("watch_selector_card"),
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
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFFFA000)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Aktuelle Uhr:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = selectedWatch?.model ?: "Keine Uhr gewählt",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Uhr wählen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
                                if (watch.serialNumber.isNotBlank()) {
                                    Text("SN: ${watch.serialNumber}", style = MaterialTheme.typography.bodySmall)
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

        // 2. NTP STATUS BANNER
        NtpStatusBar(
            ntpStatus = ntpStatus,
            liveTimeMs = currentLiveNtpTime,
            onRefresh = onSyncNtp
        )

        // 3. TARGET WATCH TIME SETTER (INTELLIGENT VOREINGESTELLT)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Ziel-Uhrenzeit (Sekundenzeiger auf 12 / :00)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                val timeFmt = remember { SimpleDateFormat("HH:mm:00", Locale.GERMANY) }
                val dateFmt = remember { SimpleDateFormat("dd. MMMM yyyy", Locale.GERMANY) }

                Text(
                    text = timeFmt.format(Date(targetWatchTimeMs)),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = dateFmt.format(Date(targetWatchTimeMs)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(
                        onClick = { onAdjustTargetMinute(-1) },
                        modifier = Modifier.testTag("target_minus_1m")
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("-1 Min")
                    }

                    Button(
                        onClick = onSetTargetToNextMinute,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.testTag("target_next_min")
                    ) {
                        Text("Nächste Min")
                    }

                    OutlinedButton(
                        onClick = { onAdjustTargetMinute(1) },
                        modifier = Modifier.testTag("target_plus_1m")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+1 Min")
                    }
                }
            }
        }

        // 4. DER GROSSE ERFASSUNGSBUTTON ("JETZT!") - DIREKT UNTER DER ZIEL-UHRENZEIT
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(elevation = 12.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                )
                .border(4.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f), CircleShape)
                .clickable {
                    triggerHapticFeedback()
                    val extraVal = extraValueText.toDoubleOrNull() ?: 20.0
                    val currentWatchId = selectedWatchId
                    val willStartNew = isNewPeriod
                    val targetSet = if (willStartNew) null else selectedPeriodId

                    onRecordMeasurement(notes, extraVal, willStartNew, targetSet) {
                        if (currentWatchId != null) {
                            onNavigateToOverview(currentWatchId)
                        }
                    }
                }
                .testTag("record_measurement_big_button"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "JETZT!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Sekundenzeiger auf 12",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.3f)
                ) {
                    Text(
                        text = "Speichern & zur Übersicht →",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 5. MESS-PERIODE (START- UND ENDDATUM WIE BEI WATCHCHECK2)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNewPeriod) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val currentPeriodInfo = if (!isNewPeriod) RateCalculator.getPeriodDisplayInfo(allWatchLogs, selectedPeriodId) else null
                val periodDisplayLabel = if (isNewPeriod) "Neue Periode (ab heute) – Start / Referenz" else currentPeriodInfo?.title ?: "Periode #${selectedPeriodId + 1}"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "Messperiode:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = periodDisplayLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isNewPeriod) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    FilterChip(
                        selected = isNewPeriod,
                        onClick = { isNewPeriod = !isNewPeriod },
                        label = { Text(if (isNewPeriod) "Neue Periode aktiv" else "+ Neue Periode") },
                        leadingIcon = {
                            Icon(
                                if (isNewPeriod) Icons.Default.Check else Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        modifier = Modifier.testTag("toggle_new_period_chip")
                    )
                }

                if (isNewPeriod) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "Hinweis: Der nächste Messpunkt beginnt eine neue, separate Messreihe (z.B. nach Einregulierung oder Tragepause). Dieser Punkt dient als neuer Referenzpunkt (Gang = Referenz).",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                } else if (existingPeriods.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Zuordnen zu:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        existingPeriods.forEach { pId ->
                            val info = RateCalculator.getPeriodDisplayInfo(allWatchLogs, pId)
                            FilterChip(
                                selected = selectedPeriodId == pId,
                                onClick = { selectedPeriodId = pId },
                                label = { Text(info.title) }
                            )
                        }
                    }
                }
            }
        }

        // 6. POSITION DROPDOWN SELECTOR (UHRENLAGE MIT TEMPERATUR-SYNCHRONISATION)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "Uhrenlage / Position",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPositionDropdown = true }
                            .testTag("position_selector"),
                        shape = RoundedCornerShape(10.dp)
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
                                        .size(12.dp)
                                        .background(selectedPosition.color, CircleShape)
                                )
                                Column {
                                    Text(
                                        text = "${selectedPosition.germanTitle} (${selectedPosition.code})",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = selectedPosition.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }

                    DropdownMenu(
                        expanded = showPositionDropdown,
                        onDismissRequest = { showPositionDropdown = false }
                    ) {
                        WatchPosition.entries.forEach { pos ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(pos.color, CircleShape)
                                        )
                                        Column {
                                            Text(
                                                "${pos.germanTitle} (${pos.code})",
                                                fontWeight = if (pos == selectedPosition) FontWeight.Bold else FontWeight.Normal
                                            )
                                            Text(
                                                pos.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    onSetPosition(pos)
                                    // Automatische Temperaturanpassung: Standard 20°, bei "Am Arm" 35°
                                    val currentTemp = extraValueText.toDoubleOrNull() ?: 20.0
                                    if (pos == WatchPosition.WORN && (currentTemp == 20.0 || currentTemp == 0.0)) {
                                        extraValueText = "35"
                                        onSetTemperature(35.0)
                                    } else if (pos != WatchPosition.WORN && currentTemp == 35.0) {
                                        extraValueText = "20"
                                        onSetTemperature(20.0)
                                    }
                                    showPositionDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 7. INFO-FREITEXT & ZUSATZDATEN (TEMPERATUR VOREINGESTELLT 20° / 35°)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Info-Freitext zur Messung (optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("z.B. tagsüber getragen, nachts 12 Uhr abgelegt...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("measurement_notes_input"),
                    singleLine = false,
                    maxLines = 2
                )

                // Schnellauswahl-Chips für typische Uhren-Notizen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Am Arm getragen", "Nachts abgelegt", "Uhrenbeweger", "Kalt (<18°C)", "Warm (>24°C)").forEach { suggestion ->
                        AssistChip(
                            onClick = {
                                notes = if (notes.isBlank()) suggestion else "$notes, $suggestion"
                            },
                            label = { Text(suggestion, fontSize = 11.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = extraValueText,
                        onValueChange = {
                            extraValueText = it
                            it.toDoubleOrNull()?.let { num -> onSetTemperature(num) }
                        },
                        label = { Text("Temperatur (°C)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("measurement_extra_input")
                    )
                }

                // Schnellwahl-Chips für Temperatur (Standard 20° / Am Arm 35°)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "35°C (Am Arm)" to 35.0,
                        "20°C (Standard)" to 20.0,
                        "22°C (Zimmer)" to 22.0,
                        "18°C (Kühl)" to 18.0
                    ).forEach { (label, tempVal) ->
                        AssistChip(
                            onClick = {
                                extraValueText = tempVal.toInt().toString()
                                onSetTemperature(tempVal)
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun NtpStatusBar(
    ntpStatus: NtpStatus,
    liveTimeMs: Long,
    onRefresh: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss", Locale.GERMANY) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (ntpStatus.isSynchronized) {
                Color(0xFF10B981).copy(alpha = 0.12f)
            } else {
                Color(0xFFF59E0B).copy(alpha = 0.15f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        .background(
                            if (ntpStatus.isSynchronized) Color(0xFF10B981) else Color(0xFFF59E0B),
                            CircleShape
                        )
                )
                Column {
                    Text(
                        text = if (ntpStatus.isSynchronized) "NTP Atomzeit synchronisiert" else "NTP-Abgleich läuft...",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Atomzeit: ${timeFormatter.format(Date(liveTimeMs))} (±${ntpStatus.estimatedAccuracyMs}ms)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "NTP aktualisieren",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
