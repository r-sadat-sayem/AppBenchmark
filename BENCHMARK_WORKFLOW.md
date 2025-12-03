# Benchmark Workflow Guide

## 📋 Table of Contents
1. [How It Works](#how-it-works)
2. [Running Benchmarks](#running-benchmarks)
3. [Product Flavors Explained](#product-flavors-explained)
4. [Troubleshooting](#troubleshooting)
5. [Advanced Usage](#advanced-usage)

---

## 🔧 How It Works

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                      Benchmark Workflow                      │
└─────────────────────────────────────────────────────────────┘

1. BUILD PHASE
   ├── Gradle builds baselineDebug APK
   ├── Gradle builds heavyDebug APK
   └── Both APKs contain BenchmarkSDK

2. TEST PHASE (./gradlew runBenchmarks)
   ├── Install & run connectedBaselineDebugAndroidTest
   │   ├── App launches with baseline configuration
   │   ├── BenchmarkSDK collects metrics automatically
   │   └── Metrics saved: /sdcard/.../benchmark-baseline.json
   │
   ├── Install & run connectedHeavyDebugAndroidTest
   │   ├── App launches with heavy configuration
   │   ├── BenchmarkSDK collects metrics automatically
   │   └── Metrics saved: /sdcard/.../benchmark-heavy.json
   │
   └── Tests complete

3. REPORT PHASE (auto-triggered)
   ├── adb pull benchmark-baseline.json
   ├── adb pull benchmark-heavy.json
   ├── Python script: generate_report.py
   │   ├── Compares baseline vs heavy
   │   ├── Calculates change percentages
   │   └── Determines severity levels
   └── Output: benchmark-results/benchmarks/report.html

4. RESULT
   └── Open report.html in browser to view comparison
```

### File Flow

```
Device Storage                     Local Machine
─────────────────                 ─────────────────
/sdcard/Android/data/             benchmark-results/
io.app.benchmark/                 └── benchmarks/
└── files/                            ├── benchmark-baseline.json
    └── benchmarks/                   ├── benchmark-heavy.json
        ├── benchmark-baseline.json ──┘ └── report.html
        └── benchmark-heavy.json ────────┘
```

---

## 🚀 Running Benchmarks

### Method 1: Complete Suite (Recommended)

Run everything with a single command:

```bash
./gradlew runBenchmarks
```

**What happens:**
1. Runs `connectedBaselineDebugAndroidTest` (installs baseline APK, runs tests)
2. Runs `connectedHeavyDebugAndroidTest` (installs heavy APK, runs tests)
3. Pulls JSON files from device
4. Generates HTML report

**Output:**
```
═══════════════════════════════════════════════════════════════
🚀 Starting Benchmark Suite
═══════════════════════════════════════════════════════════════

   📋 Selected benchmark test tasks:
      • connectedBaselineDebugAndroidTest
      • connectedHeavyDebugAndroidTest
   
   ───────────────────────────────────────────────────────────────

[Test execution logs...]

═══════════════════════════════════════════════════════════════
📥 Pulling Benchmark Data from Device
═══════════════════════════════════════════════════════════════

   📱 Pulling benchmark-baseline.json...
   ✅ Successfully pulled benchmark-baseline.json
   
   📱 Pulling benchmark-heavy.json...
   ✅ Successfully pulled benchmark-heavy.json
   
   ───────────────────────────────────────────────────────────────

Report data generated: benchmark-results/report.json

═══════════════════════════════════════════════════════════════
✅ Benchmark Report Generated Successfully!
═══════════════════════════════════════════════════════════════

📊 Report location: /path/to/benchmark-results/benchmarks/report.html
🌐 Open in browser: file:///path/to/benchmark-results/benchmarks/report.html

═══════════════════════════════════════════════════════════════
```

### Method 2: Individual Flavors

Run specific benchmark scenarios:

```bash
# Baseline only
./gradlew runBenchmarks -PbenchFlavors=baseline

# Heavy only
./gradlew runBenchmarks -PbenchFlavors=heavy

# Both (explicit)
./gradlew runBenchmarks -PbenchFlavors=baseline,heavy
```

### Method 3: Report Only

If you already have JSON files on device:

```bash
./gradlew generateBenchmarkReport
```

This skips tests and just:
1. Pulls JSON files from device
2. Generates report

### Method 4: Manual (No Gradle)

For full manual control:

```bash
# 1. Run tests manually in Android Studio or via command
./gradlew connectedBaselineDebugAndroidTest
./gradlew connectedHeavyDebugAndroidTest

# 2. Pull files manually
adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks/benchmark-baseline.json benchmark-results/benchmarks/
adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks/benchmark-heavy.json benchmark-results/benchmarks/

# 3. Generate report manually
cd benchmark-sdk/scripts
python3 generate_report.py
```

---

## 🎯 Product Flavors Explained

### What Are Product Flavors?

Product flavors are **build variants** in Android that let you create different versions of your app from the same codebase.

Think of it like:
- **Baseline** = Your app running normally
- **Heavy** = Your app under stress/heavy load

### Why Use Flavors for Benchmarking?

| Approach | Pros | Cons |
|----------|------|------|
| **Product Flavors** ✅ | Single codebase, easy config, Gradle automation | Requires build variants |
| Separate apps | Full isolation | Code duplication, hard to maintain |
| Runtime flags | Simple | Harder to enforce clean comparisons |

### Flavor Configuration

Located in `app/build.gradle.kts`:

```kotlin
android {
    flavorDimensions += "scenario"
    
    productFlavors {
        create("baseline") {
            dimension = "scenario"
            applicationIdSuffix = ".baseline"
            versionNameSuffix = "-baseline"
            
            // Custom configuration
            buildConfigField("String", "BENCH_SCENARIO", "\"baseline\"")
            buildConfigField("Int", "MAX_CONCURRENT_REQUESTS", "2")
            buildConfigField("Boolean", "ENABLE_HEAVY_FEATURES", "false")
        }
        
        create("heavy") {
            dimension = "scenario"
            applicationIdSuffix = ".heavy"
            versionNameSuffix = "-heavy"
            
            // Custom configuration
            buildConfigField("String", "BENCH_SCENARIO", "\"heavy\"")
            buildConfigField("Int", "MAX_CONCURRENT_REQUESTS", "20")
            buildConfigField("Boolean", "ENABLE_HEAVY_FEATURES", "true")
        }
    }
}
```

### Using Flavor-Specific Code

**In your app code:**

```kotlin
val scenario = BuildConfig.BENCH_SCENARIO  // "baseline" or "heavy"
val maxRequests = BuildConfig.MAX_CONCURRENT_REQUESTS  // 2 or 20

if (BuildConfig.ENABLE_HEAVY_FEATURES) {
    // Enable analytics, background sync, etc.
}
```

**Flavor-specific source sets:**

```
app/src/
├── main/                  # Shared code (both flavors)
├── baseline/              # Baseline-only code
│   └── kotlin/
│       └── ScenarioConfig.kt
├── heavy/                 # Heavy-only code
│   └── kotlin/
│       └── ScenarioConfig.kt
└── androidTest/           # Shared tests
```

### Generated Variants

From 2 flavors + 2 build types (debug/release), you get 4 variants:

| Variant | Package ID | Purpose |
|---------|-----------|---------|
| `baselineDebug` | `io.app.benchmark.baseline` | Benchmark testing |
| `baselineRelease` | `io.app.benchmark.baseline` | Production baseline |
| `heavyDebug` | `io.app.benchmark.heavy` | Benchmark testing |
| `heavyRelease` | `io.app.benchmark.heavy` | Production heavy load |

---

## 🐛 Troubleshooting

### Issue: "No benchmark JSON files found"

**Symptoms:**
```
⚠️  File not found on device: benchmark-baseline.json
Make sure you ran the baselineDebug variant first!
```

**Causes:**
1. Tests didn't run or failed
2. BenchmarkSDK didn't persist metrics
3. Wrong package name or file path

**Solutions:**

```bash
# 1. Check if tests ran successfully
./gradlew connectedBaselineDebugAndroidTest --info

# 2. Verify files exist on device
adb shell ls -la /sdcard/Android/data/io.app.benchmark/files/benchmarks/

# 3. Check logcat for BenchmarkSDK messages
adb logcat | grep BenchmarkSDK

# 4. Manually trigger metric collection (if app has debug button)
# Launch app and tap "Persist Metrics" button
```

### Issue: "Report generation failed"

**Symptoms:**
```
⚠️  Report Generation Failed
```

**Causes:**
1. Missing Python dependencies
2. Missing one or both JSON files
3. Malformed JSON

**Solutions:**

```bash
# 1. Check Python is installed
python3 --version

# 2. Verify JSON files exist and are valid
ls -la benchmark-results/benchmarks/
cat benchmark-results/benchmarks/benchmark-baseline.json | python3 -m json.tool

# 3. Run Python script manually to see errors
cd benchmark-sdk/scripts
python3 generate_report.py

# 4. Check both JSON files are present
# Need BOTH baseline AND heavy for comparison
```

### Issue: "Tests timeout or fail"

**Symptoms:**
```
FAILURE: Build failed with an exception.
> Execution failed for task ':app:connectedBaselineDebugAndroidTest'.
```

**Solutions:**

```bash
# 1. Increase test timeout in app/build.gradle.kts
android {
    defaultConfig {
        testInstrumentationRunnerArguments["timeoutInMinutes"] = "10"
    }
}

# 2. Check emulator/device is connected
adb devices

# 3. Clear app data before running
adb shell pm clear io.app.benchmark

# 4. Run tests with detailed logging
./gradlew connectedBaselineDebugAndroidTest --info --stacktrace
```

### Issue: "Wrong metrics in report"

**Symptoms:**
- Unexpected values
- Missing custom metrics
- Old data showing up

**Solutions:**

```bash
# 1. Clean old data
rm -rf benchmark-results/benchmarks/*.json
adb shell rm -rf /sdcard/Android/data/io.app.benchmark/files/benchmarks/*

# 2. Rebuild and re-run
./gradlew clean
./gradlew runBenchmarks

# 3. Verify custom metrics are registered BEFORE collectAndPersist()
# Check MainActivity.onCreate() calls defineMetric() first
```

### Issue: "adb command not found"

**Symptoms:**
```
adb: command not found
```

**Solution:**

```bash
# Add Android SDK platform-tools to PATH
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Or on Mac with Android Studio:
export PATH=$PATH:~/Library/Android/sdk/platform-tools

# Verify
which adb
adb version
```

---

## 🎓 Advanced Usage

### Filtering Test Execution

**Run specific variants:**

```bash
# By variant name
./gradlew runBenchmarks -PbenchVariants=baselineDebug

# By flavor
./gradlew runBenchmarks -PbenchFlavors=baseline

# Multiple
./gradlew runBenchmarks -PbenchFlavors=baseline,heavy
```

### Running Tests in Android Studio

1. Open **Run Configurations**
2. Select `connectedBaselineDebugAndroidTest`
3. Click **Run** ▶️
4. Repeat for `connectedHeavyDebugAndroidTest`
5. Run `./gradlew generateBenchmarkReport` to create comparison

### Automating in CI/CD

**GitHub Actions:**
```yaml
- name: Run Benchmarks
  run: ./gradlew runBenchmarks
  
- name: Upload Report
  uses: actions/upload-artifact@v4
  with:
    name: benchmark-report
    path: benchmark-results/
```

**Compare against baseline in PR:**
```yaml
- name: Download Previous Report
  uses: actions/download-artifact@v4
  with:
    name: benchmark-report
    path: previous-results/

- name: Compare Results
  run: |
    python3 scripts/compare_benchmarks.py \
      previous-results/report.json \
      benchmark-results/report.json
```

### Custom Scenarios

Add a third flavor for specific testing:

```kotlin
productFlavors {
    // ...existing flavors...
    
    create("database") {
        dimension = "scenario"
        applicationIdSuffix = ".database"
        buildConfigField("String", "BENCH_SCENARIO", "\"database\"")
        buildConfigField("Boolean", "ENABLE_DB_STRESS_TEST", "true")
    }
}
```

Then run:
```bash
./gradlew runBenchmarks -PbenchFlavors=database
```

### Viewing Historical Trends

Save reports with timestamps:

```bash
# After each run, archive the report
timestamp=$(date +%Y%m%d_%H%M%S)
cp benchmark-results/benchmarks/report.html "benchmark-results/archive/report_${timestamp}.html"
cp benchmark-results/benchmarks/report.json "benchmark-results/archive/report_${timestamp}.json"
```

---

## 📚 Additional Resources

- [README.md](../README.md) - Project overview
- [QUICKSTART_CUSTOM_METRICS.md](../QUICKSTART_CUSTOM_METRICS.md) - Add custom metrics
- [SCHEMA_GUIDE.md](../benchmark-sdk/SCHEMA_GUIDE.md) - Metric schema reference
- [API_DOCUMENTATION.md](../API_DOCUMENTATION.md) - SDK API reference

---

## 🆘 Still Having Issues?

1. **Check logs:** `adb logcat | grep BenchmarkSDK`
2. **Verify setup:** Ensure emulator/device is connected
3. **Clean build:** `./gradlew clean`
4. **Review examples:** See `app/src/androidTest/` for test examples

If problems persist, review the test code in `app/src/androidTest/kotlin/` for reference implementations.

