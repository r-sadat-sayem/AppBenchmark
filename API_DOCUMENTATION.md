# BenchmarkSDK API Documentation

## Overview
`BenchmarkSDK` is a plug-and-play library for collecting real runtime performance metrics in Android apps (API 28+). It is designed for easy integration and automatic reporting.

**Schema Version:** 1.0 (supports unified metric schema with metadata)

## Public API

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
    
    /** NEW: Define a custom metric with metadata for proper reporting and analysis. */
    fun defineMetric(
        name: String,
        category: String,
        displayName: String,
        unit: String,
        lowerIsBetter: Boolean = true,
        description: String? = null,
        thresholds: MetricThresholds? = null
    )
    
    /** NEW: Define a custom category for organizing metrics. */
    fun defineCategory(
        id: String,
        displayName: String,
        icon: String? = null,
        description: String? = null,
        order: Int = 999
    )
}

/** Threshold values for metric severity assessment. */
data class MetricThresholds(
    val good: Number? = null,
    val warning: Number? = null,
    val critical: Number? = null
)
```

## Usage Example

```kotlin
// Mark app ready after first frame
BenchmarkSDK.onAppReady()

// Tag scenario
BenchmarkSDK.setScenario("baseline")

// NEW: Define custom metrics with metadata (Phase 1)
BenchmarkSDK.defineCategory(
    id = "database",
    displayName = "Database Operations",
    icon = "💾",
    description = "Database query and transaction metrics"
)

BenchmarkSDK.defineMetric(
    name = "databaseQueryMs",
    category = "database",
    displayName = "Database Query Time",
    unit = "ms",
    lowerIsBetter = true,
    description = "Average time for SELECT queries",
    thresholds = MetricThresholds(good = 50, warning = 150, critical = 300)
)

// Record the custom metric
BenchmarkSDK.recordMetric("databaseQueryMs", 75)

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

## Integration Steps
1. Add the SDK as a dependency.
2. Call `onAppReady()` at app startup.
3. Use `setScenario()` to label runs.
4. Use `collectAndPersist()` or `collectScenarioAndPersist()` to save metrics.
5. Use `getActualRuntimeMetrics()` for direct access to metrics.
6. Use `realNetworkRequest()` for network benchmarking.

## Available Metrics (Collected & Reported)
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

