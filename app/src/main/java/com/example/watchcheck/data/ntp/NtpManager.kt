package com.example.watchcheck.data.ntp

import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

data class NtpStatus(
    val isSyncing: Boolean = false,
    val isSynchronized: Boolean = false,
    val offsetMs: Long = 0L,
    val roundTripDelayMs: Long = 0L,
    val lastSyncElapsedRealtime: Long = 0L,
    val lastSyncNtpTime: Long = 0L,
    val serverUsed: String = "pool.ntp.org",
    val errorMessage: String? = null
) {
    val estimatedAccuracyMs: Long
        get() = if (roundTripDelayMs > 0) roundTripDelayMs / 2 else 50L
}

class NtpManager(
    private val sntpClient: SntpClient = SntpClient(),
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _status = MutableStateFlow(NtpStatus())
    val status: StateFlow<NtpStatus> = _status.asStateFlow()

    init {
        syncNow()
    }

    fun syncNow(preferredHost: String = "pool.ntp.org") {
        if (_status.value.isSyncing) return
        _status.value = _status.value.copy(isSyncing = true, errorMessage = null)

        scope.launch {
            val serversToTry = listOf(
                preferredHost,
                "0.pool.ntp.org",
                "1.pool.ntp.org",
                "ptbtime1.ptb.de",
                "time.google.com",
                "time.cloudflare.com"
            ).distinct()

            var successResult: SntpClient.NtpResult? = null
            var lastError: Exception? = null

            for (server in serversToTry) {
                val res = sntpClient.requestTime(host = server, timeoutMs = 2500)
                if (res.isSuccess) {
                    successResult = res.getOrNull()
                    break
                } else {
                    lastError = res.exceptionOrNull() as? Exception
                }
            }

            if (successResult != null) {
                _status.value = NtpStatus(
                    isSyncing = false,
                    isSynchronized = true,
                    offsetMs = successResult.offsetMs,
                    roundTripDelayMs = successResult.roundTripDelayMs,
                    lastSyncElapsedRealtime = successResult.elapsedRealtime,
                    lastSyncNtpTime = successResult.ntpTime,
                    serverUsed = successResult.server,
                    errorMessage = null
                )
            } else {
                _status.value = _status.value.copy(
                    isSyncing = false,
                    errorMessage = lastError?.localizedMessage ?: "Verbindungsfehler zu NTP-Servern"
                )
            }
        }
    }

    /**
     * Gibt die atomgenaue Zeit im exakten Moment des Aufrufs zurück (in Millisekunden seit Epoch).
     * Basiert auf der hardware-unabhängigen Monotonic-Clock (SystemClock.elapsedRealtime)
     * addiert auf den letzten validierten NTP-Referenzpunkt.
     */
    fun getAtomicTimeForEvent(eventElapsedRealtime: Long = SystemClock.elapsedRealtime()): Long {
        val current = _status.value
        return if (current.isSynchronized && current.lastSyncElapsedRealtime > 0) {
            val elapsedDelta = eventElapsedRealtime - current.lastSyncElapsedRealtime
            current.lastSyncNtpTime + elapsedDelta
        } else {
            // Fallback auf Systemzeit + gemessener Offset
            System.currentTimeMillis() + current.offsetMs
        }
    }

    /**
     * Aktuelle hochpräzise Zeit jetzt.
     */
    fun nowAtomicMillis(): Long = getAtomicTimeForEvent(SystemClock.elapsedRealtime())
}
