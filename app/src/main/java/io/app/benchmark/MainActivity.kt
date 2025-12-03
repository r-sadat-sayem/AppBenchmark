package io.app.benchmark

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.app.benchmark.demo.ScenarioMetrics
import io.app.benchmark.ui.theme.SampleAppBenchmarkTheme
import io.app.benchmark.sdk.BenchmarkSDK
import io.app.benchmark.sdk.MetricThresholds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Demo: Register custom metrics and categories (Phase 1 feature)
        setupCustomMetrics()

        ScenarioMetrics.runScenarios()
        val actualMetrics = BenchmarkSDK.getActualRuntimeMetrics()
        Log.i("BenchmarkSDK", "Actual runtime metrics: $actualMetrics")
        BenchmarkSDK.setScenario(BuildConfig.BENCH_SCENARIO)
        // Automatically persist scenario metrics on startup
        BenchmarkSDK.collectScenarioAndPersist(this)
        setContent {
            SampleAppBenchmarkTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BenchmarkScreen(modifier = Modifier.padding(innerPadding)) {
                        val file = BenchmarkSDK.collectScenarioAndPersist(this@MainActivity)
                        val actualMetrics = BenchmarkSDK.getActualRuntimeMetrics()
                        Toast.makeText(this@MainActivity, "Scenario metrics saved: ${file.name}\nActual: $actualMetrics", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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