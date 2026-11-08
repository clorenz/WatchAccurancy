package com.example

import de.uhrwerksarchiv.watchaccurancy.data.backup.BackupParser
import de.uhrwerksarchiv.watchaccurancy.data.model.MeasurementLog
import de.uhrwerksarchiv.watchaccurancy.data.model.RateCalculator
import de.uhrwerksarchiv.watchaccurancy.data.model.Watch
import de.uhrwerksarchiv.watchaccurancy.data.model.WatchPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testParseUserHistoricalBackupExample() {
        val input = """
            WATCH: 3|Klassische Automatikuhr|SN-48201|1573237608379|
            LOG: 35|1|2|1565212365546|1565212500000|DU|20|
            LOG: 52|3|0|1573927428709|1573927440000|WORN|-273|Nachts 12O
        """.trimIndent()

        val report = BackupParser.parse(input)

        assertTrue("Report should be valid", report.isValid)
        assertEquals(2, report.logs.size)

        // Watch was declared with ID 3
        val watch3 = report.watches.firstOrNull { it.id == 3L }
        assertNotNull("Watch 3 should exist", watch3)
        assertEquals("Klassische Automatikuhr", watch3?.model)
        assertEquals("SN-48201", watch3?.serialNumber)
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
        // WatchCheck -273 temperature is sanitized to 0.0
        assertEquals(0.0, log52.extraValue, 0.001)
        assertEquals("Nachts 12O", log52.notes)
    }

    @Test
    fun testParseAuthenticWatchCheckExportFileWithHeader() {
        val authenticWatchCheckContent = """
            WatchCheck2
            WATCH: 1|Handaufzug Chronograph|CHRONO-01|1565100000000|Klassiker
            WATCH: 2|Taucheruhr Automatik|null|1573237608379|null
            LOG: 101|1|0|1565212365546|1565212500000|DU|20|Zifferblatt oben
            LOG: 102|1|0|1565298810230|1565298947000|CU|-273|Krone oben
            LOG: 103|2|1|1573927428709|1573927440000|WORN|null|Getragen
        """.trimIndent()

        val report = BackupParser.parse(authenticWatchCheckContent)

        assertTrue("Report should be valid without errors", report.isValid)
        assertEquals(0, report.errors.size)
        assertEquals(2, report.watches.size)
        assertEquals(3, report.logs.size)

        val watch1 = report.watches.first { it.id == 1L }
        assertEquals("Handaufzug Chronograph", watch1.model)
        assertEquals("CHRONO-01", watch1.serialNumber)
        assertEquals("Klassiker", watch1.notes)

        val watch2 = report.watches.first { it.id == 2L }
        assertEquals("Taucheruhr Automatik", watch2.model)
        assertEquals("", watch2.serialNumber) // "null" converted to empty string
        assertEquals("", watch2.notes)

        val log102 = report.logs.first { it.id == 102L }
        assertEquals(0.0, log102.extraValue, 0.001) // -273 converted to 0.0

        val log103 = report.logs.first { it.id == 103L }
        assertEquals("WORN", log103.positionCode)
        assertEquals("Getragen", log103.notes)
    }

    @Test
    fun testExportRoundTrip() {
        val watch = Watch(
            id = 42L,
            model = "Handaufzug Chronograph",
            serialNumber = "CHRONO-42",
            createdAt = 1600000000000L,
            notes = "Werk cal. 1861"
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
        assertTrue(exported.contains("WatchCheck2"))
        assertTrue(exported.contains("WATCH: 42|Handaufzug Chronograph|CHRONO-42|1600000000000|Werk cal. 1861"))
        assertTrue(exported.contains("LOG: 101|42|1|1600010000000|1600010005000|DU|21|Test Note"))

        val reparsed = BackupParser.parse(exported)
        assertEquals(1, reparsed.watches.size)
        assertEquals(1, reparsed.logs.size)
        assertEquals("Handaufzug Chronograph", reparsed.watches[0].model)
        assertEquals("CHRONO-42", reparsed.watches[0].serialNumber)
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

    @Test
    fun testMultiPeriodRateCalculation() {
        val baseTime = 1600000000000L
        val oneDayMs = 86400000L

        // Period 0: 2 logs (+4s, then +6s -> +2.0 s/d)
        val p0Log1 = MeasurementLog(id = 1L, watchId = 1L, setId = 0, ntpTimestampMs = baseTime, watchTimestampMs = baseTime + 4000L)
        val p0Log2 = MeasurementLog(id = 2L, watchId = 1L, setId = 0, ntpTimestampMs = baseTime + oneDayMs, watchTimestampMs = baseTime + oneDayMs + 6000L)

        // Period 1 (e.g. 30 days later): 2 logs (+1s, then +2s -> +1.0 s/d)
        val p1Time = baseTime + 30 * oneDayMs
        val p1Log1 = MeasurementLog(id = 3L, watchId = 1L, setId = 1, ntpTimestampMs = p1Time, watchTimestampMs = p1Time + 1000L)
        val p1Log2 = MeasurementLog(id = 4L, watchId = 1L, setId = 1, ntpTimestampMs = p1Time + oneDayMs, watchTimestampMs = p1Time + oneDayMs + 2000L)

        val points = RateCalculator.computeRatePoints(listOf(p0Log1, p0Log2, p1Log1, p1Log2))
        assertEquals(4, points.size)

        // First point of Period 0 is reference
        assertEquals(null, points[0].rateSecondsPerDay)
        assertEquals(2.0, points[1].rateSecondsPerDay!!, 0.01)

        // First point of Period 1 is a new reference (no drift calculated across period boundaries)
        assertEquals(null, points[2].rateSecondsPerDay)
        assertEquals(1.0, points[3].rateSecondsPerDay!!, 0.01)
    }

    @Test
    fun testPeriodDisplayInfoStartAndEndDate() {
        val baseTime = 1691838000000L // 12.08.2023
        val dayMs = 86400000L

        val log1 = MeasurementLog(id = 1L, watchId = 1L, setId = 0, ntpTimestampMs = baseTime, watchTimestampMs = baseTime + 2000L)
        val log2 = MeasurementLog(id = 2L, watchId = 1L, setId = 0, ntpTimestampMs = baseTime + 13 * dayMs, watchTimestampMs = baseTime + 13 * dayMs + 5000L)

        // Multiple days in period: should format as start - end
        val infoMulti = RateCalculator.getPeriodDisplayInfo(listOf(log1, log2), 0)
        assertEquals("12.08.23 - 25.08.23", infoMulti.title)
        assertEquals(2, infoMulti.count)

        // Single log in period: start == end date
        val infoSingle = RateCalculator.getPeriodDisplayInfo(listOf(log1), 0)
        assertEquals("12.08.23", infoSingle.title)
        assertEquals(1, infoSingle.count)
    }

    @Test
    fun testWatchSortingDescendingByLatestMeasurement() {
        val watchA = Watch(id = 1L, model = "Uhr A (älteste Messung)", createdAt = 1000L)
        val watchB = Watch(id = 2L, model = "Uhr B (neueste Messung)", createdAt = 2000L)
        val watchC = Watch(id = 3L, model = "Uhr C (ohne Messung)", createdAt = 3000L)

        val logs = listOf(
            MeasurementLog(id = 10L, watchId = 1L, ntpTimestampMs = 100_000L, watchTimestampMs = 100_000L),
            MeasurementLog(id = 11L, watchId = 2L, ntpTimestampMs = 500_000L, watchTimestampMs = 500_000L),
            MeasurementLog(id = 12L, watchId = 1L, ntpTimestampMs = 200_000L, watchTimestampMs = 200_000L)
        )

        val latestMap = logs.groupBy { it.watchId }
            .mapValues { entry -> entry.value.maxOfOrNull { it.ntpTimestampMs } ?: 0L }

        val watches = listOf(watchA, watchB, watchC)
        val sorted = watches.sortedWith(
            compareByDescending<Watch> { latestMap[it.id] ?: 0L }
                .thenByDescending { it.createdAt }
        )

        // Watch B has newest measurement (500_000L) -> should be first
        assertEquals(2L, sorted[0].id)
        // Watch A has older measurement (200_000L) -> should be second
        assertEquals(1L, sorted[1].id)
        // Watch C has no measurements -> should be last
        assertEquals(3L, sorted[2].id)
    }
}
