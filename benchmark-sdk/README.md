# Benchmark SDK

Lightweight Android benchmarking SDK supporting:

- Startup time measurement (process start -> first UI ready)
- Memory & CPU usage snapshot
- Custom developer-provided metrics with metadata (NEW!)
- JSON + HTML report generation with previous-run comparison
- Gradle task integration (`./gradlew runBenchmarks`)
- CI friendly (outputs saved under app external files directory on device/emulator)
- **NEW: Unified metric schema with custom definitions**

## ✨ What's New - Phase 1

The SDK now includes a **unified metric schema system** that allows:

✅ Custom metric definitions with full metadata  
✅ Custom category creation for organizing metrics  
✅ Performance thresholds for automatic severity assessment  
✅ Schema versioning for compatibility tracking  
✅ No SDK modification required for new metrics  

**Quick example:**
```kotlin
BenchmarkSDK.defineMetric(
    name = "databaseQueryMs",
    category = "database",
    displayName = "Database Query Time",
    unit = "ms",
    thresholds = MetricThresholds(good = 50, warning = 150, critical = 300)
)
```

## 🚀 Quick Start

### 1. Add SDK to Your Project

In your `settings.gradle.kts`:
```kotlin
include(":benchmark-sdk")
```

In your app's `build.gradle.kts`:
```kotlin
dependencies {
    implementation(project(":benchmark-sdk"))
}
```

### 2. Initialize in Your App

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Optional: Define custom metrics
        BenchmarkSDK.defineMetric(
            name = "customMetric",
            category = "custom",
            displayName = "My Custom Metric",
            unit = "ms"
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Mark app ready (for startup time)
        BenchmarkSDK.onAppReady()
        
        // Set scenario label
        BenchmarkSDK.setScenario(BuildConfig.BENCH_SCENARIO)
        
        // Collect and persist metrics
        BenchmarkSDK.collectScenarioAndPersist(this)
    }
}
```

### 3. Run Benchmarks

```bash
./gradlew runBenchmarks
```

This will:
1. Run instrumented tests for all benchmark scenarios
2. Collect metrics from device
3. Generate HTML comparison report

## 📖 Documentation

- **[Schema Guide](SCHEMA_GUIDE.md)** - Complete metric schema reference
- **[Quick Start](../QUICKSTART_CUSTOM_METRICS.md)** - Add custom metrics in 5 minutes
- **[Workflow Guide](../BENCHMARK_WORKFLOW.md)** - How benchmarking works
- **[API Docs](../API_DOCUMENTATION.md)** - Complete API reference

## 🎯 Features

### Automatic Metrics
- **Startup time** - Process start to first UI frame
- **Memory usage** - PSS, heap used, heap max, usage percentage
- **CPU time** - Process CPU time
- **Network latency** - Real network request benchmarking
- **APK size** - Build artifact size
- **Build config** - Version info and metadata

### Custom Metrics
- Define your own performance metrics
- Add metadata (units, thresholds, descriptions)
- Create custom categories
- Automatic inclusion in reports

### Report Generation
- HTML comparison reports (baseline vs heavy)
- JSON output with schema versioning
- Severity indicators (good/warning/critical)
- Change percentages with visual highlights

## Startup Time Detection

Two modes:
1. Automatic (legacy XML / any Activity): The SDK registers an ActivityLifecycleCallbacks and marks startup complete on the first resumed Activity.
2. Manual (recommended for Jetpack Compose or precise control): Call:
```kotlin
BenchmarkSDK.onAppReady()
```
after your first meaningful frame is drawn.

## Usage

1. Add module dependency (already added in sample app):
```kotlin
implementation(project(":benchmark-sdk"))
```
2. The SDK auto-captures process start via a ContentProvider.
3. (Optional for Compose) Invoke `BenchmarkSDK.onAppReady()` when UI is ready.
4. Register custom metrics (optional):
```kotlin
BenchmarkSDK.registerMetricProvider("activeUsers") { 42 }
```
5. Collect & persist metrics (e.g. instrumentation test or debug button):
```kotlin
BenchmarkSDK.collectAndPersist(context)
```
6. Run instrumentation benchmarks via Gradle:
```bash
./gradlew runBenchmarks
```
7. Pull results from device/emulator:
```bash
adb shell ls /sdcard/Android/data/io.app.benchmark/files/benchmarks
adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks ./benchmark-results
```

Reports include `benchmark-<timestamp>.json` and `benchmark-latest.html` with delta vs previous run.

## Manual Scenario Metrics
Use `timeScenario` to measure a code block duration and auto record:
```kotlin
val result = BenchmarkSDK.timeScenario("jsonParsingMs") { parseHugeJson() }
```
Or record arbitrary numeric values:
```kotlin
BenchmarkSDK.recordMetric("cacheWarmupMs", 123)
BenchmarkSDK.recordMetric("itemsProcessed", 4521)
```
These appear alongside automatic runtime metrics in reports.

## Future SaaS Upload
A `ResultsUploader` interface is provided for future network upload integration.

## Limitations
- Current CPU metric is cumulative process CPU time; more granular per-thread metrics can be added later.
- Startup time ends when automatic first resume happens or manual `onAppReady()` is invoked.
- Does not yet include battery/network metrics.

## Extending
Add additional metric collectors inside `BenchmarkSDK.collectRuntimeMetrics` or via custom providers.
