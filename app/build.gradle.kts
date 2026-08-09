plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.talon.demo"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.talon.demo"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-mvp"
        ndk {
            abiFilters += "arm64-v8a" // Arm-only, matches the challenge's target hardware
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Built locally via `sh scripts/build_android_library.sh` in the
    // executorch repo (see README Step 1), then copied here or referenced
    // via a flat AAR repo. Not published to Maven Central as of this
    // writing — check the executorch repo for the current recommended
    // dependency method before building.
    implementation(files("libs/executorch.aar"))
}
