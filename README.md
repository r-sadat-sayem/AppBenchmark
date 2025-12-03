# AppBenchmark SDK

This project contains a sample Android app and a lightweight Benchmark SDK library module providing:

- Startup time measurement (process start -> first UI ready)
- Memory & CPU usage metrics
- **NEW: Unified metric schema with metadata** (Phase 1 ✅)
- **NEW: Custom metric/category definition API**
- **NEW: Dynamic report generation** (Phase 2 ✅)
- **NEW: Persistent device cache storage** (Phase 2+ ✅)
- **NEW: 42 comprehensive test scenarios** (Phase 2+ ✅)
- JSON + HTML report generation with automatic diff vs previous run
- Simple Gradle tasks for CI/CD
- **Real network benchmarking** using aviationweather.gov API

## 🆕 What's New

### Phase 2 + Enhancements ✅ **COMPLETED**

The report system is now **fully dynamic** with enhanced testing and persistent storage:

✅ **No hardcoded categories** - Reports automatically adapt to any metrics  
✅ **Schema-driven display** - Icons, display names, ordering from metadata  
✅ **Automatic categorization** - Metrics organized intelligently  
✅ **Persistent device cache** - Data survives app reinstalls  
✅ **No app reinstalls** - Same package for all test variants  
✅ **42 test scenarios** - Comprehensive coverage (CPU, Memory, Network, Storage, Database, UI, Startup)  
✅ **Auto-open browser** - Report opens automatically (cross-platform)  
✅ **Zero code changes** - Add new categories without touching report code  

**Example:** Add a database category - it appears automatically with proper icon and styling!

📖 **[Phase 2 Summary](PHASE2_SUMMARY.md)** - Complete implementation details  
📖 **[Phase 2 Complete](PHASE2_COMPLETE.md)** - All enhancements documented

---

### Phase 1: Unified Metric Schema ✅ **COMPLETED**

The SDK supports a **standardized metric schema** that allows you to:

✅ Define custom metrics with metadata (units, thresholds, descriptions)  
✅ Create custom categories for organizing metrics  
✅ Set performance thresholds for automatic severity assessment  
✅ All without modifying SDK code!

**Quick Example:**
```kotlin
BenchmarkSDK.defineMetric(
    name = "databaseQueryMs",
    category = "database",
    displayName = "Database Query Time",
    unit = "ms",
    thresholds = MetricThresholds(good = 50, warning = 150, critical = 300)
)
BenchmarkSDK.recordMetric("databaseQueryMs", 75)
```

📖 **Documentation:**
- [Quick Start Guide](QUICKSTART_CUSTOM_METRICS.md) - Add custom metrics in 5 minutes
- [Workflow Guide](BENCHMARK_WORKFLOW.md) - Complete benchmarking workflow & troubleshooting
- [Schema Guide](benchmark-sdk/SCHEMA_GUIDE.md) - Metric schema reference
- [API Documentation](API_DOCUMENTATION.md) - Complete SDK API
- [Phase 1 Summary](PHASE1_SUMMARY.md) - Phase 1 implementation details
- [Phase 2 Summary](PHASE2_SUMMARY.md) - Phase 2 dynamic report system

## Modules
- `app`: Sample application using Jetpack Compose
- `benchmark-sdk`: Reusable benchmarking SDK (can be published as a dependency)

## 🚀 Quick Start: Running Benchmarks

### Single Command Workflow

Run the complete benchmark suite with one command:

```bash
./gradlew runBenchmarkTests
```

This will automatically:
1. ✅ Run instrumented tests for `baseline` and `heavy` scenarios
2. ✅ Auto-persist metrics to device cache (`/sdcard/benchmark-results/`)
3. ✅ Data persists across app reinstalls
4. ✅ App stays installed (no reinstalls between tests)

Then pull data and generate report:

```bash
./gradlew pullBenchmarkData   # Pulls from device cache
./gradlew generateReport       # Generates HTML and opens browser
```

**Or use complete workflow:**
```bash
./gradlew benchmarkComplete    # Does everything above
```

### What You Get

- **42 test scenarios** across 10 categories
- **Persistent storage** in `/sdcard/benchmark-results/`
- **Auto-open browser** with beautiful HTML report
- **Dynamic categories** with icons and styling
- **Device cache** survives app reinstalls

### Advanced Usage

**Run specific flavors:**
```bash
./gradlew runBenchmarks -PbenchFlavors=baseline
./gradlew runBenchmarks -PbenchFlavors=baseline,heavy
```

**Run specific variants:**
```bash
./gradlew runBenchmarks -PbenchVariants=baselineDebug
```

**Generate report only (skip tests):**
```bash
./gradlew generateBenchmarkReport
```

**Manual file extraction:**
```bash
adb shell ls /sdcard/Android/data/io.app.benchmark/files/benchmarks
adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks ./benchmark-results
```

## 🎯 Product Flavors (Benchmark Scenarios)

The project uses **Android product flavors** to define benchmark scenarios:

| Flavor | Purpose | App ID | Configuration |
|--------|---------|--------|---------------|
| **baseline** | Standard/minimal configuration | `io.app.benchmark.baseline` | Normal features, light workload |
| **heavy** | Stress test/maximum load | `io.app.benchmark.heavy` | All features enabled, heavy workload |

### Why Product Flavors?

✅ **Single codebase** - Share code, different configurations  
✅ **Easy A/B testing** - Direct performance comparison  
✅ **Gradle automation** - Automatically handles installation and testing  
✅ **Isolated testing** - Each flavor runs independently  

### Customizing Scenarios

Edit `app/build.gradle.kts` to customize each flavor:

