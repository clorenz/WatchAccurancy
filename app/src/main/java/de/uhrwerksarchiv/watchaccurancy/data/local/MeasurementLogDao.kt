package de.uhrwerksarchiv.watchaccurancy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.uhrwerksarchiv.watchaccurancy.data.model.MeasurementLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MeasurementLogDao {
    @Query("SELECT * FROM measurement_logs WHERE watchId = :watchId ORDER BY ntpTimestampMs ASC")
    fun getLogsForWatch(watchId: Long): Flow<List<MeasurementLog>>

    @Query("SELECT * FROM measurement_logs WHERE watchId = :watchId ORDER BY ntpTimestampMs ASC")
    suspend fun getLogsForWatchList(watchId: Long): List<MeasurementLog>

    @Query("SELECT * FROM measurement_logs ORDER BY ntpTimestampMs ASC")
    fun getAllLogs(): Flow<List<MeasurementLog>>

    @Query("SELECT * FROM measurement_logs ORDER BY ntpTimestampMs ASC")
    suspend fun getAllLogsList(): List<MeasurementLog>

    @Query("SELECT * FROM measurement_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: Long): MeasurementLog?

    @Query("SELECT * FROM measurement_logs WHERE watchId = :watchId ORDER BY ntpTimestampMs DESC LIMIT 1")
    suspend fun getLatestLogForWatch(watchId: Long): MeasurementLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MeasurementLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<MeasurementLog>): List<Long>

    @Update
    suspend fun updateLog(log: MeasurementLog)

    @Delete
    suspend fun deleteLog(log: MeasurementLog)

    @Query("DELETE FROM measurement_logs WHERE id = :id")
    suspend fun deleteLogById(id: Long)

    @Query("DELETE FROM measurement_logs WHERE watchId = :watchId")
    suspend fun deleteLogsForWatch(watchId: Long)

    @Query("DELETE FROM measurement_logs")
    suspend fun clearAll()
}
