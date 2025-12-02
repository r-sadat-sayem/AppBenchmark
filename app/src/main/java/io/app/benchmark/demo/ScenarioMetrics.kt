package io.app.benchmark.demo


import io.app.benchmark.BuildConfig
import io.app.benchmark.sdk.BenchmarkSDK

object ScenarioMetrics {
    private enum class Scenario { BASELINE, HEAVY }
    private val scenario = when (BuildConfig.BENCH_SCENARIO.lowercase()) {
        "heavy" -> Scenario.HEAVY
        else -> Scenario.BASELINE
    }

    private var retained: List<ByteArray>? = null

    fun runScenarios() {
        cpuLoop()
        allocateMemory()
        mockNetwork()
        checkApi1NetwrokMetrics()
        checkApi2NetwrokMetrics()
    }

    private fun checkApi1NetwrokMetrics() {
        if (scenario == Scenario.HEAVY) {
            Thread.sleep(2000) // Add artificial delay for heavy scenario
        }
        BenchmarkSDK.realNetworkRequest(
            url = "https://aviationweather.gov/api/data/metar?ids=KMCI&format=json",
            metricPrefix = "network_aviation"
        )
    }

    private fun checkApi2NetwrokMetrics() {
        BenchmarkSDK.realNetworkRequest(
            url = "https://www.google.com",
            metricPrefix = "network_google"
        )
        if (scenario == Scenario.HEAVY) {
            Thread.sleep(3000) // Add artificial delay for heavy scenario
        }
    }

    private fun cpuLoop() {
        val iterations = if (scenario == Scenario.BASELINE) 300_000 else 2_000_000
        BenchmarkSDK.timeScenario("cpuHeavyLoopMs") {
            var acc = 0L
            for (i in 0 until iterations) acc += i
            acc
        }
    }

    private fun allocateMemory() {
        BenchmarkSDK.timeScenario("memoryAllocationMs") {
            val blocks = if (scenario == Scenario.BASELINE) 8 else 120
            val size = if (scenario == Scenario.BASELINE) 32 * 1024 else 256 * 1024
            val list = ArrayList<ByteArray>(blocks)
            repeat(blocks) { list += ByteArray(size) }
            if (scenario == Scenario.HEAVY) retained = list.takeLast(40)
            list.size
        }
    }

    private fun mockNetwork() {
        // Use real network request to measure latency
        val url = "https://www.google.com" // You can change to any endpoint
        val result = BenchmarkSDK.realNetworkRequest(
            url = url,
            metricPrefix = "network_mock"
        )
        // Optionally, record the measured latency for dynamic provider usage
        BenchmarkSDK.recordMetric("measuredNetworkLatencyMs", result.durationMs)
    }
}