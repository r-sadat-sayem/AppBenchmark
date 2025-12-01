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

    private val customMetricProviders = ConcurrentHashMap<String, () -> Number>()
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

    /** Register a custom metric provider. */
    fun registerMetricProvider(name: String, provider: () -> Number) {
        customMetricProviders[name] = provider
    }

    /** Record a numeric metric explicitly (e.g., scenario duration ms). */
    fun recordMetric(name: String, value: Number) {
        manualMetrics[name] = value
    }

    /** Set the scenario label for this session. */
    fun setScenario(label: String) {
        scenarioLabel = label
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
        startupDurationMs?.let { metrics["startupTimeMs"] = it }
        collectRuntimeMetrics(metrics)
        collectCustom(metrics)

        val outDir = context.getExternalFilesDir("benchmarks") ?: context.filesDir
        if (!outDir.exists()) outDir.mkdirs()

        val jsonFile = File(outDir, "benchmark-latest.json")
        BenchmarkJsonWriter.write(jsonFile, metrics)

        Log.i(TAG, "Benchmark results written to ${jsonFile.absolutePath}")
        return metrics
    }

    /** Collect metrics and persist JSON report with scenario in filename. */
    fun collectScenarioAndPersist(context: Context): File {
        val metrics = collectAndPersist(context)
        val label = scenarioLabel ?: "scenario"
        val outDir = context.getExternalFilesDir("benchmarks") ?: context.filesDir
        val taggedFile = File(outDir, "benchmark-${label}.json")
        BenchmarkJsonWriter.write(taggedFile, metrics)
        return taggedFile
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
        customMetricProviders.forEach { (k, v) ->
            try {
                dest[k] = v().toDouble()
            } catch (t: Throwable) {
                dest["${k}__error"] = t.javaClass.simpleName
            }
        }
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
