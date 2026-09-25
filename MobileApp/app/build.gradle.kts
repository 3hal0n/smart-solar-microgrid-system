import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
// host machine's real LAN IP (e.g. http://192.168.1.23:5128/api/) instead.
val apiBaseUrl: String = localProperties.getProperty("API_BASE_URL", "http://10.0.2.2:5128/api/")

android {
    namespace = "com.smartmicrogrid"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.smartmicrogrid"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["mapsApiKey"] = mapsApiKey
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
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
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.maps)
    implementation(libs.play.services.location)
    implementation(libs.maps.compose)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.logging.interceptor)
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
}