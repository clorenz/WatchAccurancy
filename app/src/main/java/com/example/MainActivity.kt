package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.WatchAccurancyTheme
import com.example.watchcheck.ui.screens.backup.BackupScreen
import com.example.watchcheck.ui.screens.charts.ChartsScreen
import com.example.watchcheck.ui.screens.measure.MeasureScreen
import com.example.watchcheck.ui.screens.watches.WatchListScreen
import com.example.watchcheck.ui.viewmodel.UiMessage
import com.example.watchcheck.ui.viewmodel.WatchViewModel

enum class ScreenTab(val title: String, val icon: ImageVector, val tag: String) {
    WATCHES("Uhren", Icons.Default.Watch, "tab_watches"),
    MEASURE("Messung", Icons.Default.PlayArrow, "tab_measure"),
    CHARTS("Diagramme", Icons.Default.ShowChart, "tab_charts"),
    BACKUP("Backup", Icons.Default.Sync, "tab_backup")
}

class MainActivity : ComponentActivity() {

    private val viewModel: WatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            WatchAccurancyTheme {
                WatchAccurancyApp(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchAccurancyApp(viewModel: WatchViewModel) {
    var currentTab by remember { mutableStateOf(ScreenTab.WATCHES) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Listen to UI messages
    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(event.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    // Android Back button handling: return to Watches tab if on other tabs
    BackHandler(enabled = currentTab != ScreenTab.WATCHES) {
        currentTab = ScreenTab.WATCHES
    }

    val watches by viewModel.watches.collectAsStateWithLifecycle()
    val selectedWatchId by viewModel.selectedWatchId.collectAsStateWithLifecycle()
    val selectedPosition by viewModel.selectedPosition.collectAsStateWithLifecycle()
    val targetWatchTimeMs by viewModel.targetWatchTimeMs.collectAsStateWithLifecycle()
    val ntpStatus by viewModel.ntpStatus.collectAsStateWithLifecycle()
    val ratePoints by viewModel.ratePoints.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val chartFilterPosition by viewModel.chartFilterPosition.collectAsStateWithLifecycle()
    val importReport by viewModel.importReport.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "WatchAccurancy",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge
                            )
                            // Mini badge for NTP sync status
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (ntpStatus.isSynchronized) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(
                                                if (ntpStatus.isSynchronized) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                CircleShape
                                            )
                                    )
                                    Text(
                                        text = if (ntpStatus.isSynchronized) "NTP" else "Sync",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ntpStatus.isSynchronized) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = { viewModel.syncNtpNow() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "NTP abgleichen")
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        ScreenTab.entries.forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag(tab.tag),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        ScreenTab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        ScreenTab.WATCHES -> {
                            WatchListScreen(
                                watches = watches,
                                selectedWatchId = selectedWatchId,
                                onSelectWatch = { viewModel.selectWatch(it) },
                                onAddWatch = { model, serial, caliber, notes ->
                                    viewModel.addWatch(model, serial, caliber, notes)
                                },
                                onUpdateWatch = { viewModel.updateWatch(it) },
                                onDeleteWatch = { viewModel.deleteWatch(it) },
                                onNavigateToMeasure = {
                                    viewModel.selectWatch(it)
                                    currentTab = ScreenTab.MEASURE
                                },
                                onNavigateToCharts = {
                                    viewModel.selectWatch(it)
                                    currentTab = ScreenTab.CHARTS
                                },
                                onLoadSampleData = {
                                    viewModel.loadSampleData()
                                    viewModel.executeImport(clearExisting = false)
                                }
                            )
                        }

                        ScreenTab.MEASURE -> {
                            MeasureScreen(
                                watches = watches,
                                selectedWatchId = selectedWatchId,
                                selectedPosition = selectedPosition,
                                targetWatchTimeMs = targetWatchTimeMs,
                                ntpStatus = ntpStatus,
                                onSelectWatch = { viewModel.selectWatch(it) },
                                onSetPosition = { viewModel.setPosition(it) },
                                onAdjustTargetMinute = { viewModel.adjustTargetMinute(it) },
                                onSetTargetToNextMinute = { viewModel.setTargetTimeToNextMinute() },
                                onSyncNtp = { viewModel.syncNtpNow() },
                                onRecordMeasurement = { notes, extraVal ->
                                    viewModel.recordMeasurement(notes, extraVal)
                                }
                            )
                        }

                        ScreenTab.CHARTS -> {
                            ChartsScreen(
                                watches = watches,
                                selectedWatchId = selectedWatchId,
                                ratePoints = ratePoints,
                                statistics = statistics,
                                filterPosition = chartFilterPosition,
                                onSelectWatch = { viewModel.selectWatch(it) },
                                onFilterPosition = { viewModel.setChartFilterPosition(it) },
                                onDeleteLog = { viewModel.deleteLog(it) }
                            )
                        }

                        ScreenTab.BACKUP -> {
                            BackupScreen(
                                importReport = importReport,
                                isImporting = isImporting,
                                onCheckBackupText = { viewModel.checkBackupText(it) },
                                onExecuteImport = { clearExisting ->
                                    viewModel.executeImport(clearExisting)
                                },
                                onClearReport = { viewModel.clearImportReport() },
                                onExportBackup = { viewModel.exportDataString() },
                                onLoadSampleData = { viewModel.loadSampleData() }
                            )
                        }
                    }
                }
            }
        }
    }
}
