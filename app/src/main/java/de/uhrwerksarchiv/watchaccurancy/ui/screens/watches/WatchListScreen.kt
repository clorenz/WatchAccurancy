package de.uhrwerksarchiv.watchaccurancy.ui.screens.watches

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import de.uhrwerksarchiv.watchaccurancy.data.model.Watch

@Composable
fun WatchListScreen(
    watches: List<Watch>,
    selectedWatchId: Long?,
    latestMeasurementTimestamps: Map<Long, Long> = emptyMap(),
    onSelectWatch: (Long) -> Unit,
    onAddWatch: (String, String, String, String) -> Unit,
    onUpdateWatch: (Watch) -> Unit,
    onDeleteWatch: (Long) -> Unit,
    onNavigateToMeasure: (Long) -> Unit,
    onNavigateToCharts: (Long) -> Unit,
    onLoadSampleData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var watchToEdit by remember { mutableStateOf<Watch?>(null) }
    var watchToDelete by remember { mutableStateOf<Watch?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_watch_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Uhr hinzufügen")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.watches_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.watches_count_format, watches.size),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (watches.isEmpty()) {
                    OutlinedButton(
                        onClick = onLoadSampleData,
                        modifier = Modifier.testTag("load_sample_btn")
                    ) {
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.load_samples))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (watches.isEmpty()) {
                EmptyWatchesCard(
                    onAddClick = { showAddDialog = true },
                    onLoadSample = onLoadSampleData
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(watches, key = { it.id }) { watch ->
                        val isSelected = watch.id == selectedWatchId
                        val latestTimestamp = latestMeasurementTimestamps[watch.id]
                        WatchCard(
                            watch = watch,
                            isSelected = isSelected,
                            latestMeasurementMs = latestTimestamp,
                            onSelect = { onSelectWatch(watch.id) },
                            onEdit = { watchToEdit = watch },
                            onDelete = { watchToDelete = watch },
                            onMeasure = {
                                onSelectWatch(watch.id)
                                onNavigateToMeasure(watch.id)
                            },
                            onViewCharts = {
                                onSelectWatch(watch.id)
                                onNavigateToCharts(watch.id)
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val addTitle = androidx.compose.ui.res.stringResource(com.example.R.string.add_watch_title)
        val saveText = androidx.compose.ui.res.stringResource(com.example.R.string.save)
        WatchEditDialog(
            title = addTitle,
            confirmText = saveText,
            initialModel = "",
            initialSerial = "",
            initialCaliber = "",
            initialNotes = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { model, serial, caliber, notes ->
                onAddWatch(model, serial, caliber, notes)
                showAddDialog = false
            }
        )
    }

    watchToEdit?.let { watch ->
        val editTitle = androidx.compose.ui.res.stringResource(com.example.R.string.edit_watch_title)
        val updateText = androidx.compose.ui.res.stringResource(com.example.R.string.update)
        WatchEditDialog(
            title = editTitle,
            confirmText = updateText,
            initialModel = watch.model,
            initialSerial = watch.serialNumber,
            initialCaliber = watch.caliber,
            initialNotes = watch.notes,
            onDismiss = { watchToEdit = null },
            onConfirm = { model, serial, caliber, notes ->
                onUpdateWatch(
                    watch.copy(
                        model = model,
                        serialNumber = serial,
                        caliber = caliber,
                        notes = notes
                    )
                )
                watchToEdit = null
            }
        )
    }

    watchToDelete?.let { watch ->
        AlertDialog(
            onDismissRequest = { watchToDelete = null },
            title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.delete_watch_title)) },
            text = {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.delete_watch_msg, watch.model))
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteWatch(watch.id)
                        watchToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { watchToDelete = null }) {
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun WatchCard(
    watch: Watch,
    isSelected: Boolean,
    latestMeasurementMs: Long? = null,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMeasure: () -> Unit,
    onViewCharts: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("watch_card_${watch.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.5.dp, Color(0xFFFFA000)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }
                    Column {
                        Text(
                            text = watch.model,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (watch.serialNumber.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "SN: ${watch.serialNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (watch.caliber.isNotBlank()) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Kaliber: ${watch.caliber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // Letztes Messdatum
                        Spacer(modifier = Modifier.height(3.dp))
                        if (latestMeasurementMs != null && latestMeasurementMs > 0L) {
                            val dateFmt = remember { java.text.SimpleDateFormat("dd.MM.yy HH:mm", java.util.Locale.getDefault()) }
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.last_measurement_format, dateFmt.format(java.util.Date(latestMeasurementMs))),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = Color(0xFFFFA000),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = androidx.compose.ui.res.stringResource(com.example.R.string.no_measurements_yet),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.edit_watch_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.delete),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (watch.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = watch.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onMeasure,
                    modifier = Modifier.weight(1f).testTag("watch_measure_btn_${watch.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.btn_measure))
                }

                OutlinedButton(
                    onClick = onViewCharts,
                    modifier = Modifier.weight(1f).testTag("watch_chart_btn_${watch.id}")
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.btn_chart))
                }
            }
        }
    }
}

@Composable
private fun EmptyWatchesCard(
    onAddClick: () -> Unit,
    onLoadSample: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Watch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.empty_watches_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.empty_watches_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().testTag("empty_add_watch_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.add_first_watch))
            }

            OutlinedButton(
                onClick = onLoadSample,
                modifier = Modifier.fillMaxWidth().testTag("empty_load_sample_btn")
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.load_samples_subtitle))
            }
        }
    }
}

@Composable
private fun WatchEditDialog(
    title: String,
    confirmText: String,
    initialModel: String,
    initialSerial: String,
    initialCaliber: String,
    initialNotes: String,
    onDismiss: () -> Unit,
    onConfirm: (model: String, serial: String, caliber: String, notes: String) -> Unit
) {
    var model by remember { mutableStateOf(initialModel) }
    var serial by remember { mutableStateOf(initialSerial) }
    var caliber by remember { mutableStateOf(initialCaliber) }
    var notes by remember { mutableStateOf(initialNotes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.input_watch_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_watch_model")
                )
                OutlinedTextField(
                    value = caliber,
                    onValueChange = { caliber = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.input_watch_caliber)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_watch_caliber")
                )
                OutlinedTextField(
                    value = serial,
                    onValueChange = { serial = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.input_watch_serial)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_watch_serial")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.input_watch_notes)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("input_watch_notes")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(model, serial, caliber, notes) },
                enabled = model.isNotBlank(),
                modifier = Modifier.testTag("confirm_watch_btn")
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.cancel))
            }
        }
    )
}
