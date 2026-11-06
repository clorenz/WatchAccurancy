package de.uhrwerksarchiv.watchaccurancy.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Modell für einen berechneten Gang-Messpunkt (Rate Point).
 */
data class RatePoint(
    val log: MeasurementLog,
    val previousLog: MeasurementLog?,
    val rateSecondsPerDay: Double?, // null for the first reference measurement
    val intervalHours: Double?,
    val cumulativeDeviationSeconds: Double
) {
    val formattedRate: String
        get() {
            if (rateSecondsPerDay == null) return "Referenz"
            val sign = if (rateSecondsPerDay >= 0) "+" else ""
            return String.format(Locale.US, "%s%.2f s/d", sign, rateSecondsPerDay)
        }

    val formattedDeviation: String
        get() {
            val sign = if (cumulativeDeviationSeconds >= 0) "+" else ""
            return String.format(Locale.US, "%s%.2f s", sign, cumulativeDeviationSeconds)
        }
}

/**
 * Gesamtstatistik für eine Uhr.
 */
data class WatchStatistics(
    val totalMeasurements: Int,
    val averageRatePerDay: Double?,
    val minRatePerDay: Double?,
    val maxRatePerDay: Double?,
    val totalDriftSeconds: Double?,
    val totalObservationDays: Double?,
    val positionStats: Map<WatchPosition, PositionStatistic>
)

data class PositionStatistic(
    val count: Int,
    val averageRatePerDay: Double?
)

object RateCalculator {

    /**
     * Berechnet die Gangabweichung in s/d für eine sortierte Liste von Messpunkten.
     * Gruppiert nach Messperiode (setId), sodass jede Periode ihren eigenen Referenz-Startpunkt besitzt
     * und Gangraten nicht fälschlich über Periodenwechsel hinweg berechnet werden.
     */
    fun computeRatePoints(logs: List<MeasurementLog>): List<RatePoint> {
        if (logs.isEmpty()) return emptyList()
        val grouped = logs.groupBy { it.setId }
        val allPoints = mutableListOf<RatePoint>()

        for ((_, setLogs) in grouped) {
            val sorted = setLogs.sortedBy { it.ntpTimestampMs }
            for (i in sorted.indices) {
                val current = sorted[i]
                if (i == 0) {
                    allPoints.add(
                        RatePoint(
                            log = current,
                            previousLog = null,
                            rateSecondsPerDay = null,
                            intervalHours = null,
                            cumulativeDeviationSeconds = current.deviationSeconds
                        )
                    )
                } else {
                    val prev = sorted[i - 1]
                    val deltaMs = current.ntpTimestampMs - prev.ntpTimestampMs
                    val deltaHours = deltaMs / (1000.0 * 3600.0)
                    val deltaDays = deltaHours / 24.0

                    val rate = if (deltaDays > 0.0001) {
                        (current.deviationSeconds - prev.deviationSeconds) / deltaDays
                    } else {
                        0.0
                    }

                    allPoints.add(
                        RatePoint(
                            log = current,
                            previousLog = prev,
                            rateSecondsPerDay = rate,
                            intervalHours = deltaHours,
                            cumulativeDeviationSeconds = current.deviationSeconds
                        )
                    )
                }
            }
        }
        return allPoints.sortedBy { it.log.ntpTimestampMs }
    }

    /**
     * Erstellt die statistische Gesamtauswertung.
     */
    fun computeStatistics(logs: List<MeasurementLog>): WatchStatistics {
        if (logs.size < 2) {
            return WatchStatistics(
                totalMeasurements = logs.size,
                averageRatePerDay = null,
                minRatePerDay = null,
                maxRatePerDay = null,
                totalDriftSeconds = null,
                totalObservationDays = null,
                positionStats = emptyMap()
            )
        }

        val ratePoints = computeRatePoints(logs)
        val validRates = ratePoints.mapNotNull { it.rateSecondsPerDay }

        val first = logs.minByOrNull { it.ntpTimestampMs }!!
        val last = logs.maxByOrNull { it.ntpTimestampMs }!!
        val totalMs = last.ntpTimestampMs - first.ntpTimestampMs
        val totalDays = totalMs / (1000.0 * 86400.0)

        val totalDrift = last.deviationSeconds - first.deviationSeconds
        val avgOverall = if (validRates.isNotEmpty()) validRates.average() else null

        // Position statistics
        val posMap = mutableMapOf<WatchPosition, MutableList<Double>>()
        ratePoints.forEach { point ->
            val r = point.rateSecondsPerDay
            if (r != null) {
                posMap.getOrPut(point.log.position) { mutableListOf() }.add(r)
            }
        }

        val positionStats = posMap.mapValues { (_, rates) ->
            PositionStatistic(
                count = rates.size,
                averageRatePerDay = if (rates.isNotEmpty()) rates.average() else null
            )
        }

        return WatchStatistics(
            totalMeasurements = logs.size,
            averageRatePerDay = avgOverall,
            minRatePerDay = validRates.minOrNull(),
            maxRatePerDay = validRates.maxOrNull(),
            totalDriftSeconds = totalDrift,
            totalObservationDays = totalDays,
            positionStats = positionStats
        )
    }

    private val dateFormatter = SimpleDateFormat("dd.MM.yy HH:mm", Locale.GERMANY)
    private val dateTimeWithSecondsFormatter = SimpleDateFormat("dd.MM.yy HH:mm:ss", Locale.GERMANY)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)

    fun formatDateTime(timestampMs: Long): String {
        return dateFormatter.format(Date(timestampMs))
    }

    fun formatDateTimeWithSeconds(timestampMs: Long): String {
        return dateTimeWithSecondsFormatter.format(Date(timestampMs))
    }

    fun formatTimeOnly(timestampMs: Long): String {
        return timeFormatter.format(Date(timestampMs))
    }
}
