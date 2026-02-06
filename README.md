# MishiPay POS - RFID Scanner App

Mobile POS application for Zebra EM45 RFID devices.

## Features

- Touch to start welcome screen
- RFID tag scanning with automatic reader connection
- **EPC to SKU decoding** (SGTIN-96 → GTIN-14)
- Shopping basket with item management
- Delete items from basket
- Running total in SAR
- Scan more items functionality

## Installation

### APK File
The ready-to-install APK is located at:
```
MishiPayPOS-v1.1-epc-decode.apk
```

### Install via ADB
```bash
adb install MishiPayPOS-v1.1-epc-decode.apk
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

The app automatically decodes SGTIN-96 EPC tags to extract the GTIN-14 (SKU).

**Display format:**
- **Primary:** Decoded SKU (e.g., `03608439884597`)
- **Secondary:** Full EPC in gray (e.g., `EPC: 30396062C3C54AC22B333047`)

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
