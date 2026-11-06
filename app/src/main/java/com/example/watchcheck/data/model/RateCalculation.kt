package com.example.watchcheck.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

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
     * Berechnet die Gangabweichung in s/d für eine sortierte Liste von Messpunkten (chronologisch aufsteigend).
     */
    fun computeRatePoints(logs: List<MeasurementLog>): List<RatePoint> {
        if (logs.isEmpty()) return emptyList()
        val sorted = logs.sortedBy { it.ntpTimestampMs }
        val results = mutableListOf<RatePoint>()

        for (i in sorted.indices) {
            val current = sorted[i]
            if (i == 0) {
                results.add(
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

                // Differenz der Abweichungen:
                // Wenn prev +5s war und current +7s -> Uhr hat in deltaDays +2s zugelegt -> Rate = +2s / deltaDays
                val rate = if (deltaDays > 0.0001) {
                    (current.deviationSeconds - prev.deviationSeconds) / deltaDays
                } else {
                    0.0
                }

                results.add(
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
        return results
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

        val first = ratePoints.first()
        val last = ratePoints.last()
        val totalMs = last.log.ntpTimestampMs - first.log.ntpTimestampMs
        val totalDays = totalMs / (1000.0 * 86400.0)

        val totalDrift = last.log.deviationSeconds - first.log.deviationSeconds
        val avgOverall = if (totalDays > 0.001) totalDrift / totalDays else null

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
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)

    fun formatDateTime(timestampMs: Long): String {
        return dateFormatter.format(Date(timestampMs))
    }

    fun formatTimeOnly(timestampMs: Long): String {
        return timeFormatter.format(Date(timestampMs))
    }
}
