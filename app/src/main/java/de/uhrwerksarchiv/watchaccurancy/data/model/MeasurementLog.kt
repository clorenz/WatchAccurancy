package de.uhrwerksarchiv.watchaccurancy.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "measurement_logs",
    foreignKeys = [
        ForeignKey(
            entity = Watch::class,
            parentColumns = ["id"],
            childColumns = ["watchId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["watchId"]),
        Index(value = ["ntpTimestampMs"])
    ]
)
data class MeasurementLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val watchId: Long,
    val setId: Int = 0,
    val ntpTimestampMs: Long,
    val watchTimestampMs: Long,
    val positionCode: String = "WORN",
    val extraValue: Double = 0.0,
    val notes: String = ""
) {
    /**
     * Absolute Abweichung in Sekunden:
     * Positiv = Uhr geht vor
     * Negativ = Uhr geht nach
     */
    val deviationSeconds: Double
        get() = (watchTimestampMs - ntpTimestampMs) / 1000.0

    val deviationMs: Long
        get() = watchTimestampMs - ntpTimestampMs

    val position: WatchPosition
        get() = WatchPosition.fromCode(positionCode)
}
