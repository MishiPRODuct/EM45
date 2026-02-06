package com.mishipay.pos.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mishipay.pos.data.RfidReaderManager
import com.mishipay.pos.domain.ReaderState
import com.mishipay.pos.domain.RfidTag
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RfidViewModel(application: Application) : AndroidViewModel(application) {

    private val rfidReaderManager = RfidReaderManager(application)

    val readerState: StateFlow<ReaderState> = rfidReaderManager.readerState
    val tagFlow: SharedFlow<RfidTag> = rfidReaderManager.tagFlow

    fun connectReader() {
        viewModelScope.launch {
            rfidReaderManager.connect()
            // Auto-start scanning after connection
            if (rfidReaderManager.readerState.value is ReaderState.Connected) {
                rfidReaderManager.startInventory()
            }
        }
    }

    fun startScanning() {
        viewModelScope.launch {
            rfidReaderManager.startInventory()
        }
    }

    fun stopScanning() {
        viewModelScope.launch {
            rfidReaderManager.stopInventory()
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            rfidReaderManager.disconnect()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            rfidReaderManager.disconnect()
        }
    }
}
