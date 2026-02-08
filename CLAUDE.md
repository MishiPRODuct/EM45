# CLAUDE.md - MishiPay POS App Project Notes

## Project Overview

Mobile POS Android app for Zebra EM45 RFID Enterprise Mobile device. Based on MishiPay design, uses Jetpack Compose (Material 3) with Zebra RFID SDK v2.0.5.238.

## APK Distribution

**Latest APK:** `MishiPayPOS-v1.4-gtin-fix.apk` (18 MB)
**Location:** `/Users/theo/Downloads/MishiPayPOS-v1.4-gtin-fix.apk`

### Version History
- **v1.4** - Fixed GTIN-14 decoding: itemRefDigits already includes the indicator digit per GS1 spec, code was adding +1 causing the indicator to be dropped and item reference to gain an extra zero digit. GTINs now match physical barcodes.
- **v1.3** - Fixed EPC decode failing silently: Zebra SDK tagID contains invisible chars that broke the 24-char length check. Now strips all non-hex characters. Added debug logging to BasketViewModel.
- **v1.2** - Added Serial Number (AI 21) display alongside GTIN (AI 01)
- **v1.1** - Added EPC to SKU (GTIN-14) decoding using SGTIN-96 algorithm
- **v1.0** - Initial release with RFID scanning and basket management

### Installation on EM45 Devices

**Via ADB:**
```bash
adb install MishiPayPOS-v1.0.apk
```

**Via File Transfer:**
Transfer APK to device and tap to install.

### Required Device Settings

1. **Enable "Install from unknown sources"**
   - Settings → Security → Unknown sources → Enable

2. **USB Debugging** (only for ADB install)
   - Settings → Developer options → USB debugging → Enable

### Permissions (Auto-requested)
The app requests these permissions on first launch - user must tap **Allow**:
- Bluetooth (BLUETOOTH_CONNECT, BLUETOOTH_SCAN)
- Location (ACCESS_FINE_LOCATION - required for Bluetooth/RFID on Android)

### Important Usage Notes
1. **Unplug charging cable before scanning** - RFID radio disabled while charging
2. **Grant all permissions** when prompted on first launch
3. Works only on **Zebra EM45** devices (uses built-in RFID reader)

## Critical Version Compatibility

The Kotlin and Compose versions **must** be aligned exactly as follows:

| Component | Version | Notes |
|-----------|---------|-------|
| Kotlin | 1.9.20 | Do NOT upgrade without checking Compose compatibility |
| Compose Compiler Extension | 1.5.5 | Must match Kotlin version |
| Compose BOM | **2023.10.01** | Do NOT use 2024.01.00 |

**WARNING**: Using Compose BOM `2024.01.00` causes a runtime `NoSuchMethodError` crash in `CircularProgressIndicator` due to incompatibility with compiler extension 1.5.5.

## EM45 RFID Reader Configuration

### Transport Type (Critical)
```kotlin
readers = Readers(context, ENUM_TRANSPORT.RE_SERIAL)
```

| Device | ENUM_TRANSPORT |
|--------|---------------|
| EM45 / TC53E / ET6x (built-in) | `RE_SERIAL` |
| USB-connected readers | `SERVICE_USB` |
| Bluetooth sleds (RFD8500, RFD40) | `BLUETOOTH` |

**Do NOT use** `SERVICE_SERIAL`, `BLUETOOTH`, or `ALL` for EM45's built-in reader.

### Reader Discovery Retry Logic
The RFID service needs time to enumerate. Always use retry logic:
```kotlin
for (attempt in 1..5) {
    availableReaders = readers?.GetAvailableRFIDReaderList()
    if (!availableReaders.isNullOrEmpty()) break
    Thread.sleep(1000)
}
```

### Charging Source Restriction
The EM45 RFID radio **cannot operate while charging** (USB-C, cradle, or wireless). The SDK throws `OperationFailureException` with `vendorMessage: "Charging source connected"`.

For ADB debugging without USB:
```bash
adb tcpip 5555
adb connect <device-ip>:5555
# Then unplug USB cable
```

## App Flow

```
Welcome Screen → Empty Basket → Scan Screen → Basket with Items
      ↓               ↓              ↓               ↓
"Touch here     "Scan items"   Auto-connect    "Scan more" or
 to start"       button         + scan        "Finish and pay"
```

## EPC to SKU Decoding (SGTIN-96)

The app decodes RFID EPC tags to extract GTIN-14 and Serial Number using the GS1 SGTIN-96 standard.

### How It Works

**Verified Example (v1.4):**
- EPC: `303574530C05536BC5AC7849`
- **AI 01 (GTIN-14):** `06100163054538`
- **AI 21 (Serial):** `3320357897`

**SGTIN-96 Bit Structure (96 bits):**
```
| Header | Filter | Partition | Company Prefix | Item Reference | Serial |
| 8 bits | 3 bits |  3 bits   |   20-40 bits   |   24-4 bits    | 38 bits|
```

**Partition Table:**
| Partition | Company Prefix (bits/digits) | Item Ref (bits/digits) |
|-----------|------------------------------|------------------------|
| 0         | 40 / 12                      | 4 / 1                  |
| 1         | 37 / 11                      | 7 / 2                  |
| 2         | 34 / 10                      | 10 / 3                 |
| 3         | 30 / 9                       | 14 / 4                 |
| 4         | 27 / 8                       | 17 / 5                 |
| 5         | 24 / 7                       | 20 / 6                 |
| 6         | 20 / 6                       | 24 / 7                 |

