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

### 1. Add SDK Dependency
```kotlin
dependencies {
    implementation(project(":benchmark-sdk"))
}
```

### 2. Call onAppReady() at Startup
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BenchmarkSDK.onAppReady()
    }
}
```

### 3. Define Custom Metrics (Optional)
```kotlin
BenchmarkSDK.defineCategory(
    id = "database",
    displayName = "Database Operations",
    icon = "💾"
)

BenchmarkSDK.defineMetric(
    name = "databaseQueryMs",
    category = "database",
    displayName = "Query Time",
    unit = "ms"
)
```

### 4. Record Metrics
```kotlin
BenchmarkSDK.recordMetric("databaseQueryMs", 75)
```

### 5. Run Tests and Generate Report
```bash
./gradlew runBenchmarkTests      # Runs tests, auto-persists
./gradlew pullBenchmarkData      # Pulls from device cache
./gradlew generateReport         # Generates HTML, opens browser
```

**Phase 2 Benefit:** Your custom category automatically appears in the HTML report with icon and styling!
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

---

## Report Structure (Phase 2 - Dynamic)

The generated `report.json` now uses a **dynamic, schema-driven structure**:

```json
{
  "schema_version": "1.0",
  "latest_type": "heavy",
  "latest_time": "2025-12-04 10:30:00",
  
  "collected_metrics": ["cpuHeavyLoopMs", "memoryUsedBytes", ...],
  "missing_metrics": ["startupTimeMs", ...],
  
  // Dynamic categories (automatically detected and organized)
  "cpu": [
    {
      "metric": "cpuHeavyLoopMs",
      "baseline": 1,
      "heavy": 5,
      "change": 400.0,
      "highlight_leak": false,
      "highlight_error": false,
      "severity": "Needs Attention"
    },
    {
      "metric": "processCpuTimeMs",
      "baseline": 119,
      "heavy": 148,
      "change": 24.37,
      "severity": "Warning"
    }
  ],
  
  "memory": [...],
  "network": [...],
  "build": [...],
  "database": [...],  // Custom categories automatically included!
  
  // Category metadata for display (icons, names, ordering)
  "category_metadata": {
    "cpu": {
      "displayName": "CPU & Performance",
      "icon": "⚡",
      "description": "CPU usage and performance metrics",
      "order": 1
    },
    "memory": {
      "displayName": "Memory & Heap",
      "icon": "🧠",
      "description": "Memory allocation and usage",
      "order": 2
    },
    "database": {
      "displayName": "Database Operations",
      "icon": "💾",
      "description": "Custom database metrics",
      "order": 6
    }
  },
  
  // Overall performance summary
  "overall_performance": {
    "average_change": 145.67,
    "status": "Degraded",
    "summary": "Overall performance change: 145.67% (Degraded)"
  },
  
  // Custom metadata (if provided)
  "metadata": {
    "custom_metrics": {...},
    "custom_categories": {...}
  }
}
```

### Key Features (Phase 2)

✅ **No Hardcoded Categories** - Any category in the data will be rendered  
✅ **Automatic Categorization** - Metrics organized by schema metadata  
✅ **Display Metadata** - Icons, display names, ordering from schema  
✅ **Custom Categories** - Automatically detected and displayed  
✅ **Backward Compatible** - Legacy fields (`cpu_os`, `memory`, `network`, `other`) still present

### HTML Report Features

The `report.html` automatically:
- Detects all categories from JSON
- Sorts by `order` field from metadata
- Renders tables with icons and proper titles
- Applies category-specific highlighting (e.g., errors in network, leaks in memory)
- Shows severity badges (Needs Attention, Warning, Minor, Normal)
- Formats large numbers with commas
- Handles objects, booleans, and null values gracefully
- Responsive design for mobile devices

**No code changes needed to add new categories!**

---
