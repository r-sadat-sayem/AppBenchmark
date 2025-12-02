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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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