import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.android)
}

// Reads MAPS_API_KEY from local.properties (gitignored, per-developer — same pattern as sdk.dir
// in that same file) so a real Google Maps key never has to live in source control. Falls back to
// an empty string when it's not set, which lets the app still build and run everywhere except the
// map tiles themselves, rather than failing the build for anyone who hasn't added a key yet.
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}
val mapsApiKey: String = localProperties.getProperty("MAPS_API_KEY", "")

// Backend base URL, same local.properties override pattern as MAPS_API_KEY above. Defaults to
// 10.0.2.2 — the special alias the Android emulator resolves to the host machine's own localhost —
// so `dotnet run`'s API at localhost:5128 is reachable with zero setup on an emulator. A physical
// device can't reach 10.0.2.2 (it's emulator-only): set API_BASE_URL in local.properties to the
// host machine's real LAN IP (e.g. http://192.168.1.23:5128/) instead. No "api/" suffix here — per
// ApiService.kt (Dinil's convention, kept during the 2026-09-26 merge), every endpoint path
// includes its own leading "api/" segment instead.
val apiBaseUrl: String = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:5128/")

android {
    namespace = "com.smartmicrogrid"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.smartmicrogrid"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // ---- Migara: ZXing Core for QR Code GENERATION (Displaying to user) ----
    implementation("com.google.zxing:core:3.5.3")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // ---- Networking (verify-qr call) ----
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ---- Coroutines (network calls off the main thread) ----
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ---- CameraX (camera preview + frame analysis) ----
    implementation("androidx.camera:camera-core:1.4.0")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    // ---- ML Kit barcode scanning (QR decode) ----
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // ---- Runtime permissions (camera) ----
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
}