**Critical: Item Ref digits INCLUDE the indicator digit.** For partition 5, Item Ref digits = 6 means 1 indicator digit + 5 item reference digits. Do NOT add +1 when padding.

### GTIN-14 Construction Steps

1. **Clean EPC hex** — strip ALL non-hex characters (Zebra SDK tagID may contain `\0`, `\n`, etc.)
2. **Parse binary** — extract partition, company prefix, item reference, serial from bit positions
3. **Pad item reference** — to `itemRefDigits` digits (this already includes the indicator)
4. **Split** — first digit = indicator, remaining digits = item reference
5. **Build GTIN-13** — `indicator + companyPrefix + itemReference` = 13 digits
6. **Calculate check digit** — GS1 mod-10 algorithm on the 13 digits
7. **GTIN-14** — `GTIN-13 + checkDigit` = 14 digits

**Worked example** (EPC `303574530C05536BC5AC7849`, partition 5):
```
Company Prefix (7 digits): 6100163
Item Ref value (binary):   054538 (padded to 6 digits)
Split:                     indicator=0, itemRef=54538 (5 digits)
GTIN-13:                   0 + 6100163 + 054538 → but wait, 0610016305453 is 13 digits
                           Actually: 0 6100163 054538 → dropped to 13 → 0610016305453
Check digit:               8
GTIN-14:                   06100163054538
```

### Important: GTIN vs Product Barcode

The GTIN decoded from the EPC tag may **NOT match** the barcode printed on the product. This happens because:

1. **Different encoding entity** - Tag encoded by retailer, barcode by manufacturer
2. **Internal SKU mapping** - Retailers often use their own GTIN assignments
3. **Requires lookup table** - Map decoded GTIN → product catalog/barcode

To match products correctly, you may need a database mapping EPC GTINs to your product catalog.

### Decoder Location
`app/src/main/java/com/mishipay/pos/domain/utils/EpcDecoder.kt`

## Project Structure

```
app/src/main/java/com/mishipay/pos/
├── App.kt
├── data/
│   └── RfidReaderManager.kt        # SDK wrapper (RE_SERIAL, retry logic, charging error)
├── domain/
│   ├── RfidModels.kt               # RfidTag, ReaderState
│   ├── BasketModels.kt             # BasketItem, Basket (with EPC + SKU fields)
│   └── utils/
│       └── EpcDecoder.kt           # SGTIN-96 EPC to GTIN-14 decoder
└── presentation/
    ├── MainActivity.kt             # Entry point, permissions
    ├── navigation/
    │   └── NavGraph.kt             # Compose Navigation
    ├── viewmodel/
    │   ├── RfidViewModel.kt        # RFID state management
    │   └── BasketViewModel.kt      # Basket state management
    └── ui/
        ├── theme/
        │   └── Theme.kt            # MishiPay colors
        └── screens/
            ├── WelcomeScreen.kt
            ├── BasketScreen.kt     # Empty + filled states
            └── ScanScreen.kt
```

## Building

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
cd /Users/theo/Downloads/em45files/mishipay-pos
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

## Known Issues & Solutions

1. **App crash on launch** → Check Compose BOM version (must be 2023.10.01)
2. **"No RFID readers found"** → Verify using `RE_SERIAL` transport, check retry logic
3. **"Charging source connected"** → Unplug device from all charging sources
4. **Manifest merger conflict** → Use `tools:replace="android:allowBackup"` in AndroidManifest.xml

## Bugs Fixed (Reference)

### v1.3 — Zebra SDK Invisible Characters
**Problem:** EPC decode silently failed at runtime. App showed only raw EPC hex strings, no GTIN or serial number.
**Root cause:** `tagData.tagID` from Zebra SDK contains invisible characters (`\0`, `\n`, etc.) beyond spaces. The original code only stripped spaces with `.replace(" ", "")`, so the cleaned string was longer than 24 chars and failed the length check.
**Fix:** Changed to `.filter { it in "0123456789abcdefABCDEF" }` to strip ALL non-hex characters.
**File:** `EpcDecoder.kt` line 41

### v1.4 — GTIN-14 Indicator Digit Padding
**Problem:** Decoded GTIN-14 didn't match the barcode on the physical product. Example: decoded `61001630054532` but physical tag says `6100163054538`.
**Root cause:** The GS1 partition table's `itemRefDigits` value already includes the indicator digit (e.g., partition 5: itemRefDigits=6 means 1 indicator + 5 item ref). The code was treating it as excluding the indicator and adding `+1`, causing:
1. Item reference padded to 7 digits instead of 6
2. gtin13 became 14 chars instead of 13
3. `takeLast(13)` dropped the indicator digit
4. Extra zero inserted in item reference
5. Wrong check digit calculated

**Fix:** Changed `.padStart(partitionInfo.itemRefDigits + 1, '0')` to `.padStart(partitionInfo.itemRefDigits, '0')` and removed redundant `.padStart()` after `.drop(1)`.
**File:** `EpcDecoder.kt` lines 88-92

## SDK Documentation

- [Zebra RFID SDK Connection Management](https://techdocs.zebra.com/dcs/rfid/android/2-0-4-192/guide/connection/)
- [EM45 Integrated RFID User Guide](https://docs.zebra.com/us/en/mobile-computers/handheld/em45-series/em45-rfid-ug/c-em45-data-capture/r-scanning-considerations/c-em45-integrated-rfid.html)
