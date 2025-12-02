# AppBenchmark SDK

This project contains a sample Android app and a lightweight Benchmark SDK library module providing:

- Startup time measurement (process start -> first UI ready)
- Memory & CPU usage metrics
- Custom metric registration API
- JSON + HTML report generation with automatic diff vs previous run
- Simple Gradle task `runBenchmarks` for CI
- **Real network benchmarking** using aviationweather.gov API (see below)

## Modules
- `app`: Sample application using Jetpack Compose
- `benchmark-sdk`: Reusable benchmarking SDK (can be published as a dependency)

## Quick Start (Local)
```bash
./gradlew runBenchmarks
```
Pull results from device/emulator:
```bash
adb shell ls /sdcard/Android/data/io.app.benchmark/files/benchmarks
adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks ./benchmark-results
```

## Real Network Benchmark Example
The sample app demonstrates benchmarking a real network request:

- **Baseline scenario**: Measures actual latency for a GET request to `https://aviationweather.gov/api/data/metar?ids=KMCI&format=json`.
- **Heavy scenario**: Measures the same request, then adds a 3000ms artificial delay to simulate stress.

To run both scenarios, set `BENCH_SCENARIO` in your build config to `baseline` or `heavy`.

## Adding to Another Project
1. Publish `benchmark-sdk` (MavenLocal or remote) or copy module.
2. Add dependency:
```kotlin
implementation("io.app.benchmark:benchmark-sdk:<version>")
```
3. For Compose, call `BenchmarkSDK.onAppReady()` after first frame; for XML you can rely on automatic detection.
4. Trigger collection (instrumentation test or manual button):
```kotlin
BenchmarkSDK.collectAndPersist(context)
```

## CI Integration Examples
GitHub Actions step (assuming emulator already started):
```yaml
- name: Run Benchmarks
  run: ./gradlew runBenchmarks
- name: Pull Results
  run: |
    adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks benchmark-results
- uses: actions/upload-artifact@v4
  with:
    name: benchmark-results
    path: benchmark-results
```

Jenkins Pipeline snippet:
```groovy
stage('Benchmarks') {
  sh './gradlew runBenchmarks'
  sh 'adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks benchmark-results'
  archiveArtifacts artifacts: 'benchmark-results/**', fingerprint: true
}
```

## Future Enhancements
- Network & battery metrics
- Frame rendering / dropped frame stats
- SaaS uploader implementation
- More granular CPU profiling

## License
You can attach a suitable OSS license (e.g., Apache 2.0) here.

## Benchmark Reporting & Performance Analysis

Performance is analyzed and reported in three main segments:

1. **Inspecting Performance**
   - Collects and displays all available metrics from the app and system.
   - Includes both dynamic (runtime) and static (configuration/build) scores.
   - Example metrics: startup time, memory usage, CPU time, network latency, etc.

2. **Improving Performance**
   - Highlights areas for optimization based on collected metrics.
   - Provides actionable suggestions (e.g., reduce memory allocations, optimize network calls).

3. **Monitoring Performance**
   - Tracks performance over time and across scenarios (baseline, heavy, custom).
   - Enables continuous monitoring via CI integration and historical reports.

### Benchmark Scores
- **Dynamic Score:** Calculated from real runtime metrics (e.g., actual startup time, memory, CPU, network latency).
- **Static Score:** Derived from static app properties (e.g., build config, manifest settings, APK size).
- Both scores are reported in the HTML and JSON reports for comprehensive analysis.

### Available Metrics (Collected & Reported)
The Benchmark SDK collects and reports the following metrics:

| Metric Name                  | Description                                      |
|-----------------------------|--------------------------------------------------|
| startupTimeMs                | App startup duration (ms)                        |
| memoryPssKb                  | Proportional Set Size (memory, KB)               |
| memoryUsedBytes              | Used heap memory (bytes)                         |
| memoryHeapMaxBytes           | Max heap memory (bytes)                          |
| memoryUsagePercent           | Used memory as % of max heap                     |
| processCpuTimeMs             | CPU time used by app process (ms)                |
| network_*_requestMs          | Network request latency (ms)                     |
| network_*_responseCode       | HTTP response code                               |
| network_*_responseLength     | Response payload length (bytes)                  |
| network_*_error              | Network error indicator                          |
| measuredNetworkLatencyMs     | Actual measured network latency (ms)             |
| cpuHeavyLoopMs               | Time for heavy CPU loop (ms)                     |
| memoryAllocationMs           | Time for memory allocation scenario (ms)         |
| simulatedRequestMs           | Simulated network request time (ms)              |
| scenarioLabel                | Scenario label (baseline, heavy, custom)         |
| apkSizeBytes (static)        | APK file size (bytes)                            |
| buildConfig (static)         | Build configuration values                       |

> **Note:** Metrics prefixed with `network_*` are collected for each network scenario (e.g., `network_aviation`, `network_google`).

## BenchmarkSDK API Documentation

See the full API documentation here: [API_DOCUMENTATION.md](./API_DOCUMENTATION.md)

**Summary:**
- Plug-and-play benchmarking for Android apps (API 28+)
- Collects startup, memory, CPU, network, and custom metrics
- Easy integration and reporting
- See all available metrics, usage examples, and integration steps in the API documentation

### Public API

```kotlin
object BenchmarkSDK {
    /** Mark the app as ready (first UI frame). Records startup duration. */
    fun onAppReady()

    /** Record a custom numeric metric. */
    fun recordMetric(name: String, value: Number)

    /** Set a scenario label for this session (e.g., "baseline", "heavy"). */
    fun setScenario(label: String)

    /** Time a scenario and record its duration (ms) under the provided metric name. */
    inline fun <T> timeScenario(metricName: String, block: () -> T): T

    /** Collect metrics and persist JSON report. Returns the metrics map. */
    fun collectAndPersist(context: Context): Map<String, Any>

    /** Collect metrics and persist JSON report with scenario in filename. */
    fun collectScenarioAndPersist(context: Context): File

    /** Get actual runtime metrics from the app process and OS. */
    fun getActualRuntimeMetrics(): Map<String, Any>

    /** Perform a real network request and record metrics. */
    fun realNetworkRequest(
        url: String,
        metricPrefix: String = "network",
        method: String = "GET",
        body: String? = null
    ): NetworkBenchmarkResult
}
```

### Usage Example

```kotlin
// Mark app ready after first frame
BenchmarkSDK.onAppReady()

// Tag scenario
BenchmarkSDK.setScenario("baseline")

// Time a custom scenario
BenchmarkSDK.timeScenario("customScenarioMs") {
    // ...code to benchmark...
}

// Collect and persist metrics
BenchmarkSDK.collectAndPersist(context)

// Get actual runtime metrics
val metrics = BenchmarkSDK.getActualRuntimeMetrics()

// Benchmark a real network request
val result = BenchmarkSDK.realNetworkRequest(
    url = "https://aviationweather.gov/api/data/metar?ids=KMCI&format=json",
    metricPrefix = "network_aviation"
)
```

### Integration Steps
1. Add the SDK as a dependency.
2. Call `onAppReady()` at app startup.
3. Use `setScenario()` to label runs.
4. Use `collectAndPersist()` or `collectScenarioAndPersist()` to save metrics.
5. Use `getActualRuntimeMetrics()` for direct access to metrics.
6. Use `realNetworkRequest()` for network benchmarking.

---
