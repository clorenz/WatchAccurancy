package com.example.watchcheck.data.ntp

import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.abs

/**
 * Robust, lightweight RFC 4330 / RFC 5905 SNTP Client.
 */
class SntpClient {

    data class NtpResult(
        val ntpTime: Long,
        val elapsedRealtime: Long,
        val roundTripDelayMs: Long,
        val offsetMs: Long,
        val server: String
    )

    companion object {
        private const val NTP_PORT = 123
        private const val NTP_MODE_CLIENT = 3
        private const val NTP_VERSION = 3
        private const val NTP_PACKET_SIZE = 48

        private const val ORIGINATE_TIME_OFFSET = 24
        private const val RECEIVE_TIME_OFFSET = 32
        private const val TRANSMIT_TIME_OFFSET = 40

        // 70 years in seconds (from 1900 to 1970)
        private const val OFFSET_1900_TO_1970 = 2208988800L

        val DEFAULT_SERVERS = listOf(
            "pool.ntp.org",
            "0.pool.ntp.org",
            "time.google.com",
            "time.cloudflare.com",
            "ptbtime1.ptb.de" // Physikalisch-Technische Bundesanstalt (Atomuhr Braunschweig)
        )
    }

    suspend fun requestTime(
        host: String = "pool.ntp.org",
        timeoutMs: Int = 3000
    ): Result<NtpResult> = withContext(Dispatchers.IO) {
        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            socket.soTimeout = timeoutMs

            val address = InetAddress.getByName(host)
            val buffer = ByteArray(NTP_PACKET_SIZE)

            // Set Mode 3 (Client) and Version 3: 00 011 011 -> 0x1B
            buffer[0] = ((NTP_MODE_CLIENT or (NTP_VERSION shl 3)).toByte())

            // Record client transmit time (T0)
            val requestTime = System.currentTimeMillis()
            val requestTicks = SystemClock.elapsedRealtime()
            writeTimeStamp(buffer, TRANSMIT_TIME_OFFSET, requestTime)

            val requestPacket = DatagramPacket(buffer, buffer.size, address, NTP_PORT)
            socket.send(requestPacket)

            // Read response
            val responsePacket = DatagramPacket(buffer, buffer.size)
            socket.receive(responsePacket)
            val responseTicks = SystemClock.elapsedRealtime()

            // Extract timestamps
            val originateTime = readTimeStamp(buffer, ORIGINATE_TIME_OFFSET) // T0
            val receiveTime = readTimeStamp(buffer, RECEIVE_TIME_OFFSET)     // T1
            val transmitTime = readTimeStamp(buffer, TRANSMIT_TIME_OFFSET)   // T2
            val destinationTime = requestTime + (responseTicks - requestTicks) // T3

            val roundTripTime = (responseTicks - requestTicks) - (transmitTime - receiveTime)
            val clockOffset = ((receiveTime - originateTime) + (transmitTime - destinationTime)) / 2

            val trueNtpTime = destinationTime + clockOffset

            Result.success(
                NtpResult(
                    ntpTime = trueNtpTime,
                    elapsedRealtime = responseTicks,
                    roundTripDelayMs = roundTripTime,
                    offsetMs = clockOffset,
                    server = host
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            socket?.close()
        }
    }

    private fun readTimeStamp(buffer: ByteArray, offset: Int): Long {
        var seconds = 0L
        for (i in 0..3) {
            val b = buffer[offset + i].toLong() and 0xFFL
            seconds = (seconds shl 8) or b
        }
        var fraction = 0L
        for (i in 4..7) {
            val b = buffer[offset + i].toLong() and 0xFFL
            fraction = (fraction shl 8) or b
        }
        val ms = ((seconds - OFFSET_1900_TO_1970) * 1000L) + ((fraction * 1000L) / 0x100000000L)
        return ms
    }

    private fun writeTimeStamp(buffer: ByteArray, offset: Int, timeMs: Long) {
        val seconds = (timeMs / 1000L) + OFFSET_1900_TO_1970
        val fraction = ((timeMs % 1000L) * 0x100000000L) / 1000L

        for (i in 3 downTo 0) {
            buffer[offset + i] = (seconds shr ((3 - i) * 8)).toByte()
        }
        for (i in 7 downTo 4) {
            buffer[offset + i] = (fraction shr ((7 - i) * 8)).toByte()
        }
    }
}
