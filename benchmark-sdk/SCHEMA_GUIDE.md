# Metric Schema Guide

## Overview

The BenchmarkSDK uses a **unified metric schema** to define how performance metrics are collected, categorized, displayed, and analyzed. This guide explains the schema structure and how to work with it.

## Schema Version

**Current Version:** 1.0  
**Last Updated:** 2025-12-03

## Schema Structure

### 1. Categories

Categories group related metrics together for organized reporting. Each category has:

```json
{
  "categoryId": {
    "displayName": "Human Readable Name",
    "icon": "🎨",
    "description": "What this category contains",
    "order": 1
  }
}
```

#### Built-in Categories

| ID | Display Name | Icon | Description | Order |
|----|--------------|------|-------------|-------|
| `cpu` | CPU & Performance | ⚡ | CPU usage and processing metrics | 1 |
| `memory` | Memory & Heap | 🧠 | Memory allocation and heap usage | 2 |
| `network` | Network & API | 🌐 | Network requests and connectivity | 3 |
| `ui` | UI & Rendering | 🎨 | UI rendering and frame performance | 4 |
| `startup` | App Startup | 🚀 | Application initialization | 5 |
| `storage` | Storage & Database | 💾 | File I/O and database operations | 6 |
| `build` | Build & Configuration | 📦 | Build artifacts and metadata | 7 |
| `custom` | Custom Metrics | ⚙️ | User-defined metrics | 8 |

### 2. Metrics

Each metric definition includes:

```json
{
  "metricName": {
    "category": "cpu",
    "displayName": "Human Readable Name",
    "unit": "ms",
    "lowerIsBetter": true,
    "description": "What this metric measures",
    "thresholds": {
      "good": 100,
      "warning": 500,
      "critical": 1000
    },
    "pattern": false,
    "highlightError": false,
    "isMetadata": false
  }
}
```

#### Metric Properties

| Property | Type | Description |
|----------|------|-------------|
| `category` | string | Category ID this metric belongs to |
| `displayName` | string | Human-readable name for reports |
| `unit` | string | Unit of measurement (ms, bytes, %, boolean, etc.) |
| `lowerIsBetter` | boolean | If `true`, lower values = better performance |
| `description` | string | Explanation of what this metric measures |
| `thresholds` | object | Performance threshold values (optional) |
| `pattern` | boolean | If `true`, this is a wildcard pattern metric |
| `highlightError` | boolean | If `true`, highlight as error in reports |
| `isMetadata` | boolean | If `true`, treat as metadata, not a performance metric |

#### Threshold Values

Thresholds define performance levels:

- **good**: Value at or below which performance is excellent (for `lowerIsBetter: true`)
- **warning**: Value at which performance becomes concerning
- **critical**: Value at which performance is unacceptable

For metrics where `lowerIsBetter: false` (e.g., cache hit rate), the logic is inverted.

### 3. Wildcard Patterns

Some metrics use wildcard patterns to match multiple similar metrics:

```json
{
  "network_*_requestMs": {
    "category": "network",
    "displayName": "Network Request Duration",
    "unit": "ms",
    "lowerIsBetter": true,
    "pattern": true
  }
}
```

This matches:
- `network_google_requestMs`
- `network_aviation_requestMs`
- `network_api_requestMs`
- etc.

## Using the Schema in Code

### Kotlin/Android

#### Register Custom Metrics

```kotlin
import io.app.benchmark.sdk.BenchmarkSDK
import io.app.benchmark.sdk.MetricThresholds

// Define a custom category
BenchmarkSDK.defineCategory(
    id = "database",
    displayName = "Database Operations",
    icon = "💾",
    description = "Database query and transaction metrics",
    order = 6
)

// Define a custom metric
BenchmarkSDK.defineMetric(
    name = "databaseQueryMs",
    category = "database",
    displayName = "Database Query Time",
    unit = "ms",
    lowerIsBetter = true,
    description = "Average time for SELECT queries",
    thresholds = MetricThresholds(
        good = 50,
        warning = 150,
        critical = 300
    )
)

// Record the metric value
BenchmarkSDK.recordMetric("databaseQueryMs", 75)
```

#### Register Multiple Metrics

```kotlin
fun setupCustomMetrics() {
    // Cache metrics
    BenchmarkSDK.defineMetric(
        name = "cacheHitRate",
        category = "database",
        displayName = "Cache Hit Rate",
        unit = "%",
        lowerIsBetter = false, // Higher is better
        thresholds = MetricThresholds(good = 80, warning = 50, critical = 20)
    )
    
    // UI metrics
    BenchmarkSDK.defineMetric(
        name = "frameDropCount",
        category = "ui",
        displayName = "Dropped Frames",
        unit = "count",
        lowerIsBetter = true,
        thresholds = MetricThresholds(good = 0, warning = 5, critical = 20)
    )
    
    // Custom business metric
    BenchmarkSDK.defineMetric(
        name = "activeUserSessions",
        category = "custom",
        displayName = "Active User Sessions",
        unit = "count",
        lowerIsBetter = false,
        isMetadata = true // Not a performance metric
    )
}
```

