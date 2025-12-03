plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.app.benchmark"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.app.benchmark"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "BENCH_SCENARIO", "\"baseline\"")
    }
    productFlavors {
        create("baseline") {
            dimension = "bench"
            buildConfigField("String", "BENCH_SCENARIO", "\"baseline\"")
        }
        create("heavy") {
            dimension = "bench"
            buildConfigField("String", "BENCH_SCENARIO", "\"heavy\"")
        }
    }
    flavorDimensions += "bench"

    buildTypes {
        getByName("debug") {
            isJniDebuggable = true
            versionNameSuffix = "-debug"
            // Removed applicationIdSuffix - keeps same package across flavors
            // This prevents uninstall/reinstall between test runs
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    testOptions {
        animationsDisabled = true
        // Prevents uninstall between test runs
        execution = "ANDROIDX_TEST_ORCHESTRATOR"

        unitTests.all {
            it.useJUnitPlatform() // optional, for unit tests only
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Testing
    implementation(libs.androidx.junit)
    implementation(libs.androidx.runner)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.rules)


    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestUtil("androidx.test:orchestrator:1.4.2")

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Benchmark SDK
    implementation(project(":benchmark-sdk"))
}