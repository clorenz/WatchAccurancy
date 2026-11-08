package de.uhrwerksarchiv.watchaccurancy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.WatchAccurancyTheme
import de.uhrwerksarchiv.watchaccurancy.ui.screens.backup.BackupScreen
import de.uhrwerksarchiv.watchaccurancy.ui.screens.charts.ChartsScreen
import de.uhrwerksarchiv.watchaccurancy.ui.screens.charts.OverviewViewMode
import de.uhrwerksarchiv.watchaccurancy.ui.screens.measure.MeasureScreen
import de.uhrwerksarchiv.watchaccurancy.ui.screens.watches.WatchListScreen
import de.uhrwerksarchiv.watchaccurancy.ui.viewmodel.UiMessage
import de.uhrwerksarchiv.watchaccurancy.ui.viewmodel.WatchViewModel

enum class ScreenTab(val title: String, val icon: ImageVector, val tag: String) {
    WATCHES("Uhren", Icons.Default.Watch, "tab_watches"),
    MEASURE("Messung", Icons.Default.PlayArrow, "tab_measure"),
    CHARTS("Übersicht", Icons.Default.TableChart, "tab_charts"),
    BACKUP("Backup", Icons.Default.Sync, "tab_backup")
}

class MainActivity : ComponentActivity() {

    private val viewModel: WatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            WatchAccurancyTheme(themeMode = themeMode) {
                WatchAccurancyApp(viewModel = viewModel, currentThemeMode = themeMode)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchAccurancyApp(viewModel: WatchViewModel, currentThemeMode: AppThemeMode) {
    var currentTab by remember { mutableStateOf(ScreenTab.WATCHES) }
    var overviewViewMode by remember { mutableStateOf(OverviewViewMode.TABLE) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is UiMessage.Success -> snackbarHostState.showSnackbar(event.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

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
    val distinctPeriods by viewModel.distinctPeriods.collectAsStateWithLifecycle()
    val selectedPeriodSetId by viewModel.selectedPeriodSetId.collectAsStateWithLifecycle()
    val selectedWatchLogs by viewModel.selectedWatchLogs.collectAsStateWithLifecycle()
    val presetTemperature by viewModel.presetTemperature.collectAsStateWithLifecycle()
    val latestMeasurementTimestamps by viewModel.latestMeasurementTimestamps.collectAsStateWithLifecycle()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth > 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                val isDark = currentThemeMode == AppThemeMode.DARK || (currentThemeMode == AppThemeMode.SYSTEM && isSystemInDarkTheme())
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White,
                                border = BorderStroke(1.5.dp, Color(0xFFFFA000)),
                                shadowElevation = 1.dp,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.icon),
                                    contentDescription = null,
                                    modifier = Modifier.padding(2.5.dp)
                                )
                            }
                            Text(
                                text = "WatchAccurancy",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                color = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF212121)
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isDark) {
                                    if (ntpStatus.isSynchronized) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                } else {
                                    Color.Black.copy(alpha = 0.12f)
                                }
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
                                        color = if (isDark) (if (ntpStatus.isSynchronized) Color(0xFF10B981) else Color(0xFFF59E0B)) else Color(0xFF212121)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = if (isDark) MaterialTheme.colorScheme.surface else Color(0xFFFFA000),
                        titleContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF212121),
                        actionIconContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color(0xFF212121)
                    ),
                    actions = {
                        var showThemeMenu by remember { mutableStateOf(false) }

                        Box {
                            IconButton(
                                onClick = { showThemeMenu = true },
                                modifier = Modifier.testTag("theme_mode_button")
                            ) {
                                Icon(
                                    imageVector = when (currentThemeMode) {
                                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                                        AppThemeMode.DARK -> Icons.Default.DarkMode
                                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = "Design-Modus wechseln (aktuell: ${currentThemeMode.title})"
                                )
                            }

                            DropdownMenu(
                                expanded = showThemeMenu,
                                onDismissRequest = { showThemeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Heller Modus") },
                                    leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null) },
                                    trailingIcon = if (currentThemeMode == AppThemeMode.LIGHT) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        viewModel.setThemeMode(AppThemeMode.LIGHT)
                                        showThemeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Dunkler Modus") },
                                    leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                                    trailingIcon = if (currentThemeMode == AppThemeMode.DARK) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        viewModel.setThemeMode(AppThemeMode.DARK)
                                        showThemeMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Systemstandard") },
                                    leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null) },
                                    trailingIcon = if (currentThemeMode == AppThemeMode.SYSTEM) {
                                        { Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        viewModel.setThemeMode(AppThemeMode.SYSTEM)
                                        showThemeMenu = false
                                    }
                                )
                            }
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
                                latestMeasurementTimestamps = latestMeasurementTimestamps,
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
                                    overviewViewMode = OverviewViewMode.CHART
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
                                existingPeriods = distinctPeriods,
                                allWatchLogs = selectedWatchLogs,
                                presetTemperature = presetTemperature,
                                onSelectWatch = { viewModel.selectWatch(it) },
                                onSetPosition = { viewModel.setPosition(it) },
                                onSetTemperature = { viewModel.setTemperature(it) },
                                onAdjustTargetMinute = { viewModel.adjustTargetMinute(it) },
                                onSetTargetToNextMinute = { viewModel.setTargetTimeToNextMinute() },
                                onSyncNtp = { viewModel.syncNtpNow() },
                                onRecordMeasurement = { notes, extraVal, startNewPeriod, targetSetId, onComplete ->
                                    viewModel.recordMeasurement(notes, extraVal, startNewPeriod, targetSetId) { _, _ ->
                                        onComplete()
                                    }
                                },
                                onNavigateToOverview = { watchId ->
                                    viewModel.selectWatch(watchId)
                                    overviewViewMode = OverviewViewMode.TABLE
                                    currentTab = ScreenTab.CHARTS
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
                                distinctPeriods = distinctPeriods,
                                selectedPeriodSetId = selectedPeriodSetId,
                                allWatchLogs = selectedWatchLogs,
                                onSelectWatch = { viewModel.selectWatch(it) },
                                onSelectPeriod = { viewModel.selectPeriod(it) },
                                onFilterPosition = { viewModel.setChartFilterPosition(it) },
                                onUpdateLog = { updatedLog ->
                                    viewModel.updateMeasurementLog(updatedLog)
                                },
                                onDeleteLog = { viewModel.deleteLog(it) },
                                viewMode = overviewViewMode,
                                onViewModeChanged = { overviewViewMode = it }
                            )
                        }

                        ScreenTab.BACKUP -> {
                            BackupScreen(
                                importReport = importReport,
                                isImporting = isImporting,
                                currentThemeMode = currentThemeMode,
                                onSelectThemeMode = { viewModel.setThemeMode(it) },
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