```kotlin
productFlavors {
    create("baseline") {
        dimension = "scenario"
        buildConfigField("Int", "MAX_CACHE_SIZE_MB", "50")
        buildConfigField("Boolean", "ENABLE_ANALYTICS", "false")
        buildConfigField("Int", "NETWORK_THREADS", "2")
    }
    
    create("heavy") {
        dimension = "scenario"
        buildConfigField("Int", "MAX_CACHE_SIZE_MB", "500")
        buildConfigField("Boolean", "ENABLE_ANALYTICS", "true")
        buildConfigField("Int", "NETWORK_THREADS", "20")
    }
}
```

Then use in your code:
```kotlin
val cacheSize = BuildConfig.MAX_CACHE_SIZE_MB
val analyticsEnabled = BuildConfig.ENABLE_ANALYTICS
```

## 🧪 Example Benchmark Test

The sample app demonstrates real-world benchmarking:

**Baseline scenario:**
- Measures actual network latency to `aviationweather.gov`
- Normal memory allocation patterns
- Standard CPU usage

**Heavy scenario:**
- Same network request + 3000ms artificial delay
- Large memory allocations
- CPU-intensive operations

Both scenarios auto-persist metrics to device storage on app launch.

## 📊 Report Generation

After running tests, the Python script generates a comparison report:

**Input:** `benchmark-baseline.json` + `benchmark-heavy.json`  
**Output:** `benchmark-results/benchmarks/report.html`

The report includes:
- CPU & Performance metrics
- Memory & Heap usage
- Network latency and response times
- Custom metrics (if defined)
- Change percentage with severity indicators

## 🔧 Adding to Another Project

1. Copy the `benchmark-sdk` module to your project
2. Add to `settings.gradle.kts`:
```kotlin
include(":benchmark-sdk")
```
3. Add dependency in your app's `build.gradle.kts`:
```kotlin
implementation(project(":benchmark-sdk"))
```
4. Define custom metrics (optional):
```kotlin
BenchmarkSDK.defineMetric(
    name = "myCustomMetric",
    category = "custom",
    displayName = "My Custom Metric",
    unit = "ms",
    thresholds = MetricThresholds(good = 100, warning = 500, critical = 1000)
)
```
5. Collect metrics in tests or debug code:
```kotlin
BenchmarkSDK.collectScenarioAndPersist(context)
```

## 🚀 CI/CD Integration

### GitHub Actions

```yaml
name: Benchmark Tests

on: [push, pull_request]

jobs:
  benchmark:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Setup Android SDK
        uses: android-actions/setup-android@v3
      
      - name: Start Android Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 34
          arch: x86_64
          script: |
            ./gradlew runBenchmarks
      
      - name: Upload Benchmark Report
        uses: actions/upload-artifact@v4
        with:
          name: benchmark-report
          path: benchmark-results/benchmarks/
          retention-days: 30
      
      - name: Comment PR with Results
        if: github.event_name == 'pull_request'
        uses: actions/github-script@v7
        with:
          script: |
            const fs = require('fs');
            const report = fs.readFileSync('benchmark-results/benchmarks/report.json', 'utf8');
            const data = JSON.parse(report);
            const summary = data.overall_performance?.summary || 'No summary available';
            github.rest.issues.createComment({
              issue_number: context.issue.number,
              owner: context.repo.owner,
              repo: context.repo.repo,
              body: `## 📊 Benchmark Results\n\n${summary}\n\n[View Full Report](${process.env.GITHUB_SERVER_URL}/${context.repo.owner}/${context.repo.repo}/actions/runs/${context.runId})`
            });
```

### Jenkins

```groovy
pipeline {
    agent any
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        
        stage('Run Benchmarks') {
            steps {
                sh './gradlew runBenchmarks'
            }
        }
        
        stage('Publish Report') {
            steps {
                publishHTML([
                    reportDir: 'benchmark-results/benchmarks',
                    reportFiles: 'report.html',
                    reportName: 'Benchmark Report',
                    keepAll: true,
                    alwaysLinkToLastBuild: true
                ])
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'benchmark-results/**/*.json', allowEmptyArchive: true
        }
    }
}
```

### GitLab CI

```yaml
benchmark:
  stage: test
  image: cirrusci/flutter:latest
  
  before_script:
    - apt-get update && apt-get install -y python3
  
  script:
    - ./gradlew runBenchmarks
  
  artifacts:
    paths:
      - benchmark-results/
    reports:
      junit: app/build/outputs/androidTest-results/connected/*.xml
    expire_in: 30 days
  
  only:
    - merge_requests
    - main
```

## 📈 Viewing Results

After running benchmarks, open the generated report:

```bash
open benchmark-results/benchmarks/report.html
# or
xdg-open benchmark-results/benchmarks/report.html  # Linux
```

The report shows:
- ⚡ CPU & Performance comparisons
- 🧠 Memory usage trends
- 🌐 Network latency analysis
- 📊 Change percentages with severity indicators
- 🎯 Custom metrics (if defined)
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
| buildConfig (static)         | Build configuration values (version, id, etc.)   |

> **Note:** All metrics above are now collected and reported automatically by the SDK, including `startupTimeMs`, `apkSizeBytes`, and `buildConfig`.

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

## TODO: AI/LLM Integration for Report Analysis
- Integrate an LLM (Large Language Model) to analyze benchmark metrics and generate deeper insights, recommendations, and summaries.
- Prospects: Automated performance diagnosis, anomaly detection, and actionable suggestions for developers. Can be integrated via API (OpenAI, Azure, Google Vertex AI, etc.) and used in CI or reporting scripts.
