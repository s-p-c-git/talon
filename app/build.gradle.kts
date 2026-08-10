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
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            // Arm-only by default, matching the challenge's target hardware —
            // override with -PabiFilters=x86_64 (comma-separated) for a CI-only
            // build that runs on the x86_64 Android emulator GH-hosted runners
            // actually have (see .github/workflows/build.yml's
            // emulator-smoke-test job). Never override this for a real build.
            val abis = (project.findProperty("abiFilters") as String? ?: "arm64-v8a").split(",")
            abiFilters.addAll(abis)
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

    // ExecuTorch is now published to Maven Central — no source build
    // required for the default XNNPACK/KleidiAI backend this demo uses.
    // fbjni + nativeloader come in transitively via this artifact's POM.
    //
    // CI-verified 2026-08-10: pinning to 1.1.0 (what the reference app's
    // build.gradle.kts on GitHub main happened to use) does NOT compile —
    // `LlmCallback.onError()` and `LlmModule`'s `Closeable`/`close()` exist
    // in the pytorch/executorch GitHub main branch source but aren't in
    // the 1.1.0 *released* artifact; they were added in a later release.
    // Main-branch source and a specific pinned release are not the same
    // thing — reading GitHub source alone can't catch this, only
    // compiling against the actual binary artifact does. Bumped to the
    // latest published release; re-verify against
    // https://repo1.maven.org/maven2/org/pytorch/executorch-android/
    // before bumping further, and re-run CI, don't assume.
    implementation("org.pytorch:executorch-android:1.4.0")

    // Only needed if you build a custom ExecuTorch AAR from source (e.g. to
    // add a non-default backend like QNN or Vulkan) via
    // `scripts/setup.sh --build-aar`. Uncomment and remove the Maven
    // dependency above if you do.
    // implementation(files("libs/executorch.aar"))

    // CI-only: drives app/src/androidTest/.../SmokeTest.kt (real emulator
    // install + model load + generate, see build.yml's emulator-smoke-test
    // job). Not Espresso -- ActivityScenario + direct view access is enough
    // for this app's two plain-click buttons, so no view-matcher dependency.
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
}
