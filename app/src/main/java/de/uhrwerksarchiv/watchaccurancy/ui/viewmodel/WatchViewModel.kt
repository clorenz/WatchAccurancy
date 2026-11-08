package de.uhrwerksarchiv.watchaccurancy.ui.viewmodel

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ui.theme.AppThemeMode
import de.uhrwerksarchiv.watchaccurancy.data.backup.BackupParser
import de.uhrwerksarchiv.watchaccurancy.data.backup.ParseReport
import de.uhrwerksarchiv.watchaccurancy.data.local.AppDatabase
import de.uhrwerksarchiv.watchaccurancy.data.model.MeasurementLog
import de.uhrwerksarchiv.watchaccurancy.data.model.RateCalculator
import de.uhrwerksarchiv.watchaccurancy.data.model.RatePoint
import de.uhrwerksarchiv.watchaccurancy.data.model.Watch
import de.uhrwerksarchiv.watchaccurancy.data.model.WatchPosition
import de.uhrwerksarchiv.watchaccurancy.data.model.WatchStatistics
import de.uhrwerksarchiv.watchaccurancy.data.ntp.NtpManager
import de.uhrwerksarchiv.watchaccurancy.data.ntp.NtpStatus
import de.uhrwerksarchiv.watchaccurancy.data.repository.WatchRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface UiMessage {
    data class Success(val message: String) : UiMessage
    data class Error(val message: String) : UiMessage
}

class WatchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WatchRepository
    val ntpManager: NtpManager
    private val themePrefs = application.getSharedPreferences("watchaccurancy_theme_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        when (themePrefs.getString("theme_mode", "LIGHT")) {
            "DARK" -> AppThemeMode.DARK
            "SYSTEM" -> AppThemeMode.SYSTEM
            else -> AppThemeMode.LIGHT
        }
    )
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: AppThemeMode) {
        _themeMode.value = mode
        themePrefs.edit().putString("theme_mode", mode.name).apply()
        viewModelScope.launch {
            _uiEvents.emit(UiMessage.Success("Design: ${mode.title} aktiviert"))
        }
    }

    fun toggleThemeMode() {
        val next = when (_themeMode.value) {
            AppThemeMode.LIGHT -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.SYSTEM -> AppThemeMode.LIGHT
        }
        setThemeMode(next)
    }

    init {
        val database = AppDatabase.getInstance(application)
        repository = WatchRepository(database)
        ntpManager = NtpManager()
    }

    val ntpStatus: StateFlow<NtpStatus> = ntpManager.status

    // Sortierung der Uhren: Die Uhr mit dem aktuellsten Messdatum an oberster Stelle,
    // restliche Uhren absteigend nach letztem Messdatum (Uhren ohne Messungen danach nach createdAt absteigend)
    val watches: StateFlow<List<Watch>> = repository.allWatches
        .combine(repository.allLogs) { watchList, logsList ->
            val latestTimestampPerWatch = logsList.groupBy { it.watchId }
                .mapValues { entry -> entry.value.maxOfOrNull { it.ntpTimestampMs } ?: 0L }
            watchList.sortedWith(
                compareByDescending<Watch> { latestTimestampPerWatch[it.id] ?: 0L }
                    .thenByDescending { it.createdAt }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Map von watchId -> neuester ntpTimestampMs für schnelle Abfragen und UI-Labels
    val latestMeasurementTimestamps: StateFlow<Map<Long, Long>> = repository.allLogs
        .combine(flowOf(Unit)) { logsList, _ ->
            logsList.groupBy { it.watchId }
                .mapValues { entry -> entry.value.maxOfOrNull { it.ntpTimestampMs } ?: 0L }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _selectedWatchId = MutableStateFlow<Long?>(null)
    val selectedWatchId: StateFlow<Long?> = _selectedWatchId.asStateFlow()

    private val _selectedPosition = MutableStateFlow(WatchPosition.WORN)
    val selectedPosition: StateFlow<WatchPosition> = _selectedPosition.asStateFlow()

    private val _presetTemperature = MutableStateFlow(35.0)
    val presetTemperature: StateFlow<Double> = _presetTemperature.asStateFlow()

    private val _targetWatchTimeMs = MutableStateFlow(computeInitialFallbackTargetTime())
    val targetWatchTimeMs: StateFlow<Long> = _targetWatchTimeMs.asStateFlow()

    private val _chartFilterPosition = MutableStateFlow<WatchPosition?>(null)
    val chartFilterPosition: StateFlow<WatchPosition?> = _chartFilterPosition.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiMessage>()
    val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

    private val _importReport = MutableStateFlow<ParseReport?>(null)
    val importReport: StateFlow<ParseReport?> = _importReport.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    // Speichert pro Uhr die gewählte Periode; standardmäßig wird immer die aktuellste/letzte gewählt
    private val selectedPeriodPerWatch = mutableMapOf<Long, Int?>()

    private val _selectedPeriodSetId = MutableStateFlow<Int?>(null)
    val selectedPeriodSetId: StateFlow<Int?> = _selectedPeriodSetId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val selectedWatchLogs: StateFlow<List<MeasurementLog>> = _selectedWatchId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getLogsForWatch(id)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val distinctPeriods: StateFlow<List<Int>> = selectedWatchLogs
        .combine(flowOf(Unit)) { logs, _ ->
            logs.map { it.setId }.distinct().sorted()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val periodFilteredLogs: StateFlow<List<MeasurementLog>> = selectedWatchLogs
        .combine(_selectedPeriodSetId) { logs, periodId ->
            if (periodId == null) logs else logs.filter { it.setId == periodId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val ratePoints: StateFlow<List<RatePoint>> = periodFilteredLogs
        .combine(_chartFilterPosition) { logs, filterPos ->
            val allPoints = RateCalculator.computeRatePoints(logs)
            if (filterPos == null) {
                allPoints
            } else {
                allPoints.filter { it.log.position == filterPos }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val statistics: StateFlow<WatchStatistics> = periodFilteredLogs
        .combine(flowOf(Unit)) { logs, _ ->
            RateCalculator.computeStatistics(logs)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WatchStatistics(
                totalMeasurements = 0,
                averageRatePerDay = null,
                minRatePerDay = null,
                maxRatePerDay = null,
                totalDriftSeconds = null,
                totalObservationDays = null,
                positionStats = emptyMap()
            )
        )

    init {
        // Initial-Selektion der ersten Uhr
        viewModelScope.launch {
            watches.collect { list ->
                if (_selectedWatchId.value == null && list.isNotEmpty()) {
                    selectWatch(list.first().id)
                }
            }
        }

        // Standardmäßig bei jeder Uhr die letzte/aktuellste Periode auswählen
        viewModelScope.launch {
            selectedWatchLogs.collect { logs ->
                val currentWatch = _selectedWatchId.value
                if (currentWatch != null) {
                    if (!selectedPeriodPerWatch.containsKey(currentWatch)) {
                        val latestPeriod = logs.maxByOrNull { it.ntpTimestampMs }?.setId
                        _selectedPeriodSetId.value = latestPeriod
                        if (latestPeriod != null) {
                            selectedPeriodPerWatch[currentWatch] = latestPeriod
                        }
                    } else {
                        _selectedPeriodSetId.value = selectedPeriodPerWatch[currentWatch]
                    }
                }
            }
        }
    }

    fun selectWatch(watchId: Long) {
        _selectedWatchId.value = watchId
        if (selectedPeriodPerWatch.containsKey(watchId)) {
            _selectedPeriodSetId.value = selectedPeriodPerWatch[watchId]
        }
        prepareMeasurePresetForWatch(watchId)
    }

    fun selectPeriod(setId: Int?) {
        _selectedPeriodSetId.value = setId
        val currentWatch = _selectedWatchId.value
        if (currentWatch != null) {
            selectedPeriodPerWatch[currentWatch] = setId
        }
    }

    /**
     * Stellt für die gewählte Uhr die Werte der letzten Messung vorein:
     * - Ziel-Uhrzeit angepasst an bisherige Abweichung (so dass der Nutzer nicht manuell kurbeln muss)
     * - Letzte Position
     * - Letzte Temperatur (Standard 20° bzw. 35° bei "Am Arm")
     */
    fun prepareMeasurePresetForWatch(watchId: Long) {
        viewModelScope.launch {
            val logs = repository.getLogsForWatchList(watchId)
            val lastLog = logs.maxByOrNull { it.ntpTimestampMs }

            // 1. Intelligente Ziel-Uhrzeit
            _targetWatchTimeMs.value = computeIntelligentTargetTime(lastLog)

            // 2. Position
            val targetPosition = lastLog?.position ?: WatchPosition.WORN
            _selectedPosition.value = targetPosition

            // 3. Temperatur (Standard 20°, bei WORN 35°)
            val targetTemp = if (lastLog != null && lastLog.extraValue != 0.0) {
                lastLog.extraValue
            } else {
                if (targetPosition == WatchPosition.WORN) 35.0 else 20.0
            }
            _presetTemperature.value = targetTemp
        }
    }

    fun setPosition(position: WatchPosition) {
        val oldPosition = _selectedPosition.value
        _selectedPosition.value = position

        // Standardwert für die Temperatur ist 20°, außer wenn "Am Arm" gewählt wird, dann sind es 35°.
        if (position == WatchPosition.WORN && (_presetTemperature.value == 20.0 || _presetTemperature.value == 0.0)) {
            _presetTemperature.value = 35.0
        } else if (position != WatchPosition.WORN && _presetTemperature.value == 35.0) {
            _presetTemperature.value = 20.0
        }
    }

    fun setTemperature(temp: Double) {
        _presetTemperature.value = temp
    }

    fun setChartFilterPosition(position: WatchPosition?) {
        _chartFilterPosition.value = position
    }

    fun syncNtpNow() {
        ntpManager.syncNow()
    }

    private fun computeInitialFallbackTargetTime(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 1)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Berechnet die wahrscheinlichste Ziel-Uhrenzeit (:00 Sekunde) auf dem Zifferblatt der Uhr:
     * Wenn die Uhr z.B. 2 Minuten vorgeht (Delta t = +120s), liegt die Zeigerzeit auch 2 Minuten vor
     * der Atomzeit. Der nächste Sekundenzeiger-Nulldurchgang (:00) wird somit auf der Zeigerzeit
     * ermittelt, sodass dem Nutzer mindestens 15 bis 75 Sekunden Reaktionszeit bleiben.
     */
    fun computeIntelligentTargetTime(lastLog: MeasurementLog?): Long {
        val currentAtomicTime = ntpManager.getAtomicTimeForEvent(SystemClock.elapsedRealtime())
        val lastDeviationMs = if (lastLog != null) {
            lastLog.watchTimestampMs - lastLog.ntpTimestampMs
        } else {
            0L
        }

        // Geschätzte aktuelle Zeigerzeit auf der mechanischen Uhr
        val estimatedWatchTimeNow = currentAtomicTime + lastDeviationMs

        val cal = Calendar.getInstance().apply {
            timeInMillis = estimatedWatchTimeNow
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MINUTE, 1)
        }

        // Wenn bis zum nächsten vollen Zeigerminutensprung weniger als 15 Sekunden verbleiben,
        // geben wir dem Nutzer die darauffolgende Minute für entspannte Vorbereitung.
        val timeRemainingMs = cal.timeInMillis - estimatedWatchTimeNow
        if (timeRemainingMs < 15_000L) {
            cal.add(Calendar.MINUTE, 1)
        }
        return cal.timeInMillis
    }

    fun adjustTargetMinute(deltaMinutes: Int) {
        val cal = Calendar.getInstance().apply {
            timeInMillis = _targetWatchTimeMs.value
            add(Calendar.MINUTE, deltaMinutes)
        }
        _targetWatchTimeMs.value = cal.timeInMillis
    }

    fun setTargetTimeToNextMinute() {
        val logs = selectedWatchLogs.value
        val lastLog = logs.maxByOrNull { it.ntpTimestampMs }
        _targetWatchTimeMs.value = computeIntelligentTargetTime(lastLog)
    }

    fun setCustomTargetTime(timestampMs: Long) {
        _targetWatchTimeMs.value = timestampMs
    }

    fun recordMeasurement(
        notes: String = "",
        extraValue: Double = 0.0,
        startNewPeriod: Boolean = false,
        targetSetId: Int? = null,
        onRecorded: (MeasurementLog, RatePoint?) -> Unit = { _, _ -> }
    ) {
        val watchId = _selectedWatchId.value
        if (watchId == null) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage.Error("Bitte zuerst eine Uhr auswählen!"))
            }
            return
        }

        val tapElapsedRealtime = SystemClock.elapsedRealtime()
        val ntpTime = ntpManager.getAtomicTimeForEvent(tapElapsedRealtime)
        val watchTime = _targetWatchTimeMs.value
        val position = _selectedPosition.value

        viewModelScope.launch {
            val currentLogs = repository.getLogsForWatchList(watchId)
            val existingSetIds = currentLogs.map { it.setId }.distinct().sorted()
            val currentMaxSetId = existingSetIds.maxOrNull() ?: 0

            val computedSetId = when {
                startNewPeriod -> if (currentLogs.isEmpty()) 0 else currentMaxSetId + 1
                targetSetId != null -> targetSetId
                else -> currentMaxSetId
            }

            val log = MeasurementLog(
                watchId = watchId,
                setId = computedSetId,
                ntpTimestampMs = ntpTime,
                watchTimestampMs = watchTime,
                positionCode = position.code,
                extraValue = extraValue,
                notes = notes
            )
            val newId = repository.insertLog(log)
            val insertedLog = log.copy(id = newId)

            // Aktuelle Periode merken und auswählen
            _selectedPeriodSetId.value = computedSetId
            selectedPeriodPerWatch[watchId] = computedSetId

            // Nachfolgende Zielzeit, Position und Temperatur direkt aus dieser neuen Messung ableiten
            _targetWatchTimeMs.value = computeIntelligentTargetTime(insertedLog)
            _selectedPosition.value = insertedLog.position
            if (insertedLog.extraValue != 0.0) {
                _presetTemperature.value = insertedLog.extraValue
            }

            val devSec = insertedLog.deviationSeconds
            val devSign = if (devSec >= 0) "+" else ""
            val allWithNew = currentLogs + insertedLog
            val periodInfo = RateCalculator.getPeriodDisplayInfo(allWithNew, computedSetId)
            val msg = String.format(
                java.util.Locale.US,
                "Messpunkt gespeichert (%s)! Abweichung: %s%.2fs (%s)",
                periodInfo.title,
                devSign,
                devSec,
                position.shortLabel
            )
            _uiEvents.emit(UiMessage.Success(msg))

            onRecorded(insertedLog, null)
        }
    }

    fun updateMeasurementLog(updatedLog: MeasurementLog) {
        viewModelScope.launch {
            repository.updateLog(updatedLog)
            val devSec = updatedLog.deviationSeconds
            val devSign = if (devSec >= 0) "+" else ""
            val msg = String.format(
                java.util.Locale.US,
                "Messung korrigiert! Neue Abweichung: %s%.2fs (%s)",
                devSign,
                devSec,
                updatedLog.position.shortLabel
            )
            _uiEvents.emit(UiMessage.Success(msg))
        }
    }

    fun addWatch(model: String, serialNumber: String = "", caliber: String = "", notes: String = "") {
        if (model.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage.Error("Name/Modell der Uhr darf nicht leer sein."))
            }
            return
        }
        viewModelScope.launch {
            val newWatch = Watch(
                model = model.trim(),
                serialNumber = serialNumber.trim(),
                caliber = caliber.trim(),
                notes = notes.trim(),
                createdAt = System.currentTimeMillis()
            )
            val id = repository.insertWatch(newWatch)
            selectWatch(id)
            _uiEvents.emit(UiMessage.Success("Uhr '${newWatch.model}' hinzugefügt."))
        }
    }

    fun updateWatch(watch: Watch) {
        viewModelScope.launch {
            repository.updateWatch(watch)
            _uiEvents.emit(UiMessage.Success("Uhr aktualisiert."))
        }
    }

    fun deleteWatch(watchId: Long) {
        viewModelScope.launch {
            repository.deleteWatchById(watchId)
            selectedPeriodPerWatch.remove(watchId)
            if (_selectedWatchId.value == watchId) {
                val nextWatch = watches.value.firstOrNull { it.id != watchId }
                if (nextWatch != null) {
                    selectWatch(nextWatch.id)
                } else {
                    _selectedWatchId.value = null
                    _selectedPeriodSetId.value = null
                }
            }
            _uiEvents.emit(UiMessage.Success("Uhr gelöscht."))
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            val watchId = _selectedWatchId.value
            val existing = if (watchId != null) repository.getLogsForWatchList(watchId) else emptyList()
            val target = existing.firstOrNull { it.id == logId }
            val timeStr = if (target != null) RateCalculator.formatDateTimeWithSeconds(target.ntpTimestampMs) else ""

            repository.deleteLogById(logId)
            val msg = if (timeStr.isNotBlank()) {
                "Logeintrag vom $timeStr erfolgreich gelöscht."
            } else {
                "Messpunkt erfolgreich gelöscht."
            }
            _uiEvents.emit(UiMessage.Success(msg))
        }
    }

    fun checkBackupText(text: String) {
        val report = BackupParser.parse(text)
        _importReport.value = report
    }

    fun clearImportReport() {
        _importReport.value = null
    }

    fun executeImport(clearExisting: Boolean = false) {
        val report = _importReport.value ?: return
        if (!report.isValid && report.watches.isEmpty() && report.logs.isEmpty()) {
            viewModelScope.launch {
                _uiEvents.emit(UiMessage.Error("Keine gültigen Uhren oder Logs zum Importieren vorhanden."))
            }
            return
        }

        viewModelScope.launch {
            _isImporting.value = true
            val res = repository.importBackup(report, clearExisting)
            _isImporting.value = false

            if (res.isSuccess) {
                val (wCount, lCount) = res.getOrThrow()
                _importReport.value = null
                selectedPeriodPerWatch.clear()
                if (report.watches.isNotEmpty()) {
                    selectWatch(report.watches.first().id)
                }
                _uiEvents.emit(UiMessage.Success("Erfolgreich importiert: $wCount Uhren, $lCount Messpunkte!"))
            } else {
                _uiEvents.emit(UiMessage.Error("Fehler beim Import: ${res.exceptionOrNull()?.localizedMessage}"))
            }
        }
    }

    suspend fun exportDataString(): String {
        return repository.exportBackup()
    }

    fun loadSampleData() {
        checkBackupText(BackupParser.SAMPLE_BACKUP_DATA)
    }
}
