# Quick Start: Adding Custom Metrics

This guide shows you how to add custom performance metrics to your app using the BenchmarkSDK's Phase 1 & 2 features.

**New in Phase 2:**
- ✅ Reports automatically render your custom categories (no code changes needed)
- ✅ Persistent device cache storage (data survives app reinstalls)
- ✅ Auto-open browser with beautiful HTML reports

## 5-Minute Integration

### Step 1: Define Your Metric Category (Optional)

If your metric doesn't fit existing categories (CPU, Memory, Network, etc.), create a custom one:

```kotlin
import io.app.benchmark.sdk.BenchmarkSDK

BenchmarkSDK.defineCategory(
    id = "database",                           // Unique ID
    displayName = "Database & Storage",        // Name shown in reports
    icon = "💾",                               // Optional emoji/icon
    description = "Database operations",       // Optional description
    order = 6                                  // Display order (lower = first)
)
```

**Phase 2 Benefit:** This category will automatically appear in the HTML report with your icon and display name!

### Step 2: Define Your Metric

Tell the SDK about your metric's properties:

```kotlin
import io.app.benchmark.sdk.MetricThresholds

BenchmarkSDK.defineMetric(
    name = "databaseQueryMs",                  // Unique metric name
    category = "database",                     // Category ID from Step 1
    displayName = "Database Query Time",       // Human-readable name
    unit = "ms",                               // Unit: ms, bytes, %, count, etc.
    lowerIsBetter = true,                      // true = lower is better
    description = "Average query duration",    // Optional description
    thresholds = MetricThresholds(             // Optional performance thresholds
        good = 50,                             // Good: ≤ 50ms
        warning = 150,                         // Warning: 50-150ms
        critical = 300                         // Critical: > 150ms
    )
)
```

### Step 3: Record Metric Values

When your code runs, record the actual values:

```kotlin
// Option A: Record directly
BenchmarkSDK.recordMetric("databaseQueryMs", 75)

// Option B: Time a code block automatically
BenchmarkSDK.timeScenario("databaseQueryMs") {
    // Your database query here
    database.query("SELECT * FROM users")
}
```

### Step 4: Collect and Persist

The SDK automatically includes your custom metrics when you collect:

```kotlin
// In your benchmark test or debug button
BenchmarkSDK.collectScenarioAndPersist(context)
```

## Complete Example

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup custom metrics once at startup
        setupBenchmarkMetrics()
        
        // Your app code...
    }
    
    private fun setupBenchmarkMetrics() {
        // 1. Define category
        BenchmarkSDK.defineCategory(
            id = "database",
            displayName = "Database Operations",
            icon = "💾",
            order = 6
        )
        
        // 2. Define metrics
        BenchmarkSDK.defineMetric(
            name = "databaseQueryMs",
            category = "database",
            displayName = "Query Time",
            unit = "ms",
            lowerIsBetter = true,
            thresholds = MetricThresholds(good = 50, warning = 150, critical = 300)
        )
        
        BenchmarkSDK.defineMetric(
            name = "cacheHitRate",
            category = "database",
            displayName = "Cache Hit Rate",
            unit = "%",
            lowerIsBetter = false,  // Higher is better!
            thresholds = MetricThresholds(good = 80, warning = 50, critical = 20)
        )
    }
    
    fun performDatabaseOperations() {
        // 3. Record metrics during operations
        BenchmarkSDK.timeScenario("databaseQueryMs") {
            database.query("SELECT * FROM users WHERE active = 1")
        }
        
        val cacheHits = calculateCacheHitRate()
        BenchmarkSDK.recordMetric("cacheHitRate", cacheHits)
    }
}
```

## Metric Property Reference

### `lowerIsBetter`

Choose based on what "good" means for your metric:

| Metric Type | lowerIsBetter | Reason |
|-------------|---------------|--------|
| Latency/Duration | `true` | Lower time = faster |
| Memory Usage | `true` | Less memory = better |
| Error Count | `true` | Fewer errors = better |
| Cache Hit Rate | `false` | More hits = better |
| Throughput | `false` | More items/sec = better |
| Success Rate | `false` | Higher % = better |

### Common Units

| Unit | Use For | Example |
|------|---------|---------|
| `ms` | Milliseconds | Query time: 75ms |
| `bytes` | Raw byte count | File size: 1024 bytes |
| `KB` / `MB` | Kilobytes/Megabytes | Memory: 45678 KB |
| `%` | Percentage | CPU usage: 45% |
| `count` | Countable items | Error count: 3 |
| `boolean` | True/False | Has error: 1 (true) |
| `code` | Status codes | HTTP: 200 |

### Threshold Guidelines

Set thresholds based on **user experience impact**:

```kotlin
// Example: API Response Time
thresholds = MetricThresholds(
    good = 200,      // Under 200ms: excellent UX
    warning = 1000,  // 200-1000ms: acceptable but noticeable
    critical = 3000  // Over 1000ms: poor UX, needs attention
)

