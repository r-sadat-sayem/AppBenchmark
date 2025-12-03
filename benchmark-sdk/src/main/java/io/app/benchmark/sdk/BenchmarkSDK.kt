package io.app.benchmark.sdk

import android.content.Context
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.util.Log
import io.app.benchmark.sdk.internal.StartupTimeTracker
import io.app.benchmark.sdk.output.BenchmarkJsonWriter
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.roundToLong

/** Public entrypoint for the Benchmark SDK */
object BenchmarkSDK {
    private const val TAG = "BenchmarkSDK"

    private var startupRecorded = false
    private var startupDurationMs: Long? = null
    private val manualMetrics = ConcurrentHashMap<String, Number>()

    @Volatile private var scenarioLabel: String? = null

    /** Mark that the first frame (or main UI) is ready. Records startup duration. */
    fun onAppReady() {
        if (!startupRecorded) {
            startupDurationMs = SystemClock.elapsedRealtime() - StartupTimeTracker.processStartTime
            startupRecorded = true
            Log.d(TAG, "Startup time captured: ${startupDurationMs}ms")
        }
    }

    /** Record a numeric metric explicitly (e.g., scenario duration ms). */
    fun recordMetric(name: String, value: Number) {
        manualMetrics[name] = value
    }

    /** Set the scenario label for this session. */
    fun setScenario(label: String) {
        scenarioLabel = label
    }

    /**
     * Define a custom metric with metadata for proper reporting and analysis.
     * This allows applications to add new metrics without modifying the SDK schema.
     *
     * @param name Unique metric identifier
     * @param category Category for grouping (e.g., "database", "cache")
     * @param displayName Human-readable name for reports
     * @param unit Unit of measurement (e.g., "ms", "bytes", "count")
     * @param lowerIsBetter Whether lower values indicate better performance
     * @param description Optional description of what this metric measures
     * @param thresholds Optional performance thresholds for severity assessment
     */
    fun defineMetric(
        name: String,
        category: String,
        displayName: String,
        unit: String,
        lowerIsBetter: Boolean = true,
        description: String? = null,
        thresholds: MetricThresholds? = null
    ) {
        val metadata = MetricMetadata(
            name = name,
            category = category,
            displayName = displayName,
            unit = unit,
            lowerIsBetter = lowerIsBetter,
            description = description,
            thresholds = thresholds
        )
        MetricRegistry.registerMetric(metadata)
        Log.d(TAG, "Registered custom metric: $name in category $category")
    }

    /**
     * Define a custom category for organizing metrics.
     *
     * @param id Unique category identifier
     * @param displayName Human-readable category name
     * @param icon Optional icon or emoji
     * @param description Optional category description
     * @param order Display order (lower numbers appear first)
     */
    fun defineCategory(
        id: String,
        displayName: String,
        icon: String? = null,
        description: String? = null,
        order: Int = 999
    ) {
        val metadata = CategoryMetadata(
            id = id,
            displayName = displayName,
            icon = icon,
            description = description,
            order = order
        )
        MetricRegistry.registerCategory(metadata)
        Log.d(TAG, "Registered custom category: $id")
    }

    /** Time a scenario and record its duration (ms) under the provided metric name. */
    inline fun <T> timeScenario(metricName: String, block: () -> T): T {
        val start = SystemClock.elapsedRealtime()
        val result = block()
        val duration = SystemClock.elapsedRealtime() - start
        recordMetric(metricName, duration)
        return result
    }

    /** Collect metrics and persist JSON report. Returns the metrics map. */
    fun collectAndPersist(context: Context): Map<String, Any> {
        val metrics = LinkedHashMap<String, Any>()

        // Collect all metrics
        startupDurationMs?.let { metrics["startupTimeMs"] = it }
        collectRuntimeMetrics(metrics)
        collectCustom(metrics)

        // Add APK size (static metric)
        val apkPath = context.applicationInfo.sourceDir
        val apkFile = File(apkPath)
        metrics["apkSizeBytes"] = apkFile.length()

        // Add build config (static metric)
        val buildConfig = mapOf(
            "versionName" to try { context.packageManager.getPackageInfo(context.packageName, 0).versionName } catch (_: Exception) { null },
            "versionCode" to try { context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode } catch (_: Exception) { null },
            "applicationId" to context.packageName
        )
        metrics["buildConfig"] = buildConfig

        // Use device cache directory (persists across app reinstalls)
        // Path: /sdcard/benchmark-results/
        val outDir = File(android.os.Environment.getExternalStorageDirectory(), "benchmark-results")

        // Ensure directory exists
        if (!outDir.exists()) {
            val created = outDir.mkdirs()
            Log.i(TAG, "Creating benchmark directory: ${outDir.absolutePath} - success: $created")

            if (!created) {
                Log.e(TAG, "❌ Failed to create directory: ${outDir.absolutePath}")
                // Fallback to app-specific directory if device cache fails
                val fallbackDir = context.getExternalFilesDir("benchmarks") ?: context.filesDir
                fallbackDir.mkdirs()
                Log.w(TAG, "Using fallback directory: ${fallbackDir.absolutePath}")
                return writeMetricsToFile(fallbackDir, metrics)
            }
        }

        return writeMetricsToFile(outDir, metrics)
    }

