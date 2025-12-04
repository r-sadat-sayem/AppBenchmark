package io.app.benchmark

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import io.app.benchmark.demo.ScenarioMetrics
import io.app.benchmark.ui.theme.SampleAppBenchmarkTheme
import io.app.benchmark.sdk.BenchmarkSDK
import io.app.benchmark.sdk.MetricThresholds

class MainActivity : ComponentActivity() {

    private var isFirstLaunch = true
    private var resumeStartTime = 0L

    // Permission launcher for runtime permission requests
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Log.d("MainActivity", "✅ Storage permissions granted")
        } else {
            Log.w("MainActivity", "⚠️ Storage permissions denied")
            Toast.makeText(
                this,
                "Storage permissions needed for benchmark data",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request storage permissions for debug builds (Android 10 and below)
        requestStoragePermissionsIfNeeded()

        // Phase 3: Track startup type
        if (savedInstanceState == null) {
            // Cold start - first time creating activity
            BenchmarkSDK.recordColdStart()
            Log.d("MainActivity", "📱 Cold start detected")
        } else {
            // Warm start - activity recreated
            BenchmarkSDK.recordWarmStart(intent.getLongExtra("warmStartTime", System.currentTimeMillis()))
            Log.d("MainActivity", "📱 Warm start detected")
        }

        // Demo: Register custom metrics and categories (Phase 1 feature)
        setupCustomMetrics()

        ScenarioMetrics.runScenarios()
        val actualMetrics = BenchmarkSDK.getActualRuntimeMetrics()
        Log.i("BenchmarkSDK", "Actual runtime metrics: $actualMetrics")
        BenchmarkSDK.setScenario(BuildConfig.BENCH_SCENARIO)

        // Mark app as ready for startup time tracking
        BenchmarkSDK.onAppReady()

        setContent {
            SampleAppBenchmarkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BenchmarkScreen(modifier = Modifier.padding(innerPadding)) {
                        val file = BenchmarkSDK.collectScenarioAndPersist(this@MainActivity)
                        val actualMetrics = BenchmarkSDK.getActualRuntimeMetrics()
                        val startupType = BenchmarkSDK.getStartupType() ?: "unknown"
                        Toast.makeText(
                            this@MainActivity,
                            "Scenario: ${file.name}\nStartup: $startupType\nMetrics: $actualMetrics",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // Track hot start (app resumed from background)
        if (!isFirstLaunch) {
            BenchmarkSDK.recordHotStart(resumeStartTime)
            Log.d("MainActivity", "📱 Hot start detected (resumed from background)")
        }
        isFirstLaunch = false
        resumeStartTime = android.os.SystemClock.elapsedRealtime()
    }

    override fun onPause() {
        super.onPause()
        // Record time when going to background for hot start calculation
        resumeStartTime = android.os.SystemClock.elapsedRealtime()
    }

    /**
     * Example: Define custom metrics with metadata.
     * This demonstrates Phase 1 capability - apps can now define their own metrics
     * with proper categorization, units, and thresholds without modifying SDK code.
     */
    private fun setupCustomMetrics() {
        // Define a custom category for database operations
        BenchmarkSDK.defineCategory(
            id = "database",
            displayName = "Database & Storage",
            icon = "💾",
            description = "Database query times and storage operations",
            order = 3
        )

        // Define custom metrics with metadata
        BenchmarkSDK.defineMetric(
            name = "databaseQueryMs",
            category = "database",
            displayName = "Database Query Time",
            unit = "ms",
            lowerIsBetter = true,
            description = "Average time for database SELECT queries",
            thresholds = MetricThresholds(
                good = 50,
                warning = 150,
                critical = 300
            )
        )

        BenchmarkSDK.defineMetric(
            name = "cacheHitRate",
            category = "database",
            displayName = "Cache Hit Rate",
            unit = "%",
            lowerIsBetter = false, // Higher is better for cache hits
            description = "Percentage of requests served from cache",
            thresholds = MetricThresholds(
                good = 80,
                warning = 50,
                critical = 20
            )
        )

        Log.d("BenchmarkSDK", "Custom metrics and categories registered")
    }

    /**
     * Request storage permissions for debug builds.
     * For Android 10 (API 29) and below, external storage permissions are required.
     * For Android 11+ (API 30+), scoped storage is used and permissions not needed.
     */
    private fun requestStoragePermissionsIfNeeded() {
        // Only request for Android 10 and below
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }

            if (permissionsToRequest.isNotEmpty()) {
                Log.d("MainActivity", "📋 Requesting storage permissions: $permissionsToRequest")
                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            } else {
                Log.d("MainActivity", "✅ Storage permissions already granted")
            }
        } else {
            Log.d("MainActivity", "ℹ️ Android 11+: Scoped storage, no permissions needed")
        }
    }
}

@Composable
private fun BenchmarkScreen(modifier: Modifier = Modifier, onSave: () -> Unit) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Benchmark Scenario: ${BuildConfig.BENCH_SCENARIO}")
        Spacer(Modifier.height(8.dp))
        Text("Run app twice with different scenarios to see deltas.")
        Spacer(Modifier.height(12.dp))
        Button(onClick = onSave) { Text("Persist Metrics") }
    }
}
@Preview(showBackground = true)
@Composable
fun DemoBenchmarkPreview() {
    SampleAppBenchmarkTheme { BenchmarkScreen { } }
}