// Example: Cache Hit Rate (inverted logic)
thresholds = MetricThresholds(
    good = 80,       // Over 80%: excellent caching
    warning = 50,    // 50-80%: room for improvement
    critical = 20    // Under 50%: cache not effective
)
```

## Advanced: Wildcard Patterns

For dynamic metrics like multiple API endpoints:

```kotlin
// Single definition matches multiple metrics
BenchmarkSDK.defineMetric(
    name = "api_*_requestMs",      // Matches api_users_requestMs, api_posts_requestMs, etc.
    category = "network",
    displayName = "API Request Time",
    unit = "ms",
    lowerIsBetter = true,
    thresholds = MetricThresholds(good = 300, warning = 1000, critical = 3000)
)

// Then record with specific names
BenchmarkSDK.recordMetric("api_users_requestMs", 245)
BenchmarkSDK.recordMetric("api_posts_requestMs", 189)
BenchmarkSDK.recordMetric("api_auth_requestMs", 567)
```

## Best Practices

### ✅ Do:
- Define metrics at app startup (in `onCreate` or `Application.onCreate()`)
- Use descriptive names: `databaseQueryMs` not `dbQ`
- Set realistic thresholds based on actual performance data
- Use consistent units throughout your app
- Document what your custom metrics measure

### ❌ Don't:
- Define the same metric multiple times
- Use spaces or special characters in metric names (use camelCase)
- Set thresholds without testing real-world performance
- Mix different time units (stick to `ms` for consistency)
- Record metrics without defining them first (works but no metadata)

## Output Example

When you collect metrics, your custom data is included:

```json
{
  "schema_version": "1.0",
  "timestamp": 1733234567890,
  "metrics": {
    "databaseQueryMs": 75,
    "cacheHitRate": 85,
    "...": "...other metrics..."
  },
  "metadata": {
    "custom_metrics": {
      "databaseQueryMs": {
        "category": "database",
        "displayName": "Database Query Time",
        "unit": "ms",
        "lowerIsBetter": true,
        "thresholds": {
          "good": 50,
          "warning": 150,
          "critical": 300
        }
      }
    },
    "custom_categories": {
      "database": {
        "displayName": "Database Operations",
        "icon": "💾"
      }
    }
  }
}
```

## Troubleshooting

### My metric doesn't appear in reports
- ✓ Check you called `defineMetric()` before `collectAndPersist()`
- ✓ Verify you recorded a value with `recordMetric()`
- ✓ Ensure metric name matches exactly (case-sensitive)

### Thresholds aren't working as expected
- ✓ Check `lowerIsBetter` is set correctly for your metric type
- ✓ Verify threshold values match your unit (e.g., ms not seconds)
- ✓ For "higher is better" metrics, thresholds work in reverse

### Custom category not showing up
- ✓ Use `defineCategory()` before `defineMetric()` that uses it
- ✓ Category `id` in `defineMetric()` must match `id` in `defineCategory()`

---

## Phase 2: Automatic Report Generation

**Good news!** Your custom metrics automatically appear in the HTML report with no additional work:

### What Happens Automatically

1. **Tests Run** - Your metrics are collected during instrumented tests
2. **Auto-Persist** - Data saved to device cache (`/sdcard/benchmark-results/`)
3. **Dynamic Report** - Your custom category appears with icon and metrics
4. **Auto-Open** - Browser opens with beautiful HTML report

### Running Benchmarks

```bash
# Run tests (auto-persists to device cache)
./gradlew runBenchmarkTests

# Pull data and generate report
./gradlew pullBenchmarkData
./gradlew generateReport

# Or all-in-one
./gradlew benchmarkComplete
```

### What Your Custom Category Looks Like

In the HTML report, your database category automatically appears as:

```
💾 Database & Storage
  ├─ databaseQueryMs: 45ms → 120ms (+166.7%) ⚠️ Needs Attention
  ├─ cacheHitRate: 85% → 88% (+3.5%) ✅ Good
  └─ ...
```

**Features:**
- ✅ Icon from your category definition
- ✅ Display name instead of ID
- ✅ Color-coded severity (based on thresholds)
- ✅ Automatic change calculations
- ✅ Sorted by your `order` value

---

## Next Steps

- See [PHASE2_SUMMARY.md](PHASE2_SUMMARY.md) - How dynamic reports work
- See [SCHEMA_GUIDE.md](benchmark-sdk/SCHEMA_GUIDE.md) for complete reference
- See [API_DOCUMENTATION.md](API_DOCUMENTATION.md) for all SDK methods
- See [BENCHMARK_WORKFLOW.md](BENCHMARK_WORKFLOW.md) for complete workflow
- See [MainActivity.kt](app/src/main/java/io/app/benchmark/MainActivity.kt) for working example

## Need Help?

Check the example implementation in the sample app or review the schema documentation for more details.

