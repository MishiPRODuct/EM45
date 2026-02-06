# CLAUDE.md - MishiPay POS App Project Notes

## Project Overview

Mobile POS Android app for Zebra EM45 RFID Enterprise Mobile device. Based on MishiPay design, uses Jetpack Compose (Material 3) with Zebra RFID SDK v2.0.5.238.

## APK Distribution

**Latest APK:** `MishiPayPOS-v1.1-epc-decode.apk` (18 MB)
**Location:** `/Users/theo/Downloads/MishiPayPOS-v1.1-epc-decode.apk`

### Version History
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

The app decodes RFID EPC tags to extract GTIN-14 (SKU) using the GS1 SGTIN-96 standard.

### How It Works

**Example:**
- EPC: `30396062C3C54AC22B333047`
- Decoded GTIN-14: `03608439884597`

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

### Important: GTIN vs Product Barcode

The GTIN decoded from the EPC tag may **NOT match** the barcode printed on the product. This happens because:

1. **Different encoding entity** - Tag encoded by retailer, barcode by manufacturer
2. **Internal SKU mapping** - Retailers often use their own GTIN assignments
3. **Requires lookup table** - Map decoded GTIN → product catalog/barcode

**Example mismatch:**
- EPC GTIN: `03608439884597` (what the tag contains)
- Product barcode: `9314709575xxx` (what's printed on the item)

To match products correctly, you'll need a database mapping EPC GTINs to your product catalog.

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

## SDK Documentation

- [Zebra RFID SDK Connection Management](https://techdocs.zebra.com/dcs/rfid/android/2-0-4-192/guide/connection/)
- [EM45 Integrated RFID User Guide](https://docs.zebra.com/us/en/mobile-computers/handheld/em45-series/em45-rfid-ug/c-em45-data-capture/r-scanning-considerations/c-em45-integrated-rfid.html)
