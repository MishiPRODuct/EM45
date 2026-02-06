package com.mishipay.pos.domain

/**
 * Represents an item in the shopping basket
 */
data class BasketItem(
    val epc: String,              // Raw EPC hex string
    val sku: String? = null,      // Decoded GTIN-14 (null if decode failed)
    val price: Int = 100,
    val quantity: Int = 1
) {
    val totalPrice: Int get() = price * quantity

    // Display SKU if available, otherwise show truncated EPC
    val displayName: String
        get() = sku ?: epc

    // For backward compatibility
    val rfidTag: String get() = epc
}

/**
 * Represents the shopping basket state
 */
data class Basket(
    val items: List<BasketItem> = emptyList()
) {
    val totalItems: Int get() = items.sumOf { it.quantity }
    val totalPrice: Int get() = items.sumOf { it.totalPrice }
    val isEmpty: Boolean get() = items.isEmpty()
}
