package com.example.watchcheck.ui.screens.measure

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.watchcheck.data.model.MeasurementLog
import com.example.watchcheck.data.model.RateCalculator
import com.example.watchcheck.data.model.Watch
import com.example.watchcheck.data.model.WatchPosition
import com.example.watchcheck.data.ntp.NtpStatus
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
    onSelectWatch: (Long) -> Unit,
    onSetPosition: (WatchPosition) -> Unit,
    onAdjustTargetMinute: (Int) -> Unit,
    onSetTargetToNextMinute: () -> Unit,
    onSyncNtp: () -> Unit,
    onRecordMeasurement: (String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf("") }
    var extraValueText by remember { mutableStateOf("0") }
    var showWatchDropdown by remember { mutableStateOf(false) }
    var showPositionDropdown by remember { mutableStateOf(false) }
    var showExtraOptions by remember { mutableStateOf(false) }

    // Live Atomic NTP Clock
    var currentLiveNtpTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentLiveNtpTime = System.currentTimeMillis() + ntpStatus.offsetMs
            delay(100)
        }
    }

    val selectedWatch = watches.firstOrNull { it.id == selectedWatchId }

    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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
                    Icon(
                        Icons.Default.Watch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
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

        // 2. NTP ATOMIC STATUS BANNER
        NtpStatusBar(
            ntpStatus = ntpStatus,
            liveTimeMs = currentLiveNtpTime,
            onRefresh = onSyncNtp
        )

        // 3. TARGET WATCH TIME SETTER
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

        // 4. POSITION DROPDOWN SELECTOR (UHRENLAGE)
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
                                    showPositionDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 5. THE BIG MEASUREMENT BUTTON
        Spacer(modifier = Modifier.height(4.dp))

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
                    val extraVal = extraValueText.toDoubleOrNull() ?: 0.0
                    onRecordMeasurement(notes, extraVal)
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
                        text = "Atomgenauer Abgleich",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 6. EXPANDABLE EXTRA OPTIONS (NOTIZ, ZUSATZWERT)
        TextButton(
            onClick = { showExtraOptions = !showExtraOptions },
            modifier = Modifier.testTag("toggle_extra_options")
        ) {
            Text(if (showExtraOptions) "Optionale Felder ausblenden ▲" else "Notiz / Zusatzwert hinzufügen ▼")
        }

        AnimatedVisibility(visible = showExtraOptions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notiz zum Messpunkt (z.B. Nachts Nachttisch)") },
                        modifier = Modifier.fillMaxWidth().testTag("measurement_notes_input")
                    )
                    OutlinedTextField(
                        value = extraValueText,
                        onValueChange = { extraValueText = it },
                        label = { Text("Zusatzwert (z.B. Temperatur °C)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("measurement_extra_input")
                    )
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val dotColor by animateColorAsState(
                    targetValue = if (ntpStatus.isSynchronized) Color(0xFF10B981) else Color(0xFFF59E0B),
                    label = "ntpDot"
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(dotColor, CircleShape)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (ntpStatus.isSynchronized) "NTP Atomzeit aktiv" else "NTP Sync ausstehend",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "±${ntpStatus.estimatedAccuracyMs}ms",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    val timeFmt = remember { SimpleDateFormat("HH:mm:ss.SS", Locale.GERMANY) }
                    Text(
                        text = "Atom: ${timeFmt.format(Date(liveTimeMs))} (${ntpStatus.serverUsed})",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier.testTag("sync_ntp_btn")
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "NTP aktualisieren",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
