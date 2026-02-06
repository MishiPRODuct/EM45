package com.mishipay.pos.data

import android.content.Context
import android.util.Log
import com.mishipay.pos.domain.RfidTag
import com.mishipay.pos.domain.ReaderState
import com.zebra.rfid.api3.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

/**
 * RFID Reader Manager
 * Wrapper around Zebra RFID SDK for EM45 device
 */
class RfidReaderManager(private val context: Context) {

    companion object {
        private const val TAG = "RfidReaderManager"
    }

    private var readers: Readers? = null
    private var reader: RFIDReader? = null
    private var eventHandler: EventHandler? = null

    private val _readerState = MutableStateFlow<ReaderState>(ReaderState.Disconnected)
    val readerState: StateFlow<ReaderState> = _readerState.asStateFlow()

    private val _tagFlow = MutableSharedFlow<RfidTag>(
        extraBufferCapacity = 100,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    val tagFlow: SharedFlow<RfidTag> = _tagFlow.asSharedFlow()

    private val seenTags = mutableSetOf<String>()

    suspend fun connect(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _readerState.value = ReaderState.Connecting
            Log.d(TAG, "Initializing RFID readers...")

            readers = Readers(context, ENUM_TRANSPORT.RE_SERIAL)

            var availableReaders: List<ReaderDevice>? = null
            for (attempt in 1..5) {
                availableReaders = readers?.GetAvailableRFIDReaderList()
                if (!availableReaders.isNullOrEmpty()) break
                Log.d(TAG, "No readers found on attempt $attempt, retrying...")
                Thread.sleep(1000)
            }

            if (availableReaders.isNullOrEmpty()) {
                Log.e(TAG, "No RFID readers found after retries")
                _readerState.value = ReaderState.Error("No RFID readers found. Make sure you're running on an EM45 device.")
                return@withContext Result.failure(Exception("No RFID readers found"))
            }

            Log.d(TAG, "Found ${availableReaders.size} reader(s)")

            val readerDevice = availableReaders[0]
            reader = readerDevice.rfidReader

            Log.d(TAG, "Connecting to reader: ${readerDevice.name}")

            reader?.connect()
            configureReader()
            setupEventHandler()

            _readerState.value = ReaderState.Connected
            Log.d(TAG, "Successfully connected to RFID reader")

            Result.success(Unit)
        } catch (e: InvalidUsageException) {
            Log.e(TAG, "Invalid usage: ${e.info}", e)
            _readerState.value = ReaderState.Error("Invalid usage: ${e.info}")
            Result.failure(e)
        } catch (e: OperationFailureException) {
            Log.e(TAG, "Operation failed: ${e.vendorMessage}", e)
            if (e.vendorMessage?.contains("Charging source", ignoreCase = true) == true) {
                _readerState.value = ReaderState.Error("Disconnect USB/charging cable to use RFID reader.")
            } else {
                handleOperationFailure(e)
            }
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Connection error: ${e.message}", e)
            _readerState.value = ReaderState.Error("Connection error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun configureReader() {
        reader?.apply {
            try {
                Events.setHandheldEvent(true)
                Events.setTagReadEvent(true)
                Events.setAttachTagDataWithReadEvent(true)
                Events.setInventoryStartEvent(true)
                Events.setInventoryStopEvent(true)

                val tagStorageSettings = Config.getTagStorageSettings()
                tagStorageSettings.setTagFields(TAG_FIELD.ALL_TAG_FIELDS)
                Config.setTagStorageSettings(tagStorageSettings)

                try {
                    val antennaConfig = Config.Antennas.getAntennaRfConfig(1)
                    antennaConfig.transmitPowerIndex = 270
                    antennaConfig.setrfModeTableIndex(0)
                    Config.Antennas.setAntennaRfConfig(1, antennaConfig)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not configure antenna: ${e.message}")
                }

                try {
                    val singulationControl = Config.Antennas.getSingulationControl(1)
                    singulationControl.session = SESSION.SESSION_S0
                    singulationControl.Action.inventoryState = INVENTORY_STATE.INVENTORY_STATE_A
                    singulationControl.Action.setPerformStateAwareSingulationAction(false)
                    Config.Antennas.setSingulationControl(1, singulationControl)
                } catch (e: Exception) {
                    Log.w(TAG, "Could not configure singulation: ${e.message}")
                }

                Log.d(TAG, "Reader configured successfully")
            } catch (e: Exception) {
                Log.w(TAG, "Configuration warning: ${e.message}")
            }
        }
    }

    private fun setupEventHandler() {
        eventHandler = EventHandler()
        reader?.Events?.addEventsListener(eventHandler)
        Log.d(TAG, "Event handler registered")
    }

    suspend fun startInventory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (reader == null) {
                return@withContext Result.failure(Exception("Reader not connected"))
            }

            Log.d(TAG, "Starting inventory...")
            seenTags.clear()

            reader?.Actions?.Inventory?.perform()

            _readerState.value = ReaderState.Scanning
            Log.d(TAG, "Inventory started")

            Result.success(Unit)
        } catch (e: InvalidUsageException) {
            Log.e(TAG, "Start inventory failed: ${e.info}", e)
            _readerState.value = ReaderState.Error("Failed to start scanning: ${e.info}")
            Result.failure(e)
        } catch (e: OperationFailureException) {
            val errorMsg = e.vendorMessage ?: e.message ?: "Unknown error"
            Log.e(TAG, "Start inventory failed: $errorMsg", e)
            _readerState.value = ReaderState.Error("Failed to start scanning: $errorMsg")
            Result.failure(e)
        }
    }

