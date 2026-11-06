package de.uhrwerksarchiv.watchaccurancy.data.repository

import androidx.room.withTransaction
import de.uhrwerksarchiv.watchaccurancy.data.backup.BackupParser
import de.uhrwerksarchiv.watchaccurancy.data.backup.ParseReport
import de.uhrwerksarchiv.watchaccurancy.data.local.AppDatabase
import de.uhrwerksarchiv.watchaccurancy.data.model.MeasurementLog
import de.uhrwerksarchiv.watchaccurancy.data.model.Watch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WatchRepository(
    private val database: AppDatabase
) {
    private val watchDao = database.watchDao()
    private val logDao = database.measurementLogDao()

    val allWatches: Flow<List<Watch>> = watchDao.getAllWatches()
    val allLogs: Flow<List<MeasurementLog>> = logDao.getAllLogs()

    fun getLogsForWatch(watchId: Long): Flow<List<MeasurementLog>> {
        return logDao.getLogsForWatch(watchId)
    }

    suspend fun getWatchById(id: Long): Watch? = withContext(Dispatchers.IO) {
        watchDao.getWatchById(id)
    }

    suspend fun insertWatch(watch: Watch): Long = withContext(Dispatchers.IO) {
        watchDao.insertWatch(watch)
    }

    suspend fun updateWatch(watch: Watch) = withContext(Dispatchers.IO) {
        watchDao.updateWatch(watch)
    }

    suspend fun deleteWatch(watch: Watch) = withContext(Dispatchers.IO) {
        watchDao.deleteWatch(watch)
    }

    suspend fun deleteWatchById(watchId: Long) = withContext(Dispatchers.IO) {
        watchDao.deleteWatchById(watchId)
    }

    suspend fun getLatestLogForWatch(watchId: Long): MeasurementLog? = withContext(Dispatchers.IO) {
        logDao.getLatestLogForWatch(watchId)
    }

    suspend fun insertLog(log: MeasurementLog): Long = withContext(Dispatchers.IO) {
        logDao.insertLog(log)
    }

    suspend fun updateLog(log: MeasurementLog) = withContext(Dispatchers.IO) {
        logDao.updateLog(log)
    }

    suspend fun deleteLog(log: MeasurementLog) = withContext(Dispatchers.IO) {
        logDao.deleteLog(log)
    }

    suspend fun deleteLogById(logId: Long) = withContext(Dispatchers.IO) {
        logDao.deleteLogById(logId)
    }

    suspend fun importBackup(
        report: ParseReport,
        clearExisting: Boolean = false
    ): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            database.withTransaction {
                if (clearExisting) {
                    logDao.clearAll()
                    watchDao.clearAll()
                }
                watchDao.insertWatches(report.watches)
                logDao.insertLogs(report.logs)
            }
            Result.success(Pair(report.watches.size, report.logs.size))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exportBackup(): String = withContext(Dispatchers.IO) {
        val watches = watchDao.getAllWatchesList()
        val logs = logDao.getAllLogsList()
        BackupParser.exportToFormat(watches, logs)
    }
}
