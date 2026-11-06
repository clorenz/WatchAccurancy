package de.uhrwerksarchiv.watchaccurancy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.uhrwerksarchiv.watchaccurancy.data.model.Watch
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchDao {
    @Query("SELECT * FROM watches ORDER BY createdAt DESC")
    fun getAllWatches(): Flow<List<Watch>>

    @Query("SELECT * FROM watches WHERE id = :id LIMIT 1")
    suspend fun getWatchById(id: Long): Watch?

    @Query("SELECT * FROM watches ORDER BY createdAt DESC")
    suspend fun getAllWatchesList(): List<Watch>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatch(watch: Watch): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWatches(watches: List<Watch>): List<Long>

    @Update
    suspend fun updateWatch(watch: Watch)

    @Delete
    suspend fun deleteWatch(watch: Watch)

    @Query("DELETE FROM watches WHERE id = :id")
    suspend fun deleteWatchById(id: Long)

    @Query("DELETE FROM watches")
    suspend fun clearAll()
}
