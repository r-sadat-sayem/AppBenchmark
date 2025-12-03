plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // The benchmark plugin is typically applied to the 'app' module,
    // but we can leave it here if this specific module builds the benchmark test APK.
    alias(libs.plugins.androidx.benchmark)
}

android {
    namespace = "io.app.benchmark.sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")

        // This is necessary for Macrobenchmarks in test modules, leave as is.
        experimentalProperties["android.experimental.self-instrumenting"] = true

        // These suppressions are temporary workarounds and not recommended for accurate results.
        // It is better to fix the underlying issues (use a physical device and a non-debuggable build).
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR,DEBUGGABLE,NOT-SELF-INSTRUMENTING"
    }

    // NOTE: isDebuggable property is not directly accessible in Library modules in recent AGP versions (see notes below)
    buildTypes {
        getByName("release") {
            // isMinifyEnabled should usually be true for a release/benchmark build to get realistic performance data
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.junit)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    // Removed codegen from a library module implementation scope if it's not being used for annotation processing here

    // Changed `api` to `androidTestImplementation` for benchmark dependencies,
    // as they are typically only used during the test execution phase.
    androidTestImplementation(libs.androidx.benchmark.common)
    androidTestImplementation(libs.androidx.benchmark.junit4)
    androidTestImplementation(libs.androidx.benchmark.macro)
}