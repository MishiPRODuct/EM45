package com.mishipay.pos.domain.utils

/**
 * GS1 SGTIN-96 EPC Decoder
 * Decodes hex EPC strings to extract GTIN-14 and serial number
 *
 * SGTIN-96 bit structure (96 bits total):
 * | Header (8) | Filter (3) | Partition (3) | Company Prefix (20-40) | Item Ref (24-4) | Serial (38) |
 */
object EpcDecoder {

    // SGTIN-96 header value
    private const val SGTIN_96_HEADER = 0x30

    // Partition table for SGTIN-96
    // [partition] = Pair(companyPrefixBits, itemRefBits)
    private val PARTITION_TABLE = mapOf(
        0 to PartitionInfo(40, 12, 4, 1),
        1 to PartitionInfo(37, 11, 7, 2),
        2 to PartitionInfo(34, 10, 10, 3),
        3 to PartitionInfo(30, 9, 14, 4),
        4 to PartitionInfo(27, 8, 17, 5),
        5 to PartitionInfo(24, 7, 20, 6),
        6 to PartitionInfo(20, 6, 24, 7)
    )

    private data class PartitionInfo(
        val companyPrefixBits: Int,
        val companyPrefixDigits: Int,
        val itemRefBits: Int,
        val itemRefDigits: Int
    )

    /**
     * Decode a hex EPC string to extract GTIN and serial number
     */
    fun decode(hexEpc: String): EpcDecodeResult {
        return try {
            // Clean and validate input
            val cleanHex = hexEpc.replace(" ", "").uppercase()

            // SGTIN-96 should be 24 hex characters (96 bits)
            if (cleanHex.length != 24) {
                return EpcDecodeResult.Failure("Invalid EPC length: expected 24 hex chars, got ${cleanHex.length}")
            }

            // Convert hex to binary string
            val binary = hexToBinary(cleanHex)
            if (binary.length != 96) {
                return EpcDecodeResult.Failure("Binary conversion failed")
            }

            // Extract header (bits 0-7)
            val header = binary.substring(0, 8).toInt(2)
            if (header != SGTIN_96_HEADER) {
                return EpcDecodeResult.Failure("Not SGTIN-96 format (header: 0x${header.toString(16)})")
            }

            // Extract filter (bits 8-10) - not needed for GTIN but useful info
            val filter = binary.substring(8, 11).toInt(2)

            // Extract partition (bits 11-13)
            val partition = binary.substring(11, 14).toInt(2)

            val partitionInfo = PARTITION_TABLE[partition]
                ?: return EpcDecodeResult.Failure("Invalid partition value: $partition")

            // Calculate bit positions
            val companyPrefixStart = 14
            val companyPrefixEnd = companyPrefixStart + partitionInfo.companyPrefixBits
            val itemRefEnd = companyPrefixEnd + partitionInfo.itemRefBits
            val serialStart = itemRefEnd
            val serialEnd = 96

            // Extract company prefix
            val companyPrefixBinary = binary.substring(companyPrefixStart, companyPrefixEnd)
            val companyPrefix = companyPrefixBinary.toLong(2)
                .toString()
                .padStart(partitionInfo.companyPrefixDigits, '0')

            // Extract item reference (includes indicator digit)
            val itemRefBinary = binary.substring(companyPrefixEnd, itemRefEnd)
            val itemRefValue = itemRefBinary.toLong(2)

            // The item reference includes the indicator digit
            // Total digits = itemRefDigits + 1 (for indicator)
            val itemRefWithIndicator = itemRefValue.toString()
                .padStart(partitionInfo.itemRefDigits + 1, '0')

            val indicator = itemRefWithIndicator.first()
            val itemReference = itemRefWithIndicator.drop(1).padStart(partitionInfo.itemRefDigits, '0')

            // Extract serial number
            val serialBinary = binary.substring(serialStart, serialEnd)
            val serialNumber = serialBinary.toLong(2).toString()

            // Construct GTIN-14 (without check digit first)
            // GTIN-14 = Indicator + Company Prefix + Item Reference + Check Digit
            val gtin13 = "$indicator$companyPrefix$itemReference"

            // Ensure we have 13 digits before adding check digit
            val gtin13Padded = gtin13.takeLast(13).padStart(13, '0')

            // Calculate check digit
            val checkDigit = calculateCheckDigit(gtin13Padded)
            val gtin14 = "$gtin13Padded$checkDigit"

            EpcDecodeResult.Success(
                gtin = gtin14,
                serialNumber = serialNumber,
                companyPrefix = companyPrefix,
                itemReference = itemReference,
                indicator = indicator.toString(),
                filter = filter
            )

        } catch (e: Exception) {
            EpcDecodeResult.Failure("Decode error: ${e.message}")
        }
    }

    /**
     * Convert hex string to binary string
     */
    private fun hexToBinary(hex: String): String {
        return hex.map { char ->
            char.digitToInt(16).toString(2).padStart(4, '0')
        }.joinToString("")
    }

    /**
     * Calculate GS1 check digit using modulo 10 algorithm
     */
    private fun calculateCheckDigit(digits: String): Int {
        var sum = 0
        digits.reversed().forEachIndexed { index, char ->
            val digit = char.digitToInt()
            // Odd positions (0-indexed, so even index) multiply by 3
            sum += if (index % 2 == 0) digit * 3 else digit
        }
        return (10 - (sum % 10)) % 10
    }
}

/**
 * Result of EPC decoding
 */
sealed class EpcDecodeResult {
    data class Success(
        val gtin: String,           // GTIN-14 with check digit
        val serialNumber: String,   // Serial number
        val companyPrefix: String,  // GS1 Company Prefix
        val itemReference: String,  // Item Reference
        val indicator: String,      // Indicator digit
        val filter: Int             // Filter value (packaging level)
    ) : EpcDecodeResult()

    data class Failure(val reason: String) : EpcDecodeResult()
}