    /** Write metrics to file in specified directory. */
    private fun writeMetricsToFile(outDir: File, metrics: Map<String, Any>): Map<String, Any> {
        // Use scenario label if set, otherwise use "latest"
        val label = scenarioLabel ?: "latest"
        val jsonFile = File(outDir, "benchmark-$label.json")

        // Write with metadata
        BenchmarkJsonWriter.write(jsonFile, metrics)

        Log.i(TAG, "✅ Benchmark results written to ${jsonFile.absolutePath} (scenario: $label)")
        Log.i(TAG, "   File exists: ${jsonFile.exists()}, Size: ${jsonFile.length()} bytes")
        Log.i(TAG, "   Directory: ${outDir.absolutePath}")

        return metrics
    }

    /** Collect metrics and persist JSON report with scenario in filename. */
    fun collectScenarioAndPersist(context: Context): File {
        collectAndPersist(context)

        val label = scenarioLabel ?: "latest"
        // Use device cache directory (same as collectAndPersist)
        val outDir = File(android.os.Environment.getExternalStorageDirectory(), "benchmark-results")
        val file = File(outDir, "benchmark-$label.json")

        Log.i(TAG, "Benchmark file: ${file.absolutePath} (exists: ${file.exists()}, size: ${file.length()})")
        return file
    }

    /** Get actual runtime metrics from the app process and OS. */
    fun getActualRuntimeMetrics(): Map<String, Any> {
        val metrics = LinkedHashMap<String, Any>()
        // Memory
        val pssKb = Debug.getPss()
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory())
        metrics["memoryPssKb"] = pssKb
        metrics["memoryUsedBytes"] = usedMem
        metrics["memoryHeapMaxBytes"] = runtime.maxMemory()
        // CPU
        val cpuTimeMs = Process.getElapsedCpuTime()
        metrics["processCpuTimeMs"] = cpuTimeMs
        // Derived metric: memory usage percent of max
        val percent = if (runtime.maxMemory() > 0) (usedMem.toDouble() / runtime.maxMemory().toDouble()) * 100 else 0.0
        metrics["memoryUsagePercent"] = (percent * 100).roundToLong() / 100.0
        // Optionally, add network latency if available
        manualMetrics["measuredNetworkLatencyMs"]?.let { metrics["measuredNetworkLatencyMs"] = it }
        return metrics
    }

    private fun collectRuntimeMetrics(dest: MutableMap<String, Any>) {
        // Memory
        val pssKb = Debug.getPss() // in KB
        val runtime = Runtime.getRuntime()
        val usedMem = (runtime.totalMemory() - runtime.freeMemory())
        dest["memoryPssKb"] = pssKb
        dest["memoryUsedBytes"] = usedMem
        dest["memoryHeapMaxBytes"] = runtime.maxMemory()

        // CPU (process elapsed CPU time since start)
        val cpuTimeMs = Process.getElapsedCpuTime()
        dest["processCpuTimeMs"] = cpuTimeMs

        // Derived metric: memory usage percent of max
        val percent = if (runtime.maxMemory() > 0) (usedMem.toDouble() / runtime.maxMemory().toDouble()) * 100 else 0.0
        dest["memoryUsagePercent"] = (percent * 100).roundToLong() / 100.0
    }

    private fun collectCustom(dest: MutableMap<String, Any>) {
        manualMetrics.forEach { (k,v) -> dest[k] = v }
    }

    data class NetworkBenchmarkResult(
        val url: String,
        val method: String,
        val durationMs: Long,
        val responseCode: Int?,
        val responseLength: Int?,
        val error: String?
    )

    fun realNetworkRequest(
        url: String,
        metricPrefix: String = "network",
        method: String = "GET",
        body: String? = null
    ): NetworkBenchmarkResult {
        val start = SystemClock.elapsedRealtime()
        var responseCode: Int? = null
        var responseLength: Int? = null
        var error: String? = null
        try {
            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = method.uppercase()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            if (method.uppercase() == "POST" || method.uppercase() == "PUT") {
                connection.doOutput = true
                body?.let {
                    val bytes = it.toByteArray(Charsets.UTF_8)
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.outputStream.use { os -> os.write(bytes) }
                }
            }
            responseCode = connection.responseCode
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            responseLength = response.length
        } catch (e: Exception) {
            error = e.javaClass.simpleName + ": " + e.message
        }
        val duration = SystemClock.elapsedRealtime() - start
        // Register metrics for reporting
        recordMetric("${metricPrefix}_requestMs", duration)
        recordMetric("${metricPrefix}_responseCode", responseCode ?: -1)
        recordMetric("${metricPrefix}_responseLength", responseLength ?: 0)
        if (error != null) {
            recordMetric("${metricPrefix}_error", 1)
        }
        return NetworkBenchmarkResult(url, method, duration, responseCode, responseLength, error)
    }
}
