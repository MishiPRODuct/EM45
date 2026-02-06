package com.mishipay.pos.presentation.viewmodel

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
                // New item - decode EPC to get SKU
                val sku = decodeEpcToSku(epc)
                val newItem = BasketItem(epc = epc, sku = sku)
                currentBasket.copy(items = currentBasket.items + newItem)
            }
        }
    }

    /**
     * Decode EPC to GTIN/SKU using SGTIN-96 algorithm
     * Returns null if decoding fails
     */
    private fun decodeEpcToSku(epc: String): String? {
        return when (val result = EpcDecoder.decode(epc)) {
            is EpcDecodeResult.Success -> result.gtin
            is EpcDecodeResult.Failure -> null
        }
    }

    fun addTagToSession(epc: String) {
        if (_sessionTags.value.none { it.epc == epc }) {
            val sku = decodeEpcToSku(epc)
            _sessionTags.update { it + ScannedTag(epc = epc, sku = sku) }
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
 * Represents a scanned tag with both EPC and decoded SKU
 */
data class ScannedTag(
    val epc: String,
    val sku: String?
) {
    val displayName: String
        get() = sku ?: epc
}
