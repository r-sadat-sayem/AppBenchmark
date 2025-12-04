import json
from pathlib import Path
import time
from collections import defaultdict

root_dir = Path("benchmark-results/")
results_dir = root_dir / "benchmarks"
results_dir.mkdir(parents=True, exist_ok=True)

baseline_file = results_dir / "benchmark-baseline.json"
heavy_file = results_dir / "benchmark-heavy.json"
report_json_path = root_dir / "report.json"
schema_file = Path("benchmark-sdk/schemas/metric-schema.json")

EXPECTED_METRICS = [
    "startupTimeMs",
    "memoryPssKb",
    "memoryUsedBytes",
    "memoryHeapMaxBytes",
    "memoryUsagePercent",
    "processCpuTimeMs",
    "measuredNetworkLatencyMs",
    "cpuHeavyLoopMs",
    "memoryAllocationMs",
    "simulatedRequestMs",
    "scenarioLabel",
    "apkSizeBytes",
    "buildConfig",
    # Network metrics are dynamic, so we check for their prefixes
    "network_*_requestMs",
    "network_*_responseCode",
    "network_*_responseLength",
    "network_*_error",
]

def load_schema():
    """Load the metric schema defining categories and metric metadata."""
    try:
        if schema_file.exists():
            return json.loads(schema_file.read_text())
        else:
            print(f"Warning: Schema file not found at {schema_file}, using basic defaults")
            return {"categories": {}, "metrics": {}}
    except Exception as e:
        print(f"Error loading schema: {e}")
        return {"categories": {}, "metrics": {}}

def load_metrics(file: Path):
    """Load metrics from benchmark JSON file.

    Handles both new schema format (with schema_version and nested metrics)
    and legacy format (direct metrics map).
    Returns tuple: (metrics_dict, metadata_dict)
    """
    try:
        data = json.loads(file.read_text())

        # New schema format (v1.0+)
        if "schema_version" in data:
            metrics = data.get("metrics", {})
            metadata = data.get("metadata", {})
            return metrics, metadata

        # Legacy format - all top-level keys are metrics
        return data, {}
    except Exception as e:
        print(f"Error loading {file}: {e}")
        return {}, {}

if not baseline_file.exists() or not heavy_file.exists():
    report_json_path.write_text(json.dumps({
        "schema_version": "1.0",
        "error": "Missing scenario files; need both benchmark-baseline.json and benchmark-heavy.json to compare."
    }))
    print(f"Missing scenario files; generated placeholder report: {report_json_path}")
    raise SystemExit(0)

baseline, baseline_metadata = load_metrics(baseline_file)
heavy, heavy_metadata = load_metrics(heavy_file)

# Load schema
schema = load_schema()
schema_categories = schema.get("categories", {})
schema_metrics = schema.get("metrics", {})

# Merge custom metadata from both scenarios
custom_metrics_meta = {}
custom_metrics_meta.update(baseline_metadata.get("custom_metrics", {}))
custom_metrics_meta.update(heavy_metadata.get("custom_metrics", {}))

custom_categories_meta = {}
custom_categories_meta.update(baseline_metadata.get("custom_categories", {}))
custom_categories_meta.update(heavy_metadata.get("custom_categories", {}))

# Merge schema categories with custom categories
all_category_metadata = {}
all_category_metadata.update(schema_categories)
all_category_metadata.update(custom_categories_meta)

# Merge schema metrics with custom metrics
all_metric_metadata = {}
all_metric_metadata.update(schema_metrics)
all_metric_metadata.update(custom_metrics_meta)

latest_file = max([baseline_file, heavy_file], key=lambda f: f.stat().st_mtime)
latest_type = "baseline" if latest_file == baseline_file else "heavy"
latest_time = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(latest_file.stat().st_mtime))

all_keys = set(baseline.keys()) | set(heavy.keys())

def categorize_metric(metric_name):
    """Determine category for a metric using schema metadata.

    Returns the category name for the metric, or 'other' if not found.
    """
    # Direct lookup in merged metric metadata
    if metric_name in all_metric_metadata:
        return all_metric_metadata[metric_name].get("category", "other")

    # Pattern matching for wildcard metrics (e.g., network_*_requestMs)
    for pattern, meta in all_metric_metadata.items():
        if meta.get("pattern", False) and "*" in pattern:
            prefix = pattern.split("*")[0]
            suffix = pattern.split("*")[-1]
            if metric_name.startswith(prefix) and metric_name.endswith(suffix):
                return meta.get("category", "other")

    # Fallback: use string prefix heuristics
    if metric_name.startswith(("cpu", "process")):
        return "cpu"
    elif metric_name.startswith("memory") or "leak" in metric_name.lower():
        return "memory"
    elif metric_name.startswith("network"):
        return "network"
    elif metric_name.startswith("startup"):
        return "startup"
    elif metric_name.startswith(("apk", "build")):
        return "build"
    elif metric_name.startswith(("database", "db", "storage")):
        return "storage"
    elif metric_name.startswith("ui"):
        return "ui"

    return "other"

# Categorize all metrics dynamically
categorized_metrics = defaultdict(list)
for metric_key in all_keys:
    category = categorize_metric(metric_key)
    categorized_metrics[category].append(metric_key)

