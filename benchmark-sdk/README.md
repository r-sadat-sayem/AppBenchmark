# Benchmark SDK

Lightweight Android benchmarking SDK supporting:

- Startup time measurement (process start -> first UI ready)
- Memory & CPU usage snapshot
- Custom developer-provided metrics
- JSON + HTML report generation with previous-run comparison
- Gradle task integration (`./gradlew runBenchmarks`)
- CI friendly (outputs saved under app external files directory on device/emulator)

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
