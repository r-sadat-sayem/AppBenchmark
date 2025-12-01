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

# Grouping logic
cpu_os_keys = [k for k in baseline.keys() | heavy.keys() if k.startswith("cpu") or k.startswith("process") or k.startswith("startup") or k.startswith("os")]
memory_keys = [k for k in baseline.keys() | heavy.keys() if k.startswith("memory") or "leak" in k.lower()]
network_keys = [k for k in baseline.keys() | heavy.keys() if k.startswith("network") or k.endswith("requestMs") or k.endswith("responseCode") or k.endswith("responseLength") or k.endswith("error")]
other_keys = [k for k in baseline.keys() | heavy.keys() if k not in cpu_os_keys + memory_keys + network_keys]

def make_rows(keys, highlight_leak=False, highlight_error=False):
    rows = []
    for k in sorted(keys):
        b = baseline.get(k)
        h = heavy.get(k)
        if isinstance(b, (int, float)) and isinstance(h, (int, float)) and b not in (0, None):
            change = ((h - b) / b) * 100.0
            change_str = f"{change:.2f}%"
        else:
            change_str = "N/A"
        style = ""
        if highlight_leak and ("leak" in k.lower() or "retained" in k.lower()):
            style = " style='background-color:#ffcccc'"
        if highlight_error and ("error" in k.lower()):
            style = " style='background-color:#ffd2d2'"
        rows.append(f"<tr{style}><td>{k}</td><td>{b}</td><td>{h}</td><td>{change_str}</td></tr>")
    return rows

html = ["<h1>Benchmark Scenario Comparison</h1>", "<p>Baseline vs Heavy</p>"]

html.append("<h2>CPU and OS Related</h2>")
html.append("<table border='1'><tr><th>Metric</th><th>Baseline</th><th>Heavy</th><th>Change (%)</th></tr>")
html.extend(make_rows(cpu_os_keys))
html.append("</table>")

html.append("<h2>Memory and Leaks</h2>")
html.append("<table border='1'><tr><th>Metric</th><th>Baseline</th><th>Heavy</th><th>Change (%)</th></tr>")
html.extend(make_rows(memory_keys, highlight_leak=True))
html.append("</table>")

html.append("<h2>Network Related</h2>")
html.append("<table border='1'><tr><th>Metric</th><th>Baseline</th><th>Heavy</th><th>Change (%)</th></tr>")
html.extend(make_rows(network_keys, highlight_error=True))
html.append("</table>")

if other_keys:
    html.append("<h2>Other Metrics</h2>")
    html.append("<table border='1'><tr><th>Metric</th><th>Baseline</th><th>Heavy</th><th>Change (%)</th></tr>")
    html.extend(make_rows(other_keys))
    html.append("</table>")

report_path.write_text("".join(html))
print(f"Report generated: {report_path}")