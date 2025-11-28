import json
from pathlib import Path

results_dir = Path("benchmark-results")
results_dir.mkdir(parents=True, exist_ok=True)

baseline_file = results_dir / "benchmark-baseline.json"
heavy_file = results_dir / "benchmark-heavy.json"
report_path = results_dir / "report.html"

if not baseline_file.exists() or not heavy_file.exists():
    report_path.write_text("<h1>Benchmark Report</h1><p>Need both benchmark-baseline.json and benchmark-heavy.json to compare.</p>")
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

all_keys = sorted(set(baseline.keys()) | set(heavy.keys()))
rows = []
for k in all_keys:
    b = baseline.get(k)
    h = heavy.get(k)
    if isinstance(b, (int, float)) and isinstance(h, (int, float)) and b not in (0, None):
        change = ((h - b) / b) * 100.0
        change_str = f"{change:.2f}%"
    else:
        change_str = "N/A"
    rows.append(f"<tr><td>{k}</td><td>{b}</td><td>{h}</td><td>{change_str}</td></tr>")

html = "".join([
    "<h1>Benchmark Scenario Comparison</h1>",
    "<p>Baseline vs Heavy</p>",
    "<table border='1'><tr><th>Metric</th><th>Baseline</th><th>Heavy</th><th>Change (%)</th></tr>",
    *rows,
    "</table>"
])

report_path.write_text(html)
print(f"Report generated: {report_path}")