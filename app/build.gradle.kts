plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.mishipay.pos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.mishipay.pos"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    // Needed for AAR files
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Zebra RFID SDK - All AAR files from SDK v2.0.5.238
    implementation(files("src/main/libs/API3_READER-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_INTERFACE-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_CMN-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_TRANSPORT-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_ASCII-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_LLRP-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_NGE-protocolrelease-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_NGE-Transportrelease-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_NGEUSB-Transportrelease-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_ZIOTC-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/API3_ZIOTCTRANSPORT-release-2_0_5_238.aar"))
    implementation(files("src/main/libs/rfidseriallib.aar"))
    implementation(files("src/main/libs/rfidhostlib.aar"))
    implementation(files("src/main/libs/BarcodeScannerLibrary.aar"))

    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Compose - Use BOM 2023.10.01 (NOT 2024.01.00 due to compatibility issues)
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
