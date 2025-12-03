import json
from pathlib import Path
import time

root_dir = Path("benchmark-results/")
results_dir = root_dir / "benchmarks"
results_dir.mkdir(parents=True, exist_ok=True)

baseline_file = results_dir / "benchmark-baseline.json"
heavy_file = results_dir / "benchmark-heavy.json"
report_json_path = root_dir / "report.json"

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

# Merge custom metadata from both scenarios
custom_metrics_meta = {}
custom_metrics_meta.update(baseline_metadata.get("custom_metrics", {}))
custom_metrics_meta.update(heavy_metadata.get("custom_metrics", {}))

custom_categories_meta = {}
custom_categories_meta.update(baseline_metadata.get("custom_categories", {}))
custom_categories_meta.update(heavy_metadata.get("custom_categories", {}))

latest_file = max([baseline_file, heavy_file], key=lambda f: f.stat().st_mtime)
latest_type = "baseline" if latest_file == baseline_file else "heavy"
latest_time = time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(latest_file.stat().st_mtime))

all_keys = set(baseline.keys()) | set(heavy.keys())

cpu_os_keys = [k for k in all_keys if k.startswith(("cpu", "process", "startup", "os"))]
memory_keys = [k for k in all_keys if k.startswith("memory") or "leak" in k.lower()]
network_keys = [k for k in all_keys if k.startswith("network") or k.endswith(("requestMs", "responseCode", "responseLength", "error"))]
other_keys = [k for k in all_keys if k not in cpu_os_keys + memory_keys + network_keys]

def get_severity(row):
    if row["highlight_leak"] or row["highlight_error"]:
        return "Needs Attention"
    change = row["change"]
    if change is None:
        return "Normal"
    abs_change = abs(change)
    if abs_change > 50:
        return "Needs Attention"
    elif abs_change > 20:
        return "Warning"
    elif abs_change > 5:
        return "Minor"
    else:
        return "Normal"

def make_rows(keys, highlight_leak=False, highlight_error=False):
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
        row = {
            "metric": k,
            "baseline": b_fmt,
            "heavy": h_fmt,
            "change": change_val,
            "highlight_leak": highlight_leak and ("leak" in k.lower() or "retained" in k.lower()),
            "highlight_error": highlight_error and ("error" in k.lower()),
        }
        row["severity"] = get_severity(row)
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

report_data = {
    "schema_version": "1.0",
    "latest_type": latest_type,
    "latest_time": latest_time,
    "collected_metrics": collected_metrics,
    "missing_metrics": missing_metrics,
    "cpu_os": make_rows(cpu_os_keys),
    "memory": make_rows(memory_keys, highlight_leak=True),
    "network": make_rows(network_keys, highlight_error=True),
    "other": make_rows(other_keys) if other_keys else [],
}

# Include custom metadata if present
if custom_metrics_meta or custom_categories_meta:
    report_data["metadata"] = {}
    if custom_metrics_meta:
        report_data["metadata"]["custom_metrics"] = custom_metrics_meta
    if custom_categories_meta:
        report_data["metadata"]["custom_categories"] = custom_categories_meta

all_changes = [
    row["change"]
    for section in [report_data["cpu_os"], report_data["memory"], report_data["network"], report_data["other"]]
    for row in section if row["change"] is not None
]
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