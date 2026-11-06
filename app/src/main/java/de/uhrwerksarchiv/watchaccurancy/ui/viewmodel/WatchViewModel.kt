package de.uhrwerksarchiv.watchaccurancy.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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

    init {
        val database = AppDatabase.getInstance(application)
        repository = WatchRepository(database)
        ntpManager = NtpManager()
    }

    val ntpStatus: StateFlow<NtpStatus> = ntpManager.status

    val watches: StateFlow<List<Watch>> = repository.allWatches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedWatchId = MutableStateFlow<Long?>(null)
    val selectedWatchId: StateFlow<Long?> = _selectedWatchId.asStateFlow()

    private val _selectedPosition = MutableStateFlow(WatchPosition.WORN)
    val selectedPosition: StateFlow<WatchPosition> = _selectedPosition.asStateFlow()

    private val _targetWatchTimeMs = MutableStateFlow(computeInitialTargetTime())
    val targetWatchTimeMs: StateFlow<Long> = _targetWatchTimeMs.asStateFlow()

    private val _chartFilterPosition = MutableStateFlow<WatchPosition?>(null)
    val chartFilterPosition: StateFlow<WatchPosition?> = _chartFilterPosition.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiMessage>()
    val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

    private val _importReport = MutableStateFlow<ParseReport?>(null)
    val importReport: StateFlow<ParseReport?> = _importReport.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

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
        viewModelScope.launch {
            watches.collect { list ->
                if (_selectedWatchId.value == null && list.isNotEmpty()) {
                    _selectedWatchId.value = list.first().id
                }
            }
        }
    }

    fun selectWatch(watchId: Long) {
        if (_selectedWatchId.value != watchId) {
            _selectedWatchId.value = watchId
            // Default to all periods or keep current
        }
    }

    fun selectPeriod(setId: Int?) {
        _selectedPeriodSetId.value = setId
    }

    fun setPosition(position: WatchPosition) {
        _selectedPosition.value = position
    }

    fun setChartFilterPosition(position: WatchPosition?) {
        _chartFilterPosition.value = position
    }

    fun syncNtpNow() {
        ntpManager.syncNow()
    }

    private fun computeInitialTargetTime(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, 1)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
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
        _targetWatchTimeMs.value = computeInitialTargetTime()
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

            // Select this period so overview immediately displays it
            _selectedPeriodSetId.value = computedSetId

            setTargetTimeToNextMinute()

            val devSec = insertedLog.deviationSeconds
            val devSign = if (devSec >= 0) "+" else ""
            val periodLabel = "Periode #${computedSetId + 1}"
            val msg = String.format(java.util.Locale.US, "Messpunkt gespeichert (%s)! Abweichung: %s%.2fs (%s)", periodLabel, devSign, devSec, position.shortLabel)
            _uiEvents.emit(UiMessage.Success(msg))

            onRecorded(insertedLog, null)
        }
    }

    fun updateMeasurementLog(updatedLog: MeasurementLog) {
        viewModelScope.launch {
            repository.updateLog(updatedLog)
            val devSec = updatedLog.deviationSeconds
            val devSign = if (devSec >= 0) "+" else ""
            val msg = String.format(java.util.Locale.US, "Messung korrigiert! Neue Abweichung: %s%.2fs (%s)", devSign, devSec, updatedLog.position.shortLabel)
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
            _selectedWatchId.value = id
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
            if (_selectedWatchId.value == watchId) {
                _selectedWatchId.value = watches.value.firstOrNull { it.id != watchId }?.id
            }
            _uiEvents.emit(UiMessage.Success("Uhr gelöscht."))
        }
    }

    fun deleteLog(logId: Long) {
        viewModelScope.launch {
            repository.deleteLogById(logId)
            _uiEvents.emit(UiMessage.Success("Messpunkt gelöscht."))
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
                if (report.watches.isNotEmpty()) {
                    _selectedWatchId.value = report.watches.first().id
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