### Python (Report Generation)

Custom metrics are automatically included in report generation. The `generate_report.py` script:

1. Reads metric metadata from benchmark JSON files
2. Merges custom definitions from baseline and heavy scenarios
3. Includes metadata in the output `report.json`

## Output Format

### Benchmark JSON Output

When you call `BenchmarkSDK.collectAndPersist()`, the output includes:

```json
{
  "schema_version": "1.0",
  "timestamp": 1733234567890,
  "metrics": {
    "startupTimeMs": 1250,
    "memoryPssKb": 45678,
    "databaseQueryMs": 75,
    "cacheHitRate": 85
  },
  "metadata": {
    "custom_metrics": {
      "databaseQueryMs": {
        "category": "database",
        "displayName": "Database Query Time",
        "unit": "ms",
        "lowerIsBetter": true,
        "description": "Average time for SELECT queries",
        "thresholds": {
          "good": 50,
          "warning": 150,
          "critical": 300
        }
      },
      "cacheHitRate": {
        "category": "database",
        "displayName": "Cache Hit Rate",
        "unit": "%",
        "lowerIsBetter": false,
        "thresholds": {
          "good": 80,
          "warning": 50,
          "critical": 20
        }
      }
    },
    "custom_categories": {
      "database": {
        "displayName": "Database Operations",
        "icon": "💾",
        "description": "Database query and transaction metrics",
        "order": 6
      }
    }
  }
}
```

### Report JSON Output

The comparison report includes schema information:

```json
{
  "schema_version": "1.0",
  "latest_type": "baseline",
  "latest_time": "2025-12-03 14:50:10",
  "collected_metrics": ["startupTimeMs", "memoryPssKb", ...],
  "missing_metrics": [],
  "cpu_os": [...],
  "memory": [...],
  "network": [...],
  "other": [...],
  "metadata": {
    "custom_metrics": {...},
    "custom_categories": {...}
  },
  "overall_performance": {...}
}
```

## Best Practices

### 1. Naming Conventions

- Use camelCase for metric names: `databaseQueryMs`, `cacheHitRate`
- Include unit suffix when helpful: `Ms`, `Kb`, `Bytes`, `Percent`
- Be descriptive but concise

### 2. Category Organization

- Use existing categories when possible
- Create custom categories for domain-specific metrics
- Set appropriate `order` values to control display sequence

### 3. Threshold Selection

- Base thresholds on real-world performance data
- Consider user experience impact when setting critical thresholds
- Use `null` for thresholds if no meaningful limits exist

### 4. Unit Standardization

Common units:
- Time: `ms` (milliseconds)
- Memory: `bytes`, `KB`, `MB`
- Percentage: `%`
- Count: `count`
- Boolean: `boolean` (0 = false, 1 = true)
- Response code: `code` (HTTP status codes)

### 5. LowerIsBetter Logic

Set `lowerIsBetter`:
- `true` for: latency, memory usage, error counts, CPU time
- `false` for: throughput, cache hit rates, success counts, frame rates

## Schema Validation

### Version Compatibility

The SDK checks schema version compatibility:

- **1.0.x**: Current version, backward compatible
- Future versions will include migration guides if breaking changes occur

### Legacy Format Support

The SDK maintains backward compatibility with pre-schema benchmark files:

- Files without `schema_version` are treated as legacy format
- Metrics are extracted from the top-level JSON object
- No metadata is available for legacy files

## Extending the Schema

### For SDK Contributors

To add built-in metrics:

1. Update `benchmark-sdk/schemas/metric-schema.json`
2. Add metric to appropriate category
3. Define complete metadata including thresholds
4. Update documentation

### For SDK Users

To add custom metrics:

1. Use `BenchmarkSDK.defineMetric()` in your app code
2. Optionally define custom categories with `defineCategory()`
3. Record values with `recordMetric()`
4. Metadata automatically included in output

## Troubleshooting

### Metrics Not Appearing in Reports

- Ensure you call `defineMetric()` before `collectAndPersist()`
- Check that metric names match between definition and recording
- Verify category ID exists (define with `defineCategory()` if custom)

### Thresholds Not Working

- Confirm threshold values match your metric's unit
- Check `lowerIsBetter` is set correctly for your metric type
- Ensure thresholds are ordered: `good < warning < critical` (or reversed if `lowerIsBetter: false`)

### Schema Version Mismatch

- Update SDK to latest version
- Check compatibility notes in changelog
- Use migration guide if upgrading across major versions

## Examples

See working examples in:
- `app/src/main/java/io/app/benchmark/MainActivity.kt` - Custom metric registration
- `benchmark-sdk/schemas/metric-schema.json` - Complete schema definition
- `benchmark-results/report.json` - Example output with metadata

## Future Enhancements

Planned schema features:
- Multi-scenario comparisons (beyond baseline vs heavy)
- Historical trend tracking
- Automated performance regression detection
- Custom severity calculation rules
- Metric dependencies and correlations

## Support

For questions or issues:
- Check the main [README.md](../README.md)
- Review [API_DOCUMENTATION.md](../API_DOCUMENTATION.md)
- See sample implementations in the demo app

