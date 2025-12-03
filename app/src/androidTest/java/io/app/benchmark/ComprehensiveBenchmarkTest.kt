package io.app.benchmark

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.app.benchmark.sdk.BenchmarkSDK
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive benchmark tests that collect rich performance data.
 *
 * These tests run for both baseline and heavy flavors to generate
 * comparison data for the benchmark report.
 */
@RunWith(AndroidJUnit4::class)
class ComprehensiveBenchmarkTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Set scenario based on build flavor
        BenchmarkSDK.setScenario(BuildConfig.BENCH_SCENARIO)
    }

    @Test
    fun test_01_cpuPerformance() {
        // Test 1: Simple CPU loop
        BenchmarkSDK.timeScenario("cpuSimpleLoopMs") {
            var sum = 0L
            repeat(10_000) { i ->
                sum += i
            }
        }

        // Test 2: Heavy CPU computation (Fibonacci)
        BenchmarkSDK.timeScenario("cpuFibonacciMs") {
            fibonacci(30)
        }

        // Test 3: String operations
        BenchmarkSDK.timeScenario("cpuStringOperationsMs") {
            val builder = StringBuilder()
            repeat(1000) {
                builder.append("test_$it")
            }
            builder.toString()
        }

        // Test 4: Math operations
        BenchmarkSDK.timeScenario("cpuMathOperationsMs") {
            var result = 0.0
            repeat(10_000) { i ->
                result += kotlin.math.sqrt(i.toDouble())
            }
        }
    }

    @Test
    fun test_02_memoryAllocation() {
        // Test 1: Small object allocation
        BenchmarkSDK.timeScenario("memorySmallAllocMs") {
            val list = mutableListOf<String>()
            repeat(1000) {
                list.add("item_$it")
            }
        }

        // Test 2: Large object allocation
        BenchmarkSDK.timeScenario("memoryLargeAllocMs") {
            val array = ByteArray(1024 * 1024 * 5) // 5MB
            array.fill(1)
        }

        // Test 3: Object churn (allocation + GC pressure)
        BenchmarkSDK.timeScenario("memoryChurnMs") {
            repeat(500) {
                val temp = mutableListOf<ByteArray>()
                repeat(10) {
                    temp.add(ByteArray(1024)) // 1KB each
                }
                // Let them be garbage collected
            }
        }

        // Record current memory state
        val runtime = Runtime.getRuntime()
        BenchmarkSDK.recordMetric("memoryAfterTestUsedMb",
            (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024)
        BenchmarkSDK.recordMetric("memoryAfterTestFreeMb",
            runtime.freeMemory() / 1024 / 1024)
    }

    @Test
    fun test_03_networkOperations() {
        // Test 1: Real network request (Google)
        val googleResult = BenchmarkSDK.realNetworkRequest(
            url = "https://www.google.com",
            metricPrefix = "network_google"
        )

        // Test 2: Real network request (Aviation API)
        val aviationResult = BenchmarkSDK.realNetworkRequest(
            url = "https://aviationweather.gov/api/data/metar?ids=KMCI&format=json",
            metricPrefix = "network_aviation"
        )

        // Test 3: Simulated slow network
        BenchmarkSDK.timeScenario("networkSlowSimulationMs") {
            Thread.sleep(if (BuildConfig.BENCH_SCENARIO == "heavy") 3000 else 100)
        }

        // Record network health
        BenchmarkSDK.recordMetric("networkSuccessRate",
            if (googleResult.responseCode == 200 && aviationResult.responseCode == 200) 100 else 50)
    }

    @Test
    fun test_04_storageOperations() {
        // Test 1: File write performance
        BenchmarkSDK.timeScenario("storageFileWriteMs") {
            val file = context.cacheDir.resolve("test_write.txt")
            file.writeText("x".repeat(10_000))
        }

        // Test 2: File read performance
        BenchmarkSDK.timeScenario("storageFileReadMs") {
            val file = context.cacheDir.resolve("test_write.txt")
            if (file.exists()) {
                file.readText()
            }
        }

        // Test 3: File delete
        BenchmarkSDK.timeScenario("storageFileDeleteMs") {
            val file = context.cacheDir.resolve("test_write.txt")
            file.delete()
        }

        // Test 4: SharedPreferences write
        BenchmarkSDK.timeScenario("storagePrefsWriteMs") {
            val prefs = context.getSharedPreferences("benchmark_test", Context.MODE_PRIVATE)
            prefs.edit().apply {
                putString("test_key", "test_value_" + System.currentTimeMillis())
                apply()
            }
        }

        // Test 5: SharedPreferences read
        BenchmarkSDK.timeScenario("storagePrefsReadMs") {
            val prefs = context.getSharedPreferences("benchmark_test", Context.MODE_PRIVATE)
            prefs.getString("test_key", "default")
        }
    }

    @Test
    fun test_05_concurrentOperations() {
        // Test 1: Parallel CPU work
        BenchmarkSDK.timeScenario("concurrentCpuMs") {
            val threads = List(4) {
                Thread {
                    var sum = 0L
                    repeat(5_000) { i -> sum += i }
                }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        // Test 2: Thread creation overhead
        BenchmarkSDK.timeScenario("concurrentThreadCreateMs") {
            val threads = List(10) {
                Thread { Thread.sleep(10) }
            }
            threads.forEach { it.start() }
            threads.forEach { it.join() }
        }

        // Record thread count
        BenchmarkSDK.recordMetric("concurrentActiveThreads", Thread.activeCount())
    }

    @Test
    fun test_06_dataProcessing() {
        // Test 1: JSON parsing simulation
        BenchmarkSDK.timeScenario("dataJsonParseMs") {
            val json = """{"items": [${(1..100).joinToString { """{"id": $it, "name": "item_$it"}""" }}]}"""
            // Simulate parsing overhead
            json.count { it == '{' }
        }

        // Test 2: List sorting
        BenchmarkSDK.timeScenario("dataSortMs") {
            val list = (1..10_000).shuffled()
            list.sorted()
        }

        // Test 3: List filtering
        BenchmarkSDK.timeScenario("dataFilterMs") {
            val list = (1..10_000).toList()
            list.filter { it % 2 == 0 }
        }

        // Test 4: List mapping
        BenchmarkSDK.timeScenario("dataMapMs") {
            val list = (1..10_000).toList()
            list.map { it * 2 }
        }
    }

    @Test
    fun test_07_uiRelatedOperations() {
        // Test 1: Bitmap creation simulation
        BenchmarkSDK.timeScenario("uiBitmapSimMs") {
            val pixels = IntArray(1000 * 1000) // 1MP image
            pixels.fill(0xFF0000FF.toInt())
        }

        // Test 2: Layout inflation simulation (string building as proxy)
        BenchmarkSDK.timeScenario("uiLayoutSimMs") {
            repeat(100) {
                StringBuilder().apply {
                    append("<View>")
                    append("<Child/>")
                    append("</View>")
                }.toString()
            }
        }

        // Test 3: Animation calculation simulation
        BenchmarkSDK.timeScenario("uiAnimationSimMs") {
            repeat(60) { frame ->
                val progress = frame / 60f
                val value = android.view.animation.AccelerateDecelerateInterpolator()
                    .getInterpolation(progress)
            }
        }
    }

    @Test
    fun test_08_customScenarioMetrics() {
        // Scenario-specific behavior
        when (BuildConfig.BENCH_SCENARIO) {
            "baseline" -> {
                // Light operations for baseline
                BenchmarkSDK.timeScenario("scenarioLightworkMs") {
                    Thread.sleep(50)
                }
                BenchmarkSDK.recordMetric("scenarioComplexity", 1)
            }
            "heavy" -> {
                // Heavy operations for stress test
                BenchmarkSDK.timeScenario("scenarioHeavyworkMs") {
                    Thread.sleep(500)
                    var sum = 0L
                    repeat(100_000) { i -> sum += i }
                }
                BenchmarkSDK.recordMetric("scenarioComplexity", 10)
            }
        }

        // Record test completion time
        BenchmarkSDK.recordMetric("testCompletionTimestamp", System.currentTimeMillis())
    }

    @After
    fun collectMetrics() {
        // Get runtime metrics
        val runtimeMetrics = BenchmarkSDK.getActualRuntimeMetrics()
        println("Runtime metrics: $runtimeMetrics")

        // Persist all collected metrics
        val file = BenchmarkSDK.collectScenarioAndPersist(context)
        println("✅ Metrics saved to: ${file.absolutePath}")
    }

    // Helper functions
    private fun fibonacci(n: Int): Long {
        return when {
            n <= 1 -> n.toLong()
            else -> fibonacci(n - 1) + fibonacci(n - 2)
        }
    }
}

