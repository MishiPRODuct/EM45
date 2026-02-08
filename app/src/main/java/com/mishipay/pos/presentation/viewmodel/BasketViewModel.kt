package com.mishipay.pos.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import com.mishipay.pos.domain.Basket
import com.mishipay.pos.domain.BasketItem
import com.mishipay.pos.domain.utils.EpcDecoder
import com.mishipay.pos.domain.utils.EpcDecodeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BasketViewModel : ViewModel() {

    private val _basket = MutableStateFlow(Basket())
    val basket: StateFlow<Basket> = _basket.asStateFlow()

    // Tags scanned in current scanning session (EPC -> decoded SKU pairs)
    private val _sessionTags = MutableStateFlow<List<ScannedTag>>(emptyList())
    val sessionTags: StateFlow<List<ScannedTag>> = _sessionTags.asStateFlow()

    // Simple list of EPCs for backward compatibility
    val sessionEpcs: List<String>
        get() = _sessionTags.value.map { it.epc }

    fun addItem(epc: String) {
        _basket.update { currentBasket ->
            val existingItem = currentBasket.items.find { it.epc == epc }
            if (existingItem != null) {
                // Item exists, increment quantity
                val updatedItems = currentBasket.items.map { item ->
                    if (item.epc == epc) {
                        item.copy(quantity = item.quantity + 1)
                    } else {
                        item
                    }
                }
                currentBasket.copy(items = updatedItems)
            } else {
                // New item - decode EPC to get GTIN and serial
                val (sku, serial) = decodeEpc(epc)
                val newItem = BasketItem(epc = epc, sku = sku, serialNumber = serial)
                currentBasket.copy(items = currentBasket.items + newItem)
            }
        }
    }

    /**
     * Decode EPC to GTIN (AI 01) and Serial Number (AI 21) using SGTIN-96 algorithm
     * Returns pair of (gtin, serialNumber), both null if decoding fails
     */
    private fun decodeEpc(epc: String): Pair<String?, String?> {
        val result = EpcDecoder.decode(epc)
        Log.d("BasketVM", "decodeEpc input='$epc' len=${epc.length} bytes=${epc.toByteArray().map { it.toInt() }} result=$result")
        return when (result) {
            is EpcDecodeResult.Success -> Pair(result.gtin, result.serialNumber)
            is EpcDecodeResult.Failure -> Pair(null, null)
        }
    }

    fun addTagToSession(epc: String) {
        if (_sessionTags.value.none { it.epc == epc }) {
            val (sku, serial) = decodeEpc(epc)
            _sessionTags.update { it + ScannedTag(epc = epc, sku = sku, serialNumber = serial) }
        }
    }

    fun clearSessionTags() {
        _sessionTags.value = emptyList()
    }

    fun commitSessionToBasket() {
        _sessionTags.value.forEach { tag ->
            addItem(tag.epc)
        }
        clearSessionTags()
    }

    fun removeItem(epc: String) {
        _basket.update { currentBasket ->
            currentBasket.copy(items = currentBasket.items.filter { it.epc != epc })
        }
    }

    fun clearBasket() {
        _basket.value = Basket()
    }
}

/**
 * Represents a scanned tag with EPC, decoded SKU (GTIN), and serial number
 */
data class ScannedTag(
    val epc: String,
    val sku: String?,           // AI 01 - GTIN-14
    val serialNumber: String?   // AI 21 - Serial Number
) {
    val displayName: String
        get() = sku ?: epc
}
