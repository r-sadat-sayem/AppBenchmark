# Benchmark Workflow Guide

**Updated:** December 4, 2025  
**Version:** 3.0 (Phase 3: Startup Time Metrics)

## 📋 Table of Contents
1. [How It Works](#how-it-works)
2. [Core Metrics Collected](#core-metrics-collected)
3. [Running Benchmarks](#running-benchmarks)
4. [Product Flavors Explained](#product-flavors-explained)
5. [Troubleshooting](#troubleshooting)
6. [Advanced Usage](#advanced-usage)

---

## 🔧 How It Works

### Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│              Benchmark Workflow (Phase 3)                    │
└─────────────────────────────────────────────────────────────┘

1. BUILD PHASE
   ├── Gradle builds baselineDebug APK
   ├── Gradle builds heavyDebug APK
   ├── Both use same package: io.app.benchmark ✅
   └── Both APKs contain BenchmarkSDK

2. TEST PHASE (./gradlew runBenchmarkTests)
   ├── Install app once (same package for all flavors) ✅
   ├── Run connectedBaselineDebugAndroidTest
   │   ├── App launches with baseline configuration
   │   ├── BenchmarkSDK collects 60+ metrics across 11 categories
   │   │   ├── 🚀 Startup metrics (Cold, Warm, Hot) - MANDATORY
   │   │   ├── ⚡ CPU & Performance
   │   │   ├── 🧠 Memory & Heap
   │   │   ├── 🌐 Network & API
   │   │   ├── 💾 Storage & Database
   │   │   ├── 🎨 UI & Rendering
   │   │   └── Other categories
   │   └── Auto-persist: /sdcard/benchmark-results/benchmark-baseline.json ✅
   │
   ├── App stays installed (no reinstall) ✅
   ├── Run connectedHeavyDebugAndroidTest
   │   ├── App launches with heavy configuration
   │   ├── BenchmarkSDK collects metrics
   │   └── Auto-persist: /sdcard/benchmark-results/benchmark-heavy.json ✅
   │
   └── Tests complete - data in device cache (persists across reinstalls) ✅

3. REPORT PHASE - Phase 3 Dynamic Generation ✅
   ├── Pull data: ./gradlew pullBenchmarkData
   │   ├── adb pull /sdcard/benchmark-results/benchmark-baseline.json
   │   └── adb pull /sdcard/benchmark-results/benchmark-heavy.json
   │
   ├── Generate: ./gradlew generateReport
   │   ├── Python script: generate_report.py (DYNAMIC with Startup Thresholds)
   │   │   ├── Load metric-schema.json (categories, metadata, thresholds)
   │   │   ├── Merge custom metrics/categories from JSONs
   │   │   ├── Categorize metrics using schema metadata
   │   │   ├── Compare baseline vs heavy
   │   │   ├── Calculate change percentages & severity
   │   │   │   ├── 🚀 Startup: Cold >10% → Critical, Warm/Hot >5% → Critical
   │   │   │   └── Others: >50% → Critical, >20% → Warning
   │   │   └── Generate dynamic category structure
   │   └── Output: benchmark-results/report.html (DYNAMIC)
   │       ├── Automatically detects all categories
   │       ├── 🚀 Startup category appears FIRST (order=1)
   │       ├── Renders with icons, display names, ordering
   │       └── No hardcoded categories - fully extensible!
   │
   └── Auto-open browser ✅

4. RESULT
   └── Beautiful HTML report with startup metrics at the top
```

---

## 📊 Core Metrics Collected

### 🚀 **Startup Performance** (MANDATORY - Appears First)

Phase 3 adds comprehensive startup metrics - the most critical indicator of app quality:

| Metric | Baseline | Heavy | Description |
|--------|----------|-------|-------------|
| **Cold Startup (Initial Display)** | 150ms | 450ms | Time to first frame on cold start |
| **Cold Startup (Full Display)** | 280ms | 650ms | Time to fully drawn on cold start |
| **Cold Startup (Total)** | 430ms | 1100ms | Total cold startup time |
| **Warm Startup (Initial Display)** | 80ms | 250ms | Time to first frame on warm start |
| **Warm Startup (Full Display)** | 150ms | 350ms | Time to fully drawn on warm start |
| **Warm Startup (Total)** | 230ms | 600ms | Total warm startup time |
| **Hot Startup (Initial Display)** | 50ms | 150ms | Time to first frame on hot start |
| **Hot Startup (Full Display)** | 80ms | 200ms | Time to fully drawn on hot start |
| **Hot Startup (Total)** | 130ms | 350ms | Total hot startup time |
| **Notification Launch** | 200ms | 550ms | Startup from notification tap |
| **Library Initialization** | 100ms | 500ms | Time to initialize SDKs |
| **Splash Screen Duration** | 200ms | 500ms | Splash screen display time |
| **First Paint Time** | 280ms | 650ms | Time to first visual paint |
| **Time to Interactive** | 450ms | 1200ms | Time until user can interact |
| **Process Start** | 80ms | 180ms | Process initialization (API 24+) |
| **Background Tasks** | 5 | 12 | Tasks running during startup |
| **Startup Memory Footprint** | 45MB | 85MB | Memory consumed during startup |
| **DEX Classes Loaded** | 1200 | 3500 | Classes loaded from DEX |
| **Disk Reads** | 850KB | 2400KB | Data read from disk |

**Severity Thresholds:**
- **Cold Startup:** >10% regression → 🔴 Critical, 5-10% → 🟡 Warning
- **Warm/Hot Startup:** >5% regression → 🔴 Critical, 2-5% → 🟡 Warning

### ⚡ **CPU & Performance**

### File Flow (Phase 2+)

```
Device Cache (Persistent)         Local Machine
─────────────────────────        ─────────────────
/sdcard/benchmark-results/        benchmark-results/
├── benchmark-baseline.json ──┐   └── benchmarks/
└── benchmark-heavy.json ─────┤       ├── benchmark-baseline.json
                              │       ├── benchmark-heavy.json
✅ Persists across reinstalls │       └── report.html
                              └───────┘
```

**Key Changes:**
- ✅ Device cache: `/sdcard/benchmark-results/` (not app-specific)
- ✅ Data survives app reinstalls
- ✅ Same package prevents reinstalls between tests
- ✅ Auto-persist in tests (@After method)
- ✅ Browser auto-opens with report

---

## 🚀 Running Benchmarks

### Method 1: Complete Suite (Recommended)

Run everything with clear, sequential commands:

```bash
# Step 1: Run tests (auto-persists to device cache)
./gradlew runBenchmarkTests

# Step 2: Pull data from device cache
./gradlew pullBenchmarkData

# Step 3: Generate report and open browser
./gradlew generateReport
```

### Method 2: All-in-One

```bash
./gradlew benchmarkComplete
```

This runs all three steps automatically.

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