    suspend fun stopInventory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Stopping inventory...")

            reader?.Actions?.Inventory?.stop()

            _readerState.value = ReaderState.Connected
            Log.d(TAG, "Inventory stopped")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Stop inventory failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Disconnecting from reader...")

            try {
                reader?.Actions?.Inventory?.stop()
            } catch (e: Exception) { }

            eventHandler?.let {
                reader?.Events?.removeEventsListener(it)
            }

            reader?.disconnect()
            readers?.Dispose()

            reader = null
            readers = null
            eventHandler = null

            _readerState.value = ReaderState.Disconnected
            Log.d(TAG, "Disconnected from reader")
        } catch (e: Exception) {
            Log.e(TAG, "Disconnect error: ${e.message}", e)
        }
    }

    private fun handleOperationFailure(e: OperationFailureException): Result<Unit> {
        return when (e.results) {
            RFIDResults.RFID_READER_REGION_NOT_CONFIGURED -> {
                Log.w(TAG, "Region not configured, attempting to configure...")
                try {
                    val supportedRegions = reader?.ReaderCapabilities?.SupportedRegions
                    val regionCount = supportedRegions?.length() ?: 0
                    if (regionCount > 0) {
                        val regionInfo = supportedRegions?.getRegionInfo(0)
                        regionInfo?.let {
                            reader?.Config?.regulatoryConfig?.region = it.regionCode
                            Log.d(TAG, "Region set to: ${it.name}")
                        }
                        _readerState.value = ReaderState.Connected
                        Result.success(Unit)
                    } else {
                        _readerState.value = ReaderState.Error("No supported regions found")
                        Result.failure(e)
                    }
                } catch (ex: Exception) {
                    _readerState.value = ReaderState.Error("Failed to configure region: ${ex.message}")
                    Result.failure(ex)
                }
            }
            else -> {
                val errorMsg = e.vendorMessage ?: e.message ?: "Unknown error (code: ${e.results})"
                _readerState.value = ReaderState.Error("Operation failed: $errorMsg")
                Result.failure(e)
            }
        }
    }

    private inner class EventHandler : RfidEventsListener {

        override fun eventReadNotify(e: RfidReadEvents?) {
            e?.readEventData?.tagData?.let { tagData ->
                processTagData(tagData)
            }
        }

        override fun eventStatusNotify(e: RfidStatusEvents?) {
            e?.StatusEventData?.let { statusData ->
                when (statusData.statusEventType) {
                    STATUS_EVENT_TYPE.DISCONNECTION_EVENT -> {
                        Log.d(TAG, "Reader disconnected")
                        _readerState.value = ReaderState.Disconnected
                    }
                    else -> {
                        Log.d(TAG, "Status event: ${statusData.statusEventType}")
                    }
                }
            }
        }
    }

    private fun processTagData(tagData: TagData) {
        try {
            val epc = tagData.tagID ?: return

            val rfidTag = RfidTag(
                epc = epc,
                rssi = tagData.peakRSSI.toInt(),
                peakRssi = tagData.peakRSSI.toInt(),
                readCount = tagData.tagSeenCount.toInt(),
                timestamp = System.currentTimeMillis(),
                memoryBank = tagData.memoryBankData,
                pc = tagData.pc?.toString(),
                antennaId = tagData.antennaID.toInt()
            )

            _tagFlow.tryEmit(rfidTag)

            Log.d(TAG, "Tag read: EPC=$epc, RSSI=${rfidTag.rssi}dBm")
        } catch (e: Exception) {
            Log.e(TAG, "Error processing tag data: ${e.message}", e)
        }
    }

    fun isConnected(): Boolean = reader?.isConnected == true
}
