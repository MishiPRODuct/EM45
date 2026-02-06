package com.mishipay.pos.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Domain model representing an RFID tag read from the reader
 */
data class RfidTag(
    val epc: String,
    val rssi: Int,
    val peakRssi: Int,
    val readCount: Int,
    val timestamp: Long,
    val memoryBank: String? = null,
    val pc: String? = null,
    val antennaId: Int = 1
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Sealed class representing reader connection state
 */
sealed class ReaderState {
    data object Disconnected : ReaderState()
    data object Connecting : ReaderState()
    data object Connected : ReaderState()
    data object Scanning : ReaderState()
    data class Error(val message: String) : ReaderState()
}
