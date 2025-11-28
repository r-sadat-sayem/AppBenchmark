# SampleAppBenchmark & Benchmark SDK

This project contains a sample Android app and a lightweight Benchmark SDK library module providing:

- Startup time measurement (process start -> first UI ready)
- Memory & CPU usage metrics
- Custom metric registration API
- JSON + HTML report generation with automatic diff vs previous run
- Simple Gradle task `runBenchmarks` for CI

## Modules
- `app`: Sample application using Jetpack Compose
- `benchmark-sdk`: Reusable benchmarking SDK (can be published as a dependency)

## Quick Start (Local)
```bash
./gradlew runBenchmarks
```
Pull results from device/emulator:
```bash
adb shell ls /sdcard/Android/data/io.app.benchmark/files/benchmarks
adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks ./benchmark-results
```

## Adding to Another Project
1. Publish `benchmark-sdk` (MavenLocal or remote) or copy module.
2. Add dependency:
```kotlin
implementation("io.app.benchmark:benchmark-sdk:<version>")
```
3. For Compose, call `BenchmarkSDK.onAppReady()` after first frame; for XML you can rely on automatic detection.
4. Trigger collection (instrumentation test or manual button):
```kotlin
BenchmarkSDK.collectAndPersist(context)
```

## CI Integration Examples
GitHub Actions step (assuming emulator already started):
```yaml
- name: Run Benchmarks
  run: ./gradlew runBenchmarks
- name: Pull Results
  run: |
    adb pull /sdcard/Android/data/io.app.benchmark/files/benchmarks benchmark-results
- uses: actions/upload-artifact@v4
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

