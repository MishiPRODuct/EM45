# MishiPay POS - RFID Scanner App

Mobile POS application for Zebra EM45 RFID devices.

## Current Version: v1.4

## Version History

| Version | APK | Changes |
|---------|-----|---------|
| **v1.4** | `MishiPayPOS-v1.4-gtin-fix.apk` | Fixed GTIN-14 decoding — indicator digit padding corrected so decoded GTINs now match physical barcodes |
| **v1.3** | `MishiPayPOS-v1.3-fix-decode.apk` | Fixed EPC decode failing silently — strips invisible characters from Zebra SDK tagID |
| **v1.2** | `MishiPayPOS-v1.2-serial.apk` | Added Serial Number (AI 21) display alongside GTIN (AI 01) |
| **v1.1** | `MishiPayPOS-v1.1-epc-decode.apk` | Added EPC to SKU (GTIN-14) decoding using SGTIN-96 algorithm |
| **v1.0** | `MishiPayPOS-v1.0.apk` | Initial release — RFID scanning and basket management |

## Features

- Touch to start welcome screen
- RFID tag scanning with automatic reader connection
- **EPC to SKU decoding** (SGTIN-96 → GTIN-14)
- **Serial Number extraction** (AI 21)
- Shopping basket with item management
- Delete items from basket
- Running total in SAR
- Scan more items functionality

## Installation

### APK File
The latest ready-to-install APK:
```
MishiPayPOS-v1.4-gtin-fix.apk
```

### Install via ADB
```bash
adb install MishiPayPOS-v1.4-gtin-fix.apk
```

### Install via File Transfer
1. Copy APK to device
2. Open file manager on device
3. Tap the APK to install

## Device Requirements

- **Device:** Zebra EM45 (with built-in RFID reader)
- **Android:** 8.0+ (API 26+)

## Required Settings

### Before Installing
- Settings → Security → Enable "Unknown sources"

### Before Scanning
- **Unplug all charging cables** (USB, wireless, cradle)
- RFID radio cannot operate while charging

### Permissions
Grant these permissions when prompted:
- Bluetooth
- Location

## App Flow

1. **Welcome Screen** → Tap "Touch here to start"
2. **Empty Basket** → Tap "Scan items"
3. **Scanning** → Hold items near device, tags appear automatically
4. **Basket** → View items with decoded SKU + full EPC, delete unwanted items
5. **Scan More** → Add more items to basket
6. **Finish and Pay** → (Coming soon)

## EPC Decoding

The app automatically decodes SGTIN-96 EPC tags to extract GTIN and Serial Number per GS1 standards.

**Display format:**
- **AI 01 (GTIN):** `03608439884597`
- **AI 21 (Serial):** `9314709575`
- **EPC:** `30396062C3C54AC22B333047`

**Note:** The decoded GTIN may not match the product barcode if the tag was encoded by a different entity (retailer vs manufacturer). A lookup table may be needed to map GTINs to your product catalog.

## Building from Source

### Requirements
- Java JDK 17
- Android SDK (API 34)

### Build Commands
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17
./gradlew assembleDebug
```

### Output
```
app/build/outputs/apk/debug/app-debug.apk
```

## Technical Notes

See `CLAUDE.md` for detailed technical documentation including:
- Version compatibility requirements
- RFID SDK configuration
- Known issues and solutions

## Support

For issues related to:
- **RFID not working:** Ensure device is unplugged from charging
- **App won't install:** Enable "Unknown sources" in Settings
- **Permissions denied:** Go to Settings → Apps → MishiPay POS → Permissions
