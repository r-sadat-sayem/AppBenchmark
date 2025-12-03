# Phase 1: Unified Metric Schema & Automated Benchmark Workflow

## Overview
This phase standardizes the output format for benchmark results and simplifies the workflow to a single command. All metrics are collected and persisted automatically for both baseline and heavy scenarios.

## Workflow
1. Run `./gradlew runBenchmarks`.
2. The task runs instrumented tests for both baseline and heavy flavors.
3. Metrics are automatically collected and saved as `benchmark-baseline.json` and `benchmark-heavy.json`.
4. The `generateBenchmarkReport` task pulls these files and generates `report.json` and `report.html`.
5. If either JSON is missing, report generation will fail with an error.

## No Manual Steps Required
- You do NOT need to manually pull or persist JSON files.
- The workflow is fully automated as long as both variants are tested.

## Troubleshooting
- If you see an error about missing scenario files, ensure both baseline and heavy tests have run.
- The report will be regenerated if deleted.

## Example Command
```sh
./gradlew runBenchmarks
```

## Test Coverage
Comprehensive test cases are included to ensure both scenarios are exercised and metrics are collected.

## Schema Reference
See `benchmark-sdk/SCHEMA_GUIDE.md` for details on the metric schema.

## Output Files
- `benchmark-results/benchmarks/benchmark-baseline.json`
- `benchmark-results/benchmarks/benchmark-heavy.json`
- `benchmark-results/report.json`
- `benchmark-results/benchmarks/report.html`

## Next Steps
Proceed to Phase 2 for dynamic report rendering.
