package io.app.benchmark

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.app.benchmark.sdk.BenchmarkSDK
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.util.Log
import androidx.test.rule.GrantPermissionRule

/**
 * Comprehensive benchmark tests demonstrating all schema categories.
 *
 * Tests cover: CPU, Memory, Network, Storage, Database, UI, Startup, and Custom metrics
 * Runs for both baseline and heavy flavors to generate rich comparison data.
 *
 * Phase 2: Demonstrates dynamic report generation with custom categories
 */
@RunWith(AndroidJUnit4::class)
class ComprehensiveBenchmarkTest {

    // Grant storage permissions for tests (Android 10 and below)
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private lateinit var context: Context
    private lateinit var scenario: String

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        scenario = BuildConfig.BENCH_SCENARIO

        // Debug: Log the scenario
        Log.d("BenchmarkTest", "=== SCENARIO: $scenario ===")
        println("=== BENCHMARK SCENARIO: $scenario ===")

        // Set scenario based on build flavor
        BenchmarkSDK.setScenario(scenario)
        
        // Define custom categories and metrics for comprehensive testing
        defineCustomMetrics()
    }
    
    /**
     * Define custom categories and metrics to demonstrate Phase 2 dynamic reporting
     */
    private fun defineCustomMetrics() {
        // Database category
        BenchmarkSDK.defineCategory(
            id = "database",
            displayName = "Database Operations",
            icon = "💾",
            description = "Database query, insert, and transaction metrics",
            order = 6
        )
        
        BenchmarkSDK.defineMetric(
            name = "databaseQueryMs",
            category = "database",
            displayName = "Query Time",
            unit = "ms",
            lowerIsBetter = true,
            description = "Time to execute SELECT queries",
            thresholds = io.app.benchmark.sdk.MetricThresholds(good = 50, warning = 150, critical = 300)
        )
        
        BenchmarkSDK.defineMetric(
            name = "databaseInsertMs",
            category = "database",
            displayName = "Insert Time",
            unit = "ms",
            lowerIsBetter = true
        )
        
        // Custom startup metrics
        BenchmarkSDK.defineMetric(
            name = "startupColdBootMs",
            category = "startup",
            displayName = "Cold Boot Time",
            unit = "ms",
            lowerIsBetter = true
        )
        
        // Custom UI metrics
        BenchmarkSDK.defineMetric(
            name = "uiFrameRenderMs",
            category = "ui",
            displayName = "Frame Render Time",
            unit = "ms",
            lowerIsBetter = true
        )
        
        BenchmarkSDK.defineMetric(
            name = "uiScrollFps",
            category = "ui",
            displayName = "Scroll FPS",
            unit = "fps",
            lowerIsBetter = false
        )
        
        // Custom storage metrics
        BenchmarkSDK.defineMetric(
            name = "storageCacheHitRate",
            category = "storage",
            displayName = "Cache Hit Rate",
            unit = "%",
            lowerIsBetter = false
        )
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
            Thread.sleep(if (scenario == "heavy") 3000 else 100)
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
        
        // Test 6: Cache simulation with hit rate
        val cacheHits = simulateCacheAccess()
        BenchmarkSDK.recordMetric("storageCacheHitRate", cacheHits)
        
        // Test 7: Large file I/O
        BenchmarkSDK.timeScenario("storageLargeFileWriteMs") {
            val largeFile = context.cacheDir.resolve("large_test.bin")
            largeFile.writeBytes(ByteArray(1024 * 1024) { it.toByte() }) // 1MB
        }
    }
    
    @Test
    fun test_04a_databaseOperations() {
        // Simulated database operations (in-memory for testing)
        
        // Test 1: Query simulation
        BenchmarkSDK.timeScenario("databaseQueryMs") {
            // Simulate SELECT query with data filtering
            val data = (1..1000).toList()
            data.filter { it % 10 == 0 }
            Thread.sleep(if (scenario == "heavy") 120 else 45)
        }
        
        // Test 2: Insert simulation
        BenchmarkSDK.timeScenario("databaseInsertMs") {
            // Simulate INSERT operations
            val records = mutableListOf<Pair<Int, String>>()
            repeat(100) { i ->
                records.add(i to "record_$i")
            }
            Thread.sleep(if (scenario == "heavy") 80 else 30)
        }
        
        // Test 3: Transaction simulation
        BenchmarkSDK.timeScenario("databaseTransactionMs") {
            // Simulate multi-operation transaction
            Thread.sleep(if (scenario == "heavy") 200 else 75)
            listOf("BEGIN", "INSERT", "UPDATE", "COMMIT")
        }
        
        // Test 4: Complex join simulation
        BenchmarkSDK.timeScenario("databaseJoinMs") {
            val table1 = (1..500).map { it to "value_$it" }
            val table2 = (1..500).map { it to "data_$it" }
            table1.filter { (id, _) -> table2.any { (id2, _) -> id == id2 } }
            Thread.sleep(if (scenario == "heavy") 150 else 60)
        }
        
        // Test 5: Index lookup simulation
        BenchmarkSDK.timeScenario("databaseIndexLookupMs") {
            val indexed = (1..10_000).associate { it to "value_$it" }
            repeat(100) {
                indexed[kotlin.random.Random.nextInt(1, 10_000)]
            }
        }
        
        // Record database health metrics
        BenchmarkSDK.recordMetric("databaseConnectionPoolSize", 10)
        BenchmarkSDK.recordMetric("databaseActiveConnections", if (scenario == "heavy") 8 else 3)
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

        // Test 2: Layout inflation simulation
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
                android.view.animation.AccelerateDecelerateInterpolator()
                    .getInterpolation(progress)
            }
        }
        
        // Test 4: Frame rendering simulation (custom metric)
        BenchmarkSDK.timeScenario("uiFrameRenderMs") {
            // Simulate 60fps frame (16.67ms target)
            repeat(60) {
                Thread.sleep(if (scenario == "heavy") 18 else 12)
            }
        }
        
        // Test 5: Scroll performance simulation
        val scrollFps = simulateScrollPerformance()
        BenchmarkSDK.recordMetric("uiScrollFps", scrollFps)
        
        // Test 6: View hierarchy complexity
        BenchmarkSDK.timeScenario("uiViewHierarchyMs") {
            // Simulate deep view hierarchy traversal
            repeat(if (scenario == "heavy") 100 else 50) {
                // Traverse depth
            }
        }
        
        // Test 7: Touch event processing
        BenchmarkSDK.timeScenario("uiTouchEventMs") {
            repeat(100) {
                // Simulate touch event handling
                kotlin.random.Random.nextFloat() * 1080
                kotlin.random.Random.nextFloat() * 1920
            }
        }
        
        // Record UI health metrics
        BenchmarkSDK.recordMetric("uiDroppedFrames", if (scenario == "heavy") 15 else 2)
        BenchmarkSDK.recordMetric("uiJankCount", if (scenario == "heavy") 8 else 1)
    }
    
    @Test
    fun test_07a_startupOperations() {
        // Phase 3: Comprehensive Startup Benchmarking
        // Measures actual cold, hot, and warm start times

        Log.d("BenchmarkTest", "=== PHASE 3: Comprehensive Startup Metrics ===")

        // Test 1: Cold Start (app launched from scratch)
        val coldStartMs: Long = BenchmarkSDK.timeScenario("startupColdInitialDisplayMs") {
            ScenarioMetrics.simulateColdStart(context)
        }

        // Test 2: Hot Start (app resumed from background, already in memory)
        val hotStartMs: Long = BenchmarkSDK.timeScenario("startupHotInitialDisplayMs") {
            ScenarioMetrics.simulateHotStart(context)
        }

        // Test 3: Warm Start (activity recreated but process exists)
        val warmStartMs: Long = BenchmarkSDK.timeScenario("startupWarmInitialDisplayMs") {
            ScenarioMetrics.simulateWarmStart(context)
        }

        // Test 4: Notification-triggered startup
        val notificationStartMs: Long = BenchmarkSDK.timeScenario("startupNotificationInitialDisplayMs") {
            ScenarioMetrics.benchmarkNotificationStartup(context)
        }

        // Test 5: Full display time (time to interactive)
        BenchmarkSDK.timeScenario("startupColdFullDisplayMs") {
            Thread.sleep(if (scenario == "heavy") 650 else 280)
        }

        BenchmarkSDK.timeScenario("startupWarmFullDisplayMs") {
            Thread.sleep(if (scenario == "heavy") 350 else 150)
        }

        BenchmarkSDK.timeScenario("startupHotFullDisplayMs") {
            Thread.sleep(if (scenario == "heavy") 200 else 80)
        }

        // Test 6: Library initialization time
        BenchmarkSDK.timeScenario("startupInitLibrariesMs") {
            // Simulate SDK/library initialization
            repeat(if (scenario == "heavy") 10 else 5) {
                Thread.sleep(if (scenario == "heavy") 50 else 20)
            }
        }
        
        // Test 7: Splash screen duration
        BenchmarkSDK.timeScenario("startupSplashDurationMs") {
            Thread.sleep(if (scenario == "heavy") 500 else 200)
        }
        
        // Test 8: First paint time
        BenchmarkSDK.timeScenario("startupFirstPaintMs") {
            Thread.sleep(if (scenario == "heavy") 650 else 280)
        }
        
        // Test 9: Time to interactive (user can interact with UI)
        BenchmarkSDK.timeScenario("startupTimeToInteractiveMs") {
            Thread.sleep(if (scenario == "heavy") 1200 else 450)
        }
        
        // Test 10: Process start time (simulate)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            BenchmarkSDK.recordMetric("startupProcessStartMs", if (scenario == "heavy") 180.0 else 80.0)
        }

        // Test 11: Total startup time (derived metric)
        // Total = initial display + full display rendering time
        val coldFullDisplayTime = if (scenario == "heavy") 650L else 280L
        val totalColdStartup = coldStartMs + coldFullDisplayTime
        BenchmarkSDK.recordMetric("startupColdTotalMs", totalColdStartup.toDouble())

        val warmFullDisplayTime = if (scenario == "heavy") 350L else 150L
        val totalWarmStartup = warmStartMs + warmFullDisplayTime
        BenchmarkSDK.recordMetric("startupWarmTotalMs", totalWarmStartup.toDouble())

        val hotFullDisplayTime = if (scenario == "heavy") 200L else 80L
        val totalHotStartup = hotStartMs + hotFullDisplayTime
        BenchmarkSDK.recordMetric("startupHotTotalMs", totalHotStartup.toDouble())

        // Record additional startup health metrics
        BenchmarkSDK.recordMetric("startupBackgroundTasks", if (scenario == "heavy") 12 else 5)
        BenchmarkSDK.recordMetric("startupMemoryFootprintMB", if (scenario == "heavy") 85.0 else 45.0)
        BenchmarkSDK.recordMetric("startupDexLoadedClasses", if (scenario == "heavy") 3500 else 1200)
        BenchmarkSDK.recordMetric("startupDiskReadsKB", if (scenario == "heavy") 2400.0 else 850.0)

        // Compare startup types for reporting
        val startupComparison = mapOf(
            "cold" to coldStartMs,
            "hot" to hotStartMs,
            "warm" to warmStartMs,
            "notification" to notificationStartMs
        )

        BenchmarkSDK.recordMetric("startupTypesCompared", startupComparison.size)

        Log.d("BenchmarkTest", "✅ Phase 3: Comprehensive startup benchmarking complete")
        Log.d("BenchmarkTest", "   Cold (Initial Display): ${coldStartMs}ms")
        Log.d("BenchmarkTest", "   Cold (Total): ${totalColdStartup}ms")
        Log.d("BenchmarkTest", "   Warm (Initial Display): ${warmStartMs}ms")
        Log.d("BenchmarkTest", "   Warm (Total): ${totalWarmStartup}ms")
        Log.d("BenchmarkTest", "   Hot (Initial Display): ${hotStartMs}ms")
        Log.d("BenchmarkTest", "   Hot (Total): ${totalHotStartup}ms")
        Log.d("BenchmarkTest", "   Notification: ${notificationStartMs}ms")
    }

    @Test
    fun test_08_customScenarioMetrics() {
        // Scenario-specific behavior
        when (scenario) {
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

        try {
            // Check permissions first
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                val hasWritePermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED

                Log.d("BenchmarkTest", "WRITE_EXTERNAL_STORAGE permission: $hasWritePermission")

                if (!hasWritePermission) {
                    Log.w("BenchmarkTest", "⚠️ Missing WRITE_EXTERNAL_STORAGE permission")
                }
            }

            // Persist all collected metrics
            Log.d("BenchmarkTest", "Attempting to persist metrics for scenario: $scenario")
            val file = BenchmarkSDK.collectScenarioAndPersist(context)

            val dirPath = file.parentFile?.absolutePath ?: "unknown"
            val fileExists = file.exists()
            val fileSize = if (fileExists) file.length() else 0

            val message = """
                Scenario: $scenario
                Dir: $dirPath
                File: ${file.name}
                Exists: $fileExists
                Size: $fileSize bytes
            """.trimIndent()

            Log.d("BenchmarkTest", "✅ $message")
            println("✅ Metrics saved to: ${file.absolutePath}")

            // Show Toast on UI thread
            context.mainLooper?.let { looper ->
                android.os.Handler(looper).post {
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

            // Wait a bit for Toast to show
            Thread.sleep(1000)

            // Verify file was actually written
            if (!file.exists()) {
                val errorMsg = """
                    ❌ File does not exist after write
                    Path: ${file.absolutePath}
                    Parent exists: ${file.parentFile?.exists()}
                    Parent path: ${file.parentFile?.absolutePath}
                    Can write: ${file.parentFile?.canWrite()}
                    
                    Check logcat for BenchmarkSDK errors
                """.trimIndent()

                Log.e("BenchmarkTest", errorMsg)
                throw IllegalStateException("Benchmark file was not created: ${file.absolutePath}")
            }

        } catch (e: Exception) {
            Log.e("BenchmarkTest", "❌ Error persisting metrics", e)
            println("❌ Error: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }

    // Helper functions
    private fun fibonacci(n: Int): Long {
        return when {
            n <= 1 -> n.toLong()
            else -> fibonacci(n - 1) + fibonacci(n - 2)
        }
    }
    
    /**
     * Simulate cache access with hit/miss ratio
     * @return cache hit rate percentage
     */
    private fun simulateCacheAccess(): Double {
        val totalAccesses = 100
        val cache = mutableMapOf<Int, String>()
        
        // Pre-populate cache
        repeat(50) { i ->
            cache[i] = "cached_$i"
        }
        
        var hits = 0
        repeat(totalAccesses) {
            val key = kotlin.random.Random.nextInt(0, 75)
            if (cache.containsKey(key)) {
                hits++
            }
        }
        
        return (hits.toDouble() / totalAccesses) * 100
    }
    
    /**
     * Simulate scroll performance and calculate FPS
     * @return average frames per second
     */
    private fun simulateScrollPerformance(): Double {
        var actualFrameTime = 0.0
        
        repeat(60) {
            val start = System.nanoTime()
            // Simulate frame work
            Thread.sleep(if (scenario == "heavy") 18 else 12)
            val end = System.nanoTime()
            actualFrameTime += (end - start) / 1_000_000.0 // Convert to ms
        }
        
        val avgFrameTime = actualFrameTime / 60
        return 1000.0 / avgFrameTime // Convert to FPS
    }
}
