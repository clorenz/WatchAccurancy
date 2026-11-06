package com.example

import com.example.watchcheck.data.backup.BackupParser
import com.example.watchcheck.data.model.MeasurementLog
import com.example.watchcheck.data.model.RateCalculator
import com.example.watchcheck.data.model.Watch
import com.example.watchcheck.data.model.WatchPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testParseUserHistoricalBackupExample() {
        val input = """
            WATCH: 3|Rolex Datejust|E283744|1573237608379|
            LOG: 35|1|2|1565212365546|1565212500000|DU|20|
            LOG: 52|3|0|1573927428709|1573927440000|WORN|-273|Nachts 12O
        """.trimIndent()

        val report = BackupParser.parse(input)

        assertTrue("Report should be valid", report.isValid)
        assertEquals(2, report.logs.size)

        // Rolex Datejust was declared with ID 3
        val watch3 = report.watches.firstOrNull { it.id == 3L }
        assertNotNull("Watch 3 should exist", watch3)
        assertEquals("Rolex Datejust", watch3?.model)
        assertEquals("E283744", watch3?.serialNumber)
        assertEquals(1573237608379L, watch3?.createdAt)

        // Log 35 references Watch ID 1 which wasn't declared in WATCH line -> auto placeholder created
        val watch1 = report.watches.firstOrNull { it.id == 1L }
        assertNotNull("Watch 1 placeholder should be created for integrity", watch1)

        val log35 = report.logs.first { it.id == 35L }
        assertEquals(1L, log35.watchId)
        assertEquals(2, log35.setId)
        assertEquals(1565212365546L, log35.ntpTimestampMs)
        assertEquals(1565212500000L, log35.watchTimestampMs)
        assertEquals("DU", log35.positionCode)
        assertEquals(WatchPosition.DU, log35.position)
        assertEquals(20.0, log35.extraValue, 0.001)

        val log52 = report.logs.first { it.id == 52L }
        assertEquals(3L, log52.watchId)
        assertEquals(0, log52.setId)
        assertEquals(1573927428709L, log52.ntpTimestampMs)
        assertEquals(1573927440000L, log52.watchTimestampMs)
        assertEquals("WORN", log52.positionCode)
        assertEquals(-273.0, log52.extraValue, 0.001)
        assertEquals("Nachts 12O", log52.notes)
    }

    @Test
    fun testExportRoundTrip() {
        val watch = Watch(
            id = 42L,
            model = "Speedmaster",
            serialNumber = "OMEGA-99",
            createdAt = 1600000000000L
        )
        val log = MeasurementLog(
            id = 101L,
            watchId = 42L,
            setId = 1,
            ntpTimestampMs = 1600010000000L,
            watchTimestampMs = 1600010005000L,
            positionCode = "DU",
            extraValue = 21.0,
            notes = "Test Note"
        )

        val exported = BackupParser.exportToFormat(listOf(watch), listOf(log))
        assertTrue(exported.contains("WATCH: 42|Speedmaster|OMEGA-99|1600000000000|"))
        assertTrue(exported.contains("LOG: 101|42|1|1600010000000|1600010005000|DU|21|Test Note"))

        val reparsed = BackupParser.parse(exported)
        assertEquals(1, reparsed.watches.size)
        assertEquals(1, reparsed.logs.size)
        assertEquals("Speedmaster", reparsed.watches[0].model)
        assertEquals("Test Note", reparsed.logs[0].notes)
    }

    @Test
    fun testRateCalculatorDailyDrift() {
        val baseTime = 1600000000000L
        val oneDayMs = 86400000L

        // Day 1: Watch is +5s
        val log1 = MeasurementLog(
            id = 1L,
            watchId = 1L,
            ntpTimestampMs = baseTime,
            watchTimestampMs = baseTime + 5000L // +5.0s
        )

        // Day 2 (exact 24h later): Watch is +7.5s (drifted +2.5s in 24 hours -> +2.5 s/d)
        val log2 = MeasurementLog(
            id = 2L,
            watchId = 1L,
            ntpTimestampMs = baseTime + oneDayMs,
            watchTimestampMs = (baseTime + oneDayMs) + 7500L // +7.5s
        )

        val points = RateCalculator.computeRatePoints(listOf(log1, log2))
        assertEquals(2, points.size)

        // Point 1 is reference
        assertEquals(null, points[0].rateSecondsPerDay)
        assertEquals(5.0, points[0].cumulativeDeviationSeconds, 0.001)

        // Point 2 rate should be exactly +2.5 s/d
        assertNotNull(points[1].rateSecondsPerDay)
        assertEquals(2.5, points[1].rateSecondsPerDay!!, 0.01)
        assertEquals(7.5, points[1].cumulativeDeviationSeconds, 0.001)
    }
}
