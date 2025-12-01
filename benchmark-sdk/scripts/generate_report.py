import json
from pathlib import Path

results_dir = Path("benchmark-results/benchmarks")
results_dir.mkdir(parents=True, exist_ok=True)

baseline_file = results_dir / "benchmark-baseline.json"
heavy_file = results_dir / "benchmark-heavy.json"
report_json_path = results_dir / "benchmark-report.json"

if not baseline_file.exists() or not heavy_file.exists():
    report_json_path.write_text(json.dumps({"error": "Missing scenario files; need both benchmark-baseline.json and benchmark-heavy.json to compare."}))
    print("Missing scenario files; generated placeholder report.")
    raise SystemExit(0)

def load_metrics(file: Path):
    try:
        data = json.loads(file.read_text())
        return data.get("metrics", data)  # support plain metrics root
    except Exception:
        return {}

baseline = load_metrics(baseline_file)
heavy = load_metrics(heavy_file)

# Grouping logic
cpu_os_keys = [k for k in baseline.keys() | heavy.keys() if k.startswith("cpu") or k.startswith("process") or k.startswith("startup") or k.startswith("os")]
memory_keys = [k for k in baseline.keys() | heavy.keys() if k.startswith("memory") or "leak" in k.lower()]
network_keys = [k for k in baseline.keys() | heavy.keys() if k.startswith("network") or k.endswith("requestMs") or k.endswith("responseCode") or k.endswith("responseLength") or k.endswith("error")]
other_keys = [k for k in baseline.keys() | heavy.keys() if k not in cpu_os_keys + memory_keys + network_keys]

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

report_data = {
    "cpu_os": make_rows(cpu_os_keys),
    "memory": make_rows(memory_keys, highlight_leak=True),
    "network": make_rows(network_keys, highlight_error=True),
    "other": make_rows(other_keys) if other_keys else [],
}

report_json_path.write_text(json.dumps(report_data, indent=2))
print(f"Report data generated: {report_json_path}")
