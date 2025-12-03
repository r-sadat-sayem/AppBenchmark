package io.app.benchmark

import io.app.benchmark.sdk.BenchmarkSDK

/** Provides deterministic baseline vs heavy scenario metrics to show report differences. */
object ScenarioMetrics {
    private enum class Scenario { BASELINE, HEAVY }
    private val scenario: Scenario = when (BuildConfig.BENCH_SCENARIO.lowercase()) {
        "heavy" -> Scenario.HEAVY
        else -> Scenario.BASELINE
    }
    private var retained: List<ByteArray>? = null

    /** Runs all scenario benchmarks: CPU, memory, and network. */
    fun runScenarios() {
        cpuLoop() // Measures CPU-intensive loop
        allocateMemory() // Measures memory allocation
        realNetwork() // Measures real network latency
        // macroBenchmarkStartup() // Placeholder for macrobenchmark integration
    }

    /** Measures CPU performance by running a heavy loop. */
    private fun cpuLoop() {
        val iterations = if (scenario == Scenario.BASELINE) 300_000 else 2_000_000
        BenchmarkSDK.timeScenario("cpuHeavyLoopMs") {
            var acc = 0L
            for (i in 0 until iterations) acc += i
            acc
        }
    }

    /** Measures memory allocation performance. */
    private fun allocateMemory() {
        BenchmarkSDK.timeScenario("memoryAllocationMs") {
            val blocks = if (scenario == Scenario.BASELINE) 10 else 140
            val size = if (scenario == Scenario.BASELINE) 32 * 1024 else 256 * 1024
            val list = ArrayList<ByteArray>(blocks)
            repeat(blocks) { list += ByteArray(size) }
            if (scenario == Scenario.HEAVY) retained = list.takeLast(50)
            list.size
        }
    }

    /** Measures real network latency using BenchmarkSDK.realNetworkRequest. */
    private fun realNetwork() {
        val url = "https://aviationweather.gov/api/data/metar?ids=KMCI&format=json"
        val result = BenchmarkSDK.realNetworkRequest(
            url = url,
            metricPrefix = "network_aviation"
        )
        BenchmarkSDK.recordMetric("measuredNetworkLatencyMs", result.durationMs)
    }

    /** Placeholder for macrobenchmark startup measurement integration. */
    // private fun macroBenchmarkStartup() {
    //     // To be implemented: Use MacrobenchmarkRule for cold startup measurement
    // }

    /** Triggers a basic notification for macrobenchmark notification startup measurement. */
    fun triggerTestNotification(context: android.content.Context) {
        val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val channelId = "benchmark_test_channel"
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = android.app.NotificationChannel(channelId, "Benchmark Test", android.app.NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
        val intent = android.content.Intent(context, io.app.benchmark.MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = android.app.PendingIntent.getActivity(context, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE)
        val builder = android.app.Notification.Builder(context, channelId)
            .setContentTitle("Benchmark Test Notification")
            .setContentText("Tap to launch app for startup measurement.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
        notificationManager.notify(1001, builder.build())
    }
}