def get_severity(row, category_name="other"):
    """Determine severity level based on metric thresholds and change percentage.

    Uses category-specific thresholds for startup metrics:
    - Cold startup: >10% → "Needs Attention", 5-10% → "Warning"
    - Warm/Hot startup: >5% → "Needs Attention", 2-5% → "Warning"
    """
    if row["highlight_leak"] or row["highlight_error"]:
        return "Needs Attention"

    change = row["change"]
    if change is None:
        return "Normal"

    abs_change = abs(change)
    metric_name = row["metric"].lower()

    # Startup-specific thresholds (Phase 3)
    if category_name == "startup" or metric_name.startswith("startup"):
        # Cold startup thresholds (more lenient)
        if "cold" in metric_name or "notification" in metric_name:
            if abs_change > 10:
                return "Needs Attention"
            elif abs_change > 5:
                return "Warning"
            elif abs_change > 2:
                return "Minor"
            else:
                return "Normal"
        # Warm/Hot startup thresholds (stricter)
        elif "warm" in metric_name or "hot" in metric_name:
            if abs_change > 5:
                return "Needs Attention"
            elif abs_change > 2:
                return "Warning"
            elif abs_change > 1:
                return "Minor"
            else:
                return "Normal"

    # Default thresholds for other metrics
    if abs_change > 50:
        return "Needs Attention"
    elif abs_change > 20:
        return "Warning"
    elif abs_change > 5:
        return "Minor"
    else:
        return "Normal"

def make_rows(keys, category_name="other"):
    """Generate row data for metrics in a category.

    Uses metadata to determine highlighting behavior.
    """
    rows = []
    for k in sorted(keys):
        b = baseline.get(k)
        h = heavy.get(k)
        def fmt(val):
            if isinstance(val, float):
                return float(f"{val:.3f}")
            return val
        b_fmt = fmt(b)
        h_fmt = fmt(h)
        if isinstance(b, (int, float)) and isinstance(h, (int, float)) and b not in (0, None):
            change = ((h - b) / b) * 100.0
            change_val = float(f"{change:.3f}")
        else:
            change_val = None

        # Determine highlighting based on metric metadata or category
        metric_meta = all_metric_metadata.get(k, {})
        highlight_leak = category_name == "memory" and ("leak" in k.lower() or "retained" in k.lower())
        highlight_error = metric_meta.get("highlightError", False) or ("error" in k.lower())

        row = {
            "metric": k,
            "baseline": b_fmt,
            "heavy": h_fmt,
            "change": change_val,
            "highlight_leak": highlight_leak,
            "highlight_error": highlight_error,
            # Add unit and detailed info from schema
            "unit": metric_meta.get("unit", ""),
            "displayName": metric_meta.get("displayName", k),
            "description": metric_meta.get("description", ""),
            "detailedInfo": metric_meta.get("detailedInfo", ""),
            "goodExample": metric_meta.get("goodExample", ""),
            "badExample": metric_meta.get("badExample", ""),
            "thresholds": metric_meta.get("thresholds", {})
        }
        row["severity"] = get_severity(row, category_name)  # Pass category for startup thresholds
        rows.append(row)
    return rows

def metric_found(metric, keys):
    if "*" in metric:
        prefix = metric.split("*")[0]
        suffix = metric.split("*")[-1]
        return any(k.startswith(prefix) and k.endswith(suffix) for k in keys)
    return metric in keys

collected_metrics = sorted(all_keys)
missing_metrics = [m for m in EXPECTED_METRICS if not metric_found(m, all_keys)]

# Build dynamic category structure
categories = {}
for category_name, metric_keys in categorized_metrics.items():
    if metric_keys:  # Only include categories with metrics
        categories[category_name] = make_rows(metric_keys, category_name)

# Build category metadata for the report
category_metadata = {}
for category_name in categories.keys():
    if category_name in all_category_metadata:
        category_metadata[category_name] = all_category_metadata[category_name]
    else:
        # Provide default metadata for categories not in schema
        category_metadata[category_name] = {
            "displayName": category_name.replace("_", " ").title(),
            "icon": "📊",
            "description": f"{category_name.title()} metrics",
            "order": 999  # Put at end
        }

report_data = {
    "schema_version": "1.0",
    "latest_type": latest_type,
    "latest_time": latest_time,
    "collected_metrics": collected_metrics,
    "missing_metrics": missing_metrics,
}

# Add dynamic categories
report_data.update(categories)

# Add category metadata
report_data["category_metadata"] = category_metadata

# Backward compatibility: keep legacy structure
# (cpu_os merged into cpu, other remains as other)
report_data["cpu_os"] = categories.get("cpu", []) + categories.get("startup", [])
report_data["memory"] = categories.get("memory", [])
report_data["network"] = categories.get("network", [])
report_data["other"] = categories.get("other", []) + categories.get("build", [])

# Include custom metadata if present
if custom_metrics_meta or custom_categories_meta:
    if "metadata" not in report_data:
        report_data["metadata"] = {}
    if custom_metrics_meta:
        report_data["metadata"]["custom_metrics"] = custom_metrics_meta
    if custom_categories_meta:
        report_data["metadata"]["custom_categories"] = custom_categories_meta

# Calculate overall performance from all categories
all_changes = []
for category_name, rows in categories.items():
    for row in rows:
        if row["change"] is not None:
            all_changes.append(row["change"])

if all_changes:
    avg_change = sum(all_changes) / len(all_changes)
    avg_change_fmt = float(f"{avg_change:.3f}")
    if avg_change_fmt > 10:
        status = "Degraded"
    elif avg_change_fmt < -10:
        status = "Improved"
    else:
        status = "No Significant Change"
    report_data["overall_performance"] = {
        "average_change": avg_change_fmt,
        "status": status,
        "summary": f"Overall performance change: {avg_change_fmt}% ({status})"
    }
else:
    report_data["overall_performance"] = {
        "average_change": None,
        "status": "Unknown",
        "summary": "Insufficient data for overall performance summary."
    }

report_json_path.write_text(json.dumps(report_data, indent=2))
print(f"Report data generated: {report_json_path}")
print(f"Categories included: {', '.join(sorted(categories.keys()))}